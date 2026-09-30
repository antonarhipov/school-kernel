# Technical Rules: Timetable Matrix Lenses

## Design overview

The feature is presentation-only and lives in the `timetable-workspace` static UI. The lens is the existing
Teacher and Room filter values in `inspection-state.js`: `teacherFilterId` and `roomId`. The state module makes them
mutually exclusive and records the lens-entry scroll context. `app.js` derives the row arrangement from that state. In
class rows, it passes class row groups to the Week and Day renderers, as today. In a lens, it passes one lens row group
and a period-keyed cell index built from the represented assignments. `week-renderer.js` and `day-renderer.js` stay the
only matrix renderers. They accept generic row groups instead of cohorts, plus an optional per-cell availability
callback. Tile markup stays in `app.js` (`weekLessonButton`, `lessonButton`, `comparisonLessonButton`). It gains an
arrangement argument that selects the visible fields from the Normative data table. Inspector "Show week" actions call
the same state transitions as the filter selects. Nothing reaches the server. There are no new routes, persistence,
or contract changes. The focused schedule is removed in two steps. The desktop entry and return controls go in UC-1.
The narrow agenda, `focused-renderer.js`, and the focused state functions go in UC-4.

## Codebase alignment

- Inherited stack: Java 25, Spring Boot 4.1.1, PostgreSQL 18.6 through Testcontainers 2.0.5, Playwright for Java with
  headless Chrome, native HTML/CSS and vanilla ES modules. Node, npm, bundlers, and frontend frameworks stay excluded.
- Inherited presentation conventions: string-template renderers, `escapeHtml`/`escapeAttribute` on every interpolated
  value, native `<select>` controls built by `selectControl`, and user-visible text in `messages.js`.
- The Content-Security-Policy (`style-src 'self'`, `script-src 'self'`) is unchanged. Lens markup uses classes and
  the `hidden` attribute, not inline `style` attributes or inline handlers. Position styles that the existing popover
  sets through the CSSOM are unaffected.
- The existing `isRepresented` / `baseFilterMatches` / `sideMatches` predicates remain the single definition of the
  represented population. A lens is a filter value, so every existing filter consumer (counts, selection clearing,
  review-target clearing, search, investigation counts) sees it without a parallel code path.
- Deviation: `renderFocusedSchedule`, `renderFocused`, `focusedEntry`, and their state functions are deleted instead of
  kept for compatibility, as the spec's Removed elements table requires.

## Security surface

The feature adds and changes no route, header, or access rule. The complete surface is inherited unchanged.

| Route and method | Access and protection | Feature relevance |
|---|---|---|
| `GET /`, `GET /workspace/**` | Local loopback; same-origin static assets; CSP unchanged | Serves the changed ES modules and CSS |
| `GET /api/csrf`, `GET /api/workspace` | Local loopback; `Cache-Control: no-store`; `ETag` | Sole data source for the lens; read-only |
| `POST /api/import`, `POST /api/initial-draft/replace`, `POST /api/workspace/clear`, `POST /api/workspace/upload-definition` | Local same-origin; CSRF | Not invoked by any lens action |
| `/api/runs/**`, `/api/proposal/**`, `/api/repair-draft/**` | Local same-origin; CSRF and `If-Match` | Not invoked by any lens action |
| `POST/PATCH/DELETE /api/manual-draft`, `POST /api/manual-draft/publish` | Local same-origin; CSRF and `If-Match` | Invoked only by the unchanged manual editor inside a lens (UC-3) |
| `GET /api/accepted/export` | Local loopback | Unchanged |

## Verification strategy

- Automated verification uses Playwright browser integration tests against an ephemeral Spring Boot server and an
  isolated PostgreSQL Testcontainer. Documents come from `WorkbenchFixtures`, including the 1,000-lesson scale school
  and the comparison-shape proposal. Tests never touch developer databases or tracked files.
- Each UC has a main-scenario browser test that drives the real controls (filter selects, inspector actions,
  checkboxes, and the narrow picker) through the `Workbench` page object. After every consequential step, it asserts
  the rendered row groups, row headers, tile text by value, and filter status.
- Every lens test proves non-mutation twice. `Workbench.recordMutations()` must return an empty list for lens actions,
  and the stored workspace document must be equal before and after the journey. UC-3 edits are the exception: they
  must issue exactly the manual-draft `PATCH` requests that the edits require.
- Completeness (UC-1 G2) compares the set of rendered lens lesson IDs with an independently computed set from the
  fixture, with the matrix scrolled to the start and to the end.
