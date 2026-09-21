# School Kernel v1: Accepted Design Decisions

Status: Accepted  
Decision date: 2026-09-20  
Source: Design grilling of [`proposal.md`](proposal.md)

This document records the complete set of accepted decisions for School Kernel v1. It resolves ambiguities in the proposal and is normative for the subsequent specification and implementation. Decision identifiers preserve the order in which the decisions were accepted.

## Scheduling problem and domain

- **D001 — Scheduling horizon:** v1 schedules a recurring school week made of fixed, non-overlapping periods. Dated calendars and time zones are deferred.
- **D002 — Assignment authority:** Every required lesson has a preassigned teacher and cohort. Timefold chooses its period and room; teacher allocation is outside the v1 scheduling problem.
- **D003 — Cohort model:** A cohort is an indivisible student group. Split, merged, and overlapping groups are outside v1.
- **D007 — Lesson occurrences:** Each schedulable lesson occurrence has its own stable ID. Related occurrences may share a series ID.
- **D008 — Lesson duration:** Every lesson occupies exactly one period. Multi-period activities are outside v1.
- **D016 — Room eligibility:** Cohorts have sizes, rooms have capacities and capability tags, and lessons declare required room capabilities. Preferred rooms are a soft preference.
- **D026 — Period model:** Each period has a stable ID, ISO weekday, order within that day, and optional display times. Periods must be unique and non-overlapping within the week.
- **D027 — Teacher qualification:** A lesson assigned to a teacher who is not qualified for its subject is rejected during validation; it is not left for the solver to repair.
- **D028 — Timetable completeness:** A feasible timetable assigns every active lesson both a period and a room. Partial timetables are never successful results.
- **D048 — Availability owners:** Teachers, cohorts, and rooms may declare availability. A lesson's eligible periods derive from the availability of those resources and any explicit period lock.
- **D055 — Lesson identity across revisions:** A lesson may retain its ID when its teacher, room requirements, preferences, or locks change. A change of subject or cohort creates a new lesson and requires a new ID.
- **D061 — Series consistency:** All occurrences in a series have the same subject and cohort. They may have different preassigned teachers.
- **D065 — Domain terminology:** The external contract uses `cohort`, not `class`.
- **D066 — Resource cardinality:** Each lesson has exactly one teacher, one cohort, and one assigned room. Team teaching, merged cohorts, and multi-room lessons are deferred.
- **D068 — Explicit exclusions:** Workload limits, lunch rules, travel time, consecutive-period rules, and daily subject limits are not implicit. They require separately specified catalog constraints before becoming part of the product.
- **D069 — Identifier syntax:** IDs are case-sensitive and match `[A-Za-z0-9][A-Za-z0-9._-]{0,127}`. Human-readable names remain unrestricted Unicode metadata.
- **D073 — Target scale:** v1 is deliberately designed and benchmarked for one school week containing up to 1,000 lessons, 100 teachers, 60 cohorts, 100 rooms, and 60 periods.
- **D109 — Empty problem:** A definition with no lessons is valid and immediately produces a feasible empty timetable.
- **D110 — Weekdays:** Periods may use any ISO weekday from Monday through Sunday. The declared periods determine the actual school week.
- **D111 — Display names:** The school and every entity have a nonblank display name. Display names need not be unique;
  IDs alone establish identity and references.

## Feasibility, validation, and diagnostics

