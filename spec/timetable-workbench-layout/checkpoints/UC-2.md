# Use-Case Checkpoint: UC-2 - Prepare a protected repair in the wide task area

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `5966fab409c36a4ff89f7d32b0e7cdd5865c365e`
- Submission commit: HEAD at convergence
- Relations verified: Requires approved UC-1's verifier-checked accepted, complete-school workbench and Extends UC-1 at 5a through its existing unstaged setup action. No Included UC. The Draft overlays the same accepted model, not a second timetable.

## Contract Evidence

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1 | `WorkspaceBrowserIT.java:664–675` opens the packaged Current page with 1,000 accepted IDs, selects lesson-960, opens setup in the wide task area with teacher-16 prefilled, and compares the exact stored document/version before and after. | PASS |
| Main 2 | `WorkspaceBrowserIT.java:676–684` stages teacher-16/period-0 through the browser form and checks every saved intent field by value. | PASS |
| Main 3 | `WorkspaceBrowserIT.java:680–693` checks durable Draft, one exact direct-effect ID, all 1,000 accepted IDs still represented, textual canvas cue, and task-only Draft controls. | PASS |
| Main 4 | `WorkspaceBrowserIT.java:695–716` creates the pin conflict, navigates from excluded Day/filter context, removes the contradiction, and adds an individual room pin to an unaffected accepted lesson. | PASS |
| Main 5 | `WorkspaceBrowserIT.java:717–786` checks the exact 60 preview IDs/display order/dimension, cancel without write, failed confirmation and undo, exact successful bulk action, and undo retaining the individual pin. `WorkspaceRepairDraftIT.java:165–265` independently checks HTTP snapshot integrity and refusal. | PASS |
| Main 6 | `WorkspaceBrowserIT.java:788–818` checks the wide sections, readiness and decisions, Draft collapse/reopen across Current/Draft, focused schedule return, selected lesson retention, no mutating fetch, exact document/version, and 1600/1280/1279 geometry. | PASS |
| Main 7 | `WorkspaceBrowserIT.java:780–786` proves one saved, conflict-free `readyToSolve` Draft with a room pin, enabled generation and unchanged accepted bundle. | PASS |
| Extension 1a | `WorkspaceBrowserIT.java:670–675` closes setup before staging; Current remains sole lifecycle state and exact document/version is unchanged. | PASS |
| Extension 1b | `WorkspaceBrowserIT.java:825–839` checks 700 and 390 px read-only Draft, no setup/stage/pin/bulk/solve/discard/task controls, and exact stored state. | PASS |
| Extension 1c | `WorkspaceBrowserIT.java:2617–2647` revises a verified Proposal through the production route and checks the same retained Draft/pins reopen in the wide area while Current remains accepted. | PASS |
| Extension 2a | `WorkspaceBrowserIT.java:676–678` refuses zero periods in the real setup journey. `WorkspaceRepairDraftIT.java:276–289` refuses missing teacher and period over HTTP with exact state/version and no Draft. | PASS |
| Extension 2b | `WorkspaceBrowserIT.java:819–824` forces a stale browser stage and checks explanation, exact last durable document/version and disabled solve. `WorkspaceRepairDraftIT.java:329–354` forces storage failure and HTTP solve refusal. | PASS |
| Extension 3a | `WorkspaceBrowserIT.java:852–858` stages room-99/period-0 with no direct assignment affected, zero direct IDs, exact saved intent and ready Draft. | PASS |
| Extension 4a | `WorkspaceBrowserIT.java:695–710` checks exact conflict ID/code, canvas/task cues and disabled solve. `WorkspaceRepairDraftIT.java:129–160` covers pin and persistent-policy conflicts and proves no service-level run. | PASS |
| Extension 4b | `WorkspaceBrowserIT.java:702–707` navigates a conflict excluded by Tuesday and room filter, changes only excluding context, preserves search/teacher investigation and accepted/Draft state, and explains the adjustment. | PASS |
| Extension 5a | `WorkspaceBrowserIT.java:717–729` proves preview then cancel leaves the complete stored document and version exact. | PASS |
| Extension 5b | `WorkspaceBrowserIT.java:730–783` injects PostgreSQL failures for bulk confirm and undo, verifies exact last durable Draft/version, visible refusal and disabled solve, then explicitly retries successfully. | PASS |
| Extension 6a | `WorkspaceBrowserIT.java:846–850` confirms discard, returns to Current, removes Draft and attempt-scoped pins, and retains the exact accepted bundle. | PASS |
| Extension 6b | `WorkspaceBrowserIT.java:840–845` reloads with the saved Draft, default Draft mode and open task area, reset ephemeral selection/search/filter, and unchanged durable document/version. | PASS |
| Extension 7a | `WorkspaceRepairDraftIT.java:140–150,329–354` sends real conflicting and failed-save generation requests; the service refuses each, leaves exact document/version, creates no run or proposal, and the browser never enables solve from the unsaved revision. | PASS |
| G1 | `app.js:377–399,421–466`, `WorkspaceBrowserIT.java:679–716`: Draft renders the accepted model and protects accepted dimensions with attempt sources; the accepted definition/result remains exact. | PASS |
| G2 | `styles.css:331–378`, `WorkspaceBrowserIT.java:689–693,807–816,863–899`: measured task is beneath canvas and ≤35% viewport, independent scroll, no page horizontal overflow, inspector beside at ≥1280, and time heading plus a complete class row with reachable decisions at 1600 and 1280. | PASS |
| G3 | `WorkspaceBrowserIT.java:670–675,788–806,819–824`; `WorkspaceRepairDraftIT.java:306–354`: presentation actions issue no mutation and preserve the exact document/version; explicit writes retain conditional HTTP and durable refusal. | PASS |
| G4 | `app.js:439–462,679–684`; `WorkspaceBrowserIT.java:695–716,1877–1880`; `WorkspaceRepairDraftIT.java:129–160`: exact IDs, weekly intent, conflict reason, pin dimensions and distinct policy/attempt/available cues. | PASS |
| G5 | `WorkspaceBrowserIT.java:717–786`; `WorkspaceRepairDraftIT.java:165–265,329–354`: preview is no-write; confirmation applies the exact ID set and dimensions, undo names its action, and failed/stale saves leave last durable protection exact and block solve. | PASS |
| G6 | `WorkspaceBrowserIT.java:813–839`, `styles.css:370–386`: task remains wide beneath stacked inspector at 1279/701; 700/390 have no repair mutation controls. | PASS |
| G7 | `WorkspaceBrowserIT.java:660–859`: one verifier-checked 1,000-lesson browser journey exercises the normative teacher, room, no-effect, conflict, individual/bulk, refusal, retention, reload, discard and narrow branches with exact storage checks. | PASS |
| Success postcondition | `WorkspaceBrowserIT.java:780–786` proves ready saved Draft, exactly one individual room pin and unchanged accepted Current. | PASS |
| Minimal guarantee | `WorkspaceBrowserIT.java:670–678,702–707,717–750,760–783,819–850`; `WorkspaceRepairDraftIT.java:140–150,276–289,329–354` prove invalid, cancelled, conflicting, stale and failed actions retain authoritative state and start no ineligible run. | PASS |
| Relations | UC-1's approved 1,000-ID accepted canvas is consumed in `WorkspaceBrowserIT.java:664–669`; UC-1's setup at 5a precedes the production UC-2 stage action at `:670–684`. Shared accepted/focus browser regressions remain green. | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:40–153,377–399` reuses `GET /api/workspace`, accepted model and existing renderer/state modules. The diff adds no route, persistence field, dependency or prototype data. Exact accepted lesson set stays 1,000 in Draft. | PASS |
| RULE-2 | `inspection-state.js:1–104`, `app.js:120–151,393–411`, `WorkspaceBrowserIT.java:788–806,840–845`: one ephemeral state owner, per-mode task choice, default open Draft on reload, representable context retained and no presentation mutation. | PASS |
| RULE-3 | `WorkspaceRepairDraftIT.java:129–160,276–354` exercises invalid, stale, conflicting and failed-save HTTP transitions with exact accepted/Draft/version and absent run/proposal. No service lifecycle code changed. | PASS |
| RULE-5 | `app.js:377–462,700–711`, `styles.css:331–378`, `WorkspaceBrowserIT.java:689–693,807–816,863–899`: inspector retains concise lesson details; task owns picker, conflicts, pins, bulk and decisions; viewport geometry and reachability pass. | PASS |
| RULE-7 | `app.js:439–462,679–684,700–711`, `WorkspaceBrowserIT.java:679–716,1877–1880`, shared invalid-import/proposal browser tests: authoritative names/IDs and accepted availability, safe escaping, exact policy provenance and refusal of unmappable accepted input. | PASS |
| RULE-8 | `WorkspaceBrowserIT.java:670–786,819–858`, `WorkspaceRepairDraftIT.java:129–160,165–354`: setup before write, Draft decoration, conflict/protection/readiness, exact preview-confirm-undo and failure/refusal, and no narrow mutation. | PASS |
| RULE-12 | Diff changes no server route or security configuration. `WorkspaceImportIT` hostile Host/Origin/CSRF/`If-Match` and denied-route tests plus `WorkspaceBrowserIT.java:825–839` remain green; no school document is stored in browser storage. | PASS |
| RULE-13 | New labels live in `messages.js:76–82,191–209`; native buttons, checkboxes, selects and disclosures, textual conflict/pin/status cues, existing Utilities and initial journey pass the full browser regression. | PASS |
| RULE-14 | `WorkspaceBrowserIT.java:660–859,3054–3093` builds and independently verifies the normative accepted pair, checks exact 60-ID bulk by value and all viewports, and compares durable state/version. Full final-diff reactor: 26 unit + 84 integration, including 36 browser; 0 failures/errors/skips. Human walkthrough remains separate. | PASS |

## Validation

- Focused commands: `./mvnw -q -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#preparesWideProtectedDraftAtNormativeScaleInRealBrowser+followsAndRefusesWholeSchoolRepairOnVerifiedNormativeSnapshotInRealBrowser+showsFailedRepairEvidenceAndGatedRetryInRealBrowser+inspectsFrozenRepairRunAndRecoversWithoutPublishingInRealBrowser,WorkspaceRepairDraftIT' -Dfailsafe.failIfNoSpecifiedTests=false verify` — 26 unit + 4 browser + 9 Draft integration tests, 0 failures/errors/skips; later exact-ID UC-2 focused run — 26 unit + 1 browser, 0 failures/errors/skips.
- Full relevant suite: `./mvnw -q -pl timetable-workspace -am verify` — 26 unit + 84 integration, including 36 browser; 0 failures/errors/skips on the final diff.
- Working tree impact from tests: no tracked runtime or unrelated file changed. `node --check timetable-workspace/src/main/resources/static/workspace/app.js` and `git diff --check` passed. Screenshots remain under ignored `timetable-workspace/target/workbench-layout/`.
- Runtime evidence: administrator uses packaged local Chrome workbench backed by isolated PostgreSQL, starts from UC-1's exact accepted 1,000-lesson Week, stages protected teacher and room Drafts, navigates a conflict, previews/cancels/confirms/undoes bulk protection, sees forced storage and stale refusals, retains context, reloads/discards, and reads narrow agenda. The accepted baseline remains exact until explicit discard; no run starts from an ineligible Draft.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/{app.js,messages.js,styles.css}`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/{WorkspaceBrowserIT.java,WorkspaceRepairDraftIT.java}`, `spec/timetable-workbench-layout/{status.md,checkpoints/UC-2.md}`.
- Approved UCs regression-tested: shared `timetable-workspace` UC-1–UC-8, accepted inspection UC-1–UC-3, and workbench-layout UC-1 through the full module/browser suite. The separate `timetable-ux-polish` UC-5 administrator gate is still `PENDING_WALKTHROUGH`.

## Notes

No approved deviations. The full run recorded pin-feedback p95 252.5 ms against a 250 ms reference; this check is diagnostic-only and its functional assertions passed. The UC-2 human walkthrough is for converge, not claimed by automation.

READY FOR CONVERGENCE: UC-2
