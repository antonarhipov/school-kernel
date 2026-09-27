# Feature: Reusable hard room assignments

## Summary and resolved decisions

A school administrator can declare a mandatory room assignment once for a subject, or for a subject taught by a
particular teacher. An assignment names one or more allowed rooms or the lesson cohort's declared home room. Every
matching lesson must use an allowed room in initial planning, repair, and independent verification. These policies
do not replace an individual lesson's `roomLock`; both must hold. Catalogs 1 through 8 retain their existing meaning
and reject the new declarations. The new declarations belong to catalog 9.

- A subject policy matches every lesson of that subject. A teacher-scoped policy matches only lessons of that subject
  taught by that teacher, including after teacher reassignment. Several matching policies all apply: the effective
  permitted rooms are their intersection. A contradictory empty intersection is invalid input, not an override.
- A home-room policy resolves independently for each matching lesson's cohort. A missing home room for a matched
  lesson is invalid input. A policy with no matching lessons is allowed so it can govern future lessons.
- A policy that conflicts with an individual `roomLock`, a curator lesson's home-room obligation, room capacity, or
  required capabilities is invalid input. Existing room availability, teacher/cohort availability, and room-period
  collision rules remain hard requirements. A valid policy can still produce no feasible timetable.
- Hard allowed rooms and soft room preferences remain distinct. `preferredRoomIds` continues to guide choice among
  permitted rooms. A room outside a mandatory set cannot be selected even if it is preferred. A policy does not
  silently add or remove a lesson preference.
- Repair preserves the accepted predecessor. A baseline room made impermissible by a new policy is a forced room
  move, while a move between permitted rooms remains an ordinary room move. A failed repair publishes no successor.
- This capability is configuration in the versioned school definition, not a new workspace editing screen. Workspace
  import and repair retain and enforce the declarations, including when they upgrade an older accepted catalog.

## Actors and domain terms

- School administrator: supplies or changes a school definition, plans, repairs, and reviews the result.
- Room assignment: a reusable mandatory subject or teacher-subject rule with a non-empty allowed-room set, or a
  cohort-home-room target.
- Allowed room: a room that survives every matching assignment and the existing lock, capacity, capability, and
  availability constraints.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a timetable that honors reusable room assignments | School administrator | none |

## UC-1 - Obtain a timetable that honors reusable room assignments (primary)

- Goal: Receive and independently verify a timetable in which every matching lesson uses a declared permissible room.
- Primary actor: School administrator
- Supporting actors: scheduling kernel, timetable workspace
- Trigger: The administrator requests planning, repair, or verification with a school definition.
- Preconditions: The school definition declares subjects, teachers, cohorts, rooms, and lessons; repair additionally
  has a verifiable accepted predecessor.
- Relations:
  - Requires: none
  - Includes: none
  - Extends: none

### Main success scenario

1. The administrator supplies a catalog 9 definition containing reusable room assignments and requests planning.
2. The system validates assignment references and each matched lesson's effective permitted rooms, reporting any
   direct contradiction before searching.
3. The system searches for a complete timetable under the mandatory room assignments and the existing hard rules.
4. The system publishes a feasible timetable whose assignments respect every matching policy, while room preferences
   score only as soft choices.
5. The administrator independently verifies the published timetable against the same definition, and the system
   reports `VERIFIED` only when every room assignment and other hard rule holds.

### Extensions

- 1a. If the administrator repairs an accepted timetable under a catalog 9 successor definition, the system applies
  the same policies to the successor, retains the accepted predecessor, and reports policy-forced room moves; continue
  at step 4.
- 1b. If the administrator imports the definition into the timetable workspace and later generates repair, the
  workspace preserves the policies in the complete successor definition; continue at step 2.
- 2a. If a catalog 1 through 8 definition contains a room assignment, a reference is unknown, a matched cohort has
  no required home room, the effective room intersection is empty, or a policy directly contradicts an existing
  room lock, curator home-room obligation, capacity, or required capability, the system reports `INVALID_INPUT`
  identifying the policy and affected lesson where applicable, searches neither plan nor repair, and changes no
  accepted timetable; end.