- Regression: when a UC removes or replaces a superseded behavior, the assertions that covered it in existing browser
  ITs are rewritten to the lens equivalent in the same UC commit. They are never deleted without a replacement.
  Unrelated assertions stay intact. The full `timetable-workspace` verify suite runs before each submission.
- UI use cases end with a human walkthrough derived from the UC scenario and extensions.

## Rules

### RULE-1 - Lens is the Teacher and Room filter state

- Applies to: UC-1, UC-2, UC-3, UC-4
- Constraint: The lens MUST be represented by the existing `teacherFilterId` and `roomId` values of
  `createInspectionState`. Setting either one to a declared ID MUST clear the other in the same transition. Setting
  either one to an undeclared ID MUST return `changed: false` and leave the state unchanged. `resetFilters` and
  `clearNarrowing` MUST clear both. The module MUST NOT add a second, parallel lens field that can disagree with the
  filter values.
- Reason: The spec defines the lens as a value of the existing filter dimension. A single source keeps the counts,
  selection clearing, and review-target clearing correct without duplicated logic.
- Verification: Browser tests select a teacher and then a room and assert that only the room criterion and room lens
  remain. An undeclared ID leaves the prior rows and filters unchanged.

### RULE-2 - Lens is never persisted

- Applies to: UC-1, UC-2, UC-4
- Constraint: The local preference record MUST keep exactly `{ version, range, weekdayId }`. Lens values and the
  lens-entry scroll context MUST NOT be written to `localStorage`, `sessionStorage`, the URL, or the server.
- Reason: Resolved decision 7. The existing `validPreference` rejects records that are not exactly three keys, so a
  leaked key would also discard the stored range.
- Verification: A browser test applies a lens, reads the preference key, asserts its exact JSON, reloads, and asserts
  class rows.

### RULE-3 - One matrix renderer path for class rows and lenses

- Applies to: UC-1, UC-2, UC-3, UC-4
- Constraint: `renderWeekMatrix` and `renderDayMatrix` MUST be the only functions that produce matrix markup, both for
  class rows and for lens row groups. They MUST take row groups (`id`, arrangement kind, header label) and a cell
  lookup instead of cohorts. They MUST NOT branch into a separate lens renderer or template. Lens tiles MUST come from
  the same tile functions that class rows use, parameterized by arrangement.
- Reason: UC-1 G1 and UC-3 G1 require the same surface and parity for selection, inspector, conflict indicators,
  overlays, and comparison cues. A second renderer would drift.
- Verification: Code inspection shows a single call site per range. Browser tests assert that `.matrix-wrap` is the same
  element class, and that selection, `aria-pressed`, conflict indicators, and overlays behave identically in a lens.

### RULE-4 - Lens population comes from the represented-assignment predicate

- Applies to: UC-1, UC-2, UC-3
- Constraint: Lens cells MUST be filled from the displayed model's assignments of the lens entity, keyed by
  `periodId`. The visible lens tiles MUST be exactly those satisfying the existing `isRepresented` predicate, which is
  the same in-place filtering used on class rows. Completeness MUST NOT depend on DOM position, viewport, or scroll. In `PROPOSAL`
  mode, every comparison representation of a represented lesson MUST be placed in its own period cell, which keeps a
  joined accepted/proposed pair together when only one side matches the lens entity. In `MANUAL_DRAFT` Draft mode, the
  manual-draft model MUST be the source.
- Reason: UC-1 G2 and the "same represented-population and review-target rules" scope item. It also preserves the
  approved comparison guarantee that a focused resource keeps both sides of a joined lesson.
- Verification: The rendered lens lesson-ID set equals the independently computed set at scroll start and end. A
  proposal-mode test asserts both comparison sides of `lesson-960` under a teacher lens and a room lens.

### RULE-5 - Availability marks follow the declared set only

- Applies to: UC-1, UC-4
- Constraint: An empty lens cell MUST be marked unavailable only when the lens entity has an `availablePeriodIds` array
  and the cell's period ID is absent from it. An entity without the array MUST render ordinary empty cells. The
  unavailable cell MUST carry a localized text cue and a CSS class. Color alone is not enough. Class rows MUST NOT gain
  availability marks. Occupied cells MUST NOT be marked.
- Reason: UC-1 G3 and the Empty lens cells table: "empty" must never be read as "available" without a declaration.
- Verification: Browser tests use a teacher with declared availability (`teacher-16` in the investigation scale
  fixture) and one without. They assert the text cue on exactly the empty cells outside the set, and none elsewhere.

