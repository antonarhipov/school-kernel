# Convergence: UC-2 - Set school identity

## Summary

- Submission: checkpoints/UC-2.md at `60ff34ce8f4062ae0b3b2ac69ab5a05a0e31e6f0`; base `80146e7f00d5f51500ca0e48a71de9ade477651d`.
- Verdict: REJECT.
- Findings: 1 critical (C-1); zero protocol findings; no separate gap finding (layout evidence covered by C-1).
- Suite: 26 unit/architecture + 18 integration/browser; zero failures/errors/skips, BUILD SUCCESS. Log `/tmp/uc2-convergence.log`.
- Working tree clean before and after audit. Isolated standalone browser changed only the disposable walkthrough database, not tracked/operator data.

## Protocol Gate

1. Exactly UC-2 READY_FOR_CONVERGENCE; all other UCs retain prior statuses.
2. Checkpoint and complete ten-file implementation are together at immutable submission 60ff34c. Every changed code/test/CSS/checkpoint/status file inspected.
3. Requires UC-1 is APPROVED by its committed convergence report. No Includes/Extends.
4. No other UC in progress or ready.
5. Checkpoint has all four main steps, all four extensions, three guarantees, two postconditions, relation, constraint and regression evidence.
6. Diff is restricted to school identity and necessary form/save infrastructure. No other editor, catalog upgrade, finish or discard implemented; no unrelated changes.

## Runtime Reproduction

The verifier reran the checkpoint's exact Maven command against the immutable submission, obtained its own fresh results, and read each claimed boundary assertion. Actual Chrome drives opening/name input/explicit save/invalid input/blank save/navigation/storage denial/access denial/stale review/retry/reload/restart; full durable JSON/version assertions verify prohibited side effects. A separate Chrome CLI session (`uc2-audit`) opened the packaged isolated localhost:18082 server through Create school/Open draft, saved 140 ASCII A characters as the display name and resized to 1280x800. Browser measurement: innerWidth 1280, documentElement.scrollWidth 2402. This is a valid supported school name and reproduces C-1 independently of the submitted suite.

| Actor | Step or extension | Executor reported | Converge observed |
|---|---|---|---|
| Local administrator | Main 1–4 | Suggested/overridden ID, explicit durable save | Full name/ID JSON equality, Saved status, immutable identity after first save |
| Local administrator | Invalid/rename/reserved ID | Refusal and retained working values | Actual form/HTTP 422/409; full document/version unchanged |
| Local administrator | Blank name | Save with blocker | Exact blank text and complete attributed issue; correction clears identity issue |
| Local administrator | Access/state/version/storage failure | No committed write, working form retained | Real denying CSRF filter, all disallowed states, changed version, rejecting PostgreSQL trigger; working values remain Unsaved |
| Local administrator | Navigation / read-only / reload/restart | Explicit save/discard/stay; durable reload | Main and navigation assertions rerun; all required widths pass for representative names |
| Local administrator | Initial/accepted source | Source/Current fidelity | Exact complete source, optional values, editor context, removed IDs and diagnostics; Current export pair verified |
| Local administrator | Long supported name | Not covered after save | 1280px viewport expands to 2402px page width; fails presentation guarantee |

## Evidence Ledger

