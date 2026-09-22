// The inspection UI has one read-only, indexed representation of the accepted snapshot.
// Renderers receive this model; they never rebuild it from DOM positions or mutable view state.
export function makeAcceptedModel(baseline) {
  const immutableBaseline = structuredClone(baseline);
  deepFreeze(immutableBaseline);
  const definition = immutableBaseline.definition;
  const maps = Object.fromEntries(['subjects', 'teachers', 'cohorts', 'rooms', 'periods', 'lessons']
    .map(key => [key, new Map(definition[key].map(item => [item.id, item]))]));
  const weekdays = Object.freeze([...new Set(definition.periods.map(period => period.weekday))]);
  const assignments = Object.freeze(immutableBaseline.result.timetable.assignments.map(assignment => Object.freeze({ ...assignment,
    subject: maps.subjects.get(assignment.subjectId), teacher: maps.teachers.get(assignment.teacherId), cohort: maps.cohorts.get(assignment.cohortId),
    room: maps.rooms.get(assignment.roomId), period: maps.periods.get(assignment.periodId), lesson: maps.lessons.get(assignment.lessonId) })));
  const assignmentMap = new Map(assignments.map(item => [item.lessonId, item]));
  const assignmentsByCell = new Map();
  const assignmentsBySubject = new Map();
  const assignmentsByTeacher = new Map();
  for (const assignment of assignments) {
    const key = `${assignment.cohortId}\u0000${assignment.periodId}`;
    const cell = assignmentsByCell.get(key) || [];
    cell.push(assignment);
    assignmentsByCell.set(key, cell);
    addIndexed(assignmentsBySubject, assignment.subjectId, assignment);
    addIndexed(assignmentsByTeacher, assignment.teacherId, assignment);
  }
  freezeIndex(assignmentsByCell);
  freezeIndex(assignmentsBySubject);
  freezeIndex(assignmentsByTeacher);
  return Object.freeze({ definition, assignments, assignmentMap, assignmentsByCell, assignmentsBySubject, assignmentsByTeacher, maps, weekdays });
}

function addIndexed(index, key, assignment) {
  const values = index.get(key) || [];
  values.push(assignment);
  index.set(key, values);
}

function freezeIndex(index) {
  for (const [key, values] of index) index.set(key, Object.freeze(values));
}

function deepFreeze(value) {
  if (!value || typeof value !== 'object' || Object.isFrozen(value)) return value;
  Object.freeze(value);
  for (const nested of Object.values(value)) deepFreeze(nested);
  return value;
}
