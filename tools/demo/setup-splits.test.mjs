import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyDraft,readableDraft} from '../../timetable-workspace/src/main/resources/static/workspace/setup-model.js';
import {balancedGroups,splitIssues,splitReferences} from '../../timetable-workspace/src/main/resources/static/workspace/setup-splits.js';
import {schoolDocument,parseSchoolDocument,applySchoolDocument} from '../../timetable-workspace/src/main/resources/static/workspace/school-document.js';
test('normative balanced defaults and disclosed limits retain distinct identities',()=>{
  for(const [n,k,sizes] of [[29,2,[15,14]],[28,3,[10,9,9]],[28,4,[7,7,7,7]]]){
    const groups=balancedGroups(n,k);assert.deepEqual(groups.map(g=>g.size),sizes);
    assert.equal(new Set(groups.map(g=>g.id)).size,k);
  }
  for(const args of [[1,2],[28,129],[0,2],[28,2.5],['unfinished',2]])assert.equal(balancedGroups(...args),null);
  assert.equal(balancedGroups(130,128).length,128);
});
test('invalid draft and exact split values survive document round trip without resizing or retargeting',()=>{
  const d=emptyDraft();d.classes=[{id:'c',displayName:'5A',size:28,splits:[{id:'s',displayName:'Õ & <split>',groupCount:3,subjectIds:['english'],groups:balancedGroups(28,3)}]}];d.subjects=[{id:'english',displayName:'English'}];
  const s=d.classes[0].splits[0];s.groups[0].displayName='Full custom label õ ä <script>';s.groups[1].size='unfinished';
  const before=structuredClone(d);assert(splitIssues(d).some(i=>i.message.includes('positive integer')));assert(splitIssues(d).some(i=>i.message.includes('Group total')));
  assert.deepEqual(d,before);const restored=applySchoolDocument(emptyDraft(),parseSchoolDocument(JSON.stringify(schoolDocument(d))));
  assert.deepEqual(restored.classes,d.classes);assert(readableDraft(restored));
  d.plans=[{id:'p',splitId:'s',audienceId:s.groups[0].id}];assert.deepEqual(splitReferences(d,s),['/plans/0/splitId','/plans/0/audienceId']);
});
