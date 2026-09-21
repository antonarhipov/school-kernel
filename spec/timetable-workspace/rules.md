# Technical Rules: School Timetable Operations Workspace

## Design overview

The repository becomes a Java 25 Maven reactor with a schema-and-fixture-only public kernel-contract module, the
headless kernel CLI, and a Spring Boot workspace application. The workspace serves native HTML, CSS, and ES modules,
persists one versioned workspace aggregate in PostgreSQL through `JdbcClient`, and applies Flyway migrations. It invokes
the packaged kernel only through explicit file-and-process commands: `verify` for imports, `plan` for initial
proposals, and definition-aware `replan` for repairs.

Every workspace mutation is a PostgreSQL transaction guarded by the lifecycle state and an HTTP `If-Match` version.
Solves run in one background process slot; start returns a run identifier, polling exposes state, and cancellation
terminates only that run. Kernel files and results remain authoritative. The browser receives one complete workspace
snapshot and performs search, filtering, grouping, and timetable rendering locally at the validation scale.

Spring Security provides same-origin protection without login. The application and development database bind only to
loopback interfaces. Spring Boot owns a repository-root Docker Compose PostgreSQL lifecycle for local JVM startup and
walkthroughs; authentication, roles, remote deployment, and production packaging remain later decisions.

## Codebase alignment

- The existing Java 25 kernel, schemas, solver boundaries, canonical revisions, atomic CLI publication, and integration
  tests remain the scheduling authority. Moving them into reactor modules must not change their public behavior.
- The current dependency-free timetable viewer supplies presentation conventions and focused-view behavior. The
  workspace extends those conventions with native ES modules; it does not introduce Node, npm, a frontend framework,
  or a browser build step.
- Spring Boot 4.1.1 is the pinned workspace baseline. PostgreSQL 18.6 is the development and integration-test database
  baseline, Testcontainers 2.0.5 is the test baseline, and PostgreSQL JDBC 42.7.13 is the pinned driver baseline.
- The kernel retains its separately pinned Timefold, Jackson, schema-validation, Picocli, canonicalization, and logging
  baseline. Spring dependency management must not silently change kernel runtime dependencies. Tested stable upgrades
  are allowed only through an explicit version change and the complete kernel compatibility suite.
- During development, the workspace runs as a local JVM process and Spring Boot Docker Compose support starts and
  stops the repository-root PostgreSQL service with it. This local walkthrough lifecycle does not select a production
  application image, installer, hosted deployment, or production database topology.

## Security surface

There are no accounts, login page, logout action, roles, or remotely accessible routes. Spring Security permits the
listed local routes without authentication, creates an HTTP session only for CSRF protection, and denies or leaves
unmapped every route not listed. The HTTP listener binds to loopback only; accepted `Host` values are the configured
port on `localhost`, `127.0.0.1`, and `[::1]` only when IPv6 loopback is enabled. Forwarded host headers are not trusted.
No CORS origin is allowed.

| Route and method | Access and protection | Purpose |
|---|---|---|
| `GET /` | Local, no CSRF; redirects only to `/workspace/` | Stable entry point |
| `GET /workspace/**` | Local, no CSRF; static allowlist and security headers | HTML, CSS, ES modules, and same-origin assets |
| `GET /api/csrf` | Local, no CSRF; no school data | Obtain the Spring Security CSRF token for the current local session |
| `GET /api/workspace` | Local, no CSRF; emits current `ETag` | Complete workspace snapshot used by the browser |
| `POST /api/import` | Local, CSRF and `If-Match` required | Import an initial definition or accepted bundle |
| `POST /api/initial-draft/replace` | Local, CSRF and `If-Match` required | Replace an unaccepted initial definition |
| `POST /api/repair-draft` | Local, CSRF and `If-Match` required | Start a repair draft |
| `PATCH /api/repair-draft` | Local, CSRF and `If-Match` required | Stage availability, apply or undo pins, and update draft intent |
| `DELETE /api/repair-draft` | Local, CSRF and `If-Match` required | Explicitly discard a repair draft |
| `POST /api/repair-draft/bulk-pin-preview` | Local, CSRF and `If-Match` required; no mutation | Calculate the exact preview snapshot against one workspace version |
| `POST /api/runs` | Local, CSRF and `If-Match` required | Start initial planning, repair, or the allowed two-minute retry |
| `GET /api/runs/{runId}` | Local, no CSRF; active run identifier required | Poll authoritative run state and safe diagnostics |
| `DELETE /api/runs/{runId}` | Local, CSRF and `If-Match` required | Cancel the active run only |
| `GET /api/proposal` | Local, no CSRF; emits current `ETag` | Read the current initial or repair proposal |
| `POST /api/proposal/accept` | Local, CSRF and `If-Match` required | Explicitly accept the current proposal |
| `DELETE /api/proposal` | Local, CSRF and `If-Match` required | Discard the proposal while retaining the applicable draft |
| `GET /api/accepted/export` | Local, no CSRF | Download accepted data only as the normative ZIP archive |
| `/login`, `/logout`, `/actuator/**`, all other routes | Not exposed | Authentication, management endpoints, and accidental application surface are absent |

