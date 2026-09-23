# Use-Case Checkpoint: UC-2 - Prepare a repair without leaving the timetable canvas

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `7b06b3300e08948f855552b02c50fb6a9ac33a0d`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1 (uses its accepted canvas/identity/presentation owner); Extends UC-1 at 5a (supported repair entry starts from the accepted timetable).

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1–3, 6–7; G1, G2, G4, G5; success | `WorkspaceBrowserIT.retainsAcceptedCanvasThroughTeacherDraftModesAndReload` (1080–1135); `app.js:350–376,455–508,521–576` | PASS |
| Main 4–5; individual and bulk protection; G3, G8 | `WorkspaceBrowserIT.resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources` (1137–1205); inspector `app.js:362–437,546–576` | PASS |
| Extension 2a; minimal guarantee | `WorkspaceBrowserIT.refusesSolveAfterRealDraftPersistenceFailure` (1255–1295): real failed database update, unchanged accepted/document/version and no run | PASS |
| Extension 3a | `WorkspaceBrowserIT.keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly` (1207–1226): exact room and weekly period, zero effects, retained durable intent | PASS |
| Extensions 4a, 4b, 5a | `WorkspaceBrowserIT.resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources` (1149–1202): exact conflict; blocked solve; canceled preview no write; confirmed and undone bulk preserving individual pin | PASS |
| Extension 6a | `app.js:646–659,708–713,830–834` preserves range and investigation while announcing an unrepresentable filtered selection; both modes use the same accepted identities | PASS |
| Extensions 7a, 7b; G6 | `WorkspaceBrowserIT.keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly` (1227–1252): narrow no actions, default Draft on reload, confirmed discard to Current without accepted advancement | PASS |
| G7 and success/minimal postconditions | Five focused browser journeys, existing guarded service HTTP tests and 71-integration full suite; `WorkspaceBrowserIT` exact database comparisons | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1, RULE-2 | `app.js:44–143,350–359,455–508` one accepted model and existing presentation state; browser Current↔Draft/reload assertions | PASS |
| RULE-3, RULE-4 | `app.js:362–437,555–605,906–930` existing CSRF/If-Match service requests, protected preview/undo/solve; browser real persistence failure and exact conflict | PASS |
| RULE-6, RULE-7 | `app.js:362–381,521–576`, `messages.js:89–90,176–181`, `styles.css:62–76` escaped labels, catalog status, fixed inspector, native read-only narrow presentation | PASS |
| RULE-8, RULE-9 | No routes/storage/schema/dependency added; `WorkspaceBrowserIT` isolated database and verifier; full HTTP/browser suite | PASS |
| RULE-10 | `WorkspaceBrowserIT` five UC-2/UC-1 actor journeys (1080–1295); full browser suite (23 tests) and all workspace integration tests green | PASS |

## Validation

- Focused commands: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#retainsAcceptedCanvasThroughTeacherDraftModesAndReload+resolvesConflictingPinAndUndoesOnlyConfirmedBulkSources+keepsZeroEffectRoomDraftOnReloadThenDiscardsExplicitly+refusesSolveAfterRealDraftPersistenceFailure+preparesProtectedRepairDraftInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` (26 unit, 5 browser, PASS); `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#measuresTargetScalePinFeedbackInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` (26 unit, 1 browser, PASS).
- Full relevant suite: `./mvnw -pl timetable-workspace -am verify` (26 unit, 71 integration including all 23 browser, 0 failures/errors/skips).
- Working tree impact from tests: no tracked runtime changes; tool-generated `.output.txt` removed; pre-existing `.idea/encodings.xml` preserved and excluded.
- Runtime evidence: real Chromium at `/workspace/` with isolated PostgreSQL; exact accepted/draft document and lifecycle compared after mutation, presentation navigation, reload, discard and failed save.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-ux-polish/{status.md,checkpoints/UC-2.md}`.
- Approved UCs regression-tested: UC-1 and shared workspace inspection/repair flows; 23 browser tests PASS.

## Notes

- Target-scale pin feedback p95 is diagnostic only; the focused run recorded 316.4 ms against a 250 ms reference, with no normative latency threshold. The full suite passed.
- Human walkthrough remains for convergence; no actor-visible later-UC scenarios were implemented.

READY FOR CONVERGENCE: UC-2