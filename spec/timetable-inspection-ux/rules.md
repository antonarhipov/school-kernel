# Technical Rules: Timetable Inspection UX

## Design overview

The existing Spring Boot workspace continues to deliver one complete accepted-workspace snapshot to dependency-free
native ES modules. Browser code converts that snapshot once into one immutable, indexed inspection model; separate
presentation-state and Day, Week, and focused-schedule components consume the same stable lesson identities. Week uses
a complete native table DOM with class rows, weekday columns, and definition-ordered period slots. It is not paginated,
virtualized, or backed by a new server query.

The only new persistence is a bounded, versioned `localStorage` preference containing Day/Week and weekday for one
authoritative school ID. No workspace route, database object, lifecycle transition, or kernel contract changes. All
display text remains in the existing message catalog, and all accepted-definition values are rendered as untrusted
text. The feature has one Compact density and targets pointing-device operation; it adds no custom keyboard-navigation
system. Existing loopback, same-origin, CSP, PostgreSQL, packaging, and real-browser verification boundaries remain in
force.

## Codebase alignment

- The implementation stays in the existing `timetable-workspace` Maven module and consumes the existing complete
  `GET /api/workspace` snapshot. It does not add a backend read model, controller route, migration, or kernel call.
- Java 25, Spring Boot 4.1.1, PostgreSQL 18.6, Flyway, `JdbcClient`, Testcontainers 2.0.5, native HTML/CSS/ES modules,
  and the packaged real-browser boundary remain inherited. Node, npm, a frontend framework, a bundler, and a browser
  build step remain excluded.
- The current message catalog, native controls, whole-school table, sticky header/class-column vocabulary, lesson
  inspector, and class/teacher/room focused schedules are extended rather than replaced by parallel components.
- This successor deliberately replaces the accepted density toggle with one Compact density and does not require a
  dedicated keyboard-only path. Native browser keyboard behavior may remain, but custom grid shortcuts and keyboard-
  only verification are not part of this feature.
- Previously recorded UC-3 latency diagnostics may continue to run as regression information, but this feature adds no
  comparison, percentile, latency threshold, or timing-evidence requirement.

## Security surface

This feature adds no route and changes no access decision. The complete workspace surface remains:

| Route and method | Access and protection | Feature relevance |
|---|---|---|
| `GET /` | Local loopback only; no CSRF; redirects to `/workspace/` | Existing entry point |
| `GET /workspace/**` | Local loopback only; static same-origin resources; security headers | Serves the inspection UI and assets |
| `GET /api/csrf` | Local loopback only; no school data | Existing mutation support; inspection does not use it |
| `GET /api/workspace` | Local loopback only; `ETag`, `Cache-Control: no-store`, safe headers | Sole inspection data source |
| `POST /api/import` | Local same-origin; CSRF and matching `If-Match` | Existing import; unchanged |
| `POST /api/initial-draft/replace` | Local same-origin; CSRF and matching `If-Match` | Existing draft replacement; unchanged |
| `POST /api/repair-draft` | Local same-origin; CSRF and matching `If-Match` | Existing repair start; unchanged |
| `PATCH /api/repair-draft` | Local same-origin; CSRF and matching `If-Match` | Existing repair intent mutation; unchanged |
| `DELETE /api/repair-draft` | Local same-origin; CSRF and matching `If-Match` | Existing draft discard; unchanged |
| `POST /api/repair-draft/bulk-pin-preview` | Local same-origin; CSRF and matching `If-Match`; non-mutating preview | Existing preview; unchanged |
| `POST /api/runs` | Local same-origin; CSRF and matching `If-Match` | Existing initial/repair run start; unchanged |
| `GET /api/runs/{runId}` | Local loopback only; active run identity required | Existing polling; unchanged |
| `DELETE /api/runs/{runId}` | Local same-origin; CSRF and matching `If-Match` | Existing cancellation; unchanged |
| `GET /api/proposal` | Local loopback only; `ETag`, `Cache-Control: no-store` | Existing proposal read; unchanged |
| `POST /api/proposal/accept` | Local same-origin; CSRF and matching `If-Match` | Existing acceptance; unchanged |
| `DELETE /api/proposal` | Local same-origin; CSRF and matching `If-Match` | Existing proposal discard; unchanged |
| `GET /api/accepted/export` | Local loopback only; accepted data only; `Cache-Control: no-store` | Existing export; unchanged |
| `/login`, `/logout`, `/actuator/**`, every other route or method | Denied or not exposed | No authentication or accidental new surface |

