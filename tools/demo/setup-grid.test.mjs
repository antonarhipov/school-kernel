import test from 'node:test';
import assert from 'node:assert/strict';
import { gridIssues, periodReferences, applyReferenceDecisions } from '../../timetable-workspace/src/main/resources/static/workspace/setup-grid.js';
import { emptyDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
const interval = (id,start,end,kind='Lesson') => ({id, periodId:id, day:'MONDAY',kind,label:id,start,end});
test('incomplete, overlapping and out-of-order day input remains located draft data', () => {
  const d=emptyDraft(); d.grid={activeDays:['MONDAY'],intervals:[interval('a','08:00','08:45'),interval('b','08:30','08:20')]};
  const issues=gridIssues(d); assert(issues.some(i=>i.record==='b'&&i.message.includes('end must')));
  assert(issues.some(i=>i.record==='b'&&i.message.includes('overlap')));
  d.grid.intervals[1].start='unfinished'; assert(gridIssues(d).some(i=>i.record==='b'&&i.message.includes('both times')));
  assert.equal(d.grid.intervals[1].start,'unfinished');
});
test('reference replacement requires every explicit valid decision and preserves whitelist intent and other values', () => {
  const d=emptyDraft(); d.grid={activeDays:['MONDAY'],intervals:[interval('a','08:00','08:45'),interval('b','09:00','09:45')]};
  d.teachers=[{id:'teacher',availability:{intent:'whitelist',availablePeriodIds:['a','b']},undesirablePeriodIds:['a']}];
  d.classes=[{id:'class',lunchPeriodIds:['a'],periodLock:'a'}];
  d.rooms=[{id:'room',availability:{intent:'all',unavailablePeriodIds:[]}}];
  const before=structuredClone(d),refs=periodReferences(d,new Set(['a','b']));
  assert.equal(refs.length,5);
  const next=structuredClone(d);next.grid.intervals=[interval('c','10:00','10:45')];
  assert.throws(()=>applyReferenceDecisions(next,refs,[]),/Every/);
  assert.throws(()=>applyReferenceDecisions(next,refs,refs.map(()=>'missing')),/surviving/);
  const resolved=applyReferenceDecisions(next,refs,refs.map(r=>r.path.includes('periodLock')?'c':'__clear__'));
  assert.deepEqual(resolved.teachers[0].availability,{intent:'whitelist',availablePeriodIds:[]});
  assert.deepEqual(resolved.classes[0],{id:'class',lunchPeriodIds:[],periodLock:'c'});
  assert.deepEqual(resolved.rooms,before.rooms);assert.deepEqual(d,before);
});
