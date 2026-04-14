ALTER TABLE parent_accounts
    ADD COLUMN IF NOT EXISTS fcm_token TEXT;

ALTER TABLE child_profiles
    ADD COLUMN IF NOT EXISTS fcm_token TEXT;
