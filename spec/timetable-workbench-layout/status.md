# Use-Case Status: timetable-workbench-layout

## Current

- Use case: UC-2
- Status: READY_FOR_CONVERGENCE
- Next eligible: none until UC-2 converges and its walkthrough is confirmed

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `c407a02` | `convergence/UC-1.md`: APPROVE; automated gate green and administrator walkthrough passed 2026-09-24 |
| UC-2 | READY_FOR_CONVERGENCE | UC-1 | HEAD at convergence; `checkpoints/UC-2.md` | - |
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

## Blockers

None. The separate `timetable-ux-polish` UC-5 administrator gate remains `PENDING_WALKTHROUGH`; this feature does not change that verdict.

## Deviations

None.
