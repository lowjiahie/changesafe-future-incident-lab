-- =============================================================================
-- Migration: 20260926-orders-add-idempotency-key
-- Description: Add idempotency_key VARCHAR(64) UNIQUE NULL to the orders table.
--
-- Background: This column was added to schema.sql during checkout-flow-01 (2026-09-26)
-- to prevent duplicate order placement on repeated form submission (BR-001, R-04).
-- The schema.sql change only affects fresh installs. Any MySQL database that was
-- initialised before 2026-09-26 is missing this column and must have it added via
-- this migration. Fresh installs already have the column via CREATE TABLE IF NOT EXISTS.
--
-- Affected tables: orders
-- Prerequisites: none
-- Applied by: <operator name>
-- Applied at: <YYYY-MM-DD HH:MM UTC>
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Step 0: Create applied-migration record table if not exists
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS migrations_applied (
    migration_id   VARCHAR(100) NOT NULL PRIMARY KEY,
    description    VARCHAR(255) NOT NULL,
    applied_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by     VARCHAR(100) NOT NULL DEFAULT 'unknown'
);

-- ---------------------------------------------------------------------------
-- Step 1: Precondition checks
-- ---------------------------------------------------------------------------

-- 1a. Verify the column does not yet exist.
--     Expected result: 0
--     If result is 1, the column already exists — skip Step 2 and proceed to Step 4.
SELECT COUNT(*) AS already_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'orders'
  AND column_name  = 'idempotency_key';

-- 1b. Data safety check — only run after confirming 1a returned 0.
--     If 1a returned 1 (column already exists), skip 1b.
--
--     Checks whether any existing idempotency_key values would violate the UNIQUE
--     constraint. Expected result: 0 (no duplicates among non-NULL rows).
--     If result > 0, investigate and resolve the duplicates before continuing.
--
--     NOTE: If the column does not yet exist (1a = 0), this query will fail with
--     "Unknown column 'idempotency_key'". That failure is expected and confirms the
--     column is absent; proceed directly to Step 2.
--
-- SELECT COUNT(*) AS duplicate_keys
-- FROM (
--     SELECT idempotency_key
--     FROM orders
--     WHERE idempotency_key IS NOT NULL
--     GROUP BY idempotency_key
--     HAVING COUNT(*) > 1
-- ) AS dupes;

-- ---------------------------------------------------------------------------
-- Step 2: Migration SQL
-- ---------------------------------------------------------------------------

-- Add the column as NULL (not NOT NULL) because existing orders rows have no
-- idempotency key. Historical rows legitimately have NULL. The UNIQUE constraint
-- allows multiple NULL values in both MySQL (any version) and H2 2.x.
--
-- MySQL 5.7 does not support ADD COLUMN IF NOT EXISTS; the precondition check
-- in Step 1a is the idempotency guard.
ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(64) NULL;

-- Add the UNIQUE constraint separately so that if the column already exists
-- without the constraint, the constraint can still be added independently.
ALTER TABLE orders
    ADD CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key);

-- Note: If running against a database where the column already exists but the
-- UNIQUE constraint is absent, run only the second ALTER TABLE statement above.

-- ---------------------------------------------------------------------------
-- Step 3: Verification
-- ---------------------------------------------------------------------------

-- Confirm the column now exists.
-- Expected result: 1
SELECT COUNT(*) AS column_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'orders'
  AND column_name  = 'idempotency_key';

-- Confirm the UNIQUE constraint is present.
-- Expected result: 1
SELECT COUNT(*) AS constraint_exists
FROM information_schema.table_constraints
WHERE table_schema   = DATABASE()
  AND table_name     = 'orders'
  AND constraint_name = 'uq_orders_idempotency_key'
  AND constraint_type = 'UNIQUE';

-- ---------------------------------------------------------------------------
-- Step 4: Record migration as applied
-- Replace <operator name> with the name of the person or process that applied this.
-- ---------------------------------------------------------------------------
INSERT INTO migrations_applied (migration_id, description, applied_by)
VALUES (
    '20260926-orders-add-idempotency-key',
    'Add idempotency_key VARCHAR(64) UNIQUE NULL to orders table (checkout-flow-01)',
    '<operator name>'
);
