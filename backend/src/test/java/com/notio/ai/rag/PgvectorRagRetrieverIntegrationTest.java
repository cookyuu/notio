package com.notio.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.common.config.properties.NotioRagProperties;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.repository.NotificationEmbeddingRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class PgvectorRagRetrieverIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("notio_test")
            .withUsername("notio")
            .withPassword("notio");

    private JdbcTemplate jdbcTemplate;
    private NotificationEmbeddingRepository embeddingRepository;

    @BeforeEach
    void setUp() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        embeddingRepository = new NotificationEmbeddingRepository(jdbcTemplate);

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("DROP TABLE IF EXISTS notification_embeddings");
        jdbcTemplate.execute("DROP TABLE IF EXISTS notifications");
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGSERIAL PRIMARY KEY,
                    primary_email VARCHAR(255) NOT NULL,
                    display_name VARCHAR(100) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted_at TIMESTAMPTZ
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE notifications (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    source VARCHAR(50) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    body VARCHAR(2000) NOT NULL,
                    priority VARCHAR(50) NOT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted_at TIMESTAMPTZ
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE notification_embeddings (
                    id BIGSERIAL PRIMARY KEY,
                    notification_id BIGINT NOT NULL REFERENCES notifications(id),
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    source VARCHAR(50) NOT NULL,
                    content_hash VARCHAR(64) NOT NULL,
                    embedding vector(3) NOT NULL,
                    embedded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted_at TIMESTAMPTZ
                )
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX idx_notification_embeddings_notification_content_hash
                    ON notification_embeddings(notification_id, content_hash)
                    WHERE deleted_at IS NULL
                """);
    }

    @Test
    void pgvectorExtensionIsAvailable() {
        final Integer extensionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class
        );

        assertThat(extensionCount).isEqualTo(1);
    }

    @Test
    void embeddingRepositoryInsertsVectorEmbedding() {
        insertUser(1L);
        insertNotification(10L, 1L, NotificationSource.GITHUB, "PR review", "Review requested", NotificationPriority.HIGH);
        final Notification notification = notification(
                10L,
                1L,
                NotificationSource.GITHUB,
                "PR review",
                "Review requested",
                NotificationPriority.HIGH
        );

        embeddingRepository.save(notification, "hash-github", new float[] {1.0f, 0.0f, 0.0f});

        assertThat(embeddingRepository.existsByNotificationIdAndContentHash(10L, "hash-github")).isTrue();
        final Integer dimensions = jdbcTemplate.queryForObject(
                "SELECT vector_dims(embedding) FROM notification_embeddings WHERE notification_id = 10",
                Integer.class
        );
        assertThat(dimensions).isEqualTo(3);
    }

    @Test
    void retrieverUsesCosineSimilarityAndUserScopeIsolation() {
        insertUser(1L);
        insertUser(2L);
        insertNotification(10L, 1L, NotificationSource.GITHUB, "PR review", "Review requested", NotificationPriority.HIGH);
        insertNotification(11L, 1L, NotificationSource.SLACK, "Incident update", "Deploy incident", NotificationPriority.HIGH);
        insertNotification(20L, 2L, NotificationSource.GITHUB, "Other user's PR", "Should not leak", NotificationPriority.HIGH);
        embeddingRepository.save(notification(
                10L,
                1L,
                NotificationSource.GITHUB,
                "PR review",
                "Review requested",
                NotificationPriority.HIGH
        ), "hash-user-1-github", new float[] {1.0f, 0.0f, 0.0f});
        embeddingRepository.save(notification(
                11L,
                1L,
                NotificationSource.SLACK,
                "Incident update",
                "Deploy incident",
                NotificationPriority.HIGH
        ), "hash-user-1-slack", new float[] {0.0f, 1.0f, 0.0f});
        embeddingRepository.save(notification(
                20L,
                2L,
                NotificationSource.GITHUB,
                "Other user's PR",
                "Should not leak",
                NotificationPriority.HIGH
        ), "hash-user-2-github", new float[] {1.0f, 0.0f, 0.0f});
        final PgvectorRagRetriever retriever = new PgvectorRagRetriever(
                input -> new float[] {1.0f, 0.0f, 0.0f},
                jdbcTemplate,
                new NotioRagProperties(5, 3)
        );

        final List<RagDocument> documents = retriever.retrieve(1L, "PR 리뷰 요청 알려줘");

        assertThat(documents)
                .extracting(RagDocument::notificationId)
                .containsExactly(10L, 11L);
        assertThat(documents.getFirst().similarityScore()).isGreaterThan(documents.get(1).similarityScore());
    }

    private void insertUser(final Long userId) {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, primary_email, display_name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """,
                userId,
                "user-%d@example.com".formatted(userId),
                "user-%d".formatted(userId)
        );
    }

    private void insertNotification(
            final Long notificationId,
            final Long userId,
            final NotificationSource source,
            final String title,
            final String body,
            final NotificationPriority priority
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO notifications (id, user_id, source, title, body, priority)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                notificationId,
                userId,
                source.name(),
                title,
                body,
                priority.name()
        );
    }

    private Notification notification(
            final Long id,
            final Long userId,
            final NotificationSource source,
            final String title,
            final String body,
            final NotificationPriority priority
    ) {
        return Notification.builder()
                .id(id)
                .userId(userId)
                .source(source)
                .title(title)
                .body(body)
                .priority(priority)
                .build();
    }
}
