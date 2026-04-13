CREATE TABLE IF NOT EXISTS mission_daily_progress (
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    progress_date DATE NOT NULL,
    first_attempt_at TIMESTAMPTZ NOT NULL,
    best_score INT NOT NULL CHECK (best_score >= 0),
    total INT NOT NULL CHECK (total > 0),
    first_xp_earned INT NOT NULL CHECK (first_xp_earned >= 0),
    review_attempt_count INT NOT NULL DEFAULT 0 CHECK (review_attempt_count >= 0),
    PRIMARY KEY (child_id, mission_id, progress_date),
    CHECK (best_score <= total)
);
