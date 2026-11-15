-- Intake service database: treatment courses, medication courses and substance-level intake rules.
-- Catalog medicine ids are plain UUIDs; the medicine name and brand are stored as a read snapshot.

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

CREATE TABLE intake_type (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE notification_type (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE intake (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID NOT NULL,
    intake_type_id UUID REFERENCES intake_type(id),
    name           VARCHAR(255),
    start_date     DATE,
    end_date       DATE,
    note           TEXT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced      BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE course_medicine (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID NOT NULL,
    medicine_id  UUID,
    medicine_name VARCHAR(255) NOT NULL,
    brand        VARCHAR(255),
    dosage       VARCHAR(255),
    frequency    VARCHAR(255),
    start_date   DATE,
    status_id    UUID REFERENCES status(id),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced    BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE taken_substance (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intake_id           UUID NOT NULL REFERENCES intake(id) ON DELETE CASCADE,
    medicine_id         UUID,
    active_substance_id UUID,
    status_id           UUID REFERENCES status(id),
    dosage              VARCHAR(255),
    frequency           VARCHAR(255),
    intake_time         TIME,
    end_date            DATE,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced           BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

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
    user_id            UUID NOT NULL,
    taken_substance_id UUID REFERENCES taken_substance(id) ON DELETE CASCADE,
    status_id          UUID REFERENCES status(id),
    time               TIME,
    days_before        INTEGER CHECK (days_before >= 0),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_synced          BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_intake_user           ON intake(user_id);
CREATE INDEX idx_course_medicine_user  ON course_medicine(user_id);
CREATE INDEX idx_course_medicine_med   ON course_medicine(medicine_id);
CREATE INDEX idx_taken_sub_intake      ON taken_substance(intake_id);
CREATE INDEX idx_taken_sub_medicine    ON taken_substance(medicine_id);
CREATE INDEX idx_taken_sub_substance   ON taken_substance(active_substance_id);
CREATE INDEX idx_notification_taken    ON notification(taken_substance_id);
CREATE INDEX idx_alarm_user            ON alarm(user_id);
CREATE INDEX idx_alarm_taken           ON alarm(taken_substance_id);
CREATE INDEX idx_intake_synced         ON intake(is_synced) WHERE is_synced = FALSE;
CREATE INDEX idx_alarm_synced          ON alarm(is_synced) WHERE is_synced = FALSE;
CREATE INDEX idx_status_lower_name     ON status(lower(name));
CREATE INDEX idx_course_medicine_active ON course_medicine(user_id, is_deleted, updated_at DESC);
CREATE INDEX idx_course_medicine_changed ON course_medicine(user_id, updated_at);

CREATE TRIGGER trg_intake_updated_at
    BEFORE UPDATE ON intake
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
