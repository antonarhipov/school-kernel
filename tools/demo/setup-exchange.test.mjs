import test from 'node:test';import assert from 'node:assert/strict';
import { emptyDraft,readableDraft } from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import { mapDefinition,strictParse } from '../../timetable-workspace/src/main/resources/static/workspace/setup-exchange.js';
import { compileDraft,definitionRevision } from '../../timetable-workspace/src/main/resources/static/workspace/setup-compile.js';
const definition=()=>({schemaVersion:1,catalogVersion:9,schoolId:'school',displayName:'Legacy',subjects:[{id:'s',displayName:'Subject',reservedPeriodsAllowed:false}],teachers:[{id:'t',displayName:'Teacher',qualifiedSubjectIds:['s'],availablePeriodIds:['p']}],cohorts:[{id:'c',displayName:'Class',size:20}],rooms:[{id:'r',displayName:'Room',capacity:25,capabilityIds:[]}],periods:[{id:'p',displayName:'First',weekday:'MONDAY',order:5}],lessons:[{id:'l',displayName:'Unique occurrence',teacherId:'t',cohortId:'c',subjectId:'s',preferredRoomIds:[],undesirablePeriodIds:[]}],roomAssignments:[],softConstraintOverrides:[]});
test('conversion retains optional presence, explicit empties, order, unspecified times and ungrouped identity',()=>{
 const source=definition(),before=structuredClone(source),{draft,changes}=mapDefinition(source);assert(changes.some(x=>x.includes('forbidden')));assert.equal(draft.grid.intervals[0].unspecifiedTimes,true);
 assert.deepEqual(draft.teachers[0].eligibleClassIdsBySubject,{s:['c']});assert(draft.plans[0].blockEditsProtected);assert(!Object.hasOwn(draft.plans[0],'seriesId'));
 const out=compileDraft(draft);out.catalogVersion=9;delete out.periods[0].adjacentToNext;assert.deepEqual(out,source);assert.deepEqual(source,before);
});
test('old adoption discloses and preserves effective missing defaults/weights; mixed restrictions remain protected',()=>{
 const source=definition();source.catalogVersion=1;delete source.subjects[0].reservedPeriodsAllowed;delete source.roomAssignments;source.lessons.push({...source.lessons[0],id:'l2',seriesId:'mixed',periodLock:'p'});const {draft,changes}=mapDefinition(source);
 assert.equal(draft.classes[0].maxDailyGaps,2147483647);assert.equal(draft.kernelExtras.softConstraintOverrides.filter(w=>w.weight===0).length,3);assert(changes.some(x=>x.includes('weight zero')));assert(draft.plans[0].protectedReasons.length);
 assert.deepEqual(draft.plans[0].occurrences,source.lessons);
});
test('strict JSON refuses duplicates at any object depth while allowing repeated values',()=>{
 assert.throws(()=>strictParse('{"a":[{"x":1,"x":2}]}'),/Duplicate field x/);assert.throws(()=>strictParse('{"a":1,"a":2}'),/Duplicate/);assert.deepEqual(strictParse('{"a":["x","x"],"b":{"a":2}}'),{a:['x','x'],b:{a:2}});
});
test('definition revision preserves absence and normalizes entity/set order',async()=>{
 const a=definition(),b=structuredClone(a);b.teachers[0].availablePeriodIds=[];assert.notEqual(await definitionRevision(a),await definitionRevision(b));
});

test('recovery retains unfinished scalars but refuses structural corruption before editor rendering',()=>{
 const d=emptyDraft();d.plans=[{id:'p',requiredCount:'unfinished',allocations:[],blockRules:[],occurrences:[]}];assert(readableDraft(d));d.plans[0].allocations={wrong:'shape'};assert(!readableDraft(d));d.plans=[];d.teachers=[{id:'t',qualifiedSubjectIds:{wrong:'shape'}}];assert(!readableDraft(d));
});
