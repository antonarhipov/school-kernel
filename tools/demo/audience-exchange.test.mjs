import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';
import {mapDefinition} from '../../timetable-workspace/src/main/resources/static/workspace/setup-exchange.js';
import {compileDraft,definitionRevision} from '../../timetable-workspace/src/main/resources/static/workspace/setup-compile.js';
import {schoolDocument,parseSchoolDocument,importSchoolDocument} from '../../timetable-workspace/src/main/resources/static/workspace/school-document.js';
const pairs=JSON.parse(fs.readFileSync('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json'));
test('both bundled schools retain every scheduling value when converted and exported with grouped and unsplit classes',async()=>{
 for(const school of ['full','star-wars']){
  const source=JSON.parse(fs.readFileSync(`timetable-workspace/src/main/resources/demo/timetables/${school}-definition.json`));
  const before=structuredClone(source),{draft}=mapDefinition(source);
  const restored=importSchoolDocument(parseSchoolDocument(JSON.stringify(schoolDocument(draft))));
  for(const candidate of [draft,restored]){
   const out=compileDraft(candidate,{allowIssues:true});
   assert.equal(await definitionRevision(out),await definitionRevision(source),school);
   for(const c of source.cohorts){
    const actual=out.cohorts.find(item=>item.id===c.id);
    assert.equal(Object.hasOwn(actual,'splits'),Object.hasOwn(c,'splits'),`${school}: ${c.id}`);
    assert.deepEqual(actual.splits,c.splits);
   }
  }
  assert.deepEqual(source,before);
 }
});
test('audience kernel to structured school document roundtrip preserves exact policies, empty/absent boundaries and locks',()=>{
 for(const id of ['nested','crossing','groups2','groups3','groups4','gap']){
  const source=structuredClone(pairs[id].definition);source.sessionLocks=[{sessionId:pairs[id].assignments[0].lessonId,periodId:pairs[id].assignments[0].periodId}];delete source.periods[0].gapBoundaryAfter;
  const before=structuredClone(source),{draft,changes}=mapDefinition(source);
  assert(changes.some(c=>c.includes('No automatic')));
  const doc=parseSchoolDocument(JSON.stringify(schoolDocument(draft))),out=compileDraft(importSchoolDocument(doc));
  assert.deepEqual(out,source,id);assert.deepEqual(source,before);
 }
});
test('unmappable raw values are identified before conversion rather than erased',()=>{
 const source=structuredClone(pairs.nested.definition);source.futureSetting={verbatim:'Õ original'};
 assert.throws(()=>mapDefinition(source),/futureSetting.*Unsupported/);assert.deepEqual(source.futureSetting,{verbatim:'Õ original'});
 const inherited=structuredClone(pairs.nested.definition);inherited.basedOnRevision='sha256:ancestry';assert.throws(()=>mapDefinition(inherited),/basedOnRevision.*ancestry/);
});
test('explicit automatic adoption compiles only public scheduling fields while retaining exact fixed source provenance',async()=>{
 const {adoptFixedPlan}=await import('../../timetable-workspace/src/main/resources/static/workspace/setup-audience-teachers.js');
 const original=structuredClone(pairs.groups2.definition),d=mapDefinition(original).draft;delete d.audiencePlans;delete d.importedDefinition;d.kernelExtras.schemaVersion=1;
 d.plans=[{id:'fixed',classId:'c',subjectId:'s',requiredCount:2,allocations:[{teacherId:'T0',count:1},{teacherId:'T1',count:1}],occurrences:[{id:'one',displayName:'Original first Õ',teacherId:'T0'},{id:'two',teacherId:'T1'}],blockRules:[]}];
 const next=adoptFixedPlan(d,d.plans[0]),out=compileDraft(next,{allowIssues:true});assert.equal(out.schemaVersion,2);assert.equal(out.cohorts[0].maxDailyGaps,0);assert.equal(out.audiencePlans[0].sourceFixedPlan,undefined);assert.deepEqual(next.audiencePlans[0].sourceFixedPlan,d.plans[0]);
 assert.deepEqual(out.audiencePlans[0].portions[0].sessionIds,['one','two']);assert.equal(out.audiencePlans[0].portions[0].teacherPolicy.mode,'automatic');
});
