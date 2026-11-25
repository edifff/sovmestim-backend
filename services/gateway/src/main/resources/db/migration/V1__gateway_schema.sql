-- Gateway database: idempotency records for sync pushes. Business data lives in the owning
-- services; the gateway only remembers the responses it already returned.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE sync_request (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    response_json   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sync_request_user_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_sync_request_user ON sync_request(user_id);
