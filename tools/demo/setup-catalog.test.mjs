import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import { subjectIssues, referencesTo } from '../../timetable-workspace/src/main/resources/static/workspace/setup-catalog.js';
test('subject hard issues preserve unfinished limits and distinguish permission from block length', () => {
  const d=emptyDraft();d.subjects=[{id:'s',displayName:'Subject',maxWeeklyReservedLessonsPerCohort:'unfinished',maxDailyLessonsPerCohort:2}];
  d.plans=[{id:'p',subjectId:'s',classId:'c',blockRules:[{length:3,count:1,operator:'EXACTLY'}]}];
  const before=structuredClone(d);const issues=subjectIssues(d);
  assert(issues.some(i=>i.message.includes('positive integer')));assert(issues.some(i=>i.message.includes('requires explicit')));
  assert(issues.some(i=>i.message.includes('below required block length 3')));assert.deepEqual(d,before);
  delete d.subjects[0].maxWeeklyReservedLessonsPerCohort;delete d.subjects[0].maxDailyLessonsPerCohort;
  assert.deepEqual(subjectIssues(d),[]);assert(!Object.hasOwn(d.subjects[0],'reservedPeriodsAllowed'));
});
test('subject removal finds plans, qualifications, subject-specific eligibility and retained room policies', () => {
  const d=emptyDraft();d.subjects=[{id:'s',displayName:'Subject'}];
  d.teachers=[{id:'t',qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}];
  d.plans=[{id:'p',subjectId:'s'}];d.subjectRoomPolicies=[{id:'policy',subjectId:'s',allowedRoomIds:[]}];
  assert.deepEqual(referencesTo(d,'subjects','s'),['teachers.0.qualifiedSubjectIds','teachers.0.eligibleClassIdsBySubject.s','plans.0.subjectId','subjectRoomPolicies.0.subjectId']);
  assert.deepEqual(referencesTo(d,'subjects','unused'),[]);
});

test('class issues preserve explicit zero, inherited defaults and multiple independent curators', async () => {
  const {classIssues,protectedPlans}=await import('../../timetable-workspace/src/main/resources/static/workspace/setup-catalog.js');
  const d=emptyDraft();d.teachers=[{id:'t'}];d.rooms=[{id:'r'}];
  d.classes=[{id:'a',displayName:'A',size:20,curatorTeacherId:'t',homeRoomId:'r',dailyLessonSpreadLimit:0},{id:'b',displayName:'B',size:22,curatorTeacherId:'t'}];
  assert.deepEqual(classIssues(d),[]);assert(!Object.hasOwn(d.classes[1],'maxDailyGaps'));
  d.classes[0].size='unfinished';assert(classIssues(d).some(i=>i.record==='a'&&i.message.includes('student count')));
  d.plans=[{id:'p',classId:'a',occurrences:[{id:'o',periodLock:'period'}]}];assert.equal(protectedPlans(d,'a')[0].id,'p');
});
