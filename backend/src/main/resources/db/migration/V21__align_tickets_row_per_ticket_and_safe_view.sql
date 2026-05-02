CREATE TABLE IF NOT EXISTS public.tickets_row_per_ticket (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(id) ON DELETE CASCADE,
    ticket_type ticket_type_enum NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO public.tickets_row_per_ticket (id, child_id, ticket_type, created_at)
SELECT gen_random_uuid(), child_id, 'NORMAL'::ticket_type_enum, COALESCE(updated_at, NOW())
FROM public.tickets
CROSS JOIN generate_series(1, GREATEST(normal, 0))
WHERE EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'tickets'
      AND column_name = 'normal'
);

INSERT INTO public.tickets_row_per_ticket (id, child_id, ticket_type, created_at)
SELECT gen_random_uuid(), child_id, 'RARE'::ticket_type_enum, COALESCE(updated_at, NOW())
FROM public.tickets
CROSS JOIN generate_series(1, GREATEST(rare, 0))
WHERE EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'tickets'
      AND column_name = 'rare'
);

INSERT INTO public.tickets_row_per_ticket (id, child_id, ticket_type, created_at)
SELECT gen_random_uuid(), child_id, 'EPIC'::ticket_type_enum, COALESCE(updated_at, NOW())
FROM public.tickets
CROSS JOIN generate_series(1, GREATEST(epic, 0))
WHERE EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'tickets'
      AND column_name = 'epic'
);

DROP TABLE public.tickets;

ALTER TABLE public.tickets_row_per_ticket RENAME TO tickets;

CREATE INDEX IF NOT EXISTS idx_tickets_child_unused_type
    ON public.tickets (child_id, ticket_type)
    WHERE used_at IS NULL;

ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_tickets_all ON public.tickets;
CREATE POLICY backend_tickets_all ON public.tickets
    FOR ALL USING (TRUE) WITH CHECK (TRUE);

DROP VIEW IF EXISTS public.question_bank_safe;

CREATE VIEW public.question_bank_safe AS
SELECT
    id,
    mission_id,
    question_type,
    prompt,
    options_json,
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
WHERE is_active = TRUE;
