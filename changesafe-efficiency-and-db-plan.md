# Plan: ChangeSafe Efficiency and Safe Database Evolution

> **IBM Bob assisted:** This plan was produced by IBM Bob IDE (Plan mode, then Agent mode).
> The planning session investigated the three prior Bob session screenshots, analysed
> `schema.sql`, `SKILL.md`, and all ChangeSafe workflow files, and produced this plan for
> human approval before implementation. See
> [`bob-assistance-evidence.md`](bob-assistance-evidence.md) for full session evidence and
> Task 04 details.

> Status: APPROVED · Implementation complete.

---

## Corrections from investigation

**`idempotency_key` column origin:** The `orders` table in
[`src/main/resources/schema.sql`](src/main/resources/schema.sql) already contains
`idempotency_key VARCHAR(64) UNIQUE` at line 38. This column was added by Bob during the
checkout-flow-01 task by directly editing the `CREATE TABLE` statement. The user confirms
they did not add it manually. This is exactly the problem the database-evolution sub-task
addresses: Bob edited `CREATE TABLE` without producing a companion `ALTER TABLE` migration,
so any MySQL database that existed before checkout-flow-01 is missing this column silently.
No existing database will be modified by this plan; the plan adds the convention and the
missing migration file as documentation of what needs to happen on a pre-existing database.

---

## Top-Level Overview

Two independent improvements to the ChangeSafe workflow:

**1 — Bobcoin efficiency.**
Measured session data shows three tasks consumed 7.48 + 15.61 + 18.80 = 41.89 Bobcoins.
The 18.80-coin task (checkout-flow audit) reached 78 % context fill for a broad, 10-risk
investigation. The workflow already has token-efficiency rules but they are intent
statements with no actionable phase-level triggers, no per-run spending cap, no configurable
reserve threshold, and no explicit monitoring instruction. This plan adds those mechanisms
without inventing cost claims that are not in evidence, and without assuming automatic
metering exists.

**2 — Safe database evolution.**
`schema.sql` uses `CREATE TABLE IF NOT EXISTS` for all 14 tables. Spring Boot runs it on every
startup (`spring.sql.init.mode=always`). This is correct for a fresh install but provides
no upgrade path. When Bob added `idempotency_key` to the `orders` table during checkout-flow-01,
it edited the `CREATE TABLE` statement and left no migration for existing databases.
This plan introduces a minimal manual versioned-migration convention (no Flyway, no Liquibase),
a `database-migration` Guardian, supporting updates to the Output Contract, templates,
project-conventions rule, and validator — all kept consistent.

---

## Sub-Task 1 — Diagnose and document measured cost drivers

**Intent:** Establish the factual cost baseline so later sub-tasks target real problems.

**Expected Outcomes:**
- New `## Bobcoin budget notes` section added to `changesafe/README.md` recording:
  - The three measured Bobcoin totals and context-fill percentages.
  - Observed cost drivers per task, labelled as measured facts.
  - The disclaimer: Bobcoins ≠ tokens; per-run cost is not fixed; no automatic metering.
  - How to monitor budget: check the Bob IDE task consumption panel manually at the three
    checkpoints defined in Sub-Task 2.

**Todo List:**
1. Append the `## Bobcoin budget notes` section to `changesafe/README.md` with:
   - Table of three measured sessions (task ID, coins, context fill, driver).
   - Note that Task 01 (15.61 coins) is one-time scaffold cost; Task 02 (7.48) is
     planning-only; Task 03 (18.80, 78 % fill) is the representative repeat-run high.
   - Note that no automatic metering API exists; budget is read manually from the Bob IDE
     task consumption panel before, during (post-investigation), and before writing fixes.
   - Cross-reference the checkpoints added in Sub-Task 2.

**Relevant Context:**
- `bob_sessions/TeamLTY_task0{1,2,3}_*.png` — source of the three measurements.
- `changesafe/README.md` — target file.

**Status:** [ ] pending

---

## Sub-Task 2 — Add phase-level budget gates and per-run cap to the SKILL

