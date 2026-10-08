-- ============================================================
-- Idempotency for sync pushes.
-- A retried batch (same user + idempotency_key) returns the stored response instead of
-- re-applying changes and re-running advice checks.
-- ============================================================

CREATE TABLE sync_request (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(255) NOT NULL,
    response_json   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sync_request_user_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_sync_request_user ON sync_request(user_id);
