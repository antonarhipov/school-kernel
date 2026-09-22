# Use-Case Status: School Timetable Operations Workspace

## Current

- Use case: UC-8
- Status: PENDING_WALKTHROUGH
- Next eligible: none

## Progress

| Use case | Status | Depends on | Implementation | Convergence |
|---|---|---|---|---|
| UC-1 | APPROVED | none | `854493f`; revises `2671b2a` for C-1 through C-3 and G-1 through G-10 | `convergence/UC-1.md` - APPROVED |
| UC-2 | APPROVED | UC-1 | `b7180a9`; started from `dfe0e75` | `convergence/UC-2.md` - APPROVED WITH NOTES |
| UC-3 | APPROVED | UC-1 | `1a796d2`; walkthrough startup `2523205`; started from `a9ab56a` | `convergence/UC-3.md` - APPROVED |
| UC-4 | APPROVED | UC-1 | `5f545ee`; started from `f3b9253` | `convergence/UC-4.md` - APPROVED |
| UC-5 | APPROVED | UC-4 | `75a6c22`; revises `340cf31` for C-1 and G-1 through G-3; started from `c7ea4f8` | `convergence/UC-5.md` - APPROVED after walkthrough confirmation |
| UC-6 | APPROVED | UC-5 | `844b3d1` resolves C-1; original `f02223d` | `convergence/UC-6.md` - APPROVED after repaired walkthrough confirmation |
| UC-7 | APPROVED | UC-1; includes UC-3, UC-4, UC-5, UC-6 | `b02ce5f` incorporates the non-blocking performance decision after C-3; `ccfc5d1` resolves C-2; `433ea70` resolves C-1; original `4acf363` | `convergence/UC-7.md` - APPROVED after walkthrough confirmation |
| UC-8 | PENDING_WALKTHROUGH | UC-1 | `b1e8077`; started from `bcce1c443385ac4b231b1eae75f5c30b0c601608` | `convergence/UC-8.md` - PENDING WALKTHROUGH |

## UC-1 Evidence

- Started: 2026-09-20T22:00:15Z
- Started from: `db18108c24ddc14b9a1496f0a258f47d3d06c870`
- Pre-existing dirty files: none
- Revision started: 2026-09-21; base `d0e90317359b08be026c9d8f9a8f37195cbea3f7`; user selected the required
  school-level `displayName` contract resolution for C-1.
- Implementation submission: HEAD at convergence
- Changed files in this revision: kernel verification/publication architecture tests; workspace process, security, problem,
  archive, import, credential, and UI production files; Flyway/process/browser/archive/HTTP tests; decisions, status,
  checkpoint, Jev bundle, and `output/playwright/uc1-display-name.png`.
- Commands and results: `mvn -q clean verify` — 120 tests, 0 failures, 0 errors, 0 skipped; focused malformed
  multipart test — PASS; independent kernel suites and packaged plan -> replan -> verify — PASS; `git diff --check` —
  PASS; visible Playwright import — `Demo School`, exact revision, zero console errors/warnings; Jev bundle local
  validation — valid, 33 items; external Jev preflight — REVIEW because disclosure was not authorized.

