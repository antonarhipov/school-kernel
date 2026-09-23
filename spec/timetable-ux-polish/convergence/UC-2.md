# Convergence: UC-2 - Prepare a repair without leaving the timetable canvas

## Summary

- Submission: `spec/timetable-ux-polish/checkpoints/UC-2.md` committed at `11beb7f` against `7b06b33`.
- Verdict: APPROVE WITH NOTES.
- Findings: 0 critical, 0 gaps, 0 protocol; one non-blocking diagnostic note.
- Suite: focused 26 unit + 5 browser, scale 26 unit + 1 browser, full 26 unit + 71 integration (23 browser); 0 failures, errors, or skips in each run.
- Working tree impact from verification: only tool-generated `.output.txt`, removed; pre-existing `.idea/encodings.xml` untouched.

## Protocol Gate

The sole pending UC was UC-2 (`status.md:5-17`), and its checkpoint and implementation were committed together at `11beb7f`. UC-1 is `APPROVED` (`convergence/UC-1.md`), satisfying both `Requires` and the base of `Extends` at UC-1 5a; no other UC was in progress. The checkpoint supplies main, extension, guarantee, postcondition, rule, regression, changed-file and command evidence (`checkpoints/UC-2.md:10-45`). The diff `7b06b33..11beb7f` changes only the UC-2 workbench presentation, browser tests and submission artifacts; the user's unrelated IDE change is not included. The committed submission boundary precedes this report.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator, packaged Chromium against isolated PostgreSQL | Start teacher unavailability in accepted Current; inspect Draft and round-trip through Current, focus and reload | Durable accepted overlay and retained representable context | Focused browser passed; `WorkspaceBrowserIT.java:1081-1134` compares exact intent, direct-effect ID, accepted IDs, modes, selection, range, focus return, document and reload default. |
| Administrator | Protect lesson, encounter and clear conflict, cancel/confirm/undo bulk preview | Exact conflict and protected sources; no write on cancel | Focused browser passed; `WorkspaceBrowserIT.java:1139-1203` checks conflict code/ID, disabled solve, unchanged preview/cancel document, confirmed sources and exact restored intent on undo. |
| Administrator | Stage zero-effect room rule, use narrow view, reload and explicitly discard | No invented effect; read-only narrow; prior accepted survives | Focused browser passed; `WorkspaceBrowserIT.java:1209-1251` compares exact room/period intent, zero effects, absent narrow actions, durable reload and accepted baseline after confirmed discard. |
| Administrator | Draft save fails at durable storage | Report failure; no unpersisted solve/run | Focused browser passed; database trigger causes actual failed update; `WorkspaceBrowserIT.java:1257-1294` checks visible error, disabled solve, identical full stored document/version, no run or proposal. |
| Administrator | Scale accepted timetable, repeated pin feedback | Diagnostic p95 only | Scale browser passed against 60 classes, 100 teachers/rooms and 1,000 lessons; p95 430.5 ms versus diagnostic 250 ms reference, not a contractual threshold. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Stage recurring named resource and periods in Current | `WorkspaceBrowserIT.java:1090-1102,1214-1224`, real form and exact durable intent | STRONG | yes |
| Main 2 | Enter durable Draft with Current available and context retained | `WorkspaceBrowserIT.java:1094-1118`, exact stored draft and selected Day/lesson/investigation | STRONG | yes |
| Main 3 | Accepted canvas shows effect/conflict and Current remains accepted | `WorkspaceBrowserIT.java:1095-1110,1149-1158`; `app.js:455-495,555-576` | STRONG | yes |
| Main 4 | Inspect affected assignment, protect dimensions, preview/confirm bulk | `WorkspaceBrowserIT.java:1149-1196`; `app.js:546-552,579-605,408-416` | STRONG | yes |
| Main 5 | Explain intent, effects, provenance, conflicts and readiness | `app.js:362-375,546-576`; browser asserts exact draft fields and blocking conflict (`WorkspaceBrowserIT.java:1098-1106,1152-1158,1187-1202`) | STRONG | yes |
| Main 6 | Current/Draft switching and focused schedule | `WorkspaceBrowserIT.java:1112-1125` | STRONG | yes |
| Main 7 | Restore Draft context and leave conflict-free draft ready | `WorkspaceBrowserIT.java:1117-1133,1197-1202`, full document unchanged by navigation | STRONG | yes |
| Extension 2a | Failed durable save does not enable solve | `WorkspaceBrowserIT.java:1257-1294`, real database failure, error, rollback, no run | STRONG | yes |
| Extension 3a | No affected lesson still retains staged intent | `WorkspaceBrowserIT.java:1215-1226`, explicit zero direct effects and exact room/period | STRONG | yes |
| Extension 4a | Contradictory pin chooses no winner | `WorkspaceBrowserIT.java:1149-1164`, exact conflict code and pin source; disabled solve | STRONG | yes |
| Extension 4b | Canceled preview pins nothing | `WorkspaceBrowserIT.java:1174-1184`, full stored document identical | STRONG | yes |
| Extension 5a | Conflict blocks generation and remains navigable | `WorkspaceBrowserIT.java:1150-1159`; `app.js:367,426,429-440` conflict button clears obstructing filters to reveal exact lesson | STRONG | yes |
| Extension 6a | Unrepresentable context clears only itself with explanation | Current/Draft share `acceptedModel` and the same lesson/focus identities (`app.js:44-143,455-495`), making a mode-only loss of identity unreachable; filtered-out selection follows `app.js:646-659,708-713` with notice, without clearing range/investigation | STRONG | yes |
| Extension 7a | Confirmed discard removes intent and returns Current | `WorkspaceBrowserIT.java:1236-1250`, exact accepted baseline and absent draft/run/proposal | STRONG | yes |
| Extension 7b | Reload restores durable Draft and allowed preference only | `WorkspaceBrowserIT.java:1128-1133,1237-1241`, Draft default and Day restored, selection absent, full document unchanged | STRONG | yes |
| G1 | Overlay never changes accepted assignments | `WorkspaceBrowserIT.java:1107-1115,1125-1126`; `app.js:455-495` renders accepted model | STRONG | yes |
| G2 | Shared lesson identity, no duplicate/misread through modes | `WorkspaceBrowserIT.java:1088-1118`, same exact accepted ID set and selected lesson | STRONG | yes |
| G3 | Text/non-color direct, conflict, policy/pin/unpinned cues | `app.js:543-576`; browser checks explicit direct/conflict/pin labels (`WorkspaceBrowserIT.java:1110,1150-1158,1166-1173`) | STRONG | yes |
| G4 | Presentation navigation does not alter intent | `WorkspaceBrowserIT.java:1112-1126,1174-1184`; exact durable document compares | STRONG | yes |
| G5 | Accepted definition/result never change across actions | `assertDraftUnchangedBaseline` at `WorkspaceBrowserIT.java:1097,1147-1202,1219,1291`; exact baseline on discard `:1246` | STRONG | yes |
| G6 | Narrow Draft status without mutation actions | `WorkspaceBrowserIT.java:1227-1233`; `app.js:350-356` | STRONG | yes |
| G7 | Existing protected lifecycle/refusal persists | `app.js:587-594,926-954` sends CSRF/If-Match through existing routes; full HTTP integration suite passed | STRONG | yes |
| G8 | Real browser covers declared branches and durable before/after | Focused four UC-2 journeys plus approved UC-1 regression and full 23-browser suite; assertion locations above | STRONG | yes |
| Success postcondition | Durable conflict-free Draft on same accepted canvas | `WorkspaceBrowserIT.java:1097-1118,1160-1164,1197-1202` | STRONG | yes |
| Minimal guarantee | Unchanged accepted; no conflict/unpersisted solve | `WorkspaceBrowserIT.java:1152-1158,1257-1293` | STRONG | yes |
| Requires UC-1; Extends UC-1 5a | Supported entry consumes real accepted postcondition | `WorkspaceBrowserIT.java:1087-1094,1143-1146`, existing accepted canvas and start repair form `app.js:336-347`; UC-1 approved and browser regression green | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | MUST extend packaged model/renderers; MUST NOT fork model or add dependencies | `app.js:455-495` uses `acceptedModel`, existing renderers/snapshot; six-file diff contains no dependency, backend or route change | PASS |
| RULE-2 | MUST retain representable context; persist only range/weekday; MUST NOT mutate on presentation transitions | `inspection-state.js` unchanged; `WorkspaceBrowserIT.java:1112-1133` asserts retained selection, focused return, reload default and exact unchanged stored document | PASS |
| RULE-3 | MUST use guarded service transitions and refuse stale/ineligible actions below UI | `app.js:343-347,404-421,587-594,926-954` uses existing versioned CSRF routes; full HTTP regression and real failed persistence path `WorkspaceBrowserIT.java:1257-1293` | PASS |
| RULE-4 | Draft MUST overlay accepted IDs; canceled preview MUST NOT mutate; conflict or failed save MUST disable solve | `app.js:455-495,555-576,590,933-942`; `WorkspaceBrowserIT.java:1107-1118,1152-1202,1280-1293` | PASS |
| RULE-6 | MUST display exact safe names/accepted availability and MUST escape school text | `app.js:366-387,543-576,895-909`; accepted model supplies assignments/availability; existing hostile-name browser regression in full suite | PASS |
| RULE-7 | MUST reuse native responsive context and catalog; inspector fixed and non-color | `app.js:350-376,455-495,608-623`; `messages.js`, `styles.css:62-76`; browser collapse/focus/narrow and full inspection regression | PASS |
| RULE-8 | MUST preserve complete local route/security boundary and withhold narrow mutation | No service/routes/security config changed; existing HTTP access tests in full suite; `WorkspaceBrowserIT.java:1227-1233` verifies no mutation controls | PASS |
| RULE-9 | MUST use isolated validated snapshot, no production/tracked data | Browser Testcontainers and verifier (`WorkspaceBrowserIT.java:1082-1085`), validated 60/100/100/1,000 scale fixture and exact fields (`:567-620`), scale Draft interaction (`:1477-1515`); no credential or production data added | PASS |
| RULE-10 | MUST exercise UC-1/UC-2 at real actor boundary and compare exact state | `WorkspaceBrowserIT.java:1081-1295`, focused and full browser runs; database exact-document and baseline assertions after stage, preview, mutation, failure, reload and discard | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| `timetable-ux-polish` UC-1 | Required accepted canvas and extension point 5a | Full `./mvnw -pl timetable-workspace -am verify` includes approved UC-1 browser journeys | PASS |
| `timetable-inspection-ux` UC-1–UC-3, existing workspace repair lifecycle | Shared accepted/focused renderers, filters, service state and browser shell | Full workspace suite: 26 unit, 71 integration including 23 browser; zero failed/errors/skipped | PASS |

## Findings

- D-1 (diagnostic, non-blocking): Target-scale pin feedback measured p95 430.5 ms against a 250 ms reference (`WorkspaceBrowserIT.java:1477-1515`). `spec.md:590-592` and `rules.md:82,290-291` expressly set no feature timing threshold; this is a performance observation, not a failed acceptance criterion.

## Walkthrough

UC-2-derived walkthrough: start teacher/room unavailability from Current, inspect direct effect and conflict on the unchanged accepted canvas, protect an affected lesson, cancel then confirm/undo bulk preview, navigate Current/Draft and focused return, collapse/reopen the inspector, reload Draft, check narrow read-only, and confirm discard. User reported manually testing the app and “lgtm”; taken as UC-2 UI walkthrough confirmation, not as evidence for the separate five-participant administrator feature gate (`spec.md:569-592`). Branch-specific durable failure and no-effect checks remain automated evidence.

## Status Update

UC-2 `READY_FOR_CONVERGENCE` → `APPROVED`; UC-3 is next eligible. UC-1 stays `APPROVED`; UC-4 and UC-5 remain `NOT_STARTED`.

## Response to execute

APPROVED WITH NOTES: target-scale pin-feedback p95 exceeded a diagnostic reference, not a contractual threshold.