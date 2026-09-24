# Use-Case Checkpoint: UC-1 - Scan and inspect the complete school in the compact workbench

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `1c26853df73b91d214d66e8fd9d6f1182a62d357`
- Submission commit: HEAD at convergence
- Relations verified: no internal Requires, Includes, or Extends. The existing accepted snapshot and focused inspection paths remain the production paths; the shared workspace browser regression passed. Extension 5a opens only unstaged setup and hands staging to UC-2.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| UC-1 main 1 | `WorkspaceBrowserIT.inspectsCompactWideCurrentWorkbenchInRealBrowser` opens the packaged app in Chrome against isolated PostgreSQL with a verified accepted scale school at 1600×900. | PASS |
| UC-1 main 2 | `WorkspaceBrowserIT.java:501–550` checks school, exact 1,000 IDs, five Week days, closed task area and concise initial inspector; `WorkspaceBrowserIT.java:394–414` checks accepted status, revision and Current-only mode. | PASS |
| UC-1 main 3 | `WorkspaceBrowserIT.java:553–563` changes Week/Day, search, subject and teacher investigation, and explicit filters; `narrowsAndFocusesAcceptedTimetableInRealBrowser` covers additional weekday/filter interactions. | PASS |
| UC-1 main 4 | `WorkspaceBrowserIT.java:563–573` asserts unchanged 1,000-lesson population for search/highlights, accepted teacher availability, exact 40- and 0-lesson narrowed counts, named criteria and direct clear with highlights retained. | PASS |
| UC-1 main 5 | `WorkspaceBrowserIT.java:574–586` selects lesson-960, focuses its teacher schedule, then returns; existing `opensFocusedAcceptedSchedulesInRealBrowser` covers class, teacher and room. | PASS |
| UC-1 main 6 | `WorkspaceBrowserIT.java:574–586` checks full authoritative class and lesson name, weekday/period/ID in accessible name, inspector detail, selected identity and retained search on return. | PASS |
| UC-1 main 7 | `WorkspaceBrowserIT.java:584–586` collapses and reopens inspector; `inspectsPolishedCurrentWorkbenchInRealBrowser` opens Utilities. | PASS |
| UC-1 main 8 | `WorkspaceBrowserIT.java:584–591` retains selected lesson and no write on setup; `WorkspaceBrowserIT.java:447–485` checks expanded canvas, restored detail, export-only Utilities and failed-export continuity. | PASS |
| UC-1 extension 1a | `WorkspaceBrowserIT.refusesUnmappableAcceptedMetadataInRealBrowser` imports a purported pair missing a required lesson name; packaged verification refuses it, browser shows no Current lesson/inspector, and empty durable state/version remain exact. | PASS |
| UC-1 extension 1b | `WorkspaceBrowserIT.java:633–647` checks 700 and 390 px read-only focused agenda, lifecycle text, one warning and no matrix, modes or repair controls. | PASS |
| UC-1 extension 2a | `WorkspaceBrowserIT.showsDeclaredEmptyAcceptedTimetableInRealBrowser` checks accepted identity, every declared class/time position, empty slots and zero invented lesson IDs. | PASS |
| UC-1 extension 3a | `WorkspaceBrowserIT.handlesInvalidAndBlockedInspectionPreferencesInRealBrowser` checks corrupt/wrong-version/wrong-weekday/other-school preference fallback, excluded selected lesson explanation, optional storage failure and unchanged document. | PASS |
| UC-1 extension 4a | `WorkspaceBrowserIT.java:566–573` checks zero represented lessons, all 60 declared class rows, explicit filtered status, named closed criteria and visible clear action. | PASS |
| UC-1 extension 5a | `WorkspaceBrowserIT.java:586–591` opens and closes protected repair setup without staging; mutating-fetch log stays empty and durable document/version remain exact. UC-2 owns subsequent staging. | PASS |
| UC-1 extension 6a | `WorkspaceBrowserIT.returnsFromEmptyAndNarrowFocusedSchedulesInRealBrowser` checks empty class, teacher and room recurring schedules and return without invented assignments. | PASS |
| UC-1 extension 8a | `WorkspaceBrowserIT.inspectsPolishedCurrentWorkbenchInRealBrowser` blocks the export GET and checks explicit no-bundle message, retained Current/filter/selection and exact stored document/version. | PASS |
| UC-1 G1 | `WorkspaceBrowserIT.java:511–550,601–646`: shell ≥1520 px at 1600; five Week days with inspector and no internal horizontal scroll; 1280 no page-level overflow, task non-overlay and one visible class row; 1279/701 inspector below canvas; 700/390 read-only. | PASS |
| UC-1 G2 | `WorkspaceBrowserIT.java:511–561,574–586`: one Compact density, subject-first/room-visible tiles, ordinary Week slot ≤36 px and Day cell ≤60 px, full names/details/accessibility, declared period order and empty positions. | PASS |
| UC-1 G3 | `WorkspaceBrowserIT.java:511–550,574–586`; `styles.css:115–119,319–329`: toolbar outside matrix scroll, sticky headings, focused schedule labelled as drill-down with return. | PASS |
| UC-1 G4 | `WorkspaceBrowserIT.java:543–550,584–591,647` records no mutating fetch and exact unchanged durable document/version; preference failure test checks nonblocking inspection. | PASS |
| UC-1 G5 | `app.js:294–303,487–526,904–1040`, `messages.js:214–253`, and browser selection/filter/narrow assertions: textual/structural status cues and native button, select, search and disclosure controls. Existing keyboard browser coverage remains green. | PASS |
| UC-1 G6 | Diff changes no server route, access rule or remote resource. `WorkspaceImportIT` local Host/Origin/CSRF/denied-route tests and full suite pass; rejected import exposes no Current data or write. | PASS |
| UC-1 G7 | `WorkspaceBrowserIT.java:490–647,2805–2844`: verifier-checked normative scale snapshot; exact lesson IDs, Week/Day, focus return, inspector/Filters/Utilities, responsive boundaries, and full stored document before/after. | PASS |
| UC-1 success postcondition | `WorkspaceBrowserIT.java:574–591`: exact accepted lesson in complete school Week, focus return with representable selection/investigation and restored inspector. | PASS |
| UC-1 minimal guarantee | Rejected import, invalid preference, zero match/empty schedule, failed export, unstaged setup and narrow checks above prove no invented lesson, prohibited UI disclosure or authoritative mutation. | PASS |
| UC-1 relations | No internal relations; extension 5a stops before the UC-2 durable staging action. Full regression covers approved accepted inspection and lifecycle surfaces sharing the packaged app. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:40–42,294–303,487–526` uses existing `GET /api/workspace`, accepted model and renderers; diff adds no route, model, asset source, dependency or persisted field. | PASS |
| RULE-2 | `inspection-state.js:1–104` is the single ephemeral owner for modes, area/disclosure defaults and school-scoped range preference; `WorkspaceBrowserIT.java:543–550,584–647` tests reset, retention, no mutating fetch and exact document/version. | PASS |
| RULE-4 | `styles.css:268–356`, `week-renderer.js:1–11`, `WorkspaceBrowserIT.java:511–561,601–646` measure width, fit, tile height, sticky headers, internal-only scroll and one Compact density. | PASS |
| RULE-5 | `app.js:523–529,741–760`, `styles.css:310–356`, `WorkspaceBrowserIT.java:587–646` keep selected context in a collapsible inspector, place setup in a full-width lower task area, and measure non-overlaying 1280 geometry and 1279/701 stacking. | PASS |
| RULE-6 | `app.js:487–526,904–939`, `WorkspaceBrowserIT.java:553–586` compare complete, narrowed and zero populations; search/highlights do not narrow, closed Filters still show names/count/clear, and focused return restores context. | PASS |
| RULE-7 | `app.js:562–568,1020–1053`, `week-renderer.js:1–11`, `WorkspaceBrowserIT.java:574–586,164–206,2805–2844` preserve authoritative names, weekday/order, stable ID and accepted availability; invalid accepted metadata is refused. Escaping remains in existing renderers. | PASS |
| RULE-12 | No route/security changes; `WorkspaceImportIT.java:459–534` exercises hostile Host/Origin, CSRF and denied paths; `WorkspaceBrowserIT.java:626–646` proves narrow read-only behavior and exact unchanged state. | PASS |
| RULE-13 | New text is in `messages.js:74–81,214–253`; `app.js:487–526,523–529` uses native/textual controls, Utilities for applicable export, and the separate initial journey remains green in full browser regression. | PASS |
| RULE-14 | `WorkspaceBrowserIT.java:490–647,2805–2844` builds an isolated normative school and verifies accepted definition/result through the packaged kernel, checks exact state and viewports, and the complete 35-test shared browser regression passes. Human walkthrough remains outstanding. | PASS |

## Validation

- Focused command: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#inspectsCompactWideCurrentWorkbenchInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — 26 unit + 1 browser test, 0 failed/errors/skipped.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify` — 26 unit + 83 integration, including 35 browser; 0 failed/errors/skipped.
- Working tree impact from tests: no tracked runtime or unrelated file changed. `git diff --check` passed. Reproducible test screenshots are under `timetable-workspace/target/workbench-layout/` and are not part of the submission.
- Runtime evidence: administrator opens a packaged local workbench backed by isolated PostgreSQL, inspects all 1,000 verified accepted identities across five weekdays, narrows and clears explicitly, focuses/returns, collapses/reopens inspector, opens/closes unstaged repair setup, and reads the 390 px agenda while the exact stored document/version remains unchanged. An independent read-only Playwright Chrome view confirmed the accepted 1600 px shell and exposed the period-prefix legibility issue corrected in this submission.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,inspection-state.js,messages.js,styles.css,week-renderer.js}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-workbench-layout/{status.md,checkpoints/UC-1.md}`.
- Approved UCs regression-tested: shared `timetable-workspace` UC-1–UC-8, accepted inspection UC-1–UC-3, and existing repair/planning browser paths through the full module suite. The separate `timetable-ux-polish` UC-5 administrator gate remains `PENDING_WALKTHROUGH` and is not represented as passed.

## Notes

No deviations. UC-2–UC-5 behavior is not submitted here. The earlier polish administrator gate and this feature's own human walkthrough remain separate from the green automated suite.

READY FOR CONVERGENCE: UC-1
