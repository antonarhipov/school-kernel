# Convergence: UC-2 - Trace subject teaching and teacher load

## Summary

- Submission: `checkpoints/UC-2.md` at `90fe265`
- Verdict: REJECT
- Findings: 0 critical, 1 gap, 0 protocol
- Suite: 26 unit tests plus the UC-2 real-browser test passed; the complete `WorkspaceBrowserIT` class was not completed
- Working tree impact from verification: none

## Protocol Gate

1. UC-2 was the sole `READY_FOR_CONVERGENCE` target and its committed checkpoint exists at `90fe265`.
2. Required/base UC-1 is `APPROVED` in `convergence/UC-1.md`.
3. No other UC was in progress or ready.
4. The implementation diff contains the model, state, native presentation, styles, real-browser test, status, and checkpoint for UC-2 only.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Subject, teacher, combined, filter, ribbon, zero-range, clear, and no-mutation paths | Packaged browser journey | `WorkspaceBrowserIT#tracesSubjectTeachingAndTeacherLoadInRealBrowser`: 1 passed; Chrome and Testcontainers PostgreSQL used |
| Administrator | UC-1 shared accepted timetable path | Shared regression | `WorkspaceBrowserIT#inspectsAcceptedWholeSchoolTimetableInRealBrowser`: 1 passed; Chrome and Testcontainers PostgreSQL used |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-2 main steps 1-5 | Exact subject/teacher highlights, distinct dual cue, counts, ribbon, range retention | `WorkspaceBrowserIT.java:328-366` drives the live packaged UI and checks Math/Alex, dual match, all ribbon states, and Day retention | STRONG | yes |
| UC-2 main steps 6-9 | Explicit filters intersect and clear independently | `WorkspaceBrowserIT.java:353-398` drives native controls, proves selection clearing, single/intersected modes, clear-one, and clear-all | STRONG | yes |
| UC-2 extensions 2a, 4a, 6a, 7a | Zero ranges, no teacher assignment, empty intersection, filtered selection | `WorkspaceBrowserIT.java:353-392` exercises Monday/Tuesdays, exact zero counts, active criteria, unavailable/available ribbon state, and closed inspector | STRONG | yes |
| UC-2 G1-G7 | Identity-only matching, non-destructive cues, model counts, authoritative availability, ephemeral state, non-color controls | `accepted-model.js:3-45`; `inspection-state.js:4-63`; `app.js:604-715` | STRONG | yes |
| UC-2 G8/success/minimal guarantee | Real browser uses production path and preserves durable document | `WorkspaceBrowserIT.java:313-400` compares exact database document before/after | STRONG | yes |
| RULE-1, RULE-2, RULE-4, RULE-6-RULE-12, RULE-14, RULE-16 | Native model/state/render path, no backend delta, catalog text, isolated browser fixture | Submitted diff and focused Chrome/PostgreSQL tests | STRONG | yes |
| Full relevant suite and approved related-UC regression | Full `WorkspaceBrowserIT` suite rerun | Only targeted UC-2 and UC-1 browser methods were completed; long class could not finish before this environment terminated its command window | ABSENT | no |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1, RULE-2 | One native immutable model and one accepted snapshot read | `accepted-model.js:3-45`; no Java route/migration change in submitted diff | PASS |
| RULE-4, RULE-6 | One state authority and stable lesson identity | `inspection-state.js:4-63`, `app.js:542-715` | PASS |
| RULE-7, RULE-8 | Exact stable-ID cues/counts and authoritative availability | `app.js:648-715`; real-browser assertions at `WorkspaceBrowserIT.java:328-392` | PASS |
| RULE-9-RULE-12 | Catalogued safe presentation, native controls, and no boundary/security delta | `messages.js`, `styles.css`, submitted diff, durable-state comparison | PASS |
| RULE-14 | UC-2 begins from shared UC-1 production path and covers required browser branches | Targeted real-browser path passed | PASS |
| RULE-16 | Isolated fixture and walkthrough gate | Isolated fixture used; human walkthrough remains pending | PENDING |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Accepted model, range state, whole-school rendering | Its direct real-browser journey passed after UC-2 | PASS |
| Other approved workspace use cases | Shared browser class | Full class not completed in this environment | GAP |

## Findings

### G-1 GAP - Full applicable browser regression was not completed

UC-2's executor protocol requires the full relevant suite and approved related-UC regression. The focused UC-2 and direct UC-1 Chrome/PostgreSQL journeys are strong, but the complete `WorkspaceBrowserIT` class is still absent because the environment terminates its command window after 30 seconds. Re-run `mvn -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT verify` in a persistent execution environment, inspect its final count, and reconverge.

## Status Update

`READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`; UC-3 is not eligible while G-1 remains open.

## Response to execute

REVISE UC-2: G-1 full WorkspaceBrowserIT regression is required before the UI walkthrough gate.
