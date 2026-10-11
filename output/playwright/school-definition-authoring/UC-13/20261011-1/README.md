# UC-13 walkthrough — PASS

Submission `caddae3`, 2026-10-11. Real Chromium via cached Playwright CLI, `http://localhost:18089/workspace/`. Repackaged production application; Compose disabled and startup datasource verified as disposable `authoring_loop_uc13` in container `school-authoring-loop-20261011`. Operator school untouched. Start EMPTY v0, imported verified synthetic catalog-9 definition v1, authoring v2. Source fixture has three lessons, two teachers, one class, two rooms, three periods and a literal hard-room policy. No Current is invented.

## Recorded administrator journey

| Contract | Action and expected outcome | Observed result | Evidence |
|---|---|---|---|
| Preconditions / G4 | Import and open authoring; exact recorded source | PASS v0→1→2, sourceDocument equals imported workspace | [entry](entry.json), [image](entry.png) |
| Steps 1–3 / extension unresolved | Inspect Period mon-3; root reservation and teacher undesirability shown; refuse unresolved preview | PASS both references visible; complete API snapshot unchanged | [main](main.json), [refusal](unresolved-dependencies.png) |
| Steps 3–5 / cancel | Explicitly remove both references; preview all edits; cancel, then confirm | PASS preview/cancel nonmutating; v3 has two periods, both selected arrays empty, all lessons exact | [preview top](period-preview-top.png), [bottom](period-preview-bottom.png), [saved](period-saved.png) |
| Explicit replacements / invalid draft | Inspect Room101; refuse unknown replacements, then replace Science preference and last hard-policy target with Room102 | PASS refusal nonmutating; atomic v4, lab constraints retained and blockers exposed, no inferred capabilities | [unknown](unknown-replacement.png), [saved invalid draft](room-removed-invalid-draft.png) |
| G1 | Remove Blair, explicitly reassign imported Math occurrence to Alex | PASS v5, every lesson Alex; Alex qualifications unchanged | [exact values](main.json) |
| Membership / no cascade | Remove Math series by explicitly detaching both members | PASS v6, three exact lessons except selected seriesId removal; context removed | [series](series-detached.png) |
| Explicit cancellation | Remove Science occurrence; refuse until cancellation checkbox selected | PASS refusal exact, only selected lesson cancelled v7 | [main](main.json) |
| Complete references | Remove Science subject and explicitly remove Alex qualification literal | PASS v8, Math teaching retained | [main](main.json) |
| No dependency single removal | Remove Math room policy | PASS v9, lessons unchanged | [main](main.json) |
| Owned teaching / cancellation | Remove class; refuse unresolved, select both surviving Math cancellations | PASS atomic v10, no classes/lessons; all source identity reservations hasSource=true | [main final](main-final.png), [exact state](main.json) |
| No dependency / cancel | Add new unused room through Rooms UI, preview/cancel removal | PASS v11 added capacity0/capabilities[]; preview/cancel unchanged | [unused preview](unused-preview.png), [recovery](recovery.json) |
| Stale extension | Second tab saves teacher-gap weight4 v12, confirm old removal | PASS entire newer snapshot exact; save disabled, refreshed review required | [stale](stale-refused.png) |
| Transport extension | Refresh dependencies, preview, abort exact save route | PASS no mutation; selected choices/error/modal retained | [transport](transport-refused.png) |
| G4 / presentation | Inspect at 1600,1280,390,700 CSS px | PASS no horizontal overflow; narrow has no removal controls, Unsaved retained and saved-only inspection; snapshot unchanged | [1600](responsive-1600.png), [1280](responsive-1280.png), [390](responsive-390.png), [700](responsive-700.png) |
| Unsaved navigation | Stay, then Save and continue and separately confirm complete preview | PASS Stay retains target; explicit v13 removes only unused-room, Review shown, weight4 retained | [recovery](recovery.json) |
| G2 / durability | Reload saved v13 | PASS exact whole snapshot, source/initialDefinition exact, removed new identity hasSource=false | [final](final-saved.png), [exact reload](recovery.json) |

All 17 screenshots inspected using a contact sheet plus original modal/narrow images. Internal dialog scrolling keeps confirmation accessible; no overlapping controls or horizontal overflow observed. Backend-only exact assertions have no additional useful screenshot: values are retained in JSON above. Scripts and complete CLI outputs are adjacent.

## Technical support and disposition

[Technical log](technical-checks.log): seven complete removal journeys, 27 unit/architecture checks, BUILD SUCCESS. Includes saved accepted-source Current/historical fidelity, all normative references and context, last-target refusal/explicit home mode, equal-valued exception regrouping, CSRF/state/token guards, unrelated selections, reserved identity reuse, actual rejected PostgreSQL updates and fresh-process restart. Storage/restart evidence is from technical checks, not claimed as standalone clicks. Sixteen unchanged grid/occurrence/requirement regressions passed in `/tmp/uc13-verify.log`.

Earlier failed technical checks remain in `/tmp/uc13-{verify,revision-verify,final-verify,handoff-verify}.log`: fixture errors, missing occurrence callback and disabled modal-close controls corrected before this first standalone attempt. No walkthrough FAIL occurred. Restoration handoff remains UC-14; removal does not claim restoration is implemented.

Overall PASS. Main browser retained on saved Review; recovery tab closed, older agent-owned UC-12 tabs retained. Server PID6342 and isolated database retained for evidence/follow-on checks.
