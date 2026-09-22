# Convergence: UC-7 - Keep the weekly timetable operational after a disruption

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-7.md` at `b02ce5f6bc09da9253cc46da8c4ed07adf164b19`
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical; 0 gap; 0 protocol; 0 drift; 0 cosmetic
- Suite: focused complete real-Chrome class - 10 run, 0 failed, 0 errors, 0 skipped; clean reactor - 167 run, 0 failed, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

- Exactly UC-7 was `READY_FOR_CONVERGENCE`; no other use case was `IN_PROGRESS` or `READY_FOR_CONVERGENCE`.
- The immutable submission is committed at `b02ce5f`; the original implementation, C-1 and C-2 repairs, user-selected
  non-blocking performance decision, specification/rule revision, checkpoint, and advisory Jev bundle are attributable
  through that commit. The worktree was clean before verification.
- Required UC-1 and included UC-3, UC-4, UC-5, and UC-6 are all recorded `APPROVED`.
- The checkpoint has evidence for all seven main steps, five extensions, five guarantees, both postconditions, every
  relationship, every applicable rule, changed files, focused commands, full regression, and repository hygiene.
- The submitted diff contains the two-repair operational journey and necessary shared reliability/performance work. It
  contains no UC-8 behavior or unrelated user changes.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator over HTTP/PostgreSQL/packaged kernel | Inspect, stage teacher outage plus room pin, solve, accept, stage later room outage, solve, accept | PASS | The clean reactor reproduced both production repair cycles. Each packaged `replan` produced a complete `FEASIBLE` result that independently returned `VERIFIED`; exact accepted bundles, direct lineage, transient-state cleanup, absent prior attempt pin, and both atomic acceptances passed. |
| Administrator in real Chrome | Complete two-repair native-control journey | PASS | The independent 10-test Chrome class completed both repairs and acceptances through native controls. The later ROOM repair started from the first accepted result, inherited no attempt pin, distinguished direct/ripple effects, and ended on the second accepted baseline without browser errors. |
| Administrator failure paths | Conflict, failed/cancelled solve, discard, stale proposal, durable acceptance failure | PASS | Clean PostgreSQL/HTTP suites passed the conflict, no-feasible, cancellation, invalid, internal, transport, interruption, timeout, rejected-output, stale-completion, discard, and injected rollback paths while retaining the exact accepted bundle. |
| Administrator at validation scale | Search/filter/day/selection, persisted pin feedback, proposal review | Diagnostic evidence | The unchanged 1,000-lesson browser fixture recorded raw samples and p95 values without threshold assertions. Focused p95 values were search 76.7 ms, filter 53.8 ms, day 80.2 ms, selection 68.0 ms, pin feedback 152.7 ms, and review opening 382.1 ms. The clean reactor recorded 53.1, 21.6, 64.8, 55.4, 92.3, and 315.8 ms respectively; solver time was excluded. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Inspect accepted resource context | Chrome opened the accepted whole-school workspace and entered repair from the selected resource; HTTP first compared the exact accepted snapshot in `WorkspaceRepairPlanningIT.keepsWeeklyTimetableOperationalAcrossTeacherAndRoomDisruptions`. | STRONG | yes |
| Main step 2 | Accepted timetable remains unmistakable | Chrome kept `Accepted baseline remains current` visible during draft/solve/proposal states; PostgreSQL assertions compared the complete accepted bundle throughout both cycles. | STRONG | yes |
| Main step 3 | Stage outage and protect accepted placement | Production UI and HTTP paths created teacher unavailability plus a room-only attempt pin and asserted the exact direct-effect set, one pin, no conflict, and no accepted-bundle mutation. | STRONG | yes |
| Main step 4 | Obtain complete feasible proposal without replacement | Two asynchronous packaged `replan` calls returned `REPAIR_PROPOSAL`; both complete outputs were independently verified and the accepted baseline stayed current. | STRONG | yes |
| Main step 5 | Account for changes and explicitly accept | Chrome rendered old/proposed assignments, direct effects, solver ripple effects, and explicit confirmation twice; HTTP compared each accepted definition/result exactly with its proposal. | STRONG | yes |
| Main step 6 | Show accepted result as current and reusable | Both boundaries returned to `ACCEPTED_BASELINE`, exposed repair entry, and proved draft/proposal/run cleanup after the first acceptance. | STRONG | yes |
| Main step 7 | Later repair directly parents the new result without prior pins | `WorkspaceRepairPlanningIT.java:199` proves the later `basedOnRevision` equals the first result `inputRevision`, prior attempt locks are absent, and the second proposal/acceptance uses that exact direct parent. Chrome reproduced the same later-room journey. | STRONG | yes |
| Extension 3a | Conflicting pin blocks until explicitly resolved | `WorkspaceRepairDraftIT.java:131` asserts the exact conflict, not-ready state, explicit unpin, resumed ready state, and unchanged accepted baseline. | STRONG | yes |
| Extension 4a | Failed/cancelled solve preserves baseline and resumes only with a complete proposal | `WorkspaceRepairPlanningIT.java:376` and `:420` cover retry gating and all failure/rejection classes with exact retained accepted/draft data and no partial proposal. | STRONG | yes |
| Extension 5a | Discard retains baseline and draft | `WorkspaceRepairPlanningIT.java:290` removes only the proposal and compares the retained draft and accepted trees exactly. | STRONG | yes |
| Extension 5b | Durable acceptance failure retains retryable proposal | `WorkspaceRepairPlanningIT.java:344` injects PostgreSQL failure, observes a safe response, compares exact pre-failure state, and successfully retries the unchanged proposal. | STRONG | yes |
| Extension 7a | Later different-room disruption follows the same journey | HTTP and Chrome select ROOM and period through production controls, invent no direct effects for the no-effect intent, and independently solve, review, and accept. | STRONG | yes |
| G1 | Exactly one identifiable accepted timetable | Lifecycle labels and complete accepted-bundle comparisons cover every consequential transition in both repair cycles. | STRONG | yes |
| G2 | Administrator needs no JSON or command line | Real Chrome performs inspection, disruption staging, pinning, two solves, two reviews, and two acceptances using native controls only. | STRONG | yes |
| G3 | Direct and ripple effects stay distinct | HTTP asserts direct-effect IDs; Chrome separately renders direct and solver-ripple sections for both proposals. | STRONG | yes |
| G4 | Subsequent repair uses exact newly accepted direct parent | The later draft, proposal identity, candidate definition, and second accepted bundle are compared directly with the first accepted result revision. | STRONG | yes |
| G5 | Failures preserve accepted bytes and disclose no partial replacement | Conflict, run failure/cancel/rejection, discard, stale identity, and durable-acceptance rollback tests compare complete authoritative state and safe responses. | STRONG | yes |
| Success postcondition | Complete feasible repair is current, intact, and ready for another disruption | Final HTTP and Chrome assertions prove the second verified `FEASIBLE` bundle is `ACCEPTED_BASELINE`, lineage is intact, transient state is empty, and repair entry remains available. | STRONG | yes |
| Minimal guarantee | Last durable accepted baseline survives every incomplete step | Every included negative branch asserts the exact accepted bundle remains current and identifiable with no partial candidate publication. | STRONG | yes |
| Requires UC-1 | Consume the approved accepted bundle | Both journeys start from the production singleton containing the verified definition/result/manifest bundle and compare it before mutation. | STRONG | yes |
| Includes UC-3 | Use approved whole-school inspection | Chrome begins in the accepted whole-school view and HTTP consumes the complete production snapshot. | STRONG | yes |
| Includes UC-4 | Use approved protected-draft path | Both repair cycles stage intents and pins through the production draft endpoints/UI; approved conflict, snapshot, undo, and discard regressions pass. | STRONG | yes |
| Includes UC-5 | Use approved asynchronous packaged repair | Two actual packaged `replan` processes cross POST/poll lifecycle with exact parent/candidate identities and independent verification. | STRONG | yes |
| Includes UC-6 | Use approved review and atomic decision | Chrome explicitly reviews/confirms twice; PostgreSQL assertions prove identity-bound atomic advancement, rollback, and cleanup. | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Separate contract, CLI, workspace modules and process boundary | Clean architecture/dependency checks passed; UC-7 adds no kernel linkage or alternate orchestration API. | PASS |
| RULE-2 | Pinned Java/Spring/PostgreSQL stack | Clean Java 25 reactor ran Spring Boot 4.1.1 and disposable PostgreSQL 18.6 with no dependency changes. | PASS |
| RULE-3 | One authoritative JSONB aggregate | Both repair cycles and all negative paths query and compare the singleton aggregate; scoped pin mutation retains conditional durable persistence. | PASS |
| RULE-5 | Declared lifecycle only | `ACCEPTED_BASELINE -> REPAIR_DRAFT -> SOLVING_REPAIR -> REPAIR_PROPOSAL -> ACCEPTED_BASELINE` executes twice; exhaustive transition regressions pass. | PASS |
| RULE-6 | Matching strong ETag and conditional SQL | Every command carries the current ETag; stale/missing/race tests pass exact no-mutation assertions. | PASS |
| RULE-7 | Atomic accepted-state changes | Both acceptances store exact definition/result/manifest and clear transients together; injected trigger failure rolls back completely. | PASS |
| RULE-10 | Exact packaged process-only `replan` | Launcher evidence records two packaged `replan` calls; complete input, output, identity, and verification assertions pass. | PASS |
| RULE-11 | One asynchronous run with conditional completion | 202/poll lifecycle passes twice; active-run, cancellation, recovery, retry, and late-result regressions remain green. | PASS |
| RULE-15 | Overlay compiles a complete direct successor without accepted mutation | Teacher and room intents compile complete successors; later successor has the exact new parent and no inherited attempt lock. | PASS |
| RULE-16 | Immutable effects/conflicts/pin snapshots | Exact direct sets, independent pin dimensions, no-effect room intent, conflict matrix, bulk snapshot, undo, and source-scoped behavior pass. | PASS |
| RULE-17 | Identity-bound independently verified acceptance | Both candidates are independently verified; stale/corrupt identity and durable rollback matrices refuse mutation. | PASS |
| RULE-19 | Complete server snapshot and native presentation | Complete 10-test Chrome class passes actor journeys; client uses production snapshots and cached metadata-ordered matrices without reconstructing scheduling truth. | PASS |
| RULE-20 | Accessible English native controls | Native forms, checkboxes, buttons, keyboard confirmation, narrow layout, and non-color state text pass. | PASS |
| RULE-21 | Loopback same-origin session/CSRF boundary | Full route/security matrix passes; UC-7 uses the established same-origin session and CSRF controls. | PASS |
| RULE-22 | Stable safe failures | Conflict, process, identity, and storage failures return stable safe problems without candidate or infrastructure disclosure. | PASS |
| RULE-23 | Safe observability | Packaged-run correlation/evidence tests pass and no school data logging was introduced. | PASS |
| RULE-24 | Real PostgreSQL, HTTP, packaged kernel, and browser boundaries | Independent focused Chrome and full clean reactor execute all real boundaries; Docker permission denial in the first sandboxed clean attempt was rerun successfully with Docker access. | PASS |
| RULE-25 | Diagnostic scale evidence separate from solving | Unchanged 1,000-lesson/20-sample tests report raw values, nearest-rank p95, former thresholds as diagnostics, and separate solver time. No latency value gates verification or convergence under the revised rule. | PASS |
| RULE-26 | Shared typed kernel application boundary | Typed-handler/architecture checks pass; no parallel mapper or status switch was added. | PASS |
| RULE-27 | Controlled metadata and solver APIs | Catalog/version/solver guard suites pass and UC-7 changes no controlled API. | PASS |
| RULE-28 | Bounded private race-safe process files | Missing/malformed/mismatched/late-output tests pass; both successful repair publications match exact identities. | PASS |
| RULE-29 | Reference corpus without universal feasibility claims | Kernel corpus passes; UC-7 claims only the two observed independently verified `FEASIBLE` results. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Accepted aggregate, verification, import | Clean import/verification/security/fidelity suites passed. | PASS |
| UC-2 | Async process lifecycle and atomic acceptance | Initial planning success/failure/cancel/recovery/acceptance suites passed. | PASS |
| UC-3 | Accepted browser inspection and diagnostic scale | Whole-school/keyboard/narrow/empty journeys passed; clean diagnostic p95 was search 53.1 ms, filter 21.6 ms, day 64.8 ms, selection 55.4 ms. | PASS |
| UC-4 | Protected draft, conflicts, pins, bulk snapshots, discard | Actor behavior and persistence passed; clean diagnostic pin-feedback p95 was 92.3 ms. | PASS |
| UC-5 | Repair generation and failure matrix | Packaged success/failure/cancel/retry/late-completion suites passed. | PASS |
| UC-6 | Review, identity guard, atomic accept/rollback | Review/decision/rollback suites passed; clean diagnostic review opening was 315.8 ms. | PASS |
| Kernel UC-1/UC-2 | Shared plan/replan/verify contracts and corpus | Kernel unit, architecture, CLI, and corpus suites passed in the 167-test reactor. | PASS |

## Findings

No critical, gap, protocol, drift, or cosmetic findings. The former C-3 performance finding is closed by the
administrator's explicit contract decision and the revised `RULE-25`: the measurements remain visible diagnostics,
while functional assertions, persistence, fixture cardinality, sample count, percentile calculation, and solver-time
separation remain mandatory and passed.

## Walkthrough

Automated evidence is strong, but UC-7 is a UI use case and cannot be approved before administrator confirmation.

1. Open the accepted whole-school timetable and locate a teacher with recurring assignments.
2. Start a repair, make that teacher unavailable for a recurring period, protect one accepted placement with a room
   or period pin, generate a proposal, and confirm the accepted baseline remains identified as current while solving.
3. Review that direct effects and solver ripple effects are separate, then explicitly accept the proposal.
4. Confirm the accepted result is now current and immediately start another repair from it.
5. For the second repair choose a different room disruption; confirm no attempt pin from the first repair is inherited,
   generate/review the proposal, and either accept it or exercise the declared discard path.
6. Confirm the final accepted timetable is identifiable, complete, and available for another repair.

Administrator result: pending.

## Status Update

`READY_FOR_CONVERGENCE -> PENDING_WALKTHROUGH`; UC-7 awaits the administrator walkthrough above. UC-8 remains blocked
from execution until UC-7 is approved.

## Response to execute

PENDING WALKTHROUGH: UC-7 automated convergence passed; confirm the administrator two-repair walkthrough before approval or UC-8.
