# Convergence: UC-5 - Obtain a feasible repair proposal

## Summary

- Submission: `spec/timetable-workspace/checkpoints/UC-5.md` at `340cf31b846d3f66952de9d289855677dd734005`
- Verdict: REJECT
- Findings: 1 CRITICAL, 3 GAP, 0 PROTOCOL
- Suite: 157 tests, 1 failure, 0 errors, 0 skipped
- Working tree impact from verification: none

## Protocol Gate

1. Exactly UC-5 was submitted with status `READY_FOR_CONVERGENCE`: PASS.
2. Checkpoint and implementation are committed together at `340cf31`: PASS.
3. Required UC-4 is `APPROVED`: PASS.
4. No other use case is `IN_PROGRESS` or `READY_FOR_CONVERGENCE`: PASS.
5. The checkpoint contains rows for the complete UC-5 contract, applicable rules, commands, changed files, and regressions: PASS.
6. Diff `c7ea4f8..340cf31` is attributable to UC-5 and necessary UC-4 solve-guard regression work: PASS.

Jev remained advisory `REVIEW`: its 37-item bundle validates locally, but no external report exists because repository-excerpt disclosure was not authorized. It did not affect any evidence grade.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator over packaged process adapter | Exact replan inputs, presets, verification, and change report | PASS | Focused `KernelPlannerTest`: 7 tests, 0 failures/errors/skips. Exact replan success and `NO_FEASIBLE_SOLUTION_FOUND` mapping reproduced. |
| Administrator over HTTP/PostgreSQL | Feasible repair, retry gate, cancellation, stale start, and recovery | PASS | Focused `WorkspaceRepairPlanningIT`: 3 tests, 0 failures/errors/skips against PostgreSQL 18.6 and packaged CLI. The feasible run reached `REPAIR_PROPOSAL`; cancellation/recovery retained exact accepted and draft JSON. |
| Administrator over full reactor/browser | UC-1 through UC-5 regressions | 157 green | `./mvnw -q clean verify` ran 157 tests but failed `WorkspaceBrowserIT.measuresTargetScalePinFeedbackInRealBrowser`: p95 274.5 ms exceeded 250 ms. |
| Administrator after unsuccessful repair | Production run creates diagnostics and unchanged-intent retry eligibility | PASS claimed | Not reproduced: the test writes a synthetic `lastRun` directly with SQL at `WorkspaceRepairPlanningIT.java:124-135`, bypassing `KernelPlanner.replan` and `RepairPlanningService.persistOutcome`. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Default 30-second repair starts | Real HTTP test asserts 202, `SOLVING_REPAIR`, and `PT30S` at `WorkspaceRepairPlanningIT.java:79-87`; packaged and browser reproductions passed. | STRONG | yes |
| Main step 2 | Older proposal invalidated, controls frozen, navigation responsive, cancel available | HTTP draft mutation receives 409 at `WorkspaceRepairPlanningIT.java:89-92`; Chrome observes accepted search and no draft controls while solving at `WorkspaceBrowserIT.java:397-402`. | STRONG | yes |
| Main step 3 | Exact intent compiles to complete successor and packaged replan receives immutable accepted pair | `RepairPlanningService.java:73-108` deep-copies accepted pair and compiler output; `KernelPlannerTest.java:214-243` inspects exact four path arguments/files. | STRONG | yes |
| Main step 4 | Kernel returns complete FEASIBLE result and authoritative report | Real packaged HTTP and browser runs reached verified FEASIBLE; exact six arrays and identities are asserted at `WorkspaceRepairPlanningIT.java:94-108`. | STRONG | yes |
| Main step 5 | Verified proposal with identity/evidence appears while prior baseline remains accepted | Persisted/API proposal equality and accepted equality at `WorkspaceRepairPlanningIT.java:95-112`; visible Chrome labels at `WorkspaceBrowserIT.java:403-415`. | STRONG | yes |
| Extension 2a | Cancel returns exact draft with no proposal | Real HTTP cancellation compares exact draft/accepted JSON and proposal absence at `WorkspaceRepairPlanningIT.java:173-180`; late packaged process logs cancellation. | STRONG | yes |
| Extension 2b | Restart restores draft with no eligible proposal | Production recovery is invoked after persisted solving state and exact state is asserted at `WorkspaceRepairPlanningIT.java:184-196`. | STRONG | yes |
| Extension 3a | Stale draft/baseline identity is refused before submission | Obsolete ETag receives 412 at `WorkspaceRepairPlanningIT.java:181-184`; conditional SQL and explicit `basedOnRevision` guard prevent submission. | STRONG | yes |
| Extension 4a | INVALID_INPUT becomes safe navigable feedback and unchanged draft | Only adapter-level failure mapping is exercised. No repair run reaches `REPAIR_DRAFT` through HTTP with persisted validation feedback and exact no-mutation/no-disclosure assertions. | MISPLACED | no |
| Extension 4b | No-feasible run creates exact diagnostics/retry offer with no proposal | `WorkspaceRepairPlanningIT.java:124-135` manufactures `lastRun` by direct SQL rather than executing the unsuccessful production branch. UI/persistence claims can pass if production never stores those diagnostics. | MISPLACED | no |
| Extension 4c | Unchanged intent starts a new PT2M run | Given the arranged prior state, real HTTP proves changed intent refusal, unchanged intent 202/PT2M, cancellation, and proposal absence at `WorkspaceRepairPlanningIT.java:144-161`. | STRONG | yes |
| Extension 4d | Interrupted/transport/internal failure restores exact draft safely | Process-adapter tests cover outcome mapping, but no real repair HTTP run proves the service completion, persisted safe outcome, exact draft/accepted equality, and proposal absence. | MISPLACED | no |
| Extension 5a | Incomplete/mismatched/stale/unverified result is rejected without exposure | Adapter result checks and completion guards are inspected, but no repair HTTP journey alters each identity/result and proves no candidate disclosure or proposal transaction. | MISPLACED | no |
| G1 | Draft controls cannot change during run | Server rejects a real draft PATCH with 409 and Chrome removes editing controls. | STRONG | yes |
| G2 | Only complete independently verified FEASIBLE output becomes proposal | Success is strong; failure/rejection absence is not proven at the actor boundary for extensions 4a, 4d, and 5a. | WEAK | no |
| G3 | Accepted bundle remains immutable for every outcome | Feasible, cancellation, retry cancellation, stale start, and recovery are exact; invalid/transport/internal/rejected-output service outcomes are unexercised. | WEAK | no |
| G4 | Proposal contains every exact identity and complete result | Field-by-field persisted/API assertions at `WorkspaceRepairPlanningIT.java:95-112`, including absent invented `resultRevision`. | STRONG | yes |
| G5 | Priority wording is exact and never claims optimality | Chrome asserts required priority text; catalog inspection finds no optimal/global-minimum claim. | STRONG | yes |
| G6 | Actual limit/termination/elapsed/six counts are retained | Production copies verified result evidence at `RepairPlanningService.java:211-232`; HTTP and Chrome assertions verify fields/categories. | STRONG | yes |
| G7 | Diagnostics are run evidence, not impossibility/recipe proof | Wording is safe, but the tested diagnostics are manually inserted rather than produced by the run. | WEAK | no |
| Success postcondition | One exact proposal is eligible against unchanged accepted/intent identities | Real packaged HTTP and Chrome journeys prove the state and identity. | STRONG | yes |
| Minimal guarantee | All non-success outcomes preserve exact accepted/draft data and no candidate eligibility | Cancellation/stale/recovery are strong; invalid, failed, interrupted, and rejected result service paths remain below-boundary evidence only. | WEAK | no |
| Requires UC-4 | UC-5 consumes approved durable draft/compiler output | Real tests create the draft through the UC-4 HTTP route and carry its exact intent revision/compiler output into UC-5. | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-3 | Singleton relationally guarded JSONB aggregate | Repository/mutation inspection and PostgreSQL journeys show run/draft/proposal/evidence in one document with lifecycle/version/run guards. | PASS |
| RULE-5 | Exact lifecycle transitions; every other transition refused without side effects | Main, draft-freeze, cancel, and recovery paths are exercised transactionally; unrelated states remain guarded by conditional SQL. | PASS |
| RULE-6 | Strong ETag plus matching conditional SQL version | Real commands carry `If-Match`; stale start receives 412; repository SQL includes the expected version. | PASS |
| RULE-8 | Lossless canonical kernel documents | Accepted equality and canonical private process input construction are directly asserted/inspected. | PASS |
| RULE-10 | Process-only exact packaged replan boundary and verified identities | Exact four paths, explicit arguments, result evidence, and verifier invocation are reproduced in focused tests. | PASS |
| RULE-11 | One async run; exact guarded completion; late/duplicate suppression | Async 202/polling/navigation/cancel are strong, but UC-5 lacks active-run conflict, duplicate completion, and state-change-during-completion boundary tests required by the rule. | GAP G-3 |
| RULE-12 | Exact presets, bounded cancellation/watchdog, unchanged retry | Retry gate is strong; forced cancellation/watchdog tests exercise `plan`, not the distinct `replan` wait/watchdog branch. | GAP G-3 |
| RULE-13 | Private bounded process files and cleanup | Replan test inspects distinct files and cleanup; shared bounded capture/correlation/no-debug checks remain green. | PASS |
| RULE-15 | Intent overlay compiles without accepted mutation | Approved compiler path, exact based-on lineage, and accepted equality are consumed by real UC-5 runs. | PASS |
| RULE-17 | Complete proposal identity/evidence | Every named proposal field is written/asserted and no invented result revision is exposed. | PASS |
| RULE-21 | Same-origin local security | Existing route/security suite is green and real UC-5 commands use session CSRF, Origin, Host, and ETag. | PASS |
| RULE-22 | Stable safe failures with no technical/candidate disclosure | Adapter mappings are safe, but the missing actor-boundary repair failure journeys leave response/body/state non-disclosure unproven. | GAP G-1 |
| RULE-23 | Safe logs and retained evidence for every validation outcome | Success/no-feasible replan logs are exercised; every repair failure class and authoritative persisted diagnostics are not driven end to end. | GAP G-2/G-3 |
| RULE-24 | PostgreSQL-only HTTP/browser journeys; all negatives assert absent side effects; standard lifecycle green | Full standard lifecycle failed with one browser regression; several negative repair branches lack outer-boundary assertions. | FAIL C-1/G-1/G-2 |
| RULE-26 | Typed kernel boundaries and independent hard verification | Kernel architecture tests remain green and workspace verifier runs before FEASIBLE publication. | PASS |
| RULE-27 | Controlled catalog/version/public solver APIs | Unchanged kernel architecture/metadata suites remain green. | PASS |
| RULE-28 | Bounded input and race-safe publication | Unchanged packaged kernel boundary suites remain green; UC-5 consumes the published verified destination. | PASS |
| RULE-29 | Reference repair corpus without universal promise | Existing approved kernel corpus remains unchanged; UC-5 text makes no feasibility/optimality/latency promise. | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 | Singleton aggregate, routes, verification, canonical documents | Clean reactor tests ran; no UC-1 assertion failed. | PASS |
| UC-2 | Shared planner/process/poll/cancel/proposal surfaces | Clean reactor tests ran; no UC-2 assertion failed. | PASS |
| UC-3 | Accepted inspection remains available during solving | Real UC-5 Chrome assertion passed; UC-3 scale/inspection tests passed. | PASS |
| UC-4 | Required durable draft, shared browser repair rendering, pin feedback | Functional UC-4 tests passed, but target-scale pin feedback failed at p95 274.5 ms against the 250 ms contract. | FAIL C-1 |

