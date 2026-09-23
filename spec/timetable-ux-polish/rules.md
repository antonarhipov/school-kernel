# Technical Rules: Timetable UX Polish

## Design overview

The existing Spring Boot workspace and packaged native HTML/CSS/ES modules form one persistent desktop workbench. A
complete workspace snapshot supplies the immutable accepted inspection model, durable repair draft, active run, and
verified proposal review. One browser presentation-state owner adds the available mode, inspector, and utility state
to the existing Day/Week, investigation, selection, and focused-context state. Draft and Solving decorate the accepted
canvas; Proposal uses an ephemeral comparison index keyed by stable lesson ID, joining accepted and proposed sides to
the authoritative review without replacing the accepted model. Existing services alone stage intent, run the packaged
kernel, verify outcomes, and atomically accept proposals. The prototype supplies visual hierarchy, never fixture data,
scripts, or dependencies. The complete whole-school canvas stays primary; a collapsible inspector and compact Utilities
disclosure provide contextual and secondary controls.

## Codebase alignment

- Extend the existing `timetable-workspace` Spring Boot 4.1.1 / Java 25 module, its packaged native ES modules, shared
  inspection model/state, Day/Week/focused renderers, message catalog, and PostgreSQL 18.6 / Flyway persistence. The
  standalone `ui/` result viewer is not the production workspace; the prototype is an illustrative reference only.
- Continue using `GET /api/workspace` and its accepted bundle, durable draft, run, and proposal review. Existing
  versioned mutation services, independent kernel verification, packaged process invocation, and atomic acceptance
  remain authoritative; no new route, response field, workspace field, migration, or scheduling policy is needed.
- The accepted inspection feature's bounded, school-scoped Day/Week and weekday preference remains the only browser
  storage. Mode, inspector, utility, selection, and repair/proposal details stay ephemeral. Native controls and the
  same-origin CSP exclude prototype CDN, inline-script, and unsafe HTML rendering patterns.
- The accepted inspection feature's UC-2 remains `NEEDS_REVISION` in its recorded status. Its complete shared browser
  regression and accurate convergence status are prerequisite evidence, not a reason to weaken its semantics here.

## Security surface

This presentation change introduces no authentication, role, login/logout, or new endpoint. Every permitted route is
local loopback only; mutating requests require same-origin `Host`/`Origin`, session CSRF, and matching `If-Match`.

| Route and method | Access and protection | Workbench use |
|---|---|---|
| `GET /` | Local; no CSRF; redirects to `/workspace/` | Entry |
| `GET /workspace/**` | Local; no CSRF; same-origin static assets and security headers | Existing workbench |
| `GET /api/csrf` | Local; no CSRF; no school data | Existing mutation token |
| `GET /api/workspace` | Local; `ETag`, `Cache-Control: no-store` | Complete snapshot and accepted inspection |
| `POST /api/import` | Local same-origin; CSRF, `If-Match` | Empty-workspace journey only |
| `POST /api/initial-draft/replace` | Local same-origin; CSRF, `If-Match` | Initial journey only |
| `POST /api/repair-draft` | Local same-origin; CSRF, `If-Match` | Start Draft |
| `PATCH /api/repair-draft` | Local same-origin; CSRF, `If-Match` | Stage intent and protection |
| `DELETE /api/repair-draft` | Local same-origin; CSRF, `If-Match` | Confirmed Draft discard |
| `POST /api/repair-draft/bulk-pin-preview` | Local same-origin; CSRF, `If-Match`; preview does not mutate | Preview protection |
| `POST /api/runs` | Local same-origin; CSRF, `If-Match` | Generate or retry |
| `GET /api/runs/{runId}` | Local; matching active or retained run ID required; no CSRF | Poll run and diagnostics |
| `DELETE /api/runs/{runId}` | Local same-origin; CSRF, `If-Match` | Cancel active run |
| `GET /api/proposal` | Local; `ETag`, `Cache-Control: no-store`; proposal required | Existing proposal read |
| `POST /api/proposal/accept` | Local same-origin; CSRF, `If-Match` | Confirmed acceptance |
| `DELETE /api/proposal` | Local same-origin; CSRF, `If-Match` | Proposal discard |
| `GET /api/accepted/export` | Local; accepted-only archive; no CSRF; currently requires Accepted baseline | Applicable utility |
| `/login`, `/logout`, `/actuator/**`, all other routes/methods | Denied or not exposed | No new access path |

