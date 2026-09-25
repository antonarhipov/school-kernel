# Use-Case Status: timetable-workbench-layout

## Current

- Use case: UC-5
- Status: READY_FOR_CONVERGENCE
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `c407a02` | `convergence/UC-1.md`: APPROVE; automated gate green and administrator walkthrough passed 2026-09-24 |
| UC-2 | APPROVED | UC-1 | `255ab36`; `checkpoints/UC-2.md` | `convergence/UC-2.md`: APPROVE; automated gate green and administrator walkthrough passed 2026-09-24 |
| UC-3 | APPROVED | UC-2 | `4bb218a`; `checkpoints/UC-3.md` | `convergence/UC-3.md`: APPROVE; automated gate green and administrator walkthrough accepted 2026-09-24 |
| UC-4 | APPROVED | UC-3 | `fa2323a`; revised submission `f519d4a` | `convergence/UC-4.md`: APPROVE; automated gate and administrator walkthrough passed 2026-09-25 |
| UC-5 | READY_FOR_CONVERGENCE | UC-1, UC-2, UC-3, UC-4 | HEAD at convergence; `checkpoints/UC-5.md` | - |

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

## UC-2 Evidence

- Started: 2026-09-24 19:16 EEST
- Started from: `5966fab409c36a4ff89f7d32b0e7cdd5865c365e`
- Pre-existing dirty files: none
- Prior convergence findings: none; UC-1 is `APPROVED`.
- Implementation submission: HEAD at convergence; see `checkpoints/UC-2.md`.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceBrowserIT.java,WorkspaceRepairDraftIT.java}`, this ledger, and `checkpoints/UC-2.md`.
- Focused verification: four affected real-browser journeys and all nine `WorkspaceRepairDraftIT` cases passed together (0 failures/errors/skips); the strengthened normative UC-2 browser journey also passed alone (26 unit + 1 browser, 0 failures/errors/skips). `node --check` and `git diff --check` pass.
- Full relevant suite on the final diff: `./mvnw -q -pl timetable-workspace -am verify` passed 26 unit + 84 integration tests, including all 36 browser cases; 0 failures/errors/skips. `git diff --check` and `node --check` passed; no tracked runtime or unrelated file changed. Pin-feedback p95 was 252.5 ms versus a 250 ms diagnostic reference, not a functional gate.
- Runtime evidence: isolated Testcontainers PostgreSQL, packaged workspace/kernel verifier, real Chrome at 1600×900, 1280×800, 1279×800, 701×844, 700×844, and 390×844 CSS px. Ignored screenshots: `timetable-workspace/target/workbench-layout/uc2-draft-{1600,1280}.png`.

| Contract element | Evidence |
|---|---|
| Main 1–3; Requires UC-1; Extends UC-1 at 5a | `WorkspaceBrowserIT.java:660–693` starts from UC-1's verifier-checked 1,000-lesson accepted canvas, opens/closes setup without a write, stages exact teacher-16/period-0 intent, checks direct effect lesson-960, exact accepted bundle, and Draft-only task controls. |
| Main 4 | `WorkspaceBrowserIT.java:695–716` creates an exact blocking pin conflict, navigates to it from a Day/filter exclusion, resolves it, and applies individual room protection to lesson-500; accepted definition/result stay exact. |
| Main 5 | `WorkspaceBrowserIT.java:717–786` checks all 60 class-eight IDs in preview, cancel with no write, failed confirmation, exact successful action/dimensions/IDs, failed undo, and successful undo retaining the individual pin. `WorkspaceRepairDraftIT.java:165–231` checks production HTTP snapshot semantics by value. |
| Main 6–7; success | `WorkspaceBrowserIT.java:782–818` checks saved ready conflict-free Draft, separate Draft collapse state, Current/Draft/focus return and zero mutation requests, exact stored document/version, 1600/1280 geometry and 1279 stacking. |
| Extension 1a | `WorkspaceBrowserIT.java:670–675`: setup close leaves only accepted Current and the exact document/version. |
| Extension 1b | `WorkspaceBrowserIT.java:825–839`: 700 and 390 px show read-only Draft with no stage, pin, bulk, solve, discard, or task controls; durable state exact. |
| Extension 1c | `WorkspaceBrowserIT.java:2617–2647`: verified Proposal revision reopens the same durable Draft and pins in the wide area without advancing Current. |
| Extension 2a | `WorkspaceBrowserIT.java:676–678` refuses no periods before Draft creation; `WorkspaceRepairDraftIT.java:276–289` refuses missing teacher/period over HTTP with exact unchanged lifecycle, document, and version. |
| Extension 2b | `WorkspaceBrowserIT.java:819–824` rejects stale stage with exact prior document/version and disabled solve; `WorkspaceRepairDraftIT.java:329–354` forces failed pin persistence and proves HTTP solve refusal. |
| Extension 3a | `WorkspaceBrowserIT.java:852–858`: room-99/period-0 remains saved and ready with zero invented direct effects. |
| Extension 4a | `WorkspaceBrowserIT.java:695–710` shows exact conflict on canvas/task and disables solve; `WorkspaceRepairDraftIT.java:129–160` confirms service-level pin/policy conflict and refused run without process state. |
| Extension 4b | `WorkspaceBrowserIT.java:702–707`: conflict navigation returns Tuesday/room narrowing to Monday/required view, preserves search/teacher investigation, selects the same lesson, explains adjustment, and saves nothing. |
| Extension 5a | `WorkspaceBrowserIT.java:717–729`: preview/cancel leave complete stored document and version exact. |
| Extension 5b | `WorkspaceBrowserIT.java:730–783`: real PostgreSQL write failures on bulk confirm and undo retain exact Draft/version, report failure and disable generation; later explicit retry succeeds. |
| Extension 6a | `WorkspaceBrowserIT.java:846–850`: confirmed discard removes Draft/attempt pins, returns to accepted Current, and preserves exact accepted bundle. |
| Extension 6b | `WorkspaceBrowserIT.java:840–845`: reload selects Draft/open task area with saved intent and reset selection/search/filters, no durable change. |
| Extension 7a | `WorkspaceRepairDraftIT.java:140–150,329–354`: conflicting and failed-save HTTP runs are refused below the UI with exact prior state/version and no run/proposal; browser solve remains disabled. |
| G1 | `app.js:377–399,421–466`; `WorkspaceBrowserIT.java:679–716`: Draft decorates the accepted model, and pins retain accepted period/room values and attempt sources without editing assignments. |
| G2 | `styles.css:331–378`; `WorkspaceBrowserIT.java:689–693,807–816,863–899`: separate task/inspector, task ≤35% viewport, no overlay/page horizontal overflow, heading plus full class row visible, scrollable task and reachable decisions at both target desktop sizes. |
| G3 | `WorkspaceBrowserIT.java:788–806,819–824`; `WorkspaceRepairDraftIT.java:306–354`: presentation actions issue no mutation and preserve exact document/version; writes retain existing CSRF/`If-Match`/storage refusal boundary. |
| G4 | `app.js:439–462,679–684`; `WorkspaceBrowserIT.java:695–716,1877–1880`; `WorkspaceRepairDraftIT.java:129–160`: exact IDs/conflict reason/pin dimensions and distinct persistent-versus-attempt lock provenance with text cues. |
| G5 | `WorkspaceBrowserIT.java:717–786`; `WorkspaceRepairDraftIT.java:165–265`: immutable preview, exact confirmation and named undo, with failed/stale saves unable to alter protection or enable solve. |
| G6 | `WorkspaceBrowserIT.java:813–839`; `styles.css:370–386`: wide stacked intermediate task at 1279/701 and no repair controls at 700/390. |
| G7 | `WorkspaceBrowserIT.java:660–859`: real scale browser exercises teacher/room/no-effect, conflict, individual/bulk, save failure, collapse/focus/reload/discard, narrow refusal, and exact accepted/Draft/version state. |
| Success postcondition | `WorkspaceBrowserIT.java:780–786`: one durable conflict-free Draft with explicit room pin remains ready while accepted baseline is exact Current. |
| Minimal guarantee | `WorkspaceBrowserIT.java:670–678,702–707,717–750,760–783,819–850`; `WorkspaceRepairDraftIT.java:140–150,276–289,329–354`: refused/cancelled/stale/unsaved actions keep the last durable accepted and Draft state exact and start no ineligible run. |
| RULE-1, RULE-2 | `app.js:40–153,377–399`, `inspection-state.js:1–104`, `WorkspaceBrowserIT.java:788–806`; one existing snapshot/model/inspection-state owner, per-mode ephemeral task state and exact no-mutation browser trace; no new route, persisted field or dependency. |
| RULE-3, RULE-8 | `WorkspaceRepairDraftIT.java:129–160,165–265,276–354`; guarded lifecycle, conflict/preview/pin/save refusal; browser stages and protects through existing production routes. |
| RULE-5, RULE-7 | `app.js:377–462,679–684`, `styles.css:331–378`, `WorkspaceBrowserIT.java:660–859`: task owns workflow, inspector retains authoritative details/cues, text escaped and lock provenance distinct, measured desktop/intermediate layout. |
| RULE-12, RULE-13 | No server/security diff; shared hostile Host/Origin/CSRF and narrow-browser regression passed. New labels are in `messages.js`; visible buttons, native fields/disclosures, text cues and applicable Utilities remain. |
| RULE-14 | Verifier-checked normative accepted school, exact browser/JDBC state and final-diff 36-case shared browser regression passed; the UC-2 administrator walkthrough remains for convergence. |

## UC-3 Evidence

- Started: 2026-09-24 21:14 EEST
- Started from: `06e89134600b685304354bf7aa982a4a146819e0`
- Pre-existing dirty files: none
- Prior convergence findings: none; UC-2 is `APPROVED`.
- Implementation submission: HEAD at convergence; see `checkpoints/UC-3.md`.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this ledger, and `checkpoints/UC-3.md`.
- Focused verification: the normative UC-3 browser journey passed alone; four UC-3 browser journeys plus all ten `WorkspaceRepairPlanningIT` cases passed together; the strengthened normative journey and the shared real-repair browser regression passed together. All focused runs had 0 failures/errors/skips. `node --check` for both changed modules and `git diff --check` passed.
- Full relevant suite on the final diff: `./mvnw -q -pl timetable-workspace -am verify` passed 26 unit + 84 integration tests, including all 36 browser cases; 0 failures/errors/skips. The run changed no tracked runtime or unrelated file. The pin-feedback p95 was 291.4 ms against a 250 ms diagnostic-only reference; the functional assertions passed.
- Runtime evidence: isolated PostgreSQL, packaged workspace and kernel verifier, real Chrome against the independently verified 1,000-lesson school at 1600×900, 1280×800, 1279×800, 701×844, 700×844, and 390×844 CSS px. Ignored screenshots: `timetable-workspace/target/workbench-layout/uc3-solving-{1600,1280,390}.png` and `uc3-proposal-handoff-1280.png`.

| Contract element | Evidence |
|---|---|
| Main 1; Requires UC-2 | `WorkspaceBrowserIT.java:2090–2143` starts from UC-2's saved conflict-free teacher-unavailability Draft with exact room pin, then launches through the existing browser solve action. `WorkspaceRepairPlanningIT.java:93–140` exercises the packaged replan boundary. |
| Main 2 | `WorkspaceBrowserIT.java:2144–2152,2165–2168` checks the exact accepted bundle/Draft/run ID and PT30S limit, frozen named intent and pin in the wide task area, visible cancellation, and absent editing/decision controls. |
| Main 3–4 | `WorkspaceBrowserIT.java:2153–2219` changes Week/Day, inspector, frozen detail, modes, responsive width, and selected context; `WorkspaceBrowserIT.java:1925–1942` adds focused teacher schedule return during a live run. Exact document and single process invocation remain unchanged, Current stays accepted, and no Proposal is exposed. |
| Main 5–6; success | `WorkspaceBrowserIT.java:2229–2272` submits a complete fixture outcome through the production run boundary, independently verifies it with the packaged verifier, checks exact accepted/Draft/proposal fields, removes Solving, opens Proposal's wide task area and measures its handoff geometry. `WorkspaceBrowserIT.java:1731–1794` also runs a genuine packaged feasible replan and checks reviewable Proposal with old Current exact. |
| Extension 1a | `WorkspaceRepairDraftIT.java:129–160,329–354` and `WorkspaceRepairPlanningIT.java:502–535` refuse conflicting, unsaved, and stale Draft starts at HTTP/service boundaries with exact prior state/version and no scheduler run. |
| Extension 2a | `WorkspaceBrowserIT.java:2219–2225` cancels a blocked browser run and verifies Draft, cancelled terminal outcome, old Current, and no Proposal. `WorkspaceRepairPlanningIT.java:454–535` tests cancellation and late-output suppression. |
| Extension 2b | `WorkspaceBrowserIT.java:2033–2085` stops the actual application mid-run, restarts against the same durable school, checks one safe interrupted terminal record, exact accepted/Draft data, editable task, absent Proposal, and no late application. |
| Extension 3a | `WorkspaceBrowserIT.java:2208–2213` switches a live run to Tuesday from a selected Monday lesson, checks explanatory selection clearing and intact status/cancellation, compares the entire running document, then restores Monday and the same lesson. |
| Extension 5a | `WorkspaceBrowserIT.java:1976–2030` checks no-feasible, invalid input, transport, interruption, mismatch, and watchdog outcomes with safe Draft/Utilities diagnostics and no candidate; `WorkspaceRepairPlanningIT.java:420–453` asserts exact HTTP/JDBC state for failed and rejected output. |
| Extension 5b | `WorkspaceBrowserIT.java:1987–2005` retries only unchanged no-feasible intent at PT2M; cancellation and other failures expose no retry. `WorkspaceRepairPlanningIT.java:376–419` verifies service-level retry gating by value. |
| Extension 6a | `WorkspaceBrowserIT.java:2224–2228` rejects mismatched output before candidate rendering, retains exact Draft/Current and shows safe diagnostics; `WorkspaceRepairPlanningIT.java:420–453` covers incomplete, stale, non-feasible, mismatched and rejected outcomes at the production HTTP boundary. |
| G1 | `app.js:437–477,595–647`; `WorkspaceBrowserIT.java:2144–2152,2200–2219`: only accepted assignments render, saved intent is frozen, and pin/edit/discard/decision controls are absent while cancel is present. |
| G2 | `styles.css:337–351,410–412`; `WorkspaceBrowserIT.java:2163–2200,2277–2307`: status/cancel survive detail collapse, 1600/1280 geometry keeps task ≤35% height below canvas, headers and one full row visible, inspector beside, no overflow/clipping; 1279/701 stack. |
| G3 | `WorkspaceBrowserIT.java:2153–2219` captures mutating fetch methods (none), checks exact document and one replan invocation through range/inspector/task/mode/resize/focus actions, and retains representable selection. |
| G4 | `WorkspaceBrowserIT.java:1731–1794,2224–2272`; `WorkspaceRepairPlanningIT.java:93–140,420–453`: independently verified complete feasible output alone creates Proposal; failed/rejected output exposes no candidate or Current change. |
| G5 | `app.js:450–477`; `messages.js:122–137`; `WorkspaceBrowserIT.java:2151–2169,1976–2030`: textual running/limit/accepted/frozen/cancel and safe failure diagnostics, native buttons, no optimality claim. |
| G6 | `WorkspaceBrowserIT.java:2183–2200`, `WorkspaceImportIT.java:459–534`: 700/390 read-only true Solving state without cancellation or mutations; local access/security and exact stored run preserved. |
| G7 | `WorkspaceBrowserIT.java:2090–2272,2033–2085,1976–2030`: normative-scale browser and actual restart, exact accepted/Draft/run/Proposal comparisons, cancelled/unsuccessful/rejected/retry/verified branches and all target/boundary viewports. |
| Success postcondition | `WorkspaceBrowserIT.java:2229–2272`: one independently verified Proposal opens for review; accepted baseline and saved Draft are exact and Current is still accepted. |
| Minimal guarantee | `WorkspaceBrowserIT.java:1976–2085,2219–2228`; `WorkspaceRepairPlanningIT.java:420–535`: cancellation, restart, failure, timeout and rejected output keep exact old accepted/Draft data and expose no unverified Proposal. |
| RULE-1, RULE-2 | `app.js:52–174,437–465`, `inspection-state.js:1–104`, `WorkspaceBrowserIT.java:2153–2219`: one snapshot/accepted-model/state owner, per-mode page-session detail, no new route/storage/dependency and no mutation request from presentation actions. |
| RULE-3, RULE-9 | `WorkspaceRepairPlanningIT.java:376–535`, `WorkspaceBrowserIT.java:1900–2272`: existing service lifecycle/refusal, frozen Solving, bounded cancellation/recovery/failure/retry, independent verification and exact terminal states. |
| RULE-5, RULE-7 | `app.js:176–257,437–477,595–647`; `styles.css:337–355,410–412`; `WorkspaceBrowserIT.java:2151–2200,2229–2272`: task/inspector split, authoritative names/IDs, safe text, accepted availability, measured solving and Proposal-handoff layout. |
| RULE-12, RULE-13 | No server/security diff; full hostile Host/Origin/CSRF/`If-Match` regressions passed. `messages.js:122–137`, `app.js:450–465`, `WorkspaceBrowserIT.java:2183–2200`: catalog strings, native/textual controls, read-only narrow agenda and unchanged local routes. |
| RULE-14 | `WorkspaceBrowserIT.java:2090–2272,3122–3200`: independently verifier-checked normative school and exact durable state; full final-diff 36-case shared browser regression and 84-case integration suite passed. Human UC-3 walkthrough remains for convergence. |

## UC-4 Evidence

- Started: 2026-09-24 from `7e276394cfc287d0a07d3e50f2642f33b6db8d88`.
- Pre-existing dirty files: none; prior convergence findings: none; UC-3 is `APPROVED`.
- Implementation submission: HEAD at convergence; see `checkpoints/UC-4.md`.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,inspection-state.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this ledger, and `checkpoints/UC-4.md`.
- Focused verification: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#reviewsIndependentlyVerifiedNormativeRepairInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` passed with Docker access: 1 real-browser case, 26 workspace unit tests, 58 kernel tests; 0 failures/errors/skips. An unprivileged attempt could not initialize Testcontainers; a first assertion expected a toolbar search input inside the focused schedule, where no toolbar is rendered, and was corrected before the passing run.
- Full relevant suite on the final diff: `./mvnw -q -pl timetable-workspace -am verify` passed 195 tests: 58 kernel unit, 27 kernel integration, 26 workspace unit and 84 workspace integration (36 real-browser); 0 failures/errors/skips. `node --check` for all three changed ES modules and `git diff --check` passed. The run changed no tracked runtime data or unrelated file. Pin-feedback p95 was 317.8 ms against a 250 ms diagnostic-only reference; functional assertions passed.
- Runtime evidence: isolated PostgreSQL, packaged workspace and kernel verifier, real Chrome with independently verified 1,000-lesson school; inspected `target/workbench-layout/uc4-proposal-{1600,1280}.png`. The screenshots show subject/room and protected cues on a selected compact Week tile, accepted/proposed identity, a class row and time headings beside the inspector, and reachable acceptance/revise/discard decisions beneath the canvas.
- Convergence findings: `convergence/UC-4.md` G-1 (no mutating-request trace), G-2 (missing Proposal viewport/scroll checks), G-3 (independent full reactor had one isolated-passing UC-2 CDP timeout). No observed UC-4 product failure; final gate is incomplete.
- Revision started: 2026-09-25 from `e2137b1`; pre-existing dirty files: none. Revised files: `timetable-workspace/src/main/resources/static/workspace/styles.css`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this ledger, and `checkpoints/UC-4.md`. The 701 px browser check exposed a 10 px internal Proposal-task overflow; stacking its before/after panels at 701–800 px removed it.
- Revision focused verification: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#reviewsIndependentlyVerifiedNormativeRepairInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` passed after the responsive fix: 1 real-browser case, 26 workspace unit cases and 58 kernel unit cases, 0 failures/errors/skips. The first revised focused run failed at the new 701 px overflow assertion, prompting the CSS correction.
- Revision full verification: `./mvnw -q -pl timetable-workspace -am verify` passed on the revised diff: 195 tests (58 kernel unit, 27 kernel integration, 26 workspace unit, 84 workspace integration including all 36 browser cases), 0 failures/errors/skips. `git diff --check` passed; the run left no tracked runtime data or unrelated changes. G-1 now records zero review-only mutating fetches and exactly one confirmed acceptance `POST`; G-2 measures 1600/1280/1279/701, reads the verified Proposal at 700/390, and compares both matrix scroll offsets through collapse/reopen; G-3 has a clean shared reactor.

