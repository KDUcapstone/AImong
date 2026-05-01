CREATE TABLE IF NOT EXISTS streak_milestones (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    target_days SMALLINT NOT NULL CHECK (target_days > 30),
    tier SMALLINT NOT NULL CHECK (tier IN (1, 2, 3)),
    achieved BOOLEAN NOT NULL DEFAULT FALSE,
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    achieved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, target_days),
    CHECK (reward_claimed = FALSE OR achieved = TRUE),
    CHECK ((achieved = FALSE AND achieved_at IS NULL) OR (achieved = TRUE AND achieved_at IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_streak_milestones_child
    ON streak_milestones(child_id);