Keep loopback binding, strict Host/Origin checking, disabled CORS/forwarded-host trust, no-store school-data responses,
`nosniff`, same-origin referrer policy, and same-origin-only CSP. Public without login means *local only*, not remotely
accessible. Do not expose export in a state the existing service refuses or relax that service to accommodate a mode.

## Verification strategy

Use fresh isolated PostgreSQL Testcontainers storage, ephemeral loopback HTTP, packaged workspace assets, a real
browser, the packaged scheduling process, and temporary fixtures/files. Never use runtime workspace data or modify
tracked examples/generated files. Validate the generated or properly anonymized `spec.md` snapshot by value against a
freshly populated database: all declared classes, teachers, rooms, periods, assignments, metadata, and change
categories must match, with no extra values; verify the definition/result pair and record data provenance. No
credentials are seeded by this feature. Compare exact accepted bundle, draft, run, proposal, and version before and
after every consequential action; negative assertions also prove absence of unauthorized disclosure, writes, process
launch, candidate rendering, or late-result application as appropriate. Test doubles must preserve production
exception, nullability, option, and field contracts.

Each UC has a real-browser main journey at the administrator boundary, including its success and minimal guarantees.
UC-1 covers empty/long metadata, verifier refusal of missing metadata before acceptance, Day/Week, focus and return,
collapse/reopen, Utilities, export failure, invalid preference, and narrow read-only behavior. UC-2 covers teacher/room
intent, zero direct effects, conflicting pins, individual/bulk preview/confirmation/cancellation/undo, failed save,
reload, mode/focus return, discard, and
narrow behavior. UC-3 covers responsive inspection and mode changes during a packaged run, cancellation, restart,
failed/no-feasible/rejected results, retry, diagnostics, and narrow behavior. UC-4 covers every overlay shape,
one-sided filters, category zeros, overlapping explanations, protected/unchanged lessons, missing names, revise,
discard, stale refusal, failed and successful acceptance, and narrow behavior. UC-5 runs the complete journey and
second repair, consuming the same UC-1→UC-4 production paths and their real postconditions. Assert state after each
transition; compare unique ID sets and exact fields, not tile counts or status text alone. Exercise the specified
validation scale and supported-desktop/narrow viewport boundary without inventing a latency threshold. Run the
complete shared browser regression, including the outstanding accepted-inspection UC-2 regression, not only focused
new tests.

After automation, perform a human browser walkthrough directly from each UC's scenario and extensions. The
administrator feature gate uses exactly the six tasks and participant thresholds in `spec.md`; record participant
school distribution, per-task completion/errors/corrections, Current-state answers, desktop/narrow evidence, and
durable-state evidence. Missing browser, kernel, isolated storage, or participants is a reported limitation, not an
inferred pass.

## Rules

### RULE-1 - One production workbench and existing boundaries

- Applies to: all use cases
- Constraint: The polished workbench MUST extend the existing packaged workspace's accepted model, presentation-state
  owner, Day/Week/focused renderers, native controls, and message catalog; it MUST NOT fork the accepted timetable into
  independent mode-specific models or reuse prototype fixture data, inline scripts, remote assets, Node, a bundler,
  frontend framework, or new runtime dependency. `GET /api/workspace` MUST remain the complete snapshot source for
  workbench rendering; existing run polling and proposal reads do not create a second timetable authority.
- Reason: A single identity and snapshot path prevents drift across modes and preserves the existing deployment and
  CSP contract.
- Verification: Dependency/asset inspection, network capture, and browser identity comparisons across all modes and
  focused views prove shared data and absence of added APIs or external resources.

### RULE-2 - Separate presentation transitions from durable lifecycle

- Applies to: all use cases
- Constraint: One presentation-state authority MUST derive available/default modes from the authoritative workspace
  state in `spec.md`; it MUST retain representable range, investigation, selection, focus, and scroll on mode changes,
  clear only unrepresentable context with an explanation, and reset mode, inspector, and Utilities on reload. It MUST
  persist only the existing school-scoped Day/Week and weekday preference, and MUST NOT issue a mutation request for
  mode, inspector, range, selection, focus, filter, search, or utility navigation.
