# Use-Case Status: timetable-workbench-layout

## Current

- Use case: UC-1
- Status: READY_FOR_CONVERGENCE
- Next eligible: none until UC-1 is approved

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | READY_FOR_CONVERGENCE | none | HEAD at convergence | - |
| UC-2 | NOT_STARTED | UC-1 | - | - |
| UC-3 | NOT_STARTED | UC-2 | - | - |
| UC-4 | NOT_STARTED | UC-3 | - | - |
| UC-5 | NOT_STARTED | UC-1, UC-2, UC-3, UC-4 | - | - |

## UC-1 Evidence

- Started: 2026-09-24 15:53 EEST
- Started from: `1c26853df73b91d214d66e8fd9d6f1182a62d357`
- Pre-existing dirty files: none
- Implementation submission: HEAD at convergence; see `checkpoints/UC-1.md`.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,inspection-state.js,messages.js,styles.css,week-renderer.js}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this ledger, and `checkpoints/UC-1.md`.
- Focused command: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#inspectsCompactWideCurrentWorkbenchInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` — 26 unit and 1 browser test, 0 failures/errors/skips.
- Full command: `./mvnw -q -pl timetable-workspace -am verify` — 26 unit and 83 integration tests, including 35 real-browser tests; 0 failures/errors/skips. `git diff --check` passed; no tracked runtime or unrelated changes after the run.
- Runtime evidence: isolated Testcontainers PostgreSQL, packaged workspace and kernel verifier, and real Chrome at 1600×900, 1280×800, 1279×800, 701×844, 700×844, and 390×844 CSS px. Screenshots: `timetable-workspace/target/workbench-layout/uc1-current-{1600,1280,390}.png` and `uc1-current-day-1600.png`.

| Contract element | Evidence |
|---|---|
| Main 1–2 | `WorkspaceBrowserIT.java:490–550`: accepted scale school, exact 1,000 lesson IDs, five Week headings, compact header, closed Current task area and disclosures. |
| Main 3–4 | `WorkspaceBrowserIT.java:552–573,928–984`: Week/Day, search/highlights without narrowing, teacher availability, intersected filters, exact represented count, closed-filter summary and clear. |
| Main 5–6 | `WorkspaceBrowserIT.java:574–586,988–1070`: exact accepted selection, accessible name, class/teacher/room focus and return, including empty recurring schedules. |
| Main 7–8 | `WorkspaceBrowserIT.java:584–591,385–485`: inspector collapse/reopen and Utilities, retained selection, larger canvas, export confined to Utilities. |
| Extension 1a | `WorkspaceBrowserIT.java:164–206`: real import rejects missing accepted lesson name; no Current/inspector or durable change. Existing import verifier tests cover invalid input and unmappable references. |
| Extension 1b | `WorkspaceBrowserIT.java:626–646`: 700 and 390 px read-only agenda, true lifecycle, no desktop matrix or repair controls, one warning. |
| Extension 2a | `WorkspaceBrowserIT.java:1074–1098`: accepted zero-assignment school retains declared classes/time/empty positions without lessons. |
| Extension 3a | `WorkspaceBrowserIT.java:1102–1150`: invalid/blocked school-scoped preference falls back, selected lesson outside Day clears with explanation, exact durable state unchanged. |
| Extension 4a | `WorkspaceBrowserIT.java:563–573`: zero filter matches retain all 60 class rows and time structure, named zero count, direct clear, no complete-school label. |
| Extension 5a | `WorkspaceBrowserIT.java:586–591`: repair setup opens then closes before staging with no mutating request or durable write; UC-2 remains separate. |
| Extension 6a | `WorkspaceBrowserIT.java:1032–1070`: empty focused class/teacher/room schedules identify the entity and do not invent lessons. |
| Extension 8a | `WorkspaceBrowserIT.java:456–485`: blocked export reports no bundle and preserves selection, filter, Current, and exact durable document. |
| G1–G3 | `WorkspaceBrowserIT.java:511–550,601–646`; `styles.css:268–356`: measured width, five-day fit, 36/60 px ordinary slots, inspector breakpoints, no page-level horizontal scroll, sticky matrix headings and read-only narrow view. |
| G4 | `WorkspaceBrowserIT.java:500,543–550,584–591,647`: intercepts mutating fetch calls and compares full durable document/version before and after presentation actions; optional preference failure is nonblocking. |
| G5–G6 | `app.js:294–303,487–526`; `messages.js:214–253`; `WorkspaceImportIT.java:459–534`: structural/text cues, native controls, English catalog, unchanged local Host/Origin/CSRF and denied-route boundary. |
| G7 | `WorkspaceBrowserIT.java:490–647,2805–2844`: independently verifier-checked normative accepted pair, exact rendered lesson set and durable state, viewport and focus/disclosure evidence. |
| Success postcondition | `WorkspaceBrowserIT.java:574–591`: exact accepted lesson and focused return in complete-school context. |
| Minimal guarantee | `WorkspaceBrowserIT.java:164–206,563–573,626–646,1102–1150,456–485`: invalid, empty, utility-failure and narrow branches do not invent or mutate accepted data. |
| RULE-1, RULE-2 | `app.js:40–42,157–171`; `inspection-state.js:1–104`: one snapshot source and presentation-state owner; no new route, persistence field, dependency or durable layout state. |
| RULE-4, RULE-5 | `styles.css:268–356`; `WorkspaceBrowserIT.java:511–550,601–646`: compact canvas, stable/below-canvas inspector, separate non-overlaying setup area and measured desktop/breakpoint geometry. |
| RULE-6, RULE-7 | `app.js:487–526,904–1040`; `week-renderer.js:1–11`; `WorkspaceBrowserIT.java:552–586,164–206`: complete/narrowed population, exact names/IDs/order and accepted availability; malformed import refused. |
| RULE-12, RULE-13 | `WorkspaceImportIT.java:459–534`; `WorkspaceBrowserIT.java:385–485,626–646`; `messages.js:74–81,214–253`: local security regression, native/textual controls, applicable Utilities, no narrow mutation. |
| RULE-14 | `WorkspaceBrowserIT.java:490–647,2805–2844`: real browser, isolated normative school, exact document comparison, 35/35 shared browser regression green. Human walkthrough remains for convergence. |

## Blockers

None. The separate `timetable-ux-polish` UC-5 administrator gate remains `PENDING_WALKTHROUGH`; this feature does not change that verdict.

## Deviations

None.
