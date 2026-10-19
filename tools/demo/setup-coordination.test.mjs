import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyDraft,readableDraft} from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import {coordinationIssues,activityCatalog} from '../../timetable-workspace/src/main/resources/static/workspace/setup-coordination.js';
import {synchronizeAudienceSessions} from '../../timetable-workspace/src/main/resources/static/workspace/setup-audience-plans.js';
import {compileDraft} from '../../timetable-workspace/src/main/resources/static/workspace/setup-compile.js';
function fixture(){const d=emptyDraft();d.name='coordination';d.classes=[{id:'c',displayName:'5A',size:28}];d.subjects=[{id:'s',displayName:'Science'}];d.rooms=[{id:'lab',capacity:28},{id:'other',capacity:28}];d.grid={activeDays:['MONDAY'],intervals:[1,2,3,4].map(n=>({id:'i'+n,day:'MONDAY',kind:'Lesson',label:'P'+n,start:'',end:'',periodId:'p'+n}))};d.audiencePlans=[{id:'plan',classId:'c',subjectId:'s',weeklyTotal:4,wholeClassCount:0,delivery:'whole',overrides:{},portions:[]}];synchronizeAudienceSessions(d,d.audiencePlans[0]);return d;}
test('hard rules survive exact authoring and published compilation without enumerated pairings',()=>{
 const d=fixture(),p=d.audiencePlans[0],q=p.portions[0];p.blockRules=[{id:'double',length:2,count:1,operator:'EXACTLY'}];p.allowedRoomIds=['lab'];q.fixedRoomId='lab';
 d.coordinationRules=[{id:'fixed',classId:'c',type:'FIXED',count:1,activities:[{portionId:q.id,unit:'BLOCK',blockRuleId:'double'}],day:'MONDAY',periodId:'p1'},{id:'gap',classId:'c',type:'GAP_FREE',days:['MONDAY']}];
 assert.deepEqual(coordinationIssues(d),[]);assert(readableDraft(d));const compiled=compileDraft(d,{allowIssues:true});assert.equal(compiled.schemaVersion,2);assert.deepEqual(compiled.coordinationRules,d.coordinationRules);assert.deepEqual(compiled.audiencePlans,d.audiencePlans);assert.equal(activityCatalog(d)[0].blocks,undefined);
});
test('counts, duration, room inheritance and unfinished blocks block without rewriting the draft',()=>{
 const d=fixture(),p=d.audiencePlans[0],q=p.portions[0];p.blockRules=[{id:'b',length:2,count:3,operator:'AT_LEAST'}];p.allowedRoomIds=['lab'];q.allowedRoomIds=['other'];q.fixedRoomId='other';d.coordinationRules=[{id:'r',classId:'c',type:'FIXED',count:2,activities:[{portionId:q.id,unit:'BLOCK',blockRuleId:'b'}],day:'MONDAY',periodId:'p4'}];
 const before=structuredClone(d),messages=coordinationIssues(d).map(i=>i.message).join(' ');for(const text of ['consume 6','empty inherited','outside','complete required block','one occurrence'])assert(messages.includes(text),messages);assert.deepEqual(d,before);
 p.blockRules[0].count='unfinished';assert(readableDraft(d));assert(coordinationIssues(d).some(i=>i.message.includes('positive frequency')));
});
test('explicit empty allowed sets and missing block references remain distinct from omission',()=>{
 const d=fixture(),q=d.audiencePlans[0].portions[0];d.coordinationRules=[{id:'allowed',classId:'c',type:'ALLOWED',count:5,activities:[{portionId:q.id,unit:'SESSION'}],days:[],periodIds:[]}];const issues=coordinationIssues(d);assert(issues.some(i=>i.message.includes('distinct rounds exceeds')));assert.equal(issues.filter(i=>i.message.includes('nonempty')).length,2);
 d.coordinationRules[0].activities[0]={portionId:q.id,unit:'BLOCK',blockRuleId:'gone'};assert(coordinationIssues(d).some(i=>i.message.includes('missing')));d.coordinationRules[0].unexpected='raw';assert(!readableDraft(d));
});
test('singleton fixed starts cannot be excluded by a counted allowed set or fixed twice',()=>{
 const d=fixture(),q=d.audiencePlans[0].portions[0];q.sessionIds=q.sessionIds.slice(0,1);const ref={portionId:q.id,unit:'SESSION'};
 d.coordinationRules=[{id:'one',classId:'c',type:'FIXED',count:1,activities:[ref],day:'MONDAY',periodId:'p1'},{id:'two',classId:'c',type:'FIXED',count:1,activities:[ref],day:'MONDAY',periodId:'p2'},{id:'allowed',classId:'c',type:'ALLOWED',count:1,activities:[ref],periodIds:['p3']}];
 const before=structuredClone(d),messages=coordinationIssues(d).map(i=>i.message).join(' ');assert(messages.includes('contradictory slots'));assert(messages.includes('excludes a fixed'));assert.deepEqual(d,before);
});
