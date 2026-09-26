# ChangeSafe: Future Incident Lab — Build Plan

> Stage A output. Reviewed against `CHANGESAFE_BOB_BUILD_BRIEF.md` and `CHANGESAFE_OUTPUT_CONTRACT.md`.
> No files have been edited yet. Each sub-task below is self-contained and must be completed in order.
> After each sub-task, update its status here and wait for approval before proceeding.

## Top-level overview

**Goal:** Materialize the full ChangeSafe scaffold inside this repository so that a future Stage C
run can invoke `/changesafe` and immediately execute the 12-phase workflow without rewriting
procedures or templates.

**Scope:** Bob configuration, templates, scripts, and documentation only.
No application code, no seeded defects, no demo evidence, no commits or PRs.

**Approach:** Five sequential sub-tasks, each targeting a distinct concern.
The project-conventions rule (sub-task 1) must be human-approved before any code-generation
work proceeds. All templates are copied verbatim from the Output Contract; no competing layout
is introduced.

**Source state at plan creation:** working tree contains only
`CHANGESAFE_BOB_BUILD_BRIEF.md` and `CHANGESAFE_OUTPUT_CONTRACT.md` as untracked files;
no `.bob/` or `changesafe/` directory exists yet.

---

## Sub-task 1 — Draft project-conventions workspace rule

**Status:** [x] done

### Intent
Before any code or test is generated, Bob must record the repository's *observed* conventions
in a version-controlled workspace rule. This rule is loaded by Bob on subsequent tasks and
prevents repeated repository-wide exploration. It must be marked DRAFT and must not be used
for code generation until the team approves it.

### Expected outcomes
- `.bob/rules/01-project-conventions.md` exists with status `DRAFT`.
- Every claim in the file cites at least one representative file path.
- Uncertain or conflicting observations are in the `Unconfirmed` section, not stated as rules.
- No application code is generated or changed.

### Todo list
1. Create directory `.bob/rules/`.
2. Write `.bob/rules/01-project-conventions.md` using the **exact layout** from
   Output Contract §6 (fixed headings, DRAFT status, source state, authority block).
