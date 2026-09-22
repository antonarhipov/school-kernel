# Use-Case Checkpoint: UC-7 - Keep the weekly timetable operational after a disruption

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `7c76b2e` (revision for convergence finding C-1; original implementation based on `c53b1a624bfd7bda319bb28bdff0fec9095fab40`)
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
| RULE-3 | Both repairs read and conditionally replace the same singleton JSONB aggregate; tests query and compare its complete document. | PASS |
| RULE-5 | The journey executes `ACCEPTED_BASELINE -> REPAIR_DRAFT -> SOLVING_REPAIR -> REPAIR_PROPOSAL -> ACCEPTED_BASELINE` twice; the existing exhaustive transition suite remains green. | PASS |
| RULE-6 | Every command in the HTTP journey carries the current strong ETag through the production helper and conditional SQL; the clean concurrency regressions remain green. | PASS |
| RULE-7 | Each acceptance stores definition/result/manifest and clears draft/proposal/run together. Existing trigger-failure coverage proves exact rollback; the new second acceptance exposed and now regresses asymmetric prior-lock handling. | PASS |
| RULE-10 | Two recorded argument lists invoke packaged `replan`; proposal identities prove immutable accepted parent definition/result and complete successor inputs. | PASS |
| RULE-11 | Each solve returns `202`, is polled to `REPAIR_PROPOSAL`, and retains the accepted baseline. Active-run, cancellation, recovery, and late-result regressions remain green. | PASS |
| RULE-15 | Teacher and room intents compile to complete successor definitions without accepted mutation; the later compilation proves the newly accepted direct parent and dropped prior attempt pin. | PASS |
| RULE-16 | Attempt-scoped room protection is explicit and dimension-specific; the approved immutable bulk/conflict/snapshot suite remains green, and the later no-effect room intent invents no direct effects. | PASS |
| RULE-17 | Both acceptances consume identity-bound proposals and independently verified `FEASIBLE` results; the full stale-identity matrix remains green. | PASS |
| RULE-19 | Real Chrome consumes complete server snapshots for accepted, draft, solving, proposal, and later-repair states and does not reconstruct scheduling truth. | PASS |
| RULE-20 | Native form, checkbox, and button controls plus visible state text support the complete actor journey; acceptance is keyboard-tested and state is not color-only. | PASS |
| RULE-21 | New coverage uses the existing loopback same-origin session/CSRF boundary; the complete security matrix remains green. | PASS |
| RULE-22 | Included conflict, solve, stale, and storage failure regressions return stable safe problem responses without candidate or infrastructure disclosure. | PASS |
| RULE-23 | Packaged run evidence is retained with safe correlation/execution fields; captured-log validation remains green and no school data logging was added. | PASS |
| RULE-24 | Disposable PostgreSQL 18.6, ephemeral HTTP, packaged School Kernel, and real Chrome execute the complete two-repair journey in the standard Maven lifecycle. | PASS |
| RULE-25 | Existing validation-scale browser checks remain green: inspection interactions are measured separately from solving, pin feedback remains below 250 ms p95, and review opening is 346.100 ms below one second. | PASS |
| RULE-26 | The two production repair calls cross the shared typed kernel command boundary and the clean architecture/schema tests remain green. | PASS |
| RULE-27 | No metadata or solver API source changed; both packaged repairs use the controlled catalog/version/solver boundary covered by the clean suite. | PASS |
| RULE-28 | Both complete inputs and verified outputs cross the existing bounded, private, atomic process-file boundary; mismatch/late-publication regressions remain green. | PASS |
| RULE-29 | The clean reactor reruns the kernel reference corpus while UC-7 makes only the bounded claim that both observed runs produced independently verified feasible results. | PASS |

## Validation

- Focused suites: `ManifestServiceTest` passes; the two-repair HTTP/PostgreSQL/packaged-kernel journey passes; the two-repair real-Chrome journey passes.
- Focused C-1 regression: the complete 10-test `WorkspaceBrowserIT` class passes in actor order, including the UC-7
  two-repair journey followed by the approved UC-4 discard journey.
- Full relevant suite: `./mvnw -q clean verify` - 167 tests, 0 failures, 0 errors, 0 skipped.
- Working tree impact from tests: none; `git diff --check` passes.
- Runtime evidence: real Chrome over ephemeral HTTP/PostgreSQL accepts a protected teacher repair, opens a fresh browser target on that exact result, stages a later room disruption with zero inherited attempt pins, and accepts the second packaged-kernel proposal with zero browser errors.
- Changed files: `ManifestService`, its regression test, the composite repair HTTP/PostgreSQL and Chrome journeys,
  headless-Chrome process-output isolation, status, checkpoint, and Jev advisory bundle.
- Approved UCs regression-tested: timetable-workspace UC-1 through UC-6 and kernel UC-1/UC-2 all pass in the 167-test clean reactor.
- Jev preflight: `UC-7.jev-bundle.json` validates locally with 46 atomic items using the already-built pinned `jev-1.13.0` helper. External review requires disclosure of narrow repository excerpts to TypeSafe and is `REVIEW` because this turn did not authorize that disclosure; no report was produced and no semantic approval is claimed.

## Notes

- The second acceptance found a production defect in `ManifestService.lockIds`: a prior manifest entry containing only `roomLockOrigin` was read through absent `periodLockOrigin`. The guarded field lookup now treats absent lock dimensions independently, and unit, packaged HTTP, and real-browser regressions cover the exact failure.
- Convergence C-1 reproduced the approved UC-4 Chrome test stalling after the longer UC-7 test in two full reactors
  while passing alone. Each launched Chrome merged output into an unread process pipe. Redirecting that diagnostic
  output to `DISCARD` removes process backpressure while preserving the real browser, every actor assertion, and the
  original 15-second CDP command timeout. The complete browser class and clean reactor then pass.
- The later room disruption deliberately uses an unoccupied recurring period. This is the specified no-direct-effect UC-4 behavior and proves retained intent, direct lineage, attempt-pin cleanup, a second verified proposal, and a second atomic acceptance without relying on solver heuristics to move a particular lesson.
- Clean verification emitted expected stopped-Testcontainers Hikari warnings from negative tests; the reactor exited 0 and every report records zero failures/errors/skips.
- The required administrator walkthrough and independent evidence audit remain convergence responsibilities; this checkpoint claims automated technical readiness only.

READY FOR CONVERGENCE: UC-7