- Reason: Presentation continuity must not become a parallel lifecycle or persisted source of truth.
- Verification: Browser transitions/reloads and request capture compare exact rendered contexts, storage keys/value
  shapes, workspace document, ETag/version, and absence of mutation traffic.

### RULE-3 - Lifecycle actions stay guarded by existing services

- Applies to: UC-2, UC-3, UC-4, UC-5
- Constraint: Start, stage/pin/undo/discard, solve/retry/cancel, revise/discard, and accept MUST use the existing
  service/domain transitions in `spec.md`: Accepted baseline→Repair draft→Solving repair→Repair proposal→Accepted
  baseline, plus the specified returns to Repair draft or Accepted baseline. Every other attempted transition MUST be
  refused by the service/mutation layer with `WorkspaceProblem` code `INVALID_WORKSPACE_TRANSITION` (or
  `STALE_WORKSPACE_VERSION` for a stale version, `STALE_RUN` for a superseded run, `STALE_PROPOSAL` for invalid proposal
  identity; existing `DRAFT_CONFLICT`, `DRAFT_NOT_DURABLE`, `RETRY_NOT_AVAILABLE`, and `STALE_WORKSPACE_STATE` codes
  apply to their respective solve prerequisites), not merely hidden in the UI. Refusal MUST leave accepted data and
  ineligible draft/run/proposal unchanged; the existing stale-proposal invalidation to Draft is the specified exception.
  Failed durable acceptance MUST leave
  the prior accepted bundle intact and the unchanged proposal retryable; presentation MUST NOT mark it Current.
- Reason: A visually unified shell cannot grant mutation authority or bypass atomic acceptance and recovery.
- Verification: Real-state JDBC/HTTP tests force each forbidden transition, stale version/run/proposal, canceled or
  rejected run, and failed acceptance; assert response code plus exact durable documents, version, absence of process
  launch/candidate disclosure, and correct invalidation or retry eligibility.

### RULE-4 - Draft is an overlay on accepted assignments

- Applies to: UC-2, UC-3, UC-5
- Constraint: Draft and frozen Solving MUST render the same accepted lesson IDs/placements as Current, decorated only
  with durable staged intent, direct-effect membership, conflicts, attempt-scoped period/room pins, and distinct
  persistent locks. Only an explicitly confirmed bulk preview may invoke the existing pin mutation; a canceled
  preview MUST invoke none. A conflict or unpersisted revision MUST disable repair generation without altering the
  accepted definition/result; Solving MUST expose cancellation but no draft mutation or proposal action.
- Reason: Draft is not a candidate timetable and a failed or contradictory instruction must not silently win.
- Verification: Browser and service tests compare accepted assignment IDs and bytes, overlay membership and pin
  provenance, preview/confirmation calls, conflict details, frozen controls, and absent solve/process start for
  conflict or failed save.

### RULE-5 - Derived comparison index, not a second timetable authority

- Applies to: UC-4, UC-5
- Constraint: Proposal mode MUST construct an ephemeral comparison index keyed by stable lesson ID from the accepted
  model, verified proposed definition/result, and authoritative review/change report. The accepted model MUST remain
  unchanged. A period move renders labelled origin and destination tied to one ID; a same-slot change one combined
  tile; addition only a proposed side; cancellation only an accepted side. Filtering/investigation MUST match either
  side while retaining both needed representations and identifying the matching side. Unique totals MUST use IDs and
  authoritative category/group values, never DOM tiles or sums of overlapping memberships.
- Reason: The confirmed derived-index approach gives one auditable identity join while preserving accepted authority
  and exact proposal semantics.
- Verification: Model and browser tests compare indexed IDs, two-sided fields, filter sets, zero/overlapping category
  values, origin/destination representations, and unchanged accepted snapshot with independently expected sets.

### RULE-6 - Exact safe display and honest availability

- Applies to: all use cases
- Constraint: All modes MUST use school-controlled names and definition-ordered days/periods; a missing name in a
  received presentation snapshot MUST show the stable ID plus an unavailable-name cue, never an abbreviation or
  meaning inferred from an opaque ID. An imported accepted pair with missing names or unmappable references MUST be
  refused by the verifier before reaching Current; such a pair MUST NOT be fabricated as a verified fixture. Current,
  Draft, and Solving availability MUST use accepted definitions/assignments; Proposal-side availability MUST use the
  verified proposal definition and be labelled proposed. School-controlled text and diagnostics MUST be rendered as
  inert text, never copied from the prototype as trusted HTML or logged as raw school/solver data.
- Reason: Cross-mode and before/after labels must preserve identity, confidentiality, and safe rendering.
- Verification: Opaque-ID, long-name, hostile-text, and two-sided availability fixtures prove exact labels, escaped
  DOM, correct source definition, unchanged CSP, and absence of raw-data logging. For UC-1, an invalid-name/reference
  import proves refusal and unchanged state at the actor boundary; static review confirms the defensive display
  fallback without fabricating a verified accepted pair.

### RULE-7 - Workbench presentation integration

- Applies to: all use cases
- Constraint: The workbench MUST reuse the existing Compact Day/Week whole-school structure, sticky context, focused
  return path, native forms/buttons/links, responsive read-only Day/focused agenda, styles, and message catalog.
  The desktop inspector MUST have one fixed open width and MUST NOT be resizable; collapse MUST retain selection,
  expand the canvas without a workspace request, and leave a visible summary/reopen control. Explicit contextual
  selection MUST open it. Utilities MUST contain applicable export/diagnostics only; import stays in the empty journey
  and lifecycle decisions stay in their mode context. Every new visible, accessible, status, error, and
  unavailable-name string MUST be in the existing English message catalog, and every state/effect MUST have a
  non-color cue.
- Reason: The polish changes hierarchy without removing accepted inspection semantics or creating inaccessible or
  untranslated parallel presentation.
- Verification: Packaged real-browser desktop/narrow/pointing-device and native-keyboard checks inspect sticky table,
  inspector width/collapse, focus return, disclosure/actions, catalog coverage, non-color labels, and no mutation on
  collapse; static review checks strings and safe controls.

### RULE-8 - Existing route protection remains complete

- Applies to: all use cases
- Constraint: The full route-to-access table above MUST remain unchanged. Browser code MUST NOT add an endpoint,
  disclose a candidate before independent verification, place school documents or secrets in browser storage, relax
  loopback/Host/Origin/CORS/CSRF/`If-Match`/security-header protections, or expose mutation actions on narrow screens.
  The export utility MUST respect the existing Accepted-baseline-only service refusal.
- Reason: Reorganizing controls or the prototype's external resources must not expand the local security surface.
- Verification: Complete route/method and hostile Host/Origin/CSRF/stale-version HTTP matrix, CSP/storage audit,
  network capture, and narrow-browser checks prove denial and absence of disclosure, archive, write, or process start.

### RULE-9 - Normative validation data is exact and isolated

- Applies to: all use cases
- Constraint: The normative snapshot and six administrator tasks in `spec.md` MUST be the sole feature validation
  source. Fresh isolated database fixtures MUST meet every declared cardinality and content condition by value,
  include no undeclared data relative to their generated/anonymized definition/result, and verify the accepted pair
  before use. Tests MUST NOT read/write production school data, a runtime workspace, or tracked generated files.
  No account or credential seeding is introduced.
- Reason: Illustrative prototype records and small sample data cannot prove whole-school fidelity or real failure
  boundaries.
- Verification: Fresh-database aggregate/document comparisons check full IDs/fields, ordering, absence of extras,
  verified revisions and provenance, scale bounds, and all specified special-case outcomes; isolation checks inspect
  data source, temporary paths, and unchanged tracked files.

### RULE-10 - Inspect and stage at the actor boundary

