CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_chat_messages_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_chat_messages_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_messages_user_id_created_at
    ON chat_messages(user_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE notification_embeddings (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(768) NOT NULL,
    embedded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_notification_embeddings_notifications FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_notification_embeddings_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX idx_notification_embeddings_notification_content_hash
    ON notification_embeddings(notification_id, content_hash)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notification_embeddings_user_source_embedded_at
    ON notification_embeddings(user_id, source, embedded_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notification_embeddings_user_embedded_at
    ON notification_embeddings(user_id, embedded_at DESC)
    WHERE deleted_at IS NULL;

-- Keep exact pgvector scans for Phase 0. IVFFlat/HNSW indexes should be added
-- after representative data exists and recall/performance can be measured.
