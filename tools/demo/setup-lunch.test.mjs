import test from 'node:test';
import assert from 'node:assert/strict';
import { markLunch,lunchIssues } from '../../timetable-workspace/src/main/resources/static/workspace/setup-lunch.js';
import { available } from '../../timetable-workspace/src/main/resources/static/workspace/setup-availability.js';
test('lunch reasons can be removed independently without relaxing retained class availability',()=>{
  const c={id:'c',displayName:'Class',availablePeriodIds:['b'],undesirablePeriodIds:['a']},before=structuredClone(c);
  const lunch=markLunch(c,'a',true);assert(!available(lunch,'a'));assert.deepEqual(lunch.availablePeriodIds,['b']);
  const clear=markLunch(lunch,'a',false);assert(!available(clear,'a'));assert.deepEqual(clear.lunchPeriodIds,[]);assert.deepEqual(clear.undesirablePeriodIds,['a']);assert.deepEqual(c,before);
  const free=markLunch({id:'other'},'a',true);assert(!available(free,'a'));assert(available(markLunch(free,'a',false),'a'));
  const d={classes:[lunch],plans:[{classId:'c',requiredCount:2}],grid:{intervals:[{kind:'Lesson',periodId:'a'},{kind:'Lesson',periodId:'b'},{kind:'Lunch'}]}};
  assert(lunchIssues(d).some(i=>i.message.includes('2 lessons exceed 1')));
});
