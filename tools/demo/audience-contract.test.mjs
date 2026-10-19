import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import {execFileSync} from 'node:child_process';
import {mapDefinition} from '../../timetable-workspace/src/main/resources/static/workspace/setup-exchange.js';
import {compileDraft} from '../../timetable-workspace/src/main/resources/static/workspace/setup-compile.js';
import {adoptFixedPlan} from '../../timetable-workspace/src/main/resources/static/workspace/setup-audience-teachers.js';
const output=`output/kernel/student-groups-and-coordinated-lessons/UC-15/${Date.now()}`;fs.mkdirSync(output,{recursive:true});
const run=(command,d,candidate,tag,exit=0)=>{
 const path=`${output}/${tag}`;fs.mkdirSync(path);const def=`${path}/definition.json`,input=`${path}/candidate.json`,out=`${path}/output.json`;
 fs.writeFileSync(def,JSON.stringify(d));fs.writeFileSync(input,JSON.stringify(candidate));const before=[fs.readFileSync(def),fs.readFileSync(input)];
 let actual=0;try{execFileSync('java',['-jar','kernel-cli/target/school-kernel.jar',command,'--definition',def,...(command==='review'?['--assignments',input]:command==='verify'?['--result',input]:['--step-limit','10000']), '--output',out],{stdio:'pipe'});}catch(e){actual=e.status;fs.writeFileSync(`${path}/stderr.txt`,e.stderr??'');}
 assert.equal(actual,exit,fs.existsSync(out)?fs.readFileSync(out,'utf8'):'missing output');assert.deepEqual(fs.readFileSync(def),before[0]);assert.deepEqual(fs.readFileSync(input),before[1]);
 fs.appendFileSync(`${output}/ledger.md`,`- PASS ${tag}: published ${command} exit${exit}; exact input bytes unchanged; [output](${tag}/output.json).\n`);
 return JSON.parse(fs.readFileSync(out));
};
fs.writeFileSync(`${output}/ledger.md`,`# UC-15 public compatibility and adoption\n\n${new Date().toISOString()}\nPackaged public CLI; exact definitions, source/candidate/results retained. Working diff based on3cb66f3. No operator data or database.\n\n`);
test('published legacy pair retains differing fixed teachers and hard limits; unsupported feature versions refuse',()=>{
 const base=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).groups2;
 const d={schemaVersion:1,catalogVersion:10,schoolId:'legacy-transfer',displayName:'Legacy fixed Õ',subjects:base.definition.subjects,teachers:base.definition.teachers.map(({eligibleClassIdsBySubject,referenceWeeklyMaximum,...t})=>t),cohorts:[{id:'c',displayName:'5A',size:28,maxDailyGaps:0}],rooms:base.definition.rooms,periods:base.definition.periods.map(({gapBoundaryAfter,...p})=>p),lessons:[{id:'one',displayName:'First Õ session',subjectId:'s',cohortId:'c',teacherId:'T0',seriesId:'old'},{id:'two',displayName:'Second session',subjectId:'s',cohortId:'c',teacherId:'T1',seriesId:'old'}]};
 const a=d.lessons.map((l,i)=>({lessonId:l.id,subjectId:'s',cohortId:'c',teacherId:l.teacherId,periodId:`${i}-1`,roomId:'R0'}));
 const review=run('review',d,a,'legacy-review');assert(review.valid);assert.deepEqual(review.result.timetable.assignments,a);run('verify',d,review.result,'legacy-exact-pair');
 const {draft}=mapDefinition(d),compiled=compileDraft(draft);assert.deepEqual(compiled.lessons,d.lessons);assert.equal(compiled.cohorts[0].maxDailyGaps,0);assert.equal(compiled.schemaVersion,1);
 const round=run('review',compiled,a,'legacy-roundtrip');assert(round.valid);assert.deepEqual(round.result.score,review.result.score);run('verify',compiled,round.result,'legacy-roundtrip-pair');
 const grouped=structuredClone(base.definition);grouped.schemaVersion=1;assert.equal(run('review',grouped,base.assignments,'grouped-as-legacy',2).status,'INVALID_INPUT');
});
test('faithful grouped roundtrip preserves certificate/teachers/quality; explicit adoption becomes executable automatic allocation',()=>{
 const base=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).nested;
 const checked=run('review',base.definition,base.assignments,'audience-source');assert(checked.valid);
 const mapped=compileDraft(mapDefinition(base.definition).draft);assert.deepEqual(mapped,base.definition);const verified=run('verify',mapped,checked.result,'audience-roundtrip');assert.equal(verified.status,'VERIFIED');assert.equal(verified.assertedQualityMismatch,false);
 const small=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json')).groups2;
 const d=mapDefinition(small.definition).draft;delete d.audiencePlans;d.kernelExtras.schemaVersion=1;d.classes[0].splits=[];d.grid.activeDays=['MONDAY','TUESDAY'];d.grid.intervals=d.grid.intervals.filter(i=>d.grid.activeDays.includes(i.day));
 d.plans=[{id:'fixed-plan',classId:'c',subjectId:'s',requiredCount:2,allocations:[{teacherId:'T0',count:1},{teacherId:'T1',count:1}],occurrences:[{id:'one',teacherId:'T0',displayName:'Original Õ'},{id:'two',teacherId:'T1'}],blockRules:[]}];
 const original=structuredClone(d),adopted=adoptFixedPlan(d,d.plans[0]),definition=compileDraft(adopted,{allowIssues:true});assert.deepEqual(d,original);
 const plan=run('plan',definition,{},'explicit-automatic-adoption');assert.equal(plan.status,'FEASIBLE');assert.equal(plan.weeklyTeacherAllocations.length,1);assert.equal(plan.weeklyTeacherAllocations[0].mode,'automatic');assert.equal(new Set(plan.timetable.assignments.map(a=>a.teacherId)).size,1);assert.equal(definition.cohorts[0].maxDailyGaps,0);run('verify',definition,plan,'adopted-pair');
});
test.after(()=>fs.appendFileSync(`${output}/ledger.md`,'\nAll input files retained; processes completed. See the test process result for the overall verdict.\n'));
