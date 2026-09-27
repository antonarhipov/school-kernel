# Technical Rules: Cohort curators and home rooms

## Rules

### RULE-1 - Versioned fields

- Applies to: UC-1
- Constraint: Catalog 8 MUST accept `curatorTeacherId` and `homeRoomId` on cohorts and `curatorLesson` on subjects.
  Catalogs 1 through 7 MUST reject each. Each MUST participate in definition revisions.
- Verification: `ContractTest.curatorFieldsSchemaAndRevision`.

### RULE-2 - Curator lessons are validated input

- Constraint: `DefinitionValidator` MUST reject a curator lesson whose cohort has no curator or whose teacher is not
  the curator, unknown curator or home room references, and a home room contradicting the lesson's room lock,
  capacity, or required capabilities. The curator MUST NOT need the subject in `qualifiedSubjectIds`.
- Verification: `DefinitionValidatorTest.curatorLessonsBelongToTheCohortCurator`,
  `DefinitionValidatorTest.homeRoomContradictionsAreInvalidInput`.

### RULE-3 - One count, physical level

- Constraint: `SchoolConstraintProvider.cohortHomeRoom`, `ScheduleEvaluator`, and `HardConstraintDiagnostics` MUST
  share `SchoolConstraintProvider.outsideHomeRoom` and use hard level 0, alongside room locks.
- Reason: A home room binds a curator lesson like a room lock; it is not a day-shape rule.
- Verification: `HomeRoomConstraintTest.curatorLessonOutsideHomeRoomIsPhysicalHard`.

### RULE-4 - Mapping and actor-boundary evidence

- Constraint: `PlanningMapper` MUST carry the home room only on curator lessons and MUST default an empty room
  preference to a home room that can host the lesson. Preflight MUST report a curator lesson whose home room is never
  usable, and a baseline outside the home room MUST NOT be kept as the starting assignment. Packaged planning MUST
  hold a curator lesson in its home room over its own preference, refuse it once the home room is closed, and reject
  the fields under catalog 7.
- Verification: `HomeRoomConstraintTest.homeRoomBindsCuratorLessonsAndIsTheDefaultPreference`, `HomeRoomCliIT`.

### RULE-5 - MVK

- Constraint: MVK MUST match the normative configuration in the specification, including the explicit room locks
  for eleven curator-taught IntÕ lessons and fourteen Siiri Aiaste Ajalugu lessons, validate, and plan feasibly at a
  60-second limit with a result that verifies.
- Verification: `ContractTest.mvkDeclaresCuratorsAndHomeRooms`, `HomeRoomConstraintTest.mvkBindsKlassitundToTheHomeRoom`;
  packaged MVK plan and verify.
