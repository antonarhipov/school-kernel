# Convergence: UC-1 - Inspect the accepted school in Week or Day

## Summary

- Submission: `spec/timetable-inspection-ux/checkpoints/UC-1.md` at `71d02c6`
- Verdict: REJECT
- Findings: 0 critical, 3 gaps, 0 protocol, 0 drift, 0 cosmetic
- Suite: 11 real-browser tests, 0 failures, 0 errors, 0 skips; 26 focused unit tests, 0 failures, 0 errors, 0 skips
- Working tree impact from verification: convergence report and status update only

## Protocol Gate

1. UC-1 was the only target and was `READY_FOR_CONVERGENCE`: pass before this audit.
2. The checkpoint was committed with the submitted implementation at `71d02c6`: pass.
3. UC-1 has no dependency: pass.
4. No other feature UC was in progress or ready: pass.
5. The checkpoint contains scenario and rule claims: pass, but the evidence is insufficient where recorded below.
6. The implementation diff is confined to the inspection slice and its test/status artifacts: pass.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Open accepted timetable, inspect Week, switch to Day, select lesson | `WorkspaceBrowserIT` passed | Current report records 11 Chrome/Testcontainers tests passing; the journey asserts Week/Day, a lesson inspector, narrow mode, and unchanged aggregate. |
| Administrator | Empty accepted timetable | Browser test passed | Existing browser journey asserts declared class and empty period output. |
| Administrator | Invalid, blocked, or cross-school preference | Browser test passed | No test or runtime evidence found. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-1 main steps 1-3 | Week default and Day rendering | `WorkspaceBrowserIT.java:247-264`; `app.js:356-429` | STRONG | yes |
| UC-1 main steps 4-7 | Range switch and lesson inspector | `WorkspaceBrowserIT.java:256-286`; `app.js:510-563` | STRONG | yes |
| UC-1 extension 2a | Empty assignments retain structure | `WorkspaceBrowserIT.java:311-342` | STRONG | yes |
| UC-1 extension 2b, 5b | Invalid/blocked preference fallback and failed store | no matching browser evidence | ABSENT | no |
| UC-1 extension 5a | Excluding manual Day clears selection and announces why | implementation at `app.js:238-257`; no browser assertion | WEAK | no |
| UC-1 extension 7a | Off-viewport selection | no matching browser evidence | ABSENT | no |
| UC-1 extension 2c | Narrow read-only agenda | `WorkspaceBrowserIT.java:297-305` | STRONG | yes |
| UC-1 G1, success/minimal postconditions | Aggregate unchanged | before/after assertion at `WorkspaceBrowserIT.java:307` | STRONG | yes |
| RULE-1 | Separate immutable model, state owner, Day/Week/focused renderers | immutable model exists at `accepted-model.js`, but state and both renderers remain in `app.js` | WEAK | no |
| RULE-3 | Complete semantic Week with fixed compact density | `app.js:410-427`, `styles.css`; basic browser assertion only | WEAK | no |
| RULE-5 | Bounded and robust local preference | validation code at `app.js:205-235`; no full invalid/failure/cross-school/browser-storage suite | WEAK | no |
| RULE-11 | No backend/lifecycle mutation | diff inspection plus aggregate comparison | STRONG | yes |
| RULE-13 | Every UC-1 extension at real browser boundary | browser report passes but omits specified cases above | ABSENT | no |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-2 | One accepted snapshot read/model | `app.js:29-33`, `accepted-model.js:3-21` | PASS |
| RULE-9 | Safe catalog text and tile detail | catalog additions and escaped interpolation in Week/Day renderer | PASS |
| RULE-10 | Visible native pointing controls; no custom keyboard grid | native buttons/selects; no new key handler | PASS |
| RULE-12 | Existing security surface retained | presentation-only diff; no route/security Java changes | PASS |
| RULE-16 | Exact normative fixture and human gate | no five-professional walkthrough record | PENDING after automated gaps are closed |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| Earlier accepted-workspace behavior | Existing Chrome browser suite | Current `WorkspaceBrowserIT` report: 11 passed | PASS |

## Findings

### Gaps

- **C-1 GAP — RULE-13 browser coverage is incomplete.** RULE-13 requires invalid and blocked preference storage, manual Day exclusion, and off-viewport selection through the real packaged browser boundary. The submitted browser journey covers Week/Day, empty timetable, narrow mode, and a selected tile, but has no evidence for those required branches. Add these actor-boundary journeys with aggregate/version and storage assertions.
- **C-2 GAP — RULE-1 component boundary is not implemented.** RULE-1 requires a separate presentation-state owner and Day, Week, and focused renderers. The submitted immutable model is separate, but state transitions and both `matrix`/`weekMatrix` renderers remain together in `app.js`. Extract the required modules and prove that renderers emit intent rather than write shared state directly.
- **C-3 GAP — RULE-5 preference proof is incomplete.** The implementation has defensive parsing, but there is no real-browser evidence for malformed, oversized, stale, unknown-school, unknown-weekday, blocked storage, or cross-school isolation, nor proof that no disallowed values are stored. Add an explicit storage test matrix.

## Status Update

`READY_FOR_CONVERGENCE` -> `NEEDS_REVISION`; next eligible use case: UC-1.

## Response to execute

REVISE UC-1: C-1 through C-3
