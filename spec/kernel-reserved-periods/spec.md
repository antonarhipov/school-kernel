# Feature: Reserved school periods

## Summary and resolved decisions

A school administrator can mark any declared periods as reserved in the school definition. Ordinary planning and repair exclude them from assignments. Reservation is a school-wide policy distinct from teacher, cohort, and room availability; those resource settings are not rewritten. IDs, names, order values, and clock times do not imply reservation. An omitted or empty reservation list means no periods are reserved.

MVK reserves exactly `mon-0`, `tue-0`, `wed-0`, `thu-0`, and `fri-0`. Those slots remain declared and visible but empty in a feasible ordinary timetable. If the remaining slots cannot accommodate all lessons, the system returns its normal no-feasible-solution outcome, never a timetable using a reserved slot. A period lock to a reserved slot is invalid input. The reserved list contains only existing, distinct period IDs.

For definitions using this policy, gap and weekday-availability quality calculations treat reserved periods as outside the regular school day. The cohort late-start measure, when present in the definition's catalog, locates the first three **non-reserved** weekday slots in declared order. Existing definitions without reservation settings retain their current scheduling, scoring, result, and revision meanings. The content-derived definition revision includes any explicit reservation list; its order is immaterial, while omission and an explicit empty list may have different revisions as different input documents.

## Actors and terms

- School administrator: supplies a definition, requests planning or repair, and reviews the outcome.
- Declared period: a slot present in the school definition, available for timetable display and reference validation.
- Reserved period: a declared period excluded school-wide from ordinary lesson assignments, regardless of resource availability.
- Regular period: a declared period not reserved by the school.
- Accepted predecessor: the previously verified definition and complete timetable used as the comparison baseline for repair.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a timetable that respects reserved periods | School administrator | none |

## UC-1 - Obtain a timetable that respects reserved periods (primary)

- Goal: Receive an initial or repaired timetable that never uses a school-reserved period.
- Primary actor: School administrator
- Supporting actors: scheduling kernel
- Trigger: The administrator requests initial planning or repair with a definition containing a reservation list.
- Preconditions: A school definition is supplied; repair additionally has a verifiable accepted predecessor and a successor definition.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator supplies a definition naming reserved periods and requests planning.
2. The system validates the reserved IDs and searches for a complete timetable using only regular periods for lesson assignments.
3. The system publishes a feasible candidate whose assignment periods exclude every reserved ID, while retaining the declared periods and their labels for timetable display.
4. The administrator can inspect the candidate and verify that reserved slots are empty and regular-period quality is reported consistently.

### Extensions

- 1a. If the administrator requests repair, the system verifies the accepted predecessor under its original definition, applies the successor's reservation policy, and searches for a feasible successor while retaining the predecessor unchanged; continue at step 3.
- 1b. If an accepted assignment uses a period newly reserved in the successor definition, the system may move it to a regular period during repair; continue at step 3.
- 2a. If the reservation list is omitted or empty, the system uses every declared period under the existing resource and lesson constraints; continue at step 3.
- 2b. If a reserved ID is unknown or duplicated, or a lesson is locked to a reserved period, the system reports invalid input before search and publishes no candidate; end.
- 2c. If no complete timetable is found without reserved periods within the accepted search limit, the system reports no feasible solution and publishes no candidate; end.
- 2d. If repair's accepted predecessor is invalid, the system reports invalid input without using the successor to reinterpret or alter the predecessor; end.

### Guarantees

- G1. Every feasible initial or repaired assignment uses a regular period. No resource availability declaration, soft weight, baseline assignment, or search limit can permit a reserved assignment. Reservation is not inferred from a period ID suffix or numeric order.
- G2. Reservation does not delete or rename a declared period or mutate teacher, cohort, or room availability. Existing timetable display can still show a reserved slot as empty.
- G3. A reserved period contributes no teacher or cohort internal-gap match. It contributes no available teaching weekday by itself. When the late-start preference applies, its third-slot threshold counts regular weekday periods only, in declared order, with unchanged behavior when none are reserved.
- G4. Definitions and accepted definition/result pairs that omit reservations remain valid with unchanged revisions, assignments, preference rows, and score semantics. Explicit reservation list order does not change the definition revision.
- G5. Repair preserves the verified accepted predecessor as an immutable baseline. A successor may reserve a previously assigned period; only a complete independently feasible successor can be published as a candidate. Existing hard-feasibility and period/room stability priorities remain in force.

### Postconditions

- Success: A complete feasible, revision-verifiable candidate exists with no assignment in a reserved period; acceptance remains a separate action.
- Minimal guarantee: Invalid, interrupted, or unsuccessful planning publishes no candidate and leaves any accepted predecessor unchanged.

## Normative MVK reservation

The MVK definition reserves exactly these five period IDs—no more, no fewer:

| Period ID | Weekday |
|---|---|
| `mon-0` | Monday |
| `tue-0` | Tuesday |
| `wed-0` | Wednesday |
| `thu-0` | Thursday |
| `fri-0` | Friday |

Other schools may reserve any subset of their declared periods, including periods without a `-0` suffix.

## Out of scope

No cohort or lesson may be excepted from reservation. Configurable age/cohort latest-start targets, new UI controls, mandatory lessons on every available day, and automatic candidate acceptance are deferred.
