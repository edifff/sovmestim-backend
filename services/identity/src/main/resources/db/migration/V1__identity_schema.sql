-- Identity service database: accounts, directory genders, one-time codes and refresh tokens.
-- Owned exclusively by the identity service.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE gender (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE app_user (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    gender_id   UUID REFERENCES gender(id),
    surname     VARCHAR(255),
    name        VARCHAR(255),
    patronymic  VARCHAR(255),
    birth_date  DATE,
    email       VARCHAR(255) UNIQUE,
    phone       VARCHAR(50),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced   BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_user_gender ON app_user(gender_id);

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

CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
