# UC-9 walkthrough — attempt 1

Overall **FAIL**, 2026-10-11, submission 8109b2b, packaged application http://localhost:18084/workspace/, real headed Chrome session authoring-loop. Verified isolated database authoring_loop_uc9, container school-authoring-loop-20261011, port 24483. Operator data was not mutated. Initial launch was intercepted read-only when Compose service connections overrode datasource properties; that server was stopped, Compose disabled and the correct disposable database URL confirmed in startup log before Import/Author/Open draft.

Technical prerequisite: `/tmp/uc9-final-verify.log`, BUILD SUCCESS, 21 relevant browser/HTTP journeys plus 26 unit/architecture checks. Packaged build `/tmp/uc9-walkthrough-package.log`. Real UI imported verified `/tmp/uc9-fixture.json`, then opened draft v2; source/Current stored in [entry evidence](occurrence-entry.txt).

| Contract | Action / expected | Observed | Verdict |
|---|---|---|---|
| Main 1–2, G2 | Inspect native values and imported Mixed teacher | All 11 fields, explicit exception, qualifications and scoped policy visible | PASS |
| Extension 3a | Follow Mixed teacher without a shared default; expect refusal without mutation | Explained establish shared teacher, exact v2 state unchanged | PASS [image](mixed-refusal.png) |
| Main 3–5, G4 | Change name, explicit equal teacher, capabilities, empty preference, undesirable periods, both locks; cancel then save | Cancel preserves working values/no mutation; exact 11-field save with persistent provenance and invalid-capability blockers | PASS [top](all-fields-preview-top.png), [bottom](all-fields-preview-bottom.png) |
| G4, extension 3a | Correct capability, inherit preferred rooms, omit locks | Native presence exact, preference exception cleared; blockers resolved | PASS |
| G1 | Reassign single teacher without rule transfer | Only selected lesson changes; policies, teachers/qualifications and science exact | PASS |
| Main 3–6 | Requirement handoff, select both Math members and separate scoped-rule transfer | Exact selected teacher refs/rule changes and unselected teaching fidelity | PASS [image](scoped-transfer-preview.png) |
| Extensions 1a–1b, G2–3 | Invalid/colliding new series; explicit fresh group; detach; incompatible regroup; individual class/subject invalid edits | Refusals non-mutating; identities/names/counts stable; detach keeps lessons; incomplete edits saved with blockers | PASS |
| Harness final correction assertion | Regroup corrected selected lesson and assert no workspace blockers | Selected lesson corrected, but old zero-count Mixed requirement still has a legitimate requirement-context blocker | FAIL F-1 [state](failure-state.txt), [image](failure.png) |

F-1 is a walkthrough setup/expectation error: retained unfinished requirement context is contractual work, not a selected-lesson regression. Repeat full journey on a fresh disposable database, explicitly complete that context through Weekly requirements, retaining all existing assertions. No UC approval inferred. Main command error retained in [main.txt](main.txt). Responsive/stale/transport/reload journey was not reached. Database and server retained for evidence; subsequent attempt uses separate data. Screenshots require inspection before final approval. No useful screenshot for pure JSON fidelity; exact read-only API snapshots supply that evidence.