**Intent:** Convert intent-level efficiency rules into actionable phase-level conditions:
a configurable reserve threshold (default 5 Bobcoins remaining), a per-run spending cap
(configurable by the requester at intake), and explicit pause-and-ask checkpoints.

**Exact changes to `.bob/skills/changesafe/SKILL.md`:**

### Token-efficiency rules section (top of SKILL.md)

Replace the existing bullet list with a revised list that:
- Defines `RESERVE_THRESHOLD` (default 5 Bobcoins remaining): if remaining budget falls
  at or below this value at any checkpoint, deliver a minimal honest report and stop.
- Defines `RUN_CAP` (optional, set by the requester at intake in Bobcoins): if cumulative
  spend since Phase 1 reaches this cap, pause and ask the requester for approval before
  continuing. No default; if not set, the cap is not enforced.
- States explicitly: "There is no automatic Bobcoin meter. Read the Bob IDE task consumption
  panel manually at each checkpoint below and record the reading in the change-brief."
- Retains all existing efficiency bullet points (cheap intake first, test script before
  deep reasoning, two-pass analysis, minimal context packets, conditional parallelism,
  bound outputs, meter and stop).

### Phase 1 additions

After step 3 (capture source state), add:

> **Budget intake checkpoint (mandatory):**
> 1. Ask the requester: "What is your per-run spending cap for this ChangeSafe task?
>    (Optional. Leave blank to use the global reserve threshold only.)"
>    Record the answer as `RUN_CAP` in the change-brief header.
> 2. Remind the requester to read the current Bobcoin balance from the Bob IDE task
>    consumption panel. Record the reading as `Budget at intake` in the change-brief header.
> 3. If remaining balance ≤ `RESERVE_THRESHOLD` (default 5): deliver a one-paragraph scope
>    summary, mark the change-brief status INCOMPLETE — BUDGET EXHAUSTED, and stop.

### Phase 2 addition — convention reuse gate

After step 1 (load project conventions), add:

> **Convention reuse gate:** If `01-project-conventions.md` status is APPROVED and its
> recorded source-state commit matches the current HEAD (no changes to convention-relevant
> files since the rule was approved), skip all repository-wide exploration. Note
> "Conventions reused from commit `<hash>`" in the change-brief and proceed to Phase 3.
> Do not re-read README.md, pom.xml, or representative source files during the reuse path.

### Phase 5 addition — complexity gate

Before spawning the impact-explorer guardian, add:

> **Complexity gate:** If the proposed change touches ≤ 2 files AND a targeted grep finds
> no cross-module callers (i.e. no references from packages outside the changed file's
> bounded context), skip the impact-explorer guardian. Record "Local change — no cross-module
> guardian spawned" in the change-brief impact section and proceed to Phase 6.

### Phase 5 addition — database-migration guardian routing

After the complexity gate, add:

> **Database-migration routing:** If the proposed change touches `schema.sql`,
> any `application*.properties` datasource-init key, or any `src/main/resources/**/*.sql`
> file, activate the `database-migration` guardian. Pass only the fields in its Input
> packet. Record its output in the change-impact map under component "database schema".

### Phase 6a addition — risk-count guard

After the exhaustive risk enumeration instruction, add:

> **Risk-count guard:** Enumerate all identifiable risks from code evidence (no cap on the
> list). Then select the top 3 by severity for active tracking. Remaining risks are
> summarised in one backlog row each in Section 7's deferred risk table, not expanded to
> full evidence rows in context. This reduces in-context token use for broad audits;
> all risks remain visible in the report.

### Phase 11 addition — regeneration guard

Before running the validator, add:

> **Regeneration guard:** If a prior run's `change-brief.md` or `risk-report.md` exists
> and the relevant source files have not changed since that run's source state, do not
> regenerate those files. Update only the sections that changed. If the validator passes
> on the existing files, stop — do not re-render unchanged content.

### Budget checkpoint reminders

Add a one-line checkpoint reminder at the end of Phase 5, Phase 7, and Phase 10:

> "**Budget checkpoint:** Read the Bob IDE task consumption panel. If remaining balance ≤
> `RESERVE_THRESHOLD` or cumulative spend has reached `RUN_CAP`, pause and ask the requester
> before continuing."

