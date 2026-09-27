-- =============================================================================
-- Migration template
-- Copy this file and fill in each placeholder before applying.
-- File name convention: migrate-YYYYMMDD-<description>.sql
-- Location: src/main/resources/migrations/
-- This directory is NOT on Spring Boot's auto-run classpath.
-- Apply manually or via deployment tooling; never run automatically on startup.
-- =============================================================================
-- Migration: <YYYYMMDD>-<description>
-- Description: <what this migration does and why>
-- Affected tables: <comma-separated list>
-- Prerequisites: <other migration IDs that must be applied first, or "none">
-- Applied by: <operator name or automation reference>
-- Applied at: <YYYY-MM-DD HH:MM UTC>
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Step 0: Create applied-migration record table if not exists
-- Run this on every migration — safe to repeat; no-op if already exists.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS migrations_applied (
    migration_id   VARCHAR(100) NOT NULL PRIMARY KEY,
    description    VARCHAR(255) NOT NULL,
    applied_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by     VARCHAR(100) NOT NULL DEFAULT 'unknown'
);

-- ---------------------------------------------------------------------------
-- Step 1: Precondition check
-- Run this SELECT before applying Step 2.
-- Read the result and confirm it matches the "Expected result" comment.
-- If it does not, STOP and investigate before continuing.
-- ---------------------------------------------------------------------------

-- Example — check that a column does not yet exist:
-- SELECT COUNT(*) AS already_exists
-- FROM information_schema.columns
-- WHERE table_schema = DATABASE()
--   AND table_name   = '<table>'
--   AND column_name  = '<column>';
-- Expected result: 0 (column does not yet exist).
-- If result is 1, the column already exists — skip Step 2 and proceed to Step 4.

-- Replace the example above with the appropriate precondition for this migration.

-- ---------------------------------------------------------------------------
-- Step 2: Migration SQL
-- Replace the placeholder below with the actual DDL/DML statement.
-- ---------------------------------------------------------------------------

-- <ALTER TABLE / CREATE TABLE / CREATE INDEX / UPDATE / INSERT statement>

-- ---------------------------------------------------------------------------
-- Step 3: Verification
-- Run this SELECT after applying Step 2.
-- Confirm the result matches the "Expected result" comment before proceeding.
-- ---------------------------------------------------------------------------

-- Example — confirm the column now exists:
-- SELECT COUNT(*) AS column_exists
-- FROM information_schema.columns
-- WHERE table_schema = DATABASE()
--   AND table_name   = '<table>'
--   AND column_name  = '<column>';
-- Expected result: 1

-- Replace the example above with the appropriate verification for this migration.

-- ---------------------------------------------------------------------------
-- Step 4: Record migration as applied
-- Replace the VALUES placeholders with the actual migration ID and description.
-- ---------------------------------------------------------------------------
-- INSERT INTO migrations_applied (migration_id, description, applied_by)
-- VALUES (
--     '<YYYYMMDD>-<description>',
--     '<same one-line description as the header above>',
--     '<operator name>'
-- );
