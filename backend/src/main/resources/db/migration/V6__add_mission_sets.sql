CREATE TABLE IF NOT EXISTS public.mission_sets (
    set_id VARCHAR(32) PRIMARY KEY,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    mission_code VARCHAR(16) NOT NULL,
    level_no SMALLINT NOT NULL CHECK (level_no BETWEEN 1 AND 6),
    stage SMALLINT NOT NULL CHECK (stage BETWEEN 1 AND 3),
    difficulty difficulty_band_enum NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (mission_id, level_no)
);

INSERT INTO public.mission_sets (
    set_id,
    mission_id,
    mission_code,
    level_no,
    stage,
    difficulty,
    title,
    description,
    display_order,
    is_active
)
SELECT
    m.mission_code || '-L' || levels.level_no,
    m.id,
    m.mission_code,
    levels.level_no,
    CAST(SUBSTRING(m.mission_code FROM 2 FOR 2) AS SMALLINT),
    CASE
        WHEN levels.level_no <= 3 THEN CAST('LOW' AS difficulty_band_enum)
        WHEN levels.level_no <= 5 THEN CAST('MEDIUM' AS difficulty_band_enum)
        ELSE CAST('HIGH' AS difficulty_band_enum)
    END,
    m.title || ' L' || levels.level_no,
    m.description,
    (CAST(SUBSTRING(m.mission_code FROM 2 FOR 2) AS INT) * 1000)
        + (CAST(SUBSTRING(m.mission_code FROM 4 FOR 2) AS INT) * 10)
        + levels.level_no,
    m.is_active
FROM public.missions m
CROSS JOIN generate_series(1, 6) AS levels(level_no)
WHERE m.mission_code IS NOT NULL
ON CONFLICT (set_id) DO UPDATE SET
    mission_id = EXCLUDED.mission_id,
    mission_code = EXCLUDED.mission_code,
    level_no = EXCLUDED.level_no,
    stage = EXCLUDED.stage,
    difficulty = EXCLUDED.difficulty,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active;

ALTER TABLE public.question_bank
    ADD COLUMN IF NOT EXISTS set_id VARCHAR(32);