All mutating responses use `Cache-Control: no-store`. School-data responses and exports use `Cache-Control: no-store`,
`X-Content-Type-Options: nosniff`, a same-origin referrer policy, and a Content Security Policy that permits only the
application's own scripts, styles, images, and connections. Error rendering never reflects uploaded content into HTML.

## Verification strategy

Unit tests cover lifecycle decisions, intent compilation, direct-effect calculation, pin provenance, change grouping,
canonical serialization, safe error mapping, and UI data transformations with deterministic fixtures. JDBC and HTTP
integration tests use PostgreSQL 18.6 through Testcontainers; H2, mocked JDBC behavior, and a developer database are
forbidden substitutes. Flyway is exercised against both a fresh database and every supported prior migration state.

Each use case has an outer-boundary journey through the real Spring Boot HTTP application and packaged browser assets.
Planning journeys invoke the packaged kernel process with real temporary files. Import, acceptance, restart, stale
version, cancellation, and failure tests assert the database snapshot before and after every consequential action.
Negative tests assert the response plus absence of database mutation, candidate disclosure, export, process launch, or
late-result application as applicable.

Browser automation covers the complete desktop journey and narrow-screen read-only boundary; a human walkthrough
follows the use-case scenarios after automated checks. The realistic scale fixture measures browser interactions
separately from solver time. Tests use isolated containers, temporary directories, and ephemeral ports and never modify
tracked files, example data, or a runtime workspace.

## Rules

### RULE-1 - Reactor modules preserve the process boundary

- Applies to: all use cases
- Constraint: The build MUST contain separate public-contract resource, kernel CLI, and workspace application modules
  under one Maven reactor. The workspace MUST NOT compile against kernel application, domain, solver, or CLI classes;
  it MAY consume the resource-only public schemas, fixtures, and documentation and MUST invoke kernel behavior through
  the packaged executable. The public-contract artifact MUST contain no compiled Java API or implementation class.
- Reason: The workspace is a client of the stable file-and-CLI contract, while one canonical schema source prevents
  duplicated contract files from drifting.
- Verification: Maven dependency analysis proves the allowed graph; archive inspection proves the contract artifact
  contains no `.class` file; ArchUnit inspects every compiled production class and rejects kernel implementation
  packages on the workspace classpath; clean packaging produces independently runnable kernel and workspace artifacts.

### RULE-2 - Pinned Java, Spring, and PostgreSQL baseline

- Applies to: all use cases
- Constraint: The workspace MUST use Java 25, Spring Boot 4.1.1, PostgreSQL 18.6, PostgreSQL JDBC 42.7.13,
  Testcontainers 2.0.5, Flyway, Spring `JdbcClient`, Spring MVC, and Spring Security. It MUST NOT add JPA, Hibernate,
  Spring Data JDBC, H2, an embedded production database, Node, npm, a frontend framework, prerelease dependencies, or
  Timefold internal `.impl` APIs. Direct dependencies and build plugins MUST be pinned; a stable upgrade MAY replace a
  baseline only after dependency review and the complete clean compatibility suite pass.
- Reason: These confirmed choices provide one reproducible stack and avoid parallel persistence or frontend models.
- Verification: Effective-POM and dependency-tree checks compare resolved components and versions by value and prove
  the excluded dependencies are absent; a clean Java 25 reactor build succeeds.

### RULE-3 - JSONB-first singleton workspace aggregate

- Applies to: UC-1, UC-2, UC-4, UC-5, UC-6, UC-7, UC-8
- Constraint: PostgreSQL MUST store exactly one logical workspace aggregate with relational identity, lifecycle state,
  monotonically increasing version, optional active-run identifier, and one authoritative JSONB document containing
  the evolving definition, accepted result, draft, proposal, intent, manifest, and run evidence appropriate to that
  state. Data access MUST use explicit `JdbcClient` SQL; implementation MUST NOT introduce a second relational read or
  domain model for kernel documents during this increment.
- Reason: JSONB permits early schema evolution while relational guard fields make lifecycle and concurrency enforcement
  explicit.
- Verification: Fresh-database tests compare the singleton row, columns, constraints, and empty JSON document exactly;
  round-trip tests compare every document field and prove no parallel persistence store is used.

### RULE-4 - Flyway is the only schema authority

- Applies to: all use cases
- Constraint: Flyway versioned SQL migrations MUST create and evolve all database objects. Runtime schema generation
  MUST be disabled, startup MUST fail on migration or validation error, migrations MUST be forward-only and
  transactional where PostgreSQL permits, and no application credential or school data MAY be seeded. The fresh schema
  MUST contain one `EMPTY` workspace aggregate and no extra lifecycle or catalog values.
- Reason: A single migration history is required for reproducible local state and real PostgreSQL testing.
- Verification: Testcontainers tests migrate an empty database and each retained prior version, compare resulting
  objects and seed values by value, and prove startup refusal for checksum drift and failed migration.

### RULE-5 - Complete lifecycle state machine

