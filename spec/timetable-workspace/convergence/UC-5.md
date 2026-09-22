# Convergence: UC-5 - Obtain a feasible repair proposal

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-5.md` at `75a6c223ee4648df7a764260236737387f89924e`
- Verdict: APPROVE
- Findings: 0 CRITICAL, 0 GAP, 0 PROTOCOL
- Suite: 159 tests, 0 failures, 0 errors, 0 skipped
- Working tree impact from verification: convergence report and status only

## Protocol Gate

1. Exactly UC-5 was submitted with status `READY_FOR_CONVERGENCE`: PASS.
2. Checkpoint, implementation, and tests are committed together at `75a6c22`: PASS.
3. Required UC-4 is `APPROVED`: PASS.
4. No other use case is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: PASS.
5. The checkpoint covers the complete UC-5 contract, applicable technical surfaces, commands, changed files, and approved-UC regressions: PASS.
6. Diff `ade3ae3..75a6c22` is attributable to the UC-5 convergence revision: PASS.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator over HTTP/PostgreSQL and packaged CLI | Feasible repair proposal | PASS | Focused `WorkspaceRepairPlanningIT` reached `SOLVING_REPAIR` and then one independently verified `REPAIR_PROPOSAL`, with exact lineage/evidence and unchanged accepted JSON. |
| Administrator over HTTP/PostgreSQL | All unsuccessful and failure paths | PASS | All five focused tests passed against PostgreSQL 18.6; every negative returned to the exact draft with no proposal/result/timetable candidate. |
| Administrator in real Chrome | Accepted inspection during solve and proposal presentation | PASS | The standard lifecycle completed the UC-5 Chrome journey using packaged replan. |
| Administrator using approved UC-4 pinning | Persisted pin feedback remains below 250 ms p95 | PASS | Samples `[220.9,93.9,71,95.2,157.4,42.2,33.8,31.5,23.6,27.3,27.6,109.5,69.1,36.5,26.6,24.5,23.5,31.5,24.2,47.4]`; p95 157.4 ms, solver time excluded. |
| Full reactor | Approved regressions plus UC-5 | 159 green | Independent `./mvnw -q clean verify` exited 0 with 159 tests and no failures, errors, or skips. |

The first focused attempt could not access Docker inside the sandbox and failed during Testcontainers initialization. The authorized rerun succeeded; this was an environment restriction, not product evidence.

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Default 30-second repair starts | Real HTTP asserts 202, `SOLVING_REPAIR`, and `PT30S` at `WorkspaceRepairPlanningIT.java:95-102`. | STRONG | yes |
| Main step 2 | Controls freeze, inspection stays available, cancel is offered | Draft PATCH is refused at `:104-107`; real Chrome inspection/cancel journey passes. | STRONG | yes |
| Main step 3 | Exact intent compiles to a complete successor for packaged `replan` | Proposal lineage equals accepted `inputRevision` and exact intent revision at `:112-116`; packaged adapter tests inspect exact inputs. | STRONG | yes |
| Main step 4 | Kernel returns complete `FEASIBLE` result and report | HTTP asserts `FEASIBLE`, exact limit, and all six change arrays at `:117-123`. | STRONG | yes |
| Main step 5 | Verified proposal/evidence appears while baseline remains accepted | Persisted/API proposal equality, run status, and accepted equality at `:124-127`; Chrome presentation passes. | STRONG | yes |
| Extension 2a | Cancel returns exact draft with no proposal | HTTP cancel proves exact draft/accepted state and proposal absence at `:256-272`; forced repair termination at `:217-229`. | STRONG | yes |
| Extension 2b | Restart restores draft with no eligible proposal | Production recovery asserts exact draft/accepted equality and run/proposal absence at `:276-288`. | STRONG | yes |
| Extension 3a | Stale identity is refused before submission | Obsolete ETag receives 412 at `:274-276` without a new run. | STRONG | yes |
| Extension 4a | `INVALID_INPUT` becomes safe draft feedback | Real completion asserts `INVALID_REFERENCE`, exact draft/accepted equality, and candidate/raw-stderr absence at `:183-204`. | STRONG | yes |
| Extension 4b | No-feasible run retains diagnostics and retry eligibility | Actual process outcome asserts exact code/diagnostics, unchanged intent/accepted state, and no proposal at `:139-149`. | STRONG | yes |
| Extension 4c | Only unchanged intent starts PT2M | Changed intent gets 409 without mutation; restored intent gets 202/PT2M and cancellation leaves no proposal at `:151-171`. | STRONG | yes |
| Extension 4d | Interrupted/transport/internal/timeout outcomes are safe | Real HTTP covers internal, transport, and interruption at `:183-205`; watchdog forces termination at `:231-237`. | STRONG | yes |
| Extension 5a | Mismatched or stale output cannot publish | Mismatch becomes `REJECTED_OUTPUT` with no candidate at `:183-204`; stale completion leaves exact JSON unchanged at `:239-253`. | STRONG | yes |
| G1 | Draft controls cannot change during a run | Server refusal and Chrome frozen controls are both exercised. | STRONG | yes |
| G2 | Only complete independently verified `FEASIBLE` output becomes proposal | Success verifies; all failure/rejection journeys assert proposal/result/timetable absence. | STRONG | yes |
| G3 | Accepted bundle is immutable for every outcome | Five integration tests compare exact accepted JSON across every terminal path. | STRONG | yes |
| G4 | Proposal contains exact identity and complete result | Field assertions at `:110-127`, including absent invented `resultRevision`. | STRONG | yes |
| G5 | Priority wording is exact and makes no optimality claim | Chrome and centralized message-catalog regressions pass. | STRONG | yes |
| G6 | Actual execution evidence and six counts are retained | HTTP and Chrome assert limit/report/evidence fields. | STRONG | yes |
| G7 | Diagnostics are run evidence, not impossibility proof | Actual no-feasible output supplies safe persisted diagnostics and no proposal. | STRONG | yes |
| Success postcondition | One exact proposal is eligible against unchanged baseline/intent | Packaged HTTP and Chrome prove state, identities, and accepted immutability. | STRONG | yes |
| Minimal guarantee | Every non-success preserves draft/accepted state and no candidate | Real HTTP negatives assert exact values and candidate absence. | STRONG | yes |
| Requires UC-4 | UC-5 consumes the approved durable draft/compiler | Repair journeys use the UC-4 route and exact intent/compiler output. | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate reactor/process boundary | Architecture/package tests pass; workspace invokes packaged CLI only. | PASS |
| RULE-2 | Pinned Java/Spring/PostgreSQL baseline | Clean Java 25 reactor used Spring Boot 4.1.1 and PostgreSQL 18.6; exclusions pass. | PASS |
| RULE-3 | Singleton guarded JSONB aggregate | All repair state/evidence remains in the one conditional PostgreSQL aggregate. | PASS |
| RULE-4 | Flyway-only schema authority | Fresh migration and deliberate migration/checksum refusal tests pass. | PASS |
| RULE-5 | Exact lifecycle transitions | Success, terminal, and refused transitions have exact state assertions. | PASS |
| RULE-6 | Strong ETag and conditional SQL | Real commands carry current ETag; stale start receives 412 without launch. | PASS |
| RULE-8 | Lossless canonical documents | Accepted JSON equality and canonical process-boundary checks pass. | PASS |
| RULE-10 | Exact process-only packaged `replan` | Explicit paths, identities, and independent verification pass. | PASS |
| RULE-11 | One async run and exact completion guard | Active conflict launches no process; stale authoritative completion cannot mutate. | PASS |
| RULE-12 | Presets and bounded cancel/watchdog | PT30S/PT2M gate, forced cancel, watchdog, and late suppression pass through HTTP. | PASS |
| RULE-13 | Private bounded process files | Permissions, distinct files, bounded output, safe correlation, and cleanup pass. | PASS |
| RULE-15 | Intent overlay without accepted mutation | Exact lineage/compiler output is consumed while accepted JSON stays equal. | PASS |
| RULE-17 | Complete proposal identity/evidence | Every named field is asserted; no result revision is invented. | PASS |
| RULE-21 | Same-origin local security | CSRF, Origin, Host, ETag, headers, and route matrix pass. | PASS |
| RULE-22 | Stable safe failures | Real failures expose safe codes and no paths, stderr, stack, or candidates. | PASS |
| RULE-23 | Safe logs and retained evidence | Captured logs and persisted success/failure evidence pass without school data. | PASS |
| RULE-24 | PostgreSQL-only HTTP/browser verification | Five HTTP tests, packaged success, Chrome, negatives, and full lifecycle pass. | PASS |
| RULE-26 | Typed kernel boundary and verifier | Architecture/verifier suites pass; only verified feasible output publishes. | PASS |
| RULE-27 | Controlled catalog/version/public APIs | Catalog, manifest, dependency, and public-API checks pass. | PASS |
| RULE-28 | Bounded input and race-safe publication | Packaged boundary/publication regressions pass. | PASS |
| RULE-29 | Reference corpus without universal promise | Repair corpus passes; text makes no universal feasibility/optimality/latency promise. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Aggregate, routes, verification, documents | Clean reactor has no UC-1 failure. | PASS |
| UC-2 | Planner/process/poll/cancel/proposal | Clean reactor has no UC-2 failure. | PASS |
| UC-3 | Accepted inspection during solving | UC-5 inspection and UC-3 browser/scale journeys pass. | PASS |
| UC-4 | Required draft, pinning, repair rendering | Functional tests pass; Chrome pin-feedback p95 is 157.4 ms against 250 ms. | PASS |

## Findings

None. Prior C-1 and G-1 through G-3 are resolved by the minimal pin response and expanded production-boundary repair journeys.

## Walkthrough

The administrator confirmed the following walkthrough:

1. Open a ready repair draft with no blocking conflict and request the default 30-second repair.
2. While solving, confirm draft controls are frozen, accepted-timetable inspection/navigation remain responsive, and Cancel is available.
3. Let it complete and confirm the proposal remains separate from the still-accepted baseline.
4. Confirm the 30-second limit, termination reason, elapsed time, six change categories, and “period stability before room-only stability before ordinary preferences,” with no optimality claim.
5. Start another repair, cancel it, and confirm the unchanged draft returns with no proposal.
6. Confirm a no-feasible outcome says only none was found within that run, shows navigable diagnostics, and offers the two-minute retry only while intent is unchanged.
7. Change intent and confirm retry is unavailable; restore the exact intent and confirm the two-minute retry starts.
8. If exercising restart/stale/failure fixtures, confirm each returns to the unchanged draft and exposes no candidate timetable.

User result: confirmed by the administrator on 2026-09-22.

## Status Update

`PENDING_WALKTHROUGH -> APPROVED`; UC-6 and UC-8 are eligible next.

## Response to execute

APPROVED