| Contract element | Evidence |
|---|---|
| UC-1 main steps 1-4 | Real HTTP definition, accepted-pair, and exact-archive journeys at `timetable-workspace/src/test/java/org/schoolkernel/workspace/WorkspaceImportIT.java:93`, `:115`, and `:141`; browser initial-draft journey in `output/playwright/uc1-initial-draft.png` |
| UC-1 extension 1a | Result-only HTTP request returns `MATCHING_DEFINITION_REQUIRED`, exposes no assignments, and preserves exact empty state at `WorkspaceImportIT.java:182` |
| UC-1 extension 1b | Bounded in-memory ZIP tests at `SafeImportReaderTest.java:41,56` and real HTTP unsafe-archive rejection at `WorkspaceImportIT.java:141` |
| UC-1 extension 2a | Packaged strict verification at `kernel-cli/src/test/java/org/schoolkernel/cli/VerifyCliIT.java:78,111`, shared hard-baseline mutation coverage, and HTTP no-mutation assertions |
| UC-1 extension 2b | Successor-only packaged verify refusal at `VerifyCliIT.java:78` |
| UC-1 extension 2c | Exact complete `FEASIBLE` packaged verify and accepted-pair HTTP persistence at `VerifyCliIT.java:56` and `WorkspaceImportIT.java:115` |
| UC-1 extension 4a | Accepted-pair PostgreSQL failure injection and complete rollback at `WorkspaceImportIT.java:333` |
| UC-1 G1-G5 | Distinct import-state assertions, full JSON comparison, persistent-lock manifest test, isolated version validation, and local/no-partial-state security tests recorded in `checkpoints/UC-1.md` |
| UC-1 success postcondition | Exact singleton state/version/document assertions after both successful modes |
| UC-1 minimal guarantee | All refused/failing HTTP journeys assert `EMPTY`, version `0`, `{}` JSONB, and candidate absence |
| RULE-1, RULE-2 | Reactor/contract archive/dependency/architecture checks plus clean Java 25 build |
| RULE-3 through RULE-9 | PostgreSQL aggregate, Flyway, lifecycle, ETag race, transaction rollback, canonical document fidelity, and packaged verify evidence in `checkpoints/UC-1.md` |
| RULE-13, RULE-14 | Private bounded process adapter and strict no-extraction archive reader/tests |
| RULE-21 through RULE-24 | Loopback Host/Origin/CSRF/CORS route tests, safe problem responses/logs, real PostgreSQL/HTTP/browser boundary |
| RULE-26 through RULE-28 | Typed shared handlers, controlled catalog/version/public solver APIs, bounded input and race-safe publication suites |

## UC-2 Evidence

- Started: 2026-09-21; base `dfe0e75`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: initial-planning service, packaged planner adapter, recovery hook, repository/mutation/controller/security/import/problem extensions, native workspace UI, planner/HTTP/browser tests, two Playwright screenshots, status, checkpoint, and Jev advisory artifacts.
- Commands and results: focused `KernelPlannerTest` — 6 tests PASS; focused `WorkspaceInitialPlanningIT` — 8 tests PASS; `./mvnw -q clean verify` — 135 tests, 0 failures, 0 errors, 0 skipped; `git diff --check` — PASS; Jev bundle validation — valid, 38 items; authorized external `jev-1.13.0` preflight — 13 findings, 22 reviews, 3 clear, with every flag independently dispositioned in `checkpoints/UC-2.md`.
- Runtime evidence: visible real-browser import -> production 30-second plan -> feasible initial proposal -> checkbox-confirmed acceptance opened `Demo School` at timetable revision `sha256:232c53bcf2ee6b373cb3965afc87849d245040c9c171f0cc543dff8742bf6b3c`; browser console reported 0 messages, errors, or warnings; screenshots are `output/playwright/uc2-initial-proposal.png` and `output/playwright/uc2-accepted-baseline.png`.

| Contract element | Evidence |
|---|---|
| UC-2 main steps 1-6 | Real PostgreSQL/HTTP/package journey at `WorkspaceInitialPlanningIT.java:100`; real Chrome journey at `WorkspaceBrowserIT.java:134`; visible production-limit journey and screenshots above |
| UC-2 extension 1a | Replacement success, unsuccessful search preservation, and invalid replacement no-mutation assertions at `WorkspaceInitialPlanningIT.java:179` |
| UC-2 extensions 2a and 4a | Cancel/late-result suppression and discard preserve the exact draft with no proposal at `WorkspaceInitialPlanningIT.java:148`; forced process cancellation at `KernelPlannerTest.java:86` |
| UC-2 extension 2b | Recovery of both interrupted solving and unaccepted proposal states at `WorkspaceInitialPlanningIT.java:214` and `WorkspaceRepository.java:92` |
| UC-2 extensions 3a and 3b | Safe invalid, no-feasible, internal, transport, interruption, watchdog, missing/malformed, and mismatched-output results at `KernelPlannerTest.java:67,126,165,214`; HTTP failure-class and mismatch journeys at `WorkspaceInitialPlanningIT.java:215,230` |
| UC-2 extension 5a | Stale identity invalidation plus the independent every-identity/result matrix at `WorkspaceInitialPlanningIT.java:261,394` |
| UC-2 extension 6a | Kernel revalidation unavailability and injected PostgreSQL acceptance failure retain the exact proposal/version at `WorkspaceInitialPlanningIT.java:261` |
| UC-2 G1-G6 and both postconditions | Explicit-confirmation browser/HTTP acceptance, exact definition/result comparison, safe labels/controls, all-or-nothing rollback, and retryable draft/proposal assertions recorded in `checkpoints/UC-2.md` |
| RULE-1 through RULE-29 applicable to UC-2 | Reactor, lifecycle, transaction, process, security, packaged-kernel, PostgreSQL/browser, and kernel compatibility evidence recorded by rule in `checkpoints/UC-2.md` |

