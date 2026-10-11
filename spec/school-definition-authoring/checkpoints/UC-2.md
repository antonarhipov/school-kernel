# Use-Case Checkpoint: UC-2 - Set school identity

## Summary

- Status: READY_FOR_CONVERGENCE
- Base commit: 80146e7f00d5f51500ca0e48a71de9ade477651d
- Submission commit: HEAD at convergence
- Revision base: 8153851 (initial C-1 convergence finding); original implementation 60ff34c.
- Relations verified: Requires UC-1 (APPROVED); no Includes or Extends.
- Technical constraints: spec.md UC-2, School fields, Identifier rules, form/lifecycle models; approved UC-1 access/presentation/fidelity/evidence boundary; existing local security, architecture and versioned PostgreSQL aggregate. No feature rules.md exists.

## Contract Evidence

Tests are in `timetable-workspace/src/test/java/org/schoolkernel/workspace/`. SchoolIdentityBrowserIT uses actual Chrome inputs/HTTP, an isolated migrated PostgreSQL database, complete JSON-by-value assertions, a real failing database trigger and actual application restart. The reserved-school and same-ID teacher variants are explicitly labelled; cross-collection identifiers remain independent. Network abort is before the request reaches the server. Access denial strips the actual CSRF header from both real form attempts; no stubbed success/error response is used.

| Contract element | Test or runtime evidence | Result |
|---|---|---|
| Main 1: name, new suggestion or advanced override | SchoolIdentityBrowserIT:17,54; actual input; Õ/ä/š, fallback, 128-character limit, unique suffix and uppercase override | PASS |
| Main 2: working values, catalog and Saved/Unsaved | :17,90; rendered form and catalog, complete stored document unchanged before explicit save | PASS |
| Main 3: explicit save | :17,90; button / Save and continue drive production HTTP route | PASS |
| Main 4: saved name/ID, complete resulting issues, Saved | :17,54; full definition/issue equality and browser status | PASS |
| Extension 1a: established identity read-only; rename refused | :17,54; readonly input and HTTP 409 SCHOOL_ID_IMMUTABLE; complete document/version unchanged | PASS |
| Extension 3a: invalid/reserved identity retains unsaved work | :54,194; actual invalid and reserved-ID form save, 422/409; exact working text and durable equality | PASS |
| Extension 3b: incomplete non-identity values save with blockers | :54; blank name saved exactly; complete attributed issue; correction removes blocker | PASS |
| Extension 3c: access/state/version/storage failure retains working values | :125,194; actual PostgreSQL rejecting trigger, stale newer save, pre-request network abort, removed CSRF header on actual form, changed lifecycle; exact durable data/version retained; working values Unsaved; review/retry | PASS |
| G1: root fields; name edit keeps identity/source | :17,158; full JSON tree equality; existing/first-saved ID immutable; source metadata and Current exact | PASS |
| G2: system-managed schema/revisions/catalog; optional/text fidelity | :17,158,194; unknown/managed fields refused; draft revision changes; exact optional values, editor context, removed identities and unrelated diagnostics preserved; literal names/diacritics/whitespace | PASS |
| G3: atomic explicit save, local access, responsive form, Current exact | :90,125,158,194; all required viewports; no horizontal overflow; inputs/Save/status inside desktop viewport; narrow read-only and widened unsaved values retained; real CSRF/state/version/storage denial and exact Current export | PASS |
| C-1 regression / G3 saved long name | SchoolIdentityBrowserIT:214; 140-character saved name wraps at all four required widths; exact name/ID/document retained | PASS |
| Success postcondition | :17,54,158; DEFINITION_DRAFT durable and labelled with saved identity; reload plus independently restarted application restores exact document | PASS |
| Minimal guarantee | :54,90,125,194; full document/version unchanged on refused/failed save; working edits distinguishable and retained | PASS |
| Requires UC-1 | All seven journeys open through UC-1's production modal/path; complete seven-test UC-1 suite rerun | PASS |

## Rule Evidence

