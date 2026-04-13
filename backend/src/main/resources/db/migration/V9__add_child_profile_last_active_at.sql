ALTER TABLE child_profiles
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ;
