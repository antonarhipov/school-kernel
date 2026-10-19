import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {emptyDraft,draftIssues} from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import {applySchoolDocument} from '../../timetable-workspace/src/main/resources/static/workspace/school-document.js';
import {audienceReadiness} from '../../timetable-workspace/src/main/resources/static/workspace/setup-audience-readiness.js';
import {compileDraft,definitionRevision} from '../../timetable-workspace/src/main/resources/static/workspace/setup-compile.js';
import {createReadiness} from '../../timetable-workspace/src/main/resources/static/workspace/setup-review.js';
const fixture=()=>applySchoolDocument(emptyDraft(),JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/authoring.json')));
test('a retained split with no outstanding group demand requires no joint attendance data',()=>{
 const d=fixture(),c=d.classes[0],p=d.audiencePlans[0];
 const inactive=structuredClone(c.splits[0]);inactive.id='inactive';inactive.groups.forEach(g=>{g.id=`inactive-${g.id}`;});c.splits.push(inactive);
 const zero=structuredClone(p);zero.id='zero-plan';zero.subjectId='zero-subject';zero.splitId=inactive.id;zero.delivery='grouped';zero.wholeClassCount=0;zero.weeklyTotal=0;zero.overrides={};zero.portions=[];
 d.subjects.push({id:zero.subjectId,displayName:'Unused subject'});d.audiencePlans.push(zero);
 assert(!audienceReadiness(d).issues.some(i=>i.relationship));const compiled=compileDraft(d);assert(compiled.cohorts[0].splits.some(s=>s.id==='inactive'));assert.equal(compiled.cohorts[0].relationships,undefined);
});
test('unused incomplete arrangements are warnings and remain authoring-only; a used split becomes blocking',()=>{
 const d=fixture(),c=d.classes[0];c.splits.push({id:'unused',displayName:'Later',subjectIds:[],groupCount:3,groups:[{id:'unfinished',displayName:'',size:''}]});
 const before=structuredClone(d);assert.equal(draftIssues(d).length,0);assert(audienceReadiness(d).warnings.length);const compiled=compileDraft(d);assert.deepEqual(compiled.cohorts[0].splits.map(s=>s.id),['E']);assert.deepEqual(d,before);
 d.audiencePlans[0].splitId='unused';assert(draftIssues(d).some(i=>i.split==='unused'));assert.throws(()=>compileDraft(d));
});
test('actual audience size and eligible weekly teacher availability determine necessary readiness',()=>{
 const d=fixture();d.rooms[0].capacity=14;assert(audienceReadiness(d).issues.some(i=>i.message.includes('no compatible room for 28')));
 d.audiencePlans[0].delivery='grouped';d.audiencePlans[0].wholeClassCount=0;d.audiencePlans[0].portions=d.audiencePlans[0].portions.slice(1);for(const q of d.audiencePlans[0].portions)q.sessionIds=['one'];assert(!audienceReadiness(d).issues.some(i=>i.message.includes('no compatible room')));
 d.teachers.forEach(t=>t.availablePeriodIds=[]);assert(audienceReadiness(d).issues.some(i=>i.message.includes('compatible available teaching periods')));
});
test('readiness marks exactly schema2/catalog11 revision ready and refuses late completion',async()=>{
 let d=fixture(),complete;const r=createReadiness({getDraft:()=>d,build:()=> 'build',request:async(_,opts)=>({value:{build:'build',definitionRevision:await definitionRevision(JSON.parse(opts.body))}}),render(){},flush(){},isDisposed:()=>false});
 const result=await r.check();assert.equal(d.validation,'Ready');assert.equal(result.catalogVersion,11);assert.equal(result.authoringRevision,d.revision);assert.deepEqual(result.definition,compileDraft(d));
 d=fixture();const delayed=createReadiness({getDraft:()=>d,build:()=> 'build',request:(_,opts)=>new Promise(resolve=>{complete=async()=>resolve({value:{build:'build',definitionRevision:await definitionRevision(JSON.parse(opts.body))}});}),render(){},flush(){},isDisposed:()=>false});
 const checking=delayed.check();while(!complete)await new Promise(resolve=>setImmediate(resolve));d.revision++;d.validation='Unchecked';await complete();assert.equal(await checking,null);assert.equal(d.validation,'Unchecked');assert.equal(d.check,undefined);
});
test('nested mixed teaching compilation retains the public relationship shape and exact active unions',()=>{
 const doc=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/generation-authoring.json'));
 const d=applySchoolDocument(emptyDraft(),doc),compiled=compileDraft(d);
 assert.deepEqual(compiled.cohorts[0].relationships,doc.classes[0].relationships);
 assert.equal(compiled.audiencePlans.reduce((n,p)=>n+p.portions.reduce((m,q)=>m+q.sessionIds.length,0),0),8);
});
