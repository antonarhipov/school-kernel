# UC-4 — Configure subjects

- Base: `63c2f24`; pre-existing changes: only the resumed UC-4 service/route/action/status work.
- Submission: exact UC-4 diff and intended commit recorded by the implementation ledger.
- Implements the seven Subject fields, catalog availability, Unicode-readable unique suggestions/manual overrides, permanent identity, explicit save, supported invalid-value persistence and blockers, omission versus explicit false/value, qualifications/lessons/policy effects, and curator qualification exception.
- Selected-subject transactional CAS preserves all other definition trees, source document, accepted Current/result/provenance, editing context and removed lineage. No cascade or implicit reassignment.
- Browser working edits are retained across resizing and failed writes. Narrow views show saved values without mutation controls. Unsaved navigation offers Save/Discard/Stay. Stale recovery merges untouched fields and requires an explicit same-field conflict choice.
- Destination handoffs: catalog upgrade UC-12; guided removal UC-13; source restoration UC-14. Their explanation/refusal boundary is delivered here; destination implementation is still outstanding.
- Technical checks: 26 unit/architecture and all six new subject browser journeys passed in `/tmp/uc4-revision-verify.log` (BUILD SUCCESS). The first run `/tmp/uc4-verify.log` retains the Add-subject finding and fixture issue; all 24 existing grid/school/open/resume compatibility browser journeys passed there. The production fix changes only subject selection. Syntax and diff hygiene passed.

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionSubjectsBrowserIT,DefinitionGridBrowserIT,SchoolIdentityBrowserIT,DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT,WorkspaceLocalSecurityIT' -Dfailsafe.failIfNoSpecifiedTests=false verify
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionSubjectsBrowserIT' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

`WorkspaceLocalSecurityIT` does not exist in this checkout and is not counted. Subject tests directly prove CSRF, state and version protection; existing local Host/Origin protection is unchanged.
- Runtime proof uses isolated Testcontainers PostgreSQL/Chrome. Operator data is untouched.
- Standalone Playwright ledger: pending technical checks, under `output/playwright/school-definition-authoring/UC-4/`.

## Walkthrough F-1 revision

- Attempt 1 failed accessible numeric-control inspection; preserved ledger/screenshots in UC-4/20261011-1. Explicit accessible names added to both numeric values with a real-Chrome regression assertion. No persistence logic changed. Focused check passed: 26 unit/architecture checks and the strengthened main subject save/reload/restart journey; BUILD SUCCESS `/tmp/uc4-label-verify.log`.
