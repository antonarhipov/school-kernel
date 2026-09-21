# Use-Case Checkpoint: UC-2 - Establish the first accepted timetable

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `dfe0e75`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1; the approved durable `INITIAL_DRAFT` postcondition is consumed through the production import path before planning.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main step 1 | `WorkspaceInitialPlanningIT.plansAndExplicitlyAcceptsInitialTimetable` (`timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceInitialPlanningIT.java:100`) imports the approved UC-1 initial definition, reads its summary, and starts from `INITIAL_DRAFT`; the real-browser journey at `WorkspaceBrowserIT.java:134` asserts the visible lesson count and `No accepted timetable`. | PASS |
| Main step 2 | The same HTTP test asserts `202`, `SOLVING_INITIAL`, a unique run ID, `PT30S`, and no accepted timetable; `KernelPlannerTest.java:37` verifies explicit `plan` arguments, private distinct paths, no seed/step controls, and packaged verification; the browser remains pollable while solving. | PASS |
| Main step 3 | `WorkspaceInitialPlanningIT.java:112-121` asserts a complete `FEASIBLE` result, explicit `NO_BASELINE`/`NO_INTENT` identity, exact assignments, and proposal-only state after independent packaged `verify`; `KernelPlannerTest.java:61` rejects mismatched execution evidence before candidate verification. | PASS |
| Main step 4 | `WorkspaceBrowserIT.java:164-174` observes proposal status, lesson count, `PT30S`, termination reason, timetable details, and `No timetable is accepted yet`; visible evidence is `output/playwright/uc2-initial-proposal.png`. | PASS |
| Main step 5 | `WorkspaceBrowserIT.java:175-180` checks the confirmation box before invoking acceptance; `app.js:72-78` keeps acceptance disabled until that explicit confirmation. | PASS |
| Main step 6 | `WorkspaceInitialPlanningIT.java:123-143` compares the exact accepted definition and result with the proposal, checks the accepted revision, and proves draft/proposal removal; the browser opens `Accepted baseline`, and `output/playwright/uc2-accepted-baseline.png` records the accepted whole timetable. | PASS |
| Extension 1a | `WorkspaceInitialPlanningIT.replacesDraftAndPreservesItAfterUnsuccessfulSearch` (`:179`) proves successful verified replacement, exact persistence, failed-search preservation, and invalid replacement `422` with unchanged version/document. | PASS |
| Extension 2a | `WorkspaceInitialPlanningIT.cancelAndDiscardPreserveInitialDraft` (`:148`) cancels the active HTTP run, asserts exact draft/no proposal, and proves late completion cannot change state; `KernelPlannerTest.forceCancelsAnUncooperativeProcess` (`:86`) proves graceful then forced termination. | PASS |
| Extension 2b | `WorkspaceInitialPlanningIT.restartStaleIdentityAndStorageFailureAreSafe` (`:214`) arranges durable `SOLVING_INITIAL`, invokes production recovery, and asserts exact draft/no run/no proposal; `WorkspaceRepository.java:92-102` also invalidates an unaccepted initial proposal on restart. | PASS |
| Extension 3a | The HTTP replacement test produces packaged `NO_FEASIBLE_SOLUTION_FOUND`; `WorkspaceInitialPlanningIT.planningFailureClassesReturnSafeDiagnosticsAndExactDraft` (`:230`) also drives `INVALID_INPUT`, internal error, transport failure, and interruption through HTTP/PostgreSQL and asserts each exact safe diagnostic, exact draft, no proposal/acceptance, and no raw process disclosure. `KernelPlannerTest.java:126,165,214` covers the same adapter branches, missing/malformed output, and watchdog timeout. | PASS |
| Extension 3b | `KernelPlannerTest.rejectsMismatchedResultEvidence` (`:67`) changes authoritative execution evidence and asserts zero verifier invocation/no result; `WorkspaceInitialPlanningIT.mismatchedKernelOutputNeverBecomesProposal` (`:215`) drives the mismatch through HTTP and asserts `REJECTED_OUTPUT`, exact draft, no proposal, and no accepted timetable. | PASS |
| Extension 4a | `WorkspaceInitialPlanningIT.cancelAndDiscardPreserveInitialDraft` discards through real HTTP and compares the unchanged definition with no proposal. | PASS |
| Extension 5a | `WorkspaceInitialPlanningIT.rejectsEveryStaleProposalIdentity` (`:394`) independently changes every stored identity/count and the complete result, then asserts `409 STALE_PROPOSAL`, invalidation to the exact initial draft, and no accepted baseline; acceptance revalidation and identity checks execute inside the conditional transaction at `InitialPlanningService.java:117-162`. | PASS |
| Extension 6a | `WorkspaceInitialPlanningIT.restartStaleIdentityAndStorageFailureAreSafe` (`:261`) injects verifier unavailability and a PostgreSQL acceptance failure; each returns a safe `503`, leaves `INITIAL_PROPOSAL`, and compares the exact version/document before and after. | PASS |
| G1 | Main, replacement, failure, cancellation, discard, and storage-failure assertions all prove no accepted state; only the explicit acceptance request advances state. | PASS |
| G2 | Proposal UI says `Initial proposal · feasible`; source/test scans and visible evidence contain no `optimal`, `best`, or `globally minimal` claim. | PASS |
| G3 | HTTP and UI evidence use exactly `No feasible timetable was found within this run.` and retain the draft, without an impossibility claim. | PASS |
| G4 | Planner tests reject seed/step arguments; browser proposal details show the actual `PT30S` limit and authoritative termination reason. | PASS |
| G5 | `WorkspaceBrowserIT.java:134` and the visible Playwright journey complete import, plan, review, and accept entirely through the local UI. | PASS |
| G6 | Transactional acceptance, exact rollback assertions, and restart recovery prove no half-accepted baseline can become visible. | PASS |
| Success postcondition | Accepted snapshot contains one exact definition/result/manifest bundle, matching revisions, `ACCEPTED_BASELINE`, and no draft/proposal/run; it is directly inspectable through the accepted timetable view. | PASS |
| Minimal guarantee | Every cancel, failed solve, rejected output, interruption recovery, stale proposal, verifier outage, and storage failure asserts either the exact retryable initial draft or the unchanged reviewable proposal with no accepted timetable. | PASS |
| Requires UC-1 | UC-1 is approved at `dfe0e75`; every HTTP/browser success journey uses the production UC-1 import route and its durable verified initial definition. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | Reactor dependency/archive/ArchUnit checks remain green; workspace planning uses only the packaged `school-kernel` executable through `KernelProcessLauncher`. | PASS |
| RULE-2 | The clean Java 25 reactor resolves the pinned Spring/PostgreSQL/Testcontainers/JDBC stack and architecture tests reject excluded alternatives. | PASS |
| RULE-3 | `WorkspaceRepository.java:23-138` keeps lifecycle, version, run ID, and the complete definition/run/proposal/accepted JSON in the singleton JSONB aggregate through explicit `JdbcClient` SQL; HTTP tests compare full JSON trees. | PASS |
| RULE-5 | `InitialPlanningService.java:45-162`, state-qualified SQL, cancel/discard/recovery tests, and the active-run conflict test cover every UC-2 edge; the conflict test now also compares the process-command count to prove wrong-state refusal launches no second process. | PASS |
| RULE-6 | Every mutation route requires `If-Match`; `WorkspaceInitialPlanningIT.java:309` proves missing-precondition and conflicting-run refusals, the shared stale/race suite remains green, and SQL includes the same expected version and state. | PASS |
| RULE-7 | `InitialPlanningService.accept` revalidates within the transaction; `WorkspaceMutation.java:74-80` and one conditional JSONB update atomically advance the complete bundle; injected storage failure preserves exact proposal/version. | PASS |
| RULE-8 | Planner inputs use `CanonicalJson.bytes`; proposal and accepted-definition/result comparisons are full-tree equality checks preserving all IDs and fields; the approved canonicalization/round-trip suites remain green. | PASS |
| RULE-10 | `KernelPlanner.java:64-162` uses explicit process argument lists for packaged `plan`, distinct file paths, exit/schema/identity checks, and packaged `verify`; `KernelPlannerTest.java:67,165` directly rejects mismatched, missing, and malformed output, and the HTTP mismatch test proves none becomes a proposal. | PASS |
| RULE-11 | `POST /api/runs` returns `202` before the single executor runs; run polling, navigation, single-run conflict, conditional run/version completion, cancel/late-result suppression, and recovery are proven by `WorkspaceInitialPlanningIT`. | PASS |
| RULE-12 | Exact 30-second arguments, absent forbidden controls, two-second force escalation, 40-second watchdog boundary, and no-proposal outcomes are asserted at `KernelPlannerTest.java:37,86,158`. No two-minute release/repair retry is exposed by UC-2. | PASS |
| RULE-13 | `KernelPlannerTest.java:37` verifies owner-only files, distinct input/output, bounded channels, correlation/no-debug arguments, and cleanup; cancel, failure, watchdog, and mismatch tests also end with empty temporary roots. | PASS |
| RULE-17 | Proposal construction stores every required identity/evidence field at `InitialPlanningService.java:182-203`; the every-field/result matrix at `WorkspaceInitialPlanningIT.java:345` proves transactional revalidation, stale invalidation, and no invented whole-result revision. | PASS |
| RULE-21 | Security allowlists only the new run/proposal methods; CSRF/If-Match and Host/Origin enforcement are exercised through real HTTP, including hostile-origin no-disclosure at `WorkspaceInitialPlanningIT.java:331`; shared full route/loopback/secret tests remain green. | PASS |
| RULE-22 | Shared problem responses plus UC-2 HTTP tests assert safe `403`, `409`, `428`, and `503` codes/messages without assignments, raw stderr, SQL, or paths; current ETag/state handling remains covered by the approved suite. | PASS |
| RULE-23 | Planner logs contain only correlation/run IDs, command, exit class, elapsed time, configured limit, termination reason, and feasible flag; durable `lastRun` retains only safe solver evidence. Existing captured-log disclosure tests remain green. | PASS |
| RULE-24 | Standard `clean verify` runs disposable PostgreSQL 18.6, real HTTP, packaged CLI, and real Chrome; UC-2 tests assert state after consequential steps and exact no-mutation/no-candidate outcomes. | PASS |
| RULE-26 | The unchanged approved kernel typed handlers/shared loading and independent hard-verifier architecture passes its complete unit, packaged plan/replan/verify, and ArchUnit suites. | PASS |
| RULE-27 | The unchanged approved catalog/manifest/public-Timefold boundary passes exactness, package metadata, solver completion, cancellation, and architecture checks. | PASS |
| RULE-28 | The unchanged approved 10 MiB/depth/token input limits and atomic publication boundary pass the packaged concurrency and preservation suites; workspace planning accepts only the independently verified published result. | PASS |
| RULE-29 | This checkpoint is not a release candidate and makes no arbitrary-school feasibility or latency promise. The production 30-second visible/browser journeys record the actual limit and termination evidence; the conditional target-scale release gate remains tied to its documented external reference runner. | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am -Dtest=KernelPlannerTest -Dsurefire.failIfNoSpecifiedTests=false test` — 6 tests, 0 failures/errors/skips; `./mvnw -q -pl timetable-workspace -am -Dtest=WorkspaceInitialPlanningIT -Dsurefire.failIfNoSpecifiedTests=false test` — 8 tests, 0 failures/errors/skips.
- Full relevant suite: `./mvnw -q clean verify` — 135 tests, 0 failures, 0 errors, 0 skipped.
- Working tree impact from tests: none; `git diff --check` passed.
- Runtime evidence: visible Chrome/Playwright journey reached proposal and then accepted `Demo School`; result was `FEASIBLE`, limit `PT30S`, termination `TIME_LIMIT`, revision `sha256:232c53bcf2ee6b373cb3965afc87849d245040c9c171f0cc543dff8742bf6b3c`; console had 0 messages/errors/warnings.
- Changed files: `InitialPlanningService.java`, `KernelPlanner.java`, `WorkspaceRecovery.java`, `ImportService.java`, `ProblemHandler.java`, `SecurityConfiguration.java`, `WorkspaceController.java`, `WorkspaceMutation.java`, `WorkspaceRepository.java`, `app.js`, `styles.css`, `KernelPlannerTest.java`, `WorkspaceInitialPlanningIT.java`, `WorkspaceBrowserIT.java`, `status.md`, this checkpoint and Jev artifacts, and `output/playwright/uc2-*.png`.
- Approved UCs regression-tested: workspace UC-1 and kernel-v1 UC-1/UC-2 all passed inside the 132-test clean reactor verification.
- Jev preflight: authorized TypeSafe call completed with pinned `jev-1.13.0`; `spec/timetable-workspace/checkpoints/UC-2.jev.json` contains 38 item judgments: 13 findings, 22 reviews, and 3 clear. Independent inspection found and fixed one real interruption-recovery defect, then reproduced every planning failure class through HTTP/PostgreSQL. Remaining advisory flags are dispositioned below; no semantic approval is claimed from Jev.

## Notes

- Integration tests accelerate repeated branch coverage through an injected launcher that rewrites only test-time limits; the standard real Chrome test and visible Playwright evidence both exercised the unmodified packaged 30-second production run.
- Jev confirmed issue resolved: interruption originally restored the worker interrupt flag before PostgreSQL outcome persistence, causing JDBC to abort and leave `SOLVING_INITIAL`. `InitialPlanningService.java:164-234` now clears the flag only while persisting the terminal outcome and restores it afterward; the HTTP matrix proves `INTERRUPTED` returns to the exact draft.
- Jev findings independently not adopted as defects: `UC-2 main` is directly exercised only through browser controls; extension `3b` now has both adapter and HTTP no-candidate evidence; `G6` has trigger-injected exact rollback; `RULE-1`, `RULE-3`, `RULE-8`, and `RULE-10` are absence/boundary properties established by compiled-class architecture checks, complete production inventories, canonical full-tree comparisons, and explicit process tests; `RULE-5`, `RULE-13`, `RULE-21`, `RULE-23`, and `RULE-26` have direct state/process/filesystem/route/log/architecture suites; `RULE-29` is conditional on a release-candidate declaration, which this checkpoint expressly does not make. These 13 IDs remain model findings despite the cited direct evidence.
- Jev reviews independently inspected with no substantive defect adopted: extensions `1a`, `2a`, `2b`, `4a`, `5a`, `6a`; `G1`, `G2`, `G4`, `G5`; success postcondition; minimal guarantee; `Requires UC-1`; and `RULE-6`, `RULE-7`, `RULE-11`, `RULE-12`, `RULE-17`, `RULE-22`, `RULE-24`, `RULE-27`, `RULE-28`. `UC-2 main` fluctuated between review and finding across identical evidence reruns; its final low-margin negative flag is covered by the browser-only actor journey. `UC-2 extension 3a`, `G3`, and `RULE-2` are clear in the final report.
- The administrator study and reference-runner release gate remain explicit external validation dependencies; this checkpoint claims technical UC-2 readiness, not product-hypothesis validation or a release-candidate performance verdict.

READY FOR CONVERGENCE: UC-2
