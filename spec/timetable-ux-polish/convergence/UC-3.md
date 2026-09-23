# Convergence: UC-3 - Follow repair generation without losing timetable context

## Summary

- Submission: `spec/timetable-ux-polish/checkpoints/UC-3.md` at immutable revision `3f96d79f5f838642874389c82c3b165e0e57c700`, based on previous verifier commit `d44501e20d68a44399a494198ee1c624c08229d2`; original implementation `7e35c40829c8e1265439afbd17c39c1b6eca497a` based on `71c7c0a8f40254dd650279d6d2486ffab4b85022`.
- Verdict: APPROVE. Findings: 0 critical, 0 gaps, 0 protocol, 0 drift/cosmetic; previous G-1/G-2 resolved by independently exercised evidence. User confirmed the UC-3 walkthrough passed after the PENDING WALKTHROUGH verdict.
- Independently rerun: revised focused (26 unit + 2 browser), original focused (26 unit + 3 browser), full `./mvnw -pl timetable-workspace -am verify` (26 unit + 75 integration, including 27 browser); each 0 failed, 0 errors, 0 skipped, exit 0. The full suite includes approved related use cases. Flyway checksum-failure test logs its intentionally refused startup; app-stop test logs a committed-response broken pipe; neither failed a test.
- Working tree: `git status --short` before testing showed only pre-existing `.idea/encodings.xml`; after testing it showed that file and generated `.output.txt`, which was removed. Final pre-report status again showed only `.idea/encodings.xml`; no tracked runtime file changed.

## Protocol Gate

1. Exactly UC-3 is `READY_FOR_CONVERGENCE` (`status.md:3-17`); UC-4/UC-5 are `NOT_STARTED` and no other row is in progress or ready.
2. The revised checkpoint, status, and two new browser journeys were committed together at `3f96d79`. The previous report at `d44501e` remains the explicit revision base; no uncommitted implementation is used.
3. Sole `Requires` dependency UC-2 is `APPROVED`, as is shared UC-1 (`status.md:13-16`, `convergence/UC-2.md:81-91`); UC-3 has no `Includes`/`Extends` (`spec.md:323-333`).
4. Checkpoint rows cover main, extensions, guarantees, both postconditions, relationship, all nine applicable rules, commands, changed files and related regression (`checkpoints/UC-3.md:10-52,96-99`).
5. Examined the entire `d44501e..3f96d79` diff (checkpoint, status and `WorkspaceBrowserIT.java`), the initial UC-3 production changes recorded in the previous convergence report, and production/test paths cited below. No later-UC implementation or unrelated IDE file is in the revision; `git diff --check d44501e 3f96d79` passed. Process doubles retain the production process-launch boundary (`WorkspaceRepairPlanningIT.java:627-711`), and the real feasible path invokes the packaged kernel. Architecture tests and test configuration were not weakened.

## Runtime Reproduction

