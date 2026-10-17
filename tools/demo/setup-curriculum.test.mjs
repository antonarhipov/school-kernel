import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import { copyCurriculum } from '../../timetable-workspace/src/main/resources/static/workspace/setup-curriculum.js';
test('curriculum copies exactly subjects/counts to independent unassigned rows and refuses partial replacement',()=>{
  const d=emptyDraft();d.classes=[{id:'a',curatorTeacherId:'t'},{id:'b',homeRoomId:'home'}];d.subjects=[{id:'s'}];
  d.plans=[{id:'p',classId:'a',subjectId:'s',requiredCount:5,allocations:[{teacherId:'t',count:5}],seriesId:'source',blockRules:[{length:2,count:1}],fixedRoomId:'r',occurrences:[{id:'o',periodLock:'period'}]}];
  const before=structuredClone(d),next=copyCurriculum(d,'a','b'),p=next.plans[1];
  assert.equal(p.subjectId,'s');assert.equal(p.requiredCount,5);assert.deepEqual(p.allocations,[]);assert.deepEqual(p.occurrences,[]);assert.deepEqual(p.blockRules,[]);assert(!Object.hasOwn(p,'fixedRoomId'));assert.notEqual(p.seriesId,'source');assert.notEqual(p.id,'p');assert.deepEqual(d,before);assert.deepEqual(next.classes,d.classes);
  const again=structuredClone(next);assert.throws(()=>copyCurriculum(next,'a','b'),/empty destination/);assert.deepEqual(next,again);
  d.plans[0].requiredCount='unfinished';assert.throws(()=>copyCurriculum(d,'a','b'),/No rows were copied/);assert.equal(d.plans.length,1);
});
