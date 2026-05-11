ALTER TABLE public.parent_accounts
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS public.parent_notification_settings (
    parent_id TEXT PRIMARY KEY REFERENCES public.parent_accounts(parent_id) ON DELETE CASCADE,
    privacy_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    study_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    return_reward_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quest_reward_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.child_profiles
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE public.child_profiles
    ALTER COLUMN code DROP NOT NULL;

ALTER TABLE public.child_profiles
    DROP CONSTRAINT IF EXISTS chk_child_profiles_code;

ALTER TABLE public.child_profiles
    ADD CONSTRAINT chk_child_profiles_code CHECK (code IS NULL OR code ~ '^[0-9]{6}$');

ALTER TABLE public.quiz_attempts
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN IF NOT EXISTS abandoned_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS abandon_reason VARCHAR(64);

UPDATE public.quiz_attempts
SET status = 'SUBMITTED'
WHERE submitted_at IS NOT NULL
  AND status = 'IN_PROGRESS';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_quiz_attempts_status'
          AND conrelid = 'public.quiz_attempts'::regclass
    ) THEN
        ALTER TABLE public.quiz_attempts
            ADD CONSTRAINT chk_quiz_attempts_status
            CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED', 'ABANDONED'));
    END IF;
END $$;

ALTER TABLE public.parent_notification_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_parent_notification_settings_all ON public.parent_notification_settings;
CREATE POLICY backend_parent_notification_settings_all
    ON public.parent_notification_settings
    FOR ALL
    USING (TRUE)
    WITH CHECK (TRUE);
