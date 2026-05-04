ALTER TABLE public.daily_quest_progress
    ADD COLUMN IF NOT EXISTS current_value INT NOT NULL DEFAULT 0 CHECK (current_value >= 0);

ALTER TABLE public.weekly_quest_progress
    ADD COLUMN IF NOT EXISTS current_value INT NOT NULL DEFAULT 0 CHECK (current_value >= 0);
