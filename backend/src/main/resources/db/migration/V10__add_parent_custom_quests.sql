DO $$ BEGIN
    CREATE TYPE custom_quest_status_enum AS ENUM (
        'ACTIVE',
        'PENDING_CONFIRM',
        'COMPLETED',
        'AUTO_CONFIRMED',
        'EXPIRED',
        'CANCELLED'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS public.parent_custom_quests (
    quest_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id TEXT NOT NULL REFERENCES public.parent_accounts(parent_id) ON DELETE CASCADE,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    title VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    reward_text VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status custom_quest_status_enum NOT NULL DEFAULT 'ACTIVE',
    completed_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_parent_custom_quests_completed_at CHECK (
        status NOT IN ('PENDING_CONFIRM', 'COMPLETED', 'AUTO_CONFIRMED')
        OR completed_at IS NOT NULL
    ),
    CONSTRAINT chk_parent_custom_quests_confirmed_at CHECK (
        status NOT IN ('COMPLETED', 'AUTO_CONFIRMED')
        OR confirmed_at IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_parent_custom_quests_child_status
    ON public.parent_custom_quests(child_id, status);

CREATE INDEX IF NOT EXISTS idx_parent_custom_quests_parent_child_status_latest
    ON public.parent_custom_quests(parent_id, child_id, status, (COALESCE(confirmed_at, completed_at, created_at)) DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_parent_custom_quests_active_expires
    ON public.parent_custom_quests(expires_at)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_parent_custom_quests_pending_completed
    ON public.parent_custom_quests(completed_at)
    WHERE status = 'PENDING_CONFIRM';

ALTER TABLE public.parent_custom_quests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_parent_custom_quests_all ON public.parent_custom_quests;
CREATE POLICY backend_parent_custom_quests_all
    ON public.parent_custom_quests
    FOR ALL
    USING (TRUE)
    WITH CHECK (TRUE);
