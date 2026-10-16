# Timetable-first demo

The user confirmed this design on 2026-10-16 after twelve product decisions and explicitly authorized implementation.

The public demo opens a timetable matrix, with school and timetable selectors at the top-left and the existing
inspection filters above the matrix. A first visit opens the full Estonian school's Variant A. Switching schools
clears filters and lesson selection; switching timetables in one school preserves filters. Switching is immediate,
requires no confirmation or solve, and retains unfinished edits with their source timetable.

Utilities contains the school-definition JSON editor, its Quick/Thorough generation controls and a scoped local-data cleanup button. Saving an
edited definition creates a named school copy. A generated timetable opens in the matrix for naming and saving.
The protected-repair entry, reopen-inspector button, separate teacher-change section and inspector JSON-route
button disappear from the demo.

The lesson inspector changes teacher, room and time slot. Every change validates the complete draft. Conflicting
intermediate changes are allowed so users can make swaps; errors identify affected lessons, explain the problem
and possible corrections, and are reachable despite filters. Mandatory errors block saving; preference warnings
are advisory. A bottom overlay toolbar appears with any change and contains the change count, validation state,
new timetable name, Save as new timetable, and Discard changes. A saved sibling version never overwrites its source.
Teacher changes are specific to that version. Named timetables and unfinished drafts persist in this browser
across visits, session expiry, and deployments, without accounts or a database.

On 2026-10-17 the user requested local-artifact cleanup in Utilities, with an explicit explanation that only this
browser/device's locally saved school definitions and timetables are deleted. Cleanup includes unfinished drafts,
editor work and artifacts saved by earlier demo versions. It requires confirmation, keeps bundled examples and
unrelated browser data, explains storage failures, and returns to the clean default timetable.

Four schools ship with the demo: an Estonian 5A/5B school, a full Estonian basic school covering grades 1–9
with parallel classes, and a playful Imperial academy with a staged curriculum. Its 18 cohorts progress from 25 weekly foundation lessons to 30 practical-preparation lessons and 35 senior operational lessons. The Estonian fixtures use real,
age-appropriate subject names and fictional Estonian staff. Star Wars subjects are English and characters may mix
eras and factions. Each school has two precomputed, meaningfully different, valid arrangements of identical lesson
requirements, teacher assignments and availability. Meadowbrook Community School adds an independently constructed
English-language example: 22 classes in Years 1–9, 614 weekly lessons, 41 teachers and 35 rooms.
Small school is removed from the public catalog.

This feature supersedes the affected demo-deployment navigation, sample selection, manual editing and browser
retention contracts. The normal local workspace, existing security/owner boundaries, queue/allowance limits and
deployment capacity/billing qualification remain governed by their existing features.
