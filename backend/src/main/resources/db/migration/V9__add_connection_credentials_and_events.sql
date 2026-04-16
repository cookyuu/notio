CREATE INDEX IF NOT EXISTS idx_connections_provider
    ON connections(provider)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_connections_status
    ON connections(status)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_connections_external_account
    ON connections(external_account_id)
    WHERE deleted_at IS NULL AND external_account_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_connections_external_workspace
    ON connections(external_workspace_id)
    WHERE deleted_at IS NULL AND external_workspace_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_connections_subscription
    ON connections(subscription_id)
    WHERE deleted_at IS NULL AND subscription_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS connection_credentials (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    auth_type VARCHAR(50) NOT NULL,
    key_prefix VARCHAR(64),
    key_preview VARCHAR(64),
    key_hash VARCHAR(128),
    access_token_encrypted TEXT,
    refresh_token_encrypted TEXT,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_connection_credentials_connections FOREIGN KEY (connection_id) REFERENCES connections(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_connection_credentials_active_key_prefix
    ON connection_credentials(key_prefix)
    WHERE revoked_at IS NULL AND deleted_at IS NULL AND key_prefix IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_connection_credentials_connection_id
    ON connection_credentials(connection_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_connection_credentials_auth_type
    ON connection_credentials(auth_type)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS connection_events (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_connection_events_connections FOREIGN KEY (connection_id) REFERENCES connections(id),
    CONSTRAINT fk_connection_events_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_connection_events_connection_created_at
    ON connection_events(connection_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_connection_events_user_created_at
    ON connection_events(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_connection_events_type
    ON connection_events(event_type, created_at DESC);
