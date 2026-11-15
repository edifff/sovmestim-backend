-- Catalog service database: ATC classification, active substances, trade marks, medicines and the
-- medicine-substance links. Owned exclusively by the catalog service.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE unit_measurement (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE form_release (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE atc (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE active_substance (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    atc_id      UUID REFERENCES atc(id),
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE trade_mark (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name_brand    VARCHAR(255) NOT NULL,
    manufacturer  VARCHAR(255),
    country       VARCHAR(255),
    photo_url     TEXT
);

CREATE TABLE medicine (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    form_release_id UUID REFERENCES form_release(id),
    trade_mark_id   UUID REFERENCES trade_mark(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT
);

CREATE TABLE substance_in_medicine (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    medicine_id          UUID NOT NULL REFERENCES medicine(id) ON DELETE CASCADE,
    active_substance_id  UUID NOT NULL REFERENCES active_substance(id),
    unit_measurement_id  UUID REFERENCES unit_measurement(id),
    dosage               VARCHAR(255),
    UNIQUE (medicine_id, active_substance_id)
);

CREATE INDEX idx_medicine_trade        ON medicine(trade_mark_id);
CREATE INDEX idx_medicine_form         ON medicine(form_release_id);
CREATE INDEX idx_substance_med_med     ON substance_in_medicine(medicine_id);
CREATE INDEX idx_substance_med_sub     ON substance_in_medicine(active_substance_id);
CREATE INDEX idx_trade_mark_lower_brand ON trade_mark(lower(name_brand));
