-- ============================================================
-- Application-support tables not present in the base tracker schema:
-- passwordless login (OTP + rotating refresh tokens) and the advice audit trail.
-- ============================================================

CREATE TABLE otp_code (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email      VARCHAR(255) NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    consumed   BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts   INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_code_email ON otp_code(email, created_at DESC);

CREATE TABLE refresh_token (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);

CREATE TABLE advice_record (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    drug_name       VARCHAR(255),
    status          VARCHAR(64)  NOT NULL,
    level           VARCHAR(64),
    rules_version   VARCHAR(64),
    mapping_version VARCHAR(64),
    catalog_version VARCHAR(64),
    request_json    TEXT,
    result_json     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_advice_record_user ON advice_record(user_id, created_at DESC);
