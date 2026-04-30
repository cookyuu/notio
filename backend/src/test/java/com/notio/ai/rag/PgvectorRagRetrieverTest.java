package com.notio.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.ai.embedding.EmbeddingProvider;
import com.notio.chat.metrics.ChatMetrics;
import com.notio.common.config.properties.NotioRagProperties;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class PgvectorRagRetrieverTest {

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PgvectorRagRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new PgvectorRagRetriever(
                embeddingProvider,
                jdbcTemplate,
                new NotioRagProperties(5, 3),
                new ChatMetrics(new NotioMetrics(new SimpleMeterRegistry(), new NotioMetricsTagPolicy()))
        );
    }

    @Test
    void retrieveEmbedsQuestionAndSearchesUserScopedActiveNotifications() {
        final Long userId = 10L;
        final float[] embedding = new float[] {0.1f, 0.2f, 0.3f};
        final RagDocument document = new RagDocument(
                1L,
                "GITHUB",
                "PR review",
                "Review requested",
                "HIGH",
                null,
                0.91
        );
        when(embeddingProvider.embed("오늘 중요한 알림 알려줘")).thenReturn(embedding);
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(500),
                eq(500),
                eq("[0.1,0.2,0.3]"),
                eq(userId),
                eq(userId),
                eq("[0.1,0.2,0.3]"),
                eq(5)
        )).thenReturn(List.of(document));

        final List<RagDocument> documents = retriever.retrieve(userId, "오늘 중요한 알림 알려줘", Optional.empty());

        assertThat(documents).containsExactly(document);
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(500),
                eq(500),
                eq("[0.1,0.2,0.3]"),
                eq(userId),
                eq(userId),
                eq("[0.1,0.2,0.3]"),
                eq(5)
        );
        assertThat(sqlCaptor.getValue())
                .contains("ne.embedding <=> ?::vector")
                .contains("ne.user_id = ?")
                .contains("n.user_id = ?")
                .contains("ne.deleted_at IS NULL")
                .contains("n.deleted_at IS NULL")
                .contains("LIMIT ?")
                .doesNotContain("n.created_at >= ?")
                .doesNotContain("n.created_at < ?");
    }

    @Test
    void retrieveAddsCreatedAtRangeWhenTimeRangeExists() {
        final Long userId = 10L;
        final float[] embedding = new float[] {0.1f, 0.2f, 0.3f};
        final Instant startInclusive = Instant.parse("2026-04-23T00:00:00Z");
        final Instant endExclusive = Instant.parse("2026-04-24T00:00:00Z");
        final TimeRange timeRange = new TimeRange(startInclusive, endExclusive);
        when(embeddingProvider.embed("오늘 중요한 알림 알려줘")).thenReturn(embedding);
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(500),
                eq(500),
                eq("[0.1,0.2,0.3]"),
                eq(userId),
                eq(userId),
                eq(Timestamp.from(startInclusive)),
                eq(Timestamp.from(endExclusive)),
                eq("[0.1,0.2,0.3]"),
                eq(5)
        )).thenReturn(List.of());

        final List<RagDocument> documents = retriever.retrieve(userId, "오늘 중요한 알림 알려줘", Optional.of(timeRange));

        assertThat(documents).isEmpty();
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq(500),
                eq(500),
                eq("[0.1,0.2,0.3]"),
                eq(userId),
                eq(userId),
                eq(Timestamp.from(startInclusive)),
                eq(Timestamp.from(endExclusive)),
                eq("[0.1,0.2,0.3]"),
                eq(5)
        );
        assertThat(sqlCaptor.getValue())
                .contains("ne.user_id = ?")
                .contains("n.user_id = ?")
                .contains("ne.deleted_at IS NULL")
                .contains("n.deleted_at IS NULL")
                .contains("AND n.created_at >= ?")
                .contains("AND n.created_at < ?")
                .contains("ORDER BY ne.embedding <=> ?::vector")
                .contains("LIMIT ?");
    }

    @Test
    void retrieveReturnsEmptyListWhenNoDocumentsMatch() {
        final Long userId = 10L;
        when(embeddingProvider.embed("없는 알림 찾아줘")).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(500),
                eq(500),
                eq("[0.1,0.2,0.3]"),
                eq(userId),
                eq(userId),
                eq("[0.1,0.2,0.3]"),
                eq(5)
        )).thenReturn(List.of());

        final List<RagDocument> documents = retriever.retrieve(userId, "없는 알림 찾아줘", Optional.empty());

        assertThat(documents).isEmpty();
    }

    @Test
    void retrieveRejectsUnexpectedEmbeddingDimension() {
        when(embeddingProvider.embed("질문")).thenReturn(new float[] {0.1f, 0.2f});

        assertThatThrownBy(() -> retriever.retrieve(10L, "질문", Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected query embedding dimension");
    }
}
