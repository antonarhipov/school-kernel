# Convergence: UC-2 - Set school identity

## Summary

- Submission: [UC-2 checkpoint](../checkpoints/UC-2.md) at `9b595839745ef96578aadd9a70bcebbd4ee8a550`; original implementation `60ff34ce8f4062ae0b3b2ac69ab5a05a0e31e6f0`, original base `80146e7f00d5f51500ca0e48a71de9ade477651d`.
- Verdict: PENDING WALKTHROUGH.
- Findings: zero open critical, gap or protocol findings. Initial C-1 is resolved by the committed heading correction and real-browser regression.
- Suite: 26 unit/architecture + 19 integration/browser tests; zero failures/errors/skips; BUILD SUCCESS. Fresh reconvergence log `/tmp/uc2-reconvergence.log`.
- Working tree clean before and after verification. No tracked runtime or unrelated changes. Standalone audit used only the disposable UC-2 database.

## Protocol Gate

1. Exactly UC-2 is READY_FOR_CONVERGENCE in revised immutable submission 9b59583.
2. Checkpoint and implementation are committed. All original ten files inspected during the original audit; all four revision files inspected again against 8153851. No production/test changes occurred during this verifier run.
3. Requires UC-1 is APPROVED by its committed report. No Includes or Extends relations.
4. No other UC in progress or ready. Later UCs remain NOT_STARTED.
5. Checkpoint covers all four main steps, four extensions, three guarantees, two postconditions, relationship and project constraints; C-1 regression is included.
6. Scope is school identity with necessary form/save infrastructure. No later editor, catalog upgrade, full review, finish, whole-draft discard or unrelated behavior is implemented.

## Runtime Reproduction

The convergence role obtained fresh results by rerunning the checkpoint command against the immutable revision, rather than adopting builder-reported results. Real Chrome drives production authoring entry, form inputs and explicit saves; actual local HTTP routes, migrated PostgreSQL storage, rejecting triggers, CSRF denials, changed states/versions, reload and real application-context restart establish the observed responses and consequential state.

```sh
./mvnw -pl timetable-workspace -am -Pworkspace-browser '-Dit.test=SchoolIdentityBrowserIT,DefinitionAuthoringBrowserIT,SavedDefinitionDraftBrowserIT,WorkspaceImportIT#localSecurityDenialsPreserveState+localRouteSurfaceIsNarrow' -Dfailsafe.failIfNoSpecifiedTests=false verify
```

| Actor | Step/extension | Executor reported | Converge observed |
|---|---|---|---|
| Local administrator | Main 1–4 | Readable suggestion / override, explicit save | Õ/ä/š normalize to o/a/s; fallback, unique suffix, 128-character limit and uppercase override; unchanged stored draft before save; complete name/ID JSON equality after save and Saved status |
| Local administrator | Extension 1a | Identity remains read-only | Actual readonly input after first save; direct rename gives 409 SCHOOL_ID_IMMUTABLE with no durable change |
| Local administrator | Extension 3a | Invalid/reserved ID refused | Real form invalid/reserved save plus HTTP invalid-ID cases give 422/409; exact unsaved text and complete stored document/version retained; cross-collection ID allowed |
| Local administrator | Extension 3b | Blank name saved with blocker | Blank/whitespace stored exactly; complete attributed issue record rendered; correcting name clears the identity issue |
| Local administrator | Extension 3c | Failed guarded save keeps working values | Actual database write failure, newer committed version, pre-request transport abort, removed CSRF header on both actual form attempts and changed lifecycle; no prohibited write; working text remains Unsaved; refresh/review/retry succeeds |
| Local administrator | Navigation / presentation | Save, discard working edits or stay | Actual dialogs and failed Save-and-continue retain the School form; desktop inputs/Save/status fit, narrow hides editing, widening retains exact unsaved text |
| Local administrator | Source/Current/metadata | Unchanged source and Current | Full initial/accepted definitions and source trees compared by value; optional settings, saved context, removed IDs and unrelated diagnostics preserved; save-before-Current returns to newest draft; export matches exact accepted definition/result |
| Local administrator | Durable reload/restart | Saved values survive | Working-only values disappear on reload; saved literal name and stable ID survive independently restarted application context with complete stored document/version equality |
| Local administrator | C-1 long-name revision | No page horizontal overflow | New regression saves 140 A characters, checks every required width and exact text/ID/document; standalone packaged-server Chrome measurement confirms 1280/1280 and 390/390 (viewport/page width), with no mobile form |

