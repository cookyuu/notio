DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'devices'
    ) THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'token'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'fcm_token'
    ) THEN
        ALTER TABLE devices RENAME COLUMN token TO fcm_token;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'device_type'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'platform'
    ) THEN
        ALTER TABLE devices RENAME COLUMN device_type TO platform;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'user_id'
    ) THEN
        ALTER TABLE devices ADD COLUMN user_id BIGINT;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'app_version'
    ) THEN
        ALTER TABLE devices ADD COLUMN app_version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'os_version'
    ) THEN
        ALTER TABLE devices ADD COLUMN os_version VARCHAR(50) NOT NULL DEFAULT 'unknown';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'device_id'
    ) THEN
        ALTER TABLE devices ADD COLUMN device_id VARCHAR(255);
        UPDATE devices
        SET device_id = COALESCE(NULLIF(device_name, ''), 'legacy-device-' || id)
        WHERE device_id IS NULL;
        ALTER TABLE devices ALTER COLUMN device_id SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'fcm_token'
    ) THEN
        ALTER TABLE devices ALTER COLUMN fcm_token TYPE VARCHAR(500);
        ALTER TABLE devices ALTER COLUMN fcm_token SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'platform'
    ) THEN
        ALTER TABLE devices ALTER COLUMN platform TYPE VARCHAR(20);
        ALTER TABLE devices ALTER COLUMN platform SET NOT NULL;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_devices_fcm_token ON devices(fcm_token);
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
