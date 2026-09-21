# Use-Case Checkpoint: UC-5 - Obtain a feasible repair proposal

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `c7ea4f82045db1c156b667ed7881e6d1e29d4fec`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-4; the production run consumes the approved durable repair draft, exact intent revision, compiled successor definition, and unchanged accepted definition/result bundle.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-5 | `WorkspaceRepairPlanningIT.createsVerifiedRepairProposalWithoutReplacingAcceptedBaseline` runs the exact production HTTP/PostgreSQL/packaged-CLI journey, including 202 solving state, frozen controls, accepted inspection, verified `FEASIBLE` proposal, lineage, execution evidence, and six authoritative change categories. `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` performs the actor journey in real Chrome and keeps accepted inspection live. | PASS |
| Extension 2a | `cancellationStalenessAndRestartAreSafe` cancels the active run and compares the exact durable draft and accepted bundle while proving proposal absence; `boundsAndSuppressesConcurrentLateRepairRuns` drives an uncooperative repair process through the production HTTP cancellation path and observes forced termination. | PASS |
| Extension 2b | The same integration test persists `SOLVING_REPAIR`, invokes production startup recovery, and asserts `REPAIR_DRAFT`, absent active run/proposal, and exact draft/accepted equality. | PASS |
| Extension 3a | The integration test reuses an obsolete pre-run ETag, receives `412 STALE_WORKSPACE_VERSION`, and proves no new active run or proposal; start also checks compiled `basedOnRevision` before submission. | PASS |
| Extension 4a | `failedAndRejectedRepairOutputsNeverBecomeProposals` drives structured `INVALID_INPUT` through the production HTTP/PostgreSQL completion path, retains only safe `validationReport` feedback, returns to the exact draft, and exposes no candidate or raw stderr. | PASS |
| Extension 4b | `gatesTwoMinuteRetryByUnchangedUnsuccessfulIntent` observes `NO_FEASIBLE_SOLUTION_FOUND`, structured constraint/entity diagnostics, unchanged intent/accepted data, no proposal, and the exact bounded-run message. | PASS |
| Extension 4c | The same test proves changed intent receives `409 RETRY_NOT_AVAILABLE`, restoring the exact intent enables a new `PT2M` run, and cancellation retains only run evidence with no proposal. | PASS |
| Extension 4d | `failedAndRejectedRepairOutputsNeverBecomeProposals` drives internal, transport, and interruption failures through real HTTP and asserts safe outcomes, exact accepted/draft preservation, and candidate absence; `boundsAndSuppressesConcurrentLateRepairRuns` drives the repair watchdog through the same boundary and observes `KERNEL_TIMEOUT` plus forced termination. | PASS |
| Extension 5a | `failedAndRejectedRepairOutputsNeverBecomeProposals` drives mismatched feasible output through HTTP and proves it is rejected without candidate retention; `boundsAndSuppressesConcurrentLateRepairRuns` changes authoritative version/state during execution and proves late completion cannot publish. | PASS |
| G1 | Draft `PATCH` during the real packaged run returns `409 INVALID_WORKSPACE_TRANSITION`; cancellation is the only path back to editable `REPAIR_DRAFT`. | PASS |
| G2 | `KernelPlanner.replan` checks schema/status/correlation/school/seed/limit/termination/assignments/exact report and invokes `KernelVerifier`; only its `FEASIBLE` outcome enters the proposal branch. | PASS |
| G3 | All five UC-5 integration journeys compare the exact accepted JSON around feasible, unsuccessful, failed, rejected, cancelled, stale, timed-out, conflicting, and recovered repair outcomes. | PASS |
| G4 | The persisted/API proposal is asserted to contain exact accepted timetable revision, successor definition/input revision, intent revision, complete result, proposed timetable revision, and no invented public result revision. | PASS |
| G5 | Real Chrome renders the required period-stability then room-only-stability priority and bounded-run evidence; centralized messages make no optimality or global-minimum claim. | PASS |
| G6 | Production copies actual limit, termination reason, and elapsed time and derives all six counts from the verified kernel report; HTTP and browser assertions verify the values and labels. | PASS |
| G7 | The safe message says only that no feasible repair was found within this run; structured diagnostics remain attached to that run and never claim impossibility or a guaranteed recipe. | PASS |
| Success postcondition | The real packaged journey reaches one `REPAIR_PROPOSAL`; persisted and API proposal documents are equal, all lineage identities match, and the accepted baseline remains current. | PASS |
| Minimal guarantee | Cancellation, retry refusal, unsuccessful search, stale start, process failure mappings, output rejection, and recovery retain the durable accepted/draft data and expose no eligible candidate. | PASS |
| Requires UC-4 | UC-4 is approved; each integration/browser journey creates or persists its repair draft through the production UC-4 route, and UC-5 submits the same intent revision and compiler output. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-3 | Repair run, last-run evidence, intent identity, and proposal remain inside the existing singleton JSONB aggregate with relational lifecycle/version/run guards and explicit `JdbcClient` SQL; no parallel store or model was added. | PASS |
| RULE-5 | Transactional `REPAIR_DRAFT -> SOLVING_REPAIR -> REPAIR_PROPOSAL/REPAIR_DRAFT` paths and refused draft mutation are exercised against PostgreSQL; recovery uses the same lifecycle service boundary. | PASS |
| RULE-6 | All commands require current ETag/`If-Match`; stale start returns 412 and the same version participates in conditional start/finish SQL. | PASS |
| RULE-8 | Accepted and successor kernel documents are deep-copied losslessly and written as RFC-8785 canonical private process bytes; exact JSON comparisons prove accepted fidelity. | PASS |
| RULE-10 | `KernelPlannerTest` asserts packaged `replan` with exact `--current-definition`, `--current`, `--definition`, and `--output` private paths, explicit argument list, and independent result verification. | PASS |
| RULE-11 | POST persists a unique run and returns 202 before execution on the dedicated executor; polling/inspection remains available; a second start is refused without another process; completion after authoritative version/state change is discarded. | PASS |
| RULE-12 | Only `PT30S` and unchanged-unsuccessful `PT2M` are accepted; real HTTP repair tests prove retry gating, watchdog handling, forced cancellation, late suppression, and no candidate publication. | PASS |
| RULE-13 | Replan uses a new owner-only directory with four distinct private files, bounded stdout/stderr, correlation ID, no debug, safe messages, and unconditional cleanup. | PASS |
| RULE-15 | UC-5 consumes the approved compiler that deep-copies the complete accepted definition, sets exact `basedOnRevision`, materializes availability/pins, and never mutates accepted assignments. | PASS |
| RULE-17 | The proposal stores source version, accepted revision, successor revision, intent revision, complete result, proposed revision, run ID, limit, termination, elapsed time, and six counts; tests explicitly reject an invented `resultRevision`. | PASS |
| RULE-21 | New routes remain under the existing loopback same-origin/session-CSRF policy; every real HTTP mutation uses CSRF, accepted Origin/Host, and current ETag, and the complete security matrix remains green. | PASS |
| RULE-22 | Real HTTP repair journeys prove transition, stale, retry, validation, transport, interruption, timeout, and rejected-output failures use stable safe codes/messages without paths, raw stderr, stack traces, or candidate assignments. | PASS |
| RULE-23 | Structured replan logging records only safe metadata; real HTTP/PostgreSQL journeys assert persisted feasible, unsuccessful, failed, timed-out, and cancelled run evidence while captured-output checks reject prohibited data. | PASS |
| RULE-24 | Disposable PostgreSQL 18.6, ephemeral HTTP, packaged School Kernel, and real Chrome success journeys plus real HTTP negative repair journeys execute in the standard Maven lifecycle with consequential state and absence assertions and no H2 fallback. | PASS |
| RULE-26 | The packaged typed `replan` handler and shared kernel validation pipeline remain the scheduling authority; workspace `KernelVerifier` independently revalidates every feasible output before proposal eligibility. | PASS |
| RULE-27 | Clean catalog, manifest-version, component-boundary, and public-solver-API tests remain green; UC-5 introduces no kernel metadata constant or Timefold implementation dependency. | PASS |
| RULE-28 | The clean kernel suite covers bounded replan inputs and race-safe atomic publication; workspace invokes that packaged boundary and trusts only the independently verified complete destination. | PASS |
| RULE-29 | The deterministic target-scale localized-teacher and room-outage repair gates remain green; UC-5 labels bounded execution evidence without a universal feasibility, optimality, or latency promise. | PASS |

