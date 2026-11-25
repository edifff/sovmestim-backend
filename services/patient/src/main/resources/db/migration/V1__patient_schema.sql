-- Patient service database: profile dictionaries, allergies and chronic conditions. The owning
-- user id is stored as a plain UUID: accounts live in the identity service.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE status (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE allergy (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255)
);

CREATE TABLE mkb (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE chronic_disease (
    id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mkb_id UUID REFERENCES mkb(id),
    name   VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE severity_reaction (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE allergy_user (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    allergy_id           UUID NOT NULL REFERENCES allergy(id),
    user_id              UUID NOT NULL,
    severity_reaction_id UUID REFERENCES severity_reaction(id),
    symptoms             TEXT,
    reason               TEXT,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced            BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE chronic_disease_user (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chronic_disease_id UUID NOT NULL REFERENCES chronic_disease(id),
    user_id            UUID NOT NULL,
    status_id          UUID REFERENCES status(id),
    diagnosis_date     DATE,
    note               TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced          BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE user_disease (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID NOT NULL,
    status_id            UUID REFERENCES status(id),
    severity_reaction_id UUID REFERENCES severity_reaction(id),
    mkb_id               UUID REFERENCES mkb(id),
    disease_name         VARCHAR(255),
    onset_date           DATE,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced            BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_allergy_user_user     ON allergy_user(user_id);
CREATE INDEX idx_allergy_user_allergy  ON allergy_user(allergy_id);
CREATE INDEX idx_chronic_user_user     ON chronic_disease_user(user_id);
CREATE INDEX idx_user_disease_user     ON user_disease(user_id);
CREATE INDEX idx_user_disease_mkb      ON user_disease(mkb_id);
CREATE INDEX idx_chronic_disease_mkb   ON chronic_disease(mkb_id);

CREATE INDEX idx_allergy_lower_name           ON allergy(lower(name));
CREATE INDEX idx_severity_reaction_lower_name ON severity_reaction(lower(name));
CREATE INDEX idx_status_lower_name            ON status(lower(name));
CREATE INDEX idx_chronic_disease_lower_name   ON chronic_disease(lower(name));
CREATE INDEX idx_mkb_lower_code               ON mkb(lower(code));

CREATE INDEX idx_allergy_user_active     ON allergy_user(user_id, is_deleted, updated_at DESC);
CREATE INDEX idx_chronic_user_active     ON chronic_disease_user(user_id, is_deleted, updated_at DESC);
CREATE INDEX idx_user_disease_active     ON user_disease(user_id, is_deleted);
CREATE INDEX idx_allergy_user_changed    ON allergy_user(user_id, updated_at);
CREATE INDEX idx_chronic_user_changed    ON chronic_disease_user(user_id, updated_at);

CREATE TRIGGER trg_allergy_user_updated_at
    BEFORE UPDATE ON allergy_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_chronic_user_updated_at
    BEFORE UPDATE ON chronic_disease_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_disease_updated_at
    BEFORE UPDATE ON user_disease
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