3. Populate each section from direct inspection only:
   - **Build and runtime:** Java 17, `.\mvnw.cmd clean test`, H2 test profile (`-Dspring.profiles.active=test`),
     Spring Boot 3.2.0. Sources: `pom.xml`, `README.md`, `src/test/resources/application-test.properties`.
   - **Architecture and package boundaries:** `com.ttulka.ecommerce.<domain>.<aggregate>.[jdbc|rest|listeners]`;
     use-case interfaces as contracts; JDBC impls package-private, wired by `@Configuration`;
     event-driven cross-domain communication; no direct service-to-service calls.
     Sources: `src/main/java/com/ttulka/ecommerce/` package tree.
   - **Code and naming style:** Lombok `@RequiredArgsConstructor`, `@NonNull`, `@Slf4j`; final fields;
     no setters; no `*Impl`/`*Service`/`*DTO`/`*Repository`/`*Entity` suffixes (ArchUnit-enforced).
     Sources: `PlaceOrderJdbc.java`, `CleanCodeArchTest.java`.
   - **Tests and fixtures:** `@JdbcTest` + `@ContextConfiguration` for JDBC-layer unit tests;
     `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `@ActiveProfiles("test")` + `@Sql` for integration;
     REST Assured for HTTP assertions; AssertJ + Mockito; H2 in-memory fixtures.
     Sources: `PlaceOrderTest.java`, `OrderWorkFlowTest.java`.
   - **UI or other relevant conventions:** Thymeleaf templates in `src/main/resources/templates/`;
     only relevant if portal/web area is touched.
   - **Unconfirmed:** retry/idempotency behavior of checkout — not confirmed safe or tested;
     stock reservation timing not explicitly documented.
4. Set `Reviewed by: PENDING`, `Approved at: PENDING`.
5. Do **not** proceed to sub-task 2 implementation of code/tests until the team approves this rule.

### Relevant context
- Output Contract §6 — fixed rule layout (mandatory headings, DRAFT/APPROVED vocabulary)
- Build Brief §"First-encounter project-convention protocol"
- `src/test/java/com/ttulka/ecommerce/CleanCodeArchTest.java` — naming enforcement
- `src/test/java/com/ttulka/ecommerce/CleanModulesArchTest.java` — module boundaries
- `src/test/java/com/ttulka/ecommerce/OrderWorkFlowTest.java` — integration test pattern
- `src/test/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderTest.java` — unit test pattern

---

## Sub-task 2 — Bob configuration: mode, skill, command, and .bobignore

**Status:** [x] done

### Intent
Create the Bob-native configuration that makes ChangeSafe discoverable in the IDE:
a custom mode that enforces the analyze-first / checkpoint-before-edits policy, a skill that
encodes the 12-phase workflow (matching the approved flowchart), a `/changesafe` command entry
point, and a `.bobignore` that protects relevant source while excluding build noise.

### Expected outcomes
- `.bob/custom_modes.yaml` defines a `changesafe` mode using supported Bob tool groups.
- `.bob/skills/changesafe/SKILL.md` contains the full 12-phase workflow procedure, the two
  gap fixes from the diagram review (explicit revise-brief loop; named "hand off locally" terminal),
  and references to templates/scripts by relative path.
- `.bob/commands/changesafe.md` is a short entry point that activates the skill; it does not
  duplicate the full skill instructions.
- `.bobignore` excludes `target/`, `.idea/`, `*.class`, `*.jar`, Docker volumes, and any
  discovered local secret patterns; it does **not** exclude `changesafe/`, `.bob/`, `src/`, or
  `bob_sessions/`.
- A human-readable note in the README (written in sub-task 4) confirms that `/changesafe`
  command and mode discovery must be verified in the installed IDE; file existence alone is
  insufficient.

### Todo list
1. Create `.bob/custom_modes.yaml` with a `ChangeSafe` mode entry:
   - `roleDefinition`: analyze-first coordinator; requires explicit human checkpoint before
     app-code edits; evidence-based outputs; selective context use.
   - `groups`: include read-file tools and safe analysis tools; restrict edit/execute tools
     to require approval; follow installed Bob's supported group names exactly.
   - Do not override or shadow Bob's built-in `plan`, `agent`, or `ask` modes.
2. Create `.bob/skills/changesafe/SKILL.md`:
   - Open with a one-paragraph purpose statement.
   - List all 12 phases in order matching the approved flowchart diagram:
     1 Receive requirement → 2 Understand project → 3 Understand business rules →
     4 Plan + lightweight PRD → 5 Impact analysis → 6 Predict failures →
     7 Generate/reuse tests + baseline → 8 Present + approval checkpoint →
     (if not approved → deliver analysis-only; if revise → return to step 4) →
     develop focused changes → 9 Replay + coverage → new ambiguity check →
     required-checks gate → 10 Review + AC evaluation → rework loop →
     11 Deliver evidence pack → PR authorization check →
     (no auth → hand off locally with remaining risks) →
     12 Commit/push/PR → Human review and merge decision.
   - Cross-cutting rule: business clarification can interrupt at any phase; pause dependent
     implementation; safe independent analysis continues.
   - Token efficiency rules: cheap intake first; script before deep reasoning; two-pass analysis;
     ≤3 hypotheses; 0–2 explore subagents (never `general`); meter Bobcoins at three checkpoints.
   - Reference templates by path: `changesafe/templates/`, scripts by `changesafe/scripts/`.
3. Create `.bob/commands/changesafe.md` — short front matter + one-sentence activation
   instruction pointing at the skill. No full workflow duplication.
4. Create `.bobignore`:
   - Exclude: `target/`, `.idea/`, `.mvn/`, `*.class`, `*.jar`, `docker-compose.yml` (runtime only),
     any `*.log` files outside `changesafe/evidence/` (to avoid attaching build noise).
   - Explicitly retain (do not ignore): `src/`, `.bob/`, `changesafe/`, `bob_sessions/`,
     `CHANGESAFE_BOB_BUILD_BRIEF.md`, `CHANGESAFE_OUTPUT_CONTRACT.md`.
   - Add a comment block explaining the rationale for each exclusion group.

### Relevant context
- Build Brief item 1 (custom mode), item 2 (skill), item 3 (command), item 8 (.bobignore)
- Build Brief §"Agent structure and responsibilities" — mode/role boundaries
- Build Brief §"Token-efficient design requirements" — selective by default
- Bob docs: Custom modes, Skills, Slash commands (links in Build Brief §"Primary IBM Bob documentation")
- Approved flowchart diagram (two gap fixes: revise loop, hand-off terminal)
- Output Contract §6 last bullet — mode/command discovery must be verified in IDE

---

## Sub-task 3 — Templates: verbatim Output Contract materialization

**Status:** [x] done

### Intent
Materialize every fixed template from the Output Contract as an actual file in
`changesafe/templates/`. These are the canonical forms for every ChangeSafe run; their headings,
column order, and status vocabulary must not be altered. The live business-rules register is also
created here as an empty (not fictional) file.

### Expected outcomes
- `changesafe/templates/risk-report.md` — exact Output Contract §2 template.
- `changesafe/templates/comparison.md` — exact Output Contract §3 template.
- `changesafe/templates/change-brief.md` — exact Output Contract §8 template.
- `changesafe/templates/business-rules.md` — exact Output Contract §9 template (placeholder rows only).
- `changesafe/templates/pr-description.md` — exact Output Contract §10 template.
- `changesafe/business-rules.md` — live register with same headings as the template but **empty tables**
  (no placeholder rows, no invented decisions).
- `bob_sessions/INDEX.md` — fixed table layout per Output Contract §5; completeness-check line;
  no PNG placeholders.

### Todo list
1. Create `changesafe/templates/` directory.
2. Write `changesafe/templates/risk-report.md` by copying the fenced block from Output Contract §2
   verbatim. Do not alter any heading text, column name, or status value.
3. Write `changesafe/templates/comparison.md` from Output Contract §3 verbatim.
4. Write `changesafe/templates/change-brief.md` from Output Contract §8 verbatim.
5. Write `changesafe/templates/business-rules.md` from Output Contract §9 verbatim
   (template placeholder rows are permitted here).
6. Write `changesafe/templates/pr-description.md` from Output Contract §10 verbatim.
7. Write `changesafe/business-rules.md` — same section headings as the template; tables present
   but with zero data rows (not even placeholder rows). Add a header comment:
   "Never invent confirmed decisions. Add rows only when a requester explicitly confirms a rule."
8. Create `bob_sessions/` directory and write `bob_sessions/INDEX.md` using the fixed layout
   from Output Contract §5. Leave the table body empty (no fabricated task rows).
   Include the completeness-check line: "0 task sessions / 0 consumption screenshots. Missing: none yet."

### Relevant context
- Output Contract §2 (risk-report), §3 (comparison), §4 (test-summary JSON shape — referenced
  by scripts not templates), §5 (bob_sessions), §8 (change-brief), §9 (business-rules), §10 (PR description)
- Build Brief item 9 — "materialize as initially empty decision register; never invent confirmed rules"
- Build Brief §"Required output formats" — "copy templates verbatim; validate completed reports against it"

---

## Sub-task 4 — Scripts: test gate and output validator

**Status:** [x] done

### Intent
Create two deterministic PowerShell scripts that give the workflow its mechanical backbone:
`run-targeted-tests.ps1` runs selected Maven tests with the H2 profile and saves a non-overwriting
log plus a JSON summary; `validate-output.ps1` checks completed run directories for structural
compliance without claiming business correctness.

### Expected outcomes
- `changesafe/scripts/run-targeted-tests.ps1`:
  - Accepts a run-id and one or more test class/method names as parameters.
  - Runs `.\mvnw.cmd test -Dspring.profiles.active=test -Dtest=<selected>` (or equivalent `-pl` scoping).
  - Saves full output to `changesafe/evidence/<run-id>/logs/` with a timestamped, non-overwriting filename.
  - Parses Maven Surefire XML from `target/surefire-reports/` for test counts; uses `null` if XML absent.
  - Writes `changesafe/evidence/<run-id>/before-test-summary.json` or `after-test-summary.json`
    using the exact JSON shape from Output Contract §4.
  - Prints a short PASS/FAIL/UNKNOWN summary: command, selected tests, result, exit code,
    elapsed time (seconds), log path.
  - Returns meaningful exit code: 0 = PASS, 1 = FAIL, 2 = UNKNOWN/error.
  - Never treats a skipped test as PASS; never sends output to an external service.
  - If no test XML is found (compile failure, no tests matched), result = UNKNOWN not PASS.
- `changesafe/scripts/validate-output.ps1`:
  - Accepts a run directory path as parameter.
  - Checks: required files present (`risk-report.md`, `comparison.md`, `change-brief.md`,
    `before-test-summary.json` or `after-test-summary.json` when applicable, log files).
  - Checks: all required headings present in each Markdown file in the correct order.
  - Checks: status values use only the controlled vocabulary (PASS/FAIL/UNKNOWN/NOT RUN;
    MET/NOT MET/NOT VERIFIED; HYPOTHESIS/EVIDENCE-BACKED/REPRODUCED/PREVENTED/CONTAINED/UNRESOLVED).
  - Checks: AC IDs and criterion text match between `change-brief.md`, `risk-report.md §1`,
    and `risk-report.md §7`; and between `risk-report.md` and `comparison.md`.
  - Checks: relative Markdown links in report files resolve to actual files (link target exists).
  - Checks: JSON summary files have correct keys and correct value types (string/int/null).
  - Checks: before and after log/summary files are distinct (not the same content).
  - Checks: `bob_sessions/` contains at least one `.png` file; reports NOT VERIFIED if absent.
  - Prints a structured PASS/FAIL per check; exits 0 only if all checks pass.
  - Cannot certify business correctness, human approval authenticity, or screenshot content.
  - Template validation mode (flag `--template`): placeholders are allowed; exits 0 if structure correct.
  - Completed-run validation mode (default): unresolved placeholders are rejected; explicit
    NOT VERIFIED / NOT RUN values are allowed.

### Todo list
1. Create `changesafe/scripts/` directory.
2. Write `changesafe/scripts/run-targeted-tests.ps1`:
   - Parameter block: `-RunId` (required), `-Tests` (required, comma-separated),
     `-Phase` (required, `before-fix` or `after-fix`), `-ProjectRoot` (optional, defaults to repo root).
   - Build the Maven command string; measure elapsed time with `Measure-Command` or `[System.Diagnostics.Stopwatch]`.
   - Capture all output to a timestamped log file under `changesafe/evidence/<run-id>/logs/`
     (filename: `<phase>-<yyyyMMdd-HHmmss>.log`). Never overwrite.
   - After execution, attempt to parse `target/surefire-reports/*.xml`; extract tests/failures/errors/skipped.
   - Write JSON summary using the exact keys from Output Contract §4.
   - Print the short summary block to stdout.
   - Set exit code from result value.
3. Write `changesafe/scripts/validate-output.ps1`:
   - Parameter block: `-RunDir` (required path), `-Template` (switch for template mode).
   - Define expected headings for each template file as arrays.
   - Define allowed status vocabularies as hash tables.
   - For each check, write a helper function that returns pass/fail + message.
   - Print results as a table; final line: overall PASS or FAIL with count.
   - Exit 0 on all pass, 1 on any failure.
4. Add a comment header to each script: purpose, parameters, example invocation,
   exit codes, and limitations (especially the validator's inability to certify business facts).

### Relevant context
- Build Brief item 6 (test gate requirements), item 10 (validator requirements)
- Output Contract §4 (exact JSON shape and key names)
- Output Contract §7 (what the validator must and must not check)
- Output Contract §11 (execution grouping, retry, scaffold vs. real-run separation)
- Build Brief §"Token-efficient design requirements" rule 2 — "test script before deep reasoning"
- `src/test/resources/application-test.properties` — H2 test profile connection details
- `pom.xml` — Maven Surefire plugin behavior (Spring Boot parent includes it by default)
- Windows/PowerShell constraint: use `.\mvnw.cmd` not `./mvnw`

---

## Sub-task 5 — Developer documentation: changesafe/README.md

**Status:** [x] done

### Intent
Write the single human-facing developer guide that explains how to invoke ChangeSafe,
interpret its outputs, extend it with a Guardian, and understand what it cannot do.
This file is created once and updated when behavior changes; it is not regenerated per run.

### Expected outcomes
- `changesafe/README.md` with exactly these headings (from Output Contract §6):
  `What ChangeSafe Does`, `Prerequisites`, `Run ChangeSafe`, `Understand the Outputs`,
  `Add a Guardian`, `Safety and Limitations`.
- Each section is accurate, concise, and grounded in the actual scaffold created in sub-tasks 1–4.
- The file explicitly states: (a) mode and command discovery must be verified in the installed
  Bob IDE; (b) the project-conventions rule must be approved before code generation; (c) no
  application code is changed until human approval; (d) `bob_sessions/` screenshots are
  manually captured — ChangeSafe cannot generate them.
- The Guardian extension model is documented as a convention (trigger, scope, evidence format,
  cost/stop condition) without implementing any Guardian beyond what was built.
- The boundary between Bob IDE tooling and the Java Spring Boot application is clearly stated.

### Todo list
1. Create `changesafe/README.md` with the six fixed headings.
2. **What ChangeSafe Does:** two short paragraphs — the Future PreMortem concept;
   the Incident-to-Contract loop; what it produces (evidence pack, not a release gate).
3. **Prerequisites:** Java 17 + Maven (same as app); Bob IDE with ChangeSafe mode/skill loaded;
   project-conventions rule reviewed and approved; H2 test profile working locally.
4. **Run ChangeSafe:** step-by-step: (1) open a Bob task in ChangeSafe mode or activate the skill;
   (2) provide the change or diff; (3) follow the 12-phase workflow; (4) scripts are in
   `changesafe/scripts/` — how to call them; (5) evidence goes in `changesafe/evidence/<run-id>/`.
5. **Understand the Outputs:** table mapping each output file to its purpose and template source.
   Explain the status vocabulary (MET/NOT MET/NOT VERIFIED; PASS/FAIL/UNKNOWN; incident lifecycle).
   Note that `bob_sessions/` screenshots are manually captured.
6. **Add a Guardian:** document the extension convention a contributor must follow:
   create `.bob/skills/changesafe/guardians/<name>/GUARDIAN.md`; define trigger condition,
   read/edit permissions, evidence output format, and cost/stop condition;
   register it in the skill's Guardian routing section; the coordinator selects it only when
   the change matches its domain. Do not implement a Guardian here unless one was built.
7. **Safety and Limitations:** bullet list — human approval required for app-code edits;
   reports are advisory; validator checks structure not business correctness; screenshots are
   manually captured; no external service calls; PR requires explicit authorization;
   test results do not constitute release approval.
8. Add a footer: attribution to Tomas Tulka's original project and MIT license reference.

### Relevant context
- Output Contract §6 — fixed headings for README (mandatory)
- Build Brief item 5 — "how another developer runs the workflow, adds a specialized skill, understands the boundary"
- Build Brief §"Guardian Skills" — extension model conventions
- Build Brief §"Evidence capture — human-owned, not assumed automatic"
- Sub-tasks 1–4 outputs — README must accurately describe the actual scaffold, not a hypothetical one

---

## Implementation notes for agent mode

When switching to agent mode to implement this plan:

1. Use `start_subtask` for each sub-task in order.
2. Each subtask prompt must include: "Read `changesafe-plan.md` for full context. Implement
   sub-task N only. Do not change application code. Update the sub-task status to `[x] done`
   in the plan file after completion."
3. After sub-task 1, **pause and ask the user to review `.bob/rules/01-project-conventions.md`
   before proceeding**. The rule must be approved before any generated code or tests touch
   the Java codebase.
4. Sub-tasks 2, 3, 4, and 5 may proceed after the rule is approved (or if they produce no
   Java code — sub-tasks 3, 4, and 5 are safe to continue regardless since they produce only
   templates, scripts, and documentation).
5. After all sub-tasks: run `changesafe/scripts/validate-output.ps1 --template` on each
   template file to verify structural compliance. Report results before closing the task.

## Explicitly deferred (not in scope for this plan)

- Stage C demo run: evidence directories, filled templates, actual test logs.
- `bob_sessions/` PNG screenshots — human-captured only.
- `.bob/agents/change-impact.md` — requires IDE version verification first.
- Lifecycle hooks — after MVP is complete.
- Guardian implementations beyond the extension convention.
- Application code changes, defect seeding, or repair demo.
- Commits, pushes, or PR creation.
