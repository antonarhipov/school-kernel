import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyDraft} from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import {policyCandidates,audienceTeacherIssues,adoptFixedPlan} from '../../timetable-workspace/src/main/resources/static/workspace/setup-audience-teachers.js';
const fixture=()=>{const d=emptyDraft();d.classes=[{id:'c'}];d.subjects=[{id:'s'}];d.teachers=['A','B'].map(id=>({id,qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}));d.audiencePlans=[{id:'p',classId:'c',subjectId:'s',portions:[{id:'q',audienceId:'class:c',sessionIds:['1','2','3'],teacherPolicy:{mode:'automatic'}}]}];return d;};
test('candidate inheritance preserves empty versus absent restrictions and refuses ineligible fixed policies',()=>{
 const d=fixture(),p=d.audiencePlans[0],q=p.portions[0];assert.deepEqual(policyCandidates(d,p,q),['A','B']);assert.deepEqual(audienceTeacherIssues(d),[]);
 p.candidateTeacherIds=['A'];assert.deepEqual(policyCandidates(d,p,q),['A']);q.teacherPolicy.candidateTeacherIds=[];assert.deepEqual(policyCandidates(d,p,q),[]);assert(audienceTeacherIssues(d).length);
 q.teacherPolicy={mode:'fixed',fixedTeacherId:'B'};assert(audienceTeacherIssues(d).some(i=>i.message.includes('fixed teacher')));delete p.candidateTeacherIds;assert.deepEqual(audienceTeacherIssues(d),[]);
});
test('deliberate weekly sharing sums only its own audience portion, independent of workload reference',()=>{
 const d=fixture(),q=d.audiencePlans[0].portions[0];q.teacherPolicy={mode:'sharing',shares:[{id:'a',teacherId:'A',count:2},{id:'b',teacherId:'B',count:1}]};d.teachers[0].referenceWeeklyMaximum=0;
 assert.deepEqual(audienceTeacherIssues(d),[]);q.teacherPolicy.shares[0].count=3;assert(audienceTeacherIssues(d).some(i=>i.message.includes('total 4')));
});
test('explicit fixed adoption preserves original source and exact session IDs, and refuses unmappable locks',()=>{
 const d=fixture();delete d.audiencePlans;d.plans=[{id:'fixed',classId:'c',subjectId:'s',requiredCount:2,allocations:[{teacherId:'A',count:1},{teacherId:'B',count:1}],occurrences:[{id:'one',displayName:'Original label',teacherId:'A'},{id:'two',teacherId:'B'}],blockRules:[]}];
 const before=structuredClone(d),next=adoptFixedPlan(d,d.plans[0]);assert.deepEqual(d,before);assert.deepEqual(next.audiencePlans[0].portions[0].sessionIds,['one','two']);assert.deepEqual(next.audiencePlans[0].sourceFixedPlan,d.plans[0]);assert.equal(next.plans.length,0);
 d.plans[0].occurrences[0].periodLock='locked';assert.throws(()=>adoptFixedPlan(d,d.plans[0]),/cannot yet preserve/);
});
