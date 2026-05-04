CREATE SCHEMA IF NOT EXISTS private;

DO $$ BEGIN
    CREATE TYPE profile_image_type_enum AS ENUM ('DEFAULT', 'SPROUT', 'EXPLORER', 'CRITIC', 'GUARDIAN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE question_type_enum AS ENUM ('OX', 'MULTIPLE', 'FILL', 'SITUATION');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE difficulty_band_enum AS ENUM ('LOW', 'MEDIUM', 'HIGH');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE question_source_enum AS ENUM ('STATIC', 'GPT');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE generation_phase_enum AS ENUM ('PREGENERATED', 'RUNTIME');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE question_pool_status_enum AS ENUM ('ACTIVE', 'QUARANTINED', 'RETIRED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE pet_grade_enum AS ENUM ('NORMAL', 'RARE', 'EPIC', 'LEGEND');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE pet_stage_enum AS ENUM ('EGG', 'GROWTH', 'AIMONG');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE pet_mood_enum AS ENUM ('HAPPY', 'IDLE', 'SAD_LIGHT', 'SAD_DEEP');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE crown_type_enum AS ENUM ('silver', 'gold', 'jewel', 'shining');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE ticket_type_enum AS ENUM ('NORMAL', 'RARE', 'EPIC');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE daily_quest_type_enum AS ENUM ('MISSION_1', 'XP_20', 'CHAT_GPT', 'ALL_3');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE weekly_quest_type_enum AS ENUM ('XP_100', 'MISSION_5', 'CHAT_3');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE achievement_type_enum AS ENUM ('MISSION_10', 'MISSION_30', 'XP_100', 'XP_500');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE privacy_detected_type_enum AS ENUM ('NAME', 'SCHOOL', 'AGE', 'PHONE', 'EMAIL', 'ADDRESS', 'DATE', 'URL');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS public.parent_accounts (
    parent_id TEXT PRIMARY KEY,
    email TEXT,
    fcm_token TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.child_profiles (
    child_id UUID PRIMARY KEY,
    parent_id TEXT NOT NULL REFERENCES public.parent_accounts(parent_id) ON DELETE CASCADE,
    nickname TEXT NOT NULL,
    code VARCHAR(6) NOT NULL UNIQUE,
    starter_issued BOOLEAN NOT NULL DEFAULT FALSE,
    total_xp INT NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    today_xp INT NOT NULL DEFAULT 0 CHECK (today_xp >= 0),
    weekly_xp INT NOT NULL DEFAULT 0 CHECK (weekly_xp >= 0),
    today_xp_date DATE,
    weekly_xp_week_start DATE,
    gacha_pull_count INT NOT NULL DEFAULT 0 CHECK (gacha_pull_count >= 0),
    sr_miss_count INT NOT NULL DEFAULT 0 CHECK (sr_miss_count >= 0),
    shield_count INT NOT NULL DEFAULT 0 CHECK (shield_count >= 0),
    equipped_pet_id UUID,
    profile_image_type profile_image_type_enum NOT NULL DEFAULT 'DEFAULT',
    session_version BIGINT NOT NULL DEFAULT 0,
    fcm_token TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ,
    CONSTRAINT chk_child_profiles_code CHECK (code ~ '^[0-9]{6}$')
);

CREATE INDEX IF NOT EXISTS idx_child_profiles_parent ON public.child_profiles(parent_id);

CREATE TABLE IF NOT EXISTS public.missions (
    id UUID PRIMARY KEY,
    stage SMALLINT NOT NULL CHECK (stage BETWEEN 1 AND 16),
    title TEXT NOT NULL,
    mission_code VARCHAR(16) UNIQUE,
    description TEXT,
    unlock_condition TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.question_bank (
    id UUID PRIMARY KEY,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    question_type question_type_enum NOT NULL,
    prompt TEXT NOT NULL,
    options JSONB,
    content_tags JSONB,
    curriculum_ref TEXT NOT NULL,
    difficulty difficulty_band_enum NOT NULL,
    legacy_numeric_difficulty SMALLINT,
    source_type question_source_enum NOT NULL DEFAULT 'STATIC',
    generation_phase generation_phase_enum NOT NULL DEFAULT 'PREGENERATED',
    pack_no SMALLINT,
    difficulty_band difficulty_band_enum,
    question_pool_status question_pool_status_enum NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_question_bank_mission_active_difficulty
    ON public.question_bank(mission_id, is_active, difficulty);
CREATE INDEX IF NOT EXISTS idx_question_bank_mission_active_pack
    ON public.question_bank(mission_id, is_active, pack_no);
CREATE INDEX IF NOT EXISTS idx_question_bank_pool_status
    ON public.question_bank(mission_id, question_pool_status, is_active);

CREATE TABLE IF NOT EXISTS private.question_answer_keys (
    question_id UUID PRIMARY KEY REFERENCES public.question_bank(id) ON DELETE CASCADE,
    answer_payload JSONB NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS private.question_quality_issues (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES public.question_bank(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    reported_by_child_id UUID,
    issue_source VARCHAR(32) NOT NULL CHECK (issue_source IN ('USER_REPORT', 'SERVING_REVALIDATION')),
    issue_status VARCHAR(32) NOT NULL CHECK (issue_status IN ('OPEN', 'QUARANTINED', 'RESOLVED', 'DISMISSED')),
    reason_code VARCHAR(64) NOT NULL,
    detail_text TEXT,
    validation_decision VARCHAR(32),
    hard_fail_reasons JSONB,
    repair_hints JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_question_quality_issues_question_status
    ON private.question_quality_issues(question_id, issue_status, issue_source);

CREATE TABLE IF NOT EXISTS public.quiz_attempts (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    question_ids_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    is_review BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_child_mission
    ON public.quiz_attempts(child_id, mission_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.mission_attempts (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    attempt_date DATE NOT NULL,
    attempt_no INT NOT NULL,
    score INT NOT NULL CHECK (score >= 0),
    total INT NOT NULL CHECK (total > 0),
    is_review BOOLEAN NOT NULL DEFAULT FALSE,
    is_passed BOOLEAN NOT NULL DEFAULT FALSE,
    xp_earned INT NOT NULL DEFAULT 0 CHECK (xp_earned >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_mission_date
    ON public.mission_attempts(child_id, mission_id, attempt_date);
CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_date
    ON public.mission_attempts(child_id, attempt_date);

CREATE TABLE IF NOT EXISTS public.mission_answer_results (
    result_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES public.mission_attempts(id) ON DELETE CASCADE,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES public.question_bank(id) ON DELETE CASCADE,
    is_review BOOLEAN NOT NULL DEFAULT FALSE,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mission_answer_results_question
    ON public.mission_answer_results(question_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.mission_daily_progress (
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    progress_date DATE NOT NULL,
    first_attempt_at TIMESTAMPTZ NOT NULL,
    best_score INT NOT NULL CHECK (best_score >= 0),
    total INT NOT NULL CHECK (total > 0),
    first_xp_earned INT NOT NULL DEFAULT 0 CHECK (first_xp_earned >= 0),
    review_attempt_count INT NOT NULL DEFAULT 0 CHECK (review_attempt_count >= 0),
    PRIMARY KEY (child_id, mission_id, progress_date)
);

CREATE TABLE IF NOT EXISTS public.pets (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    pet_type TEXT NOT NULL,
    grade pet_grade_enum NOT NULL,
    xp INT NOT NULL DEFAULT 0 CHECK (xp >= 0),
    stage pet_stage_enum NOT NULL DEFAULT 'EGG',
    mood pet_mood_enum NOT NULL DEFAULT 'IDLE',
    crown_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    crown_type crown_type_enum,
    obtained_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, pet_type)
);

ALTER TABLE public.child_profiles
    DROP CONSTRAINT IF EXISTS child_profiles_equipped_pet_id_fkey;
ALTER TABLE public.child_profiles
    ADD CONSTRAINT child_profiles_equipped_pet_id_fkey
        FOREIGN KEY (equipped_pet_id) REFERENCES public.pets(id);

CREATE TABLE IF NOT EXISTS public.tickets (
    ticket_id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    ticket_type ticket_type_enum NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tickets_child_unused
    ON public.tickets(child_id, ticket_type) WHERE used_at IS NULL;

CREATE TABLE IF NOT EXISTS public.gacha_pulls (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    ticket_type ticket_type_enum NOT NULL,
    result_pet_code TEXT NOT NULL,
    grade pet_grade_enum NOT NULL,
    is_new BOOLEAN NOT NULL,
    granted_pet_id UUID REFERENCES public.pets(id) ON DELETE SET NULL,
    fragments_got INT NOT NULL DEFAULT 0 CHECK (fragments_got >= 0),
    sr_miss_before INT NOT NULL DEFAULT 0 CHECK (sr_miss_before >= 0),
    pulled_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gacha_pulls_child
    ON public.gacha_pulls(child_id, pulled_at DESC);

CREATE TABLE IF NOT EXISTS public.pet_fragments (
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    grade pet_grade_enum NOT NULL,
    count INT NOT NULL DEFAULT 0 CHECK (count >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (child_id, grade)
);

CREATE TABLE IF NOT EXISTS public.streak_records (
    child_id UUID PRIMARY KEY REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    continuous_days INT NOT NULL DEFAULT 0 CHECK (continuous_days >= 0),
    last_completed_date DATE,
    today_mission_count INT NOT NULL DEFAULT 0 CHECK (today_mission_count >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.friend_streaks (
    child_id UUID PRIMARY KEY REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    partner_child_id UUID NOT NULL UNIQUE REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friend_streaks_not_self CHECK (child_id <> partner_child_id)
);

CREATE TABLE IF NOT EXISTS public.streak_milestones (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    target_days SMALLINT NOT NULL CHECK (target_days > 0),
    tier SMALLINT NOT NULL CHECK (tier > 0),
    achieved BOOLEAN NOT NULL DEFAULT FALSE,
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    achieved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, target_days)
);

CREATE TABLE IF NOT EXISTS public.milestone_rewards (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    milestone_days SMALLINT NOT NULL CHECK (milestone_days > 0),
    rewarded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, milestone_days)
);

CREATE TABLE IF NOT EXISTS public.daily_quest_progress (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    date DATE NOT NULL,
    quest_type daily_quest_type_enum NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    UNIQUE (child_id, date, quest_type)
);

CREATE TABLE IF NOT EXISTS public.weekly_quest_progress (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    quest_type weekly_quest_type_enum NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    UNIQUE (child_id, week_start, quest_type)
);

CREATE TABLE IF NOT EXISTS public.achievement_progress (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    achievement_type achievement_type_enum NOT NULL,
    current_value INT NOT NULL DEFAULT 0 CHECK (current_value >= 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATE,
    UNIQUE (child_id, achievement_type)
);

CREATE TABLE IF NOT EXISTS public.privacy_events (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    detected_type privacy_detected_type_enum NOT NULL,
    masked BOOLEAN NOT NULL DEFAULT FALSE,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_privacy_events_child_date
    ON public.privacy_events(child_id, detected_at DESC);

CREATE TABLE IF NOT EXISTS public.chat_usage (
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    count INT NOT NULL DEFAULT 0 CHECK (count BETWEEN 0 AND 20),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (child_id, usage_date)
);

CREATE TABLE IF NOT EXISTS public.return_reward_claims (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    base_last_completed_date DATE NOT NULL,
    ticket_count INT NOT NULL CHECK (ticket_count BETWEEN 1 AND 3),
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, base_last_completed_date)
);

CREATE TABLE IF NOT EXISTS public.login_attempts (
    key VARCHAR(255) PRIMARY KEY,
    failure_count INT NOT NULL DEFAULT 0 CHECK (failure_count >= 0),
    locked_until TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_login_attempts_expiry
    ON public.login_attempts(expires_at, locked_until);

CREATE OR REPLACE VIEW public.question_bank_safe AS
SELECT
    id,
    mission_id,
    question_type,
    prompt,
    options,
    content_tags,
    curriculum_ref,
    difficulty,
    legacy_numeric_difficulty,
    source_type,
    generation_phase,
    pack_no,
    difficulty_band,
    question_pool_status,
    created_at,
    is_active
FROM public.question_bank
WHERE is_active = TRUE
  AND question_pool_status = 'ACTIVE';

ALTER TABLE public.parent_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.child_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.missions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.question_bank ENABLE ROW LEVEL SECURITY;
ALTER TABLE private.question_answer_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE private.question_quality_issues ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mission_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mission_answer_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mission_daily_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.gacha_pulls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pet_fragments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.streak_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_streaks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.streak_milestones ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.milestone_rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_quest_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.weekly_quest_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.achievement_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.privacy_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.return_reward_claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.login_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY backend_parent_accounts_all ON public.parent_accounts FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_child_profiles_all ON public.child_profiles FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_missions_all ON public.missions FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_question_bank_all ON public.question_bank FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_question_answer_keys_all ON private.question_answer_keys FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_question_quality_issues_all ON private.question_quality_issues FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_quiz_attempts_all ON public.quiz_attempts FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_mission_attempts_all ON public.mission_attempts FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_mission_answer_results_all ON public.mission_answer_results FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_mission_daily_progress_all ON public.mission_daily_progress FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_pets_all ON public.pets FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_tickets_all ON public.tickets FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_gacha_pulls_all ON public.gacha_pulls FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_pet_fragments_all ON public.pet_fragments FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_streak_records_all ON public.streak_records FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_friend_streaks_all ON public.friend_streaks FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_streak_milestones_all ON public.streak_milestones FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_milestone_rewards_all ON public.milestone_rewards FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_daily_quest_progress_all ON public.daily_quest_progress FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_weekly_quest_progress_all ON public.weekly_quest_progress FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_achievement_progress_all ON public.achievement_progress FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_privacy_events_all ON public.privacy_events FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_chat_usage_all ON public.chat_usage FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_return_reward_claims_all ON public.return_reward_claims FOR ALL USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY backend_login_attempts_all ON public.login_attempts FOR ALL USING (TRUE) WITH CHECK (TRUE);
