# Proposal: Student Groups and Coordinated Lessons

## Overview

A class is not always the unit that attends a lesson. Schools often divide classes into smaller groups for language learning, practical subjects, room-capacity limits, electives, or specialist support. Those groups may be combined or rearranged differently across subjects, and their lessons may need to be coordinated across the same period, different periods, or different days.

This proposal introduces **Student Groups and Coordinated Lessons**: a flexible capability for defining student groups, assigning lesson requirements to those groups, and expressing relationships between sessions.

The goal is to support common school scheduling practices without adding a separate solver rule for every scenario.

## 1. Use cases

### 1.1 Parallel groups for the same subject

A class of 28 students is divided into two English groups of 14. Each group has a different teacher, and both lessons happen in the same period.

**Scheduling requirements:**
- Both groups receive the required number of English lessons.
- Each session has an available teacher and suitable room.
- Students and teachers are not scheduled for overlapping lessons.

### 1.2 Cross-subject group swaps

Group A has English while Group B has handicraft. In another slot, the groups swap subjects. The two slots may be on different days.

**Scheduling requirements:**
- The coordinated sessions cover the intended students and subjects.
- Group swaps follow the configured relationship.
- Teacher availability, room suitability, capacity, and student conflicts are respected.

### 1.3 Different group arrangements for different subjects

A class may be divided into two groups for English but four groups for handicraft. Group boundaries may differ between subjects.

**Scheduling requirements:**
- Support two, three, four, or more groups.
- Allow a lesson to use a particular group arrangement without forcing every subject to use the same arrangement.
- Track student membership well enough to detect overlaps between groups.

### 1.4 Room-capacity-driven splitting

A laboratory or workshop may not accommodate the whole class. Students attend in smaller groups, potentially in separate sessions.

**Scheduling requirements:**
- Rooms have capacity and capability requirements.
- Sessions do not exceed room capacity.
- The required student groups receive their teaching time.

### 1.5 Electives and option blocks

Students from several homeroom classes may combine into one elective group. Students from the same class may choose different subjects.

**Scheduling requirements:**
- A session can include students from more than one class.
- A student's actual subject choices determine their conflicts, not just their homeroom class.
- Shared sessions are represented once, rather than duplicated for each source class.

### 1.6 Team teaching and support groups

A session may require more than one teacher, or a specialist may teach a small group while the rest of the class has another lesson.

**Scheduling requirements:**
- Sessions can require multiple teachers.
- A group can be a subset of a class.
- Shared and separate activities can coexist where needed.

### 1.7 Rotating practicals and alternating weeks

Groups may rotate through stations or alternate access to a specialist room. Some patterns may span multiple periods or alternate across weeks.

These are valuable extensions, but they need not be part of the first release.

## 2. Core modeling principles

Keep three concepts separate:

### Student group

A defined set of students. Examples include `7A-English-Group-1` or `7A-Handcraft-Group-3`.

### Group arrangement

A way of partitioning a cohort for a particular subject or activity. The same class may use two groups for English and four for handicraft.

### Coordinated sessions

A relationship between lesson sessions that must happen together, alternate, or follow a sequence. For example, English and handicraft may happen simultaneously for two groups, with the groups swapping subjects in another slot.

**Group membership belongs to students; the group arrangement belongs to the lesson or activity.** A class should not need one permanent split for all subjects.

## 3. Illustrative example

Class 7A has 28 students.

- English uses two groups of 14: `E1` and `E2`.
- Handicraft uses four groups of 7: `H1`, `H2`, `H3`, and `H4`.

A simplified coordinated schedule might look like this:

| Slot | English | Handicraft |
|---|---|---|
| Tuesday, period 2 | E1 | H3 and H4 |
| Tuesday, period 2 | E2 | H1 and H2 |
| Thursday, period 4 | E1 | H1 and H2 |
| Thursday, period 4 | E2 | H3 and H4 |

This is illustrative, not a complete scheduling prescription. It is valid only if the group memberships, lesson durations, teachers, and room requirements line up. In particular, the model must know whether the handicraft groups can be taught together and whether each teacher and room can accommodate the combined students.

The general requirement is that students receive their required lessons without clashes, and that every session respects its resource and capacity constraints.

## 4. Proposed domain model

The following are conceptual boundaries. They do not all need to become separate Java classes in the first implementation.

| Concept | Responsibility |
|---|---|
| `StudentGroup` | Identifies a set of students, possibly derived from a class split or elective choice. |
| `GroupArrangement` | Defines how a cohort is partitioned for a particular subject or activity. |
| `LessonRequirement` | Specifies subject, frequency, duration, eligible students, and grouping requirements. |
| `LessonSession` | A scheduled occurrence with its student audience, teacher(s), room(s), and time. |
| `SessionGroup` | Connects a session to the student group or groups attending it. |
| `SessionCoordination` | Describes synchronized, alternating, or sequential sessions. |
| `Room` | Defines capacity, room type, equipment, and availability. |

### Group formation versus timetable generation

These are separate problems:

1. **Group formation:** Which students belong together?
2. **Timetable generation:** When and where do those groups have lessons?

Most schools already know their group memberships, or can define simple rules such as equal-sized groups. The first release should allow schools to define groups explicitly and let the solver schedule them. Automatic group formation can be considered later if there is a demonstrated need.

This keeps the search space smaller and makes the generated timetable easier to understand.

## 5. Solver integration

Extend the existing timetable solver rather than introducing a separate group scheduler.

