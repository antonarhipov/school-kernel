import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { emptyDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import { schoolDocument, parseSchoolDocument, applySchoolDocument, locatedDraftIssues, structureIssues, schoolDocumentPath, locateKernelFeedback } from '../../timetable-workspace/src/main/resources/static/workspace/school-document.js';
import { mapDefinition } from '../../timetable-workspace/src/main/resources/static/workspace/setup-exchange.js';
import { compileDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-compile.js';
const sample = name => JSON.parse(fs.readFileSync(new URL(`../../timetable-workspace/src/main/resources/demo/timetables/${name}-definition.json`,import.meta.url)));
test('complete document preserves full authoring intent and optional presence through apply',()=>{
 const d=mapDefinition(sample('english')).draft;
 d.grid.intervals.push({id:'break',day:'MONDAY',kind:'Break',label:'Named pause',start:'18:00',end:'18:10'});
 d.teachers[0].eligibleClassIdsBySubject[d.subjects[0].id]=d.classes.map(c=>c.id);
 d.classes[0].lunchPeriodIds=[];
 d.plans.push({id:'unfinished',classId:d.classes[0].id,subjectId:d.subjects[0].id,requiredCount:'unfinished',allocations:[],occurrences:[],blockRules:[]});
 const doc=schoolDocument(d), parsed=parseSchoolDocument(JSON.stringify(doc)), next=applySchoolDocument(d,parsed);
 assert.deepEqual(schoolDocument(next),doc);
 assert.equal(next.id,d.id);assert.deepEqual(next.source,d.source);assert(!next.check);
 assert.equal(next.plans.at(-1).requiredCount,'unfinished');
 assert(!Object.hasOwn(doc,'id'));assert(!Object.hasOwn(doc,'check'));assert(!Object.hasOwn(doc,'focused'));
});
test('all bundled definitions round trip without changing compiled behavior',()=>{
 for(const name of ['english','full','star-wars','two-class']){
  const source=sample(name),d=mapDefinition(source).draft,doc=schoolDocument(d);
  assert.deepEqual(structureIssues(doc),[],name);
  assert.deepEqual(compileDraft(applySchoolDocument(d,parseSchoolDocument(JSON.stringify(doc)))) ,compileDraft(d),name);
 }
});
test('precise structural refusals preserve original and do not accept forged workflow metadata',()=>{
 const d=emptyDraft(),before=structuredClone(d),doc=schoolDocument(d);
 assert.throws(()=>parseSchoolDocument('{'),/line 1, column/);
 assert.throws(()=>parseSchoolDocument('{"name":"a","name":"b"}'),/Duplicate field name/);
 assert.throws(()=>parseSchoolDocument(JSON.stringify({...doc,check:{build:'forged'}})),/\/check: Unsupported/);
 assert.throws(()=>parseSchoolDocument(JSON.stringify({...doc,teachers:[{id:'t',qualifiedSubjectIds:{}}]})),/\/teachers\/0\/qualifiedSubjectIds: Expected array/);
 assert.throws(()=>parseSchoolDocument(JSON.stringify({...doc,grid:{activeDays:[],intervals:[{id:'i',day:'MONDAY',kind:'oops',label:'',start:'',end:''}]}})),/\/grid\/intervals\/0\/kind/);
 assert.deepEqual(d,before);
});
test('semantic inconsistency remains draft data with actionable paths and blocks readiness',()=>{
 const d=emptyDraft(),doc=schoolDocument(d);doc.teachers=[{id:'t',displayName:'T',qualifiedSubjectIds:['missing'],eligibleClassIdsBySubject:{missing:['class']}}];
 const next=applySchoolDocument(d,parseSchoolDocument(JSON.stringify(doc))),issues=locatedDraftIssues(next);
 assert(issues.some(i=>i.path==='/teachers/0/qualifiedSubjectIds/0'));
 assert(issues.some(i=>i.path==='/teachers/0/eligibleClassIdsBySubject/missing/0'));
 assert.throws(()=>compileDraft(next),/Choose at least one/);
});

test('generated solver fields have a single editable source and unsupported prototype names are rejected',()=>{
 const d=mapDefinition(sample('full')).draft,doc=schoolDocument(d);
 assert(!Object.hasOwn(doc.kernelExtras??{},'reservedPeriodIds'));
 assert(!Object.hasOwn(doc.kernelExtras??{},'schoolId'));
 assert(doc.plans.every(p=>p.occurrences.every(o=>!Object.hasOwn(o,'cohortId')&&!Object.hasOwn(o,'subjectId'))));
 assert.throws(()=>parseSchoolDocument(JSON.stringify({...doc,kernelExtras:{reservedPeriodIds:[]}})),/\/kernelExtras\/reservedPeriodIds: Unsupported/);
 assert.throws(()=>parseSchoolDocument(JSON.stringify({...schoolDocument(emptyDraft()),constructor:'unsupported'})),/\/constructor: Unsupported/);
 const raw=JSON.stringify(schoolDocument(emptyDraft())).replace('"name":','"__proto__":{},"name":');
 assert.throws(()=>parseSchoolDocument(raw),/\/__proto__: Unsupported/);
});

test('valid subject identifiers that match object property names remain ordinary identifiers',()=>{
 const source=sample('two-class'),old=source.subjects[0].id;
 source.subjects[0].id='constructor';
 for(const t of source.teachers)t.qualifiedSubjectIds=t.qualifiedSubjectIds.map(id=>id===old?'constructor':id);
 for(const l of source.lessons)if(l.subjectId===old)l.subjectId='constructor';
 for(const r of source.roomAssignments??[])if(r.subjectId===old)r.subjectId='constructor';
 const d=mapDefinition(source).draft,doc=schoolDocument(d);
 assert.deepEqual(structureIssues(doc),[]);
 assert.doesNotThrow(()=>locatedDraftIssues(applySchoolDocument(d,doc)));
 const teacher=doc.teachers.find(t=>t.eligibleClassIdsBySubject.constructor?.length);
 delete teacher.eligibleClassIdsBySubject.constructor;
 assert.doesNotThrow(()=>locatedDraftIssues(applySchoolDocument(d,doc)));
 assert(locatedDraftIssues(applySchoolDocument(d,doc)).some(i=>i.message.includes('eligibility')));
});

test('authoritative solver validation paths map to the editable complete document',()=>{
 const d=mapDefinition(sample('two-class')).draft,definition=compileDraft(d);
 const lesson=definition.lessons[0],pi=d.plans.findIndex(p=>p.occurrences.some(o=>o.id===lesson.id)),oi=d.plans[pi].occurrences.findIndex(o=>o.id===lesson.id);
 assert.equal(schoolDocumentPath(d,definition,'/lessons/0/teacherId'),`/plans/${pi}/occurrences/${oi}/teacherId`);
 assert.equal(schoolDocumentPath(d,definition,'/lessons/0/cohortId'),`/plans/${pi}/classId`);
 assert.equal(schoolDocumentPath(d,definition,'/cohorts/0/size'),'/classes/0/size');
 assert.equal(locateKernelFeedback(d,definition,'/softConstraintOverrides/0/weight: invalid'),'/kernelExtras/softConstraintOverrides/0/weight: invalid');
 const n=d.grid.intervals.findIndex(i=>i.periodId===definition.periods[0].id);
 assert.equal(schoolDocumentPath(d,definition,'/periods/0/startTime'),`/grid/intervals/${n}/start`);
 d.plans[pi].blockRules=[{id:'block',length:2,operator:'EXACTLY',count:1}];definition.consecutiveBlocks=[{id:'block'}];
 assert.equal(schoolDocumentPath(d,definition,'/consecutiveBlocks/0/weeklyCount'),`/plans/${pi}/blockRules/0/count`);
});

test('import adoption refuses stale previews and failed persistence without replacing working data',async()=>{
 const {createExchange}=await import('../../timetable-workspace/src/main/resources/static/workspace/setup-exchange.js');
 let draft=emptyDraft(),confirmAction,activated=0,canFlush=true,notice='';
 const exchange=createExchange({getDraft:()=>draft,activate(){activated++;},flush:()=>canFlush,notify:text=>{notice=text;},confirm(title,text,action){confirmAction=action;},request(){throw new Error('Complete documents require no solver request.');},build:()=>'',isDisposed:()=>false});
 const text=JSON.stringify(schoolDocument(draft));
 await exchange.preview(text,{id:'file'});draft.revision++;confirmAction();
 assert.equal(activated,0);assert.match(notice,/changed during import preview/);
 await exchange.preview(text,{id:'file'});canFlush=false;confirmAction();assert.equal(activated,0);
 canFlush=true;confirmAction();assert.equal(activated,1);
});
