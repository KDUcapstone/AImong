ALTER TABLE child_profiles
    ADD COLUMN IF NOT EXISTS today_xp_date DATE;

ALTER TABLE child_profiles
    ADD COLUMN IF NOT EXISTS weekly_xp_week_start DATE;
