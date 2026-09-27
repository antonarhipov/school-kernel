# Feature: Cohort start bounds and day-edge subjects

## Summary and resolved decisions

A catalog version 6 school definition may declare, per cohort, a hard `latestStartSlot` and a preferred
`preferredLatestStartSlot`, and, per subject, placement rules that permit reserved periods and confine lessons to
the edges of a cohort's day.

A **start slot** is a 1-based position among the weekday's regular (non-reserved) periods in declared order. A
cohort-day starts on time for bound `n` when fewer than `n` regular periods precede its first lesson. A first lesson in
a reserved period before the first regular period therefore starts on time for every bound. In MVK, slot `n` is the
period labelled `n`: `2` means `mon-2`, and the reserved `mon-0` precedes slot 1.

`latestStartSlot` has no default. Omitting it imposes no hard bound. `preferredLatestStartSlot` defaults to `3`, the
catalog 3 threshold, so omitting it preserves the existing `soft.cohort-late-start` meaning. A preferred slot later
than the hard slot is valid; it has no additional effect.

A subject's placement rules are all optional:

| Field | Meaning | Omitted |
|---|---|---|
| `reservedPeriodsAllowed` | The subject's lessons may use any school-reserved period. | `false` |
| `maxWeeklyReservedLessonsPerCohort` | At most this many of one cohort's lessons of the subject are in reserved periods per week. Requires `reservedPeriodsAllowed`. | unlimited |
| `dayEdgeOnly` | Each lesson of the subject is the first or the last lesson of its cohort's day. A lesson alone on its day is both. | `false` |
| `maxDailyLessonsPerCohort` | At most this many of one cohort's lessons of the subject fall on one weekday. | unlimited |

A reserved period occupied by a permitted lesson is part of that cohort's day for gap counting. A permitted lesson in
`mon-0` followed by a first ordinary lesson in `mon-2` leaves a gap at `mon-1`. A reserved period without a lesson
remains outside the day, as in the reserved-periods feature. Teacher gaps follow the same rule. Weekly balance and
start slots count regular periods only.

Every new rule is hard. `hard.reserved-period` joins the physical hard level. `hard.cohort-late-start`,
`hard.subject-day-edge`, `hard.subject-daily-limit`, and `hard.subject-reserved-limit` join `hard.cohort-gap` on the
second hard level. Catalogs 1 through 5 reject every new field and keep their exact meanings.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a timetable in which each cohort starts by its bound | School administrator | none |
| UC-2 | Obtain a timetable that places edge-only subjects at the ends of the day | School administrator | none |

## UC-1 - Obtain a timetable in which each cohort starts by its bound (primary)

- Goal: Receive a timetable whose taught cohort-days all start by each cohort's hard bound, preferring its soft bound.
- Primary actor: School administrator
- Supporting actors: scheduling kernel
- Trigger: The administrator requests initial planning or repair with a catalog 6 definition.
- Preconditions: A definition exists; repair additionally has a verifiable accepted predecessor.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator supplies a catalog 6 definition in which some cohorts declare start bounds and requests planning.
2. The system validates the definition and searches for a complete timetable in which every taught cohort-day starts
   by that cohort's `latestStartSlot`.
3. The system publishes a feasible candidate whose `soft.cohort-late-start` row counts the cohort-days starting after
   each cohort's preferred slot.

### Extensions

- 1a. If the administrator requests repair, the system applies the same bounds to the successor; continue at 3.
- 1b. If a catalog 1 through 5 definition declares either field, or a value is below 1 or fractional, the system
  reports invalid input without searching; end.
- 2a. If no timetable within the bounds is found, the system reports `NO_FEASIBLE_SOLUTION_FOUND` with
  `hard.cohort-late-start` diagnostics naming the cohort and weekday, publishes no timetable, and preserves any
  accepted predecessor; end.
- 2b. If a cohort has no lesson on a weekday, that day has no start and cannot violate either bound; continue at 3.

### Guarantees

- G1. The solver, published evaluation, and baseline verifier count one hard match per taught cohort-day whose
  first lesson has at least `latestStartSlot` regular periods before it.
- G2. For a cohort without `preferredLatestStartSlot`, the late-start preference counts exactly what catalog 5
  counted.
- G3. A catalog 5 accepted predecessor is repaired as a catalog 6 successor without changing the predecessor.

### Postconditions

- Success: A feasible candidate exists whose every taught cohort-day starts by its hard bound.
- Minimal guarantee: An unsuccessful search publishes no timetable and leaves any accepted predecessor unchanged.

## UC-2 - Obtain a timetable that places edge-only subjects at the ends of the day

- Goal: Receive a timetable where subjects with placement rules sit only where those rules allow.
- Primary actor: School administrator
- Supporting actors: scheduling kernel
- Trigger: The administrator requests initial planning or repair with a catalog 6 definition.
- Preconditions: A definition exists; repair additionally has a verifiable accepted predecessor.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator supplies a catalog 6 definition in which a subject declares placement rules, and requests
   planning.
2. The system validates the definition and searches for a complete timetable in which only permitted subjects use
   reserved periods and every placement limit holds.
3. The system publishes a feasible candidate. A permitted lesson may appear in a reserved period, and the result shows
   it there.

### Extensions

- 1a. If the administrator requests repair, the system applies the same rules to the successor; continue at 3.
- 1b. If a catalog 1 through 5 definition declares a placement field, a limit is below 1 or fractional, or
  `maxWeeklyReservedLessonsPerCohort` appears without `reservedPeriodsAllowed: true`, the system reports invalid
  input without searching; end.
- 1c. If a lesson of a permitted subject is period-locked to a reserved period, the lock is valid. A lock to a
  reserved period for any other subject remains invalid; end.
- 2a. If no timetable satisfying the rules is found, the system reports `NO_FEASIBLE_SOLUTION_FOUND` with diagnostics
  for each violated rule, publishes no timetable, and preserves any accepted predecessor; end.

### Guarantees

- G1. No lesson of a subject without `reservedPeriodsAllowed` is assigned to a reserved period, in planning, repair,
  or baseline verification.
- G2. The solver, published evaluation, and baseline verifier count identical matches: one per edge-only lesson with
  lessons of its cohort both before and after it that day; `max(0, count - limit)` per cohort-day and subject for
  the daily limit; and `max(0, count - limit)` per cohort and subject for the weekly reserved limit.
- G3. A definition in which no subject permits reserved periods searches the same period range as catalog 5.

### Postconditions

- Success: A feasible candidate exists in which every placement rule holds.
- Minimal guarantee: An unsuccessful search publishes no timetable and leaves any accepted predecessor unchanged.

## Normative MVK configuration

`examples/mvk.json` uses catalog 6. Cohorts `1a`, `1b`, `1c`, `2a`, `2b`, `2c`, `3a`, and `3b` declare
`latestStartSlot: 2`, and no cohort declares `preferredLatestStartSlot`. Subject `opiabi` declares
`reservedPeriodsAllowed: true`, `maxWeeklyReservedLessonsPerCohort: 1`, `dayEdgeOnly: true`, and
`maxDailyLessonsPerCohort: 1`. Every other field matches catalog 5.

## Out of scope

Earliest-start bounds, per-weekday bounds, permission for specific reserved periods only, school-wide placement
limits, and workspace screens for editing these fields.
