CREATE TABLE IF NOT EXISTS public.login_attempt_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type TEXT NOT NULL CHECK (target_type IN ('IP', 'CODE')),
    target_value TEXT NOT NULL,
    failure_count INT NOT NULL DEFAULT 0 CHECK (failure_count >= 0),
    window_expires_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (target_type, target_value)
);

CREATE INDEX IF NOT EXISTS idx_login_attempt_limits_expiry
    ON public.login_attempt_limits (window_expires_at, locked_until);

ALTER TABLE public.login_attempt_limits ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_login_attempt_limits_all ON public.login_attempt_limits;
CREATE POLICY backend_login_attempt_limits_all ON public.login_attempt_limits
    FOR ALL USING (TRUE) WITH CHECK (TRUE);
