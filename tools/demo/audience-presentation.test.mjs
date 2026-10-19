import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { makeAcceptedModel } from '../../timetable-workspace/src/main/resources/static/workspace/accepted-model.js';
import { audienceCell, sessionCount } from '../../timetable-workspace/src/main/resources/static/workspace/audience-model.js';
import { isTimetable } from '../../timetable-workspace/src/main/resources/static/workspace/timetable-storage.js';
const fixture=()=>({id:'t',name:'Cross-day',definition:JSON.parse(fs.readFileSync('kernel-cli/src/test/resources/fixtures/audience/cross-day.json')),result:{schemaVersion:2,timetable:{assignments:JSON.parse(fs.readFileSync('kernel-cli/src/test/resources/fixtures/audience/cross-day-assignments.json'))}}});
test('nested composite spans preserve actual cross-day positions and exact pair',()=>{
 const pair=fixture(),before=structuredClone(pair),m=makeAcceptedModel(pair);
 assert.equal(sessionCount(pair.definition),6);assert(isTimetable(pair));assert.deepEqual(pair,before);
 const slots=[...m.assignmentsByCell.keys()];assert.equal(slots.length,2);
 for(const key of slots){const [c,p]=key.split('\0'),cell=audienceCell(m,c,p,m.assignmentsByCell.get(key));assert.equal(cell.untaught.length,0);assert.equal(cell.spans.length,3);assert.deepEqual(cell.context.lanes.map(g=>g.id),['H1','H2','H3','H4']);const e=cell.spans.find(s=>s.item.audienceId.startsWith('E'));assert.equal(e.length,2);assert.equal(e.start,e.item.audienceId==='E1'?0:2);}
});
test('filtering distinguishes hidden teaching from true no-lesson blocks',()=>{
 const pair=fixture(),m=makeAcceptedModel(pair),item=m.assignments.find(a=>a.audienceId==='E1');
 let cell=audienceCell(m,item.cohortId,item.periodId,[item]);assert(cell.filtered);assert.equal(cell.all.length,3);assert.equal(cell.untaught.length,0);
 pair.result.timetable.assignments=pair.result.timetable.assignments.filter(a=>a.lessonId!==item.lessonId);
 const partial=makeAcceptedModel(pair);cell=audienceCell(partial,item.cohortId,item.periodId,[]);assert.equal(cell.untaught.reduce((n,b)=>n+b.size,0),14);assert.equal(cell.spans.filter(s=>s.audience).length,2);
});
test('saved pair guard rejects missing, duplicate, wrong audience and unsupported versions',()=>{
 const pair=fixture();assert(isTimetable(pair));const a=pair.result.timetable.assignments[0];a.audienceId='missing';assert(!isTimetable(pair));
 const missing=fixture();missing.result.timetable.assignments.pop();assert(!isTimetable(missing));const duplicate=fixture();duplicate.result.timetable.assignments[0]=duplicate.result.timetable.assignments[1];assert(!isTimetable(duplicate));const version=fixture();version.definition.schemaVersion=3;assert(!isTimetable(version));
});

test('quality rejects summaries whose audience/workload values do not reconcile with the exact pair',async()=>{
 const {renderAudienceQuality}=await import('../../timetable-workspace/src/main/resources/static/workspace/audience-quality.js');
 const pair=fixture();pair.result.score={internalGapStudentPeriods:0};pair.result.quality={gaps:[],coverageStudentPeriods:0,gapDetailCount:0,gapDetailsTruncated:false,teacherWorkload:pair.definition.teachers.map(t=>({teacherId:t.id,actualSessions:pair.result.timetable.assignments.filter(a=>a.teacherId===t.id).length,referenceWeeklyMaximum:t.referenceWeeklyMaximum??null}))};
 assert.match(renderAudienceQuality(makeAcceptedModel(pair),String),/0 student-periods/);
 pair.result.quality.teacherWorkload[0].actualSessions=999;
 assert.match(renderAudienceQuality(makeAcceptedModel(pair),String),/stale or cannot be reconciled/);
 assert.doesNotMatch(renderAudienceQuality(makeAcceptedModel(pair),String),/999/);
});

test('overlapping provisional or comparison attendance uses a summary instead of hiding an overlaid lesson',()=>{
 const pair=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).nested;
 const first=pair.assignments.find(a=>a.lessonId==='e1');pair.assignments.find(a=>a.lessonId==='h1').periodId=first.periodId;
 const model=makeAcceptedModel({definition:pair.definition,result:{timetable:{assignments:pair.assignments}}});
 const cell=audienceCell(model,'c',first.periodId,model.assignments);assert.equal(cell.spans,null);assert.equal(cell.all.length,4);
});
