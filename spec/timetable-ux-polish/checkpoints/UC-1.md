# Use-Case Checkpoint: UC-1 - Inspect the accepted timetable in the persistent workbench

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `ca4a3f5` (revision of submission `1e0fc8a`; prior convergence G-1–G-3)
- Submission commit: HEAD at convergence
- Relations verified: no internal Requires, Includes, or Extends; prerequisite `timetable-inspection-ux` UC-2 is APPROVED. Existing accepted inspection behavior is regression-tested.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main steps 1–8, success | `WorkspaceBrowserIT.inspectsPolishedCurrentWorkbenchInRealBrowser` exercises scale fixture, Week/Day, filter, focused return, selection, fixed-width inspector collapse/reopen, and Utilities through the packaged browser | PASS |
| UC-1 3a, 4a | Prior inspection browser regression covers invalid preference and empty focused schedules | PASS |
| UC-1 2b | `WorkspaceBrowserIT.inspectsPolishedCurrentWorkbenchInRealBrowser`: 390px read-only focused view without desktop controls | PASS |
| UC-1 5a | `app.js` preserves existing supported `startRepairForm` in the accepted whole-school context; prior browser repair journeys in full suite | PASS |
| UC-1 1a (revised from 6a by user decision) | `WorkspaceBrowserIT.refusesUnmappableAcceptedMetadataInRealBrowser`: browser imports a definition with missing accepted lesson name and matching result; kernel rejects it, no inspector or Current, exact durable EMPTY state | PASS |
| UC-1 8a, minimal guarantee | Browser blocks accepted export; reports no bundle, retains Current/filter/selection and exact stored document | PASS |
| UC-1 2a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser`: Current selected, accepted identity, all declared empty positions and no invented lessons | PASS |
| UC-1 trigger, G4 | `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser`: Draft→Current→Draft→Current retains Week, selected accepted lesson and cohort filter with no workspace request or durable change | PASS |
| UC-1 G1–G7, success postcondition | Browser asserts fixed inspector width, larger canvas on collapse, same selected lesson on reopen, unchanged workspace request count, return context, disclosure segregation, and exact durable bytes | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1, RULE-2 | `app.js` uses packaged snapshot and accepted model; `inspection-state.js` holds ephemeral mode/inspector/Utilities and persists only range/weekday | PASS |
| RULE-6, RULE-7 | `app.js`, `styles.css`, `messages.js` retain defensive safe fallback, native disclosure and controls, fixed inspector, narrow read-only surface; kernel rejects malformed pair at browser boundary | PASS |
| RULE-8 | No route/service changes; accepted export only in accepted lifecycle; full HTTP and browser regression | PASS |
| RULE-9, RULE-10 | `WorkspaceBrowserIT` normative scale fixture in isolated Testcontainers DB; actor-boundary browser verification and stored document comparisons | PASS |

## Validation

- Focused commands: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#preparesProtectedRepairDraftInRealBrowser+showsDeclaredEmptyAcceptedTimetableInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` (26 unit, 2 browser); `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#refusesUnmappableAcceptedMetadataInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` (26 unit, 1 browser); both green. Original focused Current test also green.
- Full relevant suite: `./mvnw -pl timetable-workspace -am verify` (26 unit, 67 integration including 19 browser; 0 failed, 0 errors, 0 skipped). First attempt timed out at 600 seconds; completed rerun with 1800-second timeout.
- Working tree impact from tests: no tracked runtime data modified; generated `.output.txt` removed. Pre-existing `.idea/encodings.xml` and untracked `spec/timetable-ux-polish/rules.md` left untouched.
- Runtime evidence: administrator opens verified accepted scale timetable, inspects and returns from focused class, collapses/reopens contextual inspector, opens Utilities and receives explicit intercepted-export failure with unchanged durable workspace.
- Changed files in this revision: `spec/timetable-ux-polish/{spec.md,rules.md,status.md,checkpoints/UC-1.md}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`. Original implementation remains at `1e0fc8a`.
- Approved UCs regression-tested: `timetable-inspection-ux` UC-1, UC-2, UC-3, and existing workspace lifecycle via full relevant suite.

## Notes

User-approved change: UC-1 extension 6a (unreachable in verified accepted input) is now extension 1a, pre-acceptance verifier refusal. Defensive UI fallback remains. Human visual walkthrough remains for convergence.

READY FOR CONVERGENCE: UC-1