## UC-3 Evidence

- Started: 2026-09-21; base `a9ab56a950d32e2f77d5daddffbc156a70efb1b9`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: native accepted-inspection UI and centralized message catalog; architecture, HTTP, real-browser,
  empty-state, immutability, keyboard, narrow-screen, complete-snapshot, and target-scale tests; status, checkpoint, and
  Jev advisory artifacts.
- Commands and results: focused real-browser keyboard journey — PASS; `./mvnw -q clean verify` — 140 tests, 0
  failures, 0 errors, 0 skipped; `git diff --check` — PASS. Latest target-scale p95: search 69.5 ms, filter 38.8 ms,
  day 172.6 ms, selection 81.1 ms; solver time excluded.
- Walkthrough startup enablement: `2523205` adds Spring Boot-managed `postgres:18.6` Compose startup, loopback-only
  dynamic publication, health check, persistent named volume, packaged-JAR support, isolated-test disablement, and
  one-command documentation. Live packaged smoke returned `EMPTY` with ETag `ws-0`, served `/workspace/`, stopped the
  database with the application, and retained its volume. Final `./mvnw -q clean verify` — 141 tests, 0 failures,
  0 errors, 0 skipped; final loaded-suite day-change p95 199.4 ms after pre-indexing accepted assignments.

| Contract element | Evidence |
|---|---|
| UC-3 main steps 1-5 | Real Chrome whole-school, local-filter, keyboard selection, assignment-detail, focused schedule, and return journey at `WorkspaceBrowserIT.java:185` |
| UC-3 extensions 2a, 3a, 3b, 5a | Empty accepted browser journey at `WorkspaceBrowserIT.java:255`; filtered/no-match/narrow-screen assertions at `:211-250` |
| UC-3 G1-G6 | Opaque-ID/metadata rendering, catalog/state text, keyboard, scale p95, and exact non-mutation evidence in `checkpoints/UC-3.md` |
| UC-3 success postcondition | Browser identifies accepted assignment and complete/focused school context while preserving repair entry context |
| UC-3 minimal guarantee | Empty/no-match/presentation changes compare the exact unchanged PostgreSQL document and invent no assignments |
| Requires UC-1 | Approved UC-1 accepted pair consumed by complete production snapshot test at `WorkspaceBrowserIT.java:280` |
| RULE-1, RULE-2, RULE-4, RULE-19 through RULE-25 | Reactor, architecture, Flyway, snapshot, catalog, security, safe failure/logging, PostgreSQL/browser, and scale evidence in `checkpoints/UC-3.md` |

## UC-4 Evidence

- Started: 2026-09-21; base `f3b9253b67f9be528cd6b6ff90343ba9b4992f28`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: repair draft service, controller/mutation/security/problem handling, native workspace UI/catalog/styles,
  PostgreSQL/HTTP and real-browser tests, status, checkpoint, and 35-item Jev bundle.
