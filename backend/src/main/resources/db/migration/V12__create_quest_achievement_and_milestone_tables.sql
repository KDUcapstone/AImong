DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'daily_quest_type_enum') THEN
        CREATE TYPE daily_quest_type_enum AS ENUM ('MISSION_1', 'XP_20', 'CHAT_GPT', 'ALL_3');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'weekly_quest_type_enum') THEN
        CREATE TYPE weekly_quest_type_enum AS ENUM ('XP_100', 'MISSION_5', 'CHAT_3');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'achievement_type_enum') THEN
        CREATE TYPE achievement_type_enum AS ENUM ('SPROUT', 'EXPLORER', 'CRITIC', 'GUARDIAN');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS daily_quests (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    quest_date DATE NOT NULL,
    quest_type daily_quest_type_enum NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    UNIQUE (child_id, quest_date, quest_type),
    CHECK ((completed = FALSE AND completed_at IS NULL) OR (completed = TRUE AND completed_at IS NOT NULL)),
    CHECK (reward_claimed = FALSE OR completed = TRUE)
);

CREATE TABLE IF NOT EXISTS weekly_quests (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    quest_type weekly_quest_type_enum NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    UNIQUE (child_id, week_start, quest_type),
    CHECK ((completed = FALSE AND completed_at IS NULL) OR (completed = TRUE AND completed_at IS NOT NULL)),
    CHECK (reward_claimed = FALSE OR completed = TRUE)
);

CREATE TABLE IF NOT EXISTS achievements (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    achievement_type achievement_type_enum NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, achievement_type)
);

CREATE TABLE IF NOT EXISTS milestone_rewards (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    milestone_days SMALLINT NOT NULL CHECK (milestone_days IN (7, 30)),
    rewarded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, milestone_days)
);
