# UC-4 walkthrough — attempt 2

- Overall verdict: **PASS**; F-1 resolved. Date 2026-10-11; submission dd395c4. Real headed Chrome session authoring-loop; corrected packaged application http://localhost:18083/workspace/.
- Isolation: retained disposable authoring_loop_r2 PostgreSQL database on loopback 24483, container school-authoring-loop-20261011. Starting DEFINITION_DRAFT v12, ten previously saved UC-3 periods, empty subjects, source {}, no Current. No operator data touched.

| Contract/action | Expected | Observed | Result |
|---|---|---|---|
| Main 1–3/G1/F-1: enter Subjects and all seven controls | Accessible fields, readable Unicode ID suggestion, exact explicit values | Both numeric textboxes named; oas-mathematics suggestion; overridden Math.ID_9 saves all seven values exactly at v13 | PASS |
| Extension 3c: invalid identity | Working values retained, durable document unchanged | INVALID ID refusal, complete API equality to v12, name retained | PASS |
| Extension 3a: invalid supported save | Invalid values durable, all related blockers | Blank name, numeric zero/string saved at v14; four attributed subject blockers including reserved cap without permission | PASS |
| G1: default versus explicit values | Default omission restored; name/ID exact | All five optional fields omitted at v15, only two unrelated school-identity blockers remain | PASS |
| Form model: cancel, Stay, Discard | No durable mutation, correct retained/discarded inputs | Complete API equality, saved name restored, Stay retains edits | PASS |
| UC-1 presentation/G3: four widths | 1600/1280 editable, 390/700 saved read-only, no overflow | Screenshots inspected; no overflow or narrow form; unsaved name retained on widening but not shown as saved | PASS |
| Extension 3c: real two-tab stale save | Refusal preserves durable and working data, retry merges untouched fields | v16 dayEdgeOnly true survives v17 name retry; whole API equality on refusal | PASS |
| Form model: same-field conflicts | Both sides visible, save disabled until explicit choice | v18 conflict unchanged during review; Use newer saves newer name; another conflict Keep reviewed saves working name | PASS |
| Form model/Main 1: Save and continue, Add, selection | New saved revision reaches destination; no reselection/lost save | Review gets saved name; Add presents new form; saving through selection preserves Science rename | PASS |
| Identity extension: collision | No committed change, inputs retained | Duplicate Math.ID_9 refused with complete API equality; corrected science saves | PASS |
| Postcondition: reload | Exact persisted values, source/Current/grid unchanged | Complete v25 API equality across reload, exact two subjects and original ten periods | PASS |

## Evidence and boundaries

- [Main](main.txt), [invalid/default/responsive](negative-responsive.txt), [stale/conflicts](stale.txt), [final selection/reload](final.txt): actual Playwright code, assertions and PASS results. Matching `.yml` snapshots retained beside screenshots.
- [Final exact document](final-workspace.json): version 25, subjects `Math.ID_9 / Mathematics` and `science / Science`, all optional subject fields omitted, original periods exact, source {}, DEFINITION_DRAFT, no Current, two school identity blockers.
- Screenshots inspected: [saved 1600](saved-1600.png), [working 1280](viewport-1280.png), [390 inspection](viewport-390.png), [700 inspection](viewport-700.png), [conflict choice](conflict-choice.png), [invalid saved](invalid-saved.png), [identity refusal](identity-refusal.png), [final saved](final-saved.png). The four-width assertions also cover restored working 1600. No cropped text, overlap or page overflow found.
- Applicable fresh technical checks: six UC-4 real HTTP/browser tests plus 26 unit/architecture checks; 24 existing authoring regressions pass. F-1 focused revision adds accessible-name assertion and repeats save/reload/restart. Logs `/tmp/uc4-verify.log` (initial failures preserved), `/tmp/uc4-revision-verify.log` and `/tmp/uc4-label-verify.log` (BUILD SUCCESS).
- Catalog-unavailable controls, curator qualification exception and violations, linked lessons/qualifications/room policies, accepted/initial source fidelity, all-state/CSRF refusals, injected storage and transport failures, and independent application restart are proven by the technical browser checks. These fixture/process-fault checks are not claimed as interactive clicks in this attempt; screenshots of an infrastructure fault would add no new evidence. Exact source/Current assertions are in those tests.
- UC-12 catalog, UC-13 guided removal and UC-14 restoration remain outstanding destination handoffs. Their guidance/refusal is present; destination behavior is not claimed complete.
- No unexpected browser exception was observed. Expected identity/collision/stale HTTP refusals are successful negative paths. Attempt 1 remains FAIL with retained evidence; F-1 is closed here.
- Disposition: second tab closed; main browser, server and disposable database retained for the authorized next UC. Approval is automatic under the updated execute/Playwright policy.
