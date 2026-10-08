-- ============================================================
-- PostgreSQL schema: медицинский трекер (offline-first ready)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ФУНКЦИЯ: автообновление updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- СПРАВОЧНИКИ
-- ============================================================

CREATE TABLE danger_level (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

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

CREATE TABLE status (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE intake_type (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE notification_type (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE gender (
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

CREATE TABLE doctor (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           VARCHAR(255) NOT NULL,
    specialization VARCHAR(255)
);

-- ============================================================
-- ПОЛЬЗОВАТЕЛИ
-- ============================================================

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

CREATE TABLE body_weight_change (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    value_kg    NUMERIC(6,2) NOT NULL CHECK (value_kg > 0 AND value_kg < 500),
    change_date DATE NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced   BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE allergy_user (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    allergy_id           UUID NOT NULL REFERENCES allergy(id),
    user_id              UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
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
    user_id            UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status_id          UUID REFERENCES status(id),
    diagnosis_date     DATE,
    note               TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced          BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE user_disease (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status_id            UUID REFERENCES status(id),
    severity_reaction_id UUID REFERENCES severity_reaction(id),
    mkb_id               UUID REFERENCES mkb(id),
    disease_name         VARCHAR(255),
    onset_date           DATE,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced            BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- ЛЕКАРСТВА
-- ============================================================

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
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    form_release_id UUID REFERENCES form_release(id),
    trade_mark_id  UUID REFERENCES trade_mark(id),
    name           VARCHAR(255) NOT NULL,
    description    TEXT
);

-- связь «лекарство ↔ действующее вещество» (у лекарства может быть несколько)
CREATE TABLE substance_in_medicine (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    medicine_id          UUID NOT NULL REFERENCES medicine(id) ON DELETE CASCADE,
    active_substance_id  UUID NOT NULL REFERENCES active_substance(id),
    unit_measurement_id  UUID REFERENCES unit_measurement(id),
    dosage               VARCHAR(255),
    UNIQUE (medicine_id, active_substance_id)
);

CREATE TABLE interaction_substances (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    danger_level_id UUID NOT NULL REFERENCES danger_level(id),
    substance1_id   UUID NOT NULL REFERENCES active_substance(id),
    substance2_id   UUID NOT NULL REFERENCES active_substance(id),
    description     TEXT,
    CHECK (substance1_id <> substance2_id)
);

-- ============================================================
-- ПРИЁМ ЛЕКАРСТВ
-- ============================================================

CREATE TABLE intake (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    intake_type_id UUID REFERENCES intake_type(id),
    name           VARCHAR(255),
    start_date     DATE,
    end_date       DATE,
    note           TEXT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced      BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE taken_medicine (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trade_mark_id UUID REFERENCES trade_mark(id),
    status_id     UUID REFERENCES status(id),
    dosage        VARCHAR(255),
    intake_date   DATE,
    end_date      DATE,
    note          TEXT,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced     BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE course_medicine (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    medicine_id UUID NOT NULL REFERENCES medicine(id),
    dosage      VARCHAR(255),
    frequency   VARCHAR(255),
    start_date  DATE,
    status_id   UUID REFERENCES status(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced   BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE taken_substance (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intake_id           UUID NOT NULL REFERENCES intake(id) ON DELETE CASCADE,
    medicine_id         UUID REFERENCES medicine(id),
    active_substance_id UUID REFERENCES active_substance(id),
    status_id           UUID REFERENCES status(id),
    dosage              VARCHAR(255),
    frequency           VARCHAR(255),
    intake_time         TIME,
    end_date            DATE,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced           BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- УВЕДОМЛЕНИЯ
-- ============================================================

CREATE TABLE notification (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    taken_substance_id   UUID REFERENCES taken_substance(id) ON DELETE CASCADE,
    notification_type_id UUID REFERENCES notification_type(id),
    time_notification    TIME,
    sent_at              TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced            BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE alarm (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id            UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    taken_substance_id UUID REFERENCES taken_substance(id) ON DELETE CASCADE,
    status_id          UUID REFERENCES status(id),
    time               TIME,
    days_before        INTEGER CHECK (days_before >= 0),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced          BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- ВРАЧИ И ПРИЁМЫ
-- ============================================================

CREATE TABLE diagnosis_record (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    doctor_id   UUID REFERENCES doctor(id),
    description TEXT,
    record_date DATE,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced   BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE recommendation_list (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doctor_id  UUID REFERENCES doctor(id),
    intake_id  UUID REFERENCES intake(id) ON DELETE CASCADE,
    text       TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced  BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE appointment (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intake_id      UUID REFERENCES intake(id) ON DELETE CASCADE,
    reception_date DATE,
    analysis_date  DATE,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced      BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- ИНДЕКСЫ (на FK + часто используемые поля)
-- ============================================================

CREATE INDEX idx_user_gender           ON app_user(gender_id);
CREATE INDEX idx_body_weight_user      ON body_weight_change(user_id);
CREATE INDEX idx_allergy_user_user     ON allergy_user(user_id);
CREATE INDEX idx_allergy_user_allergy  ON allergy_user(allergy_id);
CREATE INDEX idx_chronic_user_user     ON chronic_disease_user(user_id);
CREATE INDEX idx_user_disease_user     ON user_disease(user_id);
CREATE INDEX idx_user_disease_mkb      ON user_disease(mkb_id);
CREATE INDEX idx_chronic_disease_mkb   ON chronic_disease(mkb_id);
CREATE INDEX idx_medicine_trade        ON medicine(trade_mark_id);
CREATE INDEX idx_medicine_form         ON medicine(form_release_id);
CREATE INDEX idx_substance_med_med     ON substance_in_medicine(medicine_id);
CREATE INDEX idx_substance_med_sub     ON substance_in_medicine(active_substance_id);
CREATE INDEX idx_interaction_sub1      ON interaction_substances(substance1_id);
CREATE INDEX idx_interaction_sub2      ON interaction_substances(substance2_id);
CREATE INDEX idx_intake_user           ON intake(user_id);
CREATE INDEX idx_course_medicine_user  ON course_medicine(user_id);
CREATE INDEX idx_course_medicine_med   ON course_medicine(medicine_id);
CREATE INDEX idx_taken_sub_intake      ON taken_substance(intake_id);
CREATE INDEX idx_taken_sub_medicine    ON taken_substance(medicine_id);
CREATE INDEX idx_taken_sub_substance   ON taken_substance(active_substance_id);
CREATE INDEX idx_notification_taken    ON notification(taken_substance_id);
CREATE INDEX idx_alarm_user            ON alarm(user_id);
CREATE INDEX idx_alarm_taken           ON alarm(taken_substance_id);
CREATE INDEX idx_diagnosis_user        ON diagnosis_record(user_id);
CREATE INDEX idx_diagnosis_doctor      ON diagnosis_record(doctor_id);
CREATE INDEX idx_recommendation_intake ON recommendation_list(intake_id);
CREATE INDEX idx_appointment_intake    ON appointment(intake_id);

-- индексы для синхронизации
CREATE INDEX idx_intake_synced         ON intake(is_synced) WHERE is_synced = FALSE;
CREATE INDEX idx_alarm_synced          ON alarm(is_synced)  WHERE is_synced = FALSE;

-- ============================================================
-- ТРИГГЕРЫ на updated_at
-- ============================================================

CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_body_weight_updated_at
    BEFORE UPDATE ON body_weight_change
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_allergy_user_updated_at
    BEFORE UPDATE ON allergy_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_chronic_user_updated_at
    BEFORE UPDATE ON chronic_disease_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_disease_updated_at
    BEFORE UPDATE ON user_disease
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_intake_updated_at
    BEFORE UPDATE ON intake
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_taken_medicine_updated_at
    BEFORE UPDATE ON taken_medicine
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_course_medicine_updated_at
    BEFORE UPDATE ON course_medicine
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_taken_substance_updated_at
    BEFORE UPDATE ON taken_substance
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_notification_updated_at
    BEFORE UPDATE ON notification
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_alarm_updated_at
    BEFORE UPDATE ON alarm
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_diagnosis_updated_at
    BEFORE UPDATE ON diagnosis_record
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_recommendation_updated_at
    BEFORE UPDATE ON recommendation_list
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_appointment_updated_at
    BEFORE UPDATE ON appointment
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();