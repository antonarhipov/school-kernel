# Use-Case Checkpoint: UC-3 - Configure the weekly period grid

## Summary

- Status: PENDING_WALKTHROUGH
- Base commit: dc068930a7be327d96c0f8a0edee0a0e4517cf12
- Submission: HEAD at walkthrough; committed implementation boundary before the recorded browser journey.
- Relations: Requires UC-1 (APPROVED); no Includes or Extends.
- Technical constraints: spec UC-3, Period fields, Identifier rules, form/lifecycle models and approved UC-1 local access/presentation/fidelity boundaries. No feature rules.md exists.

## Contract Evidence

Test references are in timetable-workspace/src/test/java/org/schoolkernel/workspace/DefinitionGridBrowserIT.java; method names are authoritative where line numbers changed. Real Chrome and production HTTP routes run against isolated migrated PostgreSQL. Fixtures declaring resource availability are labelled isolated variants, not early implementation of later entity editors.

| Contract element | Test/runtime evidence | Result |
|---|---|---|
| Main 1 | DefinitionGridBrowserIT:14,90: Generation with chosen weekday counts/optional bell pairs; existing grid additions preserve every original row. | PASS |
| Main 2 | DefinitionGridBrowserIT:14,47,154: Real row preview; exact period rows/reservations and six named availability effects; version bound and read-only. | PASS |
| Main 3 | DefinitionGridBrowserIT:14,47: Individual second-slot time adjustment, explicit save; working edits are not durable. | PASS |
| Main 4 | DefinitionGridBrowserIT:14,90: Exact selected grid saved together, fixed IDs, issues and same-ID lesson timing impacts. | PASS |
| Extension 1a | DefinitionGridBrowserIT:90,136: Generation appends only; direct removal/replacement refused without mutation and explains guided removal. | PASS |
| Extension 2a | DefinitionGridBrowserIT:47: All duplicate/overlap owner/field/messages/related IDs compared by value; bad orders and invalid paired times retained with blockers. | PASS |
| Extension 3a | DefinitionGridBrowserIT:47: Cancel preview discards working rows only; complete durable document/version remains exact. | PASS |
| Extension 3b handoff | DefinitionGridBrowserIT:136: Direct deletion is atomically refused with GUIDED_REMOVAL_REQUIRED. Complete guided dependency resolution belongs to UC-13 and remains an outstanding destination handoff. | PASS for current boundary; UC-13 pending |
| Extension 3c | DefinitionGridBrowserIT:118,136: Real database rejecting trigger, concurrent saved version, pre-request network abort and stripped CSRF; exact durable equality, retained working rows, refresh/retry. | PASS |
| G1 | DefinitionGridBrowserIT:14,47: Exact declared day/order/name/time population; reservations explicit; omissions and paired representations restored. | PASS |
| G2 | DefinitionGridBrowserIT:154: Complete named availability-effect population compared by value; all omitted/explicit-empty resource arrays unchanged after addition. | PASS |
| G3 | DefinitionGridBrowserIT:90: Every original period retained; all affected accepted lesson IDs compared exactly, before/after times including same ID; unrelated fields and accepted/source trees exact. | PASS |
| G4 | DefinitionGridBrowserIT:14,47,118,136: Desktop/narrow widths, literal names, unsaved retention, all lifecycle refusals, security/storage/stale failures, unchanged source/export; separate walkthrough follows technical convergence. | PASS |
| Success postcondition | DefinitionGridBrowserIT:14,47: Saved labelled DEFINITION_DRAFT; exact grid/issues/source restored after reload and independent server restart. | PASS |
| Minimal guarantee | DefinitionGridBrowserIT:47,118,136: Complete document/version equality for canceled, failed, stale, denied and removal operations. | PASS |
| Requires UC-1 | All six journeys; DefinitionAuthoringBrowserIT 7/7: Actual approved authoring entry supplies durable draft; related-UC regression passes. | PASS |

## Technical and rule evidence

