ALTER TABLE public.fcm_notification_events
    ADD COLUMN IF NOT EXISTS ref_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_fcm_events_parent_type_ref
    ON public.fcm_notification_events(parent_id, notification_type, ref_id)
    WHERE ref_id IS NOT NULL;
