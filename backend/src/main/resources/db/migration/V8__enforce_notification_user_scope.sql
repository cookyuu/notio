CREATE TABLE IF NOT EXISTS connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'CLAUDE',
    auth_type VARCHAR(50) NOT NULL DEFAULT 'API_KEY',
    display_name VARCHAR(100) NOT NULL DEFAULT 'Connection',
    account_label VARCHAR(255),
    external_account_id VARCHAR(255),
    external_workspace_id VARCHAR(255),
    subscription_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    capabilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_connections_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_connections_user_id
    ON connections(user_id)
    WHERE deleted_at IS NULL;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS connection_id BIGINT;

UPDATE notifications
SET user_id = (SELECT id FROM users ORDER BY id ASC LIMIT 1)
WHERE user_id IS NULL;

ALTER TABLE notifications
    ALTER COLUMN user_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND constraint_name = 'fk_notifications_users'
    ) THEN
        ALTER TABLE notifications
            ADD CONSTRAINT fk_notifications_users FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND constraint_name = 'fk_notifications_connections'
    ) THEN
        ALTER TABLE notifications
            ADD CONSTRAINT fk_notifications_connections FOREIGN KEY (connection_id) REFERENCES connections(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
    ON notifications(user_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_read
    ON notifications(user_id, is_read)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_source
    ON notifications(user_id, source)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_connection
    ON notifications(user_id, connection_id)
    WHERE deleted_at IS NULL;
