-- ============================================================
-- Performance indexes for the hot read paths.
--
-- 1. Case-insensitive dictionary lookups (findByNameIgnoreCase / findByCodeIgnoreCase /
--    findByNameBrandIgnoreCase) compile to lower(column) = lower(?), which cannot use the plain
--    unique index on the column. Functional indexes fix that.
-- 2. The interaction lookup filters on both substance columns at once; a composite index serves
--    both predicates instead of intersecting two single-column indexes.
-- 3. The per-user sync/profile queries filter by user_id (+ deleted) and order by updated_at;
--    the composite indexes avoid a sort over all of a user's rows.
-- ============================================================

-- Case-insensitive dictionary lookups
CREATE INDEX idx_allergy_lower_name           ON allergy (lower(name));
CREATE INDEX idx_severity_reaction_lower_name ON severity_reaction (lower(name));
CREATE INDEX idx_status_lower_name            ON status (lower(name));
CREATE INDEX idx_chronic_disease_lower_name   ON chronic_disease (lower(name));
CREATE INDEX idx_mkb_lower_code               ON mkb (lower(code));
CREATE INDEX idx_trade_mark_lower_brand       ON trade_mark (lower(name_brand));
CREATE INDEX idx_danger_level_lower_name      ON danger_level (lower(name));

-- Substance-substance interaction lookup: substance1_id IN (...) AND substance2_id IN (...)
CREATE INDEX idx_interaction_sub_pair         ON interaction_substances (substance1_id, substance2_id);

-- Active profile records: WHERE user_id = ? AND is_deleted = false ORDER BY updated_at DESC
CREATE INDEX idx_allergy_user_active     ON allergy_user (user_id, is_deleted, updated_at DESC);
CREATE INDEX idx_chronic_user_active     ON chronic_disease_user (user_id, is_deleted, updated_at DESC);
CREATE INDEX idx_course_medicine_active  ON course_medicine (user_id, is_deleted, updated_at DESC);
CREATE INDEX idx_user_disease_active     ON user_disease (user_id, is_deleted);

-- Sync deltas: WHERE user_id = ? AND updated_at > ? ORDER BY updated_at
CREATE INDEX idx_allergy_user_changed    ON allergy_user (user_id, updated_at);
CREATE INDEX idx_chronic_user_changed    ON chronic_disease_user (user_id, updated_at);
CREATE INDEX idx_course_medicine_changed ON course_medicine (user_id, updated_at);
