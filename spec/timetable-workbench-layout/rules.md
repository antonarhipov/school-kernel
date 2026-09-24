# Technical Rules: Timetable Workbench Layout

## Design overview

The packaged `/workspace/` page remains one native HTML/CSS/ES-module workbench. Its existing accepted model, inspection-state owner, Day/Week/focused renderers, and proposal comparison index supply one canvas across Current, Draft, Solving, and Proposal. A compact shell and toolbar precede the canvas; a contextual inspector sits beside it on wide desktops or below it at intermediate widths; a separate, full-width task area follows the canvas. The task area presents repair setup, durable Draft controls, frozen run status, or Proposal review according to the authoritative snapshot. Layout and disclosure state are browser-session presentation state, not a second workspace state machine. Existing Spring services and the PostgreSQL aggregate alone stage intent, launch and verify the packaged kernel, and atomically accept a proposal. The prototype guides composition, not data, code, or authority. Native CSS and the existing ES modules are the leanest viable choice; adding a frontend framework, layout library, or runtime dependency would create a second rendering system without supplying a required capability.

## Codebase alignment

- The Spring Boot 4.1.1 / Java 25 `timetable-workspace` module already serves the operational page, message catalog, inspection state, accepted-model and comparison modules, and Day/Week/focused renderers. The standalone `ui/` prototype is illustrative. The current 1440 px shell, 300 px inspector, and workflow markup inside that inspector are the presentation delta; the existing repair services are not being replaced.
- The singleton PostgreSQL 18.6 JSONB aggregate, Flyway schema, version-checked repository updates, Spring transactions, packaged kernel process, independent verification, and failure recovery remain unchanged. No new route, response field, stored field, migration, solver contract, or dependency is warranted by a layout change.
- The existing school-scoped Day/Week and weekday local preference is the only persisted browser preference. Browser workbench state otherwise remains ephemeral. Existing same-origin CSP, escaped school-controlled text, native controls, and English message catalog apply to moved as well as new UI.
- `spec/timetable-ux-polish/status.md` still records its UC-5 administrator gate as `PENDING_WALKTHROUGH`. This feature does not change that verdict or treat earlier browser evidence as its own administrator gate.

## Security surface

There is no login, account, or public Internet access. “Permitted” below means only a request accepted by the existing loopback Host/Origin boundary; mutating methods also require same-origin Origin, session CSRF, and a matching `If-Match`. The layout MUST NOT make a read-only narrow viewport a substitute for server authorization. Existing lifecycle and payload checks still apply after route admission.

| Route and method | Access and protection | Workbench relevance |
|---|---|---|
| `GET /` | Local; redirects to `/workspace/` | Entry |
| `GET /workspace/**` | Local; packaged same-origin static content and security headers | Operational UI and existing import/planning UI |
| `GET /api/csrf` | Local; no timetable document | Existing mutation token |
| `GET /api/workspace` | Local; ETag, no-store | Authoritative snapshot for all modes |
| `POST /api/import` | Local same-origin; CSRF, `If-Match` | Existing initial import only |
| `POST /api/initial-draft/replace` | Local same-origin; CSRF, `If-Match` | Existing initial planning only |
| `POST /api/repair-draft` | Local same-origin; CSRF, `If-Match` | Stage first repair intent |
| `PATCH /api/repair-draft` | Local same-origin; CSRF, `If-Match` | Edit intent, pin, confirm bulk pin, undo |
| `DELETE /api/repair-draft` | Local same-origin; CSRF, `If-Match` | Confirmed Draft discard |
| `POST /api/repair-draft/bulk-pin-preview` | Local same-origin; CSRF, `If-Match`; no durable write | Protection preview |
| `POST /api/runs` | Local same-origin; CSRF, `If-Match` | Repair generation or eligible retry; existing initial run remains |
| `GET /api/runs/{runId}` | Local; matching active/retained run required; no-store | Run polling and safe outcome |
| `DELETE /api/runs/{runId}` | Local same-origin; CSRF, `If-Match` | Active-run cancellation |
| `GET /api/proposal` | Local; eligible proposal required; ETag, no-store | Existing proposal read |
| `POST /api/proposal/accept` | Local same-origin; CSRF, `If-Match` | Explicit confirmed acceptance |
| `DELETE /api/proposal` | Local same-origin; CSRF, `If-Match` | Existing Proposal revise/discard return to Draft |
| `GET /api/accepted/export` | Local; existing Accepted-baseline-only service guard; no-store | Applicable Utilities action |
| `/login`, `/logout`, `/actuator/**`, all other routes and methods | Denied or not exposed | No new access path |

