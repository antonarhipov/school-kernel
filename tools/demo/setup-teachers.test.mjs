import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import { eligible, qualification, workload, teacherIssues } from '../../timetable-workspace/src/main/resources/static/workspace/setup-teachers.js';
test('qualification and explicit subject/class eligibility never imply a cross product or allocation',()=>{
  const d=emptyDraft();d.subjects=[{id:'math'},{id:'science'},{id:'curator',curatorLesson:true}];d.classes=[{id:'a',curatorTeacherId:'t'},{id:'b'}];
  const t={id:'t',displayName:'Teacher',qualifiedSubjectIds:['math','science'],eligibleClassIdsBySubject:{math:['a'],science:['b'],curator:['a']}};d.teachers=[t];
  assert(eligible(d,t,'math','a'));assert(!eligible(d,t,'math','b'));assert(!eligible(d,t,'science','a'));assert(eligible(d,t,'science','b'));
  assert(qualification(d,t,'curator','a'));assert(!qualification(d,t,'curator','b'));assert(!t.qualifiedSubjectIds.includes('curator'));
  d.plans=[{id:'p',classId:'a',subjectId:'math',requiredCount:5,allocations:[{teacherId:'t',count:5}],blockRules:[{length:2,count:1}]}];
  assert.equal(workload(d,'t'),5);t.qualifiedSubjectIds=['science'];assert(teacherIssues(d).some(i=>i.message.includes('allocated row p')));
  assert.equal(workload(d,'t'),5);assert.equal(d.plans[0].allocations[0].count,5);
});
