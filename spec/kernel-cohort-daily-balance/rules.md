# Technical Rules: Cohort daily lesson balance

## Design overview

Catalog version 4 extends the existing versioned school definition with an optional cohort-level spread target. The
validated value flows into each planning lesson, where the existing `soft.cohort-week-balance` constraint and the
independent result evaluator use the same pairwise count formula. The score remains an ordinary preference. The
workspace carries cohort settings into repair successors while preserving accepted predecessor documents. The public
JSON schema version, CLI commands, result shape, persistence model, and routes stay unchanged.

## Codebase alignment

The kernel retains Java 25, Timefold 2.6.0, hand-authored offline Draft 2020-12 JSON schemas, RFC 8785 revisions,
and a four-level bendable score. The accepted design requires a new catalog version for changed constraint semantics;
catalogs 1 through 3 remain exact. The workspace continues to verify an accepted pair before compiling a complete
successor and to invoke the packaged kernel rather than reimplementing the score.

## Security surface

| Surface | Access |
|---|---|
| Local CLI `plan`, `replan`, `verify` | Explicit local paths; no account or network access |
| GET `/`, `/workspace/**`, `/api/csrf`, `/api/workspace`, `/api/runs/*`, `/api/proposal`, `/api/accepted/export` | Existing loopback, Host/Origin, and Spring Security filters; permitted |
| POST `/api/import`, `/api/initial-draft/replace`, `/api/repair-draft`, `/api/repair-draft/bulk-pin-preview`, `/api/runs`, `/api/proposal/accept`, `/api/workspace/clear`, `/api/workspace/upload-definition` | Existing loopback, Host/Origin, CSRF, and state guards; permitted |
| PATCH `/api/repair-draft`; DELETE `/api/repair-draft`, `/api/runs/*`, `/api/proposal`, `/api/workspace` | Existing loopback, Host/Origin, CSRF, and state guards; permitted |
| Login/logout, other routes or methods | No login/logout routes; all other requests denied |

## Verification strategy

Schema and validator tests cover absent, zero, positive, negative, fractional, and legacy-catalog settings. Exact
constraint and evaluator assertions compare different targets on different cohorts, zero-lesson available days, and
the zero-weight case. Packaged CLI tests plan and verify a catalog 4 definition, replan from a legacy predecessor,
and assert invalid/unsuccessful outcomes contain no timetable. Workspace repair integration compares predecessor
bytes or JSON values with its version 4 successor. Check MVK's exact normative cohort targets and override rows by
value, then plan it with the normal 30-second bound and independently inspect daily counts, first lesson periods,
cohort and teacher gaps, and preferred-room matches. Run the complete kernel module suite, the affected workspace
import/repair integration suite, and `git diff --check`; inspect any broader reactor failures for relation to this
feature.

## Rules

### RULE-1 - Versioned cohort setting

- Applies to: UC-1
- Constraint: Catalog version 4 MUST accept a non-negative integer `maxDailyLessonSpread` only on cohort entries, default its omission to one, and reject the field under catalogs 1 through 3. The field MUST participate in definition revisions. Catalogs 1 through 3 MUST keep their prior semantics and result rows, while catalog 4 MUST keep exactly the seven version 3 preference IDs and defaults.
- Reason: A saved definition and score must retain the meaning they had when accepted.
- Verification: Schema, validation, revision, catalog, old-result verification, and version-specific result assertions.

### RULE-2 - Exact scoring at both boundaries

- Applies to: UC-1
- Constraint: The Timefold constraint and independent evaluator MUST use each cohort's validated target and the exact UC-1 G1 pairwise formula over days with non-reserved available periods, including zero-lesson days. They MUST report identical match counts, and weighted penalty bounds MUST stay within signed 64-bit range.
- Reason: The search objective and published score must describe the same timetable without overflow.
- Verification: ConstraintVerifier, incremental-score, evaluator, and boundary tests compare exact counts for targets zero, one, and two and for two differently configured cohorts.

### RULE-3 - Repair lineage and priority

- Applies to: UC-1
- Constraint: Repair MUST preserve hard, period-move, room-only-move, then ordinary-preference priority. Workspace compilation MUST retain cohort targets already present in a catalog 4 accepted definition, upgrade older accepted catalogs to a version 4 successor, and leave the accepted definition/result pair unchanged.
- Reason: Quality tuning must not silently revise accepted authority or justify disruption.
- Verification: Packaged replan and workspace HTTP integration compare predecessor bytes/revisions, successor field values, score, and move counts.

### RULE-4 - Actor-boundary evidence

- Applies to: UC-1
- Constraint: Packaged CLI planning and verification MUST demonstrate a feasible catalog 4 candidate and the absence of a timetable for rejected input or unsuccessful search. The MVK fixture MUST match the exact normative configuration in UC-1 and be exercised at its normal limit with independent cohort-count, first-start, gap, and preferred-room inspection. The complete kernel and affected workspace import/repair regression suites MUST pass before convergence.
- Reason: The administrator needs a real proposal whose reported quality matches its assignments.
- Verification: Isolated packaged CLI tests, a normal-limit MVK run, workspace integration, `./mvnw -q -pl kernel-cli -am verify`, affected workspace tests, and `git diff --check`.

### RULE-5 - MVK cohort-gap priority

- Applies to: UC-1
- Constraint: MVK MUST set `soft.cohort-gap` to the catalog's maximum supported weight of 1,000,000, `soft.cohort-late-start` to 10,000, and `soft.cohort-week-balance` to 1,000, while retaining the other normative weights. Each internal available empty period between a cohort's first and last lesson in a continuous daily availability block MUST contribute one match and 1,000,000 to aggregate cohort-gap penalty; empty periods outside that block MUST NOT count. The weight MUST remain an ordinary preference rather than a hard constraint or a new score row.
- Reason: Avoidable holes in a cohort's daily schedule are more disruptive than small differences in other ordinary preferences, but must not suppress a hard-feasible proposal.
- Verification: Exact MVK configuration assertion, gap scoring tests for internal and leading/trailing empty periods, packaged plan/verify, and independent assignment tally against the reported gap match count and penalty.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | RULE-1, RULE-2, RULE-3, RULE-4, RULE-5 |

## Design exclusions

No new route, UI screen, authentication mechanism, database migration, dependency, hard constraint, score row,
first-start-spread setting, or acceptance transition is introduced.
