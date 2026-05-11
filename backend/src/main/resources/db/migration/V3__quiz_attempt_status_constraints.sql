DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_quiz_attempts_submitted_consistency'
          AND conrelid = 'public.quiz_attempts'::regclass
    ) THEN
        ALTER TABLE public.quiz_attempts
            ADD CONSTRAINT chk_quiz_attempts_submitted_consistency
            CHECK ((status = 'SUBMITTED') = (submitted_at IS NOT NULL));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_quiz_attempts_abandoned_consistency'
          AND conrelid = 'public.quiz_attempts'::regclass
    ) THEN
        ALTER TABLE public.quiz_attempts
            ADD CONSTRAINT chk_quiz_attempts_abandoned_consistency
            CHECK ((status = 'ABANDONED') = (abandoned_at IS NOT NULL));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_quiz_attempts_abandoned_after_created'
          AND conrelid = 'public.quiz_attempts'::regclass
    ) THEN
        ALTER TABLE public.quiz_attempts
            ADD CONSTRAINT chk_quiz_attempts_abandoned_after_created
            CHECK (abandoned_at IS NULL OR abandoned_at >= created_at);
    END IF;
END $$;
