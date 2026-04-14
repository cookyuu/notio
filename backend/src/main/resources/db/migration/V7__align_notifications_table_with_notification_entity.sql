DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
    ) THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'content'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'body'
    ) THEN
        ALTER TABLE notifications RENAME COLUMN content TO body;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'url'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'external_url'
    ) THEN
        ALTER TABLE notifications RENAME COLUMN url TO external_url;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'user_id'
    ) THEN
        ALTER TABLE notifications ADD COLUMN user_id BIGINT;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'external_id'
    ) THEN
        ALTER TABLE notifications ADD COLUMN external_id VARCHAR(255);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'metadata'
    ) THEN
        ALTER TABLE notifications ADD COLUMN metadata JSONB;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'title'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN title TYPE VARCHAR(255);
        ALTER TABLE notifications ALTER COLUMN title SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'body'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN body TYPE VARCHAR(2000);
        ALTER TABLE notifications ALTER COLUMN body SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'priority'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN priority TYPE VARCHAR(50);
        ALTER TABLE notifications ALTER COLUMN priority SET DEFAULT 'MEDIUM';
        ALTER TABLE notifications ALTER COLUMN priority SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'source'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN source TYPE VARCHAR(50);
        ALTER TABLE notifications ALTER COLUMN source SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'is_read'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN is_read SET DEFAULT FALSE;
        ALTER TABLE notifications ALTER COLUMN is_read SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'notifications'
          AND column_name = 'external_url'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN external_url TYPE VARCHAR(500);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_notifications_source ON notifications(source);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_external_id ON notifications(external_id);
