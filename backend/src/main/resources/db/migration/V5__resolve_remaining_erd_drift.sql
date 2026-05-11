DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'mission_set_progress'
          AND column_name = 'first_attempt_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'mission_set_progress'
          AND column_name = 'first_passed_attempt_id'
    ) THEN
        ALTER TABLE public.mission_set_progress RENAME COLUMN first_attempt_id TO first_passed_attempt_id;
    END IF;
END $$;

ALTER TABLE public.mission_set_progress
    ADD COLUMN IF NOT EXISTS mission_id UUID,
    ADD COLUMN IF NOT EXISTS stage SMALLINT;

UPDATE public.mission_set_progress progress
SET mission_id = COALESCE(progress.mission_id, mission_set.mission_id),
    stage = COALESCE(progress.stage, mission_set.stage)
FROM public.mission_sets mission_set
WHERE progress.set_id = mission_set.set_id
  AND (progress.mission_id IS NULL OR progress.stage IS NULL);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'mission_set_progress_mission_id_fkey'
    ) THEN
        ALTER TABLE public.mission_set_progress
            ADD CONSTRAINT mission_set_progress_mission_id_fkey
                FOREIGN KEY (mission_id) REFERENCES public.missions(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_mission_set_progress_stage'
    ) THEN
        ALTER TABLE public.mission_set_progress
            ADD CONSTRAINT chk_mission_set_progress_stage CHECK (stage BETWEEN 1 AND 3) NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.mission_set_progress WHERE mission_id IS NULL) THEN
        ALTER TABLE public.mission_set_progress ALTER COLUMN mission_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.mission_set_progress WHERE stage IS NULL) THEN
        ALTER TABLE public.mission_set_progress ALTER COLUMN stage SET NOT NULL;
    END IF;
END $$;

UPDATE public.quiz_attempts quiz_attempt
SET set_id = (
    SELECT candidate.set_id
    FROM public.mission_sets candidate
    WHERE candidate.mission_id = quiz_attempt.mission_id
    ORDER BY candidate.stage, candidate.star_level, candidate.variant_no, candidate.set_id
    LIMIT 1
)
WHERE quiz_attempt.set_id IS NULL;

UPDATE public.mission_attempts mission_attempt
SET set_id = (
    SELECT candidate.set_id
    FROM public.mission_sets candidate
    WHERE candidate.mission_id = mission_attempt.mission_id
    ORDER BY candidate.stage, candidate.star_level, candidate.variant_no, candidate.set_id
    LIMIT 1
)
WHERE mission_attempt.set_id IS NULL;

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

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_mission_attempts_score_lte_total'
    ) THEN
        ALTER TABLE public.mission_attempts
            ADD CONSTRAINT chk_mission_attempts_score_lte_total CHECK (score <= total) NOT VALID;
    END IF;
END $$;

ALTER TABLE public.streak_records
    ADD COLUMN IF NOT EXISTS shield_count INT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_streak_records_shield_count'
    ) THEN
        ALTER TABLE public.streak_records
            ADD CONSTRAINT chk_streak_records_shield_count CHECK (shield_count >= 0) NOT VALID;
    END IF;
END $$;
