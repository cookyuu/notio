-- Fix notifications table to match API specification
-- 1. Rename content -> body
ALTER TABLE notifications RENAME COLUMN content TO body;

-- 2. Fix title length (500 -> 255)
ALTER TABLE notifications ALTER COLUMN title TYPE VARCHAR(255);

-- 3. Change priority from INTEGER to VARCHAR(50) enum
ALTER TABLE notifications DROP COLUMN priority;
ALTER TABLE notifications ADD COLUMN priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM';

-- 4. Make user_id nullable for Phase 0 (single user MVP)
ALTER TABLE notifications ALTER COLUMN user_id DROP NOT NULL;

-- 5. Rename url -> external_url and adjust length
ALTER TABLE notifications RENAME COLUMN url TO external_url;

-- 6. Add missing columns
ALTER TABLE notifications ADD COLUMN external_id VARCHAR(255);
ALTER TABLE notifications ADD COLUMN metadata JSONB;

-- 7. Update body length constraint
ALTER TABLE notifications ALTER COLUMN body TYPE VARCHAR(2000);

-- Create index on external_id
CREATE INDEX idx_notifications_external_id ON notifications(external_id) WHERE deleted_at IS NULL;

-- Fix todos table
-- 1. Fix title length (500 -> 255)
ALTER TABLE todos ALTER COLUMN title TYPE VARCHAR(255);

-- 2. Change status default from 'TODO' to 'PENDING'
ALTER TABLE todos ALTER COLUMN status SET DEFAULT 'PENDING';

-- 3. Make user_id nullable for Phase 0
ALTER TABLE todos ALTER COLUMN user_id DROP NOT NULL;

-- Fix devices table to match API specification
-- 1. Rename device_token -> fcm_token
ALTER TABLE devices RENAME COLUMN device_token TO fcm_token;

-- 2. Rename device_type -> platform
ALTER TABLE devices RENAME COLUMN device_type TO platform;

-- 3. Add missing columns
ALTER TABLE devices ADD COLUMN device_id VARCHAR(255) NOT NULL;
ALTER TABLE devices ADD COLUMN app_version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE devices ADD COLUMN os_version VARCHAR(50) NOT NULL DEFAULT 'unknown';
ALTER TABLE devices ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 4. Make user_id nullable for Phase 0
ALTER TABLE devices ALTER COLUMN user_id DROP NOT NULL;

-- Update indexes for devices
DROP INDEX IF EXISTS idx_devices_device_token;
CREATE INDEX idx_devices_fcm_token ON devices(fcm_token) WHERE deleted_at IS NULL;
CREATE INDEX idx_devices_device_id ON devices(device_id) WHERE deleted_at IS NULL;

-- Fix chat_messages table
-- Make user_id nullable for Phase 0
ALTER TABLE chat_messages ALTER COLUMN user_id DROP NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN notifications.body IS 'Notification message body (max 2000 chars)';
COMMENT ON COLUMN notifications.priority IS 'Priority level: URGENT, HIGH, MEDIUM, LOW';
COMMENT ON COLUMN notifications.external_id IS 'External service ID (e.g., Slack message ID)';
COMMENT ON COLUMN notifications.external_url IS 'External service URL (e.g., Slack message link)';
COMMENT ON COLUMN notifications.metadata IS 'Additional metadata in JSON format';
COMMENT ON COLUMN todos.status IS 'Todo status: PENDING, IN_PROGRESS, DONE';
COMMENT ON COLUMN devices.platform IS 'Device platform: ANDROID, IOS';
