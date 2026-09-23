# Convergence: UC-1 - Inspect the accepted timetable in the persistent workbench

## Summary

- Submission: `spec/timetable-ux-polish/checkpoints/UC-1.md` at `1e0fc8a0cc0247827ee50c624130d1e9715f40ac`
- Verdict: REJECT
- Findings: 0 critical, 3 gaps, 0 protocol
- Suite: independently ran focused (26 unit, 1 browser; 0 failed/errors/skipped) and full `./mvnw -pl timetable-workspace -am verify` (66 tests, 0 failed/errors/skipped)
- Working tree impact from verification: generated `.output.txt` removed; pre-existing `.idea/encodings.xml` and untracked `spec/timetable-ux-polish/rules.md` preserved

## Protocol Gate

One UC, `UC-1`, is `READY_FOR_CONVERGENCE`, checkpoint and implementation are committed together at `1e0fc8a`, inspection prerequisite UC-2 is `APPROVED`, and no other polish UC is in progress. Base diff `0b27b9d..1e0fc8a` consists of accepted presentation, browser tests, checkpoint, and ledger only. The checkpoint lists scenario, extensions, guarantees, rules, tests, changed files, and regression; evidence gaps below remain subject to audit, not a protocol blocker.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator (real Chromium/CDP) | Open scale accepted timetable; Week/Day; subject investigation and cohort filter; lesson; focus and return | Complete Week/Day with exact selected lesson and retained filter | Focused real-browser test passed; `WorkspaceBrowserIT.java:341-381` asserts 60 classes, 1,000 lessons, 204 Monday assignments, exact lesson fields and return selection |
| Administrator | Inspector collapse/reopen; Utilities and failed export; narrow read-only | Canvas expands, selection and durable document remain; explicit export failure | Focused test passed; `WorkspaceBrowserIT.java:383-423` asserts request count, width, visible summary, restored lesson, network-blocked export, exact stored document, and narrow controls absent |
| Administrator | Empty accepted, invalid preference, empty focused entity | Prior inspection browser tests cover | Full browser regression passed; `WorkspaceBrowserIT.java:840-915` checks empty cells and preference fallback; `WorkspaceBrowserIT.java:807-837` checks empty focus |
| Administrator | Current chosen from another mode; missing lesson display metadata | Claimed via navigation/fallback code | No browser action/assertion drives either branch (`WorkspaceBrowserIT.java:324-424` and search across tests); gaps G-1 and G-2 |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| UC-1 main 1 | Real-browser accepted opening | `WorkspaceBrowserIT.java:341-349`, focused PASS | STRONG | yes |
| UC-1 main 2 | School, state, revision, Current, Week | `WorkspaceBrowserIT.java:342-350`, exact header/scale checks | STRONG | yes |
| UC-1 main 3 | Range, investigation, search, filters, focus | `WorkspaceBrowserIT.java:352-381`; prior inspection browser search/teacher/room tests in full suite | STRONG | yes |
| UC-1 main 4 | Honest population/return path | `WorkspaceBrowserIT.java:353-381`, prior inspection focused regression | STRONG | yes |
| UC-1 main 5 | Select accepted lesson | `WorkspaceBrowserIT.java:364-368` | STRONG | yes |
| UC-1 main 6 | Exact contextual inspector without displacement | `WorkspaceBrowserIT.java:364-374`, `app.js:456-460` | STRONG | yes |
| UC-1 main 7 | Collapse/reopen, Utilities | `WorkspaceBrowserIT.java:383-403` | STRONG | yes |
| UC-1 main 8 | Preserved selection, expanded canvas, export segregation | `WorkspaceBrowserIT.java:383-411` | STRONG | yes |
| UC-1 trigger from other available mode | Navigation code exists | `app.js:98-141` and `renderModeNavigation`, but no browser assertion selects Current from Draft, Solving, or Proposal | ABSENT | no (G-1) |
| UC-1 2a | Empty timetable | `WorkspaceBrowserIT.java:840-862` checks class/period/empty, but not Current/accepted identity for this branch | WEAK | no (G-3) |
| UC-1 2b | Narrow read-only | `WorkspaceBrowserIT.java:413-423`, prior narrow regression | STRONG | yes |
| UC-1 3a | Invalid stored range or selection | `WorkspaceBrowserIT.java:865-915` probes invalid values and selection exclusion with exact stored-document comparison | STRONG | yes |
| UC-1 4a | Empty focused entity | `WorkspaceBrowserIT.java:807-837`, browser schedule/absence and state | STRONG | yes |
| UC-1 5a | Supported repair handoff | Existing `startRepairForm` in `app.js:332-344`; full browser repair journeys exercised | STRONG | yes |
| UC-1 6a | Missing display metadata, ID and unavailable cue | `app.js:857` fallback exists; no accepted missing-metadata browser fixture/assertion | ABSENT | no (G-2) |
| UC-1 8a | Export failure no bundle/state change | `WorkspaceBrowserIT.java:397-411,423` intercepts network and compares full document | STRONG | yes |
| UC-1 G1 | Sticky header/scroll boundaries | `styles.css` existing matrix/sticky definitions and prior browser inspection tests | STRONG | yes |
| UC-1 G2 | Utilities-only export, import hidden, no lifecycle action | `WorkspaceBrowserIT.java:397-403`, `app.js:53,282-300` | STRONG | yes |
| UC-1 G3 | Fixed width/no request/persistence | `styles.css:57-66`, `WorkspaceBrowserIT.java:383-395`, storage shape test `java:903-904` | STRONG | yes |
| UC-1 G4 | Navigation read-only | `inspection-state.js:3-38`, `WorkspaceBrowserIT.java:411,423`, previous preference tests; other-mode transition missing | WEAK | no (G-1) |
| UC-1 G5 | Non-color status and native controls | `app.js:424-460`, `messages.js`, `WorkspaceBrowserIT.java:291-298,383-403` | STRONG | yes |
| UC-1 G6 | No new access surface | Commit diff has no routes, remote resources, authentication or network dependencies; full HTTP regressions green | STRONG | yes |
| UC-1 G7 | Normative scale browser journey and durable state | `WorkspaceBrowserIT.java:324-423`, scale fixture generated from validated reference; full browser suite green | STRONG | yes |
| UC-1 success postcondition | Exact inspection/focused return | `WorkspaceBrowserIT.java:364-395`, suite PASS | STRONG | yes |
| UC-1 minimal guarantee | Failed local preference/export leave durable state | `WorkspaceBrowserIT.java:411,423,915` | STRONG | yes |
| UC-1 relations | No internal relations; inspection prerequisite approved | `spec.md:179-182`, inspection status/convergence; full suite PASS | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | MUST extend packaged workbench; MUST NOT fork model/add dependencies | `app.js` imports existing accepted model and renderers; commit has no backend/route/dependency changes | PASS |
| RULE-2 | MUST derive modes in one owner, retain context, reset on reload; MUST NOT mutate via presentation | `inspection-state.js:3-38` owns ephemeral mode; `app.js` switches modes; missing other-mode browser proof | GAP G-1 |
| RULE-6 | Missing names MUST show stable ID and unavailable-name cue | `app.js:857` fallback; no browser assertion for missing accepted metadata | GAP G-2 |
| RULE-7 | MUST reuse accepted renderers, fixed inspector and safe Utilities | `app.js:424-460`, `styles.css:48-66`, real-browser collapse/export checks | PASS |
| RULE-8 | MUST NOT add route/relax protection; export accepted-only | No route/security changes; `app.js:286` only exposes export in accepted state; HTTP regression green | PASS |
| RULE-9 | MUST use exact isolated normative validation data | `WorkspaceBrowserIT.java:324-423,1357-1430` generated scale fixture and isolated Testcontainers; full suite PASS | PASS |
| RULE-10 | MUST verify actor boundary and state safety | Real Chromium/CDP plus exact stored document; uncovered 2a and 6a branches | GAP G-2, G-3 |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| `timetable-inspection-ux` UC-1, UC-2, UC-3 | Shared accepted inspection and focus renderer | Entire workspace suite, 18 browser tests, green | PASS |
| Existing repair lifecycle | Shared app shell and accepted snapshot | Full workspace suite, including repair browser journeys, green | PASS |