## Findings

### C-1 CRITICAL - UC-5 regresses the approved UC-4 pin-feedback performance contract

- Contract: UC-4 G6 requires that "at target school scale, direct-effect calculation, pinning, conflict detection, and draft auto-save feedback complete within 250 ms at the 95th percentile"; RULE-25 requires the same 250 ms p95 browser measurement.
- Evidence: `WorkspaceBrowserIT.java:476-518` is the approved real-browser 1,000-lesson journey. Independent `./mvnw -q clean verify` failed at `:517` with p95 274.5 ms and samples `[3693.7,88,40.4,62.3,97.9,40.5,34.2,42.5,68.4,21.7,21.3,28.9,21.6,26.8,26.9,26.5,274.5,203.6,42.8,68]`.
- Why blocking: convergence requires every approved related UC regression to remain green. A prior green run does not override the independently reproduced failing standard lifecycle.
- Revision outcome: make persisted pin feedback reliably remain below 250 ms p95 under the standard browser suite, then rerun UC-4/UC-5 browser regressions and the complete clean reactor.

### G-1 GAP - Repair failure and rejected-output extensions are not proven at the actor boundary

- Contract: extensions 4a, 4d, and 5a require return to the unchanged draft, no proposal/candidate exposure, safe feedback, and an end state after invalid, interrupted/failed, or rejected output.
- Evidence: `KernelPlannerTest.java:126-210,214-243` tests the adapter with process doubles. `WorkspaceRepairPlanningIT` has no HTTP repair run that produces `INVALID_INPUT`, transport/internal/interrupted failure, or mismatched/unverified output.
- Why blocking: adapter outcome checks can pass while `RepairPlanningService.persistOutcome`, HTTP snapshot/problem mapping, conditional completion, or proposal absence is wrong. This is `MISPLACED` evidence for actor-visible extensions and G2/G3/minimal guarantee.
- Revision outcome: drive each distinct service outcome class through a production HTTP repair run with a controllable process boundary and assert safe response/run evidence, exact accepted/draft equality, candidate/proposal absence, and prohibited collaborator/mutation effects.

