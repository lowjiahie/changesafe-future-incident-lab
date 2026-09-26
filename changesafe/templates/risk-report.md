# ChangeSafe Risk Report — <change-title>

| Field | Value |
| --- | --- |
| Run ID | <run-id> |
| Generated at | <ISO-8601 timestamp> |
| Requirement / change | <one-sentence description and source link> |
| Before-fix source state | <commit hash, or explicitly labeled working-tree state> |
| After-fix source state | <commit hash, or explicitly labeled working-tree state; N/A if no fix> |
| Bob task ID / title | <actual task reference; NOT VERIFIED if unavailable> |
| Project rule | <link to reviewed .bob/rules/01-project-conventions.md or NOT VERIFIED — awaiting review> |
| Change brief | [change-brief.md](change-brief.md) |
| Business decisions | <specific BR IDs and link to decision register, or NOT VERIFIED — awaiting clarification> |
| Report status | COMPLETE / NEEDS REVIEW / INCOMPLETE |

## 1. Goal and scope

**Goal:** <one measurable sentence>

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | <criterion> | <test or observation> |

**In scope:** <files, modules, and diff/reference>  
**Out of scope:** <explicit exclusions or N/A — none>

**Unresolved business questions:** <question IDs and implementation blocked, or N/A — none>

## 2. Cheap test gate

| Selected tests | Command | Result | Exit code | Duration (s) | Summary / raw log |
| --- | --- | --- | --- | --- | --- |
| <test names> | <exact command> | PASS / FAIL / UNKNOWN / NOT RUN | <number or N/A> | <measured or N/A> | <links or N/A> |

**Reason to stop or continue:** <one evidence-based sentence>

## 3. Change-impact map

| Component | Direct / downstream | Observed path or dependency | Evidence |
| --- | --- | --- | --- |
| <module> | <type> | <actual relationship> | <code/document link> |

## 4. Future incidents

| Risk ID | Trigger and incident | User / system impact | Evidence | Confidence | Status |
| --- | --- | --- | --- | --- | --- |
| R-01 | <specific failure chain> | <impact> | <links or NOT VERIFIED> | HIGH / MEDIUM / LOW | HYPOTHESIS / EVIDENCE-BACKED / REPRODUCED / PREVENTED / CONTAINED / UNRESOLVED |

**Selection rationale:** <why the highest-value risk was selected; N/A if none>

## 5. Selected safety contract

| Field | Value |
| --- | --- |
| Linked risk | <R-01 or N/A> |
| Safety invariant | <one precise, testable condition> |
| Adverse condition | <retry, invalid input, timeout, etc.> |
| Reproduction / regression test | <test file and test name link> |
| Expected result | <observable expectation> |
| Actual before-fix result | <observation and log link, or NOT RUN — reason> |
| Actual after-fix result | <observation and log link, or NOT RUN — reason> |

## 6. Implementation and verification

| Item | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test | <status / observation> | <status / observation> | <before and after links> |
| Relevant existing tests | <count / status> | <count / status> | <links> |
| Application behavior | <observed> | <observed> | <code/test links> |

**Approved code changes:** <short list and links, or N/A — no change>  
**Implementation approval:** <requester, scope, timestamp, and actual task reference; or PENDING — no app-code edits>  
**Project conventions followed:** <rule items and representative code/test evidence, or N/A — no generated code>  
**Approved deviations:** <reason and reviewer approval, or N/A — none>  
**Review findings:** <actual findings or N/A — not run>

## 7. Goal check and remaining risk

| ID | Criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | <same text as section 1> | MET / NOT MET / NOT VERIFIED | <actual result and link> |

**Overall goal:** MET / NOT MET / NOT VERIFIED — <one-sentence rule-based explanation>  
**Remaining risk:** <what is still unknown or N/A — none identified within scope>  
**Bob advisory recommendation:** <proceed / investigate / do not proceed, with reason; advisory only>

**Human go/no-go decision:** ____________________  
**Reviewer and date:** ____________________

## 8. Evidence provenance

| Artifact | Path / task reference | Captured at |
| --- | --- | --- |
| Before-fix test log | <real path or NOT RUN — reason> | <timestamp or N/A> |
| After-fix test log | <real path or NOT RUN — reason> | <timestamp or N/A> |
| Before/after comparison | [comparison.md](comparison.md) | <timestamp> |
| Change brief | [change-brief.md](change-brief.md) | <timestamp> |
| Evaluation hints | <actual hints and task references, or N/A — no hints supplied; NOT VERIFIED if not recorded> | <timestamp or N/A> |
| Bob consumption screenshot | <path under ../../../bob_sessions/ or NOT VERIFIED — not yet captured> | <timestamp or N/A> |