| Contract element | Evidence |
|---|---|
| Main 1 | `WorkspaceBrowserIT.java:2621–2648,2696–2716`: a saved UC-2 Draft enters UC-3's independently verified Proposal; Current stays the exact old bundle, Proposal is named not current, and the canvas renders all 1,000 IDs with both sides of the period move. |
| Main 2 | `WorkspaceBrowserIT.java:2649–2695`: exact unique, category, grouping, direct/ripple, protection and safe run values are compared to the persisted authoritative review in the wide task area. |
| Main 3 | `WorkspaceBrowserIT.java:2747–2778`: category, detail, and grouping links select accepted origin or proposed destination for one stable lesson, including Day and focused return. |
| Main 4 | `WorkspaceBrowserIT.java:2717–2749,3186–3190`: old/new subject, class, teacher, weekday, period and room names/IDs and changed cues are asserted by value in the inspector; the task area repeats selected before/after, effect, protection and target side. |
| Main 5 | `WorkspaceBrowserIT.java:1792–1798,2696–2703,2775–2863`: inspects several lessons, switches Current/Proposal, uses Day/focus, and requires explicit confirmation before acceptance. |
| Main 6; success postcondition | `WorkspaceBrowserIT.java:2863–2875,3100–3115`: confirmed acceptance stores the exact successor definition/result, removes Draft/Proposal, leaves only Current and the retained representable context; an out-of-Day selection clears with explanation. |
| Extension 1a | `WorkspaceBrowserIT.java:2224–2228`; `WorkspaceRepairPlanningIT.java:420–450`: rejected or invalid production run output returns to exact Draft/Current with no candidate. Existing verifier/import tests reject missing required metadata and unmappable references before operational display. |
| Extension 1b | `WorkspaceBrowserIT.java:2829–2838`: the independently verified Proposal at both 700 and 390 px is a labelled read-only agenda with both sides of the moved lesson, no review decision or desktop matrix, no page overflow and exact unchanged stored state. |
| Extension 2a | `WorkspaceBrowserIT.java:2919–2938`: zero counts and labelled empty categories/groupings remain visible; the presentation-only zero-change fixture does not advance Current. |
| Extension 3a | `WorkspaceBrowserIT.java:2540–2583,2747–2778`: one-sided filters/search retain both representations; origin/destination and grouping navigation adjust only excluding Day/period/room/focus, explain changes, preserve search/highlights and stable identity. |
| Extension 3b | `WorkspaceBrowserIT.java:2509–2513,2733–2746`: same-slot room change produces one combined tile with exact old/proposed values. |
| Extension 3c | `WorkspaceBrowserIT.java:2509–2523`: cancellation has only accepted side and addition only proposed side, both with explicit status; a proposal-only selection is cleared in Current. |
| Extension 4a | `WorkspaceBrowserIT.java:2523–2526,2776–2785`: an unchanged, protected lesson is named accepted/unchanged and remains outside the two-lesson change total. |
| Extensions 5a and 5b | `WorkspaceBrowserIT.java:2883–2915,2945–2968`: revise and discard each consume a verified scale Proposal, remove only Proposal, reopen the exact saved editable Draft, and retain the old accepted bundle. |
| Extension 6a | `WorkspaceBrowserIT.java:2969–2985`; `WorkspaceRepairPlanningIT.java:306–340`: stale identity refuses acceptance, invalidates Proposal to Draft, and preserves exact Current/Draft. |
| Extension 6b | `WorkspaceBrowserIT.java:1808–1835`; `WorkspaceRepairPlanningIT.java:343–373`: injected PostgreSQL acceptance failure leaves full document/version exact, Proposal reviewable, and explicit retry available. |
| G1 | `WorkspaceBrowserIT.java:2649–2678,2705–2716,2509–2513`: stable IDs and exact authoritative unique/category/direct/ripple counts distinguish two move tiles from one changed lesson and prevent overlapping sums. |
| G2 | `app.js:214–370,802–833`; `WorkspaceBrowserIT.java:2540–2583,2719–2778`: both-sided matching, named navigation target, complete before/after IDs, and unavailable optional name cue; verifier refuses invalid candidate output. |
| G3 | `styles.css:338–380,413–429`; `WorkspaceBrowserIT.java:2786–2852`: measured 1600×900/1280×800 task ≤35% height below canvas with inspector beside; 1279/701 stack inspector and task without page or task horizontal overflow; headings, full row and decisions remain accessible; selection/count and actual horizontal/vertical matrix scroll survive collapse/reopen. |
| G4 | `inspection-state.js:5–129`; `WorkspaceBrowserIT.java:2630–2641,2853–2868`: review-only mode/filter/search/selection/focus/task/responsive actions issue zero mutating fetch requests and preserve the exact accepted/Draft/Proposal document and version; confirmation alone issues none; only the explicit accept sends `POST /api/proposal/accept`. |
| G5 | `app.js:802–833,1151–1173`; `messages.js:129–166`; `WorkspaceBrowserIT.java:2696–2701,2769–2782`: compact text/structural origin, destination, same-slot, effect, pin and selection cues with complete accessible names; accepted and proposed availability are distinctly named. |
| G6; minimal guarantee | `WorkspaceBrowserIT.java:1808–1845,2857–2916,2971–2988`; `WorkspaceRepairPlanningIT.java:306–373`: only confirmed durable acceptance advances exact Current; stale/failed/refused/withdrawn Proposal never partially advances or mislabels it. |
| G7; Requires UC-3 | `WorkspaceBrowserIT.java:2588–2988`; `WorkspaceRepairPlanningIT.java:306–373`: real browser, packaged process/verifier, isolated JDBC state, comparison shapes and decisions consume UC-3's actual verified Proposal; all 36 shared browser cases and the full 195-test reactor passed on the revised diff. |
| RULE-1, RULE-2 | `app.js:26–42,211–349`; `inspection-state.js:5–129`; `WorkspaceBrowserIT.java:2630–2641,2853–2868`: one packaged snapshot/model and presentation owner, and zero review-only mutation traffic; no new API, durable field, runtime dependency or persisted layout state. |
| RULE-3 | `WorkspaceRepairPlanningIT.java:290–373,420–450`; guarded existing service transitions and exact stale/failure refusals; the UI calls only existing decision routes. |
| RULE-5 | `styles.css:338–380,413–429`; `WorkspaceBrowserIT.java:2786–2852`: wide task area owns review/decisions; inspector remains concise and separate, with measured 1279/1280 and 701 Proposal geometry and scroll retention. |
| RULE-6 | `app.js:689–748,1151–1232`; `WorkspaceBrowserIT.java:2540–2583,2705–2716,2747–2778`: complete unique population, intersected either-side narrowing, retained comparison sides and named context adjustments. |
| RULE-7 | `app.js:802–873,1151–1173`; `WorkspaceBrowserIT.java:2719–2746,2769–2782`: authoritative names/IDs and accepted/proposed availability, escaped text, complete accessible lesson/protection state. |
| RULE-10 | `app.js:214–370,802–833`; `WorkspaceBrowserIT.java:2649–2778`: stable comparison index, navigable groupings and both move targets, one combined same-slot tile, one-sided additions/cancellations, exact unique and overlap counts. |
| RULE-11 | `app.js:290–296`; `WorkspaceBrowserIT.java:1808–1845,2859–2915,2969–2985`: existing guarded revise/discard/accept actions and exact acceptance/refusal states. |
| RULE-12, RULE-13 | No server/security-route diff; `messages.js:129–166`, `WorkspaceBrowserIT.java:2829–2838`: existing local Host/Origin/CSRF/`If-Match` regression, verified Proposal read-only at 700/390, English catalog strings, native controls and non-color cues. |
| RULE-14 | `WorkspaceBrowserIT.java:2588–2988,3186–3190`: normative isolated school, independent verifier, exact persisted document/field assertions, request trace, responsive/scroll geometry and clean 36-browser/195-reactor regression. Administrator walkthrough remains for convergence. |

