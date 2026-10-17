import test from 'node:test';
import assert from 'node:assert/strict';
import { blockConsumption,consecutiveWindows,blockIssues } from '../../timetable-workspace/src/main/resources/static/workspace/setup-blocks.js';
const fixture=()=>({grid:{activeDays:['MONDAY'],intervals:[{id:'a',periodId:'a',day:'MONDAY',kind:'Lesson',label:'A',start:'08:00',end:'08:45'},{id:'break',day:'MONDAY',kind:'Break',label:'Break',start:'08:45',end:'09:00'},{id:'b',periodId:'b',day:'MONDAY',kind:'Lesson',label:'B',start:'09:00',end:'09:45'}]},subjects:[{id:'s'}],classes:[{id:'c',size:20}],teachers:[{id:'t'}],rooms:[{id:'r',capacity:25,capabilityIds:[]}],plans:[{id:'p',classId:'c',subjectId:'s',requiredCount:2,allocations:[{teacherId:'t',count:2}],blockRules:[{id:'rule',length:2,count:1,operator:'EXACTLY'}],occurrences:[]}]});
test('ordinary breaks preserve whole-grid adjacency; lunch, unavailability and disjoint rooms remove common block windows',()=>{
  const d=fixture();assert.equal(consecutiveWindows(d,2).length,1);assert.deepEqual(blockIssues(d),[]);
  d.grid.intervals[1].kind='Lunch';assert.equal(consecutiveWindows(d,2).length,0);assert(blockIssues(d).some(i=>i.message.includes('consecutive window')));
  d.grid.intervals[1].kind='Break';d.classes[0].lunchPeriodIds=['b'];assert(blockIssues(d).some(i=>i.message.includes('consecutive window')));delete d.classes[0].lunchPeriodIds;
  d.rooms=[{id:'one',capacity:25,capabilityIds:[],availablePeriodIds:['a']},{id:'two',capacity:25,capabilityIds:[],availablePeriodIds:['b']}];assert(blockIssues(d).some(i=>i.message.includes('common teacher and room')));
});
test('block length and frequency consumption stay independent; split allocations cannot supply a whole block',()=>{
  const d=fixture(),p=d.plans[0];p.requiredCount=5;p.blockRules=[{id:'double',length:2,count:1,operator:'EXACTLY'},{id:'triple',length:3,count:1,operator:'AT_LEAST'}];assert.equal(blockConsumption(p),5);
  p.blockRules[1].count=2;assert.equal(blockConsumption(p),8);assert(blockIssues(d).some(i=>i.message.includes('exceeds weekly demand 5')));
  p.blockRules[1].length=2;assert(blockIssues(d).some(i=>i.message.includes('duplicate length 2')));
  p.blockRules=[{id:'triple',length:3,count:1,operator:'EXACTLY'}];p.allocations=[{teacherId:'t',count:2},{teacherId:'other',count:2}];assert(blockIssues(d).some(i=>i.message.includes('allocations cannot supply')));
});
