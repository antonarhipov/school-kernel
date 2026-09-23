# Convergence: UC-3 - Follow repair generation without losing timetable context

## Summary

- Submission: `spec/timetable-ux-polish/checkpoints/UC-3.md` at `7e35c40829c8e1265439afbd17c39c1b6eca497a`, based on `71c7c0a8f40254dd650279d6d2486ffab4b85022`.
- Verdict: REJECT.
- Findings: 0 critical, 2 gaps, 0 protocol; both gaps are evidence requirements, not observed failures of the exercised production paths.
- Suite: focused `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser+showsFailedRepairEvidenceAndGatedRetryInRealBrowser+generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify`: 26 unit + 3 browser, 0 failed/errors/skipped. Full `./mvnw -pl timetable-workspace -am verify`: 26 unit + 73 integration, 0 failed/errors/skipped; includes the approved UC-1/UC-2 browser regression and existing security/lifecycle tests. Both commands exited 0.
- Working tree impact from verification: the command-output capture `.output.txt` was generated and removed; no tracked runtime changes. The pre-existing unrelated `.idea/encodings.xml` remains dirty and untouched.

## Protocol Gate

1. Exactly UC-3 is `READY_FOR_CONVERGENCE` (`status.md:3-17`); UC-4 and UC-5 remain `NOT_STARTED`.
2. The implementation, checkpoint, and executor status are committed together at `7e35c40`; `git show` identifies the immutable boundary and the supplied base. The checkpoint is `checkpoints/UC-3.md:1-55`.
3. The sole `Requires` dependency, UC-2, is `APPROVED` (`status.md:13-15`, `convergence/UC-2.md:85-91`); UC-1 is also approved. UC-3 has no `Includes` or `Extends` dependency (`spec.md:323-333`).
4. There is no other `IN_PROGRESS` or `READY_FOR_CONVERGENCE` row (`status.md:11-17`).
5. The checkpoint provides rows for the scenario, extensions, guarantees, both postconditions, each applicable rule, relationship, commands, changed files, and approved-UC regression (`checkpoints/UC-3.md:10-49`). Reported evidence does not imply that each row is sufficiently proved; see G-1/G-2.
6. The seven changed files in `71c7c0a8..7e35c40` are the checkpoint/status, recovery repository, packaged UI/catalog, and browser/HTTP test sources. The previously existing proposal presentation and acceptance assertions in `generatesRepairProposalInRealBrowser` are not new UC-4 implementation; the diff adds only UC-3 handoff assertions there. No unrelated IDE change is in the commit. `git diff --check` passed.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator in packaged Chromium, isolated PostgreSQL | From persisted protected Draft start a blocked repair; inspect Solving, Current, frozen Draft, Day, lesson and focused teacher | Responsive accepted canvas and one continuing run | Focused browser PASS: `WorkspaceBrowserIT.java:1439-1487` observes exact accepted/draft documents, run ID/limit, selection, frozen controls, unchanged selected DOM node during polling, and no run replacement on mode/narrow transitions. The blocked process is a process-boundary double (`WorkspaceRepairPlanningIT.java:640-659`). |
| Administrator | Cancel, return to Draft, then start another blocked repair | No proposal and mutable original Draft | Focused browser PASS: `WorkspaceBrowserIT.java:1488-1499` reads `REPAIR_DRAFT`, canceled `lastRun`, identical draft/accepted, no proposal and restored solve control. |
| Administrator | Application stops during solving; reload | Startup recovery restores unchanged Draft, safe interrupted diagnostic | The browser test calls `recovery.recoverInterruptedRun()` on the *running* application, then reloads that same browser (`WorkspaceBrowserIT.java:1500-1509`). Its durable-state and late-result assertions pass, but neither the application nor its process stops or restarts: this does not reproduce extension 2b's trigger. G-1. |
| Administrator | Unsuccessful run, diagnostic selection, eligible retry, rejected/mismatched/transport/timeout output | Safe failure, no proposal and bounded new run | Focused browser PASS: `WorkspaceBrowserIT.java:1516-1568` observes no-feasible diagnostic and two-minute retry plus exact unchanged draft/accepted on failures; HTTP boundary checks rejection codes and no result leakage (`WorkspaceRepairPlanningIT.java:375-449`). |
| Administrator and packaged kernel | Feasible completion | Independently verified proposal while baseline and draft remain | Focused browser PASS: `WorkspaceBrowserIT.java:1309-1370` invokes the real packaged kernel, observes Proposal selected and Solving absent, exact accepted bundle and draft unchanged, a feasible retained run, and accepted selected lesson. |
| Administrator, required normative validation snapshot | The UC-3 run/inspection/failure journey at declared school scale | Claimed full scale/inspection regression | UC-3 journeys start from `validAcceptedDocument()` (`WorkspaceBrowserIT.java:1310,1440,1517,1844-1859`), whose `src/test/resources/uc5-accepted-result.json:30-34` contains two lessons. Separate 1,000-lesson inspection tests (`WorkspaceBrowserIT.java:576-630,1571-1582`) do not solve, cancel, or recover a repair on that snapshot. G-2. |

