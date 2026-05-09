CREATE TABLE IF NOT EXISTS public.fcm_notification_events (
    id UUID PRIMARY KEY,
    parent_id VARCHAR(255) NOT NULL REFERENCES public.parent_accounts(parent_id) ON DELETE CASCADE,
    child_id UUID REFERENCES public.child_profiles(child_id) ON DELETE CASCADE,
    notification_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    detected_type VARCHAR(32),
    aggregate_count INT NOT NULL DEFAULT 1 CHECK (aggregate_count > 0),
    queued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fcm_events_parent_status_sent
    ON public.fcm_notification_events(parent_id, status, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_fcm_events_parent_type_status_queued
    ON public.fcm_notification_events(parent_id, notification_type, status, queued_at ASC);

CREATE INDEX IF NOT EXISTS idx_fcm_events_parent_child_type_sent
    ON public.fcm_notification_events(parent_id, child_id, notification_type, status, sent_at DESC);

ALTER TABLE public.fcm_notification_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS backend_fcm_notification_events_all ON public.fcm_notification_events;
CREATE POLICY backend_fcm_notification_events_all ON public.fcm_notification_events FOR ALL USING (TRUE) WITH CHECK (TRUE);
