# Use-Case Checkpoint: UC-5 - Complete a repair while retaining operational context

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: `3c473ea390d36e8ac0d49264c6a6299d65a8b7e3`
- Submission commit: HEAD at convergence
- Relations verified: Requires UC-1, UC-2, UC-3, UC-4; includes their approved production goals at steps 1–4. External `timetable-inspection-ux` UC-2 is APPROVED.

## Contract Evidence

`B` = `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`. Every browser test uses packaged assets, production HTTP endpoints and isolated PostgreSQL; real kernel verification also validates the generated accepted baseline and feasible successor. The first five rows use the *same* browser session and exact document comparisons after each transition, not stitched modes.

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1, includes UC-1 | `B:2300–2325`: 1,000 IDs and exact lesson-960 fields in accepted Week/Day, focus/return, preserved accepted bundle | PASS |
| Main 2, includes UC-2 | `B:2327–2352`: teacher-16 unavailable Monday, exact direct set, conflicting period pin resolved, protected room on lesson-500, persistent accepted bundle/Draft and retained selection, Day, filters/inspector | PASS |
| Main 3, includes UC-3 | `B:2353–2381`: live 30-second run, Current/Frozen Draft/Solving and same context, no extra process or proposal on cancellation, durable terminal status, then explicit solve with independently verified feasible result through packaged kernel | PASS |
| Main 4, includes UC-4 | `B:2382–2410`: exact old accepted/Draft and verified proposal, stable origin/destination, same-slot ripple, protected unchanged lesson, direct/ripple/unique totals and Current/Draft/Proposal inspection, unchanged document until confirmation; `B:2025–2186` checks exact review values/groups and proposed availability | PASS |
| Main 5 | `B:2408–2425`: explicit acceptance, exact proposed definition/result now accepted, revision advanced, no residual Draft/Proposal/Solving, exact whole-school ID roster and retained representable Day with explanatory clearing of moved lesson | PASS |
| Extension 2a | `B:2329–2348`: blocking conflict prevents process launch and keeps Current, navigable conflict; `B:1268–1308` proves failed durable Draft save refuses solve with no state advance | PASS |
| Extension 3a | `B:2353–2376`: cancellation preserves Draft and Current; `B:1669–1743,1554–1608,1611–1664` browser exercises rejected output at normative scale, no-feasible, transport/restart/interruption, invalid/timeout and exact unchanged baseline/Draft | PASS |
| Extension 4a | `B:2191–2223`: separately generated independently verified normative proposals, revise and discard both return to the exact Draft/old Current without Proposal | PASS |
| Extension 4b | `B:2250–2295`: stale acceptance invalidates to Draft but never Current; `B:1390–1428`: real durable write failure preserves full document/version and retryable Proposal, only explicit successful retry advances Current | PASS |
| Extension 5a | `B:2427–2438`: fresh room-based second Draft from exact accepted successor, direct `[lesson-0]`, empty pins/bulk actions, zero conflicts, no new run | PASS |
| G1 and G2 | `B:2313–2325,2327–2348,2353–2367,2378–2425`: same lesson identity/accepted metadata/time across range/focus/modes; exact state, header and Current authority at each stage | PASS |
| G3 and G4 | `B:2329–2376,2403–2425,1268–1308,1554–1608,2191–2223,2250–2295,1390–1428`: guarded service actions, no false Current or unauthorized write on conflicts, cancellation, rejected/failed run or refused acceptance | PASS |
| G5 and G6 | `B:2300–2439,2025–2186`: 1,000-lesson Week primary, supporting focused/inspector/Day context, full combined review, isolated real-browser/PostgreSQL/packaged process journey, exact accepted/Draft/Proposal and second-parent state | PASS |
| Success postcondition | `B:2408–2438`: complete accepted successor bundle available as Current and parent of another real repair | PASS |
| Minimal guarantee | `B:2329–2376,2403–2407,1268–1308,1554–1608,2191–2223,2250–2295,1390–1428`: exact previous accepted baseline on all failed/non-accepting branches, no false Current | PASS |
| Requires/includes UC-1–UC-4 | `B:2300–2439` consumes each approved production UI/service postcondition in order; full browser and workspace suites rerun, `spec/timetable-ux-polish/convergence/UC-{1,2,3,4}.md` approve prerequisites | PASS |

## Rule Evidence