- Applies to: UC-1, UC-2, UC-4, UC-5, UC-6, UC-7
- Constraint: The relational lifecycle value MUST be exactly one of `EMPTY`, `INITIAL_DRAFT`, `SOLVING_INITIAL`,
  `INITIAL_PROPOSAL`, `ACCEPTED_BASELINE`, `REPAIR_DRAFT`, `SOLVING_REPAIR`, or `REPAIR_PROPOSAL`. One application
  service MUST enforce exactly the transitions in `spec.md` inside transactions. Every other transition MUST return
  problem code `INVALID_WORKSPACE_TRANSITION` with HTTP `409` and MUST NOT change JSONB, increment the version, launch
  a process, disclose a candidate, or emit an export.
- Reason: HTTP routing and UI visibility are insufficient protection for accepted-state invariants.
- Verification: Transition tests establish each real source state in PostgreSQL, prove every allowed edge and side
  effect, and exhaustively attempt all refused state/action combinations with before-and-after row comparison.

### RULE-6 - Optimistic HTTP and SQL concurrency

- Applies to: UC-1, UC-2, UC-4, UC-5, UC-6, UC-7
- Constraint: Every workspace and proposal representation MUST emit a strong opaque `ETag` derived from the aggregate
  version. Every command and version-sensitive preview MUST require the matching `If-Match`; absence returns HTTP `428`
  with `PRECONDITION_REQUIRED`, and mismatch returns HTTP `412` with `STALE_WORKSPACE_VERSION`. The same version MUST
  appear in the conditional SQL update so the HTTP check and state change are one transaction. Neither refusal may
  mutate state or invoke the kernel.
- Reason: One operator can still have stale tabs or overlapping requests; server serialization alone cannot detect
  obsolete intent.
- Verification: Real HTTP tests exercise matching, missing, stale, repeated, and racing requests and assert exactly one
  winner, monotonic versions, current `ETag` disclosure, and zero prohibited side effects for losers.

### RULE-7 - Accepted-state changes are one database transaction

- Applies to: UC-1, UC-2, UC-6, UC-7
- Constraint: Importing an accepted bundle and accepting a proposal MUST atomically store the complete definition,
  result, manifest, and revisions; advance the accepted reference/state; and clear the applicable proposal, draft, run,
  and attempt-scoped pins in one PostgreSQL transaction. A failure at any injected statement or commit boundary MUST
  roll back the entire mutation and leave the previous accepted JSONB value and version unchanged.
- Reason: PostgreSQL replaces the originally considered filesystem pointer scheme and must preserve the specification's
  all-or-nothing accepted baseline.
- Verification: Integration tests inject failures before each write and at commit, terminate a database connection
  during acceptance, restart the application, and compare the complete accepted JSON tree and canonical export bytes
  with the pre-action snapshot.

### RULE-8 - Canonical JSON preserves document fidelity

- Applies to: UC-1, UC-2, UC-4, UC-5, UC-6, UC-8
- Constraint: Imported and generated kernel documents MUST be stored as lossless JSON values in JSONB and serialized at
  process and export boundaries with the kernel's RFC 8785 canonical JSON rules. Stable IDs and all contract fields
  MUST be preserved exactly. The application MUST NOT reconstruct kernel documents from display models or interpret ID
  spelling. Definition entity arrays MUST normalize by entity ID, reference sets lexically, soft overrides by
  constraint ID, and timetable assignments by lesson ID. Unicode text MUST NOT receive extra normalization, and an
  omitted default MUST remain distinct from an explicitly materialized value unless the kernel contract says
  otherwise. `inputRevision` MUST identify the complete normalized definition, including every scheduling-semantic
  field and excluding only the non-semantic values named by the kernel contract. An unmappable display value MUST
  retain its ID and explicit missing-name state.
- Reason: JSONB normalizes storage representation, so deterministic canonical boundary bytes are required for stable
  revisions, exact exports, and byte-for-byte safety assertions.
- Verification: Canonicalization vectors, JSONB round trips, key-order and whitespace permutations, Unicode IDs, and
  export/import cycles compare semantic trees, revisions, and canonical bytes by value.

### RULE-9 - Kernel verification is a public structured command

- Applies to: UC-1, UC-8
- Constraint: The kernel CLI MUST add `verify --definition PATH [--result PATH] --output PATH` with the existing
  correlation, overwrite, and safe-debug controls but no solve controls. It MUST parse and validate supported schemas
  and versions, semantic references, school identity, definition/result lineage, assignment integrity, hard validity,
  definition revision, and timetable revision without invoking the solver. A separate `verification-result-v1` schema
  MUST require `schemaVersion`, `status`, `kernelVersion`, `correlationId`, and `elapsedTimeMs`. Definition-only mode
  MUST reject `basedOnRevision`; baseline mode MUST require a complete `FEASIBLE` result whose `inputRevision` equals
  the computed complete-definition revision. Status is exactly `VERIFIED`,
  `INVALID_INPUT`, or `INTERNAL_ERROR`. `VERIFIED` additionally requires mode (`INITIAL_DEFINITION` or
  `ACCEPTED_BASELINE`), school ID, catalog version, and definition revision, plus timetable revision for a baseline;
  `INVALID_INPUT` requires the existing validation-report shape, and `INTERNAL_ERROR` requires only a safe message.
  Verification output MUST never contain a timetable or candidate assignments. The command MUST atomically publish its
  result and return `0`, `2`, or `4` for those statuses while retaining existing misuse, transport, and interruption
  exit codes. `VERIFIED` establishes that the submitted content is suitable as the named workspace mode; it MUST NOT
  claim authentic historical provenance. Only `VERIFIED` permits import or exported-bundle re-import.
