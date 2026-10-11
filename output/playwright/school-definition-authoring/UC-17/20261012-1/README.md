# UC-17 walkthrough attempt 1 — FAIL

2026-10-12, base e86b5d1 plus UC-17 working diff. Real headed Chrome session `authoring-uc17-r1`, app port18105, `/tmp/uc17-workspace-r1.jar`, isolated database `authoring_loop_uc17_r1`; startup log confirms datasource. Operator data untouched.

| Contract | Action / expected | Observed | Verdict / evidence |
|---|---|---|---|
| G6 source | Import exact SHA-checked MVK definition + real 60-second FEASIBLE baseline; independent packaged verify | 520 accepted assignments, source exact | PASS [import](02-import.txt), [source](01-source.png); `/tmp/uc17-mvk-plan.log`, `/tmp/uc17-mvk-baseline-verification.json` |
| G1/G6 | Forms: 6c homeRoom a210, cancel only 6c.vene-keel.02, mon-1 08:50:00–09:35:00 | Entire saved definition equals source plus exactly three normative edits and lineage; 519 lessons, other optional fields/policies exact | PASS [class](02-home-room.png), [cancellation](03-cancellation-preview.png), [grid](04-grid-time.png), [review](05-saved-review.png) |
| G1/G2 | Finish whole seed; Current exact | Exact retained authoring/seed/source asserted | PASS [seed](06-seeded-repair.png) |
| G7 | Narrow390 read-only / overflow | No solve action, no horizontal page overflow | PASS [narrow](07-narrow-seed.png) |
| UC-18 prerequisite / G6 | Cancel confirmation, then explicitly abandon repair; expect exact authoring restored | Cancel preserved data; confirmed request denied403 by missing security route | FAIL [confirmation](08-abandon-confirmation.png), [failure](failure.png), [snapshot](failure-snapshot.txt), [run error](03-primary.txt) |

Images inspected: failure screenshot directly; other images retained for later contact-sheet inspection. No full approval or acceptance claimed. CLI callback signature error [01-import](01-import.txt) preceded corrected import and caused no mutation. Focused5 service tests pass;6 Finish browser tests pass. New seeded tests detected missing route and an incorrect test expectation (run creation is202), both corrected in the next submission. Earlier logs retained `/tmp/uc17-checks-r1.log`.

Browser retained; failed database and jar retained. Server will be stopped after next build. Full journey must repeat on corrected build. Remaining steps (restoration, real repair, proposal review, acceptance, variants) were not reached.