**Relevant Context:**
- `.bob/skills/changesafe/SKILL.md` — target file; 285 lines currently.
- The checkout-flow-01 task (18.80 coins, 78 % fill) fired both guardians and expanded
  10 risks fully; the risk-count guard and complexity gate directly address this.
- Convention reuse gate prevents re-reading the full project on repeat runs.
- No automatic metering: the manual-read instruction is essential; do not imply the
  tool can read its own consumption mid-task.

**Status:** [ ] pending

---

## Sub-Task 3 — Create the `database-migration` Guardian

**Intent:** A read-only subagent that activates only when a change touches schema or
datasource-init files. It classifies the change, identifies upgrade impact, recommends
migration SQL, and surfaces data-safety questions — without making any edits.

**New file: `.bob/skills/changesafe/guardians/database-migration/GUARDIAN.md`**

Exact content to create:

```
# Guardian: database-migration

**Role:** Read-only database schema change analyst.

## When spawned

Spawned by the ChangeSafe skill during **Phase 5 — Change-impact analysis** when a proposed
change touches `schema.sql`, `application*.properties` datasource-init keys, or any SQL file
under `src/main/resources/`.

> Maximum **two** guardians may be spawned per ChangeSafe run total. Count this guardian
> against that limit.

## Input packet (required — pass nothing else)

| Field | Content |
| --- | --- |
| `change_summary` | One-paragraph description of what changed and why |
| `bounded_question` | The single schema question to answer |
| `schema_files` | Paths to `schema.sql` and any changed SQL or properties files |
| `migration_dir` | `src/main/resources/migrations/` (may not exist yet) |

## Constraints

- **Read only.** Must not edit, create, or delete any file.
- **No full conversation history.** Work only from the input packet above.
- **No full file reads** unless a targeted grep first confirms the file is relevant.
- **Cost/stop condition:** If schema files total > 500 lines, read only the changed tables
  and their direct references. Return a partial result and flag it PARTIAL if the limit
  is reached.

## Required output

Return exactly these four sections and nothing else:

### 1. Schema change classification

State one of:
- **FRESH-INSTALL-ONLY** — the change only affects a `CREATE TABLE IF NOT EXISTS` statement
  for a table that does not exist in a production database; no `ALTER TABLE` needed.
- **REQUIRES-MIGRATION** — the change adds, removes, or modifies a column, constraint,
  or index on an existing table, or renames/drops a table.
- **ADDITIVE-NEW-OBJECT** — the change adds a new table, view, or index that does not
  exist yet; no ALTER TABLE needed; still document in a migration file for operators.
- **DATA-SEEDING-ONLY** — the change only affects demo or reference data files, not DDL.

### 2. Upgrade impact

For each changed table: list table name, column/constraint/index affected, whether existing
rows could be affected (type change, NOT NULL without DEFAULT, dropped column), and whether
H2 supports the same DDL syntax (flag H2-INCOMPATIBLE if not).

If classification is FRESH-INSTALL-ONLY or DATA-SEEDING-ONLY, write "N/A — no upgrade impact".

### 3. Migration SQL recommendation

If classification is REQUIRES-MIGRATION or ADDITIVE-NEW-OBJECT:
- Write the exact `ALTER TABLE` / `CREATE INDEX` / `CREATE TABLE` SQL for MySQL.
- Note any H2-compatible equivalent needed for the test profile.
- Include a precondition check (e.g. `SELECT COUNT(*) FROM information_schema.columns WHERE ...`)
  and an idempotency guard (`... IF NOT EXISTS` or a manual check).
- Note whether existing rows need backfilling, transformation, or NULL handling.
- Provide the recommended file name: `migrate-YYYYMMDD-<description>.sql` under
  `src/main/resources/migrations/`.
- Do not write the file; surface the SQL as text in this section only.

If classification is FRESH-INSTALL-ONLY or DATA-SEEDING-ONLY, write "N/A — no migration SQL needed".

### 4. Data safety questions

List every question the team must answer before applying the migration. For each: the question,
which rows or constraint it affects, and what happens if skipped. Examples:
- "Does any existing `orders` row have a NULL `idempotency_key`? If yes, the UNIQUE constraint
  will reject the migration."
- "Are there `order_items` rows for order IDs not in `orders`? A new FK would reject them."

Write "none" if the change is purely additive with no type changes or constraint tightening
on existing data.

## Adding more guardians

To add a new guardian, create `.bob/skills/changesafe/guardians/<name>/GUARDIAN.md`
following this same structure, then reference it from the relevant SKILL.md phase.
```