## Validation

- Focused commands: expanded `WorkspaceRepairPlanningIT` - 5 tests PASS; UC-5 real-Chrome browser journey - PASS; focused UC-4 target-scale pin-feedback samples `[257.8,149,94.8,85.6,96.6,26.5,34.7,37.7,79.7,46.7,33.5,38.3,31.5,24.8,34.1,41.9,55.2,40.7,32.6,56.3]`, p95 149.0 ms - PASS below 250 ms.
- Full relevant suite: `./mvnw -q clean verify` - 159 tests, 0 failures, 0 errors, 0 skipped; UC-4 target-scale pin-feedback samples `[227.2,72.3,54.4,86.3,74,30.8,26.9,22.1,107.4,64.4,28.9,26.2,27.2,29.4,26.5,25.6,27.7,25.8,34.2,29.5]`, p95 107.4 ms - PASS below 250 ms.
- Working tree impact from tests: none; `git diff --check` passes.
- Runtime evidence: real Chrome over ephemeral HTTP/PostgreSQL starts a protected teacher repair, keeps accepted timetable inspection available during the packaged 30-second run, renders a verified feasible repair proposal with priority/execution/change evidence, and leaves the accepted baseline unchanged.
- Changed files: `RepairPlanningService.java`; planner, repair compiler guard, controller, mutation, recovery, and repository integration; native `app.js` and `messages.js`; planner, PostgreSQL/HTTP, and Chrome tests; accepted-result fixture; status, checkpoint, and Jev bundle.
- Approved UCs regression-tested: timetable-workspace UC-1 through UC-4 and kernel UC-1/UC-2 all pass in the 159-test clean reactor. Pin mutations use `Prefer: return=minimal`, merge the returned durable repair-draft delta, and avoid serializing/parsing the complete 1,000-lesson snapshot on every pin.
- Jev preflight: `UC-5.jev-bundle.json` validates locally with 37 atomic items for pinned `jev-1.13.0`; the prescribed npm wrapper could not rebuild under read-only `.agents`, so the already-built pinned helper performed validation. External review is `REVIEW` because the turn did not authorize disclosure of repository excerpts and claims to TypeSafe; no report was produced and no semantic approval is claimed.

## Notes

- Clean verification emitted Hikari connection-validation warnings from previously stopped Testcontainers contexts during the long browser phase; the reactor exited 0 and all 159 reports record zero failures/errors/skips.
- Revision resolves convergence C-1 and G-1 through G-3 with a minimal persisted-pin response and production-boundary negative repair journeys; no contract or rule was weakened.
- The required administrator walkthrough and independent evidence audit remain convergence responsibilities; this checkpoint claims automated technical readiness only.
- Jev `REVIEW` is an authorization limitation rather than counterevidence; deterministic checks and convergence remain authoritative.

READY FOR CONVERGENCE: UC-5
