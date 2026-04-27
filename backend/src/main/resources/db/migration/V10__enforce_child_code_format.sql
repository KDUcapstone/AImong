DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_child_profiles_code_digits'
    ) THEN
        ALTER TABLE child_profiles
            ADD CONSTRAINT chk_child_profiles_code_digits
            CHECK (code ~ '^[0-9]{6}$');
    END IF;
END $$;
