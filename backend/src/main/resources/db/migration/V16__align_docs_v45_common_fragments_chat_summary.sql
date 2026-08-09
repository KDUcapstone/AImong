CREATE TEMP TABLE tmp_pet_fragment_rollup ON COMMIT DROP AS
SELECT
    child_id,
    MIN(id::TEXT)::UUID AS keep_id,
    SUM(count)::INT AS total_count,
    MAX(updated_at) AS latest_updated_at
FROM public.pet_fragments
GROUP BY child_id;

UPDATE public.pet_fragments pf
SET
    count = rollup.total_count,
    updated_at = COALESCE(rollup.latest_updated_at, NOW())
FROM tmp_pet_fragment_rollup rollup
WHERE pf.id = rollup.keep_id;

DELETE FROM public.pet_fragments pf
USING tmp_pet_fragment_rollup rollup
WHERE pf.child_id = rollup.child_id
  AND pf.id <> rollup.keep_id;

ALTER TABLE public.pet_fragments
    DROP CONSTRAINT IF EXISTS uq_pet_fragments_child_grade,
    DROP CONSTRAINT IF EXISTS pet_fragments_child_id_grade_key,
    DROP CONSTRAINT IF EXISTS pet_fragments_child_id_key,
    DROP CONSTRAINT IF EXISTS uq_pet_fragments_child;

ALTER TABLE public.pet_fragments
    DROP COLUMN IF EXISTS grade;

ALTER TABLE public.pet_fragments
    ADD CONSTRAINT uq_pet_fragments_child UNIQUE (child_id);

ALTER TABLE public.chat_sessions
    ADD COLUMN IF NOT EXISTS summary TEXT;