`B` = `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`; `R` = `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceRepairPlanningIT.java`. Observations are from the verifier's own browser-backed Maven runs, not the executor's reported results.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Local administrator, Chromium, isolated PostgreSQL | Start from durable protected Draft; inspect Solving, Current, frozen Draft, Day and teacher focus | Responsive accepted canvas and same continuing run | Focused original journeys PASS: `B:1439-1489` asserts exact accepted/draft, run ID/limit, modes, selected lesson, unchanged node across polls, and same durable run after modes/narrow return. |
| Administrator | Cancel; unsuccessful/rejected outcomes and allowed retry | Original Draft and safe diagnostics, no proposal | Focused PASS: `B:1490-1495,1516-1569` observes cancellation, failure variants, inspector/Utilities diagnostics, bounded retry and no candidate; `R:375-498` checks refused changed-intent retry, missing output and late results. |
| Administrator on actually stopped and newly started application | Application stops mid-run; reload on new instance | Restart recovery, version increment, same baseline/draft, no proposal | Revised focused PASS: `B:1575-1626` starts a separate Spring application, browser-starts blocked run, closes first context, asserts inactive, starts a new context on the same container, navigates browser to new port and checks Draft default, original run ID, `INTERRUPTED`/`FAILED`, version +1, exact baseline/draft, no proposal or late write on releasing old process. `WorkspaceRecovery.java:15-19` runs on application-ready event and `WorkspaceRepository.java:170-185` performs the transition. This resolves earlier G-1; the original direct-listener test is not used as restart proof. |
| Administrator with independently verified generated full-school accepted pair | Stage teacher-16 unavailability, inspect all 1,000 accepted IDs; run, switch modes, cancel, reject mismatched output | Real scaled repair retains identities and never presents a failed candidate | Revised focused PASS: `B:1631-1705` verifier checks definition/result revisions and 60 cohorts, 100 teachers/rooms, 60 periods, 1,000 unique accepted IDs; browser checks exact Week/Monday ID sets, lesson-960, durable intent/direct effect, original baseline/draft, active run, cancellation and rejection with no proposal. This resolves earlier G-2; fixture provenance is `B:2012-2086`, and `B:1635-1658` checks it before persistence. |
| Administrator and School Kernel | Verified feasible completion | Proposal available but not Current | Original focused PASS: `B:1311-1371` uses packaged solver, checks `REPAIR_PROPOSAL`, prior accepted bundle and frozen draft, retained selected lesson and accepted IDs, Proposal pressed and Solving gone. `RepairPlanningService.java:181-269` conditionally publishes only the verified matching run. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger, preconditions; Requires UC-2 | Consume actual durable conflict-free Draft and canvas | `B:1444-1454,1457-1464,1660-1677`; stage in existing accepted canvas, compare persisted intent/pin/draft; UC-2 approved, full regression green | STRONG | yes |
| Main 1 | Request repair generation | Browser clicks solve at `B:1455-1462,1674-1679`; `RepairPlanningService.java:48-112` stores guarded run before process start | STRONG | yes |
| Main 2 | Solving, modes, frozen Draft, inspector run/cancel | `B:1457-1477,1677-1689`; exact running document/limit, accepted identity, no pin/solve controls; `app.js:118-125,375-384,487-520` | STRONG | yes |
| Main 3 | Inspect accepted lessons/range and switch modes during run | `B:1451-1489,1668-1690`; selection, Day, filters, focus, retained node and run ID/document after switching | STRONG | yes |
| Main 4 | Retain context; distinguish accepted from running, non-optimal outcome | `B:1341-1348,1464-1489,1684-1690`; `app.js:294-295,375-384`, `messages.js:119-129` explicitly distinguish Current/frozen/running | STRONG | yes |
| Main 5 | Complete feasible outcome for exact frozen draft | Real packaged process `B:1341-1368`, frozen draft/baseline unchanged; `RepairPlanningService.java:181-243` guards version/run/intent and publishes verified outcome | STRONG | yes |
| Main 6 | Verify; enter Proposal, remove Solving, retain representable canvas | `B:1350-1371`; exact durable proposal, last feasible status, accepted IDs, selected accepted lesson, Proposal pressed, Solving absent | STRONG | yes |
| Extension 2a | Cancel, restore mutable Draft; no proposal | `B:1488-1495,1690-1695`; exact baseline/draft and `CANCELLED`, no proposal, solve restored; `R:453-473` asserts process termination | STRONG | yes |
| Extension 2b (prior G-1) | Actual stop/restart restores Draft, not a proposal | `B:1583-1625` first app closes before second starts on same isolated DB; browser loads second port, exact persisted state/ID/version and no late publication; `WorkspaceRecovery.java:15-19` | STRONG | yes |
| Extension 3a | Clear only context unrepresentable in next mode and explain | All Current/Draft/Solving read the same accepted ID set (`inspection-state.js:3-4,28-36`, `app.js:477-516`); mode-only loss of identity cannot arise; changing Day clears only out-of-range selection and explains it (`app.js:272-283`, UC-1/2 browser regression) | STRONG | yes |
| Extension 5a | Unsuccessful/failed/diagnostic result returns Draft with safe inspector/Utilities and no candidate | `B:1525-1533,1546-1569,1696-1704`; exact durable draft/baseline, no proposal, visible inert diagnostic and no raw process text; `R:419-449` checks HTTP absence | STRONG | yes |
| Extension 5b | Eligible unchanged retry starts a new run, no prior proposal | `B:1534-1546` new bounded `PT2M` run; `R:375-415` denies changed intent with `RETRY_NOT_AVAILABLE`, identical stored document and no process start for ineligible run | STRONG | yes |
| Extension 6a | Incomplete/non-feasible/stale/mismatched returned output rejected without candidate | `B:1548-1568,1696-1704` actual HTTP/browser and process-output rejection; `R:419-498`, `RepairPlanningService.java:196-204,249-269` reject/ignore late output, preserve baseline/draft and omit candidate | STRONG | yes |
| G1 | Only cancellation during Solving | `B:1341-1348,1464-1477,1684-1689`; no mutation/proposal controls; guarded service `RepairPlanningService.java:48-65,138-155` | STRONG | yes |
| G2 | Shared accepted identity/context, mode switch neither cancels nor restarts | `B:1459-1489,1661-1690`; exact same run/document, 1,000 accepted IDs/selected lesson; `app.js:36-44,477-516` | STRONG | yes |
| G3 | Only independently verified feasible result exposes Proposal | `B:1362-1371,1525-1568,1696-1704`; `R:419-449`; real feasible kernel versus rejected/failed with no candidate | STRONG | yes |
| G4 | Accepted bundle and durable draft unchanged on every branch | Exact JSON `B:1341,1362-1365,1460-1465,1490-1511,1525-1566,1593-1615,1664-1703`; only run/version move on recovery | STRONG | yes |
| G5 | Distinguishable limit/status/cancel/outcome/diagnostics | `B:1342-1348,1462-1466,1505-1509,1528-1533,1613-1617`; `app.js:382-384,464-469`, `messages.js:117-129` | STRONG | yes |
| G6 | Narrow identifies Solving, read-only accepted context | `B:1484-1489` no modes/cancel/other mutation and exact unchanged run; `app.js:375-379` | STRONG | yes |
| G7 (prior G-1) | Local/process/cancellation/timeout/recovery/identity/safe diagnostic boundaries | Actual stop/restart and late-result guard `B:1575-1625`; process/timeout/denial `B:1546-1569`, `R:453-498`, `WorkspaceImportIT.java:459-544` | STRONG | yes |
| G8 (prior G-1/G-2) | Real browser covers all branches and exact state at representative validation scale | Combined five focused journeys `B:1311-1371,1439-1705`, including real restart and verified 1,000-lesson solve/cancel/reject; all 27 browser tests in full suite passed | STRONG | yes |
| Success postcondition | Verified proposal on retained canvas; old accepted still Current, draft identifiable | `B:1341-1371` exact proposal/baseline/draft/accepted ID set and selection, Proposal default; `RepairPlanningService.java:181-243` | STRONG | yes |
| Minimal guarantee (prior G-1) | Cancel, restart, failure, rejection preserve durable draft/baseline and no proposal | `B:1490-1511,1530-1568,1603-1620,1690-1704`; exact JSON, version/ID on real restart, no active proposal or late publish | STRONG | yes |
| RULE-1 | One packaged model and snapshot, no new dependency | `app.js:1-7,36-44,97-135,477-516`; revision diff only adds tests and evidence | STRONG | yes |
| RULE-2 | Retain representable context, only Day/Week preference persists | `inspection-state.js:3-40,101-132`; `B:1464-1489,1684-1690`; exact durable document after mode changes | STRONG | yes |
| RULE-3 | Guarded transitions, refusal below UI | `RepairPlanningService.java:48-112,138-155,181-269`, `R:375-415,453-531`; actual recovered state and version `B:1603-1620` | STRONG | yes |
| RULE-4 | Frozen Draft overlays unchanged accepted IDs, no mutation during Solving | `app.js:375-400,477-520`; `B:1450-1477,1661-1690` identity/draft/controls | STRONG | yes |
| RULE-6 | Safe authoritative names/availability/diagnostics | `app.js:464-469,477-516,551-556`, `B:1527-1533,1548-1569,1671-1672`; invalid accepted-name import covered by shared UC-1 regression | STRONG | yes |
| RULE-7 | Native Compact responsive canvas, fixed inspector and Utilities/catalog | `styles.css:48-77,115-146`, `app.js:290-318,375-384,477-535`, `messages.js:117-129`; `B:1464-1489,1527-1533` and UC-1 collapse/keyboard browser regression | STRONG | yes |
| RULE-8 | No new routes/access/storage/narrow mutation | `SecurityConfiguration.java:25-43`, `HostOriginFilter.java:25-43`, `WorkspaceController.java:109-213`, `WorkspaceImportIT.java:459-544`, `B:1484-1489`; no new routes in diff | STRONG | yes |
| RULE-9 (prior G-2) | Verified isolated normative-size pair and representative run/failure | `B:61-72,1631-1705,2012-2086` compares revisions, declared dimensions, 1,000 unique IDs, whole Week/Day sets, exact baseline/draft and rejected state; full fixture special cases/regressions also exercised by approved UC-1/2 tests | STRONG | yes |
| RULE-11 (prior G-1/G-2) | Real durable UC-2 Draft, packaged kernel, actual restart, scale, failure/retry/modes/narrow | `B:1311-1371,1439-1705`; `R:375-531`; actual second application plus real browser and full workspace suite | STRONG | yes |

