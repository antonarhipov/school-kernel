# Technical Rules: School Kernel v1

## Design overview

School Kernel is a single-module, plain Java 25 command-line application. Picocli owns argument parsing and exit-code
mapping; an application service coordinates strict JSON loading, schema and semantic validation, revision calculation,
solver execution, result construction, and atomic publication. Hand-authored Draft 2020-12 schemas are the public
contract. Jackson DTOs map that contract into immutable validated domain values. A separate Timefold adapter maps domain
values into planning facts and entities, applies the fixed constraint catalog, and maps solutions back without leaking
Timefold types into public results. Revision hashing uses an RFC 8785 implementation over explicitly constructed
revision-bearing JSON. The runtime has no persistence or network surface. Tests cover schemas, validation, individual
constraints, solver integration, and packaged CLI processes using isolated temporary files.

## Codebase alignment

The repository currently contains a minimal single-module Maven project and no production conventions to preserve.
Implementation therefore keeps one Maven module, uses package boundaries rather than framework-managed components, and
pins every direct dependency and build plugin. The accepted baseline is Timefold Solver 2.6.0, Jackson 3.2.2,
NetworkNT JSON Schema Validator 3.0.7, Picocli 4.7.7, and java-json-canonicalization 1.1. Release candidates are
excluded. Maven Shade produces the dependency-inclusive executable JAR; a thin `school-kernel` launcher invokes it.

## Security surface

There are no HTTP routes, accounts, authentication flows, static resources, database credentials, or outbound network
calls. The complete external surface is the local `plan` and `replan` commands. Both may read only explicitly named
input files and may write only the explicitly named output through a temporary sibling file. Input content, entity
names, and exception details MUST NOT be logged except that explicit `--debug` may print technical exception details
to stderr after an internal failure. Correlation IDs may identify safe lifecycle messages. Structured results always
use safe messages.

## Verification strategy

JUnit tests use temporary directories and immutable classpath fixtures; they never modify tracked files. Hand-authored
schemas and valid examples validate each other with NetworkNT in offline Draft 2020-12 mode. Semantic validation tests
compare exact, deterministically ordered errors and prove that invalid inputs never invoke the solver. Timefold
Isolated Timefold score-analysis tests exercise matching and non-matching cases for every catalog row, while deterministic
step-limited solver tests assert complete assignments, hard validity, score-vector values, and later replanning
priorities. Packaged-process tests invoke the real dependency-inclusive JAR with real files and assert exit codes,
stdout/stderr separation, exact result values, overwrite refusal, and destination preservation. Negative tests assert
both the response and the absence of publication, mutation, timetable disclosure, or solver invocation. UC-2 tests
consume a result produced through the approved UC-1 production path. A separate non-gating benchmark covers the target
scale.

## Rules

### RULE-1 - Plain Java component boundaries

- Applies to: all use cases
- Constraint: MUST implement a plain Java 25 application with separate external JSON DTO, validated domain, and
  Timefold planning-model boundaries; Timefold annotations and score types MUST NOT appear in the public DTO or domain
  model.
- Reason: The public contract must remain independent of solver and framework upgrades.
- Verification: Architecture tests inspect dependency direction and public model signatures; DTO/domain/planning mapping
  tests compare every field by value.

### RULE-2 - Pinned stable dependency baseline

- Applies to: all use cases
- Constraint: MUST use the pinned stable baseline Timefold Solver 2.6.0, Jackson 3.2.2, NetworkNT JSON Schema Validator
  3.0.7, Picocli 4.7.7, and java-json-canonicalization 1.1, with pinned Maven plugin versions and no release-candidate
  dependencies.
- Reason: The repository has no inherited dependency policy, and reproducible behavior requires an explicit stable
  baseline.
- Verification: Maven dependency and effective-POM checks show the resolved versions; a clean wrapper build succeeds on
  Java 25.

### RULE-3 - Schema-first strict JSON

- Applies to: all use cases
- Constraint: MUST treat bundled, hand-authored Draft 2020-12 schemas as the public structural source of truth, resolve
  them without network access, reject unknown properties and unsupported versions, and configure Jackson 3 for strict
  binding without coercive acceptance of malformed values.
- Reason: DTO refactoring and dependency upgrades must not silently alter the versioned public contract.
- Verification: Schema meta-validation, valid-example tests, unknown-property and wrong-type fixtures, unsupported-version
  fixtures, offline-resolution tests, and DTO round-trip comparisons all pass.

