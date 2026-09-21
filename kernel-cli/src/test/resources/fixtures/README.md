# Merivälja example fixtures

`merivalja-5a-5b.json` is a planning fixture derived from the public 5A and 5B timetables at
<https://meripohi.edupage.org/timetable/>. The timetable was inspected with Playwright on
2026-09-20; the page identified the published timetable as valid from 2026-09-21 through
2027-06-11.

The fixture preserves the observed bell times, whole-class subject occurrence counts,
teacher assignments, named rooms, and shared teachers/rooms across 5A and 5B. Published
rooms are represented as preferences, so this remains an unscheduled `plan` input rather
than a copy of the published output timetable.

The following values are synthetic because the public timetable does not expose them:

- both cohort sizes are 24;
- all room capacities are 30;
- ordinary rooms have `general-classroom`; `MU` has `music-room`; and `STUUDIO` has
  `art-studio`;
- omitted availability means every declared period, as defined by the v1 contract.

The fixture intentionally excludes lessons that School Kernel v1 cannot represent without
changing their meaning:

- split English and physical-education groups;
- the two-period, team-taught handicraft/technology block.

The source displays lesson numbers 0 through 9. Only numbers 0 through 6 are declared here
because those are the periods used by the retained 5A and 5B lessons. Contract `order` is
one-based, so displayed lesson 0 has order 1.

## Selected primary and secondary classes

`merivalja-1a-1b-4a-4avr-4b-9a-9b.json` applies the same conversion to the published 1A,
1B, 4A, 4AVR, 4B, 9A, and 9B timetables. It contains 176 retained lessons across seven
cohorts and declares displayed lesson numbers 0 through 8.

The requested `4BVR` timetable is not present in the public class selector inspected on
2026-09-20, so no `4bvr` cohort was fabricated. The source currently lists 4A, 4AVR, and
4B before continuing with 5A.

As in the 5A/5B fixture, split rows, simultaneous subgroup rows, and multi-period or
team-taught blocks are excluded. This affects English, foreign-language, swimming,
physical-education, support, and handicraft/technology entries depending on the class.
The retained and excluded source-cell counts are:

| Class | Retained | Excluded |
|---|---:|---:|
| 1A | 22 | 0 |
| 1B | 22 | 0 |
| 4A | 25 | 10 |
| 4AVR | 26 | 6 |
| 4B | 23 | 12 |
| 9A | 28 | 11 |
| 9B | 30 | 11 |

`4AVR` is represented as an independent cohort so the data conforms to the v1 contract.
The published timetable appears to use it as a subgroup view that shares some lessons with
4A, which v1 cannot model faithfully. Consequently this fixture is representative solver
test data, not a lossless reconstruction of the school's enrollment structure.

All seven cohort sizes are synthetic values of 24 and every room capacity is a synthetic
value of 30. Room capabilities are inferred from published room labels: `MU` is a music
room, `KK*` rooms are sports halls, and `A223(LAB)` is a science lab. Other named rooms use
`general-classroom`. The retained 4AVR swimming row uses its published `KK3` sports-room
assignment.