## Rule Conformance

| Rule | Modal constraint from `rules.md` | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace's accepted model ... MUST NOT fork ... `GET /api/workspace` MUST remain the complete snapshot source” (`:94-105`) | `app.js:1-7,36-44,477-516`; no dependency/API/fixture change in revision | PASS |
| RULE-2 | “MUST derive available/default modes ... retain representable range ... MUST NOT issue a mutation request for ... navigation” (`:107-117`) | `inspection-state.js:3-40,101-132`, `B:1459-1489,1684-1690`, exact run/document unchanged | PASS |
| RULE-3 | “solve/retry/cancel ... MUST use the existing service/domain transitions ... Every other attempted transition MUST be refused” (`:119-135`) | `RepairPlanningService.java:48-112,138-155,181-269`, `R:375-415,453-531`, `B:1603-1620` | PASS |
| RULE-4 | “Draft and frozen Solving MUST render the same accepted lesson IDs/placements as Current ... Solving MUST expose cancellation but no draft mutation or proposal action” (`:137-148`) | `app.js:375-400,477-520`, `B:1457-1477,1661-1690` | PASS |
| RULE-6 | “All modes MUST use school-controlled names ... diagnostics MUST be rendered as inert text” (`:164-178`) | `app.js:464-469,551-556`, `B:1527-1533,1548-1569,1671-1672`; UC-1 name-refusal regression | PASS |
| RULE-7 | “MUST reuse ... Compact Day/Week ... fixed open width ... Utilities MUST contain applicable export/diagnostics only” (`:180-195`) | `styles.css:48-77`, `app.js:290-318,375-384,477-535`, `B:1464-1489,1527-1533`; full browser regression | PASS |
| RULE-8 | “full route-to-access table ... MUST remain unchanged ... MUST NOT ... relax loopback/Host/Origin/CORS/CSRF/`If-Match`/security-header protections” (`:197-206`) | `SecurityConfiguration.java:25-43`, `HostOriginFilter.java:25-43`, `WorkspaceController.java:109-213`, `WorkspaceImportIT.java:459-544`, full security suite | PASS |
| RULE-9 | “normative snapshot ... MUST be the sole feature validation source ... Fresh isolated database fixtures MUST meet every declared cardinality and content condition by value ... verify the accepted pair before use” (`:208-220`) | `B:61-72,1631-1705,2012-2086`; verifier validates complete generated pair/revisions before storing; compares 1,000 IDs, 60/100/100/60 definition rows, exact Week/Day and durable failure state. Shared special-case regression passed | PASS |
| RULE-11 | “Automated verification MUST ... test ... restart recovery ... responsive Current/frozen Draft inspection ... prove a failed outcome neither creates nor renders a proposal” (`:236-246`) | Real-browser packaged success `B:1311-1371`, actual restart `B:1575-1625`, normative-scale negative `B:1631-1705`, other failures/retry/narrow `B:1439-1569`, HTTP refusal `R:375-531` | PASS |

