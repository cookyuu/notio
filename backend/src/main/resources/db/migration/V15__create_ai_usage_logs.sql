-- ai_usage_logs: Claude API 호출에 대한 토큰 사용량 및 세션 기록 테이블
-- total_tokens는 input_tokens + output_tokens의 GENERATED 컬럼으로 삽입/수정 불가
CREATE TABLE ai_usage_logs (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    notification_id BIGINT          NOT NULL,
    model           VARCHAR(100)    NOT NULL,
    input_tokens    BIGINT          NOT NULL DEFAULT 0,
    output_tokens   BIGINT          NOT NULL DEFAULT 0,
    total_tokens    BIGINT          GENERATED ALWAYS AS (input_tokens + output_tokens) STORED,
    session_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT fk_ai_usage_logs_users
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_usage_logs_notifications
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE CASCADE
);

-- 사용자별 세션 시간순 조회 인덱스 (Trend API용)
CREATE INDEX idx_ai_usage_logs_user_session_at
    ON ai_usage_logs(user_id, session_at DESC)
    WHERE deleted_at IS NULL;

-- 사용자별 모델 분포 조회 인덱스 (ModelDistribution API용)
CREATE INDEX idx_ai_usage_logs_model
    ON ai_usage_logs(user_id, model)
    WHERE deleted_at IS NULL;

-- notification_id 중복 방지 인덱스 (idempotency 보장)
CREATE UNIQUE INDEX uq_ai_usage_logs_notification_id
    ON ai_usage_logs(notification_id)
    WHERE deleted_at IS NULL;
