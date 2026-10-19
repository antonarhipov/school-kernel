import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {makeAcceptedModel} from '../../timetable-workspace/src/main/resources/static/workspace/accepted-model.js';
import {audienceEditProposal} from '../../timetable-workspace/src/main/resources/static/workspace/audience-manual.js';
const make=()=>{const p=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).nested;return makeAcceptedModel({definition:p.definition,result:{timetable:{assignments:p.assignments},coordinationMatches:[{ruleId:'r',type:'START_TOGETHER',rounds:[{occurrences:[{sessionIds:['e1']},{sessionIds:['h3']},{sessionIds:['h4']}]}]}]}})};
test('manual individual edit stays independent; certified combined proposal lists every secondary change without mutation',()=>{
 const model=make(),before=JSON.stringify(model.result);
 const proposal=audienceEditProposal(model,'e1','periodId','0-2');
 assert.deepEqual(proposal.single,[{lessonId:'e1',periodId:'0-2'}]);assert.deepEqual(proposal.combined,[{lessonId:'e1',periodId:'0-2'},{lessonId:'h3',periodId:'0-2'},{lessonId:'h4',periodId:'0-2'}]);assert.equal(JSON.stringify(model.result),before);
});
test('teacher replacement requires eligible automatic allocation; fixed policy cannot become sharing',()=>{
 const model=make();assert.throws(()=>audienceEditProposal(model,'e1','teacherId','TH1'),/eligible/);
 model.assignmentMap.get('e1').lesson.teacherPolicy={mode:'fixed',teacherId:'TE'};
 assert.throws(()=>audienceEditProposal(model,'e1','teacherId','TH1'),/fixed/);
});
test('an impossible combined destination still permits an explicit individual provisional edit',()=>{
 const pair=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).nested;
 const model=makeAcceptedModel({definition:pair.definition,result:{timetable:{assignments:pair.assignments},coordinationMatches:[{ruleId:'r',type:'SAME_DAY',rounds:[{occurrences:[{sessionIds:['e1'],periodIds:['1-2']},{sessionIds:['h1'],periodIds:['3-4']}]}]}]}});
 const proposal=audienceEditProposal(model,'e1','periodId','0-4');assert.equal(proposal.unavailable,true);assert.deepEqual(proposal.single,[{lessonId:'e1',periodId:'0-4'}]);assert.deepEqual(proposal.combined,[]);
});
test('weekly teacher proposals respect both inherited and audience-specific candidate restrictions',()=>{
 const pair=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).nested;
 const spare=structuredClone(pair.definition.teachers[0]);spare.id='TE2';pair.definition.teachers.push(spare);pair.definition.audiencePlans[0].candidateTeacherIds.push('TE2');pair.definition.audiencePlans[0].portions[0].teacherPolicy.candidateTeacherIds=['TE'];
 const model=makeAcceptedModel({definition:pair.definition,result:{timetable:{assignments:pair.assignments}}});assert.throws(()=>audienceEditProposal(model,'e1','teacherId','TE2'),/eligible/);
});
