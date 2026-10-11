# Convergence: UC-1 - Open or resume a school-definition draft

## Summary

- Submission: [UC-1 checkpoint](../checkpoints/UC-1.md) at `06c0201f24b127f75efeec89f145966aa60b10ea`.
- Verdict: PENDING WALKTHROUGH.
- Findings: zero critical, gap, or protocol findings; human confirmation remains outstanding.
- Suite obtained in this audit: 26 workspace unit/architecture tests + 17 integration/browser tests, zero failures/errors/skips; BUILD SUCCESS.
- Shared accepted Current regression: 11/11 passed in the earlier completed-slice run. Not repeated because later changes affected authoring rendering/command guards, not that read-only Current path, and the user requested avoidance of unnecessary slow checks.
- Working tree impact from verification: none; status was clean before and after.

## Protocol Gate

1. Exactly UC-1 was READY_FOR_CONVERGENCE.
2. Checkpoint and implementation are together in the immutable submission commit above; the diff was inspected against `0fd30998e2b20e79ed61392a2125c73067d2c76e`.
3. UC-1 has no relationship dependencies. All other authoring UCs remain NOT_STARTED.
4. No other UC is IN_PROGRESS or READY_FOR_CONVERGENCE.
5. Checkpoint accounts for all main steps, seven extensions, seven guarantees, both postconditions, validation and affected regressions. No separate feature rules exist.
6. The diff is limited to UC-1 and its required guards/schema/security/read-only navigation. Identity/resource editors, full change review, finish and authoring discard are not implemented.

## Runtime Reproduction

The audit reran the committed tests itself, rather than treating the executor's earlier output as verification. Real Chrome drove the modal, section navigation, cancellation, failed/stale confirmations, reload and responsive inspection; browser HTTP requests drove the state/refusal/security matrices. PostgreSQL stores were compared by complete JSON value and version. Two restarted application contexts loaded the same saved draft; exports were verified through the packaged kernel.

Command:

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT,WorkspaceImportIT#importsInitialDefinition+exhaustiveLifecycleRefusalMatrix+clearsWorkspaceViaPostAndDeletes+clearsWorkspaceRejectsStalePrecondition+uploadsNewSchoolDefinitionFromNonEmptyState+localSecurityDenialsPreserveState+localRouteSurfaceIsNarrow' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

Log: `/tmp/school-authoring-convergence.log`. Results: WorkspaceImportIT 7/7; DefinitionAuthoringBrowserIT 7/7; SavedDefinitionDraftBrowserIT 3/3; all workspace unit tests 26/26.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Local administrator | Empty creation | Saved catalog-9 draft, School entry | Real browser modal opened durable exact empty definition; import was available before opening |
| Local administrator | Initial source | Exact copied source, Review entry | Full MVK JSON equality (labelled name/availability variant), literal text, two real process-context restarts |
| Local administrator | Accepted source | Upgrade confirmation; pins expire | Cancel and refusal leave source exact; confirmation upgrades to 8, preserves weights/gaps, expires only attempt period pin |
| Local administrator | Resume | No reset/write | Exact version/document/context/removed-ID/diagnostic/persistent-lock equality |
| Local administrator | Other lifecycles | Refuse and identify state | All other workspace enums returned 409, named the state and preserved full state |
| Local administrator | Competing mutation | Authoring lock | All listed POST commands and DELETE workspace returned authoring-draft refusal before body decoding; document/version unchanged |
| Local administrator | Opening failure | No partial draft | PostgreSQL trigger failure and stale modal confirmation observed in the browser; retry after reload succeeds |
| Untrusted request | Access denial | No mutation/disclosure | Missing CSRF/hostile Origin denied; hostile Host workspace GET denied without draft data |

## Evidence Ledger

Pointers are to `timetable-workspace/src/test/java/org/schoolkernel/workspace/DefinitionAuthoringBrowserIT.java`, except where another file is named.

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main step 1 | Open each declared source | :15,50,77 rerun, complete persisted-state comparisons | STRONG | yes |
| Main step 2 | Source/lock/upgrade/pin explanation | :15,77 real rendered modal and pin/adjustment text, cancellation before write | STRONG | yes |
| Main step 3 | Confirm upgrade | :77 real click and refused unconfirmed HTTP call | STRONG | yes |
| Main step 4 | Atomic copied/new saved definition | :15,50,77,124 JSON-by-value assertions; SQL version/state CAS inspected | STRONG | yes |
| Main step 5 | Navigation/counts/identity/Saved, correct initial section | :15,50 rendered sections and selected heading, 4 viewport checks; definition-editor.js inspected | STRONG | yes |
| Main step 6 | Navigate / inspect unchanged Current | :15,77 real click Current/back; source equality and verified export | STRONG | yes |
| Extension 1a | Resume without creation/reset | :15,124 full stored version/document equality including explicit policy locks and diagnostics | STRONG | yes |
| Extension 1b | Refuse conflicting workflows | :163 every disallowed enum, state in explanation and no write; UI entry availability inspected | STRONG | yes |
| Extension 2a | Empty create beside import, no Current | :15 visible import, exact catalog/collections, source EMPTY and no accepted result | STRONG | yes |
| Extension 2b | Refuse unsupported/unmappable data | :183 schema/catalog/unknown root+nested/fractional integer, location-bearing refusal and full state preservation | STRONG | yes |
| Extension 3a | Cancellation keeps source | :77 browser Cancel, exact version/document before/after | STRONG | yes |
| Extension 4a | Failed/stale open creates no partial draft | :183,212 browser storage trigger and changed-version confirmation; exact source retained | STRONG | yes |
| Extension 5a | Reload/restart preserves draft | :15,50 reload and separate actual application-context restarts, full stored data equality | STRONG | yes |
| G1 | Definition copy/lineage/provenance | :50,77,124 exact successor equality, accepted inputRevision, retained persistent lock, expired attempt pin; source assignments never copied as defaults | STRONG | yes |
| G2 | Supported fields/omissions/context/IDs/issues retained | :50 MVK tree equality including all native policies; :124 explicit-empty values, editor context, issues/removed IDs and new policy lock retained on resume | STRONG | yes |
| G3 | Workflow exclusion; Current exact | :163 commands denied, SQL reset/clear state guard inspected; :77 actual Current return/export; SavedDefinitionDraftBrowserIT:81 export pair equality | STRONG | yes |
| G4 | Concrete layouts/narrow read-only/resize preserves data | :15 all four required widths, visible navigation and no page overflow/mutation inputs; complete document unchanged | STRONG | yes |
| G5 | English/literal names/diacritics | :50 exact saved text and no injected img; English section/status output, unchanged account/language surface | STRONG | yes |
| G6 | Local access/untrusted changes | :183 denied wrong Host, Origin and CSRF with no mutation/data disclosure; WorkspaceImportIT local security/route surface rerun | STRONG | yes |
| G7 automated portion | Isolated real browser and durability | Seven browser journeys and saved-draft regressions rerun with isolated PostgreSQL, not operator data | STRONG | yes |
| Success postcondition | Durable navigable or unchanged resumed draft | :15,50,77,124 persisted DEFINITION_DRAFT and navigation/resume observations | STRONG | yes |
| Minimal guarantee | Failed/refused open preserves source/Current | :77,163,183,212 exact JSON/version and lifecycle assertions | STRONG | yes |
| Relations | No dependencies/inclusions/extensions | Full UC-1 contract and relationship graph inspected | STRONG | yes |