**Relevant Context:**
- `.bob/skills/changesafe/guardians/impact-explorer/GUARDIAN.md` — structural model.
- The `idempotency_key` column in `schema.sql` is the first concrete example: it was added
  by editing `CREATE TABLE` during checkout-flow-01. The guardian would have classified this
  as REQUIRES-MIGRATION and produced the migration SQL.

**Status:** [ ] pending

---

## Sub-Task 4 — Add migration convention to the project rule

**Intent:** The project-conventions rule is what Bob reads to generate code. Adding a
`## Database and schema evolution` section means future runs follow the convention without
re-investigating each time.

**Exact addition to `.bob/rules/01-project-conventions.md`:**

Insert after the `## Tests and fixtures` section and before `## UI or other relevant conventions`:

```markdown
## Database and schema evolution

- **Fresh install:** `schema.sql` uses `CREATE TABLE IF NOT EXISTS` for all tables.
  Spring Boot runs it on every startup via `spring.sql.init.mode=always` (confirmed in
  `src/main/resources/application.properties`). Safe for a fresh database; silent no-op
  on existing tables with missing columns.
- **Upgrade path — no migration tool in use:** No Flyway or Liquibase is present in
  `pom.xml`. Schema changes to existing tables are applied via manually executed SQL
  migration files. Do not add a migration tool without an explicit team decision.
- **Migration file convention:**
  - File name: `migrate-YYYYMMDD-<description>.sql` (e.g.
    `migrate-20260926-orders-add-idempotency-key.sql`).
  - Location: `src/main/resources/migrations/` (not on Spring Boot's auto-run path;
    operator applies manually or via deployment tooling).
  - Each file must include: (1) a header comment with description, affected table(s), and
    prerequisites; (2) a precondition check; (3) the migration SQL; (4) a verification query.
  - Use `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` where the target DB supports it; for
    MySQL < 8.0 use a manual `information_schema` guard instead.
- **Applied-migration record:** The operator records applied migrations in a
  `migrations_applied` table (see migration file template in Sub-Task 5).
  This table is created by the first migration that runs against a given database.
- **Editing `CREATE TABLE` is not a migration:** Editing an existing `CREATE TABLE IF NOT
  EXISTS` statement in `schema.sql` only affects fresh installs. It is not a substitute
  for an `ALTER TABLE` migration. Always produce both when a column is added to an existing
  table.
- **Data classification:**
  - `schema.sql` — DDL only; no data.
  - `example-data.sql` — H2 demo data; uses TRUNCATE; never run against MySQL.
  - `mysql-example-data.sql` — MySQL demo/reference data; uses INSERT IGNORE; upgrade-safe.
  - `src/test/resources/test-data-*.sql` — test fixtures only; never run in production.
  - `src/main/resources/migrations/migrate-*.sql` — upgrade migrations; operator-applied.
- **H2 compatibility:** H2 2.x does not support all MySQL DDL. Test any migration SQL
  under H2 or add an equivalent H2 migration in `src/test/resources/migrations/` with a
  Spring Boot profile condition.
- **Additive changes** (new column with DEFAULT, new table, new index) do not risk data
  loss. Still document in a migration file for operators.
- **Destructive changes** (column removal, type narrowing, NOT NULL on existing rows,
  constraint tightening) require a confirmed business decision, a backfill or validation
  step recorded in the migration file, and explicit human approval before the file is written.
- **Known un-migrated change:** `orders.idempotency_key VARCHAR(64) UNIQUE` was added to
  `schema.sql` during checkout-flow-01 without a companion migration file. A migration file
  for this column (`migrate-20260926-orders-add-idempotency-key.sql`) is part of this plan
  (Sub-Task 5b). Operators running a pre-checkout-flow-01 MySQL database must apply it.
- **Sources:** `src/main/resources/schema.sql`, `src/main/resources/application.properties`,
  `src/main/resources/application-mysql.properties`, `pom.xml`.
```