Each schedulable lesson session needs a time assignment and the resources it uses. Its audience may be a full class, a subgroup, or a group assembled from several classes.

### Hard constraints

- A student cannot attend two sessions at the same time.
- A teacher cannot teach two sessions simultaneously.
- A room cannot host two sessions simultaneously.
- Room capacity must not be exceeded.
- Required group sessions must be scheduled.
- Coordinated sessions must obey their synchronization or sequencing rules.
- Each student must receive the required number of lessons.
- Teacher qualifications, room equipment, availability, and lesson durations must be respected.

### Soft constraints

- Minimize gaps in students' timetables.
- Avoid unnecessarily fragmented school days.
- Keep teacher schedules compact.
- Prefer consecutive practical sessions where appropriate.
- Minimize changes to an existing timetable when repairing it.
- Avoid unnecessary room changes when students remain in the same activity.
- Balance undesirable time slots between groups.

### Avoid double-counting shared lessons

A shared lesson should not count as multiple lessons merely because multiple groups attend it. If one teacher teaches two groups together in one session, represent the shared audience once, with the relevant student memberships, rather than creating duplicate sessions that consume the same teacher and room.

If two English groups are taught simultaneously by two different teachers, represent these as two sessions, each with its own resources and student audience.

## 6. Expressing coordination

Avoid adding one-off fields such as `swapWithSubject`, `isSplitLesson`, or `groupRotationType` to the lesson entity. Such fields are likely to become difficult to extend as schools introduce new patterns.

Instead, represent relationships between lesson requirements or sessions. Potential relationship types include:

- `SAME_TIME`: two or more sessions must start in the same period.
- `DIFFERENT_TIME`: sessions must not overlap.
- `SEQUENCE`: one session must precede another.
- `SAME_STUDENT_COVERAGE`: sessions collectively cover a specified cohort.
- `ROTATION`: groups progress through a defined sequence of activities.
- `REQUIRES_GROUPING`: a lesson must use a defined group arrangement.

These names are illustrative. They should be refined against the actual domain model and solver behavior. Not every relationship needs to be implemented in the first release; begin with the few needed for parallel groups and cross-subject swaps.

Keep `SAME_TIME` distinct from `SAME_DAY`. A group swap may require simultaneous sessions on Tuesday and corresponding sessions on Thursday. The coordination model should express that relationship explicitly.

## 7. Proposed first release

Call the capability **Student Groups and Coordinated Lessons**.

### A. Define groups and arrangements

- Allow schools to define two, three, four, or more groups per class.
- Support explicit student membership and optional group-size rules.
- Allow each lesson to use a particular arrangement.
- Do not require every subject to use the same group boundaries.

### B. Schedule parallel group sessions

- Generate the required sessions for each group.
- Prevent student, teacher, and room conflicts.
- Enforce room capacity, availability, and subject requirements.

### C. Coordinate group swaps across subjects

- Allow groups to attend different subjects in parallel.
- Allow them to swap in another slot, including on another day.
- Support explicit synchronization rules rather than relying on the solver to infer the intended relationship.

### D. Support room constraints

- Model maximum capacity and room capabilities.
- Allow schools to specify when a lesson must be delivered to smaller groups.
- Ensure that all required groups receive the required teaching time.

### Defer to later releases

Consider postponing the following until the core model has been validated:

- Automatic student assignment to groups.
- Fortnightly or alternating-week timetables.
- Complex station-based practicals.
- General-purpose arbitrary rotation patterns.

The first four capabilities already deliver practical value and provide a foundation for these extensions.

## 8. Acceptance tests

Before building the full UI, validate the domain model and solver with focused test cases.

| Test | Expected result |
|---|---|
| A class splits into two English groups | Both groups receive the required sessions without student or teacher conflicts. |
| Two groups swap English and handicraft | Coordinated sessions satisfy the configured group and subject requirements. |
| A class splits into four workshop groups | Every group receives the required teaching time, and room capacity is respected. |
| Students from two classes share an elective | They can attend one shared session without any student being double-booked. |
| A lesson requires a laboratory | Only rooms with the required capabilities can host it. |
| One group has no feasible teacher or room | The solver reports infeasibility or a clear scheduling failure instead of silently dropping the lesson. |
| Coordinated sessions occur on different days | The configured cross-day relationship is respected; the solver does not assume every swap belongs to one simultaneous block. |
| Two named groups have overlapping membership | The solver prevents simultaneous sessions that would conflict for shared students. |

## 9. Architectural decision to make early

The solver must reason about the **actual student audience of every lesson**.

This does not necessarily mean modeling each student as a planning entity. If every student in a group always attends together, a group-level representation may be more efficient. However, the model needs enough membership information to detect overlaps between groups, especially when elective groups span multiple classes or when different arrangements overlap.

For example, Group A might contain students 1–14, while Group B contains students 8–21. Although they have different names, they share students 8–14. Scheduling them simultaneously would create a conflict.

This distinction will determine whether group scheduling remains manageable or becomes a collection of special cases.

## 10. Recommendation

Start by adding **student audiences and group arrangements** to the lesson model. Then implement parallel group sessions and coordinated cross-subject sessions.

These are the foundations. Room-capacity-driven splitting, electives, rotating practicals, and more complex scheduling patterns can build on the same model over time.

The guiding principle is simple: model who attends each session and how sessions relate to one another, rather than treating a class as an indivisible scheduling unit.
