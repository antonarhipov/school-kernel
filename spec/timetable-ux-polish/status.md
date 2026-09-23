# Use-Case Status: timetable-ux-polish

## Current

- Use case: UC-4
- Status: READY_FOR_CONVERGENCE
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | external `timetable-inspection-ux` UC-2 approval (met) | `69dad9d` | `convergence/UC-1.md`: APPROVE; G-1–G-3 resolved, walkthrough passed |
| UC-2 | APPROVED | UC-1 | `11beb7f` | `convergence/UC-2.md`: APPROVE WITH NOTES; manual walkthrough confirmed |
| UC-3 | APPROVED | UC-2 | `3f96d79` | `convergence/UC-3.md`: APPROVE; earlier G-1/G-2 resolved, UC-3 walkthrough passed |
| UC-4 | READY_FOR_CONVERGENCE | UC-3 | HEAD at convergence (revision of `6310bc1`) | `convergence/UC-4.md`: REJECT; C-1, G-1, G-2; reconvergence pending |
| UC-5 | NOT_STARTED | UC-1, UC-2, UC-3, UC-4 | - | - |

## UC-1 Evidence

- Started from: `0b27b9d034a163a8a7ca1aaa9b4897d0b5d0b92b` (2026-09-23)
- Pre-existing dirty files: `.idea/encodings.xml`; untracked `spec/timetable-ux-polish/rules.md` and `spec/timetable-ux-polish/status.md` (earlier dependency ledger)
- Implementation submission: HEAD at convergence
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,inspection-state.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-1.md}`
- Commands and results: `./mvnw -pl timetable-workspace -am -Dit.test=WorkspaceBrowserIT#inspectsPolishedCurrentWorkbenchInRealBrowser -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 1 browser); `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, all workspace integration tests including 18 browser; 0 failures/errors/skips). `git diff --check` PASS; generated `.output.txt` removed, no tracked runtime data changed.

| Contract element | Evidence |
|---|---|
| UC-1 main 1–8; G1, G3, G4, G5; success | `WorkspaceBrowserIT.inspectsPolishedCurrentWorkbenchInRealBrowser` (lines 327–424), `app.js` mode navigation, whole-school layout, and contextual inspector; unchanged stored document and zero workspace requests on collapse |
| UC-1 2a, 3a, 4a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser` and existing invalid-preference/focused-empty browser coverage; full browser suite green |
| UC-1 2b, 8a; G2, G6, G7; minimal guarantee | `WorkspaceBrowserIT.inspectsPolishedCurrentWorkbenchInRealBrowser` verifies narrow view, Utilities contents, intercepted export failure, exact unchanged stored document |
| UC-1 5a, 6a | Existing supported repair entry stays in Current; `app.js` `lessonDetails` and `entityName` retain stable ID/unavailable-name fallback |
| RULE-1, RULE-2 | `app.js` reuses accepted model and snapshot; `inspection-state.js` derives modes without persisting them; real browser preserves exact stored state |
| RULE-6, RULE-7, RULE-8 | `app.js`, `messages.js`, `styles.css` escaped labels, fixed inspector, Utilities-only export, and narrow read-only; entire browser and HTTP suite green |
| RULE-9, RULE-10 | Normative scale fixture in `WorkspaceBrowserIT` and isolated DB; new actor-boundary journey plus full browser suite |

- Convergence verdict: REJECT at `spec/timetable-ux-polish/convergence/UC-1.md`; findings G-1, G-2, G-3 require browser evidence before approval.

## UC-1 Revision Evidence

- Revision from: `ca4a3f5` (prior convergence verdict G-1–G-3); submission: HEAD at convergence.
- Product decision: user approved revising unreachable extension 6a to pre-acceptance refusal (extension 1a). The verifier cannot accept an entity without a name or an assignment with an unresolved reference; the defensive rendering fallback remains.
- Changed files for revision: `spec/timetable-ux-polish/{spec.md,rules.md,status.md,checkpoints/UC-1.md}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`.
- Focused browser runs: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#preparesProtectedRepairDraftInRealBrowser+showsDeclaredEmptyAcceptedTimetableInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 2 browser); `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#refusesUnmappableAcceptedMetadataInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 1 browser).
- Full regression: `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, 67 integration, 0 failures/errors/skips; includes all 19 browser tests). First attempt exceeded a 600-second timeout without an observed test failure; the completed second attempt used 1800 seconds. Generated `.output.txt` removed; no tracked runtime file changed.

| Convergence finding / contract | Revision evidence |
|---|---|
| G-1; UC-1 trigger, G4, RULE-2 | `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser`: real Draft→Current→Draft→Current navigation retains accepted lesson, Week, cohort filter, exact durable draft/accepted document, and workspace request count. |
| G-2; revised UC-1 1a, RULE-6, RULE-9, RULE-10 | `WorkspaceBrowserIT.refusesUnmappableAcceptedMetadataInRealBrowser`: packaged kernel rejects imported accepted pair lacking a lesson display name; no Current/inspector and exact unchanged EMPTY workspace document/version. `spec.md` and `rules.md` now match verified acceptance. |
| G-3; UC-1 2a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser`: asserts pressed Current, accepted identity, zero invented lessons, declared slots and unchanged state. |

