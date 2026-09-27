# Plan: ChangeSafe Parallel Execution Improvements

## Top-Level Overview

The ChangeSafe workflow runs phases sequentially. Within two of those phases, independent
work is currently done serially even though no dependency requires it:

1. **Phase 2 — First-encounter onboarding:** when the conventions rule does not exist,
   the skill reads `README.md`, `pom.xml`, representative source files, and representative
   test files one at a time. These four reads are independent and bounded; they can be
   issued as concurrent read-only tool calls by the coordinator. No subagents are needed.
   After all reads complete, the coordinator reconciles the observations and drafts the
   conventions rule for requester approval before adopting it.

2. **Phase 5 — Impact analysis:** the `impact-explorer` guardian traces a change as a
   single linear call chain. When a change spans ≥ 2 architectural layers (REST + JDBC +
   listeners etc.), independent per-layer questions can be investigated concurrently inside
   one guardian invocation, then merged into a single impact map entry.

**This plan is deliberately scoped to Phase 2 reads and Phase 5 exploration only.**
Broader parallelism across other phases is deferred — not ruled out.

**Constraints that apply to both improvements:**
- The two-subagent invocation cap across the entire run is unchanged and still applies.
  Concurrent tool calls in Phase 2 are not subagent invocations and do not count against it.
- Parallelism must not bypass any approval checkpoint or human gate.
- Independent concurrent reads must not share mutable state or produce conflicting writes.
- Parallelism is not presented as guaranteed Bobcoin savings; actual cost depends on
  context fill, file sizes, and model behaviour.

**Scope:**
- New file: `.bob/skills/changesafe/guardians/parallel-layer-explorer/GUARDIAN.md`
- Edit: `.bob/skills/changesafe/SKILL.md` — Phase 2 first-encounter step + Phase 5 routing rule

**Non-goals (this plan):**
- No change to the two-guardian-per-run cap
- No change to `database-migration`, `test-gap-explorer`, or any other existing guardian
- No changes to templates, scripts, evidence files, or the README
- Broader within-phase concurrency in other phases is deferred, not permanently excluded

---

## Sub-Tasks

---

### Sub-Task 1 — Write the `parallel-layer-explorer` GUARDIAN.md

**Intent:**
Create the guardian spec that defines when this guardian fires, what input it receives,
how it decomposes work into per-layer concurrent investigations within a single guardian
invocation, and what output it returns to the ChangeSafe coordinator. The spec must follow
the same four-section output contract used by `impact-explorer`, `database-migration`, and
`test-gap-explorer`.

**Expected Outcomes:**
- File `.bob/skills/changesafe/guardians/parallel-layer-explorer/GUARDIAN.md` exists.
- Contains all required sections:
  - Role statement
  - When spawned (trigger: change spans ≥ 2 architectural layers)
  - Input packet table (minimal-context discipline: change summary, bounded question, file paths)
  - Constraints (read-only; no full conversation history; no full file reads without prior grep;
    does not count as two guardian slots — one invocation of this guardian uses one slot)
  - Layer taxonomy table — recognized layers and their canonical path patterns for this project
  - Per-layer investigation instructions — what each layer sub-task answers, run concurrently
    using parallel tool calls within the guardian; not separate subagent spawns
  - Merge rules — how per-layer findings are combined into one impact map entry
  - Required output (four sections: Layer impact map, Cross-layer interaction risks,
    One concrete failure possibility, Unknowns)
  - "Adding more guardians" footer (same as other guardians)

**Todo List:**
1. Confirm the four-section output contract from the existing guardian files — done during
   planning.
2. Define the canonical layer taxonomy for this project:
   - `rest` — `*/rest/**` controllers and request/response types
   - `jdbc` — `*/jdbc/**` JDBC use-case implementations
   - `listeners` — `*/listeners/**` event-listener anti-corruption classes
   - `domain` — domain-root interfaces, value objects, and events (non-implementation)
   - `portal` — `*/portal/**` BFF orchestration layer
3. Write `GUARDIAN.md` with sections in the order listed above.
4. In the per-layer instructions section, be explicit: concurrent investigation means
   issuing parallel read-only tool calls for each layer's files simultaneously — not
   spawning one subagent per layer.

**Relevant Context:**
- `.bob/skills/changesafe/guardians/impact-explorer/GUARDIAN.md` — output contract to match
- `.bob/skills/changesafe/guardians/test-gap-explorer/GUARDIAN.md` — input packet format
- `.bob/skills/changesafe/guardians/database-migration/GUARDIAN.md` — constraint style
- Project layer patterns: `src/main/java/com/ttulka/ecommerce/<domain>/<aggregate>/rest/`,
  `…/jdbc/`, `…/listeners/`, domain-root, `portal/`

**Status:** [x] done

---

### Sub-Task 2 — Update Phase 5 routing rule in SKILL.md

**Intent:**
Replace the single `impact-explorer` routing step in Phase 5 with a two-branch routing
rule:
- **Single-layer change** (files in only one layer pattern): spawn `impact-explorer` as
  before.
- **Multi-layer change** (files in ≥ 2 distinct layer patterns): spawn
  `parallel-layer-explorer` instead of `impact-explorer`.

The two-guardian-per-run cap is unchanged. The `database-migration` routing (step 4) and
the Phase 6 `test-gap-explorer` routing are untouched.

