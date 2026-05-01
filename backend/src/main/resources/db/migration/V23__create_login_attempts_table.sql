CREATE TABLE login_attempts (
    key             VARCHAR(255) PRIMARY KEY,
    failure_count   INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ  NOT NULL
);
