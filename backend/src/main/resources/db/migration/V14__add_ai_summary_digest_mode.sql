-- 1. notifications.ai_summary 추가
ALTER TABLE notifications ADD COLUMN ai_summary TEXT;
COMMENT ON COLUMN notifications.ai_summary IS
  'LLM-generated concise summary for channel delivery. '
  'NULL when summarization is disabled/failed. '
  'ChannelRouter uses COALESCE(ai_summary, body).';

-- 2. routing_rules — Digest 모드 컬럼 추가
ALTER TABLE routing_rules
    ADD COLUMN delivery_mode       VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE',
    ADD COLUMN digest_interval_min INT NULL;

ALTER TABLE routing_rules
    ADD CONSTRAINT chk_delivery_mode
        CHECK (delivery_mode IN ('IMMEDIATE', 'DIGEST'));

-- 3. channel_delivery_logs — DIGEST_PENDING 상태 추가
ALTER TABLE channel_delivery_logs DROP CONSTRAINT chk_delivery_status;
ALTER TABLE channel_delivery_logs
    ADD CONSTRAINT chk_delivery_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRY', 'DEAD', 'DIGEST_PENDING'));

-- UNIQUE 제약 재생성 (DIGEST_PENDING 포함)
DROP INDEX uq_delivery_logs_active;
CREATE UNIQUE INDEX uq_delivery_logs_active
    ON channel_delivery_logs(notification_id, channel_id)
    WHERE status IN ('PENDING', 'RETRY', 'DIGEST_PENDING');

-- Digest 스케줄러 인덱스
CREATE INDEX idx_delivery_logs_digest_pending
    ON channel_delivery_logs(channel_id, next_retry_at)
    WHERE status = 'DIGEST_PENDING';

-- Delivery Feed API 인덱스
CREATE INDEX idx_delivery_logs_delivered_at
    ON channel_delivery_logs(delivered_at DESC)
    WHERE status = 'SUCCESS';

-- 4. chat_messages 폐기
ALTER TABLE chat_messages RENAME TO chat_messages_deprecated;
COMMENT ON TABLE chat_messages_deprecated IS
  'Deprecated: AI interactive chat history. Scheduled drop V15 after 2026-08-12.';