WITH ranked_questions AS (
    SELECT
        q.id,
        m.mission_code,
        q.difficulty,
        ROW_NUMBER() OVER (
            PARTITION BY q.mission_id, q.difficulty
            ORDER BY q.pack_no NULLS LAST, q.created_at, q.id
        ) AS difficulty_rank
    FROM public.question_bank q
    JOIN public.missions m ON m.id = q.mission_id
    WHERE m.mission_code IS NOT NULL
      AND q.difficulty IN (
          CAST('LOW' AS difficulty_band_enum),
          CAST('MEDIUM' AS difficulty_band_enum),
          CAST('HIGH' AS difficulty_band_enum)
      )
),
set_assignments AS (
    SELECT
        id,
        mission_code || '-L' ||
            CASE
                WHEN difficulty = CAST('LOW' AS difficulty_band_enum) THEN ((difficulty_rank - 1) / 10) + 1
                WHEN difficulty = CAST('MEDIUM' AS difficulty_band_enum) THEN ((difficulty_rank - 1) / 10) + 4
                ELSE 6
            END AS set_id
    FROM ranked_questions
)
UPDATE public.question_bank q
SET set_id = set_assignments.set_id
FROM set_assignments
WHERE q.id = set_assignments.id
  AND q.set_id IS NULL;

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS question_bank_set_id_fkey;
ALTER TABLE public.question_bank
    ADD CONSTRAINT question_bank_set_id_fkey
        FOREIGN KEY (set_id) REFERENCES public.mission_sets(set_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_question_bank_set_active
    ON public.question_bank(set_id, is_active, question_pool_status);

ALTER TABLE public.quiz_attempts
    ADD COLUMN IF NOT EXISTS set_id VARCHAR(32),
    ADD COLUMN IF NOT EXISTS level_no SMALLINT;

UPDATE public.quiz_attempts qa
SET set_id = ms.set_id,
    level_no = ms.level_no
FROM public.mission_sets ms
WHERE qa.mission_id = ms.mission_id
  AND qa.set_id IS NULL
  AND ms.level_no = 1;

ALTER TABLE public.quiz_attempts
    DROP CONSTRAINT IF EXISTS quiz_attempts_set_id_fkey;
ALTER TABLE public.quiz_attempts
    ADD CONSTRAINT quiz_attempts_set_id_fkey
        FOREIGN KEY (set_id) REFERENCES public.mission_sets(set_id);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_child_set
    ON public.quiz_attempts(child_id, set_id, created_at DESC);

ALTER TABLE public.mission_attempts
    ADD COLUMN IF NOT EXISTS set_id VARCHAR(32);

UPDATE public.mission_attempts ma
SET set_id = ms.set_id
FROM public.mission_sets ms
WHERE ma.mission_id = ms.mission_id
  AND ma.set_id IS NULL
  AND ms.level_no = 1;

ALTER TABLE public.mission_attempts
    DROP CONSTRAINT IF EXISTS mission_attempts_set_id_fkey;
ALTER TABLE public.mission_attempts
    ADD CONSTRAINT mission_attempts_set_id_fkey
        FOREIGN KEY (set_id) REFERENCES public.mission_sets(set_id);

CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_set_date
    ON public.mission_attempts(child_id, set_id, attempt_date);

ALTER TABLE public.mission_answer_results
    ADD COLUMN IF NOT EXISTS set_id VARCHAR(32);

UPDATE public.mission_answer_results r
SET set_id = ma.set_id
FROM public.mission_attempts ma
WHERE r.attempt_id = ma.id
  AND r.set_id IS NULL;

ALTER TABLE public.mission_answer_results
    DROP CONSTRAINT IF EXISTS mission_answer_results_set_id_fkey;
ALTER TABLE public.mission_answer_results
    ADD CONSTRAINT mission_answer_results_set_id_fkey
        FOREIGN KEY (set_id) REFERENCES public.mission_sets(set_id);

CREATE TABLE IF NOT EXISTS public.mission_set_progress (
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    set_id VARCHAR(32) NOT NULL REFERENCES public.mission_sets(set_id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    best_score INT NOT NULL CHECK (best_score >= 0),
    total INT NOT NULL CHECK (total > 0),
    first_attempt_id UUID REFERENCES public.mission_attempts(id) ON DELETE SET NULL,
    PRIMARY KEY (child_id, set_id)
);

INSERT INTO public.mission_set_progress (child_id, set_id, completed_at, best_score, total, first_attempt_id)
SELECT DISTINCT ON (ma.child_id, ma.set_id)
    ma.child_id,
    ma.set_id,
    ma.submitted_at,
    ma.score,
    ma.total,
    ma.id
FROM public.mission_attempts ma
WHERE ma.set_id IS NOT NULL
  AND ma.is_review = FALSE
  AND ma.is_passed = TRUE
ORDER BY ma.child_id, ma.set_id, ma.submitted_at ASC
ON CONFLICT (child_id, set_id) DO NOTHING;

ALTER TABLE public.mission_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mission_set_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_mission_sets_all ON public.mission_sets;
CREATE POLICY backend_mission_sets_all ON public.mission_sets FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_mission_set_progress_all ON public.mission_set_progress;
CREATE POLICY backend_mission_set_progress_all ON public.mission_set_progress FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP VIEW IF EXISTS public.question_bank_safe;

CREATE VIEW public.question_bank_safe AS
SELECT
    id,
    mission_id,
    set_id,
    question_type,
    prompt,
    options,
    content_tags,
    curriculum_ref,
    difficulty,
    legacy_numeric_difficulty,
    source_type,
    generation_phase,
    pack_no,
    difficulty_band,
    question_pool_status,
    created_at,
    is_active
FROM public.question_bank
WHERE is_active = TRUE
  AND question_pool_status = 'ACTIVE';