**Expected Outcomes:**
- Phase 5, step 3 (Complexity gate) in `SKILL.md` reads as a two-branch condition.
- `impact-explorer` is still referenced for single-layer changes.
- `parallel-layer-explorer` is referenced for multi-layer changes.
- The local-change bypass ("Local change — no cross-module guardian spawned") is preserved.
- No other section of `SKILL.md` is modified by this sub-task.

**Todo List:**
1. Read Phase 5, step 3 in `SKILL.md` (lines ~123–139) to capture the exact current text.
2. Replace only that step with the two-branch routing rule, preserving the local-change
   bypass text, the input packet reference, and the two-guardian cap note.
3. Verify no other section references `impact-explorer` in a way that also needs updating.

**Relevant Context:**
- `.bob/skills/changesafe/SKILL.md` lines ~123–139 (Phase 5, Complexity gate, step 3)
- The two-guardian cap note at end of Phase 5 and in Phase 6 must not change.

**Status:** [x] done

---

### Sub-Task 3 — Add Phase 2 concurrent-read instruction to SKILL.md

**Intent:**
The first-encounter branch of Phase 2 (rule does not exist) currently describes its reads
in a single sentence: "read README.md, pom.xml, representative source and test files."
This implies sequential reads. The improvement:

1. Name the four independent files/groups explicitly.
2. Instruct the coordinator to issue all four as a single batch of concurrent read-only tool
   calls — not subagents, not sequential reads.
3. Add an explicit reconcile-and-approve step: after the concurrent reads complete, the
   coordinator reconciles the observations into a draft conventions rule and requests
   requester approval before the draft is adopted.

**Why tool calls, not subagents:** These are bounded, read-only file reads. Subagents
carry their own context overhead and count against the two-subagent cap. Concurrent tool
calls are the correct mechanism for parallel file reads.

**Why the reconcile-and-approve step is required:** The coordinator observes four
independent data sources. Inconsistencies between them (e.g. a README claim that conflicts
with actual code structure) must be surfaced, not silently resolved. The requester approves
the drafted rule before it is used for any code generation or convention checking.

**Expected Outcomes:**
- Phase 2, step 1, "If it does not exist" branch in `SKILL.md` is expanded to:
  - Name the four reads: `README.md`, `pom.xml`, representative source file(s),
    representative test file(s).
  - Instruct the coordinator to issue all four reads in a single concurrent tool-call batch.
  - After reads complete: reconcile observations, surface any conflicts, draft the
    conventions rule, and request requester approval before adopting it.
- The reuse path (APPROVED + matching commit) and the DRAFT path are entirely untouched.
- No other phase in `SKILL.md` is modified by this sub-task.

**Todo List:**
1. Read the exact current text of the "If it does not exist" line in Phase 2, step 1
   (line ~85–86 in `SKILL.md`) — done during planning.
2. Expand that single line into a structured sub-instruction block inside the same branch:
   - List the four concurrent reads.
   - State: issue as a single parallel tool-call batch; do not spawn subagents for reads.
   - State: after reads complete, reconcile and draft the rule; present to requester for
     approval; do not adopt the rule until the requester approves.
3. Verify the reuse path and DRAFT path text above and below are not modified.

**Relevant Context:**
- `.bob/skills/changesafe/SKILL.md` line ~85: "If it does not exist: run the
  first-encounter onboarding pass (read README.md, pom.xml, representative source and
  test files) and draft the rule for review before continuing."
- The reconcile-and-approve requirement is consistent with Phase 8's existing human-gate
  principle: the coordinator surfaces findings; the requester decides.

**Status:** [x] done

---

## Validation

After all three sub-tasks are complete:

- New guardian file exists at `.bob/skills/changesafe/guardians/parallel-layer-explorer/GUARDIAN.md`
  and follows the four-section output contract.
- `SKILL.md` Phase 2 "If it does not exist" branch:
  - Names all four reads explicitly.
  - Contains the concurrent tool-call batch instruction.
  - Contains the reconcile-and-approve step after reads complete.
  - Reuse path and DRAFT path text are unmodified.
- `SKILL.md` Phase 5 step 3 correctly branches on single-layer vs. multi-layer.
- No stale sole reference to `impact-explorer` remains in a context where
  `parallel-layer-explorer` should also be mentioned.
- `validate-output.ps1` is a run-level validator; no script changes are needed.

---

## Constraints and Guardrails (apply to implementation)

| Constraint | Source |
| --- | --- |
| Two-subagent cap per run unchanged | User instruction |
| Phase 2 reads use concurrent tool calls, not subagents | User instruction |
| Coordinator must reconcile and request approval before adopting conventions | User instruction |
| No approval checkpoint may be bypassed | User instruction |
| Parallelism must not produce conflicting writes | User instruction |
| Bobcoin savings are not guaranteed | User instruction |
| Broader concurrency in other phases is deferred, not ruled out | User instruction |

---

## Open Decisions (resolved during planning)

| Question | Decision |
| --- | --- |
| New guardian or extend existing? | New guardian file only |
| Replaces or supplements impact-explorer? | Replaces for multi-layer changes; impact-explorer stays for single-layer |
| Two-guardian cap change? | No — stays at 2 per run |
| Phase 2 mechanism? | Concurrent tool calls in the coordinator — not subagents |
| Phase 2 reconciliation? | Coordinator drafts rule after reads; requester approves before adoption |
| Scope of this plan? | Phase 2 reads and Phase 5 exploration only; broader parallelism deferred |
| README update? | Not in scope for this plan |
