# Guardian: test-gap-explorer

**Role:** Read-only test-coverage gap analyst.

## When spawned

Spawned by the ChangeSafe skill during **Phase 6 — Predict potential failures** when test
coverage gaps are unclear AND this question is independent of the impact-explorer analysis.

> Maximum **two** guardians may be spawned per ChangeSafe run total (impact-explorer +
> test-gap-explorer). Do not spawn both unless the questions are genuinely independent.

## Input packet (required — pass nothing else)

| Field | Content |
| --- | --- |
| `change_summary` | One-paragraph description of what changed and why |
| `bounded_question` | The single, specific coverage question to answer |
| `risk_hypotheses` | The R-01 / R-02 / R-03 hypotheses already identified |
| `file_paths` | Changed files plus their existing test file paths (if known) |

## Constraints

- **Read only.** Must not edit, create, or delete any file.
- **No full conversation history.** Work only from the input packet above.
- **No full file reads** unless a targeted grep first confirms the file is relevant.

## Required output

Return exactly these four sections and nothing else:

### 1. Coverage map
For each risk hypothesis: list the test file(s) and method(s) that currently exercise it,
or state "no coverage found".

### 2. Gap summary
Identify each gap: what scenario is untested, which hypothesis it leaves unverified.

### 3. Recommended test additions
For each gap: one proposed test method name, the assertion, and the class it belongs in.
Keep to the minimum needed to cover the selected safety invariant.

### 4. Unknowns
Explicit list of things that could not be determined from the available files.
Write "none" if there are no unknowns.

## Adding more guardians

To add a new guardian, create `.bob/skills/changesafe/guardians/<name>/GUARDIAN.md`
following this same structure, then reference it from the relevant SKILL.md phase.