## UC-2 Evidence

- Started from: `7b06b3300e08948f855552b02c50fb6a9ac33a0d` (2026-09-23)
- Pre-existing dirty files: `.idea/encodings.xml` (preserved, unrelated)
- Prior convergence findings: none; UC-1 approved in `convergence/UC-1.md`.
- Implementation submission: HEAD at convergence.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-2.md}`.
- Commands and results: focused five-browser `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#retainsAcceptedCanvasThroughTeacherDraftModesAndReload+resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources+keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly+refusesSolveAfterRealDraftPersistenceFailure+preparesProtectedRepairDraftInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 5 browser); scale-browser `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#measuresTargetScalePinFeedbackInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 1 browser); `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, 71 integration including 23 browser; 0 failures/errors/skips). `git diff --check` PASS; generated `.output.txt` removed; no tracked runtime data changed.

| Contract element | Evidence |
|---|---|
| UC-2 main 1–7; Requires UC-1; Extends UC-1 5a; G1–G5; success | `WorkspaceBrowserIT.retainsAcceptedCanvasThroughTeacherDraftModesAndReload` (1080–1135), `resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources` (1137–1205); shared accepted-model render, overlays and inspector in `app.js:350–420,455–576`; exact durable accepted bundle assertions |
| UC-2 2a; minimal guarantee | `WorkspaceBrowserIT.refusesSolveAfterRealDraftPersistenceFailure` (1255–1295) installs a real database failure, asserts no write/version advance, no run/proposal and disabled solve; guarded service path unchanged |
| UC-2 3a, 7a, 7b; G6 | `WorkspaceBrowserIT.keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly` (1207–1253): exact room/period intent, zero effects, narrow read-only, reload, explicit discard and unchanged accepted baseline |
| UC-2 4a, 4b, 5a; G3, G8 | `WorkspaceBrowserIT.resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources` (1137–1205): exact conflict, disabled solve, cancel with no mutation, confirm/undo preserving individual pin; `app.js:362–437,555–576` |
| UC-2 6a | `app.js:646–659,708–713,830–834` clears only filtered-out selection and announces it; Current/Draft use identical accepted ID set, so mode switching does not introduce a new unrepresentable lesson or entity |
| UC-2 G4, G7; RULE-1, RULE-2, RULE-3, RULE-4 | `app.js:44–143,350–437,455–576,877–930`; `inspection-state.js` unchanged mode authority; focused tests compare exact document and version; existing HTTP lifecycle/refusal tests passed in full suite |
| RULE-6, RULE-7, RULE-8 | `app.js` escapes school-controlled names, retains CSP/CSRF/If-Match and native controls; `styles.css:62–76` fixed inspector; `messages.js:89–90,176–181` catalog cues; narrow browser and full security suite green |
| RULE-9, RULE-10 | `WorkspaceBrowserIT` isolated Testcontainers fixture, accepted verifier, five actor-boundary journeys; complete shared browser suite (23 tests) and full workspace suite green |

## UC-3 Evidence

- Started from: `71c7c0a8f40254dd650279d6d2486ffab4b85022` (2026-09-23); prior UC-3 convergence findings: none.
- Pre-existing dirty files: `.idea/encodings.xml` (unmodified by UC-3, excluded from submission).
- Implementation submission: HEAD at convergence.
- Changed files: `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceRepository.java`, `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceBrowserIT.java,WorkspaceRepairPlanningIT.java}`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-3.md}`.
- Commands and results: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser+showsFailedRepairEvidenceAndGatedRetryInRealBrowser+generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 3 browser); `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, 73 integration, 0 failures/errors/skips); final added feasible-handoff assertions rerun with `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 1 browser). `git diff --check` PASS; test-generated `.output.txt` removed; no tracked runtime data changed.
- Revision from: `d44501e20d68a44399a494198ee1c624c08229d2` (2026-09-23); findings `G-1` and `G-2` in `convergence/UC-3.md`. Only browser evidence, checkpoint, and status changed; production behavior and spec are unchanged.
- Revision commands and results: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#restoresInterruptedRepairAfterActualApplicationRestartInRealBrowser+followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 2 browser); `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, 75 integration including 27 browser, 0 failures/errors/skips). Test-generated `.output.txt` removed; `git diff --check` PASS; no tracked runtime data changed.

