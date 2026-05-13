CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE public.streak_milestones
    DROP CONSTRAINT IF EXISTS streak_milestones_target_days_check,
    DROP CONSTRAINT IF EXISTS streak_milestones_tier_check,
    DROP CONSTRAINT IF EXISTS chk_streak_milestones_reward_claimed,
    DROP CONSTRAINT IF EXISTS chk_streak_milestones_achieved_at;

ALTER TABLE public.streak_milestones
    ADD CONSTRAINT streak_milestones_target_days_check CHECK (target_days > 30) NOT VALID,
    ADD CONSTRAINT streak_milestones_tier_check CHECK (tier IN (1, 2, 3)) NOT VALID,
    ADD CONSTRAINT chk_streak_milestones_reward_claimed CHECK (reward_claimed = FALSE OR achieved = TRUE) NOT VALID,
    ADD CONSTRAINT chk_streak_milestones_achieved_at CHECK (
        (achieved = FALSE AND achieved_at IS NULL)
        OR (achieved = TRUE AND achieved_at IS NOT NULL)
    ) NOT VALID;

ALTER TABLE public.pet_fragments
    ADD COLUMN IF NOT EXISTS id UUID;

UPDATE public.pet_fragments
SET id = gen_random_uuid()
WHERE id IS NULL;

ALTER TABLE public.pet_fragments
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE public.pet_fragments
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE public.pet_fragments
    DROP CONSTRAINT IF EXISTS pet_fragments_pkey,
    DROP CONSTRAINT IF EXISTS uq_pet_fragments_child_grade;

ALTER TABLE public.pet_fragments
    ADD CONSTRAINT pet_fragments_pkey PRIMARY KEY (id),
    ADD CONSTRAINT uq_pet_fragments_child_grade UNIQUE (child_id, grade);
