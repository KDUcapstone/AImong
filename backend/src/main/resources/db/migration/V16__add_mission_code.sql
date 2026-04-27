ALTER TABLE public.missions
    ADD COLUMN IF NOT EXISTS mission_code VARCHAR(16);

CREATE UNIQUE INDEX IF NOT EXISTS uk_missions_mission_code
    ON public.missions (mission_code)
    WHERE mission_code IS NOT NULL;
