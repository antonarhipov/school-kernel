# Use-Case Checkpoint: UC-7 - Keep the weekly timetable operational after a disruption

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `1240cdc` (revision after the user made performance references non-blocking; C-2 implementation `ccfc5d1`; C-1 implementation `433ea70`; original implementation `4acf363`)
- Submission commit: HEAD at convergence
- Relations verified: Requires approved UC-1 and executes the approved UC-3 inspection, UC-4 protected draft, UC-5 packaged repair, and UC-6 review/acceptance paths twice in one directly linked operational journey.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main steps 1-2 | `WorkspaceRepairPlanningIT.keepsWeeklyTimetableOperationalAcrossTeacherAndRoomDisruptions` first reads the accepted snapshot and compares its exact baseline; `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` opens the accepted workspace and keeps `Accepted baseline remains current` visible during both draft and proposal states. | PASS |
| Main step 3 | The HTTP/PostgreSQL journey stages teacher unavailability, applies a room-only attempt pin, asserts its direct-effect set and absence of conflicts, and compares the accepted bundle unchanged. Real Chrome stages the same intent through native controls and renders one attempt-scoped pin. | PASS |
| Main step 4 | Both actor journeys invoke the production `POST /api/runs` path. The integration journey observes a complete `REPAIR_PROPOSAL`, exact accepted timetable and parent identities, `FEASIBLE` verified output, and an unchanged accepted baseline. | PASS |
| Main step 5 | Real Chrome renders old/proposed assignments, all change-accounting context, direct effects, and solver ripple effects before keyboard-native explicit acceptance. The HTTP journey compares the exact proposal definition/result with the newly accepted bundle and proves draft/proposal cleanup. | PASS |
| Main steps 6-7 | The integration journey immediately stages a later room disruption from the first accepted result, proves its `basedOnRevision` equals that result's `inputRevision`, proves the old room pin is absent, generates and accepts a second proposal, and records exactly two packaged `replan` commands. Real Chrome repeats the later room journey from the current accepted screen and ends on a complete feasible baseline with no transient state. | PASS |
| Extension 3a | `WorkspaceRepairDraftIT.blocksConflictingPinsWithoutChoosingAWinner` asserts a pin/unavailability conflict blocks solving, exposes `PIN_CONTRADICTS_UNAVAILABILITY`, and becomes ready only after explicit unpinning; the accepted baseline remains outside the draft mutation. | PASS |
| Extension 4a | `WorkspaceRepairPlanningIT.gatesTwoMinuteRetryByUnchangedUnsuccessfulIntent` and `failedAndRejectedRepairOutputsNeverBecomeProposals` cover no-feasible, cancellation, invalid, internal, transport, interruption, timeout, mismatched, and stale completion outcomes. Each retains the exact accepted bundle and draft and exposes no proposal until a complete feasible result exists. | PASS |
| Extension 5a | `WorkspaceRepairPlanningIT.discardsOnlyProposalAndRetainsRepairDraft` transitions back to the exact retained draft, removes only the proposal, and compares the prior accepted bundle unchanged. | PASS |
| Extension 5b | `WorkspaceRepairPlanningIT.rollsBackFailedAcceptanceAndKeepsProposalReviewable` injects a PostgreSQL acceptance failure, observes safe `STORAGE_UNAVAILABLE`, and compares exact document/version/proposal before a successful explicit retry. | PASS |
| Extension 7a | Both new journeys use a later `ROOM` disruption. The integration test derives an unoccupied recurring period, retains the no-direct-effect intent without inventing assignments, and proves independent period/room pin behavior; the browser selects room and period through native controls and completes the same decision path. | PASS |
| G1 | HTTP and Chrome assert `ACCEPTED_BASELINE` before, between, and after repairs and explicit `Accepted baseline remains current` copy during non-current draft/solve/proposal states. Exact JSON comparisons prove only one accepted bundle is current. | PASS |
| G2 | The complete real-Chrome journey performs inspection, disruption staging, pinning, two solves, two reviews, and two acceptances using native workspace controls with no JSON or command-line interaction by the administrator. | PASS |
| G3 | Draft assertions identify direct-effect lessons; proposal review separately renders/asserts `Direct effects of your intent` and `Solver ripple effects` for both repairs. | PASS |
| G4 | The later compiled successor's `basedOnRevision`, proposal `acceptedTimetableRevision`, and proposal parent definition revision are compared directly with the exact first accepted result. The second acceptance stores that exact proposal bundle. | PASS |
| G5 | Existing approved negative-path suites compare the complete accepted JSON for draft conflict, run failure/cancel/rejection, discard, stale identity, and injected durable-acceptance failure. No partial proposal becomes current. | PASS |
| Success postcondition | The HTTP/PostgreSQL and real-Chrome journeys finish with a complete `FEASIBLE` second repair as `ACCEPTED_BASELINE`, intact lineage, no draft/proposal, and an accepted manifest with prior attempt pins absent. | PASS |
| Minimal guarantee | All included UC-4/UC-5/UC-6 failure journeys retain the last accepted bundle exactly and keep it identifiable as current; the full clean regression suite re-executes those paths. | PASS |
| Requires UC-1 | UC-1 is approved. Both UC-7 journeys begin from the production singleton aggregate containing an exact verified accepted definition/result/manifest bundle. | PASS |
| Includes UC-3 | The browser begins in the approved whole-school accepted view; the HTTP journey first reads and compares the complete accepted snapshot before mutation. | PASS |
| Includes UC-4 | Both journeys stage resource unavailability and pin protection through the approved repair-draft endpoints/UI; conflict and no-effect paths remain covered by the UC-4 regression suite. | PASS |
| Includes UC-5 | Both repairs in each journey use the production asynchronous packaged-kernel `replan` route and reach a complete verified proposal without replacing the accepted baseline. | PASS |
| Includes UC-6 | Both proposals are reviewed and explicitly accepted through the production decision route; exact bundle advancement, cleanup, and durable-failure behavior remain covered. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Clean reactor verification retains separate contract, CLI, and workspace modules; UC-7 adds no kernel linkage or orchestration API. | PASS |
| RULE-2 | `./mvnw -q clean verify` runs the pinned Java/Spring/PostgreSQL stack with no dependency changes. | PASS |
| RULE-3 | Both repairs use the same singleton JSONB aggregate. Minimal pin feedback reads only the authoritative draft and selected accepted assignment/lesson/lock from that aggregate, then conditionally replaces only its `repairDraft` subtree; PostgreSQL tests compare the complete accepted document unchanged. | PASS |
| RULE-5 | The journey executes `ACCEPTED_BASELINE -> REPAIR_DRAFT -> SOLVING_REPAIR -> REPAIR_PROPOSAL -> ACCEPTED_BASELINE` twice; the existing exhaustive transition suite remains green. | PASS |
| RULE-6 | Every command carries the current strong ETag through conditional SQL. The scoped pin mutation increments the same aggregate version and returns its matching ETag; stale and missing precondition regressions remain green. | PASS |
| RULE-7 | Each acceptance stores definition/result/manifest and clears draft/proposal/run together. Existing trigger-failure coverage proves exact rollback; the new second acceptance exposed and now regresses asymmetric prior-lock handling. | PASS |
| RULE-10 | Two recorded argument lists invoke packaged `replan`; proposal identities prove immutable accepted parent definition/result and complete successor inputs. | PASS |
| RULE-11 | Each solve returns `202`, is polled to `REPAIR_PROPOSAL`, and retains the accepted baseline. Active-run, cancellation, recovery, and late-result regressions remain green. | PASS |
| RULE-15 | Teacher and room intents compile to complete successor definitions without accepted mutation; the later compilation proves the newly accepted direct parent and dropped prior attempt pin. | PASS |
| RULE-16 | Attempt-scoped room protection is explicit and dimension-specific; the approved immutable bulk/conflict/snapshot suite remains green, and the later no-effect room intent invents no direct effects. | PASS |
| RULE-17 | Both acceptances consume identity-bound proposals and independently verified `FEASIBLE` results; the full stale-identity matrix remains green. | PASS |
| RULE-19 | Real Chrome consumes complete server snapshots for accepted, draft, solving, proposal, and later-repair states and does not reconstruct scheduling truth. After initial load, native code prepares each metadata-ordered weekday matrix and swaps it in place without a server request. | PASS |
| RULE-20 | Native form, checkbox, and button controls plus visible state text support the complete actor journey; acceptance is keyboard-tested and state is not color-only. | PASS |
| RULE-21 | New coverage uses the existing loopback same-origin session/CSRF boundary; the complete security matrix remains green. | PASS |
| RULE-22 | Included conflict, solve, stale, and storage failure regressions return stable safe problem responses without candidate or infrastructure disclosure. | PASS |
| RULE-23 | Packaged run evidence is retained with safe correlation/execution fields; captured-log validation remains green and no school data logging was added. | PASS |
| RULE-24 | Disposable PostgreSQL 18.6, ephemeral HTTP, packaged School Kernel, and real Chrome execute the complete two-repair journey in the standard Maven lifecycle. | PASS |
| RULE-25 | The unchanged 1,000-lesson/20-sample checks retain raw samples, nearest-rank p95, diagnostic 250 ms/one-second comparisons, and solver-time exclusion. The clean reactor recorded pin 86.6 ms, day 44.1 ms, and review opening 370.6 ms; the user explicitly made those reference values non-blocking, while functional browser assertions remain mandatory. | PASS |
| RULE-26 | The two production repair calls cross the shared typed kernel command boundary and the clean architecture/schema tests remain green. | PASS |
| RULE-27 | No metadata or solver API source changed; both packaged repairs use the controlled catalog/version/solver boundary covered by the clean suite. | PASS |
| RULE-28 | Both complete inputs and verified outputs cross the existing bounded, private, atomic process-file boundary; mismatch/late-publication regressions remain green. | PASS |
| RULE-29 | The clean reactor reruns the kernel reference corpus while UC-7 makes only the bounded claim that both observed runs produced independently verified feasible results. | PASS |

