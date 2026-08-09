CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE public.child_profiles
    ALTER COLUMN child_id SET DEFAULT gen_random_uuid();

ALTER TABLE public.missions
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.question_bank
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE private.question_quality_issues
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.quiz_attempts
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.mission_attempts
    ALTER COLUMN attempt_id SET DEFAULT gen_random_uuid();

ALTER TABLE public.mission_answer_results
    ALTER COLUMN result_id SET DEFAULT gen_random_uuid();

ALTER TABLE public.pets
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.tickets
    ALTER COLUMN ticket_id SET DEFAULT gen_random_uuid();

ALTER TABLE public.gacha_pulls
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.pet_fragments
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.currency_transactions
    ALTER COLUMN transaction_id SET DEFAULT gen_random_uuid();

ALTER TABLE public.streak_milestones
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.milestone_rewards
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.daily_quest_progress
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.weekly_quest_progress
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.achievement_progress
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.privacy_events
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.return_reward_claims
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.fcm_notification_events
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.streak_milestones
    VALIDATE CONSTRAINT streak_milestones_target_days_check;

ALTER TABLE public.streak_milestones
    VALIDATE CONSTRAINT streak_milestones_tier_check;

ALTER TABLE public.streak_milestones
    VALIDATE CONSTRAINT chk_streak_milestones_reward_claimed;

ALTER TABLE public.streak_milestones
    VALIDATE CONSTRAINT chk_streak_milestones_achieved_at;