- Reason: The current `plan` and `replan` commands cannot authoritatively validate an imported baseline without
  solving, and duplicating kernel validation in the workspace would violate the confirmed boundary.
- Verification: Packaged-process tests cover both valid modes and every invalidity named by UC-1, assert zero solver
  construction/invocation, validate every status and conditional schema field, check exit codes and atomic publication,
  reject any provenance claim, and feed successful output through the real workspace import route.

### RULE-10 - Process-only planning integration

- Applies to: UC-2, UC-5, UC-7
- Constraint: The workspace MUST invoke the packaged kernel with `ProcessBuilder` argument lists and explicit paths,
  never through a shell or in-process Java call. Initial planning MUST use `plan`; repair MUST use `replan` with the
  exact argument shape `--current-definition PATH --current PATH --definition PATH --output PATH`, supplying the
  immutable accepted parent definition, its matching accepted result, and a complete compiled successor definition.
  Inputs, output, exit code, and output schema and identities MUST all be verified before a proposal transaction is
  attempted.
- Reason: Process isolation preserves the approved kernel contract and prevents Spring dependencies or workspace state
  from changing solver behavior.
- Verification: Integration tests use the packaged executable, record exact arguments and files, reject shell
  metacharacter injection, and prove that malformed, missing, stale, or mismatched outputs never become proposals.

### RULE-11 - One asynchronous run with polling

- Applies to: UC-2, UC-5, UC-7
- Constraint: `POST /api/runs` MUST transactionally invalidate an older proposal, record a unique run ID and
  `SOLVING_*` state, and return HTTP `202` before executing the kernel on one dedicated background executor. At most one
  run may be active. Polling MUST read durable run state. A result may be applied only by a conditional transaction
  matching run ID, source version, state, accepted revisions, and intent revision; late or duplicate completion MUST be
  discarded without mutation.
- Reason: Long solves must not block navigation, and stale completion must never resurrect an invalidated proposal.
- Verification: Real HTTP/process tests cover active-run conflict, polling, navigation during solve, rerun invalidation,
  duplicate completion, completion after cancel, and state change during completion; database assertions prove only the
  exact active run can write a proposal.

### RULE-12 - Bounded execution and cancellation

- Applies to: UC-2, UC-5
- Constraint: The normal run MUST pass a 30-second kernel time limit; only an unchanged unsuccessful draft may request
  the two-minute preset. The UI and API MUST NOT expose seed or step controls. Cancellation MUST target the active run,
  request graceful process termination, wait no more than two seconds, then force termination if necessary. A watchdog
  MUST end a process no later than ten seconds beyond its kernel limit. Cancelled, timed-out, or forcibly ended runs
  MUST return to the applicable draft with no proposal.
- Reason: The workspace needs bounded resource use and a reliable cancel path even if the child process does not exit
  cooperatively.
- Verification: Controllable child-process doubles and real packaged-process tests exercise graceful and forced cancel,
  watchdog expiry, exact presets, rejected retry after intent change, absence of forbidden controls, and late output
  suppression.

### RULE-13 - Private and bounded process files

- Applies to: UC-1, UC-2, UC-5, UC-8
- Constraint: Each kernel invocation MUST use a newly created owner-only temporary directory containing only explicit
  input and output files. The workspace MUST cap captured stdout and stderr at 64 KiB each, pass a correlation ID, never
  pass `--debug`, never return raw stderr to the browser, and remove temporary artifacts after completion or recovery.
  Existing accepted or imported files MUST never be used as a kernel output path.
- Reason: School data and child-process diagnostics must not escape the local bounded integration surface.
- Verification: Filesystem-permission tests, oversized-output process doubles, path-collision tests, log capture, and
  post-run directory snapshots prove permissions, bounds, safe messages, distinct paths, and cleanup on every outcome.

### RULE-14 - Safe import and archive parsing

- Applies to: UC-1, UC-8
- Constraint: A definition or result JSON document MUST be at most 10 MiB and the manifest at most 1 MiB. An archive
  MUST be at most 25 MiB compressed and 21 MiB total uncompressed and contain exactly the three root file entries in
  `spec.md`, with no duplicate, directory, encrypted, absolute, parent-traversal, or separator-containing entry. Archive
  entries MUST be parsed as bounded byte streams and MUST never be extracted to the filesystem. Parsing MUST stop at
  the first exceeded count or size limit. Rejection MUST persist nothing and MUST NOT invoke planning.
- Reason: Exact archive contents and bounded parsing prevent ambiguous bundles, path traversal, and decompression
  resource attacks.
- Verification: Boundary tests cover one byte below, at, and above every limit plus duplicate names, extra entries,
  missing entries, traversal forms, alternate separators, directories, encryption, truncation, and compressed bombs;
  filesystem snapshots prove that no entry is extracted, and database snapshots remain unchanged after rejection.

### RULE-15 - Intent overlay compiles without accepted mutation

