# Bundled group examples

Both Variant A and Variant B contain these actual teaching sessions. Select the
school and class in the timetable; use School setup → Subjects → a subject to inspect
teaching arrangements by class, group names/sizes, weekly demand, fixed teachers/rooms
and coordinated starts before conversion. After explicit conversion, open a class's
teaching arrangement there to edit delivery or create/reuse its groups. Classes retains
the reusable-group overview and membership relationships. An older retained
preview offers **Open current school copy**, preserving the previous draft.

Groups use **Group 1**, **Group 2**, etc. Timetable tiles show **G1**, **G2**,
etc.; inspectors and school definitions retain the full names. Split names
identify the arrangement, such as English groups, simulator wings or engineering
crews.

Split tiles show the subject above the actual room name, with the group badge
beside the subject. Added rooms have independent names such as Keelteklass 1,
Systems lab 1 and Mission lab 1; their names do not repeat the class or group.

| School / class | Example |
| --- | --- |
| Kasevälja / 6C, 7A, 7B, 8A, 8B, 9A, 9B, 9C | Inglise keel: two groups of 12, three weekly sessions per group, taught in parallel by distinct teachers in distinct rooms. |
| Coruscant / Intermediate Cadet 4A | Flight Simulation: wings of 13/12. Starship Systems: four engineering crews of 7/6/6/6 nested inside those wings. Three weekly lessons per student in each subject. |
| Coruscant / Senior Officer Cadet 7A | Starfighter Operations and Fleet Tactics use different two-way splits of 13/13. The membership intersection matrix is 7/6/6/7: a squadron crosses both command teams, so the two arrangements cannot be treated as interchangeable. Four weekly lessons per student in each subject. |
| Coruscant / Senior Officer Cadet 8A | Mission Planning: one whole-class briefing plus three lessons per cell, using three cells of 10/9/9. Four weekly lessons per student. |

Grouped portions have explicit fixed weekly teachers. Every grouped round has a
`START_TOGETHER` rule; parallel sessions need independent teachers and compatible
rooms. Added small laboratories have capacities matching their intended groups.
Whole-class curriculum demand remains unchanged: 520 class teaching periods at
Kasevälja become 544 actual sessions; 540 at Coruscant become 566 actual sessions.

Coruscant has **10 slots per weekday**, 50 in total, from 08:30 to 19:05. Slots
8–10 are spare scheduling capacity. Junior/intermediate/senior student timetables
retain their five/six/seven teaching periods per day. A lunch boundary after slot
4 is distinct from consecutive lesson adjacency.

## Maintenance

`add_sample_groups.py --source <original-schema-1-pair-directory>` upgrades the
two original schools and writes their complete A/B assignment seeds. It requires
the original definitions and results and refuses already-upgraded inputs.
Keep a copy of the originals before running it. `build_imperial_academy.py`
constructs that original whole-class academy, so running it alone removes the
academy group examples.

After any definition or seed edit, use the current packaged kernel:

```sh
java --class-path kernel-cli/target/school-kernel.jar tools/demo/BuildTimetableVariants.java timetable-workspace/src/main/resources/demo/timetables full
java --class-path kernel-cli/target/school-kernel.jar tools/demo/BuildTimetableVariants.java timetable-workspace/src/main/resources/demo/timetables star-wars
python3 tools/demo/record_timetable_evidence.py
```

For schema 2 the builder independently reviews the complete A/B seeds, including
student attendance, weekly teacher allocations and coordinated starts. The
evidence recorder checks all eight bundled pairs and rejects stale scores or
invalid assignments. Ordinary application startup does not regenerate samples.
