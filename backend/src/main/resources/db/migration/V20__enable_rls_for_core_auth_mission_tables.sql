ALTER TABLE IF EXISTS public.parent_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.child_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.streak_records ENABLE ROW LEVEL SECURITY;

ALTER TABLE IF EXISTS public.missions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.question_bank ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS private.question_answer_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.quiz_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.mission_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.mission_answer_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.mission_daily_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_parent_accounts_all ON public.parent_accounts;
CREATE POLICY backend_parent_accounts_all ON public.parent_accounts
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_child_profiles_all ON public.child_profiles;
CREATE POLICY backend_child_profiles_all ON public.child_profiles
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_tickets_all ON public.tickets;
CREATE POLICY backend_tickets_all ON public.tickets
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_streak_records_all ON public.streak_records;
CREATE POLICY backend_streak_records_all ON public.streak_records
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_missions_all ON public.missions;
CREATE POLICY backend_missions_all ON public.missions
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_question_bank_all ON public.question_bank;
CREATE POLICY backend_question_bank_all ON public.question_bank
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_question_answer_keys_all ON private.question_answer_keys;
CREATE POLICY backend_question_answer_keys_all ON private.question_answer_keys
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_quiz_attempts_all ON public.quiz_attempts;
CREATE POLICY backend_quiz_attempts_all ON public.quiz_attempts
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_mission_attempts_all ON public.mission_attempts;
CREATE POLICY backend_mission_attempts_all ON public.mission_attempts
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_mission_answer_results_all ON public.mission_answer_results;
CREATE POLICY backend_mission_answer_results_all ON public.mission_answer_results
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP POLICY IF EXISTS backend_mission_daily_progress_all ON public.mission_daily_progress;
CREATE POLICY backend_mission_daily_progress_all ON public.mission_daily_progress
    FOR ALL USING (TRUE) WITH CHECK (TRUE);