## Rule Conformance

No feature `rules.md` applies. Existing local protections and architecture were verified: new POST routes are explicitly allowed only behind the unchanged Host/Origin/CSRF boundary; authoring lock runs after authorization and before request decoding. Clear/reset have atomic SQL exclusions. All architecture tests passed without exemptions, credentials remain external, no accounts/outbound service/kernel implementation dependency was introduced. Source trees remain immutable copies and unknown schema fields are refused rather than dropped.

## Related-UC Regression

| Use case / shared surface | Evidence | Result |
|---|---|---|
| Existing import and all occupied lifecycle refusals | WorkspaceImportIT importsInitialDefinition, exhaustiveLifecycleRefusalMatrix | PASS |
| Existing clear, stale precondition, reset-upload | WorkspaceImportIT three affected tests | PASS |
| Existing security and route boundary | WorkspaceImportIT localSecurityDenialsPreserveState, localRouteSurfaceIsNarrow | PASS |
| Existing Current rendering/inspection | AcceptedInspectionBrowserIT 11 tests, earlier complete run; no subsequent Current-rendering change | PASS |
| Existing persisted-draft inspection/export | SavedDefinitionDraftBrowserIT 3 tests rerun | PASS |
| Kernel boundary/import/archive/provenance/review architecture | All 26 workspace unit tests | PASS |

## Findings

None. The mandatory human walkthrough remains a gate, not an implementation defect. Expensive unrelated solver/repair/proposal/browser suites were not rerun and are not claimed green. Verification is limited to UC-1 and shared affected boundaries. UC-1 adds no working edit form, so preservation of unsaved editor inputs becomes an exercised requirement when the dependent editor UCs add those forms; resizing here is verified not to write/reset saved content.

## Walkthrough

Isolated runnable environment: `http://localhost:18081/workspace/`, fresh disposable database separate from the operator's runtime school. Server session 89056, log `/tmp/school-authoring-walkthrough.log`; container `school-authoring-uc1-walkthrough-20261011`, loopback port 24481. This is for inspection/confirmation, not a deployed runtime update. Native Chrome UI inspection was attempted but the computer-use tool refused interaction while the user was active in Chrome; this does not substitute for a user result.

1. From Empty, confirm Create school appears alongside import. Open Create school, read the single-draft lock/no-Current explanation, cancel, and confirm Empty/import remain.
2. Open again and confirm Open draft. Confirm New school, catalog 9, Saved, source EMPTY, two identity blockers, and School as the opening section.
3. Visit Grid, Subjects, Resources (Teachers, Classes, Rooms, Room policies), Weekly requirements (Series, Ungrouped lessons, Occurrence details), School (Catalog/Advanced Preferences) and Review. Confirm the selected heading and saved empty/default values are understandable and the draft remains distinct from Current.
4. Reload. Confirm the same saved draft returns in Review, with the same issues and no Current or automatic acceptance.
5. Narrow the view to mobile width, confirm the wider-view explanation and read-only content, then widen. Confirm no data was saved/reset by resizing.
6. For an initial or accepted-source variant, entry starts in Review; accepted Current inspection/export stays unchanged. If a required catalog upgrade is offered, its adjustments/expired pins are shown before confirmation and cancellation leaves the source unchanged. These source variants have automated real-browser evidence; additional isolated setup can be supplied for administrator inspection.

User result: NOT RECEIVED. Confirm PASS or report the specific step needing revision. Identity/resource edits and finish/discard belong to later UCs and are intentionally absent in this checkpoint.

## Status Update

UC-1 READY_FOR_CONVERGENCE -> PENDING_WALKTHROUGH. No dependent UC is eligible until confirmation. UC-20 is an independent root in the graph, but no other implementation begins while this convergence is pending.

## Response to execute

PENDING WALKTHROUGH: UC-1 automated checks passed; obtain explicit administrator confirmation of the recorded walkthrough before implementing dependent editors.
