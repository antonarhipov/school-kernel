import test from 'node:test';
import assert from 'node:assert/strict';
import { suitableRooms,roomIssues } from '../../timetable-workspace/src/main/resources/static/workspace/setup-rooms.js';
test('room suitability intersects capacity/capabilities/policies/fixed rooms/availability and keeps ordinary home room soft',()=>{
  const d={classes:[{id:'c',size:25,homeRoomId:'home'}],subjects:[{id:'s'}],teachers:[{id:'t'}],grid:{intervals:[{kind:'Lesson',periodId:'p'}]},rooms:[{id:'home',displayName:'Home',capacity:30,capabilityIds:['lab']},{id:'other',displayName:'Other',capacity:30,capabilityIds:['lab']},{id:'small',capacity:20,capabilityIds:['lab']}],plans:[],roomAssignments:[]};
  const p={id:'row',classId:'c',subjectId:'s',allocations:[{teacherId:'t',count:1}],occurrences:[{teacherId:'t',requiredRoomCapabilityIds:['lab']}]};d.plans=[p];
  assert.deepEqual(suitableRooms(d,p,'t',p.occurrences[0]).map(r=>r.id),['home','other']);
  d.roomAssignments=[{id:'subject',subjectId:'s',allowedRoomIds:['other']},{id:'teacher',subjectId:'s',teacherId:'t',allowedRoomIds:['home']}];
  assert.deepEqual(suitableRooms(d,p,'t'),[]);assert(roomIssues(d).some(i=>i.plan==='row'&&i.message.includes('subject, teacher')));
  d.roomAssignments.pop();p.fixedRoomId='home';assert.deepEqual(suitableRooms(d,p,'t'),[]);
  p.fixedRoomId='other';assert.equal(suitableRooms(d,p,'t')[0].id,'other');d.rooms[1].availablePeriodIds=[];assert.deepEqual(suitableRooms(d,p,'t'),[]);
});

test('reserved capacity is never advertised as usable for ordinary-subject allocations',()=>{
 const d={grid:{activeDays:['MONDAY'],intervals:[{periodId:'p',kind:'Lesson'},{periodId:'rp',kind:'Reserved lesson'}]},subjects:[{id:'s'}],classes:[{id:'c',size:20}],teachers:[{id:'t'}],rooms:[{id:'r',displayName:'Room',capacity:25,capabilityIds:[]}],plans:[{id:'plan',classId:'c',subjectId:'s',allocations:[{teacherId:'t',count:2}],occurrences:[]}]};
 assert(roomIssues(d).some(i=>i.message.includes('2 allocated lessons exceed 1 compatible teaching periods')));d.subjects[0].reservedPeriodsAllowed=true;assert(!roomIssues(d).some(i=>i.message.includes('exceed')));
});
