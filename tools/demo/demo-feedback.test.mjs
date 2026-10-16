import test from 'node:test';
import assert from 'node:assert/strict';
import { conflictItems, warningItems } from '../../timetable-workspace/src/main/resources/static/workspace/demo-feedback.js';

test('a teacher eligibility error marks its named lesson without blaming other lessons using that teacher', () => {
  const assignments = [ { lessonId: 'english', teacherId: 'pe-teacher', cohortId: '5A' },
    { lessonId: 'pe', teacherId: 'pe-teacher', cohortId: '5A' } ];
  const model = { assignments, assignmentMap: new Map(assignments.map(a => [a.lessonId, a])) };
  const errors = conflictItems({ status: 'INVALID_INPUT', validationReport: { errors: [
    { location: '/lessons/0/teacherId', entityIds: ['english', 'pe-teacher', 'english-subject'], message: 'teacher is not qualified' }
  ] } }, model);
  assert.deepEqual(errors[0].lessonIds, ['english']);
  assert.match(errors[0].message, /eligible teacher/);
});

test('collision participants remain explicit and actionable independently of visible filters', () => {
  const errors = conflictItems({ conflicts: [{ constraintId: 'hard.room-period', lessonIds: ['a', 'b'] }] });
  assert.deepEqual(errors[0].lessonIds, ['a', 'b']);
  assert.equal(errors[0].title, 'Room double-booked');
  assert.match(errors[0].message, /another suitable room/);
});

test('advisory totals distinguish pre-existing costs from newly introduced costs', () => {
  const warnings = warningItems({ softMatchCounts: { 'soft.teacher-gap': 7, 'soft.non-preferred-room': 2 } },
    { score: { constraintBreakdown: [{ constraintId: 'soft.teacher-gap', matchCount: 9 }, { constraintId: 'soft.non-preferred-room', matchCount: 2 }] } });
  assert.deepEqual(warnings.map(w => [w.count, w.delta]), [[7, -2], [2, 0]]);
});

test('class-gap guidance names actual empty periods using definition metadata', () => {
  const model = { maps: { periods: new Map([['opaque-period', { weekday: 'TUESDAY', order: 4, startTime: '10:45:00', endTime: '11:30:00' }]]) } };
  const errors = conflictItems({ conflicts: [{ constraintId: 'hard.cohort-gap', lessonIds: ['a'], examples: [['class', 'opaque-period']] }] }, model);
  assert.match(errors[0].message, /Empty periods: Tuesday · 4 \(10:45–11:30\)/);
  assert.ok(!errors[0].message.includes('opaque-period'));
});

test('capacity guidance states the actual class size and room capacity', () => {
  const model = { assignmentMap: new Map([['a', { cohort: { displayName: '5A', size: 28 }, room: { displayName: 'Seminar room', capacity: 12 } }]]) };
  const errors = conflictItems({ conflicts: [{ constraintId: 'hard.room-capacity', lessonIds: ['a'] }] }, model);
  assert.match(errors[0].message, /5A: 28 pupils; Seminar room: 12 seats/);
});