### RULE-4 - Validation precedes solving

- Applies to: all use cases
- Constraint: MUST complete parsing, schema, version, reference, qualification, series, weight-overflow, lock,
  revision, and lineage validation before invoking Timefold; validation failures MUST return deterministic capped
  reports and MUST NOT invoke a solver collaborator.
- Reason: Invalid input is a distinct terminal outcome and must not consume search work or disclose a candidate.
- Verification: Boundary tests inject a recording solver adapter, assert zero invocations for every invalid fixture, and
  compare error totals, truncation flags, ordering, locations, IDs, and safe messages by value.

### RULE-5 - Interoperable content revisions

- Applies to: all use cases
- Constraint: MUST build an explicit revision-scope JSON tree, normalize all specified entity and set ordering, encode it
  with java-json-canonicalization 1.1 according to RFC 8785, hash the UTF-8 bytes with SHA-256, and verify the
  implementation against published RFC 8785 vectors and independent expected digests.
- Reason: Definition and timetable revisions are public interoperability and lineage identifiers, not implementation
  serialization artifacts.
- Verification: Canonicalization vectors, permutation tests, included/excluded-field tests, Unicode and number vectors,
  and tampered-timetable tests assert exact `sha256:<lowercase-hex>` values.

### RULE-6 - Fixed solver score and execution controls

- Applies to: all use cases
- Constraint: MUST configure Timefold programmatically with one hard-feasibility level followed by period-move,
  room-only-move, and ordinary-preference penalty levels, one solver thread, effective seed `0` by default, and exactly
  one positive time or step limit; raw Timefold scores MUST NOT enter the stable result contract.
- Reason: The solver configuration must implement the product's lexicographic priorities and reproducibility boundary
  exactly.
- Verification: Configuration tests inspect level counts, thread count, seed, mutually exclusive limits, and defaults;
  deterministic fixtures prove ordering and compare the product penalty vector and catalog breakdown by value.

### RULE-7 - Exact catalog implementation

- Applies to: all use cases
- Constraint: MUST implement exactly the hard and soft catalog rows in `spec/spec.md`, using stable IDs and signed
  64-bit-safe penalty arithmetic, and MUST NOT introduce an implicit scheduling rule or bonus.
- Reason: Extra or missing constraints change actor-visible feasibility and optimization behavior.
- Verification: One isolated Timefold score-analysis test family per catalog row proves matching and non-matching cases;
  a catalog exactness test compares IDs, order, default weights, categories, counts, and aggregate penalties by value
  and proves absence of extra rows.

### RULE-8 - Diagnostic candidates remain non-results

- Applies to: all use cases
- Constraint: MUST keep any infeasible Timefold candidate inside the solver adapter, map only deterministic hard-match
  evidence into capped search diagnostics, and MUST NOT expose its assignments or label it a timetable, proof of
  infeasibility, or optimal result.
- Reason: Search evidence is useful, but publishing a hard-violating candidate would breach the result contract.
- Verification: Unsuccessful-search process tests assert status, stable constraint evidence, uncapped aggregate counts,
  capped ordered examples, no timetable field, and no `INFEASIBLE` or `OPTIMAL` label.

### RULE-9 - Atomic same-directory publication

- Applies to: all use cases
- Constraint: MUST serialize and close a uniquely named temporary sibling file, then publish it with a same-filesystem
  atomic move; unsupported atomic replacement or any preparation, serialization, close, or move failure MUST be a
  transport failure that preserves the prior destination and removes or abandons no artifact as a successful result.
- Reason: The invocation lifecycle forbids partial results and requires destination preservation.
- Verification: Filesystem boundary tests cover absent and existing destinations, `--force`, equal paths, injected
  serialization and move failures, and assert prior bytes remain identical with no requested partial result.

### RULE-10 - CLI outcome boundary

- Applies to: all use cases
- Constraint: MUST centralize Picocli parsing, application outcomes, interruption handling, and process exit-code mapping
  so machine-readable output is written only to the requested file, normal stdout is empty, and stderr contains only
  human diagnostics allowed by the specification.
- Reason: Exit codes and channel separation are part of the public compatibility surface.
- Verification: Packaged-process tests cover every stable exit code, malformed options, both limits together, unreadable
  inputs, overwrite refusal, handled product failures, debug disclosure, and controllable interruption.

### RULE-11 - Stateless and side-effect-bounded runtime