| Contract element | Evidence |
|---|---|
| UC-3 main 1–4; Requires UC-2; G1, G2, G4, G5 | `WorkspaceBrowserIT.inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser` (`:1439–1512`): real saved Draft/pin/range/selection -> guarded run with exact run ID/limit/accepted bundle/Draft, selected lesson, frozen controls, stable DOM across polls, Current/Draft/Solving and focused context; no run restart on navigation. `app.js:36–44,106–130,369–399,470–535` reuses accepted model and contextual run controls. |
| UC-3 main 5–6; G3, G4, G8; success | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1309–1425`): real packaged kernel, verified feasible run, retained accepted identity/selected lesson and exact accepted bundle/Draft, no active run or Solving mode, Proposal selected with accepted canvas. Existing `WorkspaceRepairPlanningIT` tests independent verification and stale/rejected output. |
| UC-3 2a, 2b; G1, G4, G7; minimal | `WorkspaceBrowserIT.inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser` (`:1477–1512`): cancellation and startup-recovery listener through browser reload, exact unchanged accepted bundle/Draft and no proposal; `WorkspaceRepository.java:170–185` records safe failed outcome on restart; late released process cannot apply. |
| UC-3 3a; G2 | `app.js:245–283,449–460` retains accepted IDs across modes and clears an out-of-Day selection with explanation; UC-1 and UC-2 browser regressions exercise representable context and accepted inspection fallback. Modes share exactly the same accepted model, so an identity present in one mode is not absent in another. |
| UC-3 5a, 5b, 6a; G3, G5; minimal | `WorkspaceBrowserIT.showsFailedRepairEvidenceAndGatedRetryInRealBrowser` (`:1516–1570`): no feasible, invalid input, transport, interruption, mismatched/rejected output and bounded timeout; safe inspector/Utilities messages, no proposal, exact unchanged Draft/baseline; unchanged retry runs with PT2M only after eligible no-feasible result, not after cancellation or other failures. Existing `WorkspaceRepairPlanningIT` proves HTTP refusal codes and absence of unauthorized process launches. |
| UC-3 G6 | Browser narrow viewport during the live run (`WorkspaceBrowserIT.java:1482–1487`) asserts read-only focused schedule, no modes/cancellation, identical durable run ID/document, responsive return to desktop inspector. |
| RULE-1, RULE-2 | `app.js:36–44,47–142,470–535`, unchanged `inspection-state.js`; shared accepted model and native packaged assets; exact durable document asserted across switches; presentation preference remains scoped Day/Week only. |
| RULE-3, RULE-4 | `app.js:369–399,470–585` freezes Draft and retains accepted lesson overlays; existing `RepairPlanningService` conditional run guard plus `WorkspaceRepository.java:170–185` recovery; browser and HTTP suite cover cancellation, late result, refusal and retry. |
| RULE-6, RULE-7, RULE-8 | `app.js:287–348,369–399,462–531,569–590`, `messages.js:119–129`: escaped safe diagnostics, native inspector/Utilities, read-only narrow; existing HTTP security regression green, no new routes or browser storage. |
| RULE-9, RULE-11 | `WorkspaceBrowserIT.java:1309–1568` verified accepted fixture, isolated PostgreSQL, packaged success/recovery and production-conformant process failures; target-scale browser and all approved shared tests in the full suite green. |

| Convergence finding / contract | Revision evidence |
|---|---|
| G-1; UC-3 2b, G7, RULE-3, RULE-11 | `WorkspaceBrowserIT.restoresInterruptedRepairAfterActualApplicationRestartInRealBrowser` (`:1573–1627`): real application on random port with isolated PostgreSQL, browser-started blocked repair, application stop, fresh application startup/recovery, browser navigation to new port, exact original accepted/Draft, original run ID/`INTERRUPTED` status, version increment, no proposal, late-result release with no write. |
| G-2; UC-3 G8, RULE-9, RULE-11 | `WorkspaceBrowserIT.followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser` (`:1629–1706`): independently verified accepted pair and revisions; 1,000 lesson IDs, 60 cohorts, 100 teachers, 100 rooms, 60 periods; real teacher-16/period-0 Draft, selected lesson-960, blocked run and mode transitions with exact accepted/Draft/Day IDs, cancellation, and rejected kernel output with no proposal or mutation. |

## UC-4 Evidence

- Started from: `4bc2c64042fe54ed2309c408d8f96df703372566` (2026-09-23); prior convergence findings: none.
- Revision from: `8c6ac9f3201c961aaa30e09e0ad6036191c2a97f`; findings C-1, G-1, G-2 in `convergence/UC-4.md`; pre-existing dirty `.idea/encodings.xml` excluded.
- Revision submission: HEAD at convergence. Changed files since `8c6ac9f`: `timetable-workspace/src/main/resources/static/workspace/{app.js,focused-renderer.js,messages.js}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceBrowserIT.java,WorkspaceRepairPlanningIT.java}`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-4.md}`.
- Revision commands and results: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#reviewsIndependentlyVerifiedNormativeRepairInRealBrowser+retainsBothSidesInFocusedResourceSchedulesInRealBrowser+revisesAndDiscardsVerifiedNormativeRepairInRealBrowser+displaysEmptyComparisonGroupsInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 4 browser); `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, 81 integration including 33 browser, 0 failures/errors/skips per `failsafe-summary.xml` and `WorkspaceBrowserIT` report). `git diff --check` PASS. Test-created `.output.txt` removed; no tracked runtime data changed.

