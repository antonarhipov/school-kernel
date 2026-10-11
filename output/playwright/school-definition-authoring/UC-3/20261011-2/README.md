# UC-3 walkthrough — attempt 2

- Overall verdict: **PASS**; F-1 resolved. Date: 2026-10-11; submission a30de35. Real headed Chrome, session authoring-loop; corrected packaged application at http://localhost:18083/workspace/.
- Isolation: fresh EMPTY version 0 in disposable database authoring_loop_r2, loopback PostgreSQL 24483, container school-authoring-loop-20261011. No operator school/database used.

| Contract/action | Expected | Observed | Result |
|---|---|---|---|
| Main 1–2: Create school, confirm, Grid, generate | Exact five weekdays/two slots; no durable generation/preview mutation | Ten working rows; version 1 and empty saved grid remain intact | PASS |
| Main 3–4: individual end-time adjustments and save | Every second period ends 10:45; complete atomic grid | Exact ten rows compared by value; version 2; source {}, DEFINITION_DRAFT, no Current | PASS |
| Extension 3a: cancel preview | Durable grid unchanged; working edit discarded | Complete API equality; original Monday name restored | PASS |
| Extension 2a/G4: invalid save/correction | Every affected row gets blockers; supported work stays durable | Four exact attributed duplicate/overlap issues at v3; correction removes grid issues at v4 | PASS |
| G4: responsive and unsaved choices | 1600/1280 editable; 390/700 saved read-only; no resize mutation | No overflow/mutation controls on narrow; widened unsaved value preserved; Stay and Discard work; durable version stays 4 | PASS |
| Extension 3c/G3 F-1: two-tab stale retry | Refusal preserves durable/working values; retry keeps unrelated newer edits | v5 newer Tuesday name retained at v6 after Monday retry; exact API non-mutation on stale refusal | PASS |
| F-1 conflict boundary | Both same-field values visible; unresolved save refused; explicit newer choice respected | Conflict panel names both sides; API exact while refused; choosing newer saves it at v8 | PASS |
| G1: reservation membership versus omission | Explicit membership saved independently of name/time; default omission restored | Exactly [monday-2] saved, then field omitted; no inferred reservation | PASS |
| Form/postcondition: Save and continue, reload | Explicit save opens Review with new version; saved rows restored | Saved-on-leaving appears durably; reset to original name through UI; reload restores complete final v12 document | PASS |
| Extension 1a/3a: existing-grid additions | Originals preserved, exactly one Saturday working addition; cancel writes nothing | All ten original names retained; 11-row working preview; cancellation leaves complete v12 unchanged | PASS |

## Evidence

- Each named `.txt` file records the exact Playwright code, assertions and observed PASS result. Matching `.yml` files preserve refreshed accessible snapshots: entry, generation-pass, preview, saved, negative-grid, responsive, stale-final and final.
- [Final exact API document](final-workspace.json): v12, ten original rows, optional HH:mm strings intact, reservations omitted, source {}, two incomplete school-identity issues, no Current.
- Inspected [saved 1600](saved-1600.png), [1280 working](viewport-1280.png), [390 inspection](viewport-390.png), [700](viewport-700.png), [1600 restored working](viewport-1600.png), [narrow row](narrow-row.png), [explicit conflict choice](conflict-choice.png), [final saved](final-saved.png). Additional failure/invalid-state images: invalid-grid.png and stale-refusal.png.
- Supporting current technical evidence: 26 unit/architecture and 25 unchanged integration/browser journeys passed on the corrected code; the new two-tab regression passed after waiting for actual async refresh. Real storage/transport/CSRF/lifecycle refusals, availability population, accepted Current/export/timing and independent restart evidence remain in DefinitionGridBrowserIT and logs /tmp/uc3-f1-verify.log and /tmp/uc3-f1-focused.log. These are not claimed interactive steps in this attempt.
- Expected stale-request HTTP errors are separated from product findings; no unexpected browser exception was observed. Attempt 1 remains preserved with its FAIL verdict. Its F-1 is closed by the complete passing rerun.
- Cross-UC period deletion remains a safe refusal/guidance handoff to UC-13, not an assertion that UC-13 is complete. Its full resolution journey must close that handoff before feature completion.
- Disposition: second tab closed; main browser and agent-owned server/database retained for the immediately following authorized UC loop. Only disposable data changed. Approval is automatic under the user-updated execute/Playwright policy.
