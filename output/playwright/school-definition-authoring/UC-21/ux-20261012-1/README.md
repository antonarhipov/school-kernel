# UC-21 — PASS / automatically APPROVED

2026-10-12. Base `b1fd27daad4b7866a6e50ba9f18273b7ea1d5bc2`; exact [submission](submission.patch). Packaged `/tmp/authoring-ux.jar` SHA256 `bfe8620facdd3a196414714bed8b82262b8954876d587d3059498f2ce3591163`. Real Chrome via installed Playwright CLI; isolated http://127.0.0.1:18112/workspace/ and disposable database `authoring_ux_20261012` in existing `school-authoring-loop-20261011`. Operator235 copied read-only, with three added subjects for duplicate-name/diacritic fixtures; operator process, browser and data untouched.

| Contract | Action and expected result | Observed | Verdict |
|---|---|---|---|
| Main1–3, G2 | Search AJALUGU and select identical names by ID | Both fixture IDs distinct, correct heading and form | PASS |
| Extension2a, G1 | Filter open unsaved form to no matches | Working literal name intact; hidden-selection notice; complete snapshot unchanged | PASS |
| Extension3a | Switch records, Stay, then Save and continue | Stay retained identity/name; explicit save235→236 preserved exact name and opened requested record; search retained | PASS |
| G2–3 | Diacritic/case search, keyboard Down/Enter, pagination | Õäš found by OAS; keyboard selected exact ID; at most25 records; page2 retained across Teachers/Subjects round trip | PASS |
| Main2, G2 | Qualified-subject, capability and issue filters; natural class order | All returned teacher/room IDs satisfy predicate; no issues returns no rows; 1A…9C natural order | PASS |
| Main3–4, G4 | Teachers/Classes/Rooms at1600/1280; retain work across390 inspection | Correct IDs and selected rows; no page overflow; narrow Add/forms absent; widening restores unsaved name | PASS |
| G1, extension3b | Narrow filter, cancel edits, reload | Complete saved236 remains exact; acceptedBaseline and sourceDocument equal initial copy | PASS |

[Full journey](journey.js), [values](values.json), [filter assertions](filters.log), and screenshots retained. Inspected duplicate-name,teacher1600,class1280,room390 and sorted-class screenshots; primary identities, selected rows, search and read-only inspection are readable. Original resize capture retained; final form refinements will repeat responsive checks on the changed build. Native page error array empty.

[Technical log](technical.log): Maven verify with `*Test` and DefinitionSubjectsBrowserIT,DefinitionTeachersBrowserIT,DefinitionCohortsBrowserIT,DefinitionRoomsBrowserIT; final BUILD SUCCESS,33 workspace unit/architecture and21 entity browser cases, zero failures/errors. Syntax/package/diff checks PASS. Unchanged source/lifecycle paths consume existing approval evidence. No unavailable handoff.

Browser `authoring-ux`, server18112/native session83010 and disposable data retained for UC-22/23. Existing tracked approvals and unrelated untracked skill preserved. Approval is automatic browser evidence, not human confirmation.