- **D004 — Infeasible outcomes:** The kernel never presents a hard-violating timetable as valid. If no feasible timetable is available, it returns no timetable and instead produces a structured unsuccessful result with diagnostics.
- **D013 — Failure classification:** Expiry of a search limit without a feasible solution yields `NO_FEASIBLE_SOLUTION_FOUND`, not `INFEASIBLE`. `INFEASIBLE` is reserved for an actual proof.
- **D023 — Validation:** Detectable schema, reference, and semantic errors are collected before Timefold is invoked. Invalid input produces a structured failure without starting the solver.
- **D032 — Search diagnostics:** `NO_FEASIBLE_SOLUTION_FOUND` includes aggregated hard-constraint violations from the best diagnostic candidate. These are explicitly diagnostics, not a timetable or proof of infeasibility.
- **D038 — Contradictory locks:** Directly detectable conflicts between locks, availability, or other locked lessons are invalid input and are reported before solving.
- **D054 — Diagnostic detail:** Failed-search diagnostics contain stable constraint IDs and involved entity IDs in deterministic order. Detailed entries are capped, while aggregate counts are not.
- **D071 — Infeasibility proofs:** v1 does not implement special-purpose proof procedures. It detects deterministic validation contradictions but otherwise reports `NO_FEASIBLE_SOLUTION_FOUND` rather than claiming proof.
- **D078 — Result statuses:** Stable top-level statuses are `FEASIBLE`, `NO_FEASIBLE_SOLUTION_FOUND`, `INVALID_INPUT`, and `INTERNAL_ERROR`. `INFEASIBLE` may be introduced only with proof-producing validation.
- **D079 — Optimality claims:** A run reports `OPTIMAL` only if optimality was actually proven. Ordinary successful runs report `FEASIBLE` with a termination reason and scoring breakdown.
- **D086 — Unresolved conflicts:** A feasible timetable cannot contain unresolved hard conflicts. Failed runs contain diagnostic hard violations; feasible runs may contain unmet soft preferences.
- **D108 — Obvious unschedulability:** A well-formed lesson with no eligible room or period short-circuits to `NO_FEASIBLE_SOLUTION_FOUND` with a deterministic diagnostic. It is not classified as invalid input.
- **D112 — Validation volume:** At most 1,000 detailed validation errors are returned, together with the total detected count and a truncation flag.

## Constraints and scoring

- **D005 — Constraint customization:** v1 has a versioned built-in constraint catalog. Users may enable catalog preferences and configure soft weights; there is no user-defined constraint DSL.
- **D009 — Availability semantics:** Availability is a binary hard rule. Undesirable periods are modeled separately as soft preferences.
- **D011 — Optimization priority:** Scoring is lexicographic: hard feasibility first, then period stability, then room stability, and finally ordinary preferences.
- **D015 — Score abstraction:** Raw Timefold scores are not part of the stable product contract. Results expose product-level status and constraint information; a raw score may appear only as optional diagnostic metadata.
- **D017 — Lesson distribution:** Distribution of series occurrences across the week is a weighted preference, not a universal hard rule.
- **D025 — Constraint breakdown:** Each catalog constraint reports its stable ID, category, configured weight, match count, and aggregate impact. Timefold's internal score representation is not exposed as the contract.
- **D030 — Hard constraints:** Users cannot disable hard constraints. They define timetable validity; only soft constraints can be disabled or reweighted.
- **D031 — Soft-weight rules:** Weights are non-negative bounded integers. A weight of zero disables that preference.
- **D041 — Initial preference catalog:** v1 includes teacher gaps, lesson-series distribution across days, undesirable periods, and preferred rooms.
- **D049 — Teacher gaps:** A gap is an available, unassigned period between a teacher's first and last assigned lessons within one continuous availability block on a day. Unavailable periods break the span and do not count as gaps.
- **D050 — Series distribution penalty:** Each additional occurrence of the same series on the same day adds one match before weighting.
- **D051 — Undesirable periods:** Teachers, cohorts, and individual lessons may mark periods undesirable. When several applicable preferences match, their impacts are additive.
- **D052 — Preferred rooms:** Preferred room IDs form an unordered, equally preferred set.
- **D053 — Preference aggregation:** Catalog preference penalties combine as a weighted sum within the ordinary-preference tier. Feasibility and both stability tiers always dominate that sum.
- **D062 — Weight range:** Soft weights are integers in `0..1,000,000`. Impacts use 64-bit arithmetic, and configurations whose maximum possible aggregate may overflow are rejected.
- **D067 — Initial hard-constraint catalog:** v1 enforces teacher, cohort, and room non-collision; teacher, cohort, and room availability; room capacity; required room capabilities; and explicit locks. References and teacher qualifications are validated before solving.
- **D070 — Default configuration:** The versioned catalog supplies default soft weights; definitions contain only overrides. Results record the fully resolved effective configuration.
- **D072 — Solver score levels:** The internal solver score has one hard-feasibility level followed by three lexicographic levels for period stability, room stability, and ordinary preferences.
- **D077 — Default weights:** Every enabled v1 soft preference defaults to weight `1`, avoiding hidden educational policy.
- **D106 — Product score:** The stable result exposes a non-negative penalty vector containing period moves, room-only moves, and ordinary preference penalty, where lower is better, plus the per-constraint breakdown.
- **D107 — Penalties only:** All v1 preferences are expressed as penalties for undesirable outcomes. The catalog does not award positive bonuses.
- **D116 — Catalog evolution:** A change to catalog semantics or default weights increments the separate catalog version, not the JSON schema version. Results record both the catalog version and effective weights.