## Evidence Ledger

`B` below is `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`; `R` is `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceRepairPlanningIT.java`. Every cited browser journey passed in the independently run focused/full suites.

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-3 trigger/preconditions; Requires UC-2 | Real persisted conflict-free Draft, pin, selection and range are consumed | `B:1444-1454,1457-1463` records persisted intent/pin and the frozen run draft; UC-2 approved and full regression green | STRONG | yes |
| Main 1 | Administrator requests generation | `B:1453-1459`, browser click, blocked process and active durable run; `R:640-659` controls real process-launch seam | STRONG | yes |
| Main 2 | Enter Solving, Current/Draft/Solving, freeze Draft, accepted inspection and inspector cancel | `B:1456-1468,1470-1478`, `app.js:118-125,375-384,487-520`, exact state and missing pin/solve controls | STRONG | yes |
| Main 3 | Inspect lessons, change range and switch modes while running | `B:1447-1449,1464-1487`, selected lesson/Day/cohort, mode/focus round trip, identical run ID and durable run | STRONG | yes |
| Main 4 | Retain context, identify accepted baseline without asserting optimality | `B:1461-1487`; `app.js:294-295,375-384,516,580-582`; accepted label and non-optimal running text | STRONG | yes |
| Main 5 | Complete feasible outcome for frozen intent | `B:1337-1367`, packaged kernel and exact frozen draft; `RepairPlanningService.java:181-243` checks run/version/intent before committing feasible outcome | STRONG | yes |
| Main 6 | Verify, select Proposal, remove Solving, retain canvas | `B:1348-1370`, exact stored baseline/draft, verified feasible proposal, accepted IDs/selection and default Proposal | STRONG | yes |
| Extension 2a | Cancel, restore mutable Draft, no proposal | `B:1488-1493`, `R:453-473`, full durable accepted/draft and cancellation state | STRONG | yes |
| Extension 2b | Application stops during solving, reload restores Draft | `B:1495-1509` invokes the listener directly and reloads while the same server remains alive; no stop/restart boundary (`WorkspaceRecovery.java:15-19`) | MISPLACED | no (G-1) |
| Extension 3a | Clear only context unrepresentable in another mode, explain | `inspection-state.js:3-4,33-36` and `app.js:477-516` use the same accepted identity set for all three modes, so a mode-only loss of accepted identity is unreachable; range-specific clearing has explicit notice at `app.js:272-283` | STRONG | yes |
| Extension 5a | Failed/no-feasible results return Draft with safe inspector/Utilities diagnostic and no proposal | `B:1525-1533,1546-1565`, `R:419-449`; exact accepted/draft and candidate absence; `app.js:106-117,308-318,394-401,464-469` | STRONG | yes |
| Extension 5b | Allowed retry with unchanged intent starts new run, never shows prior candidate | `B:1534-1544` records two-minute run and subsequent cancellation, `R:375-415` checks changed-intent refusal and unchanged retry | STRONG | yes |
| Extension 6a | Incomplete, non-feasible, stale or mismatched candidate rejected without rendering | `B:1546-1565` mismatched real process output and no candidate; `R:419-498` rejected, failed and late/stale outcomes and exact stored data; `KernelPlanner.java` verifies process output before proposal | STRONG | yes |
| G1 | Solving has no draft/proposal mutation and only cancellation | `B:1341-1346,1464-1475`, `app.js:375-400,487-520,576-579`, guarded HTTP refusal in full suite | STRONG | yes |
| G2 | Accepted identities/context stable, navigation never restarts run | `B:1457-1487`, same durable run ID and accepted model; `app.js:36-44,477-516` | STRONG | yes |
| G3 | Only verified feasible outcome exposes Proposal | `B:1360-1369,1525-1565`, `R:419-449`; no proposal or candidate result on failure | STRONG | yes |
| G4 | Accepted bundle/draft unchanged across all branches | `B:1339,1361-1364,1450-1463,1491-1509,1524-1565`, exact JSON comparisons, `R:419-498` | STRONG | yes |
| G5 | Limit, status, cancel, safe outcome/diagnostic native controls | `B:1341-1346,1464,1505-1507,1527-1531,1546-1565`, `app.js:382-384,464-469`, `messages.js:117-129` | STRONG | yes |
| G6 | Narrow Solving identifies state and withholds mutation | `B:1482-1487` checks focus and no mode/cancel controls and unchanged run; `app.js:375-379,639-652` | STRONG | yes |
| G7 | Local/process/cancel/timeout/recovery/safe diagnostics unchanged | Cancellation/timeout/denial evidence at `B:1488-1493,1546-1565`, `R:453-498` and `WorkspaceImportIT.java:459-544`; recovery not exercised on actual restart (`B:1500-1509`) | WEAK | no (G-1) |
| G8 | Real-browser evidence covers all declared branches and exact state | Focused journeys and full suite cover all listed branches except actual application-stop/restart; UC-3 journeys do not use the normative-scale snapshot (`B:1310,1440,1517,1844-1859`) | WEAK | no (G-1, G-2) |
| Success postcondition | Verified proposal on retained canvas, prior baseline Current, draft identifiable | `B:1339,1348-1370`, exact persisted proposal/accepted/draft and selected lesson | STRONG | yes |
| Minimal guarantee | Cancel/restart/failure/rejection leave durable draft/accepted and no proposal | `B:1488-1509,1525-1565` proves cancel/failure/rejection and direct-listener recovery, but not recovery *after application stop/restart* | WEAK | no (G-1) |
| RULE-1 | Packaged single snapshot/model, no alternate dependency | `app.js:1-7,36-44,97-135,477-516`; unchanged dependencies/routes in diff | STRONG | yes |
| RULE-2 | Presentation authority retains context, only preference persists | `inspection-state.js:3-40,101-132`, `B:1464-1487`, no stored run change on switching | STRONG | yes |
| RULE-3 | Guarded solve/retry/cancel and refused invalid state | `RepairPlanningService.java:181-269`, `R:375-415,453-531`, browser cancellations and exact state | STRONG | yes |
| RULE-4 | Frozen accepted IDs/pins, no Draft mutation while solving | `app.js:375-400,477-520,585-607`, `B:1450-1478` | STRONG | yes |
| RULE-6 | Authoritative/safe names, accepted availability, inert diagnostics | `app.js:464-469,477-516,926-946`, `B:1527-1531,1546-1565`, approved UC-1 name-refusal regression | STRONG | yes |
| RULE-7 | Compact responsive workbench, fixed inspector, Utilities/catalog | `styles.css:48-77,115-146`, `app.js:290-318,375-384,477-532`, `B:1464-1487,1527-1533`, `messages.js:117-129` | STRONG | yes |
| RULE-8 | No expanded route/access or narrow mutation | `SecurityConfiguration.java:25-34`, `HostOriginFilter.java:25-43`, `WorkspaceImportIT.java:459-544`; unchanged security configuration, `app.js:959-989`, `B:1482-1487` | STRONG | yes |
| RULE-9 | Exact isolated normative validation snapshot | Isolated container (`B:59-70`) and separate scale checks (`B:576-630`), but UC-3 browser journeys use the two-assignment fixture (`B:1310,1440,1517,1844-1859`; `src/test/resources/uc5-accepted-result.json:30-34`) rather than the normative run snapshot | WEAK | no (G-2) |
| RULE-11 | UC-2 durable Draft, packaged kernel, restart, failures, modes/narrow | Packaged success `B:1309-1370` and negative browser/HTTP evidence `B:1439-1568, R:375-531`; simulated recovery calls the listener without application restart, and solving is not validated with the normative-scale snapshot | WEAK | no (G-1, G-2) |

