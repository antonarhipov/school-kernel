# Timetable PDF export

## Scope and resolved decisions

The PDF export uses exactly the selected school, selected schedule and this scope precedence:

| Class filter | Teacher filter | PDF scope |
| --- | --- | --- |
| None | None | Full school |
| Selected | None | That class's whole week |
| None | Selected | That teacher's whole week |
| Selected | Selected | That class's whole week, regardless of teacher |

Export PDF downloads directly from these selections. No separate type/entity chooser is offered. Day/Week, room, subject, period and search controls do not truncate the selected whole-week scope. The full school PDF contains every class and lesson of the selected school/schedule, combined into one PDF of class weekly timetables in the school class-list order. Each class starts on a new page and uses exactly the same layout and details as its individual class export. Class and teacher PDFs have weekdays as columns and periods as rows. Long timetables continue vertically with repeated headings rather than shrinking text. An unassigned class in an otherwise nonempty school receives an empty weekly grid; a wholly empty school still refuses export.

A4 landscape pages use a restrained green palette, school/timetable/class titles, weekday headings, period numbers, actual lesson times, subject, teacher, class and room. Entirely unused leading/trailing slots may be omitted from class/teacher grids; internal free slots remain explicit. Long content wraps and continues onto additional pages with repeated headings. Export uses local bundled libraries/fonts. These layout defaults keep the requested three scopes explicit and readable.

## Actors and terms

- Administrator: a visitor inspecting an available timetable.
- Export context: the displayed school, timetable and unpublished changes captured when Export PDF is requested.
- Export scope: exactly one class, one teacher, or the full school, always covering the whole week.
- Draft: visible unpublished timetable changes; a draft export is explicitly labelled.

## Use-case map

| ID | Actor goal | Primary actor | Relations |
| --- | --- | --- | --- |
| UC-1 | Download a readable weekly timetable PDF (primary) | Administrator | None |

## UC-1 - Download a readable weekly timetable PDF (primary)

- Goal: Print or share a faithful, readable timetable.
- Primary actor: Administrator.
- Supporting actors: None.
- Trigger: Administrator requests Export PDF.
- Preconditions: A timetable is displayed.
- Relations: Requires none; includes none; extends none.

### Main success scenario

1. The administrator selects a school and schedule, optionally selecting a class and/or teacher in the timetable filters.
2. The administrator requests Export PDF.
3. The system applies the scope precedence above, captures the selected school/schedule and scope, indicates preparation, and downloads a PDF with a descriptive school/schedule/type/entity/week filename.
4. The administrator opens a legible timetable with complete weekly lesson details for that scope, school/schedule names, draft status when applicable and page numbering.

### Extensions

- 1a. No timetable exists: export is disabled; end.
- 2a. No lessons are scheduled for the selected scope: explain the empty scope, produce no empty attachment, preserve selections and allow retry after changing filters; resume at step 1.
- 3a. A required resource cannot load, the selected entity is unavailable, or content cannot be rendered faithfully: explain the failure, preserve the timetable and filters, and allow retry; resume at step 2.
- 3b. Another preparation is in progress: prevent duplicate requests until it finishes; resume at step 2.
- 4a. A timetable contains unpublished changes: every page clearly identifies the export as a draft; end.

### Guarantees

- G1. Export is read-only: timetable, assignments, accepted baseline, drafts, stored browser values, selected school, filters and solver state remain unchanged on success and failure.
- G2. Every assignment belonging to the chosen whole-week scope appears exactly once with subject, teacher, class, room, weekday, period number and start/end times where supplied. Labels retain Unicode supported by the bundled font; unsupported glyphs cause an explicit refusal instead of silent corruption.
- G3. The full-school PDF is a collection of every class weekly timetable, with each class starting on a fresh page in class-list order and matching its individual export, including authoring breaks and class lunch. Every page is A4 landscape with readable text of at least 8 points, wrapped labels, repeated context/headings, page numbers, and no clipped or overlapping content. Breaks and class lunch from available authoring metadata are identified.
- G4. The keyboard-accessible export control works at desktop and phone widths, under the existing content security policy, without opening a popup, mutating requests or remote library/font requests. Libraries load only on first export.
- G5. The generated file reflects the export context captured when Export PDF is requested even if the administrator subsequently changes the displayed view.

### Postconditions

- Success: A faithful PDF is downloaded and the timetable remains usable.
- Minimal guarantee: No timetable or stored data changes, and an error permits retry when export fails.

## Out of scope

Public deployment, saved export preferences, bulk teacher books, room-only PDFs, day-only PDFs, new schedule data, edits to accepted ZIP exports and server-side PDF generation. The current browser-local timetable surface is the entry point; the historical database workspace remains separate.