Standalone browser session `uc2-audit` independently reproduced the original overflow (1280px viewport / 2402px page) and repeated the corrected build after restart (1280/1280, 390/390). Only the matching agent-created version-2 long-name variant was conditionally reset in the disposable walkthrough database; no operator state was used. The browser session is closed. The walkthrough remains runnable and fresh at EMPTY/version 0.

## Evidence Ledger

Test pointers are `timetable-workspace/src/test/java/org/schoolkernel/workspace/SchoolIdentityBrowserIT.java` unless stated otherwise. Strength grades below concern automated behavioral evidence; administrator confirmation remains the explicit human gate below.

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1: name, new suggestion or advanced override | SchoolIdentityBrowserIT:17,54; actual input; Õ/ä/š, fallback, 128-character limit, unique suffix and uppercase override | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Main 2: working values, catalog and Saved/Unsaved | :17,90; rendered form and catalog, complete stored document unchanged before explicit save | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Main 3: explicit save | :17,90; button / Save and continue drive production HTTP route | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Main 4: saved name/ID, complete resulting issues, Saved | :17,54; full definition/issue equality and browser status | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Extension 1a: established identity read-only; rename refused | :17,54; readonly input and HTTP 409 SCHOOL_ID_IMMUTABLE; complete document/version unchanged | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Extension 3a: invalid/reserved identity retains unsaved work | :54,194; actual invalid and reserved-ID form save, 422/409; exact working text and durable equality | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Extension 3b: incomplete non-identity values save with blockers | :54; blank name saved exactly; complete attributed issue; correction removes blocker | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Extension 3c: access/state/version/storage failure retains working values | :125,194; actual PostgreSQL rejecting trigger, stale newer save, pre-request network abort, removed CSRF header on actual form, changed lifecycle; exact durable data/version retained; working values Unsaved; review/retry | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| G1: root fields; name edit keeps identity/source | :17,158; full JSON tree equality; existing/first-saved ID immutable; source metadata and Current exact | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| G2: system-managed schema/revisions/catalog; optional/text fidelity | :17,158,194; unknown/managed fields refused; draft revision changes; exact optional values, editor context, removed identities and unrelated diagnostics preserved; literal names/diacritics/whitespace | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| G3: atomic explicit save, local access, responsive form, Current exact | :90,125,158,194; all required viewports; no horizontal overflow; inputs/Save/status inside desktop viewport; narrow read-only and widened unsaved values retained; real CSRF/state/version/storage denial and exact Current export | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| C-1 regression / G3 saved long name | SchoolIdentityBrowserIT:214; 140-character saved name wraps at all four required widths; exact name/ID/document retained | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Success postcondition | :17,54,158; DEFINITION_DRAFT durable and labelled with saved identity; reload plus independently restarted application restores exact document | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Minimal guarantee | :54,90,125,194; full document/version unchanged on refused/failed save; working edits distinguishable and retained | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |
| Requires UC-1 | All seven journeys open through UC-1's production modal/path; complete seven-test UC-1 suite rerun | Fresh committed-submission rerun; assertions inspected at cited lines | STRONG | yes |

## Rule Conformance

No feature rules.md exists; no rule IDs are invented. Applicable project constraints were verified:

- Security/access: GET workspace/page/csrf and accepted export retain their existing local access; POST /api/definition-draft/school is admitted only behind unchanged Host/Origin/CSRF filters. Unsupported routes/methods remain denyAll. Wrong Host reveals no draft; hostile Origin and missing CSRF deny without mutation. The single local administrator requires no accounts, role/ownership matrix, language selector or logout flow.
- State/version/atomicity: DefinitionDraftService:135 checks If-Match, restricts state to DEFINITION_DRAFT, rejects extra/system-managed fields and immutable/invalid/reserved IDs before writing. WorkspaceRepository.replace compares version and lifecycle in one UPDATE; service is transactional. Every disallowed enum is exercised by HTTP with complete persisted version/document preservation.
- Fidelity/managed fields: service deep-copies the aggregate and changes only name/ID, identity diagnostics and draftRevision. Source, accepted baseline/result/provenance, catalog/schema/basedOnRevision, optional presence and saved context/other diagnostics remain exact. Existing canonical revision implementation reused.
- Presentation: literal text/diacritics and whitespace preserved; English controls; all four required viewports exercised; narrow content read-only and widened form retains working values. C-1 wrapping fix does not truncate or change saved names.
- Architecture/hygiene: all 26 workspace unit tests, including 4 architecture tests, passed without new exemptions/skips. No schema/migration, kernel implementation dependency, external service or accounts added. Tests use isolated PostgreSQL and real production routes; no generated/tracked runtime changes.

