ALTER TABLE users
    ADD COLUMN primary_email VARCHAR(255),
    ADD COLUMN display_name VARCHAR(100),
    ADD COLUMN status VARCHAR(30);

UPDATE users
SET primary_email = email
WHERE primary_email IS NULL;

UPDATE users
SET display_name = COALESCE(NULLIF(split_part(primary_email, '@', 1), ''), 'user-' || id)
WHERE display_name IS NULL;

UPDATE users
SET status = CASE
    WHEN deleted_at IS NULL THEN 'ACTIVE'
    ELSE 'DELETED'
END
WHERE status IS NULL;

ALTER TABLE users
    ALTER COLUMN primary_email SET NOT NULL,
    ALTER COLUMN display_name SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX idx_users_primary_email ON users(primary_email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;

CREATE TABLE auth_identities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255),
    email VARCHAR(255),
    password_hash VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_auth_identities_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_identities_user_id ON auth_identities(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_identities_email ON auth_identities(email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_auth_identities_local_email
    ON auth_identities(email)
    WHERE provider = 'LOCAL' AND deleted_at IS NULL;
CREATE UNIQUE INDEX uk_auth_identities_provider_user_id
    ON auth_identities(provider, provider_user_id)
    WHERE provider_user_id IS NOT NULL AND deleted_at IS NULL;

INSERT INTO auth_identities (
    user_id,
    provider,
    provider_user_id,
    email,
    password_hash,
    email_verified,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    id,
    'LOCAL',
    NULL,
    primary_email,
    password,
    TRUE,
    created_at,
    updated_at,
    deleted_at
FROM users
WHERE primary_email IS NOT NULL
  AND password IS NOT NULL;

DROP INDEX IF EXISTS idx_users_email;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users
    DROP COLUMN password,
    DROP COLUMN email;

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    auth_identity_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMPTZ,
    CONSTRAINT fk_password_reset_tokens_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_password_reset_tokens_auth_identities FOREIGN KEY (auth_identity_id) REFERENCES auth_identities(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_password_reset_tokens_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_auth_identity_id ON password_reset_tokens(auth_identity_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at) WHERE used_at IS NULL;

CREATE TABLE auth_provider_states (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    state VARCHAR(255) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    redirect_uri VARCHAR(2048) NOT NULL,
    pkce_verifier VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_auth_provider_states_state ON auth_provider_states(state);
CREATE INDEX idx_auth_provider_states_provider ON auth_provider_states(provider);
CREATE INDEX idx_auth_provider_states_expires_at ON auth_provider_states(expires_at);
