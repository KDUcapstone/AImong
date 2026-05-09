ALTER TABLE public.mission_attempts
    ADD COLUMN IF NOT EXISTS level_no SMALLINT;

UPDATE public.mission_attempts ma
SET level_no = ms.level_no
FROM public.mission_sets ms
WHERE ma.set_id = ms.set_id
  AND ma.level_no IS NULL;

UPDATE public.mission_attempts
SET level_no = 1
WHERE level_no IS NULL;

ALTER TABLE public.mission_attempts
    ADD CONSTRAINT mission_attempts_level_no_check CHECK (level_no BETWEEN 1 AND 6);

CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_level_date
    ON public.mission_attempts(child_id, level_no, attempt_date);