| Contract element | Executor claim | Evidence obtained | Strength | Verified |
|---|---|---|---|---|
| Main 1: name, new suggestion or advanced override | SchoolIdentityBrowserIT:17,54; actual input; Õ/ä/š, fallback, 128-character limit, unique suffix and uppercase override | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Main 2: working values, catalog and Saved/Unsaved | :17,90; rendered form and catalog, complete stored document unchanged before explicit save | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Main 3: explicit save | :17,90; button / Save and continue drive production HTTP route | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Main 4: saved name/ID, complete resulting issues, Saved | :17,54; full definition/issue equality and browser status | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Extension 1a: established identity read-only; rename refused | :17,54; readonly input and HTTP 409 SCHOOL_ID_IMMUTABLE; complete document/version unchanged | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Extension 3a: invalid/reserved identity retains unsaved work | :54,194; actual invalid and reserved-ID form save, 422/409; exact working text and durable equality | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Extension 3b: incomplete non-identity values save with blockers | :54; blank name saved exactly; complete attributed issue; correction removes blocker | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Extension 3c: access/state/version/storage failure retains working values | :125,194; actual PostgreSQL rejecting trigger, stale newer save, pre-request network abort, removed CSRF header on actual form, changed lifecycle; exact durable data/version retained; working values Unsaved; review/retry | Rerun actual boundary assertions at cited lines | STRONG | yes |
| G1: root fields; name edit keeps identity/source | :17,158; full JSON tree equality; existing/first-saved ID immutable; source metadata and Current exact | Rerun actual boundary assertions at cited lines | STRONG | yes |
| G2: system-managed schema/revisions/catalog; optional/text fidelity | :17,158,194; unknown/managed fields refused; draft revision changes; exact optional values, editor context, removed identities and unrelated diagnostics preserved; literal names/diacritics/whitespace | Rerun actual boundary assertions at cited lines | STRONG | yes |
| G3: atomic explicit save, local access, responsive form, Current exact | :90,125,158,194; all required viewports; no horizontal overflow; inputs/Save/status inside desktop viewport; narrow read-only and widened unsaved values retained; real CSRF/state/version/storage denial and exact Current export | Rerun actual boundary assertions at cited lines; standalone long-name variant overflows (1280 -> 2402 CSS px) | WEAK | no |
| Success postcondition | :17,54,158; DEFINITION_DRAFT durable and labelled with saved identity; reload plus independently restarted application restores exact document | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Minimal guarantee | :54,90,125,194; full document/version unchanged on refused/failed save; working edits distinguishable and retained | Rerun actual boundary assertions at cited lines | STRONG | yes |
| Requires UC-1 | All six journeys open through UC-1's production modal/path; complete seven-test UC-1 suite rerun | Rerun actual boundary assertions at cited lines | STRONG | yes |

## Rule Conformance

No feature rules.md exists. Existing local security was inspected and exercised: new POST /api/definition-draft/school is only accessible behind unchanged Host/Origin/CSRF filters, while GET workspace/page/csrf and accepted export remain existing local read routes. Unsupported HTTP methods/routes remain denyAll. Wrong Host reveals no draft; missing CSRF/hostile Origin refuse without mutation. No roles/ownership/account/logout matrix applies to the specified single local operator. New server method enforces state/version checks and exact field allowlist, deep-copies the document and performs one transactional SQL CAS; managed and unrelated data stay untouched. Architecture checks pass without exemptions. Presentation constraint fails for C-1.

## Related-UC Regression

| Use case/shared boundary | Evidence | Result |
|---|---|---|
| APPROVED UC-1 | DefinitionAuthoringBrowserIT 7/7 rerun | PASS |
| Persisted definition draft compatibility / Current/export | SavedDefinitionDraftBrowserIT 3/3 rerun | PASS |
| Local security and route surface | WorkspaceImportIT two named tests 2/2 rerun | PASS |
| Unit/kernel-boundary/architecture | All 26 workspace tests rerun | PASS |

## Findings

### C-1 CRITICAL — long saved school name creates page-level horizontal scrolling

- Contract: UC-2 G3: “Save is atomic, explicitly requested, and subject to UC-1's access and presentation boundaries.” UC-1 G4: “At 1600×900 and 1280×800 the draft identity, section navigation, form, and save/error status are visible without page-level horizontal scroll or clipped controls.”
- Evidence: `definition-editor.js:66` renders unrestricted displayName as h2. `styles.css:800` adjusts its margin but supplies no word wrapping. SchoolIdentityBrowserIT:90 only covers a short name, and :54 checks a long suggestion before saving its name. Standalone real Chrome saved 140 A characters and measured 2402px document width at 1280px viewport.
- Required revision: wrap long school headings without truncating stored text; add a saved-name real-browser regression at the required desktop/narrow widths. Keep identifier, source and saved text exact.

## Walkthrough

Not eligible yet; fix C-1 and reconverge before requesting administrator confirmation.

## Status Update

UC-2 READY_FOR_CONVERGENCE -> NEEDS_REVISION; no later UC selected.

## Response to execute

REVISE UC-2: C-1 — wrap long saved school names and prove all required widths preserve layout and exact text.
