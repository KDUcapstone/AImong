CREATE INDEX IF NOT EXISTS idx_child_profiles_parent_active_created
    ON public.child_profiles(parent_id, created_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_mission_set_progress_child_star_completed
    ON public.mission_set_progress(child_id, star_level)
    WHERE completed = TRUE;

CREATE INDEX IF NOT EXISTS idx_mission_answer_results_child_attempt_created
    ON public.mission_answer_results(child_id, attempt_id, created_at);

CREATE INDEX IF NOT EXISTS idx_mission_answer_results_child_created
    ON public.mission_answer_results(child_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_date_passed_normal
    ON public.mission_attempts(child_id, attempt_date)
    WHERE is_review = FALSE AND is_passed = TRUE;
