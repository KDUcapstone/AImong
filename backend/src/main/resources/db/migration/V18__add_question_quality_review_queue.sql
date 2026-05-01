ALTER TABLE public.question_bank
    DROP CONSTRAINT IF EXISTS chk_question_bank_pool_status;

ALTER TABLE public.question_bank
    ADD CONSTRAINT chk_question_bank_pool_status
        CHECK (question_pool_status IN ('ACTIVE', 'QUARANTINED', 'RETIRED'));

CREATE TABLE IF NOT EXISTS private.question_quality_issues (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    mission_id UUID NOT NULL,
    reported_by_child_id UUID NULL,
    issue_source VARCHAR(32) NOT NULL,
    issue_status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    detail_text TEXT NULL,
    validation_decision VARCHAR(32) NULL,
    hard_fail_reasons JSONB NULL,
    repair_hints JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_question_quality_issues_question
        FOREIGN KEY (question_id) REFERENCES public.question_bank (id),
    CONSTRAINT fk_question_quality_issues_mission
        FOREIGN KEY (mission_id) REFERENCES public.missions (id),
    CONSTRAINT chk_question_quality_issue_source
        CHECK (issue_source IN ('USER_REPORT', 'SERVING_REVALIDATION')),
    CONSTRAINT chk_question_quality_issue_status
        CHECK (issue_status IN ('OPEN', 'QUARANTINED', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX IF NOT EXISTS idx_question_quality_issues_question_status
    ON private.question_quality_issues (question_id, issue_status, issue_source);
