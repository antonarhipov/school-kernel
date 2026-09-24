# Convergence: UC-1 - Scan and inspect the complete school in the compact workbench

## Summary

- Submission: `spec/timetable-workbench-layout/checkpoints/UC-1.md` at `c407a02d2fd2d176e99e6c45b6d5101e11206542`
- Verdict: APPROVE
- Findings: 0 critical, 0 gaps, 0 protocol; 1 cosmetic evidence-wording note
- Suite: independent focused run, 26 unit + 1 browser; independent full run, 26 unit + 83 integration including 35 browser; 0 failed, 0 errors, 0 skipped in both
- Working tree impact from verification: none tracked; `git status --short` was clean before and after tests; browser screenshots remain under ignored `target/`

## Protocol Gate

One target, UC-1, was `READY_FOR_CONVERGENCE` at the start. The checkpoint, implementation and tests are together in immutable commit `c407a02`; the base is `1c26853`. UC-1 has no internal dependencies and UC-2–UC-5 are `NOT_STARTED`, so no other UC is in progress. The checkpoint reports main steps, eight extensions, seven guarantees, both postconditions, all nine applicable rules, relations, commands, changed files and shared regressions. The eight-file diff changes only the packaged presentation, browser-state owner, catalog, one renderer, browser tests, and this feature's ledger/checkpoint; it adds no backend route, persistence field, dependency, or later-UC workflow. The separate `timetable-ux-polish` UC-5 administrator gate is still recorded `PENDING_WALKTHROUGH` and is not treated as this feature's approval.