- Commands and results: focused `WorkspaceRepairDraftIT` - 9 tests PASS; real-Chrome UC-4 journey - PASS;
  deterministic DAY/CLASS/filter/altered/stale/empty bulk snapshots - PASS; `./mvnw -q clean verify` - 152 tests,
  0 failures, 0 errors, 0 skipped, with final pin-feedback p95 166.3 ms; `git diff --check` - PASS; Jev compiled helper `--validate-only` - valid,
  35 items; external Jev - REVIEW because disclosure was not authorized.

| Contract element | Evidence |
|---|---|
| UC-4 main steps 1-7 | Teacher and room real HTTP/PostgreSQL journeys at `WorkspaceRepairDraftIT.java:68,102`; complete real-Chrome desktop/narrow journey at `WorkspaceBrowserIT.java:301` |
| UC-4 extensions 1a, 2a, 7b, 7c | Unsupported/no-effect/durable-reload/confirmed-discard assertions at `WorkspaceRepairDraftIT.java:186` |
| UC-4 extensions 4a and 7a | Attempt/policy conflict and resolution matrix at `WorkspaceRepairDraftIT.java:128` |
| UC-4 extensions 5a and 6a | Non-mutating preview, full server-recomputed confirmation, DAY/CLASS/filter selection, altered/stale refusal, overlapping provenance, and source-scoped undo at `WorkspaceRepairDraftIT.java:152` |
| UC-4 extension 6b | PostgreSQL autosave failure injection, rollback, safe 503, and refused run at `WorkspaceRepairDraftIT.java:233` |
| UC-4 G1-G5 and both postconditions | Overlay compilation, canonical intent/effect revisions, immutable snapshots, visible states, and exact accepted-bundle comparisons recorded in `checkpoints/UC-4.md` |
| UC-4 G6 | 1,000-lesson persisted pin-feedback test at `WorkspaceBrowserIT.java:432`; final p95 166.3 ms below 250 ms |
| Requires UC-1 | Approved exact accepted definition/result/manifest is consumed through the production singleton aggregate in every UC-4 integration journey |
| RULE-1 through RULE-25 applicable to UC-4 | Reactor, lifecycle, concurrency, fidelity, overlay/snapshot, browser, accessibility, security, failure, observability, PostgreSQL, and scale evidence recorded by rule in `checkpoints/UC-4.md` |

## UC-5 Evidence

- Started: 2026-09-21; base `c7ea4f82045db1c156b667ed7881e6d1e29d4fec`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: repair-planning service; planner, repair compiler guard, controller, mutation, recovery, and repository integration; native workspace UI/messages; planner, PostgreSQL/HTTP, and real-browser tests; accepted-result fixture; status, checkpoint, and Jev bundle.
- Commands and results: expanded `WorkspaceRepairPlanningIT` - 5 tests PASS; real-Chrome UC-5 journey - PASS; focused UC-4 pin-feedback p95 149.0 ms - PASS; `./mvnw -q clean verify` - 159 tests, 0 failures, 0 errors, 0 skipped, with UC-4 pin-feedback p95 107.4 ms; `git diff --check` - PASS; Jev bundle validation through the already-built pinned helper - valid, 37 items; external Jev - REVIEW because disclosure was not authorized.

| Contract element | Evidence |
|---|---|
| UC-5 main steps 1-5 | Real packaged HTTP/PostgreSQL journey at `WorkspaceRepairPlanningIT.java:72`; real Chrome journey at `WorkspaceBrowserIT.java:375` |
| UC-5 extensions 2a, 2b, 3a | Cancellation, stale ETag, and startup-recovery exact-state assertions at `WorkspaceRepairPlanningIT.java:166` |
| UC-5 extensions 4a through 4d | Real HTTP/PostgreSQL unsuccessful, validation, internal, transport, interruption, timeout, and retry journeys at `WorkspaceRepairPlanningIT.java:130,174,208`; adapter details remain covered by `KernelPlannerTest` |
| UC-5 extension 5a | Real HTTP rejection and stale authoritative-completion suppression at `WorkspaceRepairPlanningIT.java:174,208`; exact adapter evidence and independent verification at `KernelPlannerTest.java:214` |
| UC-5 G1-G7 and both postconditions | Frozen controls, exact identity/evidence, accepted immutability, safe diagnostics, and proposal/minimal-state assertions recorded in `checkpoints/UC-5.md` |
| Requires UC-4 | Approved UC-4 production draft/compiler path supplies the exact intent revision and complete successor definition in every UC-5 success journey |
| RULE-3 through RULE-29 applicable to UC-5 | JSONB lifecycle, ETag, canonical process, async/conflict/cancel/watchdog/stale completion, overlay/identity, security/failure/logging, PostgreSQL/browser, typed kernel, metadata/input, and corpus evidence recorded by rule in `checkpoints/UC-5.md` |