No feature rules apply. Project constraints: existing Host/Origin/CSRF filters retained for the new POST route (`SecurityConfiguration.java:33`, `DefinitionDraftAccess.java:26`, `WorkspaceController.java:83`). State/version refusal and one transactional CAS are in `DefinitionDraftService.java:135`; the existing SQL `WorkspaceRepository.replace` compares both version and lifecycle. No schema/migration, kernel implementation dependency, accounts or outbound service was added. ArchitectureTest 4/4 passed without exemptions. Production save updates only name/ID, identity diagnostics and the managed draft revision; opening revision, source and accepted trees remain copied unchanged.

## Validation

- Focused/full relevant completed-slice command:

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=SchoolIdentityBrowserIT,DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT,WorkspaceImportIT#localSecurityDenialsPreserveState+localRouteSurfaceIsNarrow' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

- Result: BUILD SUCCESS; 26 unit/architecture tests and 19 integration/browser tests; zero failures, errors or skips. SchoolIdentityBrowserIT 7/7, DefinitionAuthoringBrowserIT 7/7, SavedDefinitionDraftBrowserIT 3/3, WorkspaceImportIT two local access/route regressions 2/2. Fresh log `/tmp/uc2-revision-verify.log`.
- Cheap checks: JavaScript syntax (`node --check` for app.js and definition-editor.js) and `git diff --check` passed.
- Approved UC regression: all UC-1 browser journeys passed, including source opening/resume, lifecycle locks, local access, Current/export, fidelity and responsive layout. Saved-definition compatibility tests also passed unchanged.
- Test working tree impact: no tracked runtime data or unrelated files changed; only intended UC-2 files are dirty.
- Visual evidence: `/tmp/uc2-school-1600.png`, `/tmp/uc2-school-1280.png`; 1280x800 screenshot inspected, fields and Save fit the viewport. 390x844 and 700x844 tested read-only without overflow.
- Runnable isolated walkthrough: http://localhost:18082/workspace/; packaged server session 73332, `/tmp/uc2-walkthrough-final.log`; container `school-authoring-uc2-walkthrough-20261011`, loopback PostgreSQL 24482. Starts EMPTY; verified `GET /api/workspace` = state EMPTY, version 0, no Current. No operator runtime database is used.
- Broader unrelated repair/solver/proposal suites are not claimed green and were not repeated; the relevant suite is UC-2 plus every UC-1/shared affected boundary.
- Changed files:

  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/DefinitionDraftService.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/DefinitionDraftAccess.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/SecurityConfiguration.java`
  - `timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java`
  - `timetable-workspace/src/main/resources/static/workspace/definition-editor.js`
  - `timetable-workspace/src/main/resources/static/workspace/app.js`
  - `timetable-workspace/src/main/resources/static/workspace/styles.css`
  - `timetable-workspace/src/test/java/org/schoolkernel/workspace/SchoolIdentityBrowserIT.java`
  - `spec/school-definition-authoring/status.md`
  - `spec/school-definition-authoring/checkpoints/UC-2.md`

## Notes

- School identity issues are refreshed; unrelated saved issues and editor metadata are retained. Full review/validation and finish remain owned by their later UCs.
- UC-2 is the only implemented UC in this submission. UI approval awaits its own administrator walkthrough; UC-1's previous approval does not approve this new editor.


## C-1 Revision Evidence

- Initial submission 60ff34c rejected by convergence solely for unbroken saved-name heading overflow. The stored supported name remains unrestricted and exact; CSS wraps the heading rather than altering it.
- Complete relevant suite rerun: 26 unit/architecture + 19 integration/browser tests, no failures/errors/skips, BUILD SUCCESS. New saved-long-name regression passes at all four widths.
- Standalone packaged-server Chrome reproduction after restart: 1280px viewport now yields 1280px page width (previously 2402); 390px viewport yields 390px page width and no editing form. Full saved name unchanged. The agent-created audit variant was conditionally removed only from the disposable UC-2 database; walkthrough starts EMPTY again.
- Cheap syntax/diff checks remain green; no tracked runtime or unrelated changes.

READY FOR CONVERGENCE: UC-2
