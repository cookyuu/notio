package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class V14MigrationTest {

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    @SuppressWarnings("resource")
    static void runMigrations() {
        PostgreSQLContainer<?> postgres;
        try {
            postgres = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("notio_test")
                .withUsername("notio")
                .withPassword("notio");
            postgres.start();
        } catch (Exception e) {
            Assumptions.abort("Docker not available or container failed to start: " + e.getMessage());
            return;
        }

        DataSource dataSource = new DriverManagerDataSource(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void notificationsTableHasAiSummaryColumn() {
        assertThat(columnExists("notifications", "ai_summary")).isTrue();
    }

    @Test
    void routingRulesTableHasDeliveryModeColumn() {
        assertThat(columnExists("routing_rules", "delivery_mode")).isTrue();
    }

    @Test
    void routingRulesTableHasDigestIntervalMinColumn() {
        assertThat(columnExists("routing_rules", "digest_interval_min")).isTrue();
    }

    @Test
    void deliveryModeDefaultIsImmediate() {
        String columnDefault = jdbcTemplate.queryForObject(
            "SELECT column_default FROM information_schema.columns "
                + "WHERE table_name = 'routing_rules' AND column_name = 'delivery_mode'",
            String.class
        );
        assertThat(columnDefault).contains("IMMEDIATE");
    }

    @Test
    void channelDeliveryLogsSupportsDigestPendingStatus() {
        jdbcTemplate.execute(
            "INSERT INTO users (primary_email, display_name, status) "
                + "VALUES ('test@test.com', 'Test', 'ACTIVE')"
        );
        Long userId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE primary_email = 'test@test.com'", Long.class);

        jdbcTemplate.execute(
            "INSERT INTO notification_channels (user_id, channel_type, display_name, credential_encrypted) "
                + "VALUES (" + userId + ", 'SLACK', 'test', 'enc')"
        );
        Long channelId = jdbcTemplate.queryForObject(
            "SELECT id FROM notification_channels WHERE user_id = " + userId, Long.class);

        jdbcTemplate.execute(
            "INSERT INTO notifications (user_id, source, title, body, priority) "
                + "VALUES (" + userId + ", 'GITHUB', 'test', 'body', 'HIGH')"
        );
        Long notifId = jdbcTemplate.queryForObject(
            "SELECT id FROM notifications WHERE user_id = " + userId, Long.class);

        jdbcTemplate.update(
            "INSERT INTO channel_delivery_logs (notification_id, channel_id, status) "
                + "VALUES (?, ?, 'DIGEST_PENDING')",
            notifId, channelId
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM channel_delivery_logs WHERE status = 'DIGEST_PENDING'",
            Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void chatMessagesTableWasRenamedToDeprecated() {
        assertThat(tableExists("chat_messages")).isFalse();
        assertThat(tableExists("chat_messages_deprecated")).isTrue();
    }

    @Test
    void idxDeliveryLogsDigestPendingIndexExists() {
        assertThat(indexExists("idx_delivery_logs_digest_pending")).isTrue();
    }

    @Test
    void idxDeliveryLogsDeliveredAtIndexExists() {
        assertThat(indexExists("idx_delivery_logs_delivered_at")).isTrue();
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_name = ? AND column_name = ?",
            Integer.class, tableName, columnName
        );
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name = ?",
            Integer.class, tableName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?",
            Integer.class, indexName
        );
        return count != null && count > 0;
    }
}