## Rule Conformance

| Rule | Constraint quoted from `rules.md` | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace's accepted model ... MUST NOT fork ... `GET /api/workspace` MUST remain the complete snapshot source” (`:94-105`) | `app.js:1-7,36-44,477-516`, submission diff adds no endpoint or dependency | PASS |
| RULE-2 | “MUST derive available/default modes ... retain representable range ... MUST NOT issue a mutation request for ... navigation” (`:107-117`) | `inspection-state.js:3-40,101-132`, exact running document and modes at `B:1457-1487` | PASS |
| RULE-3 | “solve/retry/cancel ... MUST use the existing service/domain transitions ... Every other attempted transition MUST be refused” (`:119-135`) | `RepairPlanningService.java:181-269`; `R:375-415,453-531` verifies refusal/late-result state | PASS |
| RULE-4 | “Draft and frozen Solving MUST render the same accepted lesson IDs/placements as Current ... Solving MUST expose cancellation but no draft mutation or proposal action” (`:137-148`) | `app.js:375-400,477-520,585-607`, `B:1457-1478` | PASS |
| RULE-6 | “All modes MUST use school-controlled names ... diagnostics MUST be rendered as inert text” (`:164-178`) | `app.js:464-469,551-582,926-946`; failure text and accepted availability checked in full browser regression | PASS |
| RULE-7 | “MUST reuse ... Compact Day/Week ... fixed open width ... Utilities MUST contain applicable export/diagnostics only” (`:180-195`) | `styles.css:48-77`, `app.js:290-318,375-384,477-532`; browser verifies controls and narrow state | PASS |
| RULE-8 | “full route-to-access table ... MUST remain unchanged ... MUST NOT ... relax loopback/Host/Origin/CORS/CSRF/`If-Match`/security-header protections” (`:197-206`) | `SecurityConfiguration.java:25-34`, `HostOriginFilter.java:25-43`, `WorkspaceController.java:109-193`; `WorkspaceImportIT.java:459-544` checks hostile/missing Origin, Host, CSRF, extra routes, CSP, no CORS. No authentication/role/owner was added: anonymous **local** access is intended, remote/wrong-origin is denied. No new route in diff, `app.js:959-989` sends CSRF/ETag; full HTTP suite passed | PASS |
| RULE-9 | “normative snapshot ... MUST be the sole feature validation source ... Fresh isolated database fixtures MUST meet every declared cardinality and content condition by value ... verify the accepted pair before use” (`:208-220`) | UC-3 run tests use two assignments; separate scale inspection and isolated DB do not supply a normative-scale run/transition fixture. See G-2 | GAP |
| RULE-11 | “Automated verification MUST ... test ... restart recovery ... responsive Current/frozen Draft inspection ... prove a failed outcome neither creates nor renders a proposal” (`:236-246`) | Most paths pass through browser/HTTP and a packaged feasible kernel; listener called directly while server is running, not a restart. See G-1; G-2 also prevents representative scale verification | GAP |