## Verification strategy

Use isolated PostgreSQL Testcontainers storage, ephemeral loopback HTTP, packaged workspace assets, a real browser, and the packaged scheduling-process boundary. Fixtures and browser profiles MUST be temporary or dedicated to tests; tests MUST NOT mutate runtime school data or tracked generated files. Build the normative validation school from `spec.md`, verify its accepted definition/result pair before use, and compare every declared ID, field, order, count, special case, and absence of extra values against the isolated database. The supplementary addition/cancellation fixture is presentation-only and cannot become an eligible accepted proposal. This feature seeds no credentials.

Each UC needs its main actor journey at the real browser boundary, including extensions, guarantees, and both postconditions. Compare exact accepted bundle, Draft, run, Proposal, and version after each consequential step; presentation-only actions must additionally prove no mutation request and no durable change. Negative tests must prove refusal and absence of disclosure, write, process launch, candidate rendering, or late application as appropriate, not only an HTTP status. Set up genuine lifecycle state, including stale and failed outcomes; doubles, where unavoidable for a fault, must retain production exception, nullability, option, and field contracts. Measure CSS geometry and scroll behavior at the three normative viewports with the same scale fixture, long names, cues, and open task area. Exercise the 701/700 and 1279/1280 CSS px boundaries as additional responsive edge checks. Native keyboard and pointing-device access, non-color cues, and exact localized labels require browser assertions.

UC-1 proves complete and narrowed Day/Week inspection, empty structure, selection, focused return, inspector, Utilities, preference fallback, failed export, rejected import, and narrow reading. UC-2 begins from UC-1's actual accepted postcondition and proves setup without a write, teacher/room and no-effect intent, conflict, individual/bulk protection, save refusal, collapse/reopen, reload, discard, and narrow refusal. UC-3 consumes UC-2's saved conflict-free Draft through a packaged run and proves live inspection, frozen controls, cancellation, actual restart recovery, unsuccessful/rejected/timeout outcomes, eligible retry, and verified Proposal. UC-4 consumes that verified Proposal and proves all comparison shapes, both navigation targets, one-sided narrowing, exact/overlapping counts, revise/discard, stale and failed acceptance, successful acceptance, and narrow reading. UC-5 follows those same production paths end to end, verifies every handoff and a second repair based on the newly accepted bundle without inherited attempt pins, and runs the complete shared browser regression. No status-only, mocked-canvas, or stitched per-mode evidence substitutes for those relationships.

After automation, each UI UC requires a human walkthrough drawn from its scenario and extensions. The UC-5 feature gate uses exactly the six tasks, five participants, three-school minimum, and success/Current-identification thresholds in `spec.md`; record each participant's results and layout friction. Missing browser, scheduler, isolated storage, full regression, or participants is a validation limit, not approval.

## Rules

### RULE-1 - One packaged workbench and timetable authority

