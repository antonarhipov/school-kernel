# Convergence: UC-4 - Read a lens on a narrow screen

## Summary

- Submission: `checkpoints/UC-4.md` and implementation at `2745269` (base `218adfe2a8bd47b8806160ea6caeba0484eead94`).
- Verdict: PENDING WALKTHROUGH.
- Findings: 0 critical, 0 gap, 0 protocol.
- Suite: focused UC-4 browser journeys 2 run / 0 failures / 0 errors / 0 skipped; targeted Proposal journey 1 / 0 / 0 / 0; independent full workspace integration suite 126 / 3 / 12 / 0; isolated rerun of the four extra full-run failures 12 / 0 / 0 / 0. The full run's 11 other failing method IDs exactly match the pre-UC-4 baseline in `convergence/UC-1.md`.
- Working tree impact from verification: none. `git status --short` was empty before and after the runs. `git diff --check 218adfe2..2745269` and `node --check` for all three changed JavaScript modules passed.

The full-run failure count is reported as observed. Four failures beyond the established 11 were kernel-verification timeouts in `WorkspaceManualDraftIT.{detectsClashesAndPersistsConflicts,publishesManualDraftAtomicallyAsAcceptedBaseline,refusesOpenDraftFromNonAcceptedState}` and `RepairDraftBrowserIT.retainsAcceptedCanvasThroughTeacherDraftModesAndReload`. All four passed in the fresh isolated rerun: `WorkspaceManualDraftIT` 11/11 and that Repair Draft browser case 1/1. The submitted change has no server, kernel, test-fixture, or build modification. The executor's earlier 126-test full run had exactly the 11 baseline failures. These observations support environment variance rather than a reproducible UC-4 regression; the full suite is not called green.