**Relevant Context:**
- `.bob/rules/01-project-conventions.md` — target file; currently DRAFT. Remains DRAFT
  until team review.

**Status:** [ ] pending

---

## Sub-Task 5 — Create migration file template and the first real migration file

**Intent:** Materialise the migration file format and produce the first real migration
file that documents what needs to happen for the `idempotency_key` column on any pre-existing
MySQL database.

**New directory and files:**

### 5a — Migration file template

New file: `src/main/resources/migrations/MIGRATION_TEMPLATE.sql`

```sql
-- =============================================================================
-- Migration: <YYYYMMDD>-<description>
-- Description: <what this migration does and why>
-- Affected tables: <comma-separated list>
-- Prerequisites: <other migrations that must be applied first, or "none">
-- Applied by: <operator name or automation reference>
-- Applied at: <YYYY-MM-DD HH:MM UTC>
-- =============================================================================

-- Step 0: Create applied-migration record table if not exists (first migration only)
CREATE TABLE IF NOT EXISTS migrations_applied (
    migration_id   VARCHAR(100) NOT NULL PRIMARY KEY,
    description    VARCHAR(255) NOT NULL,
    applied_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by     VARCHAR(100) NOT NULL DEFAULT 'unknown'
);

-- Step 1: Precondition check
-- Run this SELECT before applying. If it returns unexpected rows, stop and investigate.
-- Example: SELECT COUNT(*) FROM information_schema.columns
--          WHERE table_schema = DATABASE()
--          AND table_name = '<table>'
--          AND column_name = '<column>';
-- Expected result: 0 (column does not yet exist)

-- Step 2: Migration SQL
-- <ALTER TABLE / CREATE INDEX / CREATE TABLE statement>

-- Step 3: Verification
-- Run this SELECT after applying. Expected result is noted in comments.
-- Example: SELECT COUNT(*) FROM information_schema.columns
--          WHERE table_schema = DATABASE()
--          AND table_name = '<table>'
--          AND column_name = '<column>';
-- Expected result: 1

-- Step 4: Record in applied-migration table
-- INSERT INTO migrations_applied (migration_id, description, applied_by)
-- VALUES ('<YYYYMMDD>-<description>', '<same as header description>', '<operator>');
```

### 5b — First real migration file

New file: `src/main/resources/migrations/migrate-20260926-orders-add-idempotency-key.sql`

```sql
-- =============================================================================
-- Migration: 20260926-orders-add-idempotency-key
-- Description: Add idempotency_key VARCHAR(64) UNIQUE to the orders table.
--              This column was added to schema.sql during checkout-flow-01 to
--              prevent duplicate order placement on repeated form submission.
--              Operators running a MySQL database initialised before 2026-09-26
--              must apply this migration. Fresh installs already have the column
--              via schema.sql CREATE TABLE IF NOT EXISTS.
-- Affected tables: orders
-- Prerequisites: none
-- Applied by: <operator name>
-- Applied at: <YYYY-MM-DD HH:MM UTC>
-- =============================================================================

-- Step 0: Create applied-migration record table if not exists
CREATE TABLE IF NOT EXISTS migrations_applied (
    migration_id   VARCHAR(100) NOT NULL PRIMARY KEY,
    description    VARCHAR(255) NOT NULL,
    applied_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by     VARCHAR(100) NOT NULL DEFAULT 'unknown'
);

-- Step 1: Precondition checks
-- 1a. Verify the column does not yet exist.
--     Expected result: 0
--     If result is 1, the column already exists — skip Step 2 and proceed to Step 4.
SELECT COUNT(*) AS already_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'orders'
  AND column_name  = 'idempotency_key';

-- 1b. Check whether any existing orders rows would violate the UNIQUE constraint.
--     Expected result: 0 (no duplicate idempotency_key values among non-NULL rows).
--     If result > 0, investigate and resolve before continuing.
SELECT COUNT(*) AS duplicate_keys
FROM (
    SELECT idempotency_key
    FROM orders
    WHERE idempotency_key IS NOT NULL
    GROUP BY idempotency_key
    HAVING COUNT(*) > 1
) AS dupes;

-- Note: Because the column does not yet exist on a pre-migration database, the
-- subquery in 1b will fail with "Unknown column". Run 1b only after confirming
-- that idempotency_key does not yet exist (1a returns 0). This check is provided
-- for databases where the column was partially applied.

-- Step 2: Migration SQL
ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(64) NULL UNIQUE;

-- Note: Adding as NULL (not NOT NULL) is intentional. Existing orders rows have
-- no idempotency key. The application layer generates keys for new orders only;
-- historical rows legitimately have NULL. The UNIQUE constraint allows multiple NULLs
-- in both MySQL and H2.

-- Step 3: Verification
-- Expected result: 1 (column now exists)
SELECT COUNT(*) AS column_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'orders'
  AND column_name  = 'idempotency_key';

-- Step 4: Record migration as applied
INSERT INTO migrations_applied (migration_id, description, applied_by)
VALUES (
    '20260926-orders-add-idempotency-key',
    'Add idempotency_key VARCHAR(64) UNIQUE to orders table (checkout-flow-01)',
    '<operator>'
);
```

