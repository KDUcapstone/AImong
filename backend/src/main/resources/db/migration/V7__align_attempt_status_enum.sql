DO $$
BEGIN
    CREATE TYPE public.attempt_status_enum AS ENUM ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED', 'ABANDONED');
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

ALTER TABLE public.quiz_attempts
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE public.quiz_attempts
    ALTER COLUMN status TYPE public.attempt_status_enum
    USING status::public.attempt_status_enum;

ALTER TABLE public.quiz_attempts
    ALTER COLUMN status SET DEFAULT 'IN_PROGRESS'::public.attempt_status_enum;

ALTER TABLE public.quiz_attempts
    DROP CONSTRAINT IF EXISTS chk_quiz_attempts_status;

ALTER TABLE public.mission_set_progress
    ALTER COLUMN best_score TYPE SMALLINT
    USING best_score::SMALLINT;
