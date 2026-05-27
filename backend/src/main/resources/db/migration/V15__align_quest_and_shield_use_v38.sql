ALTER TYPE public.daily_quest_type_enum ADD VALUE IF NOT EXISTS 'MISSION_3' AFTER 'MISSION_1';
ALTER TYPE public.daily_quest_type_enum ADD VALUE IF NOT EXISTS 'STREAK_CHECK' AFTER 'CHAT_GPT';
ALTER TYPE public.daily_quest_type_enum ADD VALUE IF NOT EXISTS 'ALL_DAILY' AFTER 'ALL_3';

ALTER TYPE public.weekly_quest_type_enum ADD VALUE IF NOT EXISTS 'MISSION_10' AFTER 'MISSION_5';
ALTER TYPE public.weekly_quest_type_enum ADD VALUE IF NOT EXISTS 'STREAK_5' AFTER 'CHAT_3';

ALTER TYPE public.currency_transaction_reason_enum ADD VALUE IF NOT EXISTS 'STREAK_SHIELD_USE' AFTER 'STREAK_SHIELD_PURCHASE';

ALTER TABLE public.currency_transactions
    DROP CONSTRAINT IF EXISTS currency_transactions_amount_check;

ALTER TABLE public.currency_transactions
    DROP CONSTRAINT IF EXISTS chk_currency_transactions_amount_non_zero_or_streak_shield_use;

ALTER TABLE public.currency_transactions
    ADD CONSTRAINT chk_currency_transactions_amount_non_zero_or_streak_shield_use
    CHECK (amount <> 0 OR reason::text = 'STREAK_SHIELD_USE');
