# UC-2 automated Playwright walkthrough

Date: 2026-10-11. Result: **PASS — all six steps**.

Real headed Chrome session `uc2-walkthrough`, driven with the Playwright CLI against the corrected packaged application at http://localhost:18082/workspace/. Only disposable PostgreSQL database `authoring_uc2` in container `school-authoring-uc2-walkthrough-20261011` (loopback 24482) was used. The operator workspace was not changed.

| Step | Observed result | Result |
|---|---|---|
| 1. Create identity | Opened through Create school → Open draft. `Õäš Kool` suggested `oas-kool`. Invalid `INVALID ID` was refused with working values retained. Advanced override `Demo.ID_9` saved at version 2 and became read-only. | PASS |
| 2. Rename/reload | Saved `Õpetajate Kool`, reloaded and returned from Review to School. Exact name restored; identifier unchanged. Draft, Catalog 9 and No Current remained visible. | PASS |
| 3. Unsaved navigation | Stay retained `Stay working name`; Discard restored saved content; Save and continue committed `Saved on leaving` and opened Review. | PASS |
| 4. Blank name/correction | Empty name saved at version 5 with one attributed blocking displayName issue. Correcting to `Õpetajate Kool` saved at version 6 and cleared it. | PASS |
| 5. Responsive retention | 1280×800 and 1600×900 editing controls visible. At 390×844 and 700×844, saved content was read-only, editing controls absent and page width equalled viewport. Widening restored unsaved `Working resize name`; persisted version stayed 6. | PASS |
| 6. Two-tab stale recovery | Second tab saved `Other tab saved` at version 7. First-tab save refused and retained `Working resize name`; durable name/version stayed unchanged. Review newer saved draft displayed the newer heading while retaining working input. Explicit retry saved at version 8; reload restored exact name and ID. Source document remained `{}`, issues empty, and no Current existed. | PASS |

## Captured evidence

- [Desktop 1280](desktop-1280.png), [mobile 390](mobile-390.png), [desktop 1600 after widening](desktop-1600.png).
- [Stale-save refusal](stale-save.png) and [successful explicit retry](final-saved.png).
- Accessible snapshots: [stale refusal](stale-save.yml), [review newer](review-newer.yml), [final saved](final-saved.yml), [final reload](final-reloaded.yml).
- [Final durable API document](final-workspace.json): DEFINITION_DRAFT, version 8, `Demo.ID_9`, `Working resize name`, catalog 9, empty source and issues, no accepted timetable.

Desktop, mobile and final saved screenshots were visually inspected. The expected stale-save HTTP 409 produced a browser console error; recovery succeeded. Its inherited message says “before importing” although the action is an identity save; the no-commit explanation and recovery control are accurate. No functional defect was found.

This records the requested automated walkthrough, not administrator confirmation. Existing specification UC-1 G7 and converge approval requirements remain applicable. No subsequent UC was started. Application remains available for inspection; agent-owned browser session closed after capture. No code changed and the already-passing 26 unit/architecture + 19 integration/browser checks were not repeated.