## Validation

- Focused suites: `ManifestServiceTest` passes; the two-repair HTTP/PostgreSQL/packaged-kernel journey passes; the two-repair real-Chrome journey passes.
- Focused C-1 regression: the complete 10-test `WorkspaceBrowserIT` class passes in actor order, including the UC-7
  two-repair journey followed by the approved UC-4 discard journey.
- Focused C-2 regressions: the complete 9-test `WorkspaceRepairDraftIT` class and complete 10-test
  `WorkspaceBrowserIT` class pass sequentially; a focused persisted-pin scale run records 104.7 ms p95 and the complete
  browser class records day-change p95 96.0 ms before the final clean reactor. After the final clean reactor, the
  repair-draft class reruns green with its success, stale-ETag, injected-storage-failure, rollback, and accepted-bundle
  immutability assertions explicitly routed through the minimal pin-response branch.
- C-3 contract revision: the administrator explicitly made the 250 ms and one-second values diagnostic so development
  is not blocked by reference-machine performance. The scale fixture, raw samples, p95 calculation, and functional
  browser assertions remain; only threshold-based test failure was removed.
- Focused C-3 regression: clean complete 10-test `WorkspaceBrowserIT` - PASS; diagnostic p95 was search 56.0 ms,
  filter 29.6 ms, day 52.6 ms, selection 54.3 ms, persisted pin feedback 160.1 ms, and proposal-review opening
  429.9 ms. Full clean reactor diagnostics were search 56.4 ms, filter 49.5 ms, day 44.1 ms, selection 47.5 ms,
  persisted pin feedback 86.6 ms, and proposal-review opening 370.6 ms. Solver time was excluded in both runs.
