# Use-Case Checkpoint: UC-3 - Inspect conflict details and explanation

## Summary

- Status: READY_FOR_CONVERGENCE
- Submission commit: HEAD at convergence
- Relations verified: Extends UC-2 (extension point 6a), requires UC-1

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-3 main steps 1-4 | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:272`), UI indicator & overlay binding in `app.js` and `styles.css` | PASS |
| UC-3 extension 2a (Multi-conflict enumeration) | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:302`) verifying multiple distinct conflicts on a single lesson (teacher, room, cohort clashes) | PASS |
| UC-3 G1 (Complete causal explanation) | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:309`) verifying `resourceType`, `resourceId`, `periodId`, `description`, and `competingLessonIds` | PASS |
| UC-3 G2 (Non-mutating inspection) | `WorkspaceManualDraftIT.inspectsConflictDetailsAndCausalExplanations` (`WorkspaceManualDraftIT.java:328`) verifying GET /api/workspace preserves ETag, version, and draft conflicts | PASS |
| UC-3 success postcondition | UI overlay markup (`conflictOverlayMarkup`) and inspector cues (`manualCues`) displaying itemized conflict details and competing assignments | PASS |
| UC-3 minimal guarantee | Canvas and draft state unchanged during inspection; verified in `WorkspaceManualDraftIT.java:328` | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-3 (Normative constraints) | Causal explanations explicitly report normative violations (`TEACHER_CLASH`, `ROOM_CLASH`, `COHORT_CLASH`, `ROOM_INCOMPATIBLE`, `TEACHER_UNAVAILABLE`, `ROOM_UNAVAILABLE`); tested in `WorkspaceManualDraftIT.java:302` | PASS |
| RULE-5 (Conflict visibility & explanation) | Explanatory overlay rendered directly adjacent to conflicting cell (`.conflict-overlay` positioned relative to `.lesson-cell.conflicting`), itemizing conflict type, reason, and competing lessons; tested in `WorkspaceManualDraftIT.java:309`, `styles.css`, `app.js` | PASS |
| RULE-7 (Automated integration verification) | `WorkspaceManualDraftIT.java` executed against Testcontainers PostgreSQL container | PASS |

## Validation

- Focused commands: `mvn test -pl timetable-workspace -Dtest=WorkspaceManualDraftIT` (8 run, 0 failures, 0 errors, 0 skipped)
- Working tree impact from tests: none
- Runtime evidence: Conflicting cells display `⚠️` conflict indicator. Clicking or hovering the indicator displays `.conflict-overlay` adjacent to the cell with conflict code, description, and competing assignments. Inspector sidebar also displays detailed conflict breakdown with competing assignments. Overlays dismiss on click-outside or Escape key. Non-mutating inspection preserves workspace version and ETag.
- Changed files:
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/messages.js`
  - `timetable-workspace/src/main/resources/static/workspace/styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceManualDraftIT.java`
- Approved UCs regression-tested: UC-1 tests passed, UC-2 tests passed (8/8 in `WorkspaceManualDraftIT`).

## Notes

- Both week and day views, as well as focused single-resource views, support the conflict indicator, conflict overlay, and Inspector panel breakdown.
