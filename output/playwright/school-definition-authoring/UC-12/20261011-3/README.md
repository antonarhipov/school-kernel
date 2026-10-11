# UC-12 walkthrough attempt 3 — PASS

Submission dfabfd8 (production unchanged by the later navigation assertion). 2026-10-11, Chrome authoring-loop, http://localhost:18088/workspace/, packaged app on isolated authoring_loop_uc12_r3 in school-authoring-loop-20261011, loopback 24483. Compose disabled and actual datasource verified in /tmp/uc12-walkthrough-server-r3.log. EMPTY v0 and imported catalog-1/two-occurrence source asserted, exact native source opened at v2. Operator and prior attempt databases untouched.

Technical prerequisites: /tmp/uc12-verify.log BUILD SUCCESS, 24 catalog/preferences/authoring/subjects/policy journeys; /tmp/uc12-cohort-verify.log BUILD SUCCESS, six class journeys; 27 unit/architecture. Recovery revision /tmp/uc12-revision-verify.log BUILD SUCCESS, four catalog journeys with failure controls visible after screenshot. All catalog crossings 1–8, exact defaults/compensations/explicit choices, unsupported/downgrade/oversized targets, latest no-op, stale token/version, CSRF/all non-authoring states, database rejection, exact accepted Current and restart pass. These infrastructure/API variants are not claimed as standalone browser clicks.

| Contract | Action and expected outcome | Observed / verdict / evidence |
|---|---|---|
| Main 1 | Real import/open, follow unavailable hard room policy setting into School | PASS exact catalog-1 source, no unavailable policy form, actual upgrade handoff; [entry](entry-evidence.json) |
| UC-1 form model | Unsaved school name: preview, Stay, then explicitly discard working name | PASS original working name retained on Stay; no preview until explicit choice, no durable mutation |
| Main 2, G1 | Preview catalogs, newly enabled settings, changed omitted defaults and all compensations | PASS catalog 1→9; eight enabled groups, three changed-default explanations, three zero weights/class No limit; [top](preview-top.png), [bottom](preview-adjustments.png) |
| Extension 3a | Cancel preview | PASS exact v2/document unchanged |
| UC-1 boundary | Inspect at 1600,1280,390,700 | PASS no overflow/write; narrow saved-only controls absent; [1600](responsive-1600.png), [1280](responsive-1280.png), [390](responsive-390.png), [700](responsive-700.png) |
| Extension 2a/3b | Other tab saves teacher-gap 4 after preview; confirm old preview | PASS v3 retained with catalog 1, no partial upgrade; confirmation disabled pending refresh, [stale](stale-refused.png) |
| Extension 3b | Refresh preview; abort confirmation transport | PASS same exact v3/document, failure retained with fresh-preview controls; dialog asserted visible before/after capture, [transport](transport-refused.png) |
| Main 3–4, G1–3 | Refresh/review again and explicitly confirm | PASS one atomic v4 save, catalog 9, existing weight 4, cohort-gap/week-balance/late-start 0, class maxDailyGaps 2147483647; [saved](upgraded-saved.png), main upgraded document |
| G1–3 | Compare unselected fields, source and Current | PASS exact native fields/order/omission aside from selected catalog and required compensations; exact source/initialDefinition, no Current invented, roomAssignments remains omitted |
| Main 4 | Review upgrade and inspect newly available policy form | PASS saved history/compensations visible; no data inferred by inspection, [Review](review-upgrade.png) |
| Extension 4a | Deliberately choose newer cohort-gap default through UC-11 | PASS separate v5 edit removes only that override; other compensations and upgrade history retained |
| G3 | Reload and compare complete state | PASS exact v5, catalog9, teacher-gap4/week-balance0/late-start0, class No limit; [final document](final-workspace.json), [snapshot](final-snapshot.txt), [image](final-saved.png) |

Exact consequential states/versions and assertions are in [main evidence](main-evidence.json). Eleven screenshots inspected: complete preview via top/bottom capture, saved/review/invalid recovery states and all responsive widths. Modal has bounded internal vertical scrolling; controls remain reachable by real actions and retention assertions. Recovery captures show error text; lower controls may require internal scrolling at the captured position. No horizontal clipping or unresolved layout finding. Pure JSON fidelity has no useful screenshot; read-only API state provides exact evidence. Fixture /tmp/uc12-fixture-verification.json VERIFIED.

F-1 fixed: owned dialog closure/bounded layout and strengthened retention assertion pass. F-2 fixed: host script uses supported JSON clone; complete journey/reload now passes. Both earlier attempts retained. Runtime app/Chrome and isolated databases retained for continuing. Approval is automated, not human confirmation; UC-13/14 and later feature goals remain pending.

Final handoff/restart assertion BUILD SUCCESS /tmp/uc12-handoffs-verify.log, actual subject/class/preference/policy navigation reaches School upgrade. Automatically APPROVED on dfabfd8 after complete attempt-3 PASS. Catalog handoffs closed; proceeding UC-13.
