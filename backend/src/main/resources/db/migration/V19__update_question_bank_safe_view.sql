CREATE OR REPLACE VIEW public.question_bank_safe AS
SELECT
    id,
    mission_id,
    question_type,
    prompt,
    options_json AS options,
    content_tags,
    curriculum_ref,
    difficulty,
    source_type,
    generation_phase,
    created_at
FROM public.question_bank
WHERE is_active = TRUE;
