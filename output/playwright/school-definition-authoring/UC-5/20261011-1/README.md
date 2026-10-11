# UC-5 walkthrough — attempt 1

- Overall verdict **PASS**. Date 2026-10-11; submission 1084c81; real headed Chrome session authoring-loop and packaged application http://localhost:18083/workspace/.
- Isolation: disposable authoring_loop_r2 database, loopback PostgreSQL 24483, container school-authoring-loop-20261011. Starting DEFINITION_DRAFT v25, two subjects and ten periods from previous approved journeys, no teachers, source {}, no Current. Operator data untouched.

| Contract/action | Expected | Observed | Result |
|---|---|---|---|
| Main 1–3: create teacher and all five fields | Readable ID, override, explicit qualifications/availability/preferences | oas-teacher suggestion; Teacher.ID_9 saves exact two qualifications, two available periods and one undesirable period at v26 | PASS |
| Extension 3d: invalid identity | Durable unchanged, unsaved working values retained | INVALID ID refused, entire v25 API equal, original name retained | PASS |
| Extension 3a: invalid references | Supported invalid work saved with all related blockers | v27 retains blank name and exact arrays; six teacher blockers cover invalid, duplicate and unresolved references | PASS |
| Form model: cancel/correct | Cancel writes nothing; corrections remove blockers | Complete API equality on cancel; correction removes all teacher issues, retains two unrelated school issues | PASS |
| G1/G2: omitted versus explicit empty | Omitted availability means all; empty means none; preferences separate | Omitted fields at v28; explicit [] at v29; omitted again v30; qualification selection exact throughout | PASS |
| UC-1 presentation: four widths | Desktop editable, 390/700 saved read-only; no overflow/mutation | Four-width assertions and inspected screenshots; working name retained on widening and never labelled saved on narrow | PASS |
| Form model: Stay/Discard | Correct working/durable behavior | Stay retains working name; Discard restores saved Alex; whole API unchanged | PASS |
| Extension 3d: real competing-tab save | Refusal exact, untouched newer array survives retry | v31 newer undesirable array retained at v32 name retry; whole API equality on stale refusal | PASS |
| Form model: same-field conflicts | Both sides visible; explicit choice before save | v33 reviewed without mutation, save disabled; Use newer and Keep reviewed both save their chosen side | PASS |
| Extension 3d: injected transport fault | No durable mutation, work retained, explicit retry | Actual form request aborted by Playwright route; alert and retained Alex; complete API equality; Save and continue retries successfully | PASS |
| Main 1/form model: collision/Add/selection | Collision refuses; new teacher stays new; selected save retains change | Duplicate Teacher.ID_9 refusal exact; Blair created; selection-save preserves its rename, then corrected through form | PASS |
| Postcondition/G3: reload | Exact durable teacher data, other collections/source/Current intact | Entire v40 API equal across reload; exact Alex and Blair, source {}, no Current | PASS |

## Evidence

- [Main](main.txt), [invalid/default/responsive](negative-responsive.txt), [stale/conflicts](stale.txt), [transport/selection/reload](final.txt) retain actual Playwright code, assertions and PASS outcomes. Matching `.yml` accessible snapshots and screenshots are stored beside this ledger.
- [Final exact document](final-workspace.json): v40, Alex / Teacher.ID_9 with qualifications [Math.ID_9, science], Blair / blair with explicit empty qualifications; optional availability/preferences omitted; original subjects/periods exact, source {}, no Current, two school-identity blockers.
- Screenshots inspected: [1600 saved](saved-1600.png), [1280 working](viewport-1280.png), [390 inspection](viewport-390.png), [700 inspection](viewport-700.png), [conflict choice](conflict-choice.png), [invalid saved](invalid-saved.png), [transport refusal](transport-refusal.png), [final saved](final-saved.png). No page overflow, overlapping/clipped controls or unsafe name rendering observed. Fields remain reachable by vertical scrolling.
- Fresh technical support: all five teacher and six subject journeys plus 26 unit/architecture checks pass on corrected saving-status code (`/tmp/uc5-revision-verify.log`, BUILD SUCCESS). All 14 unchanged grid/school journeys pass in `/tmp/uc5-final-verify.log`; its earlier status finding remains recorded. Compilation failure retained `/tmp/uc5-verify.log`.
- Technical browser fixture variants cover reservations marked in the declared grid, qualification removal/affected lessons, curator exception/violations, persistent locks outside availability, exact initial/accepted source and Current fidelity, invalid array types, injected storage failure, all-state/CSRF refusals and independent process restart. These are not claimed interactive actions in this attempt; infrastructure faults have no additional meaningful screenshot here. The interactive transport fault above was deliberately injected, not an unplanned network outage.
- Destination handoffs remain UC-9 teaching reassignment, UC-13 guided removal, UC-14 restoration. Their safe explanation is present; destination goals are not yet complete.
- No unexpected browser exception observed; expected identity/collision/stale/transport errors are successful negative paths. Second tab closed; main browser, server and disposable database retained for the next authorized UC. Automatic approval follows the execute/Playwright policy.