No feature rules apply. DefinitionGridService checks local draft lifecycle and workspace version before calculating a read-only preview; save recalculates the exact candidate and commits through the existing transactional version/state CAS. Existing period IDs cannot be dropped; new IDs are checked for grammar, uniqueness and lineage reservations. Supported invalid values remain JSON draft values with deterministic ordered issues. It changes only periods/reservations, grid issues, managed revision and grid timing review; source, Current, lesson settings, availability arrays and unrelated metadata retain exact trees. Optional clock strings are never parsed and reformatted for storage.

The two POST routes retain local Host/Origin/CSRF protection through SecurityConfiguration, HostOriginFilter and DefinitionDraftAccess. No kernel implementation dependency, architecture exemption, outbound service, migration or account is introduced. Existing UC-1/UC-2 suites and saved-draft compatibility/security checks passed.

## Validation

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionGridBrowserIT,SchoolIdentityBrowserIT,DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT,WorkspaceImportIT#localSecurityDenialsPreserveState+localRouteSurfaceIsNarrow' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

- Completed relevant suite: 26 unit/architecture + 25 integration/browser checks; zero failures/errors/skips. BUILD SUCCESS; /tmp/uc3-final-verify.log. Six grid, seven identity, seven opening, three compatibility and two local access/route journeys.
- Strengthened final by-value conflict/addition assertions separately verified with the targeted two-method command in /tmp/uc3-assertions-verify.log; no new production changes.
- node --check for app.js, definition-editor.js and definition-grid.js; git diff --check: PASS.
- Verification changed no tracked runtime/operator data or unrelated files.
- Provisioned disposable packaged server at localhost:18083 against the agent-owned UC-3 database; confirmed clean Flyway startup. Server/container were removed while the ordering decision is pending. No browser walkthrough or approval is claimed; no operator database was used.

## Changed files

- timetable-workspace/src/main/java/org/schoolkernel/workspace/DefinitionGridService.java
- timetable-workspace/src/main/java/org/schoolkernel/workspace/DefinitionDraftService.java
- timetable-workspace/src/main/java/org/schoolkernel/workspace/DefinitionDraftAccess.java
- timetable-workspace/src/main/java/org/schoolkernel/workspace/SecurityConfiguration.java
- timetable-workspace/src/main/java/org/schoolkernel/workspace/WorkspaceController.java
- timetable-workspace/src/main/resources/static/workspace/definition-grid.js
- timetable-workspace/src/main/resources/static/workspace/definition-editor.js
- timetable-workspace/src/main/resources/static/workspace/app.js
- timetable-workspace/src/main/resources/static/workspace/styles.css
- timetable-workspace/src/test/java/org/schoolkernel/workspace/DefinitionGridBrowserIT.java
- spec/school-definition-authoring/status.md
- spec/school-definition-authoring/checkpoints/UC-3.md

## Scope

Only UC-3 implemented. Subject/resource/requirement/finish/removal editors remain owned by their later UCs; deleting a saved period is explicitly refused with guided-removal guidance. No later-UC implementation has begun.

READY FOR WALKTHROUGH: UC-3

## Ordering finding

- UC-3 extension 3b says: “If a deletion is requested, continue with UC-13.” UC-13 remains NOT_STARTED. The grid endpoint currently returns GUIDED_REMOVAL_REQUIRED and preserves the entire document; it does not supply the full required continuation.
- Execute requires: “Do not start another use case until the current one is APPROVED.” Full extension evidence is required before submission/approval. Asked the user to allow prerequisite flows before completion or explicitly defer this extension; no contract has been changed and no later UC has begun.

## Ordering resolution

The user explicitly replaced the procedural gate with a recorded automatic Playwright approval/fix loop. Under the updated execute skill, the cross-UC continuation is verified as the current safe refusal/guidance; destination UC-13 must implement its complete removal flow before feature completion. This is not evidence of UC-13 completion. The earlier ordering finding above is historical and no longer blocks the current grid walkthrough. Existing technical evidence remains valid for unchanged production code.
