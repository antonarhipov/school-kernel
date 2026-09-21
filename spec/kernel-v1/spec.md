# School Kernel v1 Use-Case Specification

Status: Ready for technical rules

Specification date: 2026-09-20

Inputs: [`proposal.md`](proposal.md) and [`decisions.md`](decisions.md)

## Feature summary

School Kernel is a stateless, headless scheduling runtime for a recurring school week. A scheduling client supplies a complete, versioned school definition and asks the kernel either to produce an initial timetable or to replan a verified current timetable after the definition changes.

Every successful timetable is complete and satisfies all hard constraints. The kernel may stop before proving optimality, but it never publishes a hard-violating or partial timetable as feasible. Replanning gives strict priority to preserving existing period assignments, then existing rooms, before improving ordinary preferences.

The public v1 behavior is a command-line and canonical JSON contract. This specification defines actor-visible behavior and deliberately leaves source structure and framework mechanics to the technical-rules stage.

## Scope and resolved decisions

### In scope

- Initial planning of a recurring week made of fixed, non-overlapping periods from Monday through Sunday.
- One-period lessons, each with exactly one preassigned teacher and one indivisible cohort; the kernel assigns one period and one room.
- Hard enforcement of resource non-collision, availability, room eligibility, and explicit locks.
- A fixed, versioned preference catalog with bounded configurable weights.
- Replanning from a verified feasible timetable and a complete updated definition, with no separate change-command document.
- Stability-first optimization and an observable change report.
- Strict, versioned JSON; content-derived revisions; structured terminal outcomes; stable exit codes; and atomic output publication.
- Deterministic fixtures and a separate target-scale performance benchmark.

### Resolved behavioral boundaries

- A cohort is indivisible. Split groups, merged cohorts, and team teaching are not inferred.
- Teachers are preassigned. Changing a teacher is a definition change, not a solver choice.
- Every active lesson is independently identifiable even when related lessons share a series.
- Availability is hard and binary. Undesirable periods are soft preferences.
- Hard constraints cannot be disabled. Soft weights are integers from `0` through `1,000,000`; zero disables that preference.
- A failed search reports that no feasible solution was found; it does not claim the problem is infeasible or expose a diagnostic candidate as a timetable.
- A production time limit bounds execution, so a fixed seed improves reproducibility but does not guarantee byte-identical assignments across machines or engine versions.
- Supported scale is a tested performance target, not a schema maximum.

## Actors and domain terms

### Actors

- **Scheduling client:** A person or automation invoking the command-line contract, supplying files and execution controls, and consuming the result and exit code.
- **Filesystem:** The external storage boundary from which definitions and current timetables are read and to which a result may be atomically published.

There are no user accounts, roles, authentication rules, screens, or interactive navigation in v1.

### Domain terms

- **School definition:** A complete versioned snapshot of the school resources, lesson requirements, locks, and soft-constraint overrides used for one planning request.
- **Subject:** The taught topic for which a teacher may be qualified.
- **Teacher:** The preassigned instructor for a lesson, with subject qualifications and period availability.
- **Cohort:** One indivisible student group with a size and period availability.
- **Room:** A location with capacity, capabilities, and period availability.
- **Period:** One fixed, non-overlapping slot in the recurring week.
- **Lesson:** One single-period teaching occurrence with a stable ID, subject, cohort, preassigned teacher, room requirements, and optional preferences or locks.
- **Lesson series:** An optional grouping of lesson occurrences having the same subject and cohort.
- **Lock:** An authoritative requirement fixing a lesson's period, room, or both.
- **Timetable:** A complete set of period and room assignments for all active lessons.
- **Current timetable:** A verified feasible timetable supplied as the baseline for replanning.
- **Definition revision:** The content-derived identity of a canonical school definition.
- **Timetable revision:** The content-derived identity of the assignment state of a feasible timetable.
- **Catalog version:** The version governing stable constraint IDs, meanings, and default soft weights.
- **Diagnostic candidate:** Internal search evidence used only to explain why no feasible timetable was found; it is never a timetable result.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Generate an initial timetable | Scheduling client | Primary; none |
| UC-2 | Replan a current timetable | Scheduling client | Requires UC-1 because a verified feasible timetable must already exist |

The relation graph is acyclic. Exactly one use case, UC-1, is primary.

## State models

### Terminal result status

Each handled invocation produces exactly one terminal status from the [Result statuses](#result-statuses) table. A result never transitions to another status after publication. Because the runtime is stateless, a later invocation creates a new result rather than mutating an earlier one.

Only `FEASIBLE` contains a timetable. Every other status contains no timetable and therefore cannot be used as the current timetable for UC-2.

### Definition and timetable lineage

```text
Initial definition (no basedOnRevision)
        │ UC-1 FEASIBLE
        ▼
Current timetable
  inputRevision = initial definition revision
        │
        ├── Updated definition
        │     same schoolId
        │     basedOnRevision = current timetable inputRevision
        │
        └──────────── UC-2 FEASIBLE ────────────► Revised timetable
                                                    inputRevision = updated definition revision
```

Allowed lineage transitions are exactly:

1. An initial definition without `basedOnRevision` may be used by UC-1.
2. An updated definition with the same `schoolId` and a `basedOnRevision` equal to the current timetable's `inputRevision` may be used by UC-2.
3. A revised feasible timetable may become the current timetable for a later updated definition that directly names its `inputRevision` as `basedOnRevision`.

All other transitions are refused as `INVALID_INPUT` without invoking the solver or publishing a timetable. Branching is allowed only by deliberately creating another updated definition from the chosen direct parent definition revision.

### Invocation publication lifecycle

Before publication, an invocation has no new externally visible result. It ends in exactly one of these ways:

- a handled product outcome is completely serialized and atomically published;
- CLI misuse, an I/O or resource-safeguard failure, or interruption prevents publication and preserves any existing destination; or
- publication itself fails, in which case the temporary artifact is not treated as a result and any existing destination remains unchanged.

No partially written result is observable as the requested destination.

## UC-1 - Generate an initial timetable (primary)

- Goal: Obtain a complete, hard-valid timetable for an initial school definition, together with revisions, scoring information, and reproducibility metadata.
- Primary actor: Scheduling client
- Supporting actors: Filesystem
- Trigger: The client invokes `school-kernel plan` with a definition path and an output path.
- Preconditions: The client can identify the input and intended destination; no prior timetable is required.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The client requests initial planning with a versioned JSON definition, distinct input and output paths, and optional execution controls.
2. The kernel atomically publishes a `FEASIBLE` result whose timetable assigns every active lesson one eligible period and one eligible room and whose metadata identifies the accepted input and execution conditions.
3. The kernel exits with code `0`; the output file is the only machine-readable result, and any human diagnostic output is confined to stderr.

### Extensions

- 1a. If the command, option values, paths, or simultaneous use of time and step limits is invalid, the kernel reports CLI misuse on stderr, exits with code `64`, publishes no result, and leaves every existing destination unchanged; end.
- 1b. If an input path equals the output path, the kernel refuses the invocation as CLI misuse, exits with code `64`, publishes no result, and leaves the input unchanged; end.
- 1c. If an input cannot be read, the destination cannot be prepared, the destination exists without `--force`, or a configured pre-parse resource safeguard is exceeded, the kernel reports the transport failure on stderr, exits with code `74`, publishes no result, and leaves every existing destination unchanged; end.
- 2a. If the input is malformed JSON, violates its supported schema, uses an unsupported schema or catalog version, contains unknown properties, or fails semantic validation, the kernel does not invoke the solver, atomically publishes `INVALID_INPUT` with the validation information described in [Validation reporting](#validation-reporting), exits with code `2`, and ends.
- 2b. If the initial definition contains `basedOnRevision`, the kernel treats it as invalid input, publishes `INVALID_INPUT`, exits with code `2`, and ends.
- 2c. If a well-formed lesson has no eligible period or room, the kernel atomically publishes `NO_FEASIBLE_SOLUTION_FOUND` with a deterministic diagnostic, exits with code `3`, and ends without invoking a search.
- 2d. If the definition contains no lessons, the kernel atomically publishes a `FEASIBLE` empty timetable with termination reason `EMPTY_PROBLEM`, exits with code `0`, and ends.
- 2e. If the search limit is reached or the search is exhausted after at least one feasible timetable was found, the kernel publishes the best feasible result found under the accepted score ordering, records the actual termination reason, exits with code `0`, and ends without claiming optimality.
- 2f. If the search ends without finding a feasible timetable, the kernel publishes `NO_FEASIBLE_SOLUTION_FOUND` with the diagnostics described in [Search-failure diagnostics](#search-failure-diagnostics), exits with code `3`, and ends.
- 2g. If an unexpected internal failure occurs and a safe result can still be published, the kernel publishes `INTERNAL_ERROR` containing only a safe message and correlation ID, exits with code `4`, and ends. Exception details appear on stderr only when debug output was explicitly requested.
- 2h. If the client interrupts the invocation before publication, the kernel exits with code `130`, publishes no new result, and preserves every existing destination; end.
- 2i. If serialization or atomic publication fails, the kernel reports an I/O failure on stderr, exits with code `74`, does not expose a partial result, and preserves every existing destination; end.

### Guarantees

- G1. Definitions follow the complete logical contract in [School-definition data](#school-definition-data). Entity IDs and references are case-sensitive, IDs are unique within their type, and display names do not establish identity.
- G2. A `FEASIBLE` timetable satisfies exactly the hard constraints in [Hard-constraint catalog](#hard-constraint-catalog), assigns every active lesson exactly once, and contains no unresolved hard conflict.
- G3. The kernel applies exactly the soft constraints in [Soft-constraint catalog](#soft-constraint-catalog), using catalog defaults plus accepted definition overrides and the score ordering in [Product score](#product-score).
- G4. Every handled outcome follows [Result envelope](#result-envelope), [Result statuses](#result-statuses), [Termination reasons](#termination-reasons), and [Exit codes](#exit-codes).
- G5. The kernel never emits a partial or hard-violating timetable as a result, never emits a diagnostic candidate as a timetable, and never labels a merely unsuccessful search `INFEASIBLE` or `OPTIMAL`.
- G6. The definition revision and any timetable revision follow [Revision and canonicalization rules](#revision-and-canonicalization-rules). User-supplied IDs are preserved exactly in assignments and diagnostics.
- G7. Production solving uses one solver thread, seed `0` unless overridden, and a 30-second time limit unless a positive time or step limit is explicitly selected. The effective seed and limit are reported.
- G8. A fixed seed does not promise identical assignments under a wall-clock limit or after an engine upgrade. Contract semantics, hard validity, and priority ordering remain stable.
- G9. When `--force` is absent, an existing destination is never replaced. When it is present, replacement occurs only through complete atomic publication. `--force` never permits an input file to be used as the destination.
- G10. The invocation retains no school or timetable state after it ends; a subsequent invocation must supply all required data again.
- G11. The target-scale fixture described in [Verification boundary](#verification-boundary) is a performance objective, not an input-validity cap or a promise that every instance becomes feasible within 30 seconds.

### Postconditions

- Success: A complete canonical `FEASIBLE` result exists at the requested destination, its timetable can be consumed by clients, and it is eligible to serve as the current timetable for UC-2.
- Minimal guarantee: No non-feasible path publishes a timetable, and no transport, interruption, serialization, or publication failure changes an existing destination.

## UC-2 - Replan a current timetable

- Goal: Obtain a new hard-valid timetable for a complete updated school definition while minimizing avoidable changes to the verified current timetable.
- Primary actor: Scheduling client
- Supporting actors: Filesystem
- Trigger: The client invokes `school-kernel replan` with an updated definition path, a current timetable path, and an output path.
- Preconditions: A verified `FEASIBLE` timetable satisfying the UC-1 success postcondition exists; the updated definition is intended as a direct successor to the definition used for that timetable.
- Relations:
  - Requires: UC-1 because a verified feasible current timetable must exist
  - Includes: none
  - Extends: none

### Main success scenario

1. The client requests replanning with a complete updated definition, a verified current timetable, distinct input and output paths, and optional execution controls.
2. The kernel atomically publishes a `FEASIBLE` result for the updated definition, preserving current period and room assignments according to the accepted lexicographic priorities and reporting every observable lesson lifecycle or assignment change.
3. The kernel exits with code `0`; the revised timetable is eligible to become the baseline for another direct replan.

### Extensions

- 1a. If the command, option values, paths, or simultaneous use of time and step limits is invalid, the kernel reports CLI misuse on stderr, exits with code `64`, publishes no result, and leaves every existing file unchanged; end.
- 1b. If the output path equals either input path, the kernel refuses the invocation as CLI misuse, exits with code `64`, publishes no result, and leaves both inputs unchanged; end.
- 1c. If an input cannot be read, the destination cannot be prepared, the destination exists without `--force`, or a configured pre-parse resource safeguard is exceeded, the kernel reports the transport failure on stderr, exits with code `74`, publishes no result, and leaves every existing destination unchanged; end.
- 2a. If either document is malformed, violates a supported schema, contains unknown properties, or fails semantic validation, the kernel does not invoke the solver, atomically publishes `INVALID_INPUT`, exits with code `2`, and ends.
- 2b. If the current document is not a `FEASIBLE` timetable, its timetable revision cannot be verified, its `schoolId` differs from the updated definition, the updated definition omits `basedOnRevision`, or `basedOnRevision` does not equal the current timetable's `inputRevision`, the kernel publishes `INVALID_INPUT`, exits with code `2`, and ends without solving.
- 2c. If a lock contradicts availability, room eligibility, or another locked lesson, the kernel reports all directly detectable lock conflicts as invalid input, publishes no timetable, exits with code `2`, and ends.
- 2d. If the current timetable is no longer valid under the updated definition because a teacher, cohort, or room became unavailable or a lesson was cancelled or changed, the kernel accepts that condition as the reason for replanning and resumes at main step 2.
- 2e. If the updated definition contains no active lessons, the kernel publishes a `FEASIBLE` empty timetable and a change report listing the removed lessons as cancellations, records `EMPTY_PROBLEM`, exits with code `0`, and ends.
- 2f. If a well-formed active lesson has no eligible period or room, the kernel publishes `NO_FEASIBLE_SOLUTION_FOUND` with deterministic diagnostics, exits with code `3`, and ends without publishing a timetable.
- 2g. If the search limit is reached or the search is exhausted after at least one feasible timetable was found, the kernel publishes the best feasible result found under the accepted stability-first ordering, records the actual termination reason, exits with code `0`, and ends without claiming globally minimal change or optimality.
- 2h. If the search ends without finding a feasible timetable, the kernel publishes `NO_FEASIBLE_SOLUTION_FOUND` with diagnostics, exits with code `3`, and ends without publishing a timetable or change report.
- 2i. If an unexpected internal failure occurs and a safe result can still be published, the kernel publishes `INTERNAL_ERROR` containing only a safe message and correlation ID, exits with code `4`, and ends. Exception details appear on stderr only when debug output was explicitly requested.
- 2j. If the client interrupts the invocation before publication, the kernel exits with code `130`, publishes no new result, and preserves every existing destination; end.
- 2k. If serialization or atomic publication fails, the kernel reports an I/O failure on stderr, exits with code `74`, does not expose a partial result, and preserves every existing destination; end.

### Guarantees

- G1. UC-1 G1 through G11 apply to the updated definition, search, result, publication, and metadata except where this use case explicitly adds lineage and change-report behavior.
- G2. The current timetable's integrity and provenance are verified against its content-derived revision before solving. Manual assignment edits invalidate that revision; intended manual choices are expressed only through explicit locks in the updated definition.
- G3. The current timetable need not satisfy the updated definition. Its former assignments remain the comparison baseline, but only the revised timetable must satisfy the updated hard constraints.
- G4. Replanning compares stability only for lesson IDs present in both timetables. New IDs are additions; old IDs omitted from the updated complete definition are cancellations; neither contributes to move penalties.
- G5. Scoring is lexicographic: hard feasibility, then the number of period moves, then the number of room-only moves, then ordinary preference penalty. One period move has the same cost regardless of calendar distance.
- G6. A room change counts as a room-only move only when the period is unchanged. A period move is not additionally penalized merely because its room also changes.
- G7. A dimension explicitly forced to a new value by a lock is excluded from solver-disruption penalties and reported as a forced move. A teacher reassignment is reported as an external definition change; any solver-chosen period movement remains subject to stability unless that period is explicitly locked.
- G8. Period and room locks are independent. An unlocked dimension remains solver-controlled, and appearance in the current timetable never creates an implicit lock.
- G9. A successful replan contains exactly the categories and field fidelity defined in [Change report](#change-report). It does not claim to describe definition differences that cannot be observed from the current timetable snapshot and updated definition.
- G10. The revised timetable snapshots the lesson, teacher, cohort, subject, period, and room IDs for every assignment so a later direct replan can classify observable changes without receiving the older definition.
- G11. Stability is minimized within the search performed. A bounded heuristic search does not promise the globally smallest possible change set.

### Postconditions

- Success: A complete canonical `FEASIBLE` revised timetable and change report exist at the requested destination, and the revised timetable can serve as the current timetable for a later direct successor definition.
- Minimal guarantee: Refused, interrupted, unsuccessful, or failed replanning never mutates the supplied current timetable, never publishes a replacement timetable, and never changes an existing destination unless a complete result is atomically published with overwrite permission.

## Normative data

### School-definition data

The logical definition contains the following data. Versioned JSON Schema fixes the serialized syntax; these meanings and conditional requirements are normative.

| Structure | Required logical fields and rules |
|---|---|
| Definition | `schemaVersion`, `catalogVersion`, `schoolId`, nonblank school `displayName`, subjects, teachers, cohorts, rooms, periods, lessons, and optional soft-constraint overrides. `basedOnRevision` is forbidden for UC-1 and required for UC-2. A claimed current revision is forbidden. |
| Subject | Stable `id` and nonblank `displayName`. |
| Teacher | Stable `id`, nonblank `displayName`, qualified subject IDs, optional availability period IDs, and optional undesirable period IDs. |
| Cohort | Stable `id`, nonblank `displayName`, positive integer size, optional availability period IDs, and optional undesirable period IDs. |
| Room | Stable `id`, nonblank `displayName`, non-negative integer capacity, capability IDs, and optional availability period IDs. |
| Period | Stable `id`, nonblank `displayName`, ISO weekday, and positive order unique within that weekday. Optional display start and end times appear together, end after start, and do not overlap another period that day. |
| Lesson | Stable `id`, nonblank `displayName`, subject ID, cohort ID, preassigned teacher ID, optional series ID, required room capability IDs, preferred room IDs, undesirable period IDs, optional period lock, and optional room lock. Duration is exactly one period. |
| Soft override | One soft-constraint ID from [Soft-constraint catalog](#soft-constraint-catalog) and an integer weight in `0..1,000,000`. |

The following rules apply to the entire definition:

- v1 accepts `schemaVersion` value `1` and `catalogVersion` value `1`; every other value is unsupported until explicitly added by a later release.
- Entity IDs are case-sensitive and match `[A-Za-z0-9][A-Za-z0-9._-]{0,127}`; series and capability IDs use the same lexical form.
- IDs are unique within their type. Cross-type lexical duplicates are allowed because every reference is typed.
- Every reference resolves to an entity of the required type.
- Omitted availability means every declared period; an explicit list replaces that default; an empty list means unavailable in every period.
- Every lesson's teacher is qualified for its subject.
- Every occurrence sharing a series ID has the same subject and cohort; its teacher may differ.
- A room is eligible only when its capacity covers the cohort and its capabilities contain every lesson requirement.
- Hard constraints cannot be disabled or reweighted.
- Soft overrides replace catalog defaults only for the named constraints. The fully resolved effective configuration is reported.
- The maximum possible weighted aggregate is validated to fit signed 64-bit arithmetic.
- Unknown properties, unsupported versions, duplicate IDs, unresolved references, malformed values, and deterministic semantic contradictions are invalid input.

### Hard-constraint catalog

The v1 hard catalog contains exactly these rows—no more, no fewer:

| Stable ID | A feasible timetable guarantees |
|---|---|
| `hard.teacher-period` | One teacher is assigned to at most one lesson in a period. |
| `hard.cohort-period` | One cohort is assigned to at most one lesson in a period. |
| `hard.room-period` | One room is assigned to at most one lesson in a period. |
| `hard.teacher-availability` | Every lesson uses a period available to its teacher. |
| `hard.cohort-availability` | Every lesson uses a period available to its cohort. |
| `hard.room-availability` | Every lesson uses a period available to its room. |
| `hard.room-capacity` | Every assigned room has capacity at least equal to the cohort size. |
| `hard.room-capability` | Every assigned room provides every capability required by the lesson. |
| `hard.period-lock` | A period-locked lesson uses exactly its locked period. |
| `hard.room-lock` | A room-locked lesson uses exactly its locked room. |

Reference integrity, teacher qualification, series consistency, and directly detectable lock contradictions are validation rules rather than configurable solver constraints.

### Soft-constraint catalog

The v1 soft catalog contains exactly these rows—no more, no fewer. Every row is enabled by default with weight `1`; weight `0` disables it.

| Stable ID | Match semantics before weighting |
|---|---|
| `soft.teacher-gap` | One match for each available, unassigned period between a teacher's first and last lesson inside one continuous availability block on one day. An unavailable period breaks the block and is not a gap. |
| `soft.series-same-day` | For `n` occurrences of the same series on one day, `max(0, n - 1)` matches. |
| `soft.undesirable-period` | One match for each applicable teacher, cohort, or lesson declaration that marks the assigned period undesirable; applicable declarations are additive. |
| `soft.non-preferred-room` | One match when a lesson with a nonempty unordered preferred-room set is assigned another eligible room. |

The catalog provides only penalties; it does not award bonuses.

### Product score

A feasible result exposes the lexicographically ordered non-negative vector:

1. `periodMoves` — common baseline lessons whose period changed and was not authoritatively forced;
2. `roomOnlyMoves` — common baseline lessons whose period stayed the same but room changed and was not authoritatively forced;
3. `ordinaryPreferencePenalty` — the signed-64-bit-safe sum of each soft match count multiplied by its effective weight.

For initial planning, both stability components are zero. Lower is better at the first component where two feasible results differ. A constraint breakdown follows soft-catalog order and contains, for every soft catalog row, its stable ID, category, effective weight, match count, and aggregate penalty. A raw engine score is not part of the stable contract.

### Result statuses

The stable status set contains exactly these rows—no more, no fewer:

| Status | Timetable present | Meaning |
|---|---:|---|
| `FEASIBLE` | Yes | A complete timetable satisfying every hard constraint was found; optimality is not implied. |
| `NO_FEASIBLE_SOLUTION_FOUND` | No | No feasible timetable was found within the accepted evaluation or search; infeasibility is not proved. |
| `INVALID_INPUT` | No | Parsing, schema, version, reference, lineage, revision, or semantic validation failed before solving. |
| `INTERNAL_ERROR` | No | An unexpected internal failure was handled safely. |

`INFEASIBLE` and `OPTIMAL` are not v1 statuses.

### Termination reasons

When a solver or empty-problem outcome terminates normally, the stable reason set contains exactly these rows—no more, no fewer:

| Reason | Meaning |
|---|---|
| `TIME_LIMIT` | The accepted wall-clock limit ended the search. |
| `STEP_LIMIT` | The accepted deterministic step limit ended the search. |
| `SEARCH_EXHAUSTED` | The configured search space was exhausted; this does not by itself authorize an `OPTIMAL` status. |
| `EMPTY_PROBLEM` | No active lessons required assignment, so no search ran. |

Validation and internal failures have no fabricated termination reason.

### Result envelope

Every structured result contains `schemaVersion`, `status`, `kernelVersion`, `correlationId`, and elapsed time. It records the following values whenever they are derivable without inventing data:

- accepted `catalogVersion` and fully resolved effective soft weights;
- `inputRevision` after a definition can be canonicalized;
- effective seed and exactly one effective limit;
- termination reason when a search or empty-problem path terminated normally;
- timetable and `timetableRevision` only for `FEASIBLE`;
- product score and constraint breakdown only for `FEASIBLE`;
- validation report only for `INVALID_INPUT`;
- search diagnostics only for `NO_FEASIBLE_SOLUTION_FOUND`;
- safe error message only for `INTERNAL_ERROR`;
- change report only for a feasible UC-2 result.

When malformed input prevents a revision, catalog version, or effective configuration from being derived, the result omits that field rather than fabricating a value. A caller correlation ID is a nonblank string of at most 128 Unicode characters; when absent, the kernel generates an opaque correlation ID. It is included in diagnostics and logs but excluded from content revisions.

### Assignment fidelity

Every feasible timetable contains one assignment for every active lesson and no others. Each assignment preserves these IDs exactly:

- lesson;
- subject;
- cohort;
- preassigned teacher;
- assigned period;
- assigned room.

Assignments are deterministically ordered by lesson ID. There are no unmappable assignment values: an unresolved or wrong-type reference makes the input invalid, and an unassigned lesson prevents a feasible result.

### Change report

A feasible UC-2 result contains exactly these categories—no more, no fewer:

| Category | Required fidelity |
|---|---|
| Additions | Lesson IDs present in the updated definition but absent from the current timetable. |
| Cancellations | Lesson IDs present in the current timetable but absent from the updated definition. |
| Teacher changes | Common lesson IDs whose preassigned teacher changed, with old and new teacher IDs. |
| Forced moves | Common lesson IDs whose period or room changed because that dimension was explicitly locked to the new value, with old and new IDs for each forced dimension. |
| Period moves | Common lesson IDs with a solver-chosen period change, with old and new period IDs and the resulting room IDs. |
| Room-only moves | Common lesson IDs with unchanged period and solver-chosen room change, with old and new room IDs. |

Additions and cancellations never count as moves. A teacher change may coexist with an assignment-change category. Forced assignment dimensions do not also appear as solver-induced moves. The report is deterministically ordered by category and lesson ID. It does not claim to enumerate changes to definition fields that are absent from the current timetable snapshot.

### Validation reporting

Validation runs before solving and returns errors in deterministic order. It reports no more than 1,000 detailed entries and always includes the total number detected and a truncation flag. Each detail has an input location when one can be identified, involved entity IDs when available, and a safe explanation. No validation response contains a timetable or mutates an input.

### Search-failure diagnostics

A failed search reports:

- each affected hard-constraint stable ID;
- the total match count for that constraint without truncation;
- involved entity IDs for deterministically ordered detailed examples;
- no more than 1,000 detailed examples overall, together with a truncation flag.

The diagnostics are aggregated from the best available diagnostic candidate and are labeled as search evidence, not a timetable, proof of infeasibility, or guarantee that correcting only those examples will make the definition feasible.

### Revision and canonicalization rules

- An input definition never contains its own claimed revision.
- Revision-bearing content is normalized into deterministic entity, reference-set, and assignment order, serialized according to RFC 8785 JSON Canonicalization Scheme, and hashed with SHA-256.
- A revision is encoded as `sha256:` followed by lowercase hexadecimal digest text.
- The definition revision covers the complete normalized definition, including `schemaVersion`, `catalogVersion`,
  `schoolId`, school `displayName`, `basedOnRevision` when present, resources, lessons, locks, and soft overrides.
- Caller correlation, seed, solve limit, filesystem paths, elapsed time, and diagnostic output are excluded from the definition revision.
- `timetableRevision` covers exactly `schemaVersion`, `schoolId`, `inputRevision`, and the canonical assignments. It excludes scores, diagnostics, change reports, correlation data, and timing metadata.
- Manual changes to assignment content therefore invalidate the timetable revision. Changes to excluded non-assignment metadata do not create a different timetable state.

### CLI commands and options

The stable command shapes are:

```text
school-kernel plan --definition school.json --output timetable.json
school-kernel replan --definition updated-school.json --current timetable.json --output revised.json
```

The public options include:

- `--time-limit` with a positive duration; default `30s` when neither limit is specified;
- `--step-limit` with a positive deterministic step count, mutually exclusive with `--time-limit`;
- `--seed` with any signed 64-bit integer; default `0`;
- `--correlation-id` with an optional caller trace value;
- `--force` to permit atomic replacement of an existing non-input destination;
- `--debug` to permit technical exception details on stderr after an internal failure.

Machine-readable product results are written only to the requested output file. stderr is reserved for human diagnostics and transport, usage, interruption, and optional debug information.

### Exit codes

The public exit-code set contains exactly these rows—no more, no fewer:

| Code | Meaning |
|---:|---|
| `0` | A `FEASIBLE` result was published. |
| `2` | An `INVALID_INPUT` result was published. |
| `3` | A `NO_FEASIBLE_SOLUTION_FOUND` result was published. |
| `4` | An `INTERNAL_ERROR` result was published. |
| `64` | CLI misuse; no result was promised. |
| `74` | Filesystem, resource-safeguard, serialization, or publication failure; no result was promised. |
| `130` | Client interruption; no result was published. |

### Verification boundary

The behavioral contract is verified at these boundaries:

- versioned JSON Schema accepts valid examples and rejects unknown properties, unsupported versions, and malformed structures;
- validation fixtures exercise reference integrity, teacher qualification, series consistency, weight bounds and overflow, locks, revisions, and lineage;
- each hard and soft catalog row is verified in isolation with fixtures that demonstrate both matching and non-matching cases;
- end-to-end command invocations use real input and output files and verify result contents, exit codes, stderr separation, overwrite refusal, interruption safety where controllable, and atomic destination preservation on failure;
- small deterministic step-limited fixtures verify exact stability ordering and move counts without generalizing those fixtures into an optimality promise;
- executable fixtures cover initial planning, hard-conflict avoidance, invalid input, unsuccessful search, minimal-change replanning, locks, additions, and cancellations;
- a separate reproducible benchmark covers up to 1,000 lessons, 100 teachers, 60 cohorts, 100 rooms, and 60 periods. Its measurements are reported, but a shared-CI 30-second wall-clock threshold is not a correctness gate;
- documentation contains copy-pasteable plan and replan commands and explains schemas, exit codes, revision rules, scoring priorities, constraint catalog, and failure semantics; its example files are exercised by automated tests.

Runtime-generated files or prior invocations are not prerequisites for deterministic correctness fixtures; every fixture provides its own complete inputs.

## Out of scope

- Dated calendars, time zones, variable-length lessons, and multi-period adjacency.
- Split or overlapping cohorts, merged-cohort lessons, team teaching, and multi-room lessons.
- Solver-selected teachers or a broader staff-allocation problem.
- User-defined constraints or a constraint DSL.
- Workload limits, lunch rules, travel time, consecutive-period rules, daily subject limits, and preferences not listed in the v1 soft catalog.
- A separate changes document, event store, retained runtime state, database, web API, graphical UI, authentication, authorization, user management, or localization.
- YAML as a public v1 format.
- Guaranteed proof of infeasibility, optimality, globally minimal replanning, byte-identical time-limited results, or identical assignments across engine versions.
- Native executables, platform installers, and a public Java compatibility contract.

## External dependencies

There are no unresolved stakeholder or product dependencies. All product decisions needed by the technical-rules and execution stages have an explicit default in this specification. Accepted implementation constraints that do not change actor-visible behavior remain recorded in [`decisions.md`](decisions.md) for the technical-rules stage.