**Data safety note (explicit business question before applying):**
> Does any existing `orders` row have a non-NULL `idempotency_key` value that duplicates
> another row? This would be impossible if the column did not previously exist, but must
> be checked if the column was partially applied. See precondition 1b above.

**H2 compatibility note:**
The `information_schema` queries work in H2 2.x. The `ALTER TABLE ... ADD COLUMN IF NOT
EXISTS` syntax is **not** used here because MySQL 5.7 does not support it; the
precondition check in Step 1 is the idempotency guard instead. H2 test profile creates
a fresh schema on every run from `schema.sql`, so this migration file is never auto-run
against H2; it is operator-only for MySQL.

**Relevant Context:**
- `src/main/resources/schema.sql` line 38 — the column already in fresh-install DDL.
- `src/main/resources/application.properties` — confirms Spring Boot does not auto-run
  files under `migrations/`.
- `src/main/resources/application-mysql.properties` — MySQL production profile.

**Status:** [ ] pending

---

## Sub-Task 6 — Update Output Contract, risk-report template, and validator

**Intent:** The Output Contract governs heading text, column order, and status vocabulary.
The guardian adds a new artifact class (migration check). Rather than adding a new column
to an existing table (which would require all prior runs to be retroactively updated),
this sub-task adds a **separate artifact row** in the evidence-provenance section and
updates the Output Contract, risk-report template, and validator consistently.

**Exact changes:**

### 6a — `CHANGESAFE_OUTPUT_CONTRACT.md`

In **Section 2 (risk-report template)**, in the `## 8. Evidence provenance` table,
add one new row **after** the "After-fix test log" row:

```
| Migration check | <path to database-migration guardian output in change-impact map, or N/A — no schema change> | <timestamp or N/A> |
```

In **Section 7 (Validation before a report is called complete)**, add to the end of the
first paragraph:

> "If the risk report's change-impact map contains a `database schema` row from the
> database-migration guardian, verify that the guardian output includes a classification
> (FRESH-INSTALL-ONLY / REQUIRES-MIGRATION / ADDITIVE-NEW-OBJECT / DATA-SEEDING-ONLY)
> and that any REQUIRES-MIGRATION finding is accompanied by either a migration SQL file
> reference or an explicit `N/A — not yet written` notation with a reason."

In **Section 11 (Execution grouping, retries, and scaffold validation)**, add:

> "Migration files are not ChangeSafe run artifacts and must not be placed inside
> `changesafe/evidence/<run-id>/`. They belong in `src/main/resources/migrations/`.
> The risk report references them by repository path."

### 6b — `changesafe/templates/risk-report.md`

In `## 8. Evidence provenance`, add one row after "After-fix test log":

```
| Migration check | <path to database-migration guardian section in change-impact map, or N/A — no schema change> | <timestamp or N/A> |
```

No heading names, column order, or status vocabulary changes. This is a new table row
only.