Commands: the checkpoint's focused `MatrixLensBrowserIT#readsOneEntityDayOnANarrowScreen+preservesAnExplicitClassWhenNarrowing` and targeted `ProposalReviewBrowserIT#comparesOneSidedAndSameSlotChanges` Failsafe selections; `./mvnw -q -pl timetable-workspace -am verify` for the full run; then `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceManualDraftIT,RepairDraftBrowserIT#retainsAcceptedCanvasThroughTeacherDraftModesAndReload' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify` for isolation.

Paths below are relative to `timetable-workspace/src`. The browser tests are in `test/java/org/schoolkernel/workspace/`; `app.js`, `inspection-state.js`, `messages.js`, and `day-renderer.js` are in `main/resources/static/workspace/`.

## Protocol Gate

1. UC-4 is the sole target and was `READY_FOR_CONVERGENCE` in `status.md:5-16`. Pass.
2. Its checkpoint and implementation are committed together at `2745269`; the checkpoint identifies base `218adfe2a8bd47b8806160ea6caeba0484eead94`. `git diff --stat` resolves 14 changed files. Pass.
3. UC-4 has no `Requires` or `Includes` dependency. Its `Extends UC-1 at 1a` base is `APPROVED` (`status.md:13`). Pass.
4. UC-1 and UC-2 are approved; UC-3 is `PENDING_WALKTHROUGH`, not `IN_PROGRESS` or `READY_FOR_CONVERGENCE`. The requested independent UC-4 extension does not change UC-3's verdict. Pass.
5. The checkpoint has evidence rows for main 1-4, extension 3a, G1-G3, both postconditions, the UC-1 relation, every applicable rule, changed files, commands, and approved-UC regression. Pass.
6. The diff contains the narrow Day presentation, removal of the superseded focused surface, replacement browser assertions, `Workbench` picker helpers, the checkpoint, and status. No route, persistence, build, migration, architecture-test exemption, unrelated user edit, or later-UC feature is present. Pass.

## Runtime Reproduction

The focused and full browser runs used real headless Chrome against the workspace HTTP server and isolated PostgreSQL 18.6 Testcontainers. The browser harness resets the workspace and browser storage per test (`WorkbenchBrowserSupport.java:134-148`). The verifier inspected the generated 390 px class and teacher screenshots; the grid, picker, tile text, and horizontal scroll were legible without page overflow.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Open accepted timetable at 390 px | First class's Monday in the Day matrix | `MatrixLensBrowserIT.java:30-47` passed in the focused and full runs: one `CLASS/cohort-0` row, lessons 0-11, class Day tile fields, lifecycle and read-only notice, scrollable `.matrix-wrap`, disabled tiles, no desktop or decision controls. |
| Administrator | Choose teacher, room, and weekday | Each choice keeps one Day row and normative tiles | `:49-76` passed: teacher-16 Monday has lessons 960-971 and `[subject, room, class]`; its empty Friday has no invented tile and text-marked unavailable periods; room-16 Monday has `[subject, teacher, class]` and no undeclared-availability claim. |
| Administrator | Choose an already filtered class | Explicit class wins over the first-class default | `:104-126` passed: class-16 stays selected, empty Friday is an ordinary empty row, switching to teacher and back preserves the class criterion. |
| Administrator | Reload and resize | Last weekday returns; desktop Week preference survives | `:63-65,78-99` passed: exact three-key preference retained `WEEK` while the narrow view read Friday; reload cleared the lens but kept Friday; 701 px restored 60 desktop Week class groups. An already-active teacher lens was honored on narrowing. |
| Administrator | Read a repair Proposal at 390 px | Accepted/proposed cues visible, decisions withheld | `ProposalReviewBrowserIT.comparesOneSidedAndSameSlotChanges` passed 1/1; `:56-69` selects a class and Monday, finds the accepted origin and proposed destination in the Day matrix, and confirms no proposal decision control. |
| Administrator | Verify no authoritative change | No non-GET request or document change | `MatrixLensBrowserIT.java:75-76,98-99,125-126` passed; `Workbench.java:319-345` records all non-GET/HEAD requests, and `WorkbenchBrowserSupport.java:184-187` compares version plus complete stored document. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger and precondition | Timetable at 700 CSS px or narrower | Real accepted timetable at 390 px (`MatrixLensBrowserIT.java:30-38`) and retained Proposal at 390 px (`ProposalReviewBrowserIT.java:56-69`); viewport branch in `app.js:854-856` | STRONG | yes |
| Main 1: Day at last valid weekday | Narrow Day, saved weekday | `app.js:461-463` derives `DAY` without altering state range; `inspection-state.js:111-116` changes only weekday; exact Week/Friday preference and Friday after reload at `MatrixLensBrowserIT.java:63-65,78-86` | STRONG | yes |
| Main 1: first class unless explicit class or lens | Default, class, and lens paths | First `cohort-0` at `:39-40`; explicit `cohort-16` at `:104-112`; pre-existing teacher-16 lens at `:87-94`; `app.js:2024-2025` derives the implicit class | STRONG | yes |
| Main 2: notice and lifecycle | Read-only lifecycle banner | Accepted lifecycle and notice at `:37-47`; Proposal lifecycle and absent decisions at `ProposalReviewBrowserIT.java:56-69`; `app.js:207-210,496-510,634-640,654-657,685-687` routes timetable-bearing modes to the same narrow surface | STRONG | yes |
| Main 2: withhold edits, cancellation, decisions | No active desktop workflow | Disabled tiles and absent controls at `MatrixLensBrowserIT.java:43-47`; absent Proposal actions at `ProposalReviewBrowserIT.java:63-69`; `app.js:854-856` returns before desktop control binding | STRONG | yes |
| Main 3: choose class, teacher, room, weekday | Native picker transitions | Real selects at `MatrixLensBrowserIT.java:49-50,58,67-69,104-124`; `app.js:935-964` validates IDs, reuses `applyLens`, and rerenders | STRONG | yes |
| Main 4: one readable Day group, normative tiles | Row headers, tile fields, scroll | Exact row type/ID/header and tile fields at `MatrixLensBrowserIT.java:39-55,67-73,95-97`; one row, internal horizontal scroll, no page overflow, >=150 px period cell at `:43-45`; inspected 390 px class and teacher screenshots | STRONG | yes |
| Extension 3a: empty chosen day | Empty row, declared availability only | Teacher-16 Friday has zero tiles and unavailable text on periods 48-59 (`:58-62`); class-16 Friday has ordinary empty cells (`:113-117`); room without availability has none (`:73`) | STRONG | yes |
| G1: Day matrix renderer, no agenda | Shared renderer and removed surface | Narrow calls `renderDayMatrix` (`app.js:920-934`), also used for desktop Day (`:888`); `day-renderer.js:2-10` builds the grid; focused renderer is deleted; `.matrix-wrap` present and `.focused-schedule` absent at `MatrixLensBrowserIT.java:43-47` | STRONG | yes |
| G2: honest narrow read-only surface | No desktop claim or mutation | Localized notice explicitly says read-only narrow Day (`messages.js:353-355`); disabled tiles, missing workflow controls, empty request log, and byte-equal stored document at `MatrixLensBrowserIT.java:43-47,75-76,98-99`; Proposal decisions absent at `ProposalReviewBrowserIT.java:63-69` | STRONG | yes |
| G3: desktop range preference retained | Narrow weekday leaves Week stored | Exact `{version,range,weekdayId}` at `MatrixLensBrowserIT.java:63-65`; desktop Week after reload and resize at `:78-86`; `inspection-state.js:111-116,132-136` never changes range in narrow weekday selection | STRONG | yes |
| Success postcondition | Read one group's day at 390 px | One class, teacher, and room row with exact readable tiles (`MatrixLensBrowserIT.java:39-55,67-73,95-97`), scroll assertion and inspected screenshots | STRONG | yes |
| Minimal guarantee | No workspace or desktop preference change | Empty mutation logs and exact stored document at `:75-76,98-99,125-126`; Week preference retained while narrow weekday changes at `:63-65` | STRONG | yes |
| Extends UC-1 at 1a | Narrow branch of approved lens choice | `app.js:854-856,935-964` branches on viewport and feeds the approved Teacher/Room filter state into `renderDayMatrix`; UC-1/UC-2 lens cases in `MatrixLensBrowserIT` passed 20/20 with the new UC-4 cases | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Lens "MUST be represented by the existing `teacherFilterId` and `roomId`" and declared selections clear the other | `app.js:957-964,1034-1043` uses the existing transition; `inspection-state.js:86-96` enforces exclusivity, refusal, and reset; browser picker switches Teacher to Room at `MatrixLensBrowserIT.java:49-70` and UC-1 refusal remains green in 20/20 | Pass |
| RULE-2 | Preference "MUST keep exactly `{ version, range, weekdayId }`" and no lens persistence | `inspection-state.js:125-136`; exact Week/Friday value, reload without lens, and 701 px Week at `MatrixLensBrowserIT.java:63-65,78-86` | Pass |
| RULE-3 | `renderWeekMatrix` and `renderDayMatrix` "MUST be the only functions that produce matrix markup" | Narrow and desktop Day call `renderDayMatrix` (`app.js:934,888`); it consumes the shared row groups and cell lookup (`day-renderer.js:2-10`), and tiles come from `lessonButton`/`comparisonLessonButton` (`app.js:1092-1120`). No separate focused or lens renderer remains | Pass |
| RULE-5 | Empty lens cells "MUST be marked unavailable only" outside a declared set; class rows must not | `app.js:991-995,1017-1020`; teacher, room, and class empty-cell assertions at `MatrixLensBrowserIT.java:58-62,73,113-117` check text and exact cells | Pass |
| RULE-6 | Narrow picker "MUST NOT issue any non-GET request" and must leave stored document byte-identical | `Workbench.java:319-345` network log; `MatrixLensBrowserIT.java:75-76,98-99,125-126` checks it and version plus document through `WorkbenchBrowserSupport.java:184-187` | Pass |
| RULE-7 | New strings "MUST be a `messages.js` entry" and tile accessible names follow the normative table | `messages.js:350-355` supplies picker and notice; `app.js:920-934,2094-2099` uses those entries and escapes entity names; narrow tiles use the same `lessonButton` and `comparisonLessonButton` accessible-name builders as the green UC-1 lens cases (`MatrixLensBrowserIT.java:151-156`); visible notice and matrix label checked at `:43-56` | Pass |
| RULE-8 | UC-4 "MUST remove the narrow agenda, `focused-renderer.js`, `renderFocused()`" and focused state | The diff deletes `focused-renderer.js`, `renderFocused`, old `focusedType`/`focusedId` and focused state functions, unused message keys, and focused CSS. Production search finds no `focused-renderer`, `renderFocused`, `openFocused`, or `focusedType`; DOM absence at `MatrixLensBrowserIT.java:46-47` | Pass |
| RULE-9 | Focused-dependent browser assertions "MUST be replaced in the same UC commit" | Diff pairs each removed narrow focused assertion with Day-grid assertions in `AcceptedInspectionBrowserIT`, `ProposalReviewBrowserIT`, `RepairCompletionBrowserIT`, `RepairDraftBrowserIT`, and `RepairRunBrowserIT`; `Workbench.focusEntity` becomes native narrow-picker helpers. Accepted (11/11), targeted Proposal (1/1), isolated Repair Draft (1/1), and matrix (20/20) paths run. The repair/proposal methods in the known baseline still fail before their replaced narrow assertions, so those replacements are verified by source diff rather than a passing runtime case | Pass |

## Related-UC Regression

| Use case or surface | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 and UC-2 | Approved Teacher/Room lens state and Week/Day matrix | `MatrixLensBrowserIT` passed 20/20 in the independent full run, including the unchanged wide journeys | Pass |
| UC-3 | Pending-walkthrough tile/inspector/edit surface | The same 20/20 class includes UC-3 journeys; its status remains `PENDING_WALKTHROUGH` | Pass; no approval inferred |
| Accepted inspection | Superseded narrow focused assertions | `AcceptedInspectionBrowserIT` passed 11/11 in the independent full run | Pass |
| Proposal review | Superseded narrow comparison agenda | Targeted `ProposalReviewBrowserIT.comparesOneSidedAndSameSlotChanges` passed 1/1. Two other Proposal methods failed at their established pre-UC-4 points | Pass for affected narrow path; baseline retained |
| Repair Draft and manual-draft persistence | Narrow Draft and shared durable state | `RepairDraftBrowserIT.retainsAcceptedCanvasThroughTeacherDraftModesAndReload` passed 1/1 and `WorkspaceManualDraftIT` passed 11/11 in isolation; four extra full-run kernel timeouts did not reproduce | Pass in isolation |
| Other repair, planning, import, and scale paths | Shared workspace | `ScaleTimingBrowserIT` 3/3, `WorkspaceImportIT` 25/25, `WorkspaceInitialPlanningIT` 8/8, `WorkspaceRepairDraftIT` 11/11, and `WorkspaceRepairPlanningIT` 10/10; the remaining repair/proposal failures are the exact 11-method baseline | No new reproducible regression |

## Lifecycle, Security, and Hygiene

Timetable-bearing Current, Manual Draft, Repair Draft, Solving Repair, Initial Proposal, and Repair Proposal branches use the same `renderNarrowReadOnly` and `renderNarrowDay` path (`app.js:95-180,207-210,496-510,634-640,654-657,685-687,854-856`). `renderModeNavigation` withholds desktop modes at narrow width (`app.js:190-205`); the branch returns before desktop editing and decision bindings. This local workspace has no login, role, owner, or logout flow. The submission changes no route or security configuration; narrow actions consume the existing `GET /workspace/**` and `GET /api/workspace` surface, and the recorded browser journeys send no non-GET request.

| Lifecycle with a timetable | Narrow displayed source | Narrow actions |
|---|---|---|
| Initial Proposal | Initial proposal result (`app.js:95-107,207-210`) | Day picker only; no initial decision |
| Accepted Baseline | Accepted result (`app.js:108-119,496-510`) | Day picker only; no edit or repair setup |
| Repair Draft | Accepted result in Current or Draft presentation (`app.js:120-133,654-657`) | Day picker only; no draft edit or solve |
| Solving Repair | Accepted result (`app.js:134-140,685-687`) | Day picker only; no cancellation |
| Repair Proposal | Comparison, with Proposal mode selected (`app.js:141-152,270-272`) | Day picker only; no proposal decision |
| Manual Draft | Accepted or manual-draft assignments by selected mode (`app.js:153-180,842-847,634-640`) | Day picker only; no manual edit or publication |

The test harness isolates browser storage and PostgreSQL state per case (`WorkbenchBrowserSupport.java:40-61,134-148`), reports zero skips, and produced no tracked changes. The diff adds no architecture-test exemption. Long-name 390 px screenshots and DOM width assertions support presentation fidelity; administrator visual acceptance remains outstanding.

## Findings

None. The full-suite variance and unchanged baseline are stated in Summary and Related-UC Regression; neither is attributed to a reproducible UC-4 behavior or evidence gap.

## Walkthrough

Automated evidence passes. UC-4 is a UI use case, so approval waits for the administrator's confirmation of this UC-derived script. Use a workspace with a displayed timetable and set the browser to 390 CSS px wide.

1. Open the workspace. Confirm it says **Read-only Day matrix**, names the current lifecycle, and shows the first class's day in one horizontally scrollable matrix row. Editing, run cancellation, and proposal decisions must not appear.
2. Use **View by** to choose another class, then Teacher and Room. For each, check the one-row header and Day tile fields: class shows subject/teacher/room; teacher shows subject/room/class; room shows subject/teacher/class.
3. Change the weekday to one with no lessons for the selected entity. The row remains empty. If that teacher or room declares availability, cells outside it say **Unavailable**; an entity without a declaration makes no availability claim.
4. Return to a desktop width. The prior desktop Week/Day range remains selected. Narrow picker and weekday changes must not alter the accepted timetable, draft, or proposal.

User result: pending.

## Status Update

UC-4: `READY_FOR_CONVERGENCE` -> `PENDING_WALKTHROUGH`. Next eligible: none while UC-3 and UC-4 await walkthrough confirmation.

## Response to execute

PENDING WALKTHROUGH: UC-4 automated convergence passes; await the administrator's 390 px class, teacher, room, and empty-day walkthrough confirmation.
