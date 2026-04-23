package com.notio.ai.rag;

import com.notio.ai.embedding.EmbeddingProvider;
import com.notio.common.config.properties.NotioRagProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PgvectorRagRetriever implements RagRetriever {

    private static final int BODY_SUMMARY_LENGTH = 500;

    private final EmbeddingProvider embeddingProvider;
    private final JdbcTemplate jdbcTemplate;
    private final NotioRagProperties ragProperties;

    @Override
    public List<RagDocument> retrieve(
            final Long userId,
            final String question,
            final Optional<TimeRange> timeRange
    ) {
        final float[] queryEmbedding = embeddingProvider.embed(question);
        validateDimension(queryEmbedding);
        final String vectorLiteral = toVectorLiteral(queryEmbedding);
        final List<Object> parameters = new ArrayList<>();
        parameters.add(BODY_SUMMARY_LENGTH);
        parameters.add(BODY_SUMMARY_LENGTH);
        parameters.add(vectorLiteral);
        parameters.add(userId);
        parameters.add(userId);

        final StringBuilder sql = new StringBuilder(
                """
                SELECT
                    n.id AS notification_id,
                    n.source,
                    n.title,
                    CASE
                        WHEN length(n.body) > ? THEN substring(n.body FROM 1 FOR ?) || '...'
                        ELSE n.body
                    END AS body_summary,
                    n.priority,
                    n.created_at,
                    1 - (ne.embedding <=> ?::vector) AS similarity_score
                FROM notification_embeddings ne
                JOIN notifications n ON n.id = ne.notification_id
                WHERE ne.user_id = ?
                  AND n.user_id = ?
                  AND ne.deleted_at IS NULL
                  AND n.deleted_at IS NULL
                """
        );
        timeRange.ifPresent(range -> {
            sql.append("  AND n.created_at >= ?\n");
            sql.append("  AND n.created_at < ?\n");
            parameters.add(Timestamp.from(range.startInclusive()));
            parameters.add(Timestamp.from(range.endExclusive()));
        });
        sql.append(
                """
                ORDER BY ne.embedding <=> ?::vector
                LIMIT ?
                """
        );
        parameters.add(vectorLiteral);
        parameters.add(ragProperties.topK());

        return jdbcTemplate.query(sql.toString(), this::mapDocument, parameters.toArray());
    }

    private RagDocument mapDocument(final ResultSet resultSet, final int rowNumber) throws SQLException {
        final Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new RagDocument(
                resultSet.getLong("notification_id"),
                resultSet.getString("source"),
                resultSet.getString("title"),
                resultSet.getString("body_summary"),
                resultSet.getString("priority"),
                createdAt == null ? null : createdAt.toInstant(),
                resultSet.getDouble("similarity_score")
        );
    }

    private void validateDimension(final float[] embedding) {
        if (embedding.length != ragProperties.embeddingDimension()) {
            throw new IllegalStateException(
                    "Unexpected query embedding dimension: expected=%d actual=%d"
                            .formatted(ragProperties.embeddingDimension(), embedding.length)
            );
        }
    }

    private String toVectorLiteral(final float[] embedding) {
        final StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%s", embedding[index]));
        }
        return builder.append(']').toString();
    }
}
