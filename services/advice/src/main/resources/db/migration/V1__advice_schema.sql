-- Advice service database: the interaction cache, danger-level dictionary and the immutable advice
-- audit trail. Substance ids reference the catalog service; no cross-database foreign keys are used.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE danger_level (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE interaction_substances (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    danger_level_id UUID NOT NULL REFERENCES danger_level(id),
    substance1_id   UUID NOT NULL,
    substance2_id   UUID NOT NULL,
    description     TEXT,
    CHECK (substance1_id <> substance2_id)
);

CREATE TABLE advice_record (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL,
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

CREATE INDEX idx_interaction_sub1      ON interaction_substances(substance1_id);
CREATE INDEX idx_interaction_sub2      ON interaction_substances(substance2_id);
CREATE INDEX idx_interaction_sub_pair  ON interaction_substances(substance1_id, substance2_id);
CREATE INDEX idx_danger_level_lower_name ON danger_level(lower(name));
CREATE INDEX idx_advice_record_user    ON advice_record(user_id, created_at DESC);