- Applies to: UC-4, UC-5, UC-7
- Constraint: Draft intent MUST remain separate from the accepted definition. Compilation MUST deep-copy the complete
  accepted definition, set `basedOnRevision` to the accepted result's `inputRevision`, materialize remaining
  `availablePeriodIds` for staged teacher or room unavailability, and map confirmed period/room pins to locks copied
  from accepted assignments. It MUST NOT edit accepted assignment JSON, invent a substitute, emit unsupported change
  types, or create a soft-protection weight. The intent revision MUST be `sha256:<lowercase-hex>` over canonical JSON
  containing all staged changes, pin dimensions and provenance, bulk-action snapshots, and their deterministic order.
- Reason: The overlay explains direct effects while the compiled document remains the sole scheduling input.
- Verification: Table-driven tests compare accepted input, intent, and compiled output field by field for omitted and
  explicit availability, teacher and room changes, each pin dimension, persistent locks, and prohibited edit types;
  accepted JSON remains identical.

### RULE-16 - Direct effects, conflicts, and bulk pins use immutable snapshots

- Applies to: UC-4, UC-7
- Constraint: Direct-effect calculation MUST compare staged availability with accepted assignments only. Bulk preview
  MUST record the exact ordered lesson IDs, dimensions, source workspace version, direct-effect revision, count, and
  conflicts; confirmation MUST apply exactly that preview only when its `If-Match` still matches. Conflicts MUST block
  solving with `DRAFT_CONFLICT` and MUST NOT silently drop a pin or availability instruction. Undo MUST remove only the
  recorded bulk snapshot except independently applied pins and persistent policy locks.
- Reason: Re-evaluating a selection after confirmation would change administrator intent and make bulk protection
  unauditable.
- Verification: Deterministic selection tests cover day, class, currently-unaffected, filters, later draft edits,
  overlapping bulk actions, individual pins, conflict combinations, stale preview refusal, and exact undo sets.

### RULE-17 - Proposal identity and acceptance guard

- Applies to: UC-2, UC-5, UC-6, UC-7
- Constraint: A proposal MUST store source workspace version, accepted timetable revision or explicit no-baseline
  marker, successor-definition revision, intent revision, the complete canonical kernel result document, proposed
  timetable revision, run ID, limit, termination reason, elapsed time, and change counts. The workspace MUST NOT invent
  or expose a public whole-result revision absent from the kernel contract. Acceptance MUST revalidate the stored result
  against its definition and recompute and compare every authoritative identity in the same conditional transaction;
  mismatch returns `STALE_PROPOSAL`, removes proposal eligibility as specified, and MUST NOT change accepted data.
- Reason: A `FEASIBLE` label alone cannot prove that the proposal belongs to current workspace state.
- Verification: Tests alter each identity and the stored result independently, race intent edits and acceptance,
  replay old results, prove no invented whole-result revision enters the API or export, and compare the accepted JSONB
  document and export bytes before and after every refusal.

### RULE-18 - Exact normative categories and export entries

- Applies to: UC-6, UC-8
- Constraint: The change-category and archive-entry tables in `spec.md` MUST be the sole normative source. Review MUST
  expose exactly the six categories in their specified meanings, preserve allowed overlap, and compute the unique
  changed-lesson total independently. Export MUST contain exactly the three specified entries and canonical accepted
  data only; draft, proposal, process, and unrelated database data MUST be absent. `workspace-manifest.json` MUST follow
  a strict `workspace-manifest-v1` shape with exactly `manifestVersion` (`1`), `definitionSchemaVersion`,
  `resultSchemaVersion`, `catalogVersion`, `schoolId`, `inputRevision`, `timetableRevision`, and `locks`. `locks` MUST be
  ordered by lesson ID, include only dimensions locked in the accepted definition, and contain `lessonId` plus one or
  both of `periodLockOrigin` and `roomLockOrigin`, each exactly `PERSISTENT_POLICY` or `ATTEMPT_SCOPED`. A raw imported
  definition without a manifest assigns `PERSISTENT_POLICY` to every existing lock.
- Reason: Re-derived or expanded lists would change the actor-visible review and interoperability contract.
- Verification: Exactness tests compare names, order, fields, totals, overlaps, ZIP entries, manifest revisions, and
  absence of extra values; a fresh accepted archive re-imports through UC-1 to the same canonical baseline.

### RULE-19 - Complete browser snapshot and native presentation

- Applies to: UC-3, UC-4, UC-6, UC-7
- Constraint: `GET /api/workspace` MUST return one complete display snapshot for the current state, and native ES
  modules MUST perform search, filtering, day changes, selection, grouping, totals, and rendering in browser memory.
  Browser code MUST treat IDs as opaque, use definition metadata for names and ordering, and send only intent commands
  with the current `ETag`; it MUST NOT reconstruct kernel input or classify proposal changes independently.
- Reason: Client-side presentation meets the confirmed scale strategy without moving scheduling authority into the
  browser or PostgreSQL JSON-path queries.
- Verification: Contract tests compare snapshot fidelity; browser tests exercise the scale fixture without filter API
  calls, verify opaque IDs and definition ordering, and prove all mutations carry the latest `If-Match`.

### RULE-20 - Accessible, English-localized workspace conventions

