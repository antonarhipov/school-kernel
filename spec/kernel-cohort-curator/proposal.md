# Proposal: Cohort curators and home rooms

Every cohort has a curator, and most cohorts have a room of their own. Younger cohorts, grades 1 through 4, spend
almost the whole day there with their curator and leave only for music, sport, or crafts. Klassitund is conducted by
the curator in that room, even when the curator's own subject is something else.

The kernel knows none of this. MVK expresses it by habit: each Klassitund lesson names the curator as its teacher and
the home room as a soft `preferredRoomIds` entry weighted `5`. Nothing ties the teacher to the cohort, a repair may
move Klassitund out of its room for a single point of preference, and a teacher who curates a cohort without teaching
it a subject must be marked qualified for Klassitund by hand.

Catalog 8 names both roles. A cohort may declare `curatorTeacherId` and `homeRoomId`. A subject may declare
`curatorLesson`, which makes its lessons belong to the cohort's curator and, where the cohort has a home room, hold
them there as a hard rule. A cohort's other lessons prefer the home room when they state no preference of their own
and the room can host them.

A curator does not create lessons. Whether a cohort has a Klassitund in its timetable is the school's decision; the
kernel only enforces who teaches it and where.

The EduPage snapshot declares class teachers for 21 of MVK's 23 cohorts. It leaves 5D and 6C empty; 5D's curator
and home room are inferred from its 21 Klassitund lessons, all taught by Liisi Rannik in A211. The 6C curator is
synthetic: Olga Simonovitš was randomly selected from its existing subject teachers. EduPage class records declare
home rooms only for grades 1 through 4. The curator-taught IntÕ lesson provides a room for eleven more cohorts,
and 5D's Klassitund supplies its room. For 6C, Olga teaches in A210 and A204 while its IntÕ is taught by another
teacher in A209, so its home room needs a choice.

The published timetable also assigns some teacher-subject lessons consistently to one room. Siiri Aiaste's 14
Ajalugu lessons all use A231. MVK locks that concrete assignment and the eleven curator-taught IntÕ lessons to
their published rooms; a uniform room preference elsewhere is not, by itself, evidence of a mandatory assignment.
