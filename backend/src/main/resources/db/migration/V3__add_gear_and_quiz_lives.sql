CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE currency_transaction_reason_enum AS ENUM (
        'HEART_REVIVE',
        'STREAK_SHIELD_PURCHASE',
        'MISSION_CLEAR',
        'QUEST_REWARD',
        'ACHIEVEMENT_REWARD',
        'RETURN_REWARD',
        'ADMIN_ADJUST'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE public.child_profiles
    ADD COLUMN IF NOT EXISTS gear INT NOT NULL DEFAULT 0 CHECK (gear >= 0);

ALTER TABLE public.quiz_attempts
    ADD COLUMN IF NOT EXISTS remaining_lives SMALLINT NOT NULL DEFAULT 3 CHECK (remaining_lives BETWEEN 0 AND 3),
    ADD COLUMN IF NOT EXISTS wrong_count_in_session SMALLINT NOT NULL DEFAULT 0 CHECK (wrong_count_in_session >= 0),
    ADD COLUMN IF NOT EXISTS revive_count SMALLINT NOT NULL DEFAULT 0 CHECK (revive_count BETWEEN 0 AND 1),
    ADD COLUMN IF NOT EXISTS revived_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS public.currency_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    amount INT NOT NULL CHECK (amount <> 0),
    balance_after INT NOT NULL CHECK (balance_after >= 0),
    reason currency_transaction_reason_enum NOT NULL,
    ref_type TEXT,
    ref_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_currency_transactions_child
    ON public.currency_transactions(child_id, created_at DESC);

ALTER TABLE public.currency_transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_currency_transactions_all ON public.currency_transactions;
CREATE POLICY backend_currency_transactions_all ON public.currency_transactions FOR ALL USING (TRUE) WITH CHECK (TRUE);
