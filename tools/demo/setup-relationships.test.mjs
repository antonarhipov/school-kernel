import test from 'node:test';
import assert from 'node:assert/strict';
import {resolveRelationships,jointDimensions,readableRelationships} from '../../timetable-workspace/src/main/resources/static/workspace/setup-relationships.js';
const split=(id,sizes)=>({id,displayName:id,groups:sizes.map((size,i)=>({id:id+i,displayName:id+i,size}))});
test('normative nested cross-day audiences preserve each constituent and resource headcount',()=>{
 const c={id:'c',size:28,splits:[split('E',[14,14]),split('H',[7,7,7,7])],relationships:{mode:'nested',splitIds:['E','H'],constituentSplitId:'H',unions:{E0:['H0','H1'],E1:['H2','H3']},intersections:[]}};
 const {blocks,issues}=resolveRelationships(c);assert.deepEqual(issues,[]);assert.deepEqual(blocks.map(b=>b.size),[7,7,7,7]);
 assert.deepEqual(blocks.filter(b=>b.groupIds.includes('E0')).map(b=>b.groupIds.find(id=>id.startsWith('H'))),['H0','H1']);
 assert(readableRelationships(c));c.relationships.unions.E1.push('H0');assert(resolveRelationships(c).issues.some(i=>i.message.includes('exactly one')));
});
test('crossing joints and zero intersections reconcile margins without guessing or double-counting',()=>{
 const c={id:'c',size:28,splits:[split('E',[14,14]),split('G',[14,14])],relationships:{mode:'joint',splitIds:['E','G'],unions:{},intersections:jointDimensions([split('E',[14,14]),split('G',[14,14])]).map(groupIds=>({groupIds,count:7}))}};
 let result=resolveRelationships(c);assert.deepEqual(result.issues,[]);assert.equal(result.blocks.length,4);assert.equal(result.blocks.reduce((n,b)=>n+b.size,0),28);
 for(const e of ['E0','E1'])for(const g of ['G0','G1'])assert(result.blocks.some(b=>b.groupIds.includes(e)&&b.groupIds.includes(g)));
 c.relationships.intersections[0].count=14;c.relationships.intersections[1].count=0;c.relationships.intersections[2].count=0;c.relationships.intersections[3].count=14;
 result=resolveRelationships(c);assert.deepEqual(result.issues,[]);assert.equal(result.blocks.length,2);
 c.relationships.intersections[0].count=-1;assert(resolveRelationships(c).issues.length);c.relationships.intersections[0].count='unfinished';assert(resolveRelationships(c).issues.length);
});
test('three splits require joint dimensions and refuse pairwise-only, missing, stale and duplicate declarations',()=>{
 const c={id:'c',size:28,splits:[split('E',[14,14]),split('G',[14,14]),split('H',[14,14])],relationships:{mode:'joint',splitIds:['E','G','H'],unions:{},intersections:[{groupIds:['E0','G0'],count:7}]}};
 assert(resolveRelationships(c).issues.some(i=>i.message.includes('combined intersections')));
 c.relationships.intersections=jointDimensions(c.splits).map((groupIds,i)=>({groupIds,count:i%2?4:3}));
 // G margins equal 14, H differs: no silent redistribution to satisfy inconsistent supplied margins.
 assert(resolveRelationships(c).issues.length);c.relationships.intersections=jointDimensions(c.splits).map(groupIds=>({groupIds,count:3.5}));assert(resolveRelationships(c).issues.length);
 c.relationships.splitIds.push('missing');assert(resolveRelationships(c).issues.some(i=>i.message.includes('missing')));
 assert.equal(jointDimensions(Array.from({length:5},()=>split('s',[1,1,1,1,1,1]))),null);
});
test('imported property-like identities are ordinary audience IDs, and duplicate dimensions are refused',()=>{
 const c={id:'c',size:2,splits:[{id:'larger',groups:[{id:'constructor',size:2,displayName:'A'}]},{id:'base',groups:[{id:'a',size:1},{id:'b',size:1}]}],relationships:{mode:'nested',splitIds:['larger','base'],constituentSplitId:'base',unions:{},intersections:[]}};
 assert(resolveRelationships(c).issues.length);
 c.relationships.unions=JSON.parse('{"constructor":["a","b"]}');assert.deepEqual(resolveRelationships(c).issues,[]);
 c.relationships.splitIds.push('base');assert(resolveRelationships(c).issues.some(i=>i.message.includes('once')));
});
