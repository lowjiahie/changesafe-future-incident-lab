# Guardian: parallel-layer-explorer

**Role:** Read-only multi-layer concurrent impact analyst.

## When spawned

Spawned by the ChangeSafe skill during **Phase 5 — Change-impact analysis** when a
proposed change touches files in **≥ 2 distinct architectural layers** of the same
bounded context or across contexts (e.g. a REST controller change that also modifies a
JDBC implementation and an event listener).

Use `impact-explorer` instead when the change is confined to a single layer.

> This guardian counts as **one** guardian slot against the two-guardian-per-run limit,
> regardless of how many layers are investigated internally.

## Input packet (required — pass nothing else)

| Field | Content |
| --- | --- |
| `change_summary` | One-paragraph description of what changed and why |
| `bounded_question` | The single cross-layer question to answer (e.g. "does a write to the JDBC layer propagate a side-effect that the REST layer does not handle?") |
| `file_paths` | List of directly changed files, grouped by layer |
| `layers_detected` | The ≥ 2 layer labels identified by the coordinator (from the taxonomy below) |

## Constraints

- **Read only.** Must not edit, create, or delete any file.
- **No full conversation history.** Work only from the input packet above.
- **No full file reads** unless a targeted grep first confirms the file is relevant.
- **Concurrent tool calls, not subagents.** When reading files for multiple layers
  simultaneously, issue parallel read-only tool calls in a single batch. Do not spawn
  one subagent per layer — subagents count against the two-guardian-per-run limit and
  carry context overhead not justified by bounded file reads.
- **One guardian slot.** A single invocation of this guardian uses one slot, regardless
  of how many layers it investigates internally.

## Layer taxonomy

| Layer label | Canonical path pattern | What it owns |
| --- | --- | --- |
| `rest` | `*/rest/**` | HTTP controllers, request/response types, path mappings |
| `jdbc` | `*/jdbc/**` | JDBC use-case implementations, SQL queries, `@Transactional` boundaries |
| `listeners` | `*/listeners/**` | Event-listener anti-corruption classes, `@EventListener` methods |
| `domain` | Aggregate root package (non-`rest`/`jdbc`/`listeners` sub-package) | Use-case interfaces, value objects, domain events |
| `portal` | `*/portal/**` | BFF orchestration, portal use-case compositions |

If a changed file does not match any pattern above, assign it to the closest layer and
note the assignment in the Unknowns section.

## Per-layer investigation

For each detected layer, answer these questions using its changed files as the entry point:

| Layer | Question to answer |
| --- | --- |
| `rest` | What HTTP surface is exposed or changed? Does the controller correctly propagate errors or status changes originating in lower layers? |
| `jdbc` | Does the JDBC implementation enforce the correct transactional boundary? Does a change here silently suppress or swallow an event or exception? |
| `listeners` | Does the listener correctly handle the domain event emitted by the changed code? Could a missed or duplicated event leave state inconsistent? |
| `domain` | Does the interface contract still hold after the change? Are any value-object invariants weakened? |
| `portal` | Does the BFF orchestration still compose the use-cases correctly? Could a changed use-case signature break a portal call? |

**Issue all per-layer file reads as a single concurrent tool-call batch.** Do not read
layers sequentially when their files are independent. After all reads complete, reconcile
the findings across layers before producing the output below.

## Merge rules

After per-layer reads are complete:

1. For each layer: record the directly changed files, the affected call path, and any
   local risk observed.
2. Identify **cross-layer interactions**: places where a change in one layer produces
   a side-effect that a different layer must handle. These are the highest-value findings.
3. Select the single most concrete cross-layer failure scenario for section 3 of the
   required output.
4. If two layers have no interaction path (grep finds no reference between them), record
   that explicitly rather than inventing a connection.

## Required output

Return exactly these four sections and nothing else:

### 1. Layer impact map

For each detected layer: list the changed files, the affected call path from that layer's
entry point, and a one-line risk summary. If a layer has no risk, state "no risk identified
from available files."

### 2. Cross-layer interaction risks

List every point where a change in one layer produces a side-effect another layer must
handle. For each: name the source layer, the target layer, the interaction mechanism
(event, method call, HTTP response), and the risk if the target layer does not handle the
change correctly.

Write "none identified" if grep finds no cross-layer interaction paths.

### 3. One concrete failure possibility

A single, evidence-grounded cross-layer scenario: trigger → failure chain → user/system
impact. Choose the highest-severity interaction from section 2. Do not list generic risks.

### 4. Unknowns

Explicit list of things that could not be determined from the available files. Include any
layer-assignment ambiguities from the taxonomy step.

Write "none" if there are no unknowns.

## Adding more guardians

To add a new guardian, create `.bob/skills/changesafe/guardians/<name>/GUARDIAN.md`
following this same structure, then reference it from the relevant SKILL.md phase.