## UC-5 Evidence

- Started: 2026-09-25 08:02 EEST from `5542555b9855df55851c9880b7e45ad29ceae79a`.
- Pre-existing dirty files: none in the initial `git status --short`. An out-of-scope edit to `spec/timetable-workbench-layout/spec.md` appeared during execution and is excluded from this submission.
- Implementation submission: HEAD at convergence; see `checkpoints/UC-5.md`.
- Changed files: `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, this ledger, and `checkpoints/UC-5.md`. The production path was implemented and approved by UC-1 through UC-4; UC-5 adds the continuous wide-layout verification and successor-repair proof.
- Focused verification: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessorInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` passed with Docker access: 1 browser case and the applicable unit tests, 0 failures/errors/skips. The initial sandboxed attempt could not access Docker; the first Docker run exposed a hidden-row measurement and an overly strict task-bottom assertion, which were corrected before the passing run.
- Full relevant suite on the submitted diff: `./mvnw -q -pl timetable-workspace -am verify` passed 195 tests: 85 kernel (58 unit, 27 integration) and 110 workspace (26 unit, 84 integration including all 36 browser cases), 0 failures/errors/skips. `git diff --check` passed. Tests changed no tracked runtime data or unrelated file.
- Runtime evidence: one Chrome session, isolated PostgreSQL, packaged workspace and kernel verification, and the independently verified 1,000-lesson school; Draft, Solving, and Proposal screenshots at 1600×900 and 1280×800 are ignored artifacts in `timetable-workspace/target/workbench-layout/uc5-*.png`. The verified Proposal was also read at 390×844. Proposal tiles become visually dense with simultaneous investigation cues; this is recorded for the administrator feature gate, not treated as participant feedback.

