# Convergence: UC-3 - Narrow the timetable or open a focused schedule

## Summary

- Submission: `checkpoints/UC-3.md` at `66b0561`
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: module unit suite plus three isolated Chrome/Testcontainers UC-3 journeys; all passed
- Working tree impact from verification: convergence report and status update only

## Protocol Gate

1. UC-3 was the sole target at `READY_FOR_CONVERGENCE`: pass.
2. Its checkpoint and implementation are committed together at `66b0561`: pass.
3. Required/base UC-1 is approved: pass.
4. UC-2 is `NEEDS_REVISION`, not active; no other UC is in progress or ready: pass.
5. Checkpoint evidence covers UC-3's scenario, extensions, guarantees, postconditions, rules, relationships, and validation: pass.
6. The submission diff contains only UC-3 presentation, tests, status, and checkpoint artifacts: pass.

## Runtime Reproduction

| Actor | Step or extension | Converge observed |
|---|---|---|
| Administrator | Search, explicit intersections, empty search and empty narrowed result | Scale Chrome journey retains all tiles for search-only highlighting and labels explicit intersections as narrowed. |
| Administrator | Lesson details, Class/Teacher/Room focus, empty focused schedule, and return | Scale Chrome journeys use accepted stable IDs, show Room 99 empty without invention, and retain the whole-school Day/filter context. |
| Administrator | Narrow read-only context | Shared accepted-workspace narrow renderer remains read-only and withholds repair actions. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-3 main steps 1-3, extensions 2a/3a | Search highlight and explicit intersection semantics | `WorkspaceBrowserIT.narrowsAndFocusesAcceptedTimetableInRealBrowser` on the scale snapshot | STRONG | yes |
| UC-3 main steps 4-7, extensions 4a/5a | Exact details, all focus types, empty focus, retained return | `WorkspaceBrowserIT.opensFocusedAcceptedSchedulesInRealBrowser` and `returnsFromEmptyAndNarrowFocusedSchedulesInRealBrowser` | STRONG | yes |
| UC-3 extension 4b, G1-G8, postconditions | Read-only narrow behavior and no durable mutation | Packaged browser asset journeys, existing narrow regression, and exact before/after aggregate assertions | STRONG | yes |
| RULE-1,2,4,6,7,9-12,15,16 | Shared immutable model/state path and safe presentation-only behavior | Submitted source and real-browser evidence | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1, RULE-2, RULE-4, RULE-6, RULE-7 | One shared model/state authority with identity-based filtering | `accepted-model.js`, `inspection-state.js`, `app.js`, scale Chrome tests | PASS |
| RULE-9, RULE-10 | Catalogued text, safe markup, native visible controls, non-color cues | `messages.js`, `styles.css`, browser journeys | PASS |
| RULE-11, RULE-12 | Presentation-only and unchanged security surface | No Java/configuration/migration delta; exact durable-state assertions | PASS |
| RULE-15, RULE-16 | Shared UC-1 browser path, scale fixture, walkthrough gate | Targeted real-browser journeys; user confirmation on 2026-09-23 | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Accepted model, Day/Week state, narrow renderer | Module unit suite and existing shared browser path | PASS |

## Walkthrough

User result: `It looks good`, confirmed on 2026-09-23. This confirms the UC-3 administrator walkthrough after the automated browser journeys.

## Status Update

`READY_FOR_CONVERGENCE` -> `APPROVED`; UC-2 remains the next eligible revision.

## Response to execute

APPROVED
