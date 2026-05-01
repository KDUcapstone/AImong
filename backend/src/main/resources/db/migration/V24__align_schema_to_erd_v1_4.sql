DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'child_code') THEN
        CREATE DOMAIN child_code AS TEXT CHECK (VALUE ~ '^[0-9]{6}$');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'privacy_detected_type_enum') THEN
        CREATE TYPE privacy_detected_type_enum AS ENUM ('NAME', 'SCHOOL', 'AGE', 'PHONE', 'EMAIL', 'ADDRESS', 'DATE', 'URL');
    END IF;
END $$;

ALTER TYPE achievement_type_enum ADD VALUE IF NOT EXISTS 'MISSION_10';
ALTER TYPE achievement_type_enum ADD VALUE IF NOT EXISTS 'MISSION_30';
ALTER TYPE achievement_type_enum ADD VALUE IF NOT EXISTS 'XP_100';
ALTER TYPE achievement_type_enum ADD VALUE IF NOT EXISTS 'XP_500';

DROP VIEW IF EXISTS public.question_bank_safe;

ALTER TABLE public.child_profiles
    DROP CONSTRAINT IF EXISTS child_profiles_parent_id_fkey;

ALTER TABLE public.parent_accounts
    DROP CONSTRAINT IF EXISTS parent_accounts_firebase_uid_key;

ALTER TABLE public.parent_accounts
    DROP CONSTRAINT IF EXISTS parent_accounts_pkey;

ALTER TABLE public.child_profiles
    ADD COLUMN IF NOT EXISTS parent_id_text TEXT;

UPDATE public.child_profiles child
SET parent_id_text = parent.firebase_uid
FROM public.parent_accounts parent
WHERE child.parent_id_text IS NULL
  AND child.parent_id = parent.id;

ALTER TABLE public.child_profiles
    ALTER COLUMN parent_id_text SET NOT NULL,
    DROP COLUMN IF EXISTS parent_id;

ALTER TABLE public.child_profiles
    RENAME COLUMN parent_id_text TO parent_id;

ALTER TABLE public.parent_accounts
    DROP COLUMN IF EXISTS id;

ALTER TABLE public.parent_accounts
    RENAME COLUMN firebase_uid TO parent_id;

ALTER TABLE public.parent_accounts
    ADD CONSTRAINT parent_accounts_pkey PRIMARY KEY (parent_id);

ALTER TABLE public.child_profiles
    ADD CONSTRAINT child_profiles_parent_id_fkey
        FOREIGN KEY (parent_id) REFERENCES public.parent_accounts(parent_id) ON DELETE CASCADE;

ALTER TABLE public.child_profiles
    DROP CONSTRAINT IF EXISTS child_profiles_code_key;

ALTER TABLE public.child_profiles
    ALTER COLUMN code TYPE child_code USING code::TEXT::child_code,
    ADD CONSTRAINT child_profiles_code_key UNIQUE (code);

ALTER TABLE public.child_profiles
    RENAME COLUMN id TO child_id;

ALTER TABLE public.child_profiles
    ADD COLUMN IF NOT EXISTS shield_count INT NOT NULL DEFAULT 0 CHECK (shield_count >= 0),
    ADD COLUMN IF NOT EXISTS equipped_pet_id UUID;

UPDATE public.child_profiles child
SET equipped_pet_id = equipped.pet_id
FROM public.equipped_pets equipped
WHERE child.equipped_pet_id IS NULL
  AND child.child_id = equipped.child_id;

DROP TABLE IF EXISTS public.equipped_pets;

ALTER TABLE public.child_profiles
    DROP CONSTRAINT IF EXISTS child_profiles_equipped_pet_id_fkey;

ALTER TABLE public.child_profiles
    ADD CONSTRAINT child_profiles_equipped_pet_id_fkey
        FOREIGN KEY (equipped_pet_id) REFERENCES public.pets(id);