### 6c — `changesafe/scripts/validate-output.ps1`

Add Check 10 after the existing Check 9 (bob_sessions PNG check):

```powershell
# -- Check 10: Migration check row present when schema change is mentioned -----------

if (-not $Template) {
    $RiskFile = Join-Path $RunDir "risk-report.md"
    if (Test-Path $RiskFile) {
        $RiskContent = Get-Content $RiskFile -Raw
        # Detect whether the report mentions a schema change.
        $MentionsSchema = $RiskContent -match 'schema\.sql|ALTER TABLE|CREATE TABLE|idempotency_key|migration'
        if ($MentionsSchema) {
            # Check that the evidence provenance section contains a "Migration check" row.
            $HasMigrationRow = $RiskContent -match '\|\s*Migration check\s*\|'
            Add-CheckResult `
                -Check "Migration check row present when schema change mentioned" `
                -Passed $HasMigrationRow `
                -Detail $(if (-not $HasMigrationRow) {
                    "Report mentions a schema change but has no 'Migration check' row in Section 8. Add the row or mark N/A — no schema change."
                } else { "" })
        }
    }
}
```

**Relevant Context:**
- `CHANGESAFE_OUTPUT_CONTRACT.md` — authoritative contract; must be updated first.
- `changesafe/templates/risk-report.md` — must match contract after contract update.
- `changesafe/scripts/validate-output.ps1` — must enforce the new row without breaking
  the existing check structure or exit code logic.
- No column renames, heading text changes, or status-vocabulary changes.
- The `changesafe/evidence/checkout-flow-01/risk-report.md` mentions `schema.sql` in the
  impact map; Check 10 will flag it NEEDS REVIEW because that run predates this convention.
  This is the correct behaviour: it surfaces a gap in prior evidence without failing hard.

**Status:** [ ] pending

---

## Sub-Task 7 — Update README and guardian list

**Intent:** Reflect the new guardian as implemented, add the Bobcoin budget notes, and
remove the "None are implemented" placeholder.

**Exact changes to `changesafe/README.md`:**

### `Add a Guardian` section

Replace the line:
> "Examples of future Guardians: `payment-integrity`, `database-migration`, `api-compatibility`.
> None are implemented in the current MVP; this extension model is the placeholder for them."

With:
> "**Implemented Guardians:**
> | Guardian | Trigger | Output |
> | --- | --- | --- |
> | `database-migration` | Change touches `schema.sql`, `application*.properties` datasource-init keys, or `src/main/resources/**/*.sql` | Schema classification, upgrade impact, migration SQL recommendation, data-safety questions |
>
> **Future Guardian examples (not yet implemented):** `payment-integrity`, `api-compatibility`."

### New `## Bobcoin budget notes` section

Append at the end of the file:

> ## Bobcoin budget notes
>
> **Measured session costs (this project):**
>
> | Task | Bob task ID | Bobcoins | Context fill | Primary cost driver |
> | --- | --- | --- | --- | --- |
> | Initial scaffold build | d039ddb47b0a5f8e54a8b873c4b61b22 | 15.61 | 46 % | One-time: two large spec docs + full project inspection + all templates/scripts. Non-recurring. |
> | Plan / build spec read | 12db0cbc1bb316fb0c06603e45d0f7ea | 7.48 | 9 % | Planning-only; low context. |
> | Checkout flow audit | 9b5771306d8cd2fdacf3c5b6880e5e99 | 18.80 | 78 % | Broad 10-risk investigation; both guardians; no early-stop applied; full evidence pack in-context. |
>
> **Budget monitoring (manual):**
> There is no automatic Bobcoin meter. Read the Bob IDE task consumption panel manually
> at the three checkpoints in the ChangeSafe skill (intake, post-investigation, pre-fix).
> Record the reading in the change-brief header field `Budget at intake`.
>
> **Important:** Bobcoins are not tokens. The IBM documentation does not publish a fixed
> conversion ratio. Do not assume a fixed cost per run. The per-run cap (`RUN_CAP`) and
> reserve threshold (`RESERVE_THRESHOLD`) in the skill are in Bobcoins as read from the
> Bob IDE panel.

