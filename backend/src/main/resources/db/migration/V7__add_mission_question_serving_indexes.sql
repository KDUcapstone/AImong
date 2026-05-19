CREATE INDEX IF NOT EXISTS idx_question_bank_serving_mission_difficulty_status
    ON public.question_bank(mission_id, difficulty, question_pool_status, is_active);

CREATE INDEX IF NOT EXISTS idx_mission_answer_results_child_mission_review_question
    ON public.mission_answer_results(child_id, mission_id, is_review, question_id);

CREATE INDEX IF NOT EXISTS idx_mission_set_progress_child_completed_set
    ON public.mission_set_progress(child_id, completed, set_id);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_child_mission_status_expires_created
    ON public.quiz_attempts(child_id, mission_id, status, expires_at DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_child_set_status_expires_created
    ON public.quiz_attempts(child_id, set_id, status, expires_at DESC, created_at DESC);
