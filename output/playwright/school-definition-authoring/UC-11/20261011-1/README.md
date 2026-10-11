# UC-11 walkthrough attempt 1 — FAIL

Submission 1fc3328, 2026-10-11, Chrome authoring-loop, http://localhost:18087/workspace/, disposable authoring_loop_uc11; Compose disabled and actual datasource verified in /tmp/uc11-walkthrough-server-final.log. Technical prerequisites pass in /tmp/uc11-revision-verify.log, grid/policy regressions pass in /tmp/uc11-verify.log.

F-1: entry harness awaited #open-authoring, which already exists as Create school in EMPTY. It clicked before the import completed, opening an EMPTY-source draft rather than the intended verified nonempty fixture. Read-only API evidence in entry.txt caught this incorrect starting state. Main steps in main.txt passed for that blank draft, but do not satisfy the intended complete source/native fidelity journey. Overall FAIL; no product failure inferred. Four screenshots retained; not used to claim visual acceptance. No recovery/approval performed. Disposable database retained at v29; operator data untouched.

Fix the entry wait to observe Initial draft and assert exact imported definition/source before opening. Repeat the full UC-11 journey on a fresh isolated database. Preserve this failed attempt.
