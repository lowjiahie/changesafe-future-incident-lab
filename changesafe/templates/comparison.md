# ChangeSafe Before / After — <change-title>

| Field | Value |
| --- | --- |
| Run ID | <run-id> |
| Goal | <same measurable goal as risk report> |
| Safety invariant | <same invariant as risk report, or N/A — no selected risk> |
| Before-fix state | <commit or labeled working-tree state> |
| After-fix state | <commit or labeled working-tree state, or N/A — no fix> |
| Project conventions | <link to reviewed rule; note approved deviations or N/A — none> |
| Change brief | [change-brief.md](change-brief.md) |
| Comparison captured at | <ISO-8601 timestamp> |

## Before / after results

| Measure | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test | <status and measured observation> | <status and measured observation> | <links to separate logs> |
| Relevant test suite | <tests run / failures / skipped> | <tests run / failures / skipped> | <links> |
| User-visible or domain behavior | <observed behavior> | <observed behavior> | <code/test link> |
| Execution time | <measured seconds or NOT VERIFIED> | <measured seconds or NOT VERIFIED> | <summary JSON links> |

## Goal check

| ID | Acceptance criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | <same text and ID as risk report> | MET / NOT MET / NOT VERIFIED | <actual result and link> |

**Overall goal:** MET / NOT MET / NOT VERIFIED — <same conclusion as risk report>

## What the evidence proves

<Two short sentences about the tested change and result. Do not generalize beyond the test.>

## Remaining risk

<One short paragraph; N/A — none identified within scope is allowed.>

## Presentation takeaway

<One sentence the presenter can read aloud.>
