ALTER TABLE public.mission_set_progress
    ADD COLUMN IF NOT EXISTS completed BOOLEAN,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE public.mission_set_progress
SET completed = TRUE
WHERE completed IS NULL;

UPDATE public.mission_set_progress
SET updated_at = COALESCE(updated_at, completed_at, NOW())
WHERE updated_at IS NULL;

ALTER TABLE public.mission_set_progress
    ALTER COLUMN completed SET DEFAULT FALSE,
    ALTER COLUMN completed SET NOT NULL,
    ALTER COLUMN completed_at DROP NOT NULL,
    ALTER COLUMN best_score DROP NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_quiz_attempts_submitted_after_created'
          AND conrelid = 'public.quiz_attempts'::regclass
    ) THEN
        ALTER TABLE public.quiz_attempts
            ADD CONSTRAINT chk_quiz_attempts_submitted_after_created
            CHECK (submitted_at IS NULL OR submitted_at >= created_at) NOT VALID;
    END IF;
END $$;
