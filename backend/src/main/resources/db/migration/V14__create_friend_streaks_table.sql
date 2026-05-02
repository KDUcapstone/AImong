CREATE TABLE IF NOT EXISTS friend_streaks (
    child_id UUID PRIMARY KEY REFERENCES child_profiles(id) ON DELETE CASCADE,
    partner_child_id UUID NOT NULL UNIQUE REFERENCES child_profiles(id) ON DELETE CASCADE,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (child_id <> partner_child_id)
);