## Related-UC Regression

| Use case/shared boundary | Evidence obtained | Result |
|---|---|---|
| APPROVED UC-1 opening/resume, source fidelity, lifecycle lock, security, responsive navigation and Current | DefinitionAuthoringBrowserIT 7/7 rerun | PASS |
| Existing saved-draft compatibility / Current/export | SavedDefinitionDraftBrowserIT 3/3 rerun | PASS |
| Existing local security and route surface | WorkspaceImportIT localSecurityDenialsPreserveState + localRouteSurfaceIsNarrow 2/2 rerun | PASS |
| Shared kernel/process/archive/provenance/review/architecture boundaries | All 26 workspace unit/architecture tests rerun | PASS |

Broader unrelated solver/repair/proposal/reactor suites were not repeated or claimed green. Relevant suite is the complete UC-2 contract plus all UC-1/shared affected boundaries.

## Findings

No open findings. C-1 from the initial rejection (report committed at 8153851) is closed: styles.css:800 supplies overflow-wrap:anywhere; SchoolIdentityBrowserIT:214 proves saved long-name layout at 1600x900, 1280x800, 390x844 and 700x844 while preserving exact saved text, ID and full durable document. No spec weakening or extra approval is needed for this correction.

## Walkthrough

Open http://localhost:18082/workspace/ — isolated disposable workspace; editing here does not touch the operator's school. Corrected packaged server session 73332, log `/tmp/uc2-walkthrough-final.log`; PostgreSQL container `school-authoring-uc2-walkthrough-20261011`, loopback port 24482. Starts EMPTY, version 0.

1. Choose Create school → Open draft. In School, enter `Õäš Kool`; confirm suggested `oas-kool`, catalog 9 and Unsaved. Enter an invalid identifier such as `INVALID ID`, save and confirm the explanation, retained working inputs and Unsaved status. Correct the identifier to the suggestion or a chosen valid advanced override, save, and confirm Saved and the read-only identifier.
2. Edit only the school name, save, reload, then return to School. Confirm the saved name is restored and the identifier is unchanged; draft and No Current labels remain clear.
3. Edit the name without saving and select Review. Try Stay, then Discard working edits, then another edit with Save and continue. Confirm each choice has its named effect and review includes only saved values.
4. Save a blank name. Confirm Saved with a blocking name issue. Enter a nonblank name and save; confirm the issue clears and the stable identifier remains unchanged.
5. Make an unsaved edit, narrow to mobile width, then widen. Confirm read-only saved inspection with a wider-view explanation, no editing controls in narrow view, and retained unsaved working text after widening. At desktop width, check the form, navigation, save/status and literal entered names are clear.
6. For the stale-save extension, keep working edits in the first tab, open a second tab and save a different name. Save in the first tab: confirm it refuses the stale save and retains working values; use Review newer saved draft for retry, inspect the newer saved name, then explicitly save or discard your working edits.

Storage and access failures, accepted-source fidelity and actual process restart have automated production-boundary evidence; the administrator is not asked to manipulate the database or security configuration. Confirm this School identity journey or report the step that needs correction.

User result: not yet received for UC-2. Prior UC-1 walkthrough approval applies to UC-1 only.

## Status Update

UC-2 READY_FOR_CONVERGENCE -> PENDING_WALKTHROUGH. No later UC starts while this human gate is pending. UC-1 remains APPROVED; UC-3 is next in document order after UC-2 approval.

## Response to execute

PENDING WALKTHROUGH: UC-2 automated contract and regressions pass; confirm the School identity journey at localhost:18082.