Security-route audit: the diff does not alter the route table (`rules.md:29-57`). `GET /`, `/workspace/**`, `/api/csrf`, `/api/workspace`, `/api/runs/{runId}`, `/api/proposal`, and `/api/accepted/export` remain local reads; import, replace, draft, preview, run, cancel, and proposal mutations retain method allowlisting, same-origin, CSRF and `If-Match` (`SecurityConfiguration.java:25-34`, `HostOriginFilter.java:25-43`, `WorkspaceController.java:62-106,109-193`). Export remains shown only in Accepted baseline (`app.js:294-305`); `/login`, `/logout`, `/actuator/**` and other routes remain denied. No cross-school login/owner boundary exists in this local single-administrator feature. Security regression and route review reveal no change; no new permission exemption or skip was introduced.

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| `timetable-ux-polish` UC-2 | Required approved durable Draft/accepted identity and shared browser controls | Full 73-integration suite includes its five browser journeys (`WorkspaceBrowserIT.java:1080-1305`); zero failed/errors/skipped | PASS |
| `timetable-ux-polish` UC-1 | Accepted canvas/lesson/Day/focus/Utilities reused by UC-3 | Full suite includes its scale, selection, narrow and empty browser journeys (`WorkspaceBrowserIT.java:324-425,576-915`); zero failed/errors/skipped | PASS |
| Accepted inspection prerequisite and repair lifecycle | Same renderers, guarded HTTP run and local security | Full suite includes inspection, `WorkspaceRepairPlanningIT` and `WorkspaceImportIT` security tests; 26 unit + 73 integration, zero failed/errors/skipped | PASS |

