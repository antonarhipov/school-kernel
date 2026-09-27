# Proposal: Reusable room assignments

The school needs to declare room assignments for a class of lessons rather than repeat a `roomLock` on each lesson.
An assignment may match a subject alone or a subject and teacher, require one room or a defined set of rooms, or use
the cohort's declared home room. Planning, repair, and verification must honor mandatory assignments as hard
constraints. A preferred room with permitted alternatives remains a preference within the permitted set.

Requested MVK examples:

- Ajalugu always in A231.
- Matemaatika taught by Piret Noor always in B212.
- Kirjandus taught by Heiki Raudla always in A209.
- Keemia always in A233.
- Füüsika always in A223.
- Kehaline kasvatus in KK1, KK2, or KK3.
- Muusikaõpetus prefers MU but may use other permitted music rooms.
- Õpiabi in the cohort's home room.

The current public EduPage snapshot and MVK definition use A223(LAB) for Keemia and have no A233 room. They also
place some non-Aiaste Ajalugu lessons outside A231, and Heiki Raudla's Kirjandus lessons outside A209. These may be
deliberate corrections to the published timetable; the behavioral specification must settle them explicitly.

The existing catalog 8 `roomLock` remains useful for individual lessons. This proposal concerns reusable school
rules and their interaction with existing room locks, room capabilities, home rooms, and soft preferences.
