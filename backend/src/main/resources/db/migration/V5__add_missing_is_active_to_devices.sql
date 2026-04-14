DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'devices'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'devices'
          AND column_name = 'is_active'
    ) THEN
        ALTER TABLE devices
            ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_devices_is_active ON devices(is_active);
