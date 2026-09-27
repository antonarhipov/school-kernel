# Feature: Cohort curators and home rooms

## Summary and resolved decisions

A catalog version 8 school definition may declare, on a cohort, `curatorTeacherId` (a declared teacher) and
`homeRoomId` (a declared room), and, on a subject, `curatorLesson: true`. All three are optional. Catalogs 1 through
7 reject them and keep their exact meanings.

- Every lesson of a curator-lesson subject MUST belong to a cohort that declares a curator, and its teacher MUST be
  that curator. The curator is qualified for their own cohort's curator lessons without listing the subject in
  `qualifiedSubjectIds`. Any other teacher still needs the qualification.
- When the cohort declares a home room, each of its curator lessons MUST be held there (`hard.cohort-home-room`, hard
  level 0 with room locks). A room lock naming another room, or a home room too small or lacking a required
  capability, is invalid input.
- A lesson with no `preferredRoomIds` of its own, whose cohort declares a home room with enough capacity and every
  capability the lesson requires, is treated as preferring the home room for `soft.non-preferred-room`. An explicit
  preference replaces the default. Lessons the home room cannot host, such as sport in a hall, have no default.
- A curator creates no lessons, and a cohort may declare a curator without any curator lessons.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
|---|---|---|---|
| UC-1 | Obtain a timetable in which every Klassitund is taught by the curator in the cohort's home room | School administrator | none |

## UC-1 - Obtain a timetable in which every Klassitund is taught by the curator in the cohort's home room (primary)

- Goal: Receive a timetable whose curator lessons are taught by each cohort's curator and held in its home room.
- Primary actor: School administrator
- Supporting actors: scheduling kernel, timetable workspace
- Trigger: The administrator requests initial planning or repair with a catalog 8 definition.
- Preconditions: A definition exists; repair additionally has a verifiable accepted predecessor.

### Main success scenario

1. The administrator supplies a catalog 8 definition declaring curators, home rooms, and a curator-lesson subject,
   and requests planning.
2. The system validates that every curator lesson is taught by its cohort's curator and that each home room can host
   its curator lessons.
3. The system searches for a complete timetable that holds every curator lesson in its cohort's home room.
4. The system publishes a feasible candidate whose `soft.non-preferred-room` row also counts lessons held outside a
   defaulted home room.

### Extensions

- 1a. If the administrator requests repair, the system applies the same rules to the successor; continue at 4.
- 1b. If a catalog 1 through 7 definition declares any of the fields, the system reports invalid input without
  searching; end.
- 2a. If a curator lesson's cohort has no curator, its teacher is not the curator, a reference is unknown, or a home
  room contradicts a room lock, capacity, or capability, the system reports invalid input without searching; end.
- 3a. If no timetable is found, the system reports `NO_FEASIBLE_SOLUTION_FOUND` with `hard.cohort-home-room`
  diagnostics naming the lesson, its room, and the home room (or, before search, the lesson and home room), publishes
  no timetable, and preserves any accepted predecessor; end.

### Guarantees

- G1. The solver, published evaluation, and baseline verifier count identical `hard.cohort-home-room` matches.
- G2. A cohort without a home room, and a lesson with its own `preferredRoomIds`, keep exactly their catalog 7
  meaning.
- G3. A catalog 7 accepted predecessor is repaired as a catalog 8 successor without changing the predecessor.

### Postconditions

- Success: A feasible candidate exists in which every curator lesson is taught by its curator in its home room.
- Minimal guarantee: An unsuccessful search publishes no timetable and leaves any accepted predecessor unchanged.

## Normative MVK configuration

`examples/mvk.json` uses catalog 8, and subject `klassitund` declares `curatorLesson: true`. Curators follow the
EduPage `classTeacherIds`; 5D, which EduPage leaves empty, takes `liisi-rannik`, who teaches all 21 of its Klassitund
lessons. EduPage also leaves 6C empty, so its synthetic curator is `olga-simonovits`, randomly selected from
teachers who teach 6C in MVK; she teaches its two Russian lessons and curates no other cohort. Home rooms follow
the EduPage `homeRoomIds` for 1A through 4B, 5D's Klassitund room `a211`, and the room of the curator-taught IntÕ
lesson for 5A, 5B, 6A, 6B, 7A, 7B, 8A, 8B, 9A, 9B, and 9C. Their respective rooms are `a215`, `a216`, `b213`,
`b214`, `b216`, `a229`, `b214`, `a230`, `a204`, `b212`, and `b215`. 6C has no curator-taught IntÕ lesson and its
curator's two Russian lessons use different rooms, so it has no home room pending a choice. Each of those eleven
curator-taught IntÕ lessons has an explicit `roomLock` for its home room. The 14 Ajalugu lessons taught by Siiri
Aiaste each have `roomLock: "a231"`, matching their consistent published room. Other lessons retain their catalog
7 room preferences unless a room assignment is established as mandatory.

## Out of scope

Creating Klassitund lessons from a curator, several curators per cohort, workspace screens for curators and home
rooms, and a general hard home-room rule for lessons other than curator lessons. MVK's explicit `roomLock` entries
are individual school-data assignments under the existing room-lock contract.
