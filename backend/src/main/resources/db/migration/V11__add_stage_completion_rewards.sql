DO $$ BEGIN
    ALTER TYPE currency_transaction_reason_enum ADD VALUE IF NOT EXISTS 'STAGE_REWARD_GEAR';
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS public.stage_completion_rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id TEXT NOT NULL REFERENCES public.parent_accounts(parent_id) ON DELETE CASCADE,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    stage_number SMALLINT NOT NULL CHECK (stage_number IN (1, 2, 3)),
    reward_text VARCHAR(100),
    is_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, stage_number),
    CHECK ((is_triggered = FALSE AND triggered_at IS NULL) OR (is_triggered = TRUE AND triggered_at IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_stage_completion_rewards_child_stage
    ON public.stage_completion_rewards(child_id, stage_number)
    WHERE is_triggered = FALSE;

ALTER TABLE public.stage_completion_rewards ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_stage_completion_rewards_all ON public.stage_completion_rewards;
CREATE POLICY backend_stage_completion_rewards_all
    ON public.stage_completion_rewards
    FOR ALL
    USING (TRUE)
    WITH CHECK (TRUE);
