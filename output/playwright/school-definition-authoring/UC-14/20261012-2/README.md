# UC-14 walkthrough — PASS

2026-10-12; submission `7eaef7a`. Real Chromium via cached Playwright CLI at `http://localhost:18091/workspace/`, stable copied production JAR `/tmp/uc14-workspace.jar`. Startup verified disposable PostgreSQL `authoring_loop_uc14_r2` at localhost:24483; Compose disabled. Operator data untouched. EMPTY v0 → imported exact catalog9 synthetic source v1 → authoring v2. Source includes three lessons and an explicitly mixed Math teacher population. No Current is invented.

| Contract | Action / expected outcome | Observed | Evidence |
|---|---|---|---|
| Preconditions, G4 | Import and open draft with exact source | PASS v0→1→2 | [entry state/snapshot](entry.json), [entry image](entry.png) |
| Steps 1–4, G1 | Author school title and teacher name; preview source school title, cancel then save | PASS v4 unchanged by preview/cancel; v5 restores only title, authored teacher retained | [exact main values](main.json), [name preview](name-preview.png) |
| Extension 2a, G3 | Remove mon3 explicitly; restore whole source Alex with original mon3 undesirability | PASS v7 restores teacher identity/name/literal values, period stays deleted; blocker saved without adjustment | [contradiction preview](teacher-source-contradiction-preview.png), [saved blocker](restored-with-blocker.png) |
| G1/G3 | Explicitly restore deleted source mon3 | PASS v8 source identity/order/name returns; root reservedPeriodIds remains [] from prior selected removal; reservation removed; teacher link resolves | [main exact values](main.json) |
| G1/G3 | Explicitly cancel Science occurrence, then explicitly restore that source item | PASS v9→10 exact identity/name/membership/constraints; unrelated root cancellation retained | [main](main.json) |
| Selected settings | Change teacher-gap8 and nonpreferred-room4; restore only teacher-gap | PASS v13 source weight2 returns, weight4 retained | [main](main.json) |
| Source common context | Restore source Math teacher default | PASS v14 native definition exact; source mixed teacherId remains no default with both individual exceptions | [main](main.json), [final main](main-final.png) |
| Field presence | Save Alex availability []; restore only source availablePeriodIds | PASS v15→16 restores omission, preserving other fields | [recovery](recovery.json), [omitted-field preview](omitted-field-preview.png) |
| Extension 3a | Second tab saves weight5 v17; old preview confirm refused | PASS whole newer snapshot exact, old save disabled and refreshed review required | [stale refusal](stale-refused.png), [recovery](recovery.json) |
| Extension 3a | Refresh, preview; abort exact restoration save route | PASS whole newer snapshot unchanged; choices/modal retained | [transport refusal](transport-refused.png) |
| G4/presentation | Inspect 1600/1280/390/700 CSS px | PASS no horizontal overflow; narrow read-only and Unsaved selection retained, zero durable changes | [1600](responsive-1600.png), [1280](responsive-1280.png), [390](responsive-390.png), [700](responsive-700.png) |
| Unsaved navigation | Stay then Save-and-continue, separately confirm source preview | PASS scope retained; explicit atomic v18, newer unrelated weight5 retained; Review shown | [recovery](recovery.json) |
| G4 | Reload exact saved state | PASS whole v18 snapshot exact; immutable source/initialDefinition exact throughout | [recovery](recovery.json), [final saved](final-saved.png) |
| Step 2 / reading | Expand restored native values, cancel and return Review | PASS entire before/after snapshot unchanged, expanded values scroll within bounded modal | [expanded preview output](expanded-preview.txt), [image](expanded-restored-values.png) |

All 14 screenshots inspected (contact sheet plus original expanded modal). English labels, literal IDs, usable internal scroll, wrapped revision strings and no horizontal overflow observed. Exact values, snapshots and step scripts/output are adjacent. No extra screenshot is useful for read-only JSON equivalence assertions; saved values are retained above.

[Technical checks](technical-checks.log): seven restoration journeys and 27 unit/architecture PASS. Covers catalog1→9 effective source defaults (three compensating zeros and source teacher-gap2 retained), revision-consistent accepted source temporary-pin expiry/persistent-lock restoration and exact Current, unselected equal-valued exception intent, source-only access, invalid token/stale/state guards, actual rejected PostgreSQL update non-mutation, fresh-process restart, context restoration without member resurrection. Infrastructure storage/restart are technical evidence, not standalone UI clicks. [Grouped/occurrence regressions](regression-checks.log): nine journeys plus 27 checks PASS. Seven removal journeys also PASS in `/tmp/uc14-revision-verify.log` (that log has a corrected restoration-fixture assertion failure; it is not claimed globally green).

Attempt 1 [FAIL retained](../20261012-1/README.md): incorrect harness assumption of common teacher for mixed source. Corrected to exact no-default/mixed/exception source assertion; this complete repeat PASS. No product requirement weakened. Earlier build overlap caused a class-loading/server startup failure; stable JAR and sequential checks resolved it. Source restoration handoffs now implemented from every editor; whole-draft discard explanation remains the UC-19 destination.

Overall PASS. Browser retained on saved Review. Two-tab recovery page closed. Server PID8744 and disposable database retained; first-attempt server PID8158/database also retained as failed evidence.