## Findings

- **G-1 GAP — Other-mode Current trigger not proven.** UC-1 trigger: “selects Current from another available mode”; RULE-2: “MUST retain representable range, investigation, selection, focus, and scroll on mode changes.” `app.js:98-141` introduces the transition, but the only new mode assertion in `WorkspaceBrowserIT.java:347-348` checks accepted state's one Current button. Drive a real Draft→Current (and return where relevant) browser transition with exact accepted canvas/selection, no mutation/request, and retained context assertions.
- **G-2 GAP — Missing metadata fallback not proven at actor boundary.** UC-1 extension 6a: “If selected display metadata cannot be mapped, the inspector shows the stable identity with an unavailable-name cue and no guessed label”; RULE-6 requires that cue. `app.js:857` has a fallback, but `WorkspaceBrowserIT.java:364-374` selects fully mapped data only. Add a production-conformant accepted browser case with unavailable metadata and assert exact ID/cue, no guessed name, and unchanged workspace; if verifier excludes such data, establish the genuine product/spec blocker instead.
- **G-3 GAP — Empty Current identity omitted.** UC-1 extension 2a: “If the accepted timetable has no assignments ... identifies Current as accepted.” `WorkspaceBrowserIT.java:840-862` asserts class, period, and emptiness but not mode or accepted identity. Assert the Current/accepted indicator in that empty-snapshot journey, alongside absence of invented lesson.

## Walkthrough

Deferred until automated evidence is strong: open a verified accepted timetable on desktop; check school, state, revision, Week/Day and whole-school labels; inspect a lesson and focus/return; collapse/reopen the inspector and open Utilities; confirm export and absence of destructive actions; repeat on narrow viewport. User confirmation is required before UI approval.

## Status Update

UC-1 `READY_FOR_CONVERGENCE` → `NEEDS_REVISION`; no next UC eligible. Preserve submitted implementation evidence.

## Response to execute

REVISE UC-1: G-1 other-mode Current journey, G-2 missing-metadata inspector proof, G-3 empty Current identity proof.