The listener remains loopback-only. Host and Origin checks, session-bound CSRF for mutations, disabled CORS and
forwarded-host trust, same-origin referrer policy, and the existing CSP remain unchanged. `localStorage` contains only
the bounded presentation preference defined by RULE-5; it contains no timetable, display name, credential, token,
session identifier, or accepted/draft/proposal document.

## Verification strategy

Automated verification uses the existing isolated PostgreSQL 18.6 Testcontainers boundary, ephemeral loopback HTTP
server, packaged workspace assets, and real Chrome control. It does not use a developer database, runtime workspace,
mocked browser DOM, Node-based runner, or tracked generated output. Each browser context starts with explicit
`localStorage`, and tests remove that state after the journey.

UC-1 has a real-browser main journey plus empty timetable, invalid/missing/blocked preference, Day/Week selection,
manual Day exclusion, off-viewport selection, and narrow-screen extensions. UC-2 covers subject-only, teacher-only,
combined, availability ribbon, zero-match, clear-one, and intersected-filter behavior. UC-3 covers search, each filter,
intersections, empty results, reset, all focused schedules, retained return context, and narrow read-only behavior.
Every consequential step asserts visible state, represented counts, selection identity, and the exact unchanged durable
workspace document and version.

Relationship journeys start from UC-1's real rendered postcondition and invoke UC-2 and UC-3 through the same production
model, state, and renderers. Route/security regression tests cover every row above. After automated verification, five
timetable professionals from at least three schools perform the exact pointing-device tasks in `spec.md`; no timing
sample or performance threshold is required.

## Rules

### RULE-1 - Dependency-free browser component boundaries

- Applies to: all use cases
- Constraint: The feature MUST use native HTML, CSS, and ES modules with no Node, npm, transpiler, bundler, frontend
  framework, client state library, or additional runtime dependency. Browser responsibilities MUST be separated into
  one immutable accepted-snapshot model, one presentation-state owner, and Day, Week, and focused-schedule renderers
  that consume those shared objects. A renderer MUST NOT create a second timetable model or mutate accepted snapshot
  data.
- Reason: The new Week surface and investigations add enough interaction state that extending one monolithic renderer
  would make identity, transition, and non-mutation guarantees difficult to validate.
- Verification: Dependency and packaged-asset inspection proves the exclusions; architecture checks and browser
  journeys prove every renderer consumes the same lesson identities and that accepted snapshot data is unchanged.

### RULE-2 - One complete snapshot and indexed inspection model

- Applies to: all use cases
- Constraint: `GET /api/workspace` MUST remain the sole school-data read for inspection. After each authoritative
  snapshot load, browser code MUST build its lookup and grouping indexes once from the complete accepted definition and
  result, including stable lesson identity, class/day/period placement, subject, teacher, room, and availability. Day,
  Week, search, filters, counts, teacher ribbon, inspector, and focused schedules MUST derive from that model. The
  feature MUST NOT add server-side timetable filtering, pagination, projection routes, JSONB queries, or a relational
  display read model.
- Reason: One shared client model prevents Day/Week drift, keeps counts independent of rendered elements, and preserves
  the accepted workspace's established process and persistence boundaries.
- Verification: Network capture proves no extra data route is called; snapshot-fidelity and scale-fixture tests compare
  every displayed identity, ordering, grouping, count, and availability classification with the source documents.

### RULE-3 - Complete semantic Week table with one Compact density

- Applies to: UC-1
- Constraint: The desktop Week renderer MUST use a native semantic table whose rows are all definition-declared classes
  and whose columns are all definition-ordered weekdays; each class-day cell MUST contain every definition-ordered
  period slot. It MUST render the complete table DOM after snapshot load and MUST NOT virtualize, paginate, collapse
  empty periods, or request partial rows. Headers and the class-name column MUST use the existing sticky table
  conventions. Day and Week MUST use one Compact lesson-tile density and MUST NOT expose a density or custom zoom
  control.
- Reason: The confirmed validation scale is small enough for a complete DOM, while native table relationships make the
  represented population, sticky context, and empty structure more reliable than custom virtualization or ARIA
  reconstruction.
- Verification: Real-browser DOM assertions at the normative scale compare table row, weekday, period-slot, empty-slot,
  and unique lesson counts by value; scroll journeys prove sticky context; asset and browser checks prove no density
  control or virtualization path exists.

### RULE-4 - One presentation-state transition authority

- Applies to: all use cases
- Constraint: One browser presentation-state component MUST enforce every transition in `spec.md`, including Week/Day
  selection retention, Week-to-Day weekday selection, manual Day exclusion, intersected filter modes, clear-one,
  filter reset, focused-view return, reload reset, and narrow-screen surface selection. Renderers MUST emit intent to
  that component and MUST NOT change presentation state independently. An invalid event or value MUST leave current
  presentation state unchanged; an invalid persisted preference MUST produce the specified Week/first-weekday fallback.
  No presentation transition may issue a workspace mutation.
- Reason: Range, highlights, filters, selection, and focused schedules interact; scattered state writes would make the
  specified transition model and failure guarantees non-deterministic.
- Verification: Deterministic state-transition tests cover every allowed and refused edge, and real-browser tests assert
  resulting range, weekday, selection, criteria, surface, network activity, and unchanged durable workspace state.

### RULE-5 - Minimal untrusted local preference

- Applies to: UC-1
- Constraint: The only locally persisted feature value MUST be one JSON record under a versioned key namespaced by the
  authoritative school ID. The record MUST contain exactly version `1`, range `WEEK` or `DAY`, and one opaque weekday
  ID, and its serialized size MUST NOT exceed 1 KiB. Load MUST validate the record shape, enum, school key, and weekday
  membership against the accepted definition. Missing, blocked, malformed, oversized, stale-version, unknown-school,
  or unknown-weekday data MUST be ignored without blocking rendering. The feature MUST NOT store filters, highlights,
  selection, density, scroll position, display names, assignments, availability, or any workspace document locally.
- Reason: `localStorage` satisfies the confirmed device-local preference without creating a second school-data store or
  trusting mutable browser content.
- Verification: Real-browser tests cover valid per-school restore plus every invalid/failure class, inspect storage by
  key and field value, enforce the bound, prove cross-school isolation, and compare the workspace snapshot and database
  before and after.

### RULE-6 - Stable lesson identity across renderers

- Applies to: UC-1, UC-2, UC-3
- Constraint: Lesson selection, highlighting, filtering, counts, inspector content, and focused schedules MUST use the
  accepted lesson ID as their shared identity. DOM position, array index, display text, class/day/period coordinates,
  or rendered element identity MUST NOT become lesson identity. Rebuilding or switching a renderer MUST restore state
  only when that lesson remains represented and MUST clear it through the specified transition otherwise.
- Reason: Week and Day create different DOM shapes for the same accepted assignments; positional identity would select
  the wrong lesson or inflate counts after range and filter changes.
- Verification: Browser journeys select lessons with duplicate display text, switch every surface and range, apply and
  clear filters, and compare the inspector and unique counts with stable IDs from the accepted snapshot.

### RULE-7 - Model-derived highlighting, filtering, and counts

- Applies to: UC-2, UC-3
- Constraint: Exact subject and teacher selections MUST match stable IDs. Search MUST examine only the authoritative
  metadata and technical identities permitted by `spec.md`. Filters MUST be evaluated as set intersections over the
  indexed model; counts MUST be computed from unique accepted lesson IDs in the represented model result and MUST NOT
  be inferred from visible, hidden, duplicated, or off-viewport DOM elements. Highlight mode MUST retain nonmatches and
  MUST NOT apply default dimming; `Show only matches` MUST be the only subject/teacher action that narrows population.
- Reason: DOM-based counting and filtering would make full-DOM layout details part of product semantics and would drift
  when Day, Week, or focused renderers differ.
- Verification: Deterministic model checks and real-browser paths compare subject, teacher, dual-match, search, single-
  filter, intersected-filter, zero-result, and reset outputs with independently calculated ID sets.

### RULE-8 - Authoritative teacher availability classification

- Applies to: UC-2
- Constraint: The teacher ribbon MUST classify each represented period from the accepted definition and assignments.
  An accepted assignment for the teacher yields `Assigned`; otherwise a period allowed by `availablePeriodIds` yields
  `Available · unassigned`; every other period yields `Unavailable`. An omitted `availablePeriodIds` MUST use the
  kernel-v1 meaning of every declared period being available. The implementation MUST NOT infer availability from empty
  class cells, undesirable periods, room availability, or absence from the rendered viewport.
- Reason: An empty class cell is not a teacher calendar, and the kernel contract already defines the authoritative
  availability default.
- Verification: Model and real-browser tests cover explicit availability, omitted availability, assigned available
  periods, available gaps, unavailable gaps, Week/Day range changes, active filters, and zero accepted assignments.

### RULE-9 - Safe authoritative presentation and localization

- Applies to: all use cases
- Constraint: Every user-visible string introduced by the feature MUST come from the existing English message catalog.
  Definition display names and IDs MUST be treated as untrusted text and inserted through text-safe DOM operations or
  the existing escaping boundary, never as executable markup. Day tiles MUST visibly show subject, teacher, and room;
  Week tiles MUST visibly show subject and lower-right room and MUST retain the teacher in their complete accessible
  label and inspector. Full names MUST remain available when visually clamped. The UI MUST NOT derive abbreviations,
  weekday/order meaning, or labels from opaque IDs and MUST NOT display inactive or inferred group controls.
- Reason: Complete accepted snapshots contain school-controlled text, while one catalog and one escaping boundary avoid
  inconsistent language and injection across the new renderers.
- Verification: Message-catalog architecture checks reject inline production copy; hostile-text browser fixtures prove
  inert rendering and CSP preservation; exact-content tests cover clamping, inspector fidelity, opaque IDs, room
  placement, teacher visibility, and absence of group UI.

### RULE-10 - Pointing-device scope without custom keyboard machinery

- Applies to: all use cases
- Constraint: Every scenario action MUST be exposed through a visible pointing-device control using native links,
  buttons, inputs, selects, or table content as appropriate. The feature MUST NOT introduce roving `tabindex`, custom
  arrow-key grid navigation, application keyboard shortcuts, or a keyboard-only completion gate. Native control
  semantics and incidental browser keyboard behavior MUST NOT be disabled. Labels, accepted/narrowed state, selection,
  highlights, and teacher availability MUST remain distinguishable without color alone.
- Reason: The confirmed feature scope targets pointing-device administrators while retaining safe native semantics and
  avoiding an unrequested large-grid keyboard interaction system.
- Verification: Real-browser pointing-device journeys cover every main scenario and extension; asset inspection rejects
  custom keyboard handlers for matrix navigation; semantic and non-color state assertions cover the required labels and
  cues without treating keyboard-only operation as a pass condition.

### RULE-11 - No backend, database, lifecycle, or kernel delta

- Applies to: all use cases
- Constraint: This feature MUST NOT add or change a controller route, API response shape, database table or migration,
  workspace JSON field, lifecycle state or transition, ETag rule, kernel schema, kernel command, solver behavior, or
  accepted/draft/proposal mutation. Inspection browser code MUST perform no mutating request. Local preference storage
  under RULE-5 is the only new persisted value and is not workspace authority.
- Reason: The specification is presentation-only, and expanding an authoritative boundary here would couple the first
  UX slice to later repair, grouping, or policy features.
- Verification: Route and schema inventory diffs, Flyway validation, network capture, architecture checks, and exact
  database/document/version comparisons prove the absence of every prohibited delta.

### RULE-12 - Existing local security contract remains complete

- Applies to: all use cases
- Constraint: The application MUST retain the complete route-to-access table above, loopback binding, strict Host and
  Origin enforcement, disabled CORS and forwarded-host trust, session-bound CSRF on every mutation, no login/logout or
  management surface, `Cache-Control: no-store` for school data, `nosniff`, same-origin referrer policy, and the existing
  same-origin-only CSP. Inspection assets MUST use no inline script/style, remote resource, dynamic code evaluation, or
  browser storage containing school data or secrets.
- Reason: A presentation-only feature can still expand the attack surface through new assets, unsafe rendering, remote
  dependencies, or local storage even when it adds no API.
- Verification: The complete real-HTTP security matrix reruns every route and method, headers, hostile Host/Origin,
  preflight, traversal, login/logout/actuator denial, CSP, secret scanning, and browser-storage inspection.

### RULE-13 - UC-1 is verified at the accepted-workspace browser boundary

- Applies to: UC-1
- Constraint: Automated verification MUST drive UC-1's main scenario and every extension through the real packaged
  browser assets over the running workspace with a verified accepted snapshot. It MUST assert Week default/restore,
  complete table structure, Day structure, fixed Compact density, authoritative ordering and labels, selection
  transitions, empty timetable, invalid and blocked preference storage, off-viewport selection, pointer controls,
  narrow read-only behavior, and exact unchanged durable state after every consequential step.
- Reason: Component or static-markup tests cannot prove browser storage, DOM population, sticky context, responsive
  behavior, or presentation-only non-mutation at the actor boundary.
- Verification: One isolated real-browser suite records the visible and DOM assertions named above, network requests,
  storage values, console failures, and full database JSON/version comparisons; a human performs the corresponding
  UC-1 walkthrough afterward.

### RULE-14 - UC-2 is verified through the shared UC-1 production path

- Applies to: UC-2
- Constraint: Automated verification MUST begin from UC-1's real rendered success postcondition and exercise subject-
  only, teacher-only, combined, clear-one, clear-all, range change, zero-match, single-filter, intersected-filter, and
  filtered-selection paths. It MUST compare unique ID sets and all three teacher-ribbon states with an independent
  expected calculation and MUST prove no alternative snapshot model, renderer, route, or durable mutation is used.
- Reason: UC-2 extends UC-1; a standalone highlight mock would not prove the relationship or the authoritative
  availability semantics.
- Verification: Real-browser and deterministic-model evidence compare cues, counts, ribbon classifications, active
  criteria, selection behavior, network activity, and exact before/after workspace state, followed by the UC-2 human
  walkthrough tasks from `spec.md`.

### RULE-15 - UC-3 is verified through the shared UC-1 production path

- Applies to: UC-3
- Constraint: Automated verification MUST begin from UC-1's real rendered success postcondition and exercise search
  highlight, every applicable filter, intersections, empty search, empty filtered population, filter reset, selected
  result, class/teacher/room focused schedules, empty focused schedule, retained return context, off-viewport result,
  and narrow read-only behavior. It MUST prove that a narrowed or focused view is never labelled complete and that no
  alternate data or mutation path is introduced.
- Reason: UC-3 extends UC-1 and preserves approved inspection behavior; its honesty and return-context guarantees exist
  only in the integrated browser journey.
- Verification: Real-browser evidence compares represented IDs, active criteria, complete/narrowed labels, focused
  contents, restored state, network activity, and exact durable state after each consequential action, followed by the
  UC-3 human walkthrough tasks from `spec.md`.

### RULE-16 - Normative fixture and walkthrough gate are exact

- Applies to: all use cases
- Constraint: The normative validation data and six participant tasks in `spec.md` MUST be the single source for this
  feature's scale fixture and human gate. Automated fixtures MUST satisfy every cardinality and content condition by
  value and MUST use a complete verified accepted definition/result pair. Tests MUST use isolated generated or properly
  anonymized data and MUST NOT read or modify a runtime workspace or tracked generated file. Approval MUST require five
  timetable professionals from at least three schools, at least four of five completing every task without serious
  error or facilitator correction, and all five identifying `Current · accepted` throughout. No completion-time,
  percentile, latency, or comparative performance evidence may be required by this feature.
- Reason: Functional completeness and honest current-state comprehension are the confirmed gates; timing proof was
  explicitly removed from scope.
- Verification: Fixture checks report cardinalities, definition/result verification, anonymization or generation
  provenance, and task coverage; the walkthrough record reports participant school distribution, per-task result,
  errors, corrections, and accepted-state answers without timing fields.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-5, RULE-6, RULE-9, RULE-10, RULE-11, RULE-12, RULE-13, RULE-16 |
| UC-2 | RULE-1, RULE-2, RULE-4, RULE-6, RULE-7, RULE-8, RULE-9, RULE-10, RULE-11, RULE-12, RULE-14, RULE-16 |
| UC-3 | RULE-1, RULE-2, RULE-4, RULE-6, RULE-7, RULE-9, RULE-10, RULE-11, RULE-12, RULE-15, RULE-16 |

UC-2 and UC-3 MUST consume UC-1's production model, presentation state, and rendered postcondition through their
extension points. They MUST NOT duplicate snapshot loading, accepted-model construction, range state, or lesson
identity.

## Design exclusions

- New backend projection/filter endpoints, server-side timetable queries, relational display models, database
  migrations, and workspace JSON changes are excluded.
- Virtualization, pagination, lazy row loading, infinite scrolling, Web Workers, Canvas rendering, SVG timetable
  rendering, and WebAssembly are excluded.
- Node, npm, a frontend framework, bundling, transpilation, service workers, offline caching, remote assets, and CDN
  dependencies are excluded.
- Comfortable/Compact switching, custom application zoom, persisted density, and responsive density modes are
  excluded; the timetable has one Compact density.
- Custom matrix keyboard navigation, roving focus, application shortcuts, and keyboard-only verification are excluded.
- Draft/Proposal canvas integration, split groups, new kernel versions, primary-room policies, and every other sibling
  UX-evolution feature are excluded.
- Timing comparison, latency thresholds, percentile gates, and required timing evidence are excluded.

## External dependencies

- Automated durable-state verification requires the repository's Docker-compatible PostgreSQL test environment. If it
  is unavailable, the suite must report the environmental failure rather than substitute another database or report a
  pass.
- Administrator approval requires the five timetable professionals from at least three schools and the complete
  generated or properly anonymized fixture defined by `spec.md`. Until both are available, technical convergence may be
  recorded but the feature cannot be administrator-approved.