## Replanning and stability

- **D010 — Replanning inputs:** Replanning consumes a complete updated school definition and a current timetable. There is no separate `changes.yaml` or change-command document in v1.
- **D018 — Manual intervention:** Period and room locks are explicit. An assignment is never inferred to be locked merely because it appears in the current timetable.
- **D019 — Stale-input protection:** School definitions and timetables carry revision identities, and replanning rejects incompatible or unknown ancestry.
- **D020 — Change accounting:** Stability compares only lesson IDs present in both timetables. Additions and cancellations are excluded from move counts and reported separately.
- **D033 — Change classification:** Externally requested changes, such as a teacher reassignment or cancellation, are separated from solver-induced period and room disruption.
- **D036 — Replanning lineage:** Each definition has a stable `schoolId`. An updated definition has a `basedOnRevision` that must equal the current timetable's input revision.
- **D037 — Forced changes:** Assignments changed by explicit locks or other authoritative external changes are excluded from solver disruption penalties and are reported separately.
- **D043 — Edited timetables:** Users cannot manually alter generated timetable assignments and submit them as valid history. The kernel verifies the content-derived revision; intended manual changes are expressed through definition locks.
- **D045 — Direct ancestry:** Replanning requires the submitted timetable to come from the updated definition's direct parent revision. Branching is done explicitly by creating another definition from that parent.
- **D046 — Baseline validity:** The current timetable must be internally valid and have verified provenance, but it need not remain valid under the updated definition. New unavailability and cancellations are legitimate reasons to replan.
- **D056 — Assignment snapshot:** Timetable assignments snapshot the lesson's teacher, cohort, and subject IDs in addition to lesson, period, and room IDs, allowing external changes to be classified without the old definition.
- **D057 — Addition and cancellation:** A lesson ID absent from the old timetable is an addition. An old lesson ID omitted from the updated full definition is a cancellation.
- **D058 — Partial locks:** Period and room can be locked independently; any unlocked dimension remains solver-controlled.
- **D059 — Period-move cost:** Any period change costs one period move, regardless of calendar distance.
- **D060 — Room disruption:** A room change counts toward room stability only when the period is preserved. A period move is not additionally penalized for requiring another room.
- **D082 — Initial versus updated definitions:** Initial planning rejects `basedOnRevision`. Replanning requires it and verifies it against the current timetable's input revision.
- **D088 — Change-report presence:** Initial planning omits the change report. Replanning reports additions, cancellations, external changes, period moves, and room-only moves.
- **D089 — Current timetable status:** Replanning accepts only a verified `FEASIBLE` timetable with a valid timetable revision, never a failure-result document.
- **D105 — Change-report scope:** Without the old definition, the kernel reports only observable lesson lifecycle and assignment changes: additions, cancellations, teacher changes, forced moves, period moves, and room-only moves. It does not claim to report every definition difference.
- **D113 — Minimal-change guarantee:** Stability dominates preferences, and the solver minimizes disruption within the search performed. A time-limited heuristic search does not promise a globally minimal set of changes.
- **D115 — Stability verification:** Small, fully understood fixtures assert exact move counts and prove that an ordinary preference never displaces a more stable feasible solution.

