CREATE TABLE IF NOT EXISTS parent_accounts (
    id UUID PRIMARY KEY,
    firebase_uid TEXT NOT NULL UNIQUE,
    email TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS child_profiles (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL REFERENCES parent_accounts(id) ON DELETE CASCADE,
    nickname TEXT NOT NULL,
    code VARCHAR(6) NOT NULL UNIQUE,
    starter_issued BOOLEAN NOT NULL DEFAULT FALSE,
    total_xp INT NOT NULL DEFAULT 0,
    today_xp INT NOT NULL DEFAULT 0,
    weekly_xp INT NOT NULL DEFAULT 0,
    gacha_pull_count INT NOT NULL DEFAULT 0,
    sr_miss_count INT NOT NULL DEFAULT 0,
    profile_image_type TEXT NOT NULL DEFAULT 'DEFAULT',
    session_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_child_profiles_parent_id
    ON child_profiles (parent_id);

CREATE TABLE IF NOT EXISTS tickets (
    child_id UUID PRIMARY KEY REFERENCES child_profiles(id) ON DELETE CASCADE,
    normal INT NOT NULL DEFAULT 0,
    rare INT NOT NULL DEFAULT 0,
    epic INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS streak_records (
    child_id UUID PRIMARY KEY REFERENCES child_profiles(id) ON DELETE CASCADE,
    continuous_days INT NOT NULL DEFAULT 0,
    last_completed_date DATE,
    today_mission_count INT NOT NULL DEFAULT 0,
    shield_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
