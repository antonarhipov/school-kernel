import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyDraft,readableDraft} from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import {audienceDemand,synchronizeAudienceSessions,audiencePlanIssues} from '../../timetable-workspace/src/main/resources/static/workspace/setup-audience-plans.js';
import {schoolDocument,parseSchoolDocument,applySchoolDocument} from '../../timetable-workspace/src/main/resources/static/workspace/school-document.js';
const fixture=()=>{const d=emptyDraft();d.classes=[{id:'c',size:28,splits:[{id:'E',displayName:'English',groupCount:2,subjectIds:[],groups:[{id:'E1',displayName:'E1',size:14},{id:'E2',displayName:'E2',size:14}]}]}];d.subjects=[{id:'s'}];d.audiencePlans=[{id:'p',classId:'c',subjectId:'s',weeklyTotal:3,delivery:'mixed',splitId:'E',wholeClassCount:2,overrides:{},portions:[]}];return d;};
test('mixed-demand example counts four actual sessions and three received periods, with stable allocations',()=>{
 const d=fixture(),p=d.audiencePlans[0];synchronizeAudienceSessions(d,p);const before=structuredClone(p.portions);
 assert.equal(audienceDemand(d,p).sessions,4);assert.equal(audienceDemand(d,p).received,84);assert.deepEqual(p.portions.map(p=>p.sessionIds.length),[2,1,1]);
 p.overrides.E2=4;synchronizeAudienceSessions(d,p);assert.deepEqual(p.portions.slice(0,2),before.slice(0,2));assert.equal(p.portions[2].sessionIds[0],before[2].sessionIds[0]);assert.equal(p.portions[2].sessionIds.length,2);assert.equal(audienceDemand(d,p).sessions,5);
 delete p.overrides.E2;synchronizeAudienceSessions(d,p);assert.equal(p.portions[2].sessionIds.length,2);assert(audiencePlanIssues(d).some(i=>i.audiencePlan==='p'));
 synchronizeAudienceSessions(d,p,{reduce:true});assert.deepEqual(p.portions,before);assert.deepEqual(audiencePlanIssues(d),[]);
});
test('inheritance, optional overrides, invalid contribution and exact unfinished draft survive exchange',()=>{
 const d=fixture(),p=d.audiencePlans[0];p.overrides.E2=4;p.weeklyTotal=5;assert.deepEqual(audienceDemand(d,p).rows.map(r=>r.total),[5,4]);
 p.wholeClassCount=6;assert.equal(audienceDemand(d,p).issues.length,2);p.wholeClassCount='unfinished';assert(audienceDemand(d,p).issues.length);
 const restored=applySchoolDocument(emptyDraft(),parseSchoolDocument(JSON.stringify(schoolDocument(d))));assert.deepEqual(restored.audiencePlans,d.audiencePlans);assert(readableDraft(restored));
 p.wholeClassCount=3;p.weeklyTotal=3;p.overrides={};synchronizeAudienceSessions(d,p);assert.equal(p.portions.length,1);assert.equal(p.portions[0].sessionIds.length,3);
});
