ALTER TABLE IF EXISTS public.daily_quest_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.weekly_quest_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.achievement_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.pet_fragments ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.gacha_pulls ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.pets ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.friend_streaks ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.milestone_rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.streak_milestones ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.return_reward_claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.privacy_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.chat_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS private.question_quality_issues ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_daily_quest_progress_all ON public.daily_quest_progress;
CREATE POLICY backend_daily_quest_progress_all ON public.daily_quest_progress
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_weekly_quest_progress_all ON public.weekly_quest_progress;
CREATE POLICY backend_weekly_quest_progress_all ON public.weekly_quest_progress
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_achievement_progress_all ON public.achievement_progress;
CREATE POLICY backend_achievement_progress_all ON public.achievement_progress
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_pet_fragments_all ON public.pet_fragments;
CREATE POLICY backend_pet_fragments_all ON public.pet_fragments
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_gacha_pulls_all ON public.gacha_pulls;
CREATE POLICY backend_gacha_pulls_all ON public.gacha_pulls
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_pets_all ON public.pets;
CREATE POLICY backend_pets_all ON public.pets
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_friend_streaks_all ON public.friend_streaks;
CREATE POLICY backend_friend_streaks_all ON public.friend_streaks
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_milestone_rewards_all ON public.milestone_rewards;
CREATE POLICY backend_milestone_rewards_all ON public.milestone_rewards
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_streak_milestones_all ON public.streak_milestones;
CREATE POLICY backend_streak_milestones_all ON public.streak_milestones
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_return_reward_claims_all ON public.return_reward_claims;
CREATE POLICY backend_return_reward_claims_all ON public.return_reward_claims
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_privacy_events_all ON public.privacy_events;
CREATE POLICY backend_privacy_events_all ON public.privacy_events
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_chat_usage_all ON public.chat_usage;
CREATE POLICY backend_chat_usage_all ON public.chat_usage
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_question_quality_issues_all ON private.question_quality_issues;
CREATE POLICY backend_question_quality_issues_all ON private.question_quality_issues
    FOR ALL USING (TRUE) WITH CHECK (TRUE);
