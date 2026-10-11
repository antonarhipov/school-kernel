# Use-Case Checkpoint: UC-1 - Open or resume a school-definition draft

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 0fd30998e2b20e79ed61392a2125c73067d2c76e
- Submission commit: HEAD at convergence
- Relations verified: none; UC-1 has no Requires, Includes, or Extends dependencies.
- Technical constraints: spec.md, existing local Host/Origin/CSRF protection, architecture boundaries, versioned PostgreSQL aggregate. No feature rules.md.

## Contract Evidence

The tests below are in `timetable-workspace/src/test/java/org/schoolkernel/workspace/`. `DefinitionAuthoringBrowserIT` uses real Chrome and an isolated PostgreSQL database, with actual HTTP, Flyway, application restarts and packaged verification of exported Current.

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1: request from EMPTY, INITIAL_DRAFT, ACCEPTED_BASELINE | DefinitionAuthoringBrowserIT:15,50,77 | PASS |
| Main 2: source, single-draft lock, required upgrade and expired pins | Browser entry/upgrade modal and exact source assertions, :15,77 | PASS |
| Main 3: confirm required upgrade | :77; unconfirmed HTTP request refuses without write | PASS |
| Main 4: atomically save new/copied definition | :15,50,77,124; full JSON equality, omissions and accepted inputRevision | PASS |
| Main 5: sections, counts, source, saved status and entry section | :15,50; each named section reached in browser, new School and resumed Review | PASS |
| Main 6: section / unchanged Current inspection | :15,77; Current return and no writable timetable controls | PASS |
| Extension 1a: resume without resetting | :15,124; version/document equality, saved diagnostics/context/removed identities/policy locks | PASS |
| Extension 1b: competing lifecycle refusal | :163; every other enum state, response names state, exact document/version unchanged | PASS |
| Extension 2a: create alongside import | :15; visible import before opening, catalog 9, exact empty collections | PASS |
| Extension 2b: unsupported/unmappable source | :183; schema/catalog version, unknown root/nested field, fractional integer; 422 and no mutation | PASS |
| Extension 3a: cancelled required upgrade | :77; real modal Cancel, exact source/version preserved | PASS |
| Extension 4a: storage failure / stale opening | :183,212; real browser storage trigger and stale confirmation, no partial draft | PASS |
| Extension 5a: reload / actual application restart | :15,50; two independently started/closed application contexts, same database and exact saved document | PASS |
| G1: lineage, no assignment-derived defaults, lock provenance | :50,77,124; exact copied definition; only declared opening changes; persistent room lock retained, attempt period lock expired, source/result unchanged | PASS |
| G2: values, optional presence, context, identities and diagnostics | :50 (complete MVK tree plus labelled text/availability variant), :124 (saved context/removed IDs/issues/new policy lock); exact equality | PASS |
| G3: competing actions locked; Current remains inspectable/exportable | :163; all competing POST routes plus DELETE workspace; :77 and SavedDefinitionDraftBrowserIT:81 verify exported source pair | PASS |
| G4: desktop/narrow layout and read-only inspection | :15; 1600x900,1280x800,390x844,700x844; no page horizontal overflow, navigation visible, no mutation inputs; saved document unchanged | PASS |
| G5: English and literal school names/diacritics | :50; literal malicious-looking school name, no img element; English navigation and status | PASS |
| G6: existing local access and untrusted-change denial | :183; wrong Host, hostile Origin, missing CSRF, no school disclosure or document mutation; unchanged production security surface | PASS |
| G7: isolated browser/durable evidence and human approval | Above automated journeys; walkthrough remains required before approval | AUTOMATED PASS; WALKTHROUGH PENDING |
| Success postcondition | :15,50,77,124; durable DEFINITION_DRAFT and navigable saved sections | PASS |
| Minimal guarantee | :77,163,183,212; refused/cancelled/failed openings keep exact document/version and source lifecycle | PASS |

## Rule Evidence

No feature rules apply. Existing architecture tests passed without exemptions. New API routes retain Host/Origin/CSRF checks; authoring lock follows authorization before request decoding. Reset/clear also reject DEFINITION_DRAFT in their atomic SQL predicates. Definition data stays as JSON trees; the workspace does not depend on kernel implementation packages.