- Applies to: UC-1, UC-2
- Constraint: Automated verification MUST drive UC-1 and UC-2 main scenarios, extensions, guarantees, and minimal
  postconditions in a real browser against packaged assets and isolated durable storage. UC-2 MUST begin from UC-1's
  rendered accepted postcondition and use the same lesson identity, range, selection, and focused-return paths. Tests
  MUST cover empty and narrow views, preference fallback, selected context, individual/bulk protection, no-effect
  intent, conflict and save failure, reload and discard, and export failure; UC-1 extension 1a MUST exercise a
  rejected accepted-pair import from the browser and prove no accepted inspector or durable state change. Every
  consequential step MUST compare exact durable state and visible result.
- Reason: A separate draft page or mocked inspection component cannot establish the extending/required relationship.
- Verification: Browser journey assertions and database before/after snapshots prove the shared production path,
  denied solves, unchanged accepted bundle, confirmed-only pinning, and absence of export on failure.

### RULE-11 - Solve only from the durable Draft postcondition

- Applies to: UC-3, UC-5
- Constraint: Automated verification MUST start UC-3 from UC-2's real persisted, conflict-free Draft postcondition,
  invoke the packaged kernel boundary, and test main run/proposal flow, cancellation, restart recovery, no-feasible
  outcome, failed/rejected or stale output, diagnostics, allowed retry, responsive Current/frozen Draft inspection,
  mode switching, and narrow read-only behavior. It MUST prove a failed outcome neither creates nor renders a proposal
  and that mode changes neither cancel nor restart the run.
- Reason: A synthetic proposal or stubbed lifecycle would conceal state and process-boundary errors.
- Verification: Real-browser/HTTP/process and isolated database evidence checks each run ID, process invocation,
  candidate absence, frozen controls, accepted/draft bytes, lifecycle state, and mode at each step.

### RULE-12 - Review consumes only verified proposal postconditions

- Applies to: UC-4, UC-5
- Constraint: Automated verification MUST begin UC-4 with UC-3's independently verified feasible proposal and assert
  every combined-overlay shape, exact before/after dimension, one-sided match, unique/category/direct/ripple/protection
  count, missing metadata, unchanged lesson, and narrow agenda. It MUST exercise revise, discard, stale refusal,
  failed acceptance, and explicit successful acceptance via real HTTP/JDBC boundaries, including absence of partial
  accepted advancement and a retryable unchanged proposal after durable failure.
- Reason: Visible proposal plausibility does not establish identity, grouping, rollback, or Current-state honesty.
- Verification: Browser selection and independent ID/field calculations join to exact database snapshots and
  acceptance responses before/after every decision; refused actions prove absence of unauthorized writes or candidate
  disclosure.

### RULE-13 - Full repair reuses included production paths

- Applies to: UC-5
- Constraint: Automated verification MUST run UC-5's whole real-browser UC-1→UC-2→UC-3→UC-4 journey through their
  existing production paths, assert the accepted revision and whole-school context after each consequential step,
  cover conflict, run-failure, revise/discard, stale/failed acceptance alternatives, and start a second repair from the
  newly accepted baseline without previous attempt-scoped pins. The complete shared browser regression MUST pass; the
  `timetable-inspection-ux` UC-2 blocker MUST be resolved before claiming convergence.
- Reason: A stitched report or isolated per-mode tests cannot prove the included and required postconditions or
  continuity across lifecycle boundaries.
- Verification: End-to-end browser, kernel-process, and database assertions compare exact identity, state, revisions,
  pins and context at each handoff and after the second repair; record full regression results and prerequisite status.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-6, RULE-7, RULE-8, RULE-9, RULE-10 |
| UC-2 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-6, RULE-7, RULE-8, RULE-9, RULE-10 |
| UC-3 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-6, RULE-7, RULE-8, RULE-9, RULE-11 |
| UC-4 | RULE-1, RULE-2, RULE-3, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9, RULE-12 |
| UC-5 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9, RULE-11, RULE-12, RULE-13 |

## Design exclusions

- No redesign of initial import/planning/proposal, new API or storage shape, lifecycle or solver change, or second
  timetable authority.
- No inferred cohorts/groups, primary-room policies, direct drag-and-drop mutation, hosted access, custom matrix
  keyboard system, persisted mode/inspector state, mobile mutation, or second full proposal canvas.
- No prototype CDNs, scripts, literal fixture labels, or separate production entry point; no new runtime dependency or
  feature-specific timing threshold.