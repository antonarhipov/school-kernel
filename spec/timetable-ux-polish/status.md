# Use-Case Status: timetable-ux-polish

## Current

- Use case: UC-1
- Status: NEEDS_REVISION
- Next eligible: UC-1 (revision)

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | NEEDS_REVISION | external `timetable-inspection-ux` UC-2 approval (met) | `1e0fc8a` | `convergence/UC-1.md`: REJECT G-1–G-3 |
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

## Blockers

none (inspection UC-2 is APPROVED following the complete browser regression)

## Deviations

none