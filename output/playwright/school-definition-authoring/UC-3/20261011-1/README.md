# UC-3 walkthrough — attempt 1

- Date: 2026-10-11; submission: add37ea; packaged application localhost:18083/workspace/; real headed Chrome, session authoring-loop.
- Isolation: newly created loopback PostgreSQL container school-authoring-loop-20261011, database authoring_loop (24483). Starting EMPTY, version 0; no operator data used.
- Overall: **FAIL**. F-1: after a real two-tab stale refusal, review/retry overwrites an unselected newer row. Saved Tuesday 1 from the second tab reverted from `Newer tab row` to `Tuesday 1` when the first tab retried only its Monday name edit. See [actual failure](stale-recovery.txt).

| Contract/action | Expected | Observed | Result |
|---|---|---|---|
| Main 1–2: open and generate five weekdays/two slots | 10 working rows, zero durable grid changes | Real entry, version 1, saved grid still empty; assertions passed | PASS |
| Main 3–4: adjust each second-slot end and explicit save | Exact 09:00–09:45 and 10:00–10:45 grid | All ten native rows compared by value, version 2, DEFINITION_DRAFT, exact empty source, no Current | PASS |
| Extension 3a: cancel preview | Saved rows/document unchanged; working edits discarded | `Cancelled working row` reverted to `Monday 1`; complete API document unchanged | PASS |
| Extension 2a: invalid grid save/correction | All affected rows blocked, supported values saved | Four exact attributed duplicate-order/overlap issues at version 3; correction clears grid issues at version 4 | PASS |
| G4/form state: resize and unsaved navigation | Desktop editable, narrow read-only; no resize mutation; Stay/Discard work | 1600,1280,390,700 widths, no overflow or narrow mutation controls; widening retains working name; Stay/Discard preserve durable version 4 | PASS |
| Extension 3c/G3: two-tab stale recovery | Refusal preserves working rows and saved data; retry retains unrelated newer row | Refusal correct, but retry from version 5 to 6 loses newer Tuesday name | **FAIL F-1** |

## Evidence

- [Generated snapshot](generated.yml), [read-only preview result](preview.txt), [preview snapshot](preview.yml), [exact saved API result](saved.txt), [saved snapshot](saved.yml).
- [Cancellation and invalid/correction results with full issue population and snapshot](negative-grid.txt), [corrected snapshot](corrected.yml).
- [Responsive assertions](responsive.txt), [restored snapshot](responsive-restored.yml).
- Screenshots inspected: [saved 1600](saved-1600.png), [1280 working form](viewport-1280.png), [390 saved inspection](viewport-390.png), [700](viewport-700.png), [1600 widening](viewport-1600.png), [narrow row](narrow-row.png). Additional [invalid grid](invalid-grid.png) and [stale refusal](stale-refusal.png).
- Restart, database-write failure, transport abort, local security, source/Current timing and explicit availability variants were exercised by the unchanged six-test UC-3 technical suite. These are supporting technical evidence, not claimed browser actions here.
- `generation.txt` records an initial CLI function-syntax error before actions; corrected invocation is generation-pass.txt. This tooling error did not mutate the workspace.
- Cleanup/disposition: second tab closed; main browser/server/disposable database retained only for execution's fix/retest loop. Failed screenshots/logs preserved. No approval and no later UC started.