- Applies to: UC-3, UC-4, UC-6, UC-7
- Constraint: The workspace MUST reuse the existing visual vocabulary and focused class, teacher, and room interaction
  conventions while making the whole-school day matrix primary. All user-visible English text MUST come from one
  workspace message catalog, use `Class` with an explicit technical `cohort` mapping, and represent accepted, draft,
  pinned, policy-lock, required, direct-effect, solver-moved, conflict, and failed states with text or icons as well as
  color. The complete keyboard behavior in `spec.md` MUST work without pointer input.
- Reason: Central text and state semantics prevent divergent labels across matrix, focused, draft, diagnostic, and
  review views and leave a clear later localization seam.
- Verification: Message-catalog exactness tests reject inline production strings; automated accessibility and keyboard
  browser journeys cover navigation, pinning, preview, review, confirmation, focus restoration, and narrow-screen
  read-only behavior, followed by the required human walkthrough.

### RULE-21 - Same-origin local security without identity

- Applies to: all use cases
- Constraint: Spring Security MUST implement the route table above without login or roles. The server and development
  PostgreSQL port MUST bind only to loopback. Mutations MUST require a valid session-bound CSRF token and accepted Host
  and Origin; CORS and forwarded-host trust MUST be disabled. Static resources MUST be allowlisted, directory browsing
  and management endpoints MUST be absent, database credentials MUST come from local environment or configuration and
  MUST NOT be committed or returned to the browser.
- Reason: Deferring accounts does not remove DNS-rebinding, cross-site localhost mutation, accidental route exposure,
  or credential-disclosure risks.
- Verification: Real HTTP tests cover every table row and method, CSRF present/absent/wrong session, allowed and hostile
  Host/Origin values, CORS preflight, static traversal, management and login paths, loopback binding, response headers,
  and secret scanning.

### RULE-22 - Stable safe API failures

- Applies to: all use cases
- Constraint: API failures MUST use one JSON problem shape containing stable code, localized safe message, correlation
  ID, current lifecycle state, current `ETag` when disclosure is safe, and optional field/entity references. Malformed
  requests use `400`, CSRF or origin denial `403`, absent resources `404`, invalid transitions or draft conflicts `409`,
  stale versions `412`, size limits `413`, semantic validation `422`, missing `If-Match` `428`, and unavailable database
  or kernel transport `503`. Stack traces, SQL, paths, uploaded values, raw kernel stderr, and candidate assignments
  MUST NOT appear.
- Reason: The browser needs deterministic recovery without turning technical failures into data disclosure or false
  proposals.
- Verification: Controller and packaged HTTP tests exercise every mapping and inspect body, headers, logs, database,
  process invocation, and candidate absence; production-mode responses are scanned for prohibited details.

### RULE-23 - Safe observability and validation evidence

- Applies to: all use cases
- Constraint: Logs MUST be structured to stderr and limited to correlation ID, run ID, safe lifecycle transition,
  kernel command kind, exit class, elapsed time, configured limit, termination reason, feasibility flag, and aggregate
  change counts. They MUST NOT contain definitions, results, display names, assignments, draft intent, pins, database
  credentials, CSRF tokens, session IDs, uploaded filenames or paths, or exception details in normal operation. Every
  validation run MUST retain the non-sensitive solver evidence required by `spec.md` in the workspace document.
- Reason: Local diagnostics must support the study and failures without creating a second school-data store.
- Verification: Captured logging tests cover success and every failure class, assert required fields and prohibited
  values, and compare retained run evidence with the authoritative kernel result.

### RULE-24 - PostgreSQL-only isolated verification

- Applies to: all use cases
- Constraint: Database integration, lifecycle, migration, and HTTP tests MUST run against disposable PostgreSQL 18.6
  Testcontainers instances and MUST NOT fall back to H2 or an existing database. Each use case MUST have a real HTTP and
  browser success journey; each kernel-dependent journey MUST invoke the packaged CLI. Tests MUST assert state after
  every consequential step and negative paths MUST assert absence of mutation, disclosure, export, and process calls.
- Reason: Mocked or dialect-substitute tests cannot prove JSONB, transactions, conditional SQL, Flyway, security, or the
  actor-visible process boundary.
- Verification: The standard Maven verification lifecycle starts isolated containers and ephemeral servers, reports
  the packaged kernel and browser journeys per use case, leaves runtime and tracked data unchanged, and fails if Docker
  is unavailable rather than silently substituting another database.

### RULE-25 - Scale and interaction measurements are separate from solving

- Applies to: UC-3, UC-4, UC-6, UC-7
- Constraint: A complete synthetic or properly anonymized fixture of approximately 1,000 lessons, 100 teachers, 60
  classes, 100 rooms, and 60 periods MUST drive browser performance measurement. Search, filters, day changes,
  selection, and pin feedback MUST be measured after initial snapshot load at the 95th percentile against 250 ms;
  proposal review opening MUST be measured against one second. Solver duration MUST be recorded separately and MUST NOT
  be included in or used to excuse interaction latency.
- Reason: The product hypothesis depends on whole-school comprehension and responsive inspection, not merely solver
  throughput.
- Verification: A repeatable reference-machine browser script records raw samples, percentile calculation, fixture
  cardinalities, review time, and separate kernel evidence; release evidence includes the administrator walkthrough
  required by `spec.md`.

