CREATE TABLE channel_delivery_logs (
    id                   BIGSERIAL PRIMARY KEY,
    notification_id      BIGINT NOT NULL,
    channel_id           BIGINT NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count        INT NOT NULL DEFAULT 0,
    last_error           TEXT,
    external_message_id  VARCHAR(255),
    next_retry_at        TIMESTAMPTZ,
    delivered_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_logs_notifications
        FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_delivery_logs_channels
        FOREIGN KEY (channel_id) REFERENCES notification_channels(id),
    CONSTRAINT chk_delivery_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'DEAD'))
);

CREATE INDEX idx_delivery_logs_notification_id
    ON channel_delivery_logs(notification_id);
CREATE INDEX idx_delivery_logs_retry
    ON channel_delivery_logs(next_retry_at)
    WHERE status = 'RETRY' AND next_retry_at IS NOT NULL;

CREATE UNIQUE INDEX uq_delivery_logs_active
    ON channel_delivery_logs(notification_id, channel_id)
    WHERE status IN ('PENDING', 'RETRY');
