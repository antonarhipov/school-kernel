# Convergence: UC-1 - Inspect the accepted timetable in the persistent workbench

## Summary

- Submission: revised `spec/timetable-ux-polish/checkpoints/UC-1.md` at `69dad9dd94f14ba9cc1a9a7866b0adbea36e05cb` (prior submission `1e0fc8a`)
- Verdict: PENDING WALKTHROUGH
- Findings: 0 critical, 0 gaps, 0 protocol; prior G-1–G-3 resolved by revision and user-approved contract correction
- Suite: independently reran focused (26 unit, 4 browser; 0 failed/errors/skipped) and full `./mvnw -pl timetable-workspace -am verify` (26 unit, 67 integration including 19 browser; 0 failed/errors/skipped)
- Working tree impact from verification: generated `.output.txt` removed; no tracked runtime changes; pre-existing `.idea/encodings.xml` preserved. The feature's previously untracked `rules.md` was included in the user-approved revision commit.

## Protocol Gate

One UC, `UC-1`, is `READY_FOR_CONVERGENCE`, revised checkpoint and browser tests are committed together at `69dad9d`, inspection prerequisite UC-2 is `APPROVED`, and no other polish UC is in progress. Submission diff `ca4a3f5..69dad9d` contains the user-approved spec/rules correction, browser regression, checkpoint, and ledger; `.idea/encodings.xml` is excluded. All scenarios, extensions, guarantees, postconditions, applicable rules and regression are reported. No later UC behavior was added. This UI UC still requires human confirmation before approval.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator (real Chromium/CDP) | Open scale accepted timetable; Week/Day; subject investigation and cohort filter; lesson; focus and return | Complete Week/Day with exact selected lesson and retained filter | Focused real-browser test passed; `WorkspaceBrowserIT.java:341-381` asserts 60 classes, 1,000 lessons, 204 Monday assignments, exact lesson fields and return selection |
| Administrator | Inspector collapse/reopen; Utilities and failed export; narrow read-only | Canvas expands, selection and durable document remain; explicit export failure | Focused test passed; `WorkspaceBrowserIT.java:383-423` asserts request count, width, visible summary, restored lesson, network-blocked export, exact stored document, and narrow controls absent |
| Administrator | Empty accepted, invalid preference, empty focused entity | Prior inspection browser tests cover | Full browser regression passed; `WorkspaceBrowserIT.java:840-915` checks empty cells and preference fallback; `WorkspaceBrowserIT.java:807-837` checks empty focus |
| Administrator | Select Current from Draft and return; empty accepted | Claimed through revised browser journeys | Focused tests passed: `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser` retains exact selection, Week/filter, no workspace request or durable mutation; `showsDeclaredEmptyAcceptedTimetableInRealBrowser` asserts Current/accepted and no invented lesson |
| Administrator | Import purported accepted pair with missing lesson name (revised extension 1a) | Kernel rejects before acceptance | Browser import with original valid result and missing definition lesson name returned validation refusal, showed no Current/inspector and retained identical empty durable document; `refusesUnmappableAcceptedMetadataInRealBrowser` PASS |

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
| UC-1 trigger from other available mode | Draft→Current presentation transition | `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser`: Draft→Current→Draft→Current, accepted lesson, Week, filter and exact durable/request invariants; focused and full PASS | STRONG | yes |
| UC-1 1a (user-approved revision of 6a) | Invalid metadata refused before acceptance | `WorkspaceBrowserIT.refusesUnmappableAcceptedMetadataInRealBrowser`: real import rejected by packaged kernel, no Current/inspector, unchanged EMPTY document/version; focused and full PASS | STRONG | yes |
| UC-1 2a | Empty timetable | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser`: accepted identity, pressed Current, declared slots, zero invented lessons | STRONG | yes |
| UC-1 2b | Narrow read-only | `WorkspaceBrowserIT.java:413-423`, prior narrow regression | STRONG | yes |
| UC-1 3a | Invalid stored range or selection | `WorkspaceBrowserIT.java:865-915` probes invalid values and selection exclusion with exact stored-document comparison | STRONG | yes |
| UC-1 4a | Empty focused entity | `WorkspaceBrowserIT.java:807-837`, browser schedule/absence and state | STRONG | yes |
| UC-1 5a | Supported repair handoff | Existing `startRepairForm` in `app.js:332-344`; full browser repair journeys exercised | STRONG | yes |
| UC-1 8a | Export failure no bundle/state change | `WorkspaceBrowserIT.java:397-411,423` intercepts network and compares full document | STRONG | yes |
| UC-1 G1 | Sticky header/scroll boundaries | `styles.css` existing matrix/sticky definitions and prior browser inspection tests | STRONG | yes |
| UC-1 G2 | Utilities-only export, import hidden, no lifecycle action | `WorkspaceBrowserIT.java:397-403`, `app.js:53,282-300` | STRONG | yes |
| UC-1 G3 | Fixed width/no request/persistence | `styles.css:57-66`, `WorkspaceBrowserIT.java:383-395`, storage shape test `java:903-904` | STRONG | yes |
| UC-1 G4 | Navigation read-only | `inspection-state.js:3-38`, `WorkspaceBrowserIT.preparesProtectedRepairDraftInRealBrowser` proves Draft→Current round trip without request or mutation; previous preference tests PASS | STRONG | yes |
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
| RULE-2 | MUST derive modes in one owner, retain context, reset on reload; MUST NOT mutate via presentation | `inspection-state.js:3-38` owns ephemeral mode; focused real-browser Draft→Current→Draft→Current compares request count and exact durable document | PASS |
| RULE-6 | Invalid accepted pairs MUST be refused; received snapshot fallback MUST show ID/unavailable-name cue | `WorkspaceBrowserIT.refusesUnmappableAcceptedMetadataInRealBrowser` real verifier rejection; `app.js:857` defensive escaped ID/cue fallback in presentation | PASS |
| RULE-7 | MUST reuse accepted renderers, fixed inspector and safe Utilities | `app.js:424-460`, `styles.css:48-66`, real-browser collapse/export checks | PASS |
| RULE-8 | MUST NOT add route/relax protection; export accepted-only | No route/security changes; `app.js:286` only exposes export in accepted state; HTTP regression green | PASS |
| RULE-9 | MUST use exact isolated normative validation data | `WorkspaceBrowserIT` scale fixture and isolated Testcontainers; invalid-pair branch begins from valid reference and refuses deliberately missing lesson name; full suite PASS | PASS |
| RULE-10 | MUST verify actor boundary and state safety | Real Chromium/CDP plus exact stored document for scale, empty Current, mode switch, failed export, invalid import; full suite PASS | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| `timetable-inspection-ux` UC-1, UC-2, UC-3 | Shared accepted inspection and focus renderer | Entire workspace suite, 18 browser tests, green | PASS |
| Existing repair lifecycle | Shared app shell and accepted snapshot | Full workspace suite, including repair browser journeys, green | PASS |

## Findings

None. Prior G-1 is closed by Draft→Current browser transition; G-3 by accepted empty Current identity; G-2 by the user's explicit contract decision and real browser verifier-refusal evidence. The defensive renderer remains intact.

## Walkthrough

Pending user confirmation. Open a verified accepted timetable on a supported desktop. Check school, lifecycle state, accepted revision, Current and Week; change to Day and back, inspect a subject/teacher or filter, select a lesson, and visit and return from a class/teacher/room schedule. Collapse and reopen the inspector, checking that selection stays and the timetable gains width; open Utilities and find export there, with import and lifecycle actions in their proper contexts. On narrow viewport, confirm the read-only focused schedule and absence of desktop mutation controls. An empty accepted timetable should still identify Current and show declared empty slots. An invalid accepted-pair import must be refused before displaying Current; this negative path has automated actor-boundary proof. Please report whether the visual and navigation checks pass or fail; no result has been supplied yet.

## Status Update

UC-1 `READY_FOR_CONVERGENCE` → `PENDING_WALKTHROUGH`; no next UC eligible until user confirms and convergence approves.

## Response to execute

PENDING WALKTHROUGH: confirm UC-1 accepted desktop/narrow workbench layout, inspection navigation, inspector collapse/reopen, and Utilities placement before approval.