## Validation

- Construction build: `./mvnw -pl timetable-workspace -am -DskipTests package`: PASS.
- Focused completed-slice run: `./mvnw -pl timetable-workspace -am -Pworkspace-browser -Dtest=ArchitectureTest,ManifestServiceTest,SafeImportReaderTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify`: BUILD SUCCESS, 16 unit/architecture + 10 browser tests, zero failures/errors/skips. Log `/tmp/school-authoring-corrected.log`.
- Shared Current regression: AcceptedInspectionBrowserIT, 11 tests, zero failures/errors/skips in `/tmp/school-authoring-verify.log`. The first authoring run found two failures (guard ordering and an inconsistent test baseline); both were corrected and all seven authoring tests then passed. Do not label the first aggregate run green.
- JavaScript syntax: `node --check` on app.js and definition-editor.js: PASS.
- `git diff --check`: PASS.
- Shared import/clear/security regression and all workspace unit tests are included in the independent convergence command; the expensive Current browser suite will not be repeated without a new reason.
- Working tree impact from tests: no tracked runtime data or unrelated changes.
- Approved related behavior: accepted Current inspection/export and persisted-draft read compatibility. No authoring UC was previously approved in this checkout.
- Changed files: timetable-workspace/pom.xml; DefinitionDraftService.java; DefinitionDraftAccess.java; ImportService.java; SecurityConfiguration.java; WorkspaceController.java; WorkspaceMutation.java; WorkspaceRepository.java; WorkspaceState.java; static/workspace/app.js; definition-editor.js; styles.css; DefinitionAuthoringBrowserIT.java; SavedDefinitionDraftBrowserIT.java; WorkspaceImportIT.java (updated DEFINITION_DRAFT refusal code expectation); feature status.md and this checkpoint.

## Notes

This slice supplies saved-content inspection/navigation only. Identity/resource editing, save forms, comprehensive change review, finish and authoring discard are dependent UCs and remain NOT_STARTED. There are no unsaved edit forms in UC-1; resizing must leave the durable draft unchanged and will retain working inputs when later editor UCs add them. No runtime school was modified. The full reactor/API/browser suites are not claimed green; validation is scoped to this UC and affected shared boundaries per the user's request to avoid unnecessary slow checks.

READY FOR CONVERGENCE: UC-1

## Administrator-confirmed navigation revision

- Revision base: `d858598fd6d419b29cde395d6799bbbedbe5ca6f`.
- Revision submission: HEAD at convergence; original source/lifecycle submission remains `06c0201`.
- Product decision: administrator confirmed "All good", requested dedicated Teachers/Rooms tabs instead of Resources, and explicitly instructed approval after splitting them. UC-1 main step 5 and the proposal's navigation summary now record that choice.
- Navigation: School, Grid, Subjects, Teachers, Classes, Rooms, Room policies, Weekly requirements, Review. Every previous Resources subsection remains directly reachable.
- Changed files in this revision: spec.md, proposal.md, definition-editor.js, DefinitionAuthoringBrowserIT.java, status.md, this checkpoint.
- Focused command: `./mvnw -pl timetable-workspace -am -Pworkspace-browser -DskipTests=false -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=DefinitionAuthoringBrowserIT#uc1EmptyEntryNavigationResumeAndResponsiveInspection' -Dfailsafe.failIfNoSpecifiedTests=false verify`.
- Result: BUILD SUCCESS; 4 architecture checks + 1 real-browser journey passed, zero failures/errors/skips. The browser visits all nine sections, asserts Resources is absent, checks all four specified widths, reloads and resumes, and verifies exact saved-document/version preservation. Log: `/tmp/school-authoring-tab-split.log`.
- JavaScript syntax and `git diff --check`: PASS. Broader source/lifecycle/security/Current evidence from the previous convergence is unchanged and not redundantly rerun for this presentation-only revision.
- User confirmation covers the requested navigation change; no further walkthrough approval request is needed.