## Runtime Reproduction

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Administrator, real Chrome with isolated PostgreSQL | Open verified accepted scale school, Week/Day, search/highlights, explicit/zero filters, lesson and focused return | Complete 1,000-lesson population, exact narrowing, selection and unchanged durable state | Independently reran `inspectsCompactWideCurrentWorkbenchInRealBrowser`; 1/1 PASS. `WorkspaceBrowserIT.java:490–583` checks every ID, five days, 36/60 px bounds, count changes 1,000→40→0→1,000, full selected identity and focused return. |
| Administrator | Inspector, unstaged setup, responsive layouts | Context retained, setup no write, desktop/narrow boundaries honored | Focused test passed. Independent read-only Playwright Chrome inspection of the isolated test server: at 1600×900, task 270 px, matrix 428 px with a 305 px complete row, stage button reachable after task scroll; at 1280×800, task 240 px, matrix 328 px with a 305 px row, stage reachable, inspector beside canvas and document scroll width 1280 px. No form was submitted. |
| Administrator | Invalid import, empty accepted, invalid preference, empty focus, failed export | Existing production journeys cover negative branches | Full browser suite passed `refusesUnmappableAcceptedMetadataInRealBrowser`, `showsDeclaredEmptyAcceptedTimetableInRealBrowser`, `handlesInvalidAndBlockedInspectionPreferencesInRealBrowser`, `returnsFromEmptyAndNarrowFocusedSchedulesInRealBrowser`, and `inspectsPolishedCurrentWorkbenchInRealBrowser`; all compare unchanged isolated storage where applicable. |
| Unauthorized local request | Host/Origin/CSRF and denied paths | Existing local-only access unchanged | Full `WorkspaceImportIT` passed hostile Host, wrong loopback port, hostile/missing Origin, CSRF and denied-route checks; no route/security code changed in the submission. |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1 | Opens operational desktop | `WorkspaceBrowserIT.java:490–507`, focused PASS | STRONG | yes |
| Main 2 | Compact accepted identity and complete canvas | `java:507–542` plus `java:394–414`: all 1,000 IDs, five days, school/revision/Current, closed task/disclosures | STRONG | yes |
| Main 3 | Range, weekday, search, highlights, Filters | `java:553–566,928–984,1102–1150`, full browser PASS | STRONG | yes |
| Main 4 | Population/narrowing and accepted availability | `java:561–574,755–906`: exact represented sets/counts, non-narrowing investigation and accepted teacher ribbon | STRONG | yes |
| Main 5 | Select or focus and return | `java:576–583,988–1070`: selected lesson, class/teacher/room focus, return | STRONG | yes |
| Main 6 | Exact accepted inspector detail | `java:576–583,429–439`, `app.js:562–568,1027–1031`: authoritative values, weekday/period/ID accessible name | STRONG | yes |
| Main 7 | Collapse/reopen inspector, Utilities | `java:584–586,447–461`, focused/full browser PASS | STRONG | yes |
| Main 8 | Retained selection, expanded canvas, applicable utility | `java:447–485,584–591`: wider canvas, restored detail, export inside Utilities, no lifecycle actions there | STRONG | yes |
| Extension 1a | Invalid purported accepted pair refused | `java:164–206`: missing lesson name rejected through browser import, no Current/inspector or write; `WorkspaceImportIT.java:382–447` includes unknown-room assignment | STRONG | yes |
| Extension 1b | 700 px and narrower read-only | `java:633–647`: true lifecycle, one warning, no repair/matrix/mode controls at 700/390 | STRONG | yes |
| Extension 2a | Zero-assignment accepted school retains structure | `java:1074–1098`: declared classes, periods, empty cells, zero lessons | STRONG | yes |
| Extension 3a | Invalid preference/identity falls back safely | `java:1102–1150`: invalid preference variants, unavailable selected Day, explanation, exact state | STRONG | yes |
| Extension 4a | Zero filter matches | `java:566–574`: 0 represented, 60 class rows, named criteria, direct clear, filtered not complete | STRONG | yes |
| Extension 5a | Supported repair handoff | `java:584–591`, `app.js:340–363`: setup opens before staging, closes without fetch mutation or storage change; UC-2 owns staging | STRONG | yes |
| Extension 6a | Empty focused entity | `java:1032–1070` exercises empty room with exact entity/no invented lesson; `java:988–1030` exercises class/teacher titles; `focused-renderer.js:1–12` uses one shared empty branch for all three entity types against the scale fixture's empty class/teacher/room population | STRONG | yes |
| Extension 8a | Failed accepted export | `java:447–485`: blocked GET, no bundle message, Current/filter/selection and exact storage retained | STRONG | yes |
| G1 | Desktop/edge layout bounds | `java:511–542,593–647`; independent Chrome 1600 task 270/900 and row 428≥305, 1280 task 240/800 and row 328≥305 with stage reachable and no page-level horizontal scroll | STRONG | yes |
| G2 | One compact density with exact facts | `java:530–558,576–578`; `styles.css:319–330`; `week-renderer.js:3–11`: room visible, period order/name, ≤36 Week and ≤60 Day ordinary cells, full accessible detail | STRONG | yes |
| G3 | Matrix scroll boundaries and focus drill-down | `java:527,579–583`; `styles.css:115–119`: toolbar outside wrap, sticky headings, focus title and return | STRONG | yes |
| G4 | Presentation-only and optional preference safe | `java:543–550,584–591,647,1102–1150`; no mutating fetch and exact unchanged document/version | STRONG | yes |
| G5 | Text/native controls and English labels | `app.js:294–303,487–526,904–1040`, `messages.js:214–253`; browser checks state, selection, count, labels and native controls | STRONG | yes |
| G6 | Local-only access, no disclosure/mutation | Eight-file diff has no route or security change; `WorkspaceImportIT.java:459–534` and full route regression passed | STRONG | yes |
| G7 | Normative real-browser and durable evidence | `java:490–647,2805–2844`: packaged verifier checks accepted pair, 60 classes, 100 teachers/rooms, 60 periods, 1,000 assignments; browser viewport/focus/filter/disclosure and exact storage checks | STRONG | yes |
| Success postcondition | Exact accepted lesson in complete context with return | `java:576–586` and focused PASS | STRONG | yes |
| Minimal guarantee | Negative/empty/narrow/utility branches invent or mutate nothing | `java:164–206,566–574,633–647,1074–1150,447–485`; full PASS | STRONG | yes |
| Relations | None internal; handoff to UC-2 at 5a | `spec.md:185–204,232–239`; only setup opens before UC-2 staging; shared approved journeys green | STRONG | yes |

## Rule Conformance

| Rule | Constraint | Evidence | Result |
|---|---|---|---|
| RULE-1 | “MUST extend the existing packaged workspace” and “MUST NOT fork accepted assignments” | `app.js:40–42,294–303,487–526`; commit has no route/model/dependency change; exact IDs from one snapshot | PASS |
| RULE-2 | “One browser presentation-state owner MUST derive available/default modes” and presentation actions “MUST NOT issue durable mutation requests” | `inspection-state.js:1–104`; `WorkspaceBrowserIT.java:543–550,584–647,1102–1150`; exact version/document unchanged | PASS |
| RULE-4 | Shell “MUST use at least 95 percent” at 1600, five days “MUST fit”, slots ≤36/60, one Compact density | `styles.css:268–356`; `WorkspaceBrowserIT.java:511–558,593–618`; focused run PASS | PASS |
| RULE-5 | Inspector “MUST remain beside” at ≥1280 and below at 701–1279; task “MUST” be beneath/≤35% with reachable controls | `app.js:523–529,741–760`, `styles.css:310–356`, `java:584–647`; independent Chrome geometry and stage reachability at 1600/1280 | PASS |
| RULE-6 | Canvas “MUST represent every declared” position; filters “MUST intersect”; counts/clear “MUST remain visible” | `app.js:487–526,904–939`; `java:507–537,561–574,928–984` exact complete, narrowed and zero cases | PASS |
| RULE-7 | Views “MUST use authoritative school-controlled display names” and full lesson details; malformed input “MUST be refused” | `app.js:562–568,1027–1053`, `week-renderer.js:3–11`; `java:164–206,574–578,2805–2844` | PASS |
| RULE-12 | Route/access table “MUST remain unchanged”; narrow view “MUST” be read-only; denied requests no disclosure/write | No server/security diff; `WorkspaceImportIT.java:459–534`; `java:633–647`; full suite PASS | PASS |
| RULE-13 | Every new string “MUST use” catalog; non-color cues and native controls; Utilities contain export | `messages.js:74–81,214–253`, `app.js:294–303,487–526`; browser Utilities/export and selection/filter assertions | PASS |
| RULE-14 | Isolated normative actor-boundary evidence and complete shared browser regression “MUST” be met; automation is not administrator approval | `java:490–647,2805–2844`; 35/35 browser tests, 83/83 integration, exact stored document checks; walkthrough still pending | PASS |

## Related-UC Regression

| Use case | Relationship/shared surface | Evidence | Result |
|---|---|---|---|
| `timetable-workspace` UC-1–UC-8 | Shared packaged app, import, security, Draft/run/Proposal/acceptance | Independent full 83-test integration suite, including 35 browser; 0 failures/errors/skips | PASS |
| `timetable-inspection-ux` UC-1–UC-3 | Accepted Day/Week, investigation and focused renderer | Shared browser tests in the full suite, including exact ID/focus cases | PASS |
| `timetable-ux-polish` UC-1–UC-4 and UC-5 automation | Shared operational mode/inspection/lifecycle UI | Shared browser regression green; its separate UC-5 human gate remains pending | PASS automation only |

## Findings

- K-1 COSMETIC — The checkpoint's extension 6a row says `returnsFromEmptyAndNarrowFocusedSchedulesInRealBrowser` checks empty class, teacher and room schedules; that test directly drives empty room. Populated class/teacher browser paths plus the single shared empty branch in `focused-renderer.js:1–12` establish the same behavior for those entities, so no observable gap remains. Future checkpoint wording should distinguish direct browser assertion from composed source-and-browser evidence.

## Walkthrough

Automated gate passed. The administrator confirmed “UC-1 walkthrough passed” on 2026-09-24. The confirmed UC-1 script covered (1) identifying school, accepted revision, Current and five-day Week/Day count; (2) search/highlight without narrowing, Filters, zero matches and direct clear; (3) exact lesson inspection, class/teacher/room focus and return, including an empty entity; (4) inspector, Utilities and unstaged repair setup while Current remains accepted; and (5) desktop coexistence and narrow read-only agenda. This confirmation closes only the UC-1 walkthrough; the separate five-participant, three-school six-task feature gate belongs to UC-5.

## Status Update

UC-1 `PENDING_WALKTHROUGH` → `APPROVED` following explicit administrator confirmation; UC-2 is next eligible.

## Response to execute

APPROVED
