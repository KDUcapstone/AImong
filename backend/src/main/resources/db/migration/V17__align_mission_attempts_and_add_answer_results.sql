ALTER TABLE public.mission_attempts
    ADD COLUMN IF NOT EXISTS is_review BOOLEAN,
    ADD COLUMN IF NOT EXISTS is_passed BOOLEAN,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE public.mission_attempts
SET is_passed = COALESCE(is_passed, score * 10 >= total * 8)
WHERE is_passed IS NULL;

ALTER TABLE public.mission_attempts
    ALTER COLUMN is_passed SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_completed
    ON public.mission_attempts (child_id, mission_id, created_at DESC, attempt_date DESC)
    WHERE is_review = FALSE AND is_passed = TRUE;

CREATE TABLE IF NOT EXISTS public.mission_answer_results (
    result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES public.mission_attempts(id) ON DELETE CASCADE,
    child_id UUID NOT NULL REFERENCES public.child_profiles(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES public.question_bank(id) ON DELETE CASCADE,
    is_review BOOLEAN NOT NULL DEFAULT FALSE,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (attempt_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_mission_answer_results_attempt
    ON public.mission_answer_results (attempt_id);

CREATE INDEX IF NOT EXISTS idx_mission_answer_results_child_mission
    ON public.mission_answer_results (child_id, mission_id, created_at DESC);