- Applies to: all use cases
- Constraint: The layout MUST extend the existing packaged workspace, accepted model, inspection state, Day/Week/focused renderers, proposal comparison index, native controls, and message catalog. `GET /api/workspace` MUST remain the complete snapshot source. It MUST NOT fork accepted assignments into mode-specific models, adopt prototype lesson data or assets, add a frontend framework/bundler/runtime dependency, or create another API or persistence field for layout state.
- Reason: Moving controls must not create divergent timetable identities or a second authority.
- Verification: Dependency, asset, storage, and network inspection plus browser ID/field comparisons across modes prove one source and no new route or persisted shape.

### RULE-2 - Presentation state cannot transition the workspace

- Applies to: all use cases
- Constraint: One browser presentation-state owner MUST derive available/default modes from the four-row state table in `spec.md`; reject unavailable mode selection; keep task-area open/collapsed state separately per mode during the page session; and reset it, inspector, Filters, Utilities, search, highlights, filters, selection, and focus to the specified defaults on reload. Current MUST start with its task area closed, Draft and Proposal with theirs open, and Solving with status/cancellation visible. The inspector MUST start open at widths at least 1280 CSS px and collapsed at 701–1279 px; Filters and Utilities MUST start closed. The owner MUST retain representable range, investigation, selected lesson, focus return, and scroll across mode and area changes, clearing only unrepresentable context with an explanation. Only the existing valid school-scoped Day/Week and weekday preference MAY survive reload. Presentation actions MUST NOT issue durable mutation requests or change the aggregate version.
- Reason: The new dock and compact header are not an alternative lifecycle or durable preference store.
- Verification: Browser mode/reload/resize traces compare visible context, storage keys, mutation traffic, exact aggregate document, and version before and after every presentation action.

### RULE-3 - Existing service lifecycle remains the enforcement boundary

- Applies to: UC-2, UC-3, UC-4, UC-5
- Constraint: The existing services MUST enforce only the `spec.md` transitions: Accepted baseline to Repair draft; Draft edit, solve, or confirmed discard; Solving to independently verified Proposal or safely back to Draft; Proposal to explicitly accepted baseline or back to Draft; failed durable acceptance remaining Proposal. Every other requested transition MUST be refused at the service/mutation boundary with the existing `WorkspaceProblem` result (`INVALID_WORKSPACE_TRANSITION`, or applicable `STALE_WORKSPACE_VERSION`, `STALE_RUN`, `STALE_PROPOSAL`, `DRAFT_CONFLICT`, `DRAFT_NOT_DURABLE`, `RETRY_NOT_AVAILABLE`, or `STALE_WORKSPACE_STATE`), not merely hidden by layout. Refusal MUST leave accepted data and ineligible Draft/run/Proposal exact and start no process; the specified stale-Proposal invalidation to Draft is the only applicable state-changing refusal. Failed storage on acceptance MUST retain the old Current and reviewable Proposal.
- Reason: Compact controls cannot broaden the repair authority or blur its failure states.
- Verification: Real-state HTTP/JDBC tests force each disallowed transition, stale version/run/proposal, conflict, unsaved intent, rejected output, and failed acceptance; assert exact state/version, process absence, and correct invalidation or retryability.

### RULE-4 - Wide canvas and compact tile geometry

- Applies to: UC-1, UC-5
- Constraint: The operational shell MUST use at least 95 percent of the 1600 x 900 CSS px viewport, place a compact header and directly visible Day/Week, weekday, search, subject, and teacher controls above the dominant canvas, and keep header/toolbar outside matrix scrolling. At that viewport the normative five weekdays MUST fit with the inspector open without internal horizontal scrolling; ordinary occupied Week slots MUST be at most 36 CSS px high and ordinary Day cells at most 60 CSS px high, while long text and multiple cues MAY increase height rather than clip. The 1280 x 800 layout MUST have no page-level horizontal scroll or clipped task control. Definition-ordered class/time headings MUST remain available while the matrix scrolls. There MUST be exactly one Compact density, with subject leading and room visible in every occupied tile.
- Reason: Density and width are the reason for this feature, but correctness outranks a forced height on exceptional content.
- Verification: Real-browser box/scroll measurements, screenshots, and exact represented-ID comparisons at normative scale and viewports, including long labels, empty positions, and multiple state cues.