ALTER TABLE public.missions
    DROP CONSTRAINT IF EXISTS missions_stage_check;

ALTER TABLE public.missions
    ADD CONSTRAINT missions_stage_check CHECK (stage BETWEEN 1 AND 16);

ALTER TABLE public.question_bank
    RENAME COLUMN options_json TO options;

UPDATE public.question_bank
SET content_tags = COALESCE(content_tags, '[]'::jsonb),
    curriculum_ref = COALESCE(curriculum_ref, 'UNKNOWN'),
    difficulty = COALESCE(difficulty, difficulty_band, 'LOW'),
    generation_phase = 'PREGENERATED'
WHERE content_tags IS NULL
   OR curriculum_ref IS NULL
   OR difficulty IS NULL
   OR generation_phase <> 'PREGENERATED';

ALTER TABLE public.question_bank
    ALTER COLUMN content_tags SET DEFAULT '[]'::jsonb,
    ALTER COLUMN content_tags SET NOT NULL,
    ALTER COLUMN curriculum_ref SET NOT NULL,
    ALTER COLUMN difficulty SET NOT NULL,
    ALTER COLUMN generation_phase SET DEFAULT 'PREGENERATED',
    ALTER COLUMN generation_phase SET NOT NULL;

ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_generation_phase;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_generation_phase
        CHECK (generation_phase = 'PREGENERATED');

ALTER TABLE public.tickets
    RENAME COLUMN id TO ticket_id;

ALTER TABLE public.fragments RENAME TO pet_fragments;
ALTER TABLE public.daily_quests RENAME TO daily_quest_progress;
ALTER TABLE public.weekly_quests RENAME TO weekly_quest_progress;
ALTER TABLE public.achievements RENAME TO achievement_progress;

ALTER TABLE public.daily_quest_progress
    RENAME COLUMN quest_date TO date;

ALTER TABLE public.achievement_progress
    ADD COLUMN IF NOT EXISTS current_value INT NOT NULL DEFAULT 0 CHECK (current_value >= 0),
    ADD COLUMN IF NOT EXISTS completed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS completed_at DATE;

UPDATE public.achievement_progress
SET completed_at = COALESCE(completed_at, unlocked_at::date)
WHERE completed = TRUE;

ALTER TABLE public.achievement_progress
    ALTER COLUMN completed SET DEFAULT FALSE,
    ALTER COLUMN unlocked_at DROP NOT NULL;

CREATE TABLE IF NOT EXISTS public.return_reward_claims (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    base_last_completed_date DATE NOT NULL,
    ticket_count INT NOT NULL CHECK (ticket_count BETWEEN 1 AND 3),
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, base_last_completed_date)
);

CREATE TABLE IF NOT EXISTS public.privacy_events (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    detected_type privacy_detected_type_enum NOT NULL,
    masked BOOLEAN NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_privacy_events_child ON public.privacy_events(child_id);
CREATE INDEX IF NOT EXISTS idx_privacy_events_date ON public.privacy_events(detected_at);

CREATE TABLE IF NOT EXISTS public.chat_usage (
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    count INT NOT NULL DEFAULT 0 CHECK (count BETWEEN 0 AND 20),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (child_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_child_profiles_parent ON public.child_profiles(parent_id);
CREATE INDEX IF NOT EXISTS idx_tickets_child_unused ON public.tickets(child_id, ticket_type) WHERE used_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_pet_fragments_child ON public.pet_fragments(child_id);
CREATE INDEX IF NOT EXISTS idx_gacha_pulls_child ON public.gacha_pulls(child_id, pulled_at DESC);

CREATE VIEW public.question_bank_safe AS
SELECT
    id,
    mission_id,
    question_type,
    prompt,
    options,
    content_tags,
    curriculum_ref,
    difficulty,
    source_type,
    generation_phase,
    created_at
FROM public.question_bank
WHERE is_active = TRUE;