### RULE-6 - Presentation actions issue no workspace mutation

- Applies to: UC-1, UC-2, UC-3, UC-4
- Constraint: Applying, changing, or clearing a lens, activating inspector "Show week", toggling teacher investigation
  "Show only matches", and using the narrow picker MUST NOT issue any non-GET request. They MUST leave the stored
  workspace document byte-identical.
- Reason: UC-1 G4, UC-3 G3, UC-4 minimal guarantee.
- Verification: `recordMutations()` returns an empty list across each lens journey, and the stored document compares
  equal before and after.

### RULE-7 - Localized, accessible lens text

- Applies to: UC-1, UC-2, UC-3, UC-4
- Constraint: Every new user-visible or accessible string MUST be a `messages.js` entry. That includes lens row header
  type labels, the unavailable cue, "Show week" labels, the lens criterion remove control, the lens-departure
  announcement, and narrow picker labels. Lens announcements MUST be written to the existing `#inspection-notice`
  status region (or the narrow banner on narrow screens). Tile accessible names MUST follow the "Accessible name adds"
  column of the Normative data table.
- Reason: Presentation and localization convention, plus G3 honesty and G2 no-silent-disappearance.
- Verification: `app.js` contains no literal English strings for these elements. Browser tests assert the accessible
  name content and the notice text by value.

### RULE-8 - Removal is split between UC-1 and UC-4

- Applies to: UC-1, UC-4
- Constraint: UC-1 MUST remove the desktop "Focused schedules" entry buttons (`focusedEntry()`), the desktop
  `renderFocused()` call sites, and the "Return to whole school" control. It MUST add the inspector "Show week" actions
  that replace them. UC-4 MUST remove the narrow agenda, `focused-renderer.js`, `renderFocused()`, and the
  `openFocused`, `changeFocusedType`, `returnToWholeSchool`, `focusedType`, `focusedId`, and `scrollContext` state. It
  MUST also delete message keys that no longer have a caller. No UC may leave a reachable focused surface on desktop
  after UC-1.
- Reason: User decision during the rules pass. Each commit removes only what its own UC replaces, and the narrow view
  is never left without a renderer.
- Verification: After UC-1, `grep` finds no `data-open-focus` or `return-matrix` in production code. After UC-4, `grep`
  finds no `focused-renderer`, `renderFocused`, `openFocused`, or `focusedType` in production code. Browser tests
  assert that these elements are absent from the DOM.

### RULE-9 - Superseded tests are rewritten, not dropped

- Applies to: UC-1, UC-4
- Constraint: Each existing browser-IT assertion that depends on a removed focused element MUST be replaced in the same
  UC commit with an assertion of the lens behavior that supersedes it (per the Superseded clauses table). `Workbench`
  helpers for removed controls (`openFocus`, `focusEntity`, `returnToMatrix`) MUST be removed when their last caller
  goes, and lens vocabulary helpers MUST be added.
- Reason: Approved features (`timetable-inspection-ux`, `timetable-workbench-layout`, repair and proposal review)
  keep their unrelated guarantees under regression. Dropping tests would silently weaken them.
- Verification: The diff shows every removed assertion paired with a lens assertion. The full `timetable-workspace`
  verify suite passes.

### RULE-10 - Scale rendering stays within the existing diagnostic reference

- Applies to: UC-1, UC-2
- Constraint: Applying and clearing a lens on the 1,000-lesson scale school MUST NOT rebuild the accepted model or
  re-fetch the workspace. Lens cell indexing MUST be a single linear pass over the represented assignments per render.
- Reason: The validation-scale snapshot is the proposal's measurement basis. The existing `ScaleTimingBrowserIT`
  records p95 timings as diagnostics.
- Verification: Code inspection shows lens transitions calling `renderWholeSchool()` only. `ScaleTimingBrowserIT`
  stays green.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9, RULE-10 |
| UC-2 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-6, RULE-7, RULE-10 |
| UC-3 | RULE-1, RULE-3, RULE-4, RULE-6, RULE-7 |
| UC-4 | RULE-1, RULE-2, RULE-3, RULE-5, RULE-6, RULE-7, RULE-8, RULE-9 |

## Design exclusions

- Server-side lens support, lens URLs, deep links, or lens persistence of any kind.
- Virtualized or incremental matrix rendering. The single lens row group is small, and class rows keep their current
  rendering.
- Client-side conflict computation. Conflict highlights keep using `manualDraft.conflicts` from the server.
- Rewriting unrelated `app.js` structure beyond what the lens needs.
