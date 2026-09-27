# Technical Rules: Reusable hard room assignments

## Design overview

Catalog 9 adds `roomAssignments` to the versioned school definition. Each assignment selects a subject and optionally
a teacher, then either lists allowed room IDs or selects each matched cohort's home room. The contract loader rejects
malformed declarations; semantic validation resolves matching lessons and direct contradictions. The planner carries
the effective permitted-room set on each lesson, and the solver, independent evaluator, diagnostics, baseline verifier,
and repair change report use that same resolved meaning. The timetable workspace preserves the complete definition
through import and repair. No new library, service, endpoint, database column, or workspace editor is introduced.

## Codebase alignment

The existing JSON Schema/Jackson boundary, `DefinitionValidator`, Timefold constraint provider, independent
`ScheduleEvaluator`, packaged CLI, and immutable workspace successor flow remain the owners of their respective
checks. This feature adds a catalog version rather than changing catalog 8 semantics. An assignment is school-level
data, unlike the existing individual-lesson `roomLock`. Room preferences remain the existing soft constraint.

## Security surface

No route, role, login behavior, or public access changes. The existing authenticated workspace import and repair
routes continue to use their current guards; the CLI remains a local actor boundary. The definition is inert data:
policy strings are IDs validated against declared entities, not commands or paths.

## Verification strategy

Use isolated fixtures and temporary CLI files, never a tracked runtime result. At the packaged CLI boundary, test
plan followed by verify, repair from a real verified predecessor, legacy rejection, malformed references and
contradictions, infeasibility, and tampered-result rejection. Assert both response and absence of a published feasible
result or predecessor mutation on failure. At the workspace HTTP boundary, import and generate repair with a catalog
9 definition, then inspect the complete successor by value. Unit tests cross-check solver/evaluator/diagnostic counts,
room-preference softness, policy matching, and the forced-versus-ordinary move distinction. Run focused tests, the
full relevant reactor, `git diff --check`, and a packaged MVK plan/verify journey.

## Rules

### RULE-1 - Versioned and unambiguous policy structure

- Applies to: UC-1
- Constraint: Catalog 9 MUST accept a top-level `roomAssignments` array whose items have a unique `id`, required
  `subjectId`, optional `teacherId`, and exactly one target: non-empty unique `allowedRoomIds` or
  `useHomeRoom: true`. Catalogs 1 through 8 MUST reject the field. Every item and its array order MUST participate
  in the canonical definition revision. Unknown fields MUST be rejected.
- Reason: The actor can express singleton rooms, sets, and cohort-dependent rooms without duplicating lesson locks;
  old accepted definitions must remain interpretable.
- Verification: Schema tests with both target variants and malformed/legacy inputs; revision change assertions.

### RULE-2 - Resolve and validate all matching rules before search

- Applies to: UC-1
- Constraint: Semantic validation MUST check policy IDs and entity references, resolve every matched lesson using
  subject and optional teacher, intersect all matching targets, and reject a missing matched home room, empty
  intersection, incompatible `roomLock` or curator home room, or an effective set with no room satisfying cohort
  capacity and required capabilities. Errors MUST identify policy IDs and affected lesson IDs where applicable.
  A valid policy matching no current lesson MUST be retained.
- Reason: Directly contradictory school instructions are invalid configuration, not a search failure or an implicit
  priority between policy types.
- Verification: Semantic tests for selector scope, overlap, each contradiction, future-only policies, and unchanged
  existing room locks.

### RULE-3 - One physical hard constraint meaning

- Applies to: UC-1
- Constraint: The resolved policy set MUST be enforced as `hard.room-assignment` at physical hard level 0. The
  Timefold constraint, independent evaluator, preflight, infeasibility diagnostics, and accepted-baseline verifier
  MUST agree on violation meaning and count. Candidate room filtering or baseline seeding MUST NOT permit a room
  outside the set. Existing hard room requirements MUST remain independent.
- Reason: A soft preference or solver-only filter could produce a timetable that verification incorrectly accepts.
- Verification: Constraint and evaluator comparison; packaged plan, infeasibility, and tampered-baseline tests.

### RULE-4 - Repair and stability semantics

- Applies to: UC-1
- Constraint: A successor MUST retain its accepted predecessor unchanged and carry the complete policy data. If a
  new policy excludes an accepted room, repair MUST classify the necessary room change as forced; if both accepted
  and proposed rooms remain permitted, it MUST retain ordinary room-move accounting. Failed repair MUST publish no
  successor. Workspace compilation MUST preserve catalog 9 rather than downgrading to 8.
- Reason: Policy adoption is an intentional hard change; ordinary optimization should not be misreported as forced.
- Verification: Real packaged replan and workspace repair journeys comparing predecessor, successor definition,
  change report, and failure state by value.

### RULE-5 - MVK values and source distinction

- Applies to: UC-1
- Constraint: `examples/mvk.json` MUST contain exactly the seven policy rows and A233 room specified in the
  normative MVK section of `spec.md`, with no additional room policies or synthetic rooms. Affected explicit
  preferences MUST be aligned with new hard requirements. Music MUST remain a MU preference with a `music-room`
  capability, not a MU lock. The fixture MUST validate, produce a complete feasible result at a 60-second planning
  limit, and independently verify that result.
- Reason: User-specified corrections must not be confused with patterns merely observed in EduPage.
- Verification: By-value fixture assertions, packaged MVK plan and verify, and assignment checks for every row.

### RULE-6 - Boundary and regression proof

- Applies to: UC-1
- Constraint: Tests MUST exercise UC-1's main scenario and every extension at its actual CLI or workspace HTTP
  boundary, including response and prohibited side effects. They MUST use isolated input/output and run without
  changing tracked runtime files. The full relevant reactor MUST pass without new skips, and catalog 1 through 8
  plan/verify behavior MUST remain green.
- Reason: A new hard contract affects published results, accepted authority, and old catalog compatibility.
- Verification: Focused packaged CLI and HTTP tests, legacy regressions, full reactor counts, and clean worktree
  comparison before and after testing.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-5, RULE-6 |

## Design exclusions

No new policy editing UI, automatic EduPage inference, extra room-preference scoring rule, or schema version 2.
