import test from 'node:test';
import assert from 'node:assert/strict';
import { available,avoided,markAvailability,availabilityCapacityIssues } from '../../timetable-workspace/src/main/resources/static/workspace/setup-availability.js';
test('explicit markings distinguish hard/soft facts and retain whitelist including explicit empty intent',()=>{
  const t={id:'t',availablePeriodIds:[],undesirablePeriodIds:['a']},before=structuredClone(t);
  assert(!available(t,'a'));assert(avoided(t,'a'));
  const prefer=markAvailability(t,['a'],'Prefer to avoid');assert(available(prefer,'a'));assert(avoided(prefer,'a'));assert(!available(prefer,'new'));
  const clear=markAvailability(prefer,['a'],'Available');assert.deepEqual(clear.availablePeriodIds,['a']);assert.deepEqual(clear.undesirablePeriodIds,[]);
  const block=markAvailability(clear,['a'],'Unavailable');assert.deepEqual(block.availablePeriodIds,[]);assert.equal(block.availability.intent,'whitelist');assert(!available(block,'new'));assert.deepEqual(t,before);
  const all=markAvailability({id:'all'},['a'],'Unavailable');assert(!available(all,'a'));assert(available(all,'new'));assert(!Object.hasOwn(all,'undesirablePeriodIds'));
});
test('capacity errors retain explicit allocations and disregard non-teaching intervals',()=>{
  const draft={grid:{intervals:[{kind:'Lesson',periodId:'a'},{kind:'Reserved lesson',periodId:'b'},{kind:'Break'},{kind:'Lunch'}]},plans:[{allocations:[{teacherId:'t',count:2}]}]};
  const issues=availabilityCapacityIssues(draft,{id:'t',displayName:'Teacher',availablePeriodIds:['a']});assert.equal(issues.length,1);assert(issues[0].message.includes('2 allocated lessons exceed 1'));assert.equal(draft.plans[0].allocations[0].count,2);
});
