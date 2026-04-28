ALTER TABLE public.question_bank
    ADD COLUMN IF NOT EXISTS content_tags JSONB,
    ADD COLUMN IF NOT EXISTS curriculum_ref TEXT,
    ADD COLUMN IF NOT EXISTS difficulty SMALLINT,
    ADD COLUMN IF NOT EXISTS generation_phase VARCHAR(20),
    ADD COLUMN IF NOT EXISTS pack_no SMALLINT,
    ADD COLUMN IF NOT EXISTS difficulty_band VARCHAR(16),
    ADD COLUMN IF NOT EXISTS question_pool_status VARCHAR(16);

UPDATE public.question_bank
SET generation_phase = COALESCE(generation_phase, 'PREGENERATED'),
    question_pool_status = COALESCE(question_pool_status, 'ACTIVE')
WHERE generation_phase IS NULL
   OR question_pool_status IS NULL;

ALTER TABLE public.question_bank
    ALTER COLUMN generation_phase SET DEFAULT 'PREGENERATED',
    ALTER COLUMN question_pool_status SET DEFAULT 'ACTIVE';

ALTER TABLE public.question_bank
    ALTER COLUMN generation_phase SET NOT NULL,
    ALTER COLUMN question_pool_status SET NOT NULL;

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_generation_phase;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_generation_phase
        CHECK (generation_phase IN ('PREGENERATED', 'RUNTIME'));

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_difficulty_band;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_difficulty_band
        CHECK (difficulty_band IS NULL OR difficulty_band IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_pool_status;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_pool_status
        CHECK (question_pool_status IN ('ACTIVE', 'RETIRED'));

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_difficulty;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_difficulty
        CHECK (difficulty IS NULL OR difficulty BETWEEN 1 AND 4);

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_pack_no;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_pack_no
        CHECK (pack_no IS NULL OR pack_no BETWEEN 1 AND 6);

CREATE INDEX IF NOT EXISTS idx_question_bank_mission_active_pack
    ON public.question_bank (mission_id, is_active, pack_no);

CREATE INDEX IF NOT EXISTS idx_question_bank_mission_active_band
    ON public.question_bank (mission_id, is_active, difficulty_band);

CREATE OR REPLACE VIEW public.question_bank_safe AS
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