Security and lifecycle audit: route table (`rules.md:29-57`) remains unchanged: local reads `GET /`, `/workspace/**`, `/api/csrf`, `/api/workspace`, `/api/runs/{runId}`, `/api/proposal`, `/api/accepted/export`; guarded import/replace/draft/preview/run/cancel/proposal mutations use the existing Host/Origin, CSRF, and version boundary (`SecurityConfiguration.java:25-43`, `HostOriginFilter.java:25-43`, `WorkspaceController.java:62-106,109-213`). `WorkspaceImportIT.java:459-544` asserts hostile/missing origin, hostile Host, missing CSRF, forbidden login/actuator, CSP, no CORS and absence of forbidden writes/process starts. Export is presented only in Accepted baseline (`app.js:294-305`). This local single-administrator application has no role/owner credentials to test; local anonymous access is intentional, not a wrong-role allowance. `inspection-state.js:118-132` persists only scoped Day/Week preference. Service guards and `R:375-498` cover the action-by-state refusal matrix, including wrong retry, concurrent run, stale completion and no candidate disclosure; no new route or exemption was added.

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| `timetable-ux-polish` UC-2 | Required approved durable Draft, accepted identity and shared controls | Full 75-IT suite includes UC-2 browser journeys `B:1080-1305`, real persisted Draft stage in target `B:1444-1454,1660-1674` | PASS |
| `timetable-ux-polish` UC-1 | Accepted canvas, selection, range, focus, inspector and Utilities | Full 75-IT suite includes UC-1 whole-school/scale/empty/narrow browser journeys `B:324-915` | PASS |
| Accepted-inspection prerequisite and workspace security/repair | Shared renderers, guarded lifecycle and local routes | Full workspace suite: 26 unit, 75 IT incl. 27 browser and HTTP repair/security suites; 0 failed/errors/skipped | PASS |