### RULE-5 - Inspector is context; task area owns the workflow

- Applies to: UC-1, UC-2, UC-3, UC-4, UC-5
- Constraint: At widths at least 1280 CSS px a stable-width collapsible inspector MUST remain beside the canvas even with the task area open; at 701–1279 px it MUST be a collapsible region below the canvas and above the task area, with no permanent side column. Selection MUST open it; collapse MUST retain selection and expand the canvas. The inspector MUST contain exact selected-lesson details and concise state cues, but MUST NOT contain the full repair period picker, conflict/bulk workflow, proposal report, or decision controls. Those controls MUST live in a task area spanning the available workbench width beneath, never over, the canvas. At 1600 x 900 and 1280 x 800 its open height MUST be at most 35 percent of viewport height, with independent content scrolling if needed, while time headings and at least one complete class row remain visible; no active-task control may be clipped or require window resizing.
- Reason: The existing 300 px inspector is unsuitable as the full repair workspace.
- Verification: Browser DOM/geometry assertions at 1279/1280 and normative viewports, selected-context/collapse checks, screenshots, and reachable-control tests with open Draft, Solving, and Proposal areas.

### RULE-6 - Complete population and honest narrowing

- Applies to: UC-1, UC-4, UC-5
- Constraint: The canvas MUST represent every declared class, ordered weekday, period, and empty position in its selected Week or Day unless explicit filters narrow it. Search and subject/teacher investigation MUST highlight without narrowing; class, teacher, room, period, and subject/teacher-only filters MUST intersect. Filters MAY be disclosed, but their active names, exact unique-lesson count, complete-versus-narrowed status, and direct clear action MUST remain visible when closed. Focused schedules MUST retain a return path and MUST NOT be labelled whole-school. Proposal matching MUST consider either accepted or proposed side while retaining both explanatory representations and identifying the matching side.
- Reason: A compact toolbar must not silently hide lessons or misrepresent a subset as complete.
- Verification: Compare rendered unique-ID sets to independently computed fixture sets for complete, intersected, zero, one-sided Proposal, and focused states; inspect closed disclosure, return, and empty-structure labels.

### RULE-7 - Exact identities, availability, and safe text

- Applies to: all use cases
- Constraint: All views MUST use authoritative school-controlled display names and definition-ordered time; an opaque ID MUST NOT be interpreted as an abbreviation or group. The inspector and accessible lesson name MUST expose complete subject, class, teacher, weekday, period, room, and stable lesson identity. Missing required accepted or candidate metadata and unmappable references MUST be refused before operational Current or Proposal is shown; an unavailable optional diagnostic name MUST retain its stable ID with an explicit unavailable-name cue. Current, Draft, and Solving availability MUST come from the accepted definition; Proposal availability MUST be separately labelled and derived from the verified successor. School text and diagnostics MUST be inert escaped text and MUST NOT be logged raw.
- Reason: Visual compaction may shorten tiles, but it cannot invent or corrupt school facts or disclose hostile text.
- Verification: Verifier-boundary import/proposal refusals, long/opaque/hostile-name fixtures, accessible-name and inspector assertions, accepted/proposed availability comparisons, CSP and log inspection.

### RULE-8 - Draft setup and protection stay explicit