## Findings

### G-1 GAP — An invoked recovery listener is not an application restart

Contract: UC-3 extension 2b says “If the application stops during solving, reload restores Repair draft and Draft mode with no eligible proposal” (`spec.md:350-353`); its minimal guarantee includes “restart” (`:385-390`). RULE-11 requires automation to “test ... restart recovery” (`rules.md:236-246`). The claimed restart browser test (`B:1495-1509`) calls the injected `WorkspaceRecovery.recoverInterruptedRun()` (`WorkspaceRecovery.java:15-19`) while its Spring application and process double are still running, then calls `Page.reload`. The earlier HTTP test similarly injects a running row and calls the listener in-process (`R:500-531`). They prove the repository transition and display after a manually invoked listener, but not whether actual application shutdown/restart, new context initialization, stale process termination and subsequent browser load preserve the exact accepted bundle/draft and suppress a late result. This is `MISPLACED` for 2b and weak for G7/G8, the minimal guarantee, and RULE-11. Revision outcome: run a blocked repair from the real browser, stop the application, start a new application instance against the same isolated PostgreSQL database, then load `/workspace/`; assert Draft default, safe retained interruption evidence, exact accepted/draft/version transition, no run/proposal/candidate, and late-result suppression at the process boundary.

### G-2 GAP — UC-3 run journeys do not exercise the declared normative validation snapshot

Contract: RULE-9 requires the “normative snapshot and six administrator tasks ... sole feature validation source” and fresh fixtures meeting “every declared cardinality and content condition by value” (`rules.md:208-220`); the validation snapshot requires at least 60 classes, 100 teachers, 100 rooms, and 900–1,100 assignments, plus run/failure cases (`spec.md:544-563`). The UC-3 success, live-run, and failure browser tests all use `validAcceptedDocument()` (`B:1310,1440,1517,1844-1859`), backed by the two-assignment accepted result in `timetable-workspace/src/test/resources/uc5-accepted-result.json:30-34`. The existing scale browser test checks accepted investigation (`B:576-630`) and another only inspection timings (`B:1571-1582`); neither starts, retries, cancels, rejects, recovers, or completes a repair on the normative-scale data. Thus scale checks and the tiny run journey cannot together establish the specified snapshot's run-context fidelity; RULE-9 and G8 remain `WEAK`. Revision outcome: execute representative UC-3 real-browser run/inspection and consequential negative transitions on a generated or properly anonymized, independently verified normative snapshot; compare all declared dimensions and unique IDs plus exact durable accepted/draft/run/proposal state by value at each handoff. Reuse the already approved inspection fixture if possible without altering the specification.

No security regression or observed production behavior failure is asserted by either finding. The test doubles at `R:640-695` preserve process failure/exit/output shapes at the launcher seam; no tests were disabled, architecture exemptions added, or production data touched. The passed full suite is supporting evidence, not a substitute for the two missing contract checks.

## Walkthrough

Deferred: automated evidence has two blocking gaps. No UC-3 human walkthrough is requested or counted as passed on this rejection; once both gaps are closed, derive it from the UC-3 live Current/frozen Draft/Solving, cancel/restart, failure/diagnostics/retry, successful Proposal handoff, and narrow read-only paths (`spec.md:335-390`). The separate five-participant feature gate is not claimed satisfied.

## Status Update

UC-3 `READY_FOR_CONVERGENCE` → `NEEDS_REVISION`; convergence reference `convergence/UC-3.md` with G-1/G-2. `Current` remains UC-3 (`NEEDS_REVISION`), `Next eligible` remains none; UC-1/UC-2 stay approved and UC-4/UC-5 are not started.

## Response to execute

REVISE UC-3: G-1 prove real application-stop/restart recovery at the browser/durable boundary; G-2 validate UC-3 repair journeys with the declared verified normative-scale snapshot.