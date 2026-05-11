ALTER TABLE public.quiz_attempts
    ADD COLUMN IF NOT EXISTS answered_question_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'mission_attempts'
          AND column_name = 'id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'mission_attempts'
          AND column_name = 'attempt_id'
    ) THEN
        ALTER TABLE public.mission_attempts RENAME COLUMN id TO attempt_id;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.quiz_attempts WHERE set_id IS NULL) THEN
        ALTER TABLE public.quiz_attempts ALTER COLUMN set_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.mission_attempts WHERE set_id IS NULL) THEN
        ALTER TABLE public.mission_attempts ALTER COLUMN set_id SET NOT NULL;
    END IF;
END $$;
