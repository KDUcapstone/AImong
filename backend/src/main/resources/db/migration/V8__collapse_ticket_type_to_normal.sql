INSERT INTO public.tickets (child_id, ticket_type, used_at, created_at)
SELECT child_id, 'NORMAL'::public.ticket_type_enum, NULL, created_at
FROM public.tickets
WHERE ticket_type = 'RARE'::public.ticket_type_enum
  AND used_at IS NULL;

INSERT INTO public.tickets (child_id, ticket_type, used_at, created_at)
SELECT ticket.child_id, 'NORMAL'::public.ticket_type_enum, NULL, ticket.created_at
FROM public.tickets ticket
CROSS JOIN generate_series(1, 2)
WHERE ticket.ticket_type = 'EPIC'::public.ticket_type_enum
  AND ticket.used_at IS NULL;

UPDATE public.tickets
SET ticket_type = 'NORMAL'::public.ticket_type_enum
WHERE ticket_type IN ('RARE'::public.ticket_type_enum, 'EPIC'::public.ticket_type_enum);

UPDATE public.gacha_pulls
SET ticket_type = 'NORMAL'::public.ticket_type_enum
WHERE ticket_type IN ('RARE'::public.ticket_type_enum, 'EPIC'::public.ticket_type_enum);

CREATE TYPE public.ticket_type_enum_v2 AS ENUM ('NORMAL');

ALTER TABLE public.tickets
    ALTER COLUMN ticket_type TYPE public.ticket_type_enum_v2
    USING ticket_type::text::public.ticket_type_enum_v2;

ALTER TABLE public.gacha_pulls
    ALTER COLUMN ticket_type TYPE public.ticket_type_enum_v2
    USING ticket_type::text::public.ticket_type_enum_v2;

DROP TYPE public.ticket_type_enum;

ALTER TYPE public.ticket_type_enum_v2 RENAME TO ticket_type_enum;
