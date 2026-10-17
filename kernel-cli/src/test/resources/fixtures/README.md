# Test-only school fixtures

These fixtures exercise legacy catalog compatibility, shared teachers and rooms, repair deltas, curator bindings and room policies. Staff names and identifiers are fictional. The older public school examples have been retired; these files are excluded from the demo image.

- `shared-teachers-two-classes.json`: two classes, 42 lessons.
- `shared-teachers-seven-classes.json`: seven classes, 176 lessons.
- `policy-school.json`: catalog 9 room, curator and day-edge regression coverage.
- `quality-school.json`: catalog 2 quality/repair compatibility coverage.
- `initial-school.json`, `updated-school.json`, `empty-school.json`: minimal CLI/workspace contract fixtures.

The authoritative user-facing schools and eight precomputed timetables live under `timetable-workspace/src/main/resources/demo/timetables/`. No demo maintenance step rebuilds them from these test fixtures.
