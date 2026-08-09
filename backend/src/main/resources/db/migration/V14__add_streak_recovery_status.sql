DO $$ BEGIN
    CREATE TYPE streak_status_enum AS ENUM ('ACTIVE', 'PROTECTED', 'RECOVERABLE', 'BROKEN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE public.streak_records
    ADD COLUMN IF NOT EXISTS status public.streak_status_enum NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS recovery_deadline_date DATE,
    ADD COLUMN IF NOT EXISTS recovery_base_days INT,
    ADD COLUMN IF NOT EXISTS last_shield_used_date DATE;

CREATE INDEX IF NOT EXISTS idx_streak_records_status_deadline
    ON public.streak_records(status, recovery_deadline_date)
    WHERE status = 'RECOVERABLE';