## UC-7 Evidence

- Started: 2026-09-22T08:36:00Z
- Started from: `c53b1a624bfd7bda319bb28bdff0fec9095fab40`
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: attempt-lock manifest guard, manifest regression, composite two-repair PostgreSQL/HTTP and real-Chrome
  journeys, status, checkpoint, and Jev advisory bundle.
- Commands and results: focused manifest unit suite - PASS; focused two-repair HTTP/PostgreSQL/packaged-kernel journey -
  PASS; focused two-repair real-Chrome journey - PASS; `./mvnw -q clean verify` - 167 tests, 0 failures, 0 errors, 0
  skipped; `git diff --check` - PASS; Jev bundle local validation - valid, 46 items; external Jev - REVIEW because
  disclosure was not authorized.
- Runtime evidence: real Chrome accepts a protected teacher repair, opens the exact accepted result as current, starts a
  later room repair with no inherited attempt pin, distinguishes direct and ripple effects, and accepts the second
  independently verified packaged-kernel proposal with zero browser errors.
- Revision started: 2026-09-22; base `7c76b2e`; resolves convergence C-1 without weakening the approved UC-4 actor
  assertions or real-browser boundary.
- Revision validation: complete 10-test real-Chrome class - PASS; final `./mvnw -q clean verify` - 167 tests, 0
  failures, 0 errors, 0 skipped; `git diff --check` - PASS. Chrome diagnostics are discarded instead of accumulating
  in an unread child-process pipe; actor assertions and the 15-second CDP command timeout are unchanged.
- C-2 revision started: 2026-09-22; base `dcf486c`; closes each Testcontainers-backed Spring context after its test
  class so stopped-container connection pools cannot load later validation-scale browser measurements.
- C-2 revision validation: complete repair-draft plus 10-test real-Chrome classes - PASS; final `./mvnw -q clean
  verify` - 167 tests, 0 failures, 0 errors, 0 skipped; validation-scale p95 was 155.9 ms for persisted pin feedback
  and 71.5 ms for day changes, with proposal review opening in 348.6 ms; `git diff --check` - PASS. Day matrices are
  prepared after initial load and swapped in place, while the minimal pin command conditionally updates only the
  authoritative repair-draft JSONB subtree after reading the selected accepted assignment and lock from that same
  aggregate. Test Spring contexts close after each Testcontainers-backed class.
- Changed files in C-2 revision: scoped repair-draft persistence and ETag helpers; native cached day-matrix rendering;
  Testcontainers context lifecycle annotations in all five Spring integration classes; status, checkpoint, and Jev
  advisory bundle.
- C-3 revision started: 2026-09-22; base `1240cdc`; pre-existing dirty files: none. The administrator explicitly
  revised the product contract so the 250 ms interaction and one-second review values remain diagnostic evidence but
  no longer block functional delivery or convergence; fixtures, raw samples, p95 calculations, actor behavior, and
  persistence assertions remain unchanged.
- C-3 revision validation: clean complete 10-test real-Chrome class - PASS; final `./mvnw -q clean verify` - 167 tests,
  0 failures, 0 errors, 0 skipped; `git diff --check` - PASS; revised 46-item Jev bundle - locally valid; external Jev -
  REVIEW because disclosure was not authorized. Full-reactor diagnostic p95 was search 56.4 ms, filter 49.5 ms, day
  44.1 ms, selection 47.5 ms, persisted pin feedback 86.6 ms, and proposal review 370.6 ms. The relaxed contract does
  not invalidate approved UC-3, UC-4, or UC-6: their functional implementations and evidence remain unchanged and
  their earlier approvals were obtained under the stronger threshold contract.

