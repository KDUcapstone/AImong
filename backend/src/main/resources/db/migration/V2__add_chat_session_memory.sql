CREATE TABLE IF NOT EXISTS public.chat_sessions (
    session_id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_child_active
    ON public.chat_sessions(child_id, expires_at, updated_at DESC);

CREATE TABLE IF NOT EXISTS public.chat_messages (
    message_id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES public.chat_sessions(session_id) ON DELETE CASCADE,
    child_id UUID NOT NULL REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content_masked TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session_created
    ON public.chat_messages(session_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_child_created
    ON public.chat_messages(child_id, created_at DESC);

ALTER TABLE public.chat_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_chat_sessions_all ON public.chat_sessions;
CREATE POLICY backend_chat_sessions_all ON public.chat_sessions FOR ALL USING (TRUE) WITH CHECK (TRUE);
DROP POLICY IF EXISTS backend_chat_messages_all ON public.chat_messages;
CREATE POLICY backend_chat_messages_all ON public.chat_messages FOR ALL USING (TRUE) WITH CHECK (TRUE);
