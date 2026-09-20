# School Kernel

**School Kernel** is a headless school scheduling and replanning engine built with **Java** and **Timefold**.

The initial goal is to build the engine, not a UI. It takes a machine-readable description of a school and its scheduling requirements, produces a timetable, and can replan that timetable when circumstances change.

## Core workflow

```text
School Definition
       │
       ▼
 School Kernel
       │
       ▼
   Timefold
       │
       ▼
    Timetable
```

For replanning:

```text
School Definition
       +
Current Timetable
       +
Changes
       │
       ▼
 School Kernel
       │
       ▼
Replanned Timetable
```

## Domain Model

The initial model should cover:

* **Teachers** — subjects, availability, constraints
* **Classes** — students/groups and required lessons
* **Subjects** — what is being taught
* **Rooms** — capacity, equipment, availability
* **Periods** — days and time slots
* **Lessons** — required teaching activities
* **Constraints** — hard requirements and soft preferences
* **Timetable** — assignments of lessons to teachers, rooms and periods

## Scheduling

School Kernel delegates constraint solving and optimization to **Timefold**.

Hard constraints must always be satisfied, for example:

* A teacher cannot teach two classes simultaneously.
* A class cannot attend two lessons simultaneously.
* A room cannot host two classes simultaneously.
* Teachers and rooms must be available.
* Lessons must use appropriate rooms and qualified teachers.

Soft constraints represent preferences such as:

* Minimize gaps in teacher schedules.
* Distribute lessons across the week.
* Avoid undesirable periods.
* Prefer particular rooms or time slots.

## Replanning

Replanning is a core feature rather than an afterthought.

The engine accepts changes such as:

```text
Teacher unavailable
Room unavailable
Class unavailable
Lesson cancelled
Lesson moved
Teacher changed
Room changed
```

The engine should produce a new valid timetable while minimizing unnecessary changes to the existing timetable.

Conceptually:

> **Find a valid schedule while changing as little as possible.**

This makes schedule stability an important optimization objective.

## Input and Output

The engine should expose a simple machine-readable contract, initially using **JSON or YAML**.

Example:

```bash
school-kernel plan school.yaml
```

produces:

```text
timetable.json
```

Replanning:

```bash
school-kernel replan school.yaml timetable.json changes.yaml
```

produces a revised timetable.

The output should include:

* timetable assignments
* solver score
* changes from the previous timetable
* unresolved conflicts, if any
* useful explanations for infeasible schedules

## Architecture

```text
Input
  │
  ▼
Domain Model
  │
  ▼
Problem Builder
  │
  ▼
Timefold Model
  │
  ▼
Timefold Solver
  │
  ▼
Solution
  │
  ▼
Output
```

Timefold-specific implementation details should remain isolated from the external domain model and input/output formats.

## Initial Scope

The first version should provide:

1. A clean Java domain model.
2. JSON/YAML input and output.
3. Initial timetable generation using Timefold.
4. Hard and weighted soft constraints.
5. Replanning from an existing timetable.
6. Minimal-change optimization during replanning.
7. A simple CLI.
8. A set of representative scheduling fixtures and automated tests.

No web UI, database, authentication, or user management is required initially.

## Long-Term Vision

School Kernel should become a **scheduling runtime for schools**.

The timetable is not a static document. It is the current state of a system that continuously changes.

```text
Plan → Observe → Change → Replan
```

The eventual UI or API will simply provide different ways to interact with the School Kernel and display its results.