| Convergence finding / contract | Revision evidence |
|---|---|
| C-1; UC-4 1a, 3a, G4 | `focused-renderer.js:1–14`, `app.js:694–707`: focus gathers matched lesson IDs then retains both comparison representations even if teacher, room, or class differs on the other side, labelling the linked side as belonging to the other resource. `WorkspaceBrowserIT.retainsBothSidesInFocusedResourceSchedulesInRealBrowser` (`:1977–2020`) checks old/new teacher and room focus in both directions, class and subject filters and search while preserving exact durable document. |
| G-1; UC-4 main 1–6, 2a, G1, G2, G4, G8, RULE-9, RULE-12 | `WorkspaceBrowserIT.reviewsIndependentlyVerifiedNormativeRepairInRealBrowser` (`:2023–2205`): real browser stages teacher-16/period-0 Draft and room pin on lesson-500 from independently verified 1,000-lesson accepted baseline. Test launcher synthesizes deterministic successor result but both original and successor pass the real packaged `KernelVerifier` (`:2470–2560`), and the production `KernelPlanner` persists it after its own verify; exact accepted/draft/proposal retained, result and revision equality, full 1,001-tile roster, six exact category values, direct/ripple/unique counts, group memberships, room-only and period-change inspector sides, honest protected display, explicit accepted successor and cleared proposal/draft. `revisesAndDiscardsVerifiedNormativeRepairInRealBrowser` (`:2208–2255`) exercises both other decisions on the same verified full-school candidate. Supplemental `displaysEmptyComparisonGroupsInRealBrowser` covers all-zero grouping branch without ever submitting the UI-only fixture for acceptance. |
| G-2; UC-4 main 2–5, 3a, 4a, G1, G2, G5, RULE-6 | `WorkspaceBrowserIT.reviewsIndependentlyVerifiedNormativeRepairInRealBrowser` compares every `review.changedLessons` field, six category IDs/counts/lesson IDs, all four group maps/context memberships, two complete displayed old/proposed records (weekday, subject, class, teacher, period, room by name and ID) and full school roster with fixed expected values; tests real proposed teacher-16 unavailable Monday/accepted teacher-16 assigned Monday, distinct labels and exact selected identity/range after acceptance. Synthetic UI-only fixture still covers one-sided additions/cancellations, same-slot changes and missing proposed names, while `retainsBothSidesInFocusedResourceSchedulesInRealBrowser` now asserts subject investigation, cohort filter, old/new resource focus and one-sided search with opposite side retained and side-specific match cue. |
- Pre-existing dirty files: `.idea/encodings.xml` (unrelated; preserve).
- Implementation submission: HEAD at convergence.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,proposal-comparison.js,focused-renderer.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-4.md}`.
- Commands and results: red real-browser reproducer for absent whole-school accepted/proposed tiles; `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#measuresTargetScaleProposalReviewOpeningInRealBrowser+comparesOneSidedAndSameSlotChangesInRealBrowser+generatesRepairProposalInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS (26 unit, 3 browser); `./mvnw -pl timetable-workspace -am verify` PASS (26 unit, 77 integration including 29 browser, 0 failures/errors/skips); final full suite after compilation correction PASS (26 unit, 77 integration, 0 failures/errors/skips). `git diff --check` PASS. Test-generated `.output.txt` removed; no tracked runtime data changed.