| Contract element | Evidence |
|---|---|
| Main 1; Includes UC-1 | `WorkspaceBrowserIT.java:2991–3020`: verifier-checked 1,000 accepted identities, exact affected lesson, Day/Week, selected details, teacher focus and return in one real browser; accepted document exact. |
| Main 2; Includes UC-2 | `WorkspaceBrowserIT.java:3021–3046`: same browser stages teacher-16/period-0, checks direct `[lesson-960]`, resolves an actual pin conflict, saves lesson-500 room protection, retains selection/investigation and exact old Current. |
| Main 3; Includes UC-3 | `WorkspaceBrowserIT.java:3048–3084`: saved ready Draft becomes frozen Solving with exact run/Draft/Current, mode and range remain usable with one process, cancellation returns to exact Draft; a new run produces an independently verified Proposal. |
| Main 4; Includes UC-4 | `WorkspaceBrowserIT.java:3085–3123`: one browser compares period move, same-slot room ripple, protected unchanged lesson, both move sides, modes and Day context; review leaves exact old accepted/Draft/Proposal until explicit confirmation and acceptance. |
| Main 5; success postcondition | `WorkspaceBrowserIT.java:3124–3154`: durable acceptance installs exact verified successor definition/result/revision, removes Draft and Proposal, leaves Current and a representable canvas; a second Draft starts from that exact successor. |
| Extension 2a | `WorkspaceBrowserIT.java:3027–3036,1688–1729`: real conflict prevents process launch; failed Draft save keeps exact prior document/version and disables solving. |
| Extension 3a | `WorkspaceBrowserIT.java:3068–3084,1900–2085,2090–2250`; `WorkspaceRepairPlanningIT.java:420–535`: cancellation, actual restart, no-feasible/timeout/failure/rejected output return to exact Draft/Current without Proposal. |
| Extension 4a | `WorkspaceBrowserIT.java:2882–2968`: both revise and discard consume verified scale Proposals, return to retained Draft and exact old Current without acceptance. |
| Extension 4b | `WorkspaceBrowserIT.java:1808–1845,2969–2985`; `WorkspaceRepairPlanningIT.java:306–373`: stale identity invalidates to Draft; failed PostgreSQL acceptance retains exact old Current, version and reviewable Proposal for explicit retry. |
| Extension 5a | `WorkspaceBrowserIT.java:3129–3154,1731–1895`: the second room Draft has the accepted successor, exact new direct effect `[lesson-0]`, no inherited attempt pins or bulk actions, and no unrequested scheduler invocation; prior lock provenance is distinct from policy. |
| G1 | `WorkspaceBrowserIT.java:2991–3154`: stable 1,000 IDs, authoritative names/revisions and retained lesson relationship through all four modes and both ranges; one accepted model and snapshot path in `app.js` and `inspection-state.js`. |
| G2 | `WorkspaceBrowserIT.java:3021–3154,3158–3210`: header Current, active mode, direct conflict, frozen run, non-current Proposal and decision controls remain labelled through the journey. |
| G3 | `WorkspaceBrowserIT.java:3043,3060,3094–3104,3158–3210`: measured 1600/1280 Draft, Solving and Proposal with task ≤35% viewport, canvas header and represented row, side inspector, no page/task horizontal overflow and reachable active decisions; 390 Proposal is read-only. Six screenshots were visually inspected. |
| G4; minimal guarantee | `WorkspaceBrowserIT.java:3027–3036,3068–3084,3114–3123`; approved failure tests above: every blocked/cancelled/refused path keeps the last exact accepted bundle and never calls Draft, run or unaccepted Proposal Current. |
| G5 | `WorkspaceBrowserIT.java:2991–3154`: one isolated real-browser journey consumes UC-1 through UC-4 postconditions, compares exact persisted snapshots after handoffs and accepts a verified successor; all 36 browser and 195 reactor tests pass. The five-participant feature gate is reserved for convergence. |
| RULE-1, RULE-2 | `app.js:40–174`; `inspection-state.js:1–133`; `WorkspaceBrowserIT.java:3043,3064–3067,3114–3123,3158–3210`: one snapshot/model; layout/mode/resize and review actions preserve document plus version with no mutation traffic in approved related-UC tests. |
| RULE-3, RULE-8, RULE-9 | `RepairDraftService.java:35–178`; `RepairPlanningService.java:52–242`; `WorkspaceBrowserIT.java:3021–3084,2090–2250`; `WorkspaceRepairPlanningIT.java:90–535`: guarded Draft/solve/refusal transitions and exact failure outcomes. |
| RULE-4, RULE-5 | `styles.css:272–437`; `WorkspaceBrowserIT.java:490–648,3158–3210`: compact complete-school canvas and measured below-canvas task/side inspector at both desktops with read-only narrow boundary. |
| RULE-6, RULE-7 | `WorkspaceBrowserIT.java:490–648,2540–2583,2991–3020,3085–3123`; `app.js:689–833`: exact school names/IDs, accepted/proposed values, complete/narrowed population, either-side comparison and separately labelled availability. |
| RULE-10, RULE-11 | `WorkspaceBrowserIT.java:2588–2985,3085–3154`; `RepairDraftService.java:183–223`: one stable comparison identity, exact review counts and targets, explicit atomic acceptance, and next repair without inherited attempt pins. |
| RULE-12, RULE-13 | `WorkspaceBrowserIT.java:3094–3104`; `WorkspaceImportIT.java:459–545`; `messages.js:1–260`: narrow read-only Proposal, native/text cues and unchanged local Host/Origin/CSRF/`If-Match` boundary; no route or storage diff. |
| RULE-14 | `WorkspaceBrowserIT.java:2991–3210` plus the approved UC-1–UC-4 browser journeys: normative school and viewports, exact isolated state, one production-path handoff across four included UCs, complete shared regression. Human gate remains pending. |

## Blockers

None. The separate `timetable-ux-polish` UC-5 administrator gate remains `PENDING_WALKTHROUGH`; this feature does not change that verdict.

## Deviations

None.
