-- devices 테이블 보관 (즉시 DROP하지 않음, 90일 후 삭제 예정)
ALTER TABLE devices RENAME TO devices_deprecated;
COMMENT ON TABLE devices_deprecated IS
  'Deprecated: FCM device table. Migrated 2026-05-12. Drop after 2026-08-12.';

-- 채널 설정 테이블
CREATE TABLE notification_channels (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    channel_type         VARCHAR(20) NOT NULL,
    display_name         VARCHAR(100) NOT NULL,
    credential_encrypted TEXT NOT NULL,
    target_identifier    VARCHAR(255),
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_error           TEXT,
    error_count          INT NOT NULL DEFAULT 0,
    last_delivered_at    TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT fk_notification_channels_users
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_channel_type
        CHECK (channel_type IN ('SLACK', 'TELEGRAM', 'DISCORD')),
    CONSTRAINT chk_channel_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ERROR'))
);

CREATE INDEX idx_notification_channels_user_id
    ON notification_channels(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_notification_channels_status
    ON notification_channels(status) WHERE deleted_at IS NULL;

-- 라우팅 규칙 테이블
CREATE TABLE routing_rules (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    rule_name       VARCHAR(100) NOT NULL,
    priority_order  INT NOT NULL DEFAULT 100,
    conditions      JSONB NOT NULL DEFAULT '{}',
    channel_ids     JSONB NOT NULL DEFAULT '[]',
    stop_on_match   BOOLEAN NOT NULL DEFAULT TRUE,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_routing_rules_users
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_routing_rules_user_priority
    ON routing_rules(user_id, priority_order)
    WHERE is_enabled = TRUE AND deleted_at IS NULL;