## Findings

None. Prior G-1 is closed by an actual old-application close and new-application startup/browser reload on the same isolated database (`B:1575-1625`), unlike the original direct listener call; prior G-2 is closed by a verified generated full-school pair and real 1,000-lesson repair/cancel/rejected-output journey (`B:1631-1705`). These are actor-boundary assertions of exact durable and visible state, not merely a green test count. The five-participant administrator feature gate (`spec.md:569-592`) is separate and is not claimed passed.

## Walkthrough

UC-3-derived browser walkthrough as local timetable administrator, using an accepted baseline and a durable conflict-free protected Draft:

1. Generate a repair; while it runs, check the Solving mode, inspector status/limit and Cancel control. Inspect an accepted lesson, change Day/Week, switch among Current/frozen Draft/Solving and use a focused schedule/return; confirm Current remains the accepted timetable, the draft is frozen, and navigation does not restart the run or claim optimality.
2. Cancel a run; confirm editable Draft returns without a proposal and accepted lessons remain unchanged. On another run, stop and restart the application, reopen the workbench, and confirm Draft default, an interrupted diagnostic and no proposal.
3. Try an unsuccessful/failed or rejected result; inspect its safe explanation in the inspector and Utilities, with no candidate on the canvas. If the result permits unchanged-intent retry, retry and confirm a new bounded run rather than a prior proposal. On a narrow viewport during a run, confirm Solving is identified but cancellation and editing are unavailable.
4. Complete a feasible repair; check Proposal is offered on the retained accepted canvas with Current still the previous accepted timetable and the originating draft identifiable; do not accept it as part of UC-3.

User response to this exact walkthrough: **“Walkthrough passed.”** This closes the UC-3 human confirmation gate; the separate five-participant feature gate (`spec.md:569-592`) is not claimed passed.

## Status Update

UC-3 `READY_FOR_CONVERGENCE` → `PENDING_WALKTHROUGH` → `APPROVED` after the user-confirmed walkthrough; `Current` remains UC-3 (`APPROVED`), `Next eligible` is UC-4. UC-1/UC-2 remain `APPROVED`; UC-4/UC-5 remain `NOT_STARTED`.

## Response to execute

APPROVED