- 3a. If valid policies and other hard rules leave no feasible placement, the system reports
  `NO_FEASIBLE_SOLUTION_FOUND` with a room-assignment diagnostic when that rule is violated, publishes no timetable,
  and leaves any accepted predecessor unchanged; end.
- 5a. If independent verification finds a policy violation in a candidate or accepted baseline, the system reports
  `INVALID_INPUT` identifying the room-assignment violation and does not certify or alter the timetable; end.

### Guarantees

- G1. Planning, published scoring and diagnostics, and independent verification use the same policy match and
  allowed-room meaning. A policy violation is a hard infeasibility, never a weighted preference.
- G2. Lessons not matched by any policy and catalog 1 through 8 definitions retain their prior room behavior.
- G3. Individual `roomLock`, curator home-room, capacity, capability, and availability constraints remain effective
  alongside policies; matching policies cannot weaken any of them.
- G4. The school definition preserves policy IDs, selectors, targets, and catalog version through planning, repair,
  import, and verification. The published result preserves its catalog version, binding definition revision, lesson
  identities, and actual room IDs. Unknown or unmappable values are rejected, never guessed or silently dropped.
- G5. No failed plan, repair, or verification mutates an accepted predecessor or publishes a purportedly feasible
  timetable.

### Postconditions

- Success: A feasible, independently verifiable timetable exists; every matched lesson uses a room allowed by all
  matching policies, and the administrator can inspect any forced room moves during repair.
- Minimal guarantee: An invalid or infeasible request changes no accepted timetable and publishes no feasible result.

## Normative MVK configuration

`examples/mvk.json` adopts catalog 9 and contains exactly the following seven hard policy rows, no more and no fewer.
The table uses current MVK entity IDs; the subject/teacher display names are shown for review. A one-room set is an
absolute room requirement for every matched lesson. The permitted room set is not inferred from one EduPage snapshot.

| Policy ID | Subject | Teacher scope | Mandatory target | Current matched lessons |
|---|---|---|---|---:|
| `mvk-history-a231` | `ajalugu` (Ajalugu) | any | `a231` (A231) | 23 |
| `mvk-piret-maths-b212` | `matemaatika` (Matemaatika) | `piret-noor` | `b212` (B212) | 23 |
| `mvk-heiki-literature-a209` | `kirjandus` (Kirjandus) | `heiki-raudla` | `a209` (A209) | 10 |
| `mvk-chemistry-a233` | `keemia` (Keemia) | any | `a233` (A233) | 10 |
| `mvk-physics-a223` | `fuusika` (Füüsika) | any | `a223-lab` (A223(LAB)) | 10 |
| `mvk-physical-education-halls` | `kehaline-kasvatus` (Kehaline kasvatus) | any | `kk1`, `kk2`, `kk3` | 18 |
| `mvk-study-support-home` | `opiabi` (Õpiabi) | any | each lesson cohort's `homeRoomId` | 18 |

The explicitly requested A233 does not occur in the EduPage snapshot or current MVK definition. MVK therefore adds
one clearly synthetic room `a233`, display name `A233`, capacity 30, capability `science-lab`, with the default room
availability; no other synthetic room is added. `a223-lab` is the existing EduPage/MVK room corresponding to the
requested A223. A231 for all Ajalugu and A209 for Heiki Raudla's Kirjandus intentionally supersede differing
EduPage room preferences; the affected MVK preferences are aligned with these mandatory rooms. Piret Noor Maths,
Physics, and PE preferences remain soft within the mandatory sets.

All 29 `muusikaopetus` (Muusikaõpetus) lessons retain their explicit soft preference for `mu` (MU) and required
`music-room` capability. MU is preferred, not mandatory: the currently declared other music-capable room `k2` is
allowed when needed. Exactly the 18 `opiabi` lessons of grades 1 through 4 use their already declared cohort home
rooms. The missing 6C home room is not guessed and does not affect the current Õpiabi lessons.

## Out of scope

Creating or editing reusable policies in a new workspace screen; inferring mandates from observed EduPage repetition;
automatically choosing a missing home room; changing the accepted predecessor; and changing catalog 1 through 8
behavior.