- Applies to: UC-2, UC-5
- Constraint: Requesting repair setup in Current MUST open the task area before any write; closing it unstaged MUST leave Accepted baseline exact. Draft MUST decorate the accepted canvas rather than edit assignments, and its wide area MUST show saved weekly intent, direct effects, exact conflicts, distinct persistent locks and attempt pins, individual/bulk protection, readiness, and solve/discard actions. Bulk preview MUST not persist; only confirmation MAY apply the exact previewed IDs and dimensions, and undo MUST identify its action. Invalid, stale, conflicting, or failed saves MUST neither claim success nor enable solve from unsaved intent. At 700 CSS px or less the UI MUST expose none of those mutations.
- Reason: A relocated form must preserve the protective workflow and the accepted baseline.
- Verification: Browser/HTTP/JDBC comparisons for setup close, teacher/room/no-effect intent, conflict, preview cancel/confirm/undo, failed/stale save, discard, reload, and narrow view prove exact Draft/accepted state and absent ineligible process launch.

### RULE-9 - Solving remains a frozen accepted view

- Applies to: UC-3, UC-5
- Constraint: Solving MUST consume only a saved ready Draft, render accepted assignments with frozen intent, and keep run status, limit, and cancellation visible in the task area even if secondary detail is collapsed. Draft edit, pin, discard, and Proposal decisions MUST be absent during the run. Mode, range, inspector, task-area, and focus changes MUST neither restart nor cancel it. Cancellation, actual process restart, no-feasible/timeout/failure, and rejected, incomplete, stale, or mismatched output MUST return safely to editable Draft with no candidate exposed; only an independently verified complete feasible result MAY create Proposal. The two-minute retry MUST remain available only for the existing unchanged-intent no-feasible condition.
- Reason: A compact status area must not make a running or failed candidate look accepted.
- Verification: Packaged-process browser/HTTP/JDBC runs observe run IDs, frozen controls, cancellation/recovery, safe diagnostics, retry eligibility, candidate absence, accepted/Draft bytes, and verified handoff.

### RULE-10 - Proposal comparison preserves one lesson identity

- Applies to: UC-4, UC-5
- Constraint: Proposal MUST use the existing ephemeral stable-lesson-ID comparison index and authoritative review. Its wide task area MUST hold navigable categories/groupings, exact before/after details, protection, safe run evidence, and unique/direct/ripple counts. A period move MUST expose labelled accepted origin and proposed destination on the same canvas; a same-slot teacher/room change MUST use one combined representation; addition and cancellation MUST show only their existing side. Selecting either move target MUST retain one lesson identity and exact old/new fields. If a target is outside Day, focus, or narrowing, navigation MUST change only the necessary presentation context, announce each adjustment, and preserve search/highlights. Unique changed totals MUST use distinct lesson IDs and authoritative category/group counts, never DOM tile count or a sum of overlapping direct/ripple membership. No unverified candidate may render as Proposal.
- Reason: A wide review area is useful only if its navigation and counts explain the actual accepted-to-proposed relationship.
- Verification: Browser and independent model calculations assert every shape, two-sided fields, both targets, one-sided filters, zero and overlapping counts, context changes, and absence of candidate after verification failure.

### RULE-11 - Deliberate acceptance and subsequent repair

- Applies to: UC-4, UC-5
- Constraint: Proposal revise/discard and confirmed acceptance MUST remain visible in the wide task area and use the existing guarded service routes. Only explicit, independently revalidated, durably successful acceptance MAY advance Current; stale identity MUST follow the existing invalidation to Draft, while failed persistence MUST keep the old Current and unchanged reviewable Proposal. After success only Current mode remains, the task area closes, and any next repair MUST parent the new accepted bundle without carrying previous attempt-scoped pins unless reapplied.
- Reason: The broader layout cannot make proposal visibility equivalent to acceptance or leak an earlier attempt into the next one.
- Verification: Real browser and transactional JDBC tests compare complete accepted definition/result, versions, Draft/Proposal absence or retention, next Draft parent revision, and pin provenance across success and both refusal paths.

### RULE-12 - Local route and narrow-screen boundary