### G-2 GAP - The no-feasible diagnostic branch is arranged directly in PostgreSQL

- Contract: extension 4b says the workspace translates the kernel's constraint/entity evidence after a no-feasible run, offers the two-minute unchanged-intent retry, and creates no proposal; G7 says those diagnostics are evidence from this run.
- Evidence: `WorkspaceRepairPlanningIT.java:124-135` constructs `lastRun` and writes it directly with SQL. It never drives `KernelPlanner.replan -> RepairPlanningService.persistOutcome` for this branch.
- Why blocking: the test can pass if production drops, corrupts, or wrongly exposes actual `searchDiagnostics`; it only proves rendering/retry behavior from a handcrafted state.
- Revision outcome: make a repair process return a real structured `NO_FEASIBLE_SOLUTION_FOUND`, poll through `REPAIR_DRAFT`, and assert persisted/HTTP/browser diagnostics, exact cautious wording, unchanged intent/accepted data, no proposal, and PT2M eligibility by value.

### G-3 GAP - UC-5-specific concurrency and bounded-process negatives are incomplete

- Contract: RULE-11 requires active-run conflict, late/duplicate completion, and state-change-during-completion coverage; RULE-12 requires forced cancellation and watchdog expiry for repair; RULE-23 requires every failure-class evidence/log check.
- Evidence: UC-5 proves ordinary cancellation and late cancellation suppression, but forced cancel/watchdog tests call `planner.plan`, and no repair test exercises duplicate completion, conflicting start, or completion after authoritative state/identity change.
- Why blocking: UC-5 has a separate executor, state transitions, completion transaction, and `replan` watchdog branch. UC-2 evidence cannot establish these repair-specific paths.
- Revision outcome: add repair-boundary tests for active conflict, duplicate/late/stale completion, graceful and forced cancellation, 30-second and two-minute watchdogs, and safe log/evidence fields for each repair failure class.

## Status Update

`READY_FOR_CONVERGENCE -> NEEDS_REVISION`; UC-5 remains current. UC-8 is independently eligible, but execute must revise and reconverge UC-5 before beginning another use case in this request.

## Response to execute

REVISE UC-5: restore the UC-4 250 ms pin-feedback regression and add production-boundary evidence for unsuccessful, failed, rejected, stale/duplicate, cancellation, and watchdog repair outcomes.
