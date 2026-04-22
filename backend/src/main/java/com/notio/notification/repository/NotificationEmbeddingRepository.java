package com.notio.notification.repository;

import com.notio.notification.domain.Notification;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsByNotificationIdAndContentHash(final Long notificationId, final String contentHash) {
        final Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM notification_embeddings
                    WHERE notification_id = ?
                      AND content_hash = ?
                      AND deleted_at IS NULL
                )
                """,
                Boolean.class,
                notificationId,
                contentHash
        );
        return Boolean.TRUE.equals(exists);
    }

    public void save(final Notification notification, final String contentHash, final float[] embedding) {
        jdbcTemplate.update(
                """
                INSERT INTO notification_embeddings (
                    notification_id,
                    user_id,
                    source,
                    content_hash,
                    embedding
                )
                VALUES (?, ?, ?, ?, ?::vector)
                ON CONFLICT DO NOTHING
                """,
                notification.getId(),
                notification.getUserId(),
                notification.getSource().name(),
                contentHash,
                toVectorLiteral(embedding)
        );
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