- Applies to: all use cases
- Constraint: The complete route-to-access table above MUST remain unchanged. UI work MUST NOT relax loopback binding, Host/Origin, CORS, CSRF, `If-Match`, no-store, CSP, `nosniff`, or same-origin referrer policy, persist school documents in browser storage, or load remote prototype resources. At 700 CSS px or less it MUST show a read-only Day or focused agenda and true lifecycle state, with no repair editing, run cancellation, or Proposal decision; it MUST NOT claim the complete desktop canvas. Denied requests MUST disclose no timetable data and cause no write, archive, or process start.
- Reason: Rearranged controls must not widen the application's local-only surface.
- Verification: Full route/method and hostile Host/Origin/CSRF/stale-version matrix, response/body and DB/process assertions, network/storage audit, and real-browser 701/700/390 px checks.

### RULE-13 - Localized, accessible, observable presentation

- Applies to: all use cases
- Constraint: Every new visible, accessible, status, error, and unavailable-name string MUST use the existing English workbench message catalog and authoritative school-controlled names. State, accepted-versus-proposed side, selection, direct/ripple effect, conflict, pin/protection, highlighting, and narrowing MUST have textual or structural cues in addition to color. Every actor action MUST have a visible pointing-device control and native keyboard operation; no custom matrix keyboard-navigation system is required. Utilities MUST contain applicable export and safe diagnostics, while lifecycle decisions remain in their task area and initial import/planning remain separate.
- Reason: Space savings cannot remove comprehension, accessibility, or consistent wording.
- Verification: Packaged browser pointer/keyboard walkthrough, non-color and accessible-name inspection, catalog/string audit, export failure, diagnostic safety, and initial-journey regression.

### RULE-14 - Exact isolated feature and relationship evidence

- Applies to: all use cases
- Constraint: The verification strategy above MUST be met with the `spec.md` normative school and viewport data, exact isolated durable-state comparisons, each UC's real actor-boundary main scenario and extensions, and the complete shared browser regression. UC-2 MUST extend UC-1's live accepted view; UC-3 MUST consume its saved Draft; UC-4 MUST consume UC-3's independently verified Proposal; UC-5 MUST invoke those same production paths and consume their postconditions. Automated evidence MUST NOT be reported as administrator approval without the specified human walkthrough and six-task feature gate.
- Reason: A layout that passes isolated component checks may still break the sustained repair journey.
- Verification: Fixture provenance and value audit, browser/process/JDBC traces after each step, complete regression report, and per-participant gate record demonstrate the relationships and identify unmet evidence explicitly.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-4, RULE-5, RULE-6, RULE-7, RULE-12, RULE-13, RULE-14 |
| UC-2 | RULE-1, RULE-2, RULE-3, RULE-5, RULE-7, RULE-8, RULE-12, RULE-13, RULE-14 |
| UC-3 | RULE-1, RULE-2, RULE-3, RULE-5, RULE-7, RULE-9, RULE-12, RULE-13, RULE-14 |
| UC-4 | RULE-1, RULE-2, RULE-3, RULE-5, RULE-6, RULE-7, RULE-10, RULE-11, RULE-12, RULE-13, RULE-14 |
| UC-5 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9, RULE-10, RULE-11, RULE-12, RULE-13, RULE-14 |

## Design exclusions

- No redesign of initial import/planning, new solver behavior or instruction type, group inference, primary-room policy, direct accepted-assignment editing, second full Proposal canvas, or mobile mutation.
- No new authentication, route, database field, browser-persisted task state, density selector, custom zoom, custom matrix keyboard system, frontend framework, remote asset, or performance timing gate.
- The earlier `timetable-ux-polish` presentation placement is superseded here, but its recorded verdict and outstanding administrator evidence are not rewritten.

## External dependencies

- The prior `timetable-ux-polish` UC-5 feature gate remains pending in its own status. If it is still pending at execution or convergence, report it separately; do not infer approval from this feature.
- The five administrators from at least three schools, normative fixture provenance, isolated browser/storage, and packaged scheduler are required for final feature approval. If unavailable, record the missing evidence and keep the corresponding verdict short of approval.