### RULE-26 - Kernel commands share typed application boundaries

- Applies to: UC-1, UC-2, UC-5, UC-7, UC-8
- Constraint: Kernel `plan`, `replan`, and `verify` MUST be separate application handlers taking immutable commands and
  returning sealed typed outcomes through injected ports; only the CLI composition root may construct adapters and map
  those outcomes to files, diagnostics, and exit codes. All three handlers MUST share one strict definition-loading,
  schema, semantic-validation, normalization, and revision pipeline. `replan` and baseline `verify` MUST share one
  definition-aware baseline verifier. A direct domain hard-feasibility verifier, independent of Timefold constraint
  streams, MUST validate imported baselines and every solver result before publication. Expected application and
  adapter exceptions MUST map exhaustively; production code MUST NOT catch `Throwable` or convert fatal JVM `Error`
  instances into product outcomes.
- Reason: Shared parsing and provenance logic prevent command drift, while an independent verifier prevents solver
  completion or a duplicated constraint-stream mistake from becoming acceptance authority.
- Verification: ArchUnit inspects all compiled production classes for dependency direction and adapter construction;
  exhaustive outcome-mapping tests fail on an unmapped subtype; shared-fixture tests feed identical documents through
  every command; generated and mutation fixtures compare the direct verifier with every hard catalog row and prove
  invalid baselines and solver results are never published or imported.

### RULE-27 - Kernel metadata and solver APIs have one controlled source

- Applies to: UC-1, UC-2, UC-5, UC-7, UC-8
- Constraint: One immutable internal kernel catalog descriptor MUST define catalog version, constraint IDs, order,
  categories, and default weights for validation, Timefold constraints, diagnostics, and result construction. The
  packaged `kernelVersion` MUST come from the Maven project version written into the executable JAR manifest; runtime
  code MUST NOT maintain or fabricate a second version constant. The solver adapter MUST use only public Timefold
  `SolverManager` and `SolverJob` APIs, own the application deadline and termination mapping, and contain every
  Timefold planning type. Missing or malformed packaged version metadata MUST fail safely before a result is trusted.
- Reason: Duplicated catalog or version values and internal solver APIs create silent contract drift across commands
  and upgrades.
- Verification: Catalog exactness tests compare every field with the normative tables and prove no extra row; manifest
  tests compare packaged metadata with the effective Maven version and reject missing or malformed metadata; source and
  bytecode architecture checks reject `.impl` imports and Timefold types outside the solver adapter; public-API solver
  integration tests exercise completion, deadline, cancellation, and failure mapping.

### RULE-28 - Kernel input and publication boundaries are bounded and race-safe

- Applies to: UC-1, UC-2, UC-5, UC-7, UC-8
- Constraint: Every kernel input document MUST be limited independently to 10 MiB of bytes, JSON depth 64, one string
  token of 1 MiB, and one numeric token of 100 characters by a bounded stream plus Jackson streaming constraints before
  tree materialization. A breach MUST return exit `74`, invoke neither verifier nor solver, publish no structured
  result, and preserve the destination. On tested local macOS and Linux filesystems, publication without `--force`
  MUST close a unique sibling temporary file and atomically create the destination with a hard link so concurrent
  publishers have exactly one winner. Publication with `--force` MUST use same-filesystem atomic replacement, giving
  the last completed publisher the destination. Unsupported capabilities MUST fail with `74`. The guarantee covers
  process-crash atomicity, not sudden-power-loss durability; temporary-artifact cleanup is best effort and never makes
  an unpublished artifact a result.
- Reason: Bounded streaming prevents pre-parse exhaustion, and explicit filesystem primitives are needed because a
  Java atomic move does not portably provide create-if-absent semantics.
- Verification: Boundary tests exercise one byte or token below, at, and above every limit; concurrent packaged
  processes prove one-winner and forced last-completed behavior; capability, serialization, close, link, move, and
  controlled crash tests assert exit codes, complete destination bytes, prior-destination preservation, and cleanup on
  the supported filesystem matrix.

### RULE-29 - Kernel reference corpus gates releases without a universal promise

- Applies to: UC-2, UC-5, UC-7
- Constraint: A release candidate MUST pass three committed deterministic target-scale cases: initial planning, repair
  after localized teacher unavailability, and repair after a room outage with representative period and room pins.
  Each packaged Java 25 case MUST run with seed `0`, `-Xmx2g`, and `--time-limit 25s`, leaving process and structured
  verification overhead inside a 30-second wall-clock gate on the documented 4-vCPU, 4-GiB reference runner. Every
  case MUST publish a result that the structured `verify` command accepts as `VERIFIED`. This reference-runner gate
  MUST NOT be presented as a feasibility, optimality, or latency promise for arbitrary school definitions or hardware.
- Reason: The workspace depends on both initial and repair behavior at target scale, while bounded heuristic search
  cannot support a universal timing guarantee.
- Verification: The release profile records fixture cardinalities and digests, effective JVM and kernel limits,
  hardware identity, wall time, termination reason, verification result, and change counts for all three cases and
  fails when any case exceeds the gate or lacks a verified feasible result.

