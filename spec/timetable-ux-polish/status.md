# Use-Case Status: timetable-ux-polish

## Current

- Use case: UC-1
- Status: PENDING_WALKTHROUGH
- Next eligible: none (awaiting UC-1 walkthrough)

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | PENDING_WALKTHROUGH | external `timetable-inspection-ux` UC-2 approval (met) | `69dad9d` | `convergence/UC-1.md`: PENDING_WALKTHROUGH; G-1–G-3 resolved |
| UC-2 | NOT_STARTED | UC-1 | - | - |
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

## Blockers

none. Extension 1a reflects the user-approved verifier-refusal decision; all prior findings have revised browser evidence pending reconvergence.

## Deviations

none