ALTER TABLE public.question_bank
    RENAME COLUMN difficulty TO legacy_numeric_difficulty;

ALTER TABLE public.question_bank
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(16);

UPDATE public.question_bank
SET difficulty = COALESCE(
        difficulty,
        difficulty_band,
        CASE
            WHEN legacy_numeric_difficulty IS NULL THEN NULL
            WHEN legacy_numeric_difficulty <= 2 THEN 'LOW'
            WHEN legacy_numeric_difficulty = 3 THEN 'MEDIUM'
            ELSE 'HIGH'
        END
    );

UPDATE public.question_bank
SET is_active = CASE
    WHEN question_pool_status IN ('QUARANTINED', 'RETIRED') THEN FALSE
    ELSE is_active
END
WHERE question_pool_status IS NOT NULL;

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_difficulty;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_difficulty
        CHECK (difficulty IS NULL OR difficulty IN ('LOW', 'MEDIUM', 'HIGH'));

CREATE INDEX IF NOT EXISTS idx_question_bank_mission_active_difficulty
    ON public.question_bank (mission_id, is_active, difficulty);

DROP VIEW IF EXISTS public.question_bank_safe;

CREATE VIEW public.question_bank_safe AS
SELECT
    id,
    mission_id,
    question_type,
    prompt,
    options_json AS options,
    content_tags,
    curriculum_ref,
    difficulty,
    source_type,
    generation_phase,
    created_at
FROM public.question_bank
WHERE is_active = TRUE;
