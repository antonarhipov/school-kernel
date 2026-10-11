# UC-11 walkthrough attempt 2 — PASS

Submission 1fc3328, 2026-10-11. Chrome authoring-loop at http://localhost:18087/workspace/, packaged app on disposable authoring_loop_uc11_r2 in school-authoring-loop-20261011, loopback 24483. Compose disabled; actual datasource verified in /tmp/uc11-walkthrough-server-r2.log. Exact EMPTY v0 asserted before import; Initial draft awaited and verified (three occurrences, no overrides), then exact initial source opened at v2. Operator data and attempt-1 database preserved.

Technical prerequisites: /tmp/uc11-revision-verify.log BUILD SUCCESS, three preferences/seven School journeys and 27 unit/architecture checks. Initial /tmp/uc11-verify.log supplies seven grid/four room-policy passes. Initial navigation regression fixed, fixture corrected explicitly, failed logs retained. F-1 entry synchronization repaired and exact source precondition asserted before the complete repeat.

| Contract | Action and expected outcome | Observed / verdict / evidence |
|---|---|---|
| Main 1–2 | Import/open exact source, School Advanced Preferences handoff; inspect complete table/defaults/meanings | PASS [entry](entry-evidence.json), [default table](default-table.png); exactly seven ordinary rows, hard/stability fixed |
| Main 3–4, G1 | For every ID save zero, maximum 1000000, then default | PASS [main](main-evidence.json) stages; each explicit save advances once, zero disabled, default removes selected override |
| Extension 3a | Save unfinished, fractional, negative, above-limit and empty weights | PASS raw string/number values durable with blockers, [image](invalid-saved.png), main invalid states |
| Main 4, G1 | Correct to explicit one, then choose default | PASS explicit one remains distinct from omission; blocker clears, [image](explicit-one.png) |
| G2–3 | Compare native values/source/Current before and after | PASS every native field except overrides exact, source/initialDefinition exact, no Current invented; [main final](main-final.png) |
| UC-1/form model | Resize while unsaved at 1600, 1280, 390, 700 | PASS no overflow/write, narrow saved-only form absent, widen retains working 5; [1600](responsive-1600.png), [1280](responsive-1280.png), [390](responsive-390.png), [700](responsive-700.png) |
| Form model | Select another preference and Stay | PASS fixed selected identity and working 5 retained, [recovery](recovery-evidence.json) |
| Extension 3d | Abort transport, retry using Save and continue | PASS exact no-write refusal and retained input; retry persists 5 and reaches Review, [image](transport-refused.png) |
| Extension 3d | Two tabs change unrelated preferences, stale refusal/refresh/retry | PASS stale non-mutation; teacher gap 6 and newer room preference 4 both retained, [image](stale-refused.png), recovery newer/merged |
| Extension 3d | Two tabs change same preference, review explicit conflict | PASS save disabled until reviewed choice; no implicit overwrite, latest 7/working 8 visible, then explicit 8 saves; [image](conflict-review.png) |
| Form model, G3 | Discard unsaved 9, reload and compare complete workspace | PASS exact v35 retained, teacher gap 8/nonpreferred room 4, other overrides absent; [final document](final-workspace.json), [snapshot](final-snapshot.txt), [image](final-saved.png) |

All 12 screenshots inspected via complete contact sheets: saved/default/invalid states, responsive wrapping, recovery and explicit conflict controls have no clipped/overflow finding. Vertical scrolling is expected. Exact JSON comparisons have no useful screenshot; API evidence provides the durable values. Main and recovery documents preserve every saved revision. Seven ordinary IDs are native; no hard/stability changes.

Fresh technical variants, not claimed as browser clicks: accepted source/Current fidelity, omitted collection stays absent when choosing default before any override, typed boolean invalid save, selected duplicate replacement with unrelated unsupported row retained/diagnosed, unavailable/unknown/hard/stability refusals, all non-authoring states, CSRF, database rejection, restart, kernel coefficient/signed-long overflow boundary. Fixture verification /tmp/uc11-fixture-verification.json VERIFIED. No unresolved finding. UC-12/14 are destination handoffs. Browser/server/databases retained for continuing; automatic approval is not human confirmation.