| Contract element | Evidence |
|---|---|
| UC-4 main 1–2; Requires UC-3; 1a, 1b, 1c, 2a; G1, G2, G4 | `proposal-comparison.js:1–33` joins actual accepted/proposed assignments and authoritative review by ID with accepted/destination/combined/one-sided/unchanged representations. `app.js:128–166,189–205,467–527,553–612` renders them through shared Week/Day/focused/inspector structures with exact authoritative totals and guarded decision controls. `WorkspaceBrowserIT.java:1313–1387,1859–1920,1923–1973` exercises packaged verified repair and each presentation shape, category overlap/zero, direct/ripple totals and protected/unchanged lessons. |
| UC-4 main 3–5; 3a, 3b, 4a; G3, G5, G6 | `app.js:206–226,467–527,553–612,850–997` retains identity, selection and both sides through search/filter/investigation/Day/Week/focused return, derives exact side labels and unmappable metadata cues; `WorkspaceBrowserIT.java:1371–1378,1859–1920,1923–1973` checks selected sides, matching-side cue, grouped navigation, fallback ID, mode-only durable equality, collapse/reopen/focus, plus unit regression. |
| UC-4 main 6; success; G7, G8 | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1313–1466`) uses actual packaged kernel and verified proposal, prior exact accepted/draft/bundle and persisted no-mutation mode checks, explicit confirmation, exact successor definition/result, only Current available; `RepairProposalService` already guards identity, atomic durable acceptance, and rollback. |
| UC-4 5a, 5b, 6a; minimal | `WorkspaceBrowserIT.revisesDiscardsAndRejectsStaleProposalInRealBrowser` (`:1977–2021`): three browser journeys compare exact prior accepted/draft, show Draft without proposal, and preserve service refusal for stale identity; existing `WorkspaceRepairPlanningIT` covers service denial codes. |
| UC-4 6b; G7, minimal | `WorkspaceBrowserIT.generatesRepairProposalInRealBrowser` (`:1388–1428`) triggers real PostgreSQL write failure through browser, compares entire document/version, verifies Proposal is still shown and retryable, then explicitly retries and verifies accepted successor; `app.js:1021–1043` reports non-advance without false success. |
| UC-4 6c; G4, G9 | `WorkspaceBrowserIT.comparesOneSidedAndSameSlotChangesInRealBrowser` (`:1963–1973`): narrow browser shows both labelled sides in read-only focused agenda, hides decision/matrix controls, preserves exact stored document; existing approved UC-1/UC-2/UC-3 desktop/narrow journeys remain green. |
| RULE-1, RULE-2, RULE-5, RULE-12 | `app.js:1–7,128–166,467–527,850–997`; `proposal-comparison.js:1–33`; accepted model/state owner and native assets preserved. Verified repair from UC-3 is consumed only in `REPAIR_PROPOSAL`; three-mode switching does not change durable bytes/version. |
| RULE-3, RULE-8 | `app.js:199–226,1021–1043`, existing `RepairProposalService` and secured routes; verified failed/rejected/stale actions and exact rollback in browser and unchanged HTTP suite; no new endpoints/storage/imports/dependencies. |
| RULE-6, RULE-7 | `app.js:178–205,553–612,667–712,850–997`, `messages.js:124–140`, `styles.css:193–210`; exact IDs plus escaped display fallback, proposed availability, text cues, inspector and narrow safeguards; browser assertions cover missing proposed name, native actions and focused return. |
| RULE-9 | `WorkspaceBrowserIT.java:1313–1466,1859–2021,2250–2310`: isolated PostgreSQL and actual kernel-produced feasible repair plus explicit synthetic DOM-only edge-case snapshots, target-scale fixture, exact bundle comparisons, browser UI assertions; full relevant suite green and no tracked runtime-data changes. |

## Blockers

none. Extension 1a reflects the user-approved verifier-refusal decision; all prior findings have revised browser evidence pending reconvergence.

## Deviations

none