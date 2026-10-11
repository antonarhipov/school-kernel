# UC-5 — Configure teachers

- Base `9208002`; pre-existing dirty files none. Uses approved UC-1 draft lifecycle and fidelity boundaries.
- Implements all five Teacher fields, Unicode-readable unique identity suggestions/override, immutable saved ID, explicit qualification selections including empty, optional availability/undesirability with omitted versus explicit empty distinctions, declared-grid reference controls with reserved membership marked, retained unresolved identifiers and complete ordered blockers.
- Teacher save preserves every other definition tree, source/Current/result/provenance and authoring context through selected-item transactional CAS. No automatic qualification inference, teaching reassignment, policy rewrite or cascade.
- Curator qualification exception is checked against the cohort's configured curator; qualification removal reports ordinary lesson issues. Period locks outside selected availability are blockers; subject permission still governs reserved locks.
- Working inputs survive failure and resizing. Narrow saved inspection has no form controls. Save/Discard/Stay handles both section and item selection. Stale recovery merges untouched fields and requires explicit conflict resolution.
- Shared entity service has been generalized for subjects/teachers; affected UC-4 tests are included. Subject policy recomputation retains teacher-availability lock blockers.
- Destination handoffs: UC-9 explicit teaching reassignment; UC-13 removal; UC-14 restoration. Their guidance is present; destination goals remain outstanding.
- Initial compilation failure retained in `/tmp/uc5-verify.log`. `/tmp/uc5-final-verify.log` passes 26 unit/architecture, all six subject/seven grid/seven school-identity journeys; one teacher journey exposed premature Saved status on pristine submit. Production status fixed for both entity forms; successful-save helpers now require exact durable version advancement. Revision BUILD SUCCESS in `/tmp/uc5-revision-verify.log`: 26 unit/architecture and all 11 teacher/subject HTTP/browser checks pass with exact version advancement. All 14 grid/school journeys pass in the prior run; no relevant grid/school code changed after those passes. Syntax/diff hygiene pass:

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionTeachersBrowserIT,DefinitionSubjectsBrowserIT,SchoolIdentityBrowserIT,DefinitionGridBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

- Standalone real Playwright [PASS ledger](../../../output/playwright/school-definition-authoring/UC-5/20261011-1/README.md), submission 1084c81; exact v40 evidence and inspected screenshots. Automatically APPROVED. Operator data untouched by tests; runtime proof uses disposable PostgreSQL/Chrome.

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionTeachersBrowserIT,DefinitionSubjectsBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false verify
```
