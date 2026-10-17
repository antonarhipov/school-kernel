import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import { planIssues,synchronizeOccurrences,configurePlanLimits } from '../../timetable-workspace/src/main/resources/static/workspace/setup-plans.js';
const fixture=()=>{const d=emptyDraft();d.subjects=[{id:'s',displayName:'Subject'}];d.classes=[{id:'c',displayName:'Class'}];d.teachers=['a','b'].map(id=>({id,qualifiedSubjectIds:['s'],eligibleClassIdsBySubject:{s:['c']}}));return d;};
test('ordinary split teaching retains stable surviving occurrences and one spreading series without unassigned placeholders',()=>{
  const d=fixture();let p={id:'p',classId:'c',subjectId:'s',requiredCount:5,seriesId:'series',allocations:[{teacherId:'a',count:3}],occurrences:[]};d.plans=[p];
  p=synchronizeOccurrences(p,d);assert.equal(p.occurrences.length,3);assert(p.occurrences.every(o=>o.seriesId==='series'));const first=structuredClone(p.occurrences);
  p.allocations.push({teacherId:'b',count:2});p=synchronizeOccurrences(p,d);assert.deepEqual(p.occurrences.slice(0,3),first);assert.equal(p.occurrences.length,5);
  p.allocations[0].count=2;p=synchronizeOccurrences(p,d);assert.deepEqual(p.occurrences.filter(o=>o.teacherId==='a'),first.slice(0,2));assert.equal(p.occurrences.length,4);
  d.plans=[p];assert(planIssues(d).some(i=>i.message.includes('1 unassigned')));
  p.allocations[0].count=4;d.plans=[p];assert(planIssues(d).some(i=>i.message.includes('over-allocation')));
});
test('protected occurrences and homogeneous ungrouped templates survive; huge draft counts cannot materialize unbounded lessons',()=>{
  const d=fixture();const protectedRow={id:'p',classId:'c',subjectId:'s',requiredCount:2,allocations:[{teacherId:'a',count:2}],occurrences:[{id:'locked',teacherId:'a',periodLock:'mon'}]};
  assert.deepEqual(synchronizeOccurrences(protectedRow,d),protectedRow);
  let p={id:'p',classId:'c',subjectId:'s',requiredCount:2,allocations:[{teacherId:'a',count:2}],occurrences:[],occurrenceTemplate:{requiredRoomCapabilityIds:[],preferredRoomIds:['r']}};
  p=synchronizeOccurrences(p,d);assert(p.occurrences.every(o=>!Object.hasOwn(o,'seriesId')&&o.preferredRoomIds[0]==='r'&&o.requiredRoomCapabilityIds.length===0));
  configurePlanLimits({lessonMax:3});p.allocations[0].count=2147483647;d.plans=[p];const next=synchronizeOccurrences(p,d);assert.equal(next.occurrences.length,2);assert.equal(next.allocations[0].count,2147483647);assert(planIssues(d).some(i=>i.message.includes('configured demo limit of 3')));configurePlanLimits({lessonMax:2500});
});
