CREATE TABLE IF NOT EXISTS public.mission_daily_progress_v7 (
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    progress_date DATE NOT NULL,
    mission_id UUID REFERENCES public.missions(id) ON DELETE SET NULL,
    first_attempt_at TIMESTAMPTZ NOT NULL,
    completed_set_count INT NOT NULL DEFAULT 0 CHECK (completed_set_count >= 0),
    total_xp_earned INT NOT NULL DEFAULT 0 CHECK (total_xp_earned >= 0),
    best_score INT NOT NULL DEFAULT 0 CHECK (best_score >= 0),
    total INT NOT NULL DEFAULT 10 CHECK (total > 0),
    first_xp_earned INT NOT NULL DEFAULT 0 CHECK (first_xp_earned >= 0),
    review_attempt_count INT NOT NULL DEFAULT 0 CHECK (review_attempt_count >= 0),
    PRIMARY KEY (child_id, progress_date)
);

INSERT INTO public.mission_daily_progress_v7 (
    child_id,
    progress_date,
    mission_id,
    first_attempt_at,
    completed_set_count,
    total_xp_earned,
    best_score,
    total,
    first_xp_earned,
    review_attempt_count
)
SELECT
    child_id,
    progress_date,
    MIN(mission_id::TEXT)::UUID,
    MIN(first_attempt_at),
    COUNT(*),
    COALESCE(SUM(first_xp_earned), 0),
    MAX(best_score),
    MAX(total),
    COALESCE(SUM(first_xp_earned), 0),
    COALESCE(SUM(review_attempt_count), 0)
FROM public.mission_daily_progress
GROUP BY child_id, progress_date
ON CONFLICT (child_id, progress_date) DO UPDATE SET
    mission_id = EXCLUDED.mission_id,
    first_attempt_at = EXCLUDED.first_attempt_at,
    completed_set_count = EXCLUDED.completed_set_count,
    total_xp_earned = EXCLUDED.total_xp_earned,
    best_score = EXCLUDED.best_score,
    total = EXCLUDED.total,
    first_xp_earned = EXCLUDED.first_xp_earned,
    review_attempt_count = EXCLUDED.review_attempt_count;

DROP TABLE public.mission_daily_progress;
ALTER TABLE public.mission_daily_progress_v7 RENAME TO mission_daily_progress;

ALTER TABLE public.mission_daily_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_mission_daily_progress_all ON public.mission_daily_progress;
CREATE POLICY backend_mission_daily_progress_all ON public.mission_daily_progress FOR ALL USING (TRUE) WITH CHECK (TRUE);