- Full relevant suite: `./mvnw -q clean verify` - 167 tests, 0 failures, 0 errors, 0 skipped.
- Working tree impact from tests: none; `git diff --check` passes.
- Runtime evidence: real Chrome over ephemeral HTTP/PostgreSQL accepts a protected teacher repair, opens a fresh browser target on that exact result, stages a later room disruption with zero inherited attempt pins, and accepts the second packaged-kernel proposal with zero browser errors.
- Changed files: `ManifestService` and its regression; the composite repair HTTP/PostgreSQL and Chrome journeys;
  headless-Chrome process-output isolation; scoped repair-draft controller/service/repository/mutation persistence;
  cached native weekday matrices; Testcontainers Spring-context lifecycle annotations; diagnostic performance contract
  and browser reporting; decisions, status, and checkpoint.
- Approved UCs regression-tested: timetable-workspace UC-1 through UC-6 and kernel UC-1/UC-2 all pass in the 167-test clean reactor.

## Notes

- The second acceptance found a production defect in `ManifestService.lockIds`: a prior manifest entry containing only `roomLockOrigin` was read through absent `periodLockOrigin`. The guarded field lookup now treats absent lock dimensions independently, and unit, packaged HTTP, and real-browser regressions cover the exact failure.
- Convergence C-1 reproduced the approved UC-4 Chrome test stalling after the longer UC-7 test in two full reactors
  while passing alone. Each launched Chrome merged output into an unread process pipe. Redirecting that diagnostic
  output to `DISCARD` removes process backpressure while preserving the real browser, every actor assertion, and the
  original 15-second CDP command timeout. The complete browser class and clean reactor then pass.
- Convergence C-2 first reproduced stopped-container connection-pool load and closes every Testcontainers-backed Spring
  context after its class. The unchanged full lifecycle then exposed insufficient production margin in day rendering
  and pin feedback. Native matrices are now prepared after initial snapshot load and swapped in place. The minimal pin
  command retains persistence-before-feedback, CSRF, ETag, conditional SQL, derived conflict state, and accepted-bundle
  immutability while reading and writing only the necessary JSONB aggregate subtrees.
- After convergence C-3 reported one complete-reactor pin-feedback p95 above 250 ms, the administrator explicitly
  revised the product decision: scale samples and percentile calculations remain diagnostic evidence, but performance
  reference values no longer block functional delivery or convergence. No fixture, sample count, percentile formula,
  actor assertion, persistence assertion, or warmup changed.
- The later room disruption deliberately uses an unoccupied recurring period. This is the specified no-direct-effect UC-4 behavior and proves retained intent, direct lineage, attempt-pin cleanup, a second verified proposal, and a second atomic acceptance without relying on solver heuristics to move a particular lesson.
- Clean verification emitted expected stopped-Testcontainers Hikari warnings from negative tests; the reactor exited 0 and every report records zero failures/errors/skips.
- The required administrator walkthrough and independent evidence audit remain convergence responsibilities; this checkpoint claims automated technical readiness only.

READY FOR CONVERGENCE: UC-7