- Applies to: all use cases
- Constraint: MUST NOT introduce a database, retained invocation state, network access, dependency-injection framework,
  background service, or write outside the requested destination and its temporary sibling.
- Reason: v1 is a stateless local scheduling runtime with a deliberately narrow operational surface.
- Verification: Dependency review, architecture tests, two-invocation isolation tests, and filesystem snapshots prove no
  retained state or unrelated writes.

### RULE-12 - Complete executable distribution

- Applies to: all use cases
- Constraint: MUST produce one Maven-Shade dependency-inclusive executable JAR and a `school-kernel` launcher, with a
  committed Maven Wrapper and a reproducible clean-checkout Java 25 build.
- Reason: The documented CLI must be directly runnable without reconstructing a classpath.
- Verification: A clean wrapper `verify` build produces the JAR; launcher and `java -jar` smoke tests execute the same
  commands and return the same exit codes and result bytes apart from explicitly non-revision timing fields.

### RULE-13 - UC-1 complete-result invariant

- Applies to: UC-1
- Constraint: MUST map a solution to `FEASIBLE` only after independently verifying that every active lesson has exactly
  one eligible period and room and every hard rule passes; empty input MUST bypass Timefold and produce
  `EMPTY_PROBLEM`.
- Reason: Solver completion alone is insufficient evidence that a publishable timetable satisfies the UC-1 success
  postcondition.
- Verification: UC-1 packaged-process fixtures assert assignments and hard rules by value; injected partial or
  hard-negative adapter results are refused and publish no timetable; an empty fixture proves zero solver invocations.

### RULE-14 - UC-2 consumes verified UC-1 output

- Applies to: UC-2
- Constraint: MUST accept a current timetable only through the same result parser and revision verifier used for UC-1
  output, require the UC-1 `FEASIBLE` postcondition and direct lineage, and preserve the submitted current file as an
  immutable comparison baseline.
- Reason: UC-2 requires and consumes UC-1's approved postcondition rather than duplicating or weakening it.
- Verification: UC-2 end-to-end tests generate the baseline through the UC-1 production command, then prove valid
  consumption, tamper rejection, lineage rejection, and byte-identical preservation of the current file.

### RULE-15 - Replanning stability and change classification

- Applies to: UC-2
- Constraint: MUST derive stability facts and change-report categories from common lesson IDs and their verified
  assignment snapshots, exclude additions, cancellations, and explicitly forced dimensions from solver move penalties,
  and deterministically classify every observable change exactly once per applicable category.
- Reason: Stability ordering and change reporting are the actor goal of UC-2.
- Verification: Small step-limited fixtures prove period stability dominates room stability, room stability dominates
  preferences, and additions, cancellations, teacher changes, forced moves, period moves, and room-only moves have exact
  counts, fields, ordering, and non-overlap semantics.

### RULE-16 - Isolated layered verification

- Applies to: all use cases
- Constraint: MUST verify each use case with isolated schema, semantic-validation, constraint, solver-integration, and
  packaged-process tests, using temporary files and deterministic step-limited correctness fixtures; tests MUST NOT
  modify runtime data or tracked generated files.
- Reason: Unit-level score checks alone do not prove the actor-visible CLI, publication, and state guarantees.
- Verification: The Maven lifecycle runs all layers, records test counts, leaves `git status --short` unchanged, and
  separates the reproducible target-scale benchmark from correctness gates.

### RULE-17 - Safe observability

- Applies to: all use cases
- Constraint: MUST use an SLF4J-compatible stderr logging backend configured to avoid stdout and MUST NOT log complete
  input documents, generated timetables, user display names, or exception details unless `--debug` explicitly permits
  the latter for an internal failure; correlation IDs and safe lifecycle summaries MAY be logged.
- Reason: Logs must support diagnosis without creating a second machine-readable channel or leaking school data.
- Verification: Captured-channel tests inspect normal, validation, search-failure, internal-error, and debug invocations
  for required correlation and prohibited content.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1 through RULE-13, RULE-16, RULE-17 |
| UC-2 | RULE-1 through RULE-12, RULE-14 through RULE-17 |

## Design exclusions

No web or RPC API, persistence, authentication, YAML support, plugin constraint system, solver-selected teachers,
native image, module split, framework container, globally optimality-proving search, or network schema resolution is
introduced. Benchmark wiring may use a dedicated Maven profile, but benchmark thresholds do not gate correctness.