| Rule | Evidence | Result |
|---|---|---|
| RULE-1 | `app.js:100–111,781–788`; `B:2300–2439`: existing accepted model, packaged assets and `/api/workspace`, no alternate authority/new route/dependency | PASS |
| RULE-2 | `app.js:100–111,145–166`, `messages.js:139–142`, `inspection-state.js:3–41,118–133`; `B:2313–2325,2360–2367,2403–2425`: preserved representable context, explanatory reset, navigation no mutation | PASS |
| RULE-3 | `B:2327–2438,1268–1308,1390–1428,2250–2295`; `WorkspaceRepairPlanningIT.java:90–371`: guarded lifecycle and refusals, exact rollback and retry | PASS |
| RULE-4 | `app.js:781–788`; `B:2327–2376,1268–1308,1669–1743`: same accepted identities, actual pin conflict/clear, durable Draft, frozen Solving without mutation | PASS |
| RULE-5 | `proposal-comparison.js:1–33`, `B:2382–2407,1977–2020,2025–2186`: joined IDs, old/proposed focus, exact direct/ripple/category/group counts, no false unique from paired tiles | PASS |
| RULE-6 | `messages.js:139–142`, `B:2303–2325,2382–2407,2025–2186,161–194,1923–1973`: verified names/revisions, labelled proposed vs accepted availability, escaped fallback, no fabricated verified candidate | PASS |
| RULE-7 | `app.js:781–788`, `messages.js:139–142`, `B:2313–2325,2345–2348,384–481`: existing Day/Week/focus, native controls, inspector collapse/reopen, catalogued notice and narrow regression | PASS |
| RULE-8 | `B:1390–1428,2408–2425,1923–1973`, `WorkspaceImportIT.java:459–545`, `SecurityConfiguration.java:25–43`: no route/security diff, existing Host/Origin/CSRF/If-Match/CSP and narrow read-only checks remain | PASS |
| RULE-9 | `B:2300–2439,2592–2666,2669–2709`: independently verified generated 60-class/100-teacher/100-room/1,000-lesson baseline, identity/field checks, isolated fixture and no tracked runtime-data writes; user-admin feature gate still pending | PASS (automated) |
| RULE-11 | `B:2353–2381,1554–1664,1669–1743`: actual saved Draft and packaged boundary, failed output/no proposal, bounded run/cancel/retry/restart, frozen modes and diagnostics | PASS |
| RULE-12 | `B:2382–2410,2025–2223,2250–2295,1390–1428`: verified proposal after production run, combined overlay/values and explicit/refused decisions at browser/JDBC boundary | PASS |
| RULE-13 | `B:2300–2439` full contiguous actor journey and second repair; `B:1669–1743,2191–2223,2250–2295,1390–1428` alternatives; entire 34-browser suite passed; external prerequisite approved | PASS (automated) |

## Validation

- Focused commands: `./mvnw -pl timetable-workspace -am '-Dit.test=WorkspaceBrowserIT#completesWholeSchoolRepairAndStartsNextFromAcceptedSuccessorInRealBrowser' -Dfailsafe.failIfNoSpecifiedTests=false verify` PASS: 26 unit, 1 browser, 0 failures/errors/skipped; reproducer initially failed at missing Draft pin binding, then at selection retained outside newly accepted Day, and passed after fixes.
- Full relevant suite: `./mvnw -pl timetable-workspace -am verify` PASS: 26 unit + 82 integration (34 browser), 0 failures/errors/skipped. The expected `FlywayFailureIT` checksum-rejection log did not fail the suite. `git diff --check` PASS.
- Working tree impact from tests: `.output.txt` generated and removed; no tracked runtime data changed. Pre-existing `.idea/encodings.xml` remains unrelated and excluded.
- Runtime evidence: automated administrator actions in Chromium against the locally packaged workspace and isolated PostgreSQL, with two successful kernel verifications and real process invocation through the production planner; individual actions and persisted outcomes are asserted at `B:2300–2439`.
- Changed files: `timetable-workspace/src/main/resources/static/workspace/app.js`, `timetable-workspace/src/main/resources/static/workspace/messages.js`, `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceBrowserIT.java`, `spec/timetable-ux-polish/status.md`, `spec/timetable-ux-polish/checkpoints/UC-5.md`.
- Approved UCs regression-tested: `timetable-ux-polish` UC-1/2/3/4 and `timetable-inspection-ux` UC-2 in the full browser suite (34 browser / 82 integration), 0 failures/errors/skipped.

## Notes

The specification's five real administrators from at least three schools have not been recruited or observed; the per-participant six-row feature gate and narrow/desktop human evidence remain for convergence to request. No administrator approval is claimed from automation alone. The expected `FlywayFailureIT` checksum refusal is not an application regression.

READY FOR CONVERGENCE: UC-5