## Identity, revisions, and canonical data

- **D006 — Stable identity:** Teachers, rooms, cohorts, lessons, subjects, and periods have explicit opaque user-supplied IDs that are preserved exactly in outputs and change reports.
- **D029 — Revision generation:** The kernel calculates content-based definition and timetable revision IDs. A separate optional caller correlation ID is not identity.
- **D035 — Reproducibility metadata:** Results record schema and catalog versions, input revision, kernel version, seed, effective time limit, termination reason, elapsed time, and result revision.
- **D039 — Strict JSON:** Unknown properties, malformed references, and other schema violations are rejected rather than ignored.
- **D040 — Typed namespaces:** IDs must be unique within their entity type, not globally. Typed references allow different entity types to share the same lexical ID.
- **D044 — Deterministic serialization:** Entities and assignments have stable ordering and use canonical JSON. Timing metadata is non-deterministic and excluded from timetable revision calculation.
- **D063 — Version compatibility:** The kernel accepts only explicitly supported schema and catalog versions. It never silently accepts or migrates a newer version.
- **D083 — Revision algorithm:** Revision-bearing JSON is canonicalized and hashed with SHA-256. Revision IDs use `sha256:<lowercase-hex>`.
- **D084 — Canonicalization:** Canonical JSON follows RFC 8785 JSON Canonicalization Scheme so hashes are interoperable across implementations.
- **D085 — Timetable revision scope:** `timetableRevision` hashes only schema version, school ID, input revision, and assignments. Timings, diagnostics, scores, change reports, and correlation metadata are excluded.
- **D103 — Schema versions:** JSON schemas use integer versions. Every incompatible contract change increments the schema version; supported versions are explicit and no silent migration occurs.
- **D104 — Definition revision field:** Input definitions do not carry a claimed `revision` field. The kernel computes `inputRevision`; updated definitions carry only `basedOnRevision`, avoiding self-referential hashing.
- **D114 — Upgrade compatibility:** Kernel and Timefold upgrades need not reproduce identical assignments. They must preserve contract semantics, timetable validity, and the accepted priority ordering.

## Solver execution

- **D012 — Bounded solving:** Solving has a configurable limit and returns the best feasible result found. Such a result is labeled feasible, not necessarily optimal, and includes the termination reason.
- **D021 — Reproducibility intent:** Solving uses a fixed seed and deterministic configuration by default. This is later narrowed by D075 for wall-clock termination.
- **D022 — Default solve time:** The default solve limit is 30 seconds, may be overridden, and is reported with the termination reason.
- **D042 — Availability defaults:** If availability is omitted, the entity is available in every declared period. An explicit list replaces that default; an empty list means unavailable everywhere.
- **D047 — Configuration boundary:** Constraint enablement and weight overrides are revision-bearing school-definition data. Execution controls such as time limit and seed are outside the definition revision.
- **D075 — Wall-clock reproducibility:** The production wall-clock bound takes priority over byte-identical repeatability. A fixed seed improves reproducibility but cannot guarantee identical output when different machines perform different numbers of solver steps. Deterministic fixtures use step-limited mode.
- **D076 — Solver concurrency:** Default solving uses one thread. Parallel move solving may be introduced only as an explicit mode with weaker reproducibility guarantees.
- **D117 — Termination reasons:** Stable termination reasons are `TIME_LIMIT`, `STEP_LIMIT`, `SEARCH_EXHAUSTED`, and `EMPTY_PROBLEM`. Termination alone does not imply proven optimality.
- **D118 — Limit selection:** A run uses either a time limit or a step limit, never both. Production defaults to a 30-second time limit; deterministic fixtures specify a step limit.
- **D119 — Seed:** The default solver seed is `0`. Callers may supply any signed 64-bit seed, and every solver result records the effective seed.
- **D121 — Scale and safeguards:** The target scale is a tested performance objective, not a schema validity maximum. File-size and resource-protection limits are separate configurable safeguards.

## Result contract and output handling

- **D024 — CLI data channels:** Commands take explicit input and output paths. Machine-readable results go only to the output file, diagnostics go to stderr, and an existing output is not overwritten unless `--force` is supplied.
- **D034 — Outcome signaling:** Feasible, invalid-input, unsuccessful-search, and internal-failure outcomes have distinct exit codes. Every handled product outcome attempts to emit a structured result.
- **D064 — Durable writes:** Results are serialized to a temporary sibling file and published with an atomic replacement only after serialization succeeds. Existing destinations remain intact on failure.
- **D081 — Transport failures:** CLI-usage and filesystem failures are transport failures. They do not promise a structured result file; they leave the destination untouched, report on stderr, and return their own exit code.
- **D087 — Result envelope:** Every handled outcome shares an envelope containing status and metadata. Only `FEASIBLE` includes a timetable; `INVALID_INPUT` includes validation errors; an unsuccessful search includes diagnostics.
- **D090 — Internal-error disclosure:** Structured internal errors contain only a safe message and correlation ID. Technical exception details appear on stderr only when explicit debug output is enabled.
- **D091 — Exit codes:** Exit codes are `0` for feasible, `2` for invalid input, `3` for no feasible solution found, `4` for internal failure, `64` for CLI misuse, and `74` for filesystem I/O failure.
- **D092 — Interruption:** Ctrl-C exits with code `130`, publishes no new result file, and preserves any existing destination.
- **D120 — Correlation ID:** A caller may supply a correlation ID; otherwise the kernel generates one. It is outside content revisions and is included in diagnostics and logs.

## External contract and CLI

- **D014 — Data format:** v1 supports canonical, versioned JSON only. YAML may later be added as an input convenience without changing domain semantics.
- **D080 — CLI commands:** The stable command shapes are:

  ```text
  school-kernel plan --definition school.json --output timetable.json
  school-kernel replan --definition updated-school.json --current timetable.json --output revised.json
  ```

  Execution options include `--time-limit`, `--seed`, and `--force`.

- **D093 — Machine-readable schemas:** Versioned JSON Schema documents define school definitions and results. Valid plan and replan examples are checked by automated tests.
- **D095 — Public compatibility surface:** Versioned JSON and CLI behavior are the v1 public contract. Java object models and APIs are internal and may evolve.

## Architecture, packaging, and verification

- **D074 — Performance verification:** Correctness fixtures run deterministically in CI. A separate reproducible benchmark evaluates the target-scale workload; a 30-second wall-clock assertion is not a shared-CI gate.
- **D094 — Java baseline:** Java 25 is the documented minimum build and runtime version.
- **D096 — Model separation:** External JSON DTOs, validated domain objects, and internal Timefold planning types are separate. Timefold annotations and score types remain inside the solver adapter.
- **D097 — Stateless runtime:** v1 retains no school or timetable state between commands. Every invocation receives all required files and produces a complete result.
- **D098 — Distribution:** v1 produces one runnable dependency-inclusive JAR and a `school-kernel` launcher. Native images and platform installers are deferred.
- **D099 — Reproducible build:** Maven Wrapper is committed; dependency and plugin versions are pinned; the documented build works from a clean checkout.
- **D100 — Supporting libraries:** Picocli implements the CLI and Jackson implements strict JSON handling, using explicitly pinned compatible versions.
- **D101 — Test layers:** The test suite includes schema and validation tests, isolated constraint tests, solver integration tests, and black-box CLI plan/replan tests using real files.
- **D102 — Executable contract fixtures:** Normative fixtures cover initial planning, hard-conflict avoidance, validation failure, unsuccessful search, minimal-change replanning, locks, additions, and cancellations.
- **D122 — Documentation:** The executable is accompanied by documentation for both commands, schemas, exit codes, revision rules, scoring priorities, constraint catalog, failure semantics, and copy-pasteable plan/replan examples verified by tests.