**Relevant Context:**
- `changesafe/README.md` — target file; `Add a Guardian` section is at line 169.
- Task IDs confirmed from the three session screenshots.

**Status:** [ ] pending

---

## Verification Plan

After all sub-tasks are implemented, run these checks in order:

1. **Template validator (no regression):**
   ```powershell
   .\changesafe\scripts\validate-output.ps1 -RunDir "changesafe/templates" -Template
   ```
   Must exit 0. A failure here means a structural regression in the templates.

2. **Existing evidence validator (no new hard failures):**
   ```powershell
   .\changesafe\scripts\validate-output.ps1 -RunDir "changesafe/evidence/checkout-flow-01"
   ```
   The checkout-flow-01 run was previously NEEDS REVIEW (missing screenshots, NOT VERIFIED ACs).
   Check 10 will fire because the report mentions `schema.sql` but has no migration-check row;
   this is expected and correct — it surfaces the pre-existing gap. It must not produce any
   new FAIL checks beyond what was already failing.

3. **Guardian file structure check (manual):**
   - `.bob/skills/changesafe/guardians/database-migration/GUARDIAN.md` exists.
   - It contains exactly four output sections matching the structure of the two existing guardians.

4. **Migration file review (human):**
   - `src/main/resources/migrations/migrate-20260926-orders-add-idempotency-key.sql` is
     reviewed by the team before being applied to any database.
   - The `MIGRATION_TEMPLATE.sql` is reviewed for completeness.

5. **Convention rule review (human):**
   - The new `## Database and schema evolution` section in `01-project-conventions.md`
     is reviewed and approved before being used in any code generation. It remains DRAFT
     until that review.

6. **No application code changes, no SQL execution, no database modification:**
   All sub-tasks modify ChangeSafe workflow, documentation, and migration reference files only.
   `schema.sql`, Java source, `pom.xml`, and all evidence directories are not changed.

---

## Estimated Budget

These are estimates, not measured costs. Actual Bobcoin cost depends on context fill,
model behaviour, and file sizes.

| Sub-Task | Files Changed | Rough estimate |
| --- | --- | --- |
| 1 — Document cost drivers | 1 (README) | Low |
| 2 — SKILL phase gates | 1 (SKILL.md) | Low–Medium |
| 3 — database-migration guardian | 1 new file | Low |
| 4 — Convention rule DB section | 1 (01-project-conventions.md) | Low |
| 5 — Migration template + first migration | 2 new files | Low |
| 6 — Output Contract + template + validator | 3 files | Low–Medium |
| 7 — README updates | 1 file | Low |

Expected total: lower than Task 02 (7.48 coins, planning-only). All changes are to
documentation and workflow files; no Maven test run is needed. This estimate is advisory only.

---

## Files Affected

| File | Sub-Tasks | Nature of change |
| --- | --- | --- |
| `changesafe/README.md` | 1, 7 | New `Bobcoin budget notes` section; guardian list update |
| `.bob/skills/changesafe/SKILL.md` | 2 | Phase-level gates; per-run cap; checkpoint reminders; DB guardian routing |
| `.bob/skills/changesafe/guardians/database-migration/GUARDIAN.md` | 3 | New file |
| `.bob/rules/01-project-conventions.md` | 4 | New DB evolution section (stays DRAFT) |
| `src/main/resources/migrations/MIGRATION_TEMPLATE.sql` | 5a | New file |
| `src/main/resources/migrations/migrate-20260926-orders-add-idempotency-key.sql` | 5b | New file |
| `CHANGESAFE_OUTPUT_CONTRACT.md` | 6a | New evidence-provenance row; validation and migration-file guidance |
| `changesafe/templates/risk-report.md` | 6b | New evidence-provenance row (matching contract) |
| `changesafe/scripts/validate-output.ps1` | 6c | Check 10: migration-check row presence |

**Not changed:** `schema.sql`, `application*.properties`, any Java source, tests, `pom.xml`,
any `changesafe/evidence/` directory, `changesafe/business-rules.md`,
`changesafe/templates/change-brief.md`, `changesafe/templates/comparison.md`,
`changesafe/templates/pr-description.md`.