### RULE-30 - Local application startup owns the walkthrough database

- Applies to: UC-3
- Constraint: Starting the workspace as a local JVM from the repository root MUST use Spring Boot's Docker Compose
  lifecycle to start one `postgres:18.6` service, wait for `pg_isready`, and supply its JDBC connection details without
  a separate database-start command. The service MUST publish only to IPv4 loopback, MUST use a named volume so an
  accepted walkthrough workspace survives ordinary application restarts, and MUST be removable with one documented
  reset command. The Compose file MAY contain clearly labeled, well-known local-development credentials; they MUST NOT
  be described or reused as production secrets. Docker Compose support MUST be packaged with the executable workspace
  JAR, while production and test profiles MUST be able to disable it explicitly. It MUST NOT containerize the
  workspace application or alter the packaged kernel process boundary.
- Reason: Administrator walkthroughs need one application command and durable local state without weakening loopback
  exposure, PostgreSQL fidelity, or the separate kernel executable contract.
- Verification: Configuration tests compare the Compose image, health check, loopback mapping, volume, credentials,
  and Spring Boot dependency/plugin settings by value. A live packaged-JAR smoke test begins with no project database
  container, starts the application once, observes Compose create a healthy PostgreSQL 18.6 service and Flyway-migrated
  `EMPTY` workspace, reaches `/workspace/` over loopback, stops the application, and proves the database service stops
  while its named volume remains. The standard Testcontainers suite disables Compose and stays isolated.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-3, RULE-4, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9, RULE-13, RULE-14, RULE-21, RULE-22, RULE-23, RULE-24, RULE-26, RULE-27, RULE-28 |
| UC-2 | RULE-3, RULE-5, RULE-6, RULE-7, RULE-8, RULE-10, RULE-11, RULE-12, RULE-13, RULE-17, RULE-21, RULE-22, RULE-23, RULE-24, RULE-26, RULE-27, RULE-28, RULE-29 |
| UC-3 | RULE-19, RULE-20, RULE-21, RULE-22, RULE-23, RULE-24, RULE-25, RULE-30 |
| UC-4 | RULE-3, RULE-5, RULE-6, RULE-8, RULE-15, RULE-16, RULE-19, RULE-20, RULE-21, RULE-22, RULE-23, RULE-24, RULE-25 |
| UC-5 | RULE-3, RULE-5, RULE-6, RULE-8, RULE-10, RULE-11, RULE-12, RULE-13, RULE-15, RULE-17, RULE-21, RULE-22, RULE-23, RULE-24, RULE-26, RULE-27, RULE-28, RULE-29 |
| UC-6 | RULE-3, RULE-5, RULE-6, RULE-7, RULE-8, RULE-17, RULE-18, RULE-19, RULE-20, RULE-21, RULE-22, RULE-23, RULE-24, RULE-25 |
| UC-7 | RULE-3, RULE-5, RULE-6, RULE-7, RULE-10, RULE-11, RULE-15, RULE-16, RULE-17, RULE-19, RULE-20, RULE-21, RULE-22, RULE-23, RULE-24, RULE-25, RULE-26, RULE-27, RULE-28, RULE-29 |
| UC-8 | RULE-3, RULE-8, RULE-9, RULE-13, RULE-14, RULE-18, RULE-21, RULE-22, RULE-23, RULE-24, RULE-26, RULE-27, RULE-28 |

RULE-1 and RULE-2 apply to every row through their `all use cases` applicability. UC-7's included use cases use the
same production HTTP, persistence, intent-compilation, process, and review paths tested for UC-3 through UC-6; no
orchestration-specific duplicate implementation is permitted. Every required use case consumes its predecessor's real
persisted postcondition in relationship tests.

## Design exclusions

- Production application images, Compose deployment, installers, service managers, hosted operation, and production
  database topology are not selected during the development period. RULE-30 selects only local JVM walkthrough startup.
- Authentication, accounts, roles, remote clients, multi-school routing, multi-user locking, and authorization policy
  are deferred. The local same-origin protections in these rules are not deferred.
- JPA/Hibernate, Spring Data repositories, H2, filesystem state as a second authority, event sourcing, audit history,
  queues, distributed jobs, WebSockets, and Server-Sent Events are excluded.
- Server-side JSONB timetable filtering, a relational/materialized timetable read model, and persistence of unaccepted
  proposals across restart are excluded.
- Node-based assets, bundling, transpilation, service workers, offline caching, and CDN resources are excluded.
- Kernel algorithm, score meaning, and catalog semantics are excluded. The allowed kernel contract delta is limited to
  the structured non-solving `verify` command, definition-aware `replan` provenance, bounded publication behavior, and
  supporting schemas needed by UC-1, UC-2, UC-5, and UC-8.

## External dependencies

- Final packaging is intentionally undecided. During development the default is a local JVM workspace process with a
  loopback-bound PostgreSQL 18.6 container; a future packaging decision must preserve the behavior and security rules
  above or revise this feature's rules explicitly.
- Automated PostgreSQL verification requires an available Docker-compatible container runtime. The suite must report
  that environmental dependency honestly; lack of a runtime is not a passing database result.
- Product validation still depends on the administrator study and reference validation machine defined in `spec.md`.