| Contract element | Evidence |
|---|---|
| UC-7 main steps 1-7 and extension 7a | Two-repair production journeys at `WorkspaceRepairPlanningIT.java:195` and `WorkspaceBrowserIT.java:376` inspect the accepted timetable, stage teacher unavailability plus a room pin, generate/review/accept, then stage a different-room disruption from the exact new result and generate/review/accept again |
| UC-7 extensions 3a, 4a, 5a, 5b | Approved conflict, process-failure/cancellation/retry, proposal discard/revise, and PostgreSQL acceptance rollback regressions rerun green in the clean reactor; exact references are recorded in `checkpoints/UC-7.md` |
| UC-7 G1-G5 and both postconditions | Exact accepted JSON comparisons, visible state labels, native browser-only journey, direct/ripple separation, revision equality, transient-state cleanup, and failure invariants are recorded in `checkpoints/UC-7.md` |
| Requires/Includes UC-1, UC-3, UC-4, UC-5, UC-6 | All dependencies are approved; the composite tests use their production snapshot, draft, packaged-run, review, and acceptance paths without adding a duplicate orchestrator |
| RULE-1 through RULE-29 applicable to UC-7 | Reactor, lifecycle, concurrency, atomic acceptance, process, overlay, identity, browser/accessibility, security, failure, observability, PostgreSQL, scale, typed-kernel, and corpus evidence is recorded by rule in `checkpoints/UC-7.md` |

## UC-8 Evidence

- Started: 2026-09-22; base `bcce1c443385ac4b231b1eae75f5c30b0c601608`.
- Pre-existing dirty files: none.
- Implementation submission: HEAD at convergence.
- Changed files: accepted-baseline export service and ZIP adapter; shared bounded archive reader; controller and security
  route; native accepted-state export UI/messages/styles; PostgreSQL/HTTP and real-Chrome tests; status, checkpoint, and
  Jev advisory bundle.
- Commands and results: focused `WorkspaceImportIT` - PASS; focused real-Chrome UC-8 journey - PASS; escalated
  `./mvnw -q clean verify` - 170 tests, 0 failures, 0 errors, 0 skipped; `git diff --check` - PASS. The first sandboxed
  clean attempt was blocked by Docker-socket permission and was rerun unchanged with approved Docker access. The
  26-item Jev bundle validates locally with the already-built pinned helper; external review is `REVIEW` because
  disclosure was not authorized.

| Contract element | Evidence |
|---|---|
| UC-8 main steps 1-5 | Real HTTP/PostgreSQL/package export and exact re-import at `WorkspaceImportIT.java:209`; real Chrome identity and ZIP receipt at `WorkspaceBrowserIT.java:190` |
| UC-8 extensions 1a, 3a, 4a | Empty-state refusal plus injected archive-creation and completed-archive corruption paths at `WorkspaceImportIT.java:266`; all return safe problems with no attachment and exact state preservation |
| UC-8 G1-G4 and both postconditions | Exact normative entry order, canonical accepted documents, `ATTEMPT_SCOPED` provenance, packaged `verify`, re-import equality, same-origin browser receipt, and complete aggregate before/after assertions recorded in `checkpoints/UC-8.md` |
| Requires UC-1 | Approved UC-1 production import and packaged verification path re-imports the exported archive to the exact accepted baseline |
| RULE-1, RULE-2, RULE-3, RULE-8, RULE-9, RULE-13, RULE-14, RULE-18, RULE-21 through RULE-24, RULE-26 through RULE-28 | Reactor, canonical JSON, structured verify, bounded private files/archive parsing, exact manifest/ZIP, local security, safe failure/logging, PostgreSQL/browser, typed kernel, controlled metadata, and bounded publication evidence recorded by rule in `checkpoints/UC-8.md` |

## Blockers

None. The user resolved C-1 on 2026-09-21 by selecting a required school-level `displayName` in the kernel definition.

## Deviations

None.
