# Use-Case Status: timetable-ux-polish

## Current

- Use case: UC-2
- Status: APPROVED
- Next eligible: UC-3

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | external `timetable-inspection-ux` UC-2 approval (met) | `69dad9d` | `convergence/UC-1.md`: APPROVE; G-1–G-3 resolved, walkthrough passed |
| UC-2 | APPROVED | UC-1 | `11beb7f` | `convergence/UC-2.md`: APPROVE WITH NOTES; manual walkthrough confirmed |
| UC-3 | NOT_STARTED | UC-2 | - | - |
| UC-4 | NOT_STARTED | UC-3 | - | - |
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

## Blockers

none. Extension 1a reflects the user-approved verifier-refusal decision; all prior findings have revised browser evidence pending reconvergence.

## Deviations

none