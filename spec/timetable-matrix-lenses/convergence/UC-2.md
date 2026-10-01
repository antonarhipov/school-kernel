# Convergence: UC-2 - Change or clear the lens

## Summary

- Submission: `checkpoints/UC-2.md` and implementation at `fee1bb5` (revision 1; first submission `013c12a` was rejected in `f54ec03`). The user's D-1 scroll decision is explicit in `spec.md` at `2cf808f`.
- Verdict: APPROVE WITH NOTES
- Findings: 0 critical, 0 gap, 0 protocol, 0 drift. The earlier G-1 and G-2 are resolved. Notes concern the unchanged full-suite baseline and the user-approved UC-3 sequencing exception.
- Suite: focused `MatrixLensBrowserIT` 18 run / 0 failures / 0 errors / 0 skipped; full `./mvnw -q -pl timetable-workspace -am verify` workspace unit 26 / 0 / 0 / 0 and workspace integration 124 / 2 failures / 9 errors / 0 skipped. The 11 failing integration IDs exactly match the baseline listed in the first UC-2 report; no new IDs failed.
- Working tree impact from verification: none. Before testing, `README.md`, `pom.xml`, and `spec/README.md` had unrelated user edits. They still have those edits. The only additional change made during this verification was the separately committed UC-2 specification clarification.

The browser test path is `timetable-workspace/src/test/java/org/schoolkernel/workspace/MatrixLensBrowserIT.java`. The production paths below are under `timetable-workspace/src/main/resources/static/workspace/`.

## Protocol Gate

1. UC-2 is the single target and was `READY_FOR_CONVERGENCE` before this audit. Pass.
2. Revision 1 has an immutable implementation and checkpoint at `fee1bb5`. Pass.
3. UC-1, the only `Requires` dependency, is `APPROVED` at `002cc2f`. Pass.
4. UC-3 is also `READY_FOR_CONVERGENCE`, an explicit user-approved sequencing exception documented in `checkpoints/UC-3.md`. Its implementation was committed later at `eb7680c`; no UC-3 edits were made during this audit. The user exception governs the usual one-at-a-time gate. Pass with note.
5. The revised checkpoint contains evidence for every UC-2 scenario, guarantee, postcondition, relation, applicable rule, test run, changed file, and related regression. Pass.
6. `fee1bb5` changes only UC-2 test, scroll behavior, checkpoint, and status files. The later UC-3 diff was inspected, and all 18 lens tests passed on the current descendant checkout. Unrelated working-tree edits were excluded. Pass.

## Runtime Reproduction

The administrator journeys ran through headless Chrome against the workspace HTTP server and PostgreSQL Testcontainers, with Docker access. Both the focused run and the full reactor executed the real browser class.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator | Remove a lens criterion and choose All teachers | `clearsALensBackToTheRecordedScroll:345` | Class rows, complete status, and the exact recorded scroll return after teacher-to-teacher-to-room changes; test green. |
| Administrator | Clear while Day, search, investigations, period, and selection are active | `clearsALensRetainingTheInvestigativeContext:384` | Remaining period filter yields 17 visible class rows; all context values and selected inspector remain; narrowed status; test green. |
| Administrator | Reset and Clear filters from teacher and subject Show only matches modes | `returnsShowOnlyMatchesModesToHighlightsOnReset:492` | Both modes end unchecked, both investigated IDs remain, 40 teacher and 61 subject highlight tiles return, status and counts become complete, saved scroll returns; Reset clears search and Clear filters keeps it. Test green. |
| Administrator | Clear a lens with a represented selection away from the saved position | `prefersTheSelectionOverTheRecordedScroll:525` | The entry position is over 2500 px and lesson-0 is initially out of view; after clearing, it is selected and visible with scrollTop below 1000 px. A restore-first implementation cannot satisfy this assertion. Test green. |
| Administrator | Change Week to Day in a lens, then clear; change weekday within Day | `restoresTheRecordedScrollOnlyInItsRange:548` | Week-to-Day class rows start at 0; a Day weekday change retains the recorded Day offset. Test green. |
| Administrator | Change entity or type with and without a represented selection | `changesTheLensEntityOrType:419` | New single lens row group is named correctly; represented selection stays, unrepresented selection closes with the stated notice. Test green. |
| Administrator | Presentation actions do not mutate | UC-2 tests above | Mutation logs are empty and stored workspace documents compare equal in each committed browser journey. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Trigger and precondition | UC-1 lens active | Browser tests enter through Filters, inspector Show week, and teacher Show only matches; the UC-1 lens tests are green | STRONG | yes |
| Main 1: remove criterion or choose All | `:345`, `:384` | Real criterion click and native select change clear the active lens | STRONG | yes |
| Main 2: class rows under remaining filters | `:384` | 60 class groups return, with 17 visible under period-0 and 17 represented lessons | STRONG | yes |
| Main 3 and state rule 1: retain context | `:384` | Day, Monday, search, investigations, period, selection, and inspector asserted by value; class-filter retention is covered by `AcceptedInspectionBrowserIT` | STRONG | yes |
| Main 4 and state rule 2: saved position or selected tile | `:345`, `:525`, `:548` | Exact saved offset, discriminating selected-tile precedence, and the approved range scope all pass | STRONG | yes |
| Main 5: complete or narrowed status | `:345`, `:384`, `:492` | Complete and period-only narrowed labels and active criteria asserted by value | STRONG | yes |
| Extension 1a: other entity of same type | `:345`, `:419` | Single row-group entity changes and selection clears when outside the new lens; the first audit independently checked the new entity's tile population | STRONG | yes |
| Extension 1b: other lens type | `:345`, `:419` | Other filter clears in the same transition; represented selection stays and unrepresented selection closes | STRONG | yes |
| Extension 1c and state rule 3: reset | `:455`, `:492` | Reset, Clear filters, and empty-lens reset cover narrowing, investigation modes, search distinction, highlights, and scroll | STRONG | yes |
| Extension 3a: unrepresented selection | `:419` | Inspector closes, selected tile clears, and the notice names the new lens | STRONG | yes |
| G1: context retention | `:384`, `:492` | Every applicable presentation value checked after the transition | STRONG | yes |
| G2: no surface change | `:345` | Matrix remains; no focused surface or return control | STRONG | yes |
| Success postcondition | New lens or class rows with context | Main and extension journeys above | STRONG | yes |
| Minimal guarantee | Self-consistent presentation; authoritative data unchanged | Criteria, rows, selection, notices, empty mutation logs, and equal stored documents in the browser journeys | STRONG | yes |
| Requires UC-1 | Uses approved lens | UC-2 enters via UC-1 paths, and the seven UC-1 browser tests pass in the same class | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | Lens is the existing, mutually exclusive teacher/room filter state | `inspection-state.js:30-37,92-97`; UC-2 reset and switching tests | Pass |
| RULE-2 | Persist only `{version, range, weekdayId}`; never persist lens or entry scroll | `inspection-state.js:143-149`; UC-1 exact preference and reload assertions remain green | Pass |
| RULE-3 | Class rows and lens rows use the same matrix renderers | `app.js:854` renders through the existing Week/Day matrix path; no UC-2 renderer added | Pass |
| RULE-4 | Visible lens assignments follow the existing represented predicate | `app.js:984-993` clears selection through the existing representation check; UC-1 exact ID sets and UC-2 switching tests pass | Pass |
| RULE-6 | Presentation actions issue no non-GET request and leave the stored document equal | Empty mutation logs and byte-equal stored documents in all UC-2 browser tests | Pass |
| RULE-7 | New visible text comes from `messages.js` and notices use `#inspection-notice` | `app.js:990-992`, `messages.js:322`; extension 3a asserts the notice text | Pass |
| RULE-10 | No model rebuild or refetch on lens changes | `app.js:984-1007` renders once and settles scroll; `ScaleTimingBrowserIT` 3/3 | Pass |

## Related-UC Regression

| Use case | Shared surface | Evidence | Result |
|---|---|---|---|
| UC-1 (approved) | Lens entry, row group, state, renderer | Seven UC-1 tests in `MatrixLensBrowserIT`; whole class 18/18 in focused and full runs | Pass |
| `timetable-inspection-ux` (approved) | Reset and investigation modes | `AcceptedInspectionBrowserIT` 11/11 | Pass |
| UC-3 (submitted, pending verdict) | Same lens code after `fee1bb5` | Four UC-3 tests in `MatrixLensBrowserIT` pass | No break observed |
| Other workspace UCs | Workbench routes and browser layout | Full reactor's 11 failures are exactly the pre-existing IDs from the first UC-2 report; no new failing IDs | No UC-2 regression observed |

## Findings

No blocking findings. G-1 is closed by the Reset/Clear filters browser journey at `MatrixLensBrowserIT:492`. G-2 is closed by the separated selection and saved-position journey at `:525`. D-1 was decided by the user and written into the specification at `2cf808f`; `:548` proves the chosen behavior.

Notes: the full reactor remains red in 11 pre-existing repair/proposal browser cases (2 failures, 9 errors). Their IDs are unchanged from the recorded baseline. UC-3 was submitted before UC-2's revised verdict under the user's explicit exception; it still requires its own convergence.

## Walkthrough

The UC-2 human walkthrough was reported as good by the user on 2026-10-01, as recorded in `checkpoints/UC-3.md` under its process deviation. The relevant script is: enter a teacher or room lens; change its entity and type; clear through the criterion and All option; reset from Show only matches; confirm retained investigations, selected-lesson behavior, scroll, and complete/narrowed status. The independent browser run above verifies these paths after that report.

## Status Update

UC-2: `READY_FOR_CONVERGENCE` -> `APPROVED`. UC-3 remains `READY_FOR_CONVERGENCE` and awaits its own verdict. UC-4 stays `NOT_STARTED`; no next implementation is eligible until UC-3 is approved.

## Response to execute

APPROVED WITH NOTES: UC-2 G-1 and G-2 are resolved; the full suite has only its unchanged 11 baseline failures, and UC-3 still awaits convergence.
