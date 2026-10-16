import test from 'node:test';
import assert from 'node:assert/strict';
import { prefix, useKey, maxBytes, syntaxFeedback, validateText, readEnvelope, savedRows, deleteVersions, rememberDefinitionUse, savedVersionInUse } from '../../timetable-workspace/src/main/resources/static/workspace/demo-definitions.js';
const storage = () => ({getItem(key) { return Object.hasOwn(this,key) ? this[key] : null; }, setItem(key,value) {this[key]=value;}, removeItem(key) {delete this[key];}});
const envelope = (id='draft', text=' { "unfinished": true }\r\n') => ({formatVersion:1,identifier:id,savedAt:'2026-10-16T12:00:00.000Z',derivedFrom:{kind:'sample',identifier:'full'},documentText:text});
test('syntax error locations include nested structures, escapes, CRLF, EOF and non-JSON whitespace', () => {
  for (const [text,line,column] of [['{\n "x": [1,] }',2,10], ['{"x":truX}',1,9], ['{\r\n "x": "a\\q"}',2,10], ['{"x":',1,6], ['\u00a0{}',1,1], ['[01]',1,3], ['[1e+]',1,5], ['[-x]',1,3], ['[1.]',1,4]])
    assert.equal(syntaxFeedback(text),`JSON syntax error at line ${line}, column ${column}.`);
  for (const text of ['{}','[true,false,null,{"a":[1.5e-2,"a\\u1234"]}]']) assert.equal(syntaxFeedback(text),'');
});
test('exact five-field envelopes preserve text and enforce complete provenance/format', () => {
  const store=storage(), value=envelope();store.setItem(prefix+'draft',JSON.stringify(value));
  assert.deepEqual(readEnvelope(store,'draft'),value);
  for (const change of [v=>v.formatVersion=2,v=>v.extra=true,v=>v.identifier='other',v=>v.savedAt='2026-02-30T12:00:00.000Z',v=>v.derivedFrom.extra=true,v=>v.derivedFrom.identifier='unknown',v=>v.documentText='[]',v=>v.documentText=' {']) {
    const bad=envelope();change(bad);store.setItem(prefix+'draft',JSON.stringify(bad));assert.throws(()=>readEnvelope(store,'draft'));
  }
});
test('size is inclusive UTF-8 bytes and syntax-valid unfinished objects need no kernel', () => {
  assert.doesNotThrow(()=>validateText('{"unfinished":"é"}'));
  const exact='{"x":"'+'é'.repeat((maxBytes-8)/2)+'"}';assert.equal(new TextEncoder().encode(exact).length,maxBytes);
  assert.doesNotThrow(()=>validateText(exact));assert.throws(()=>validateText(exact+' '),/2 MiB/);
});
test('rows sort newest first, retain corrupt entries and never touch view preferences', () => {
  const store=storage();store.setItem('view','keep');store.setItem(prefix+'draft',JSON.stringify(envelope()));
  const newer=envelope('newer');newer.savedAt='2026-10-16T12:01:00.000Z';store.setItem(prefix+'newer',JSON.stringify(newer));store.setItem(prefix+'corrupt','{');
  assert.deepEqual(savedRows(store).map(row=>row.id),['newer','draft','corrupt']);
  const original=store.removeItem;let calls=0;store.removeItem=function(key){if(++calls===2)throw Error('unavailable');original.call(this,key);};
  assert.throws(()=>deleteVersions(store,['newer','draft']));assert.equal(readEnvelope(store,'newer').documentText,newer.documentText);assert.equal(store.view,'keep');
});
test('in-use binds complete definition, exact saved envelope, source and loaded workspace; imports cannot inherit it', async () => {
  const store=storage(),value=envelope('draft','{"a":[1,2],"b":null}');store.setItem(prefix+'draft',JSON.stringify(value));
  const snap={workspace:{importMode:'INITIAL_DEFINITION',initialDefinition:{b:null,a:[1,2]}}};
  await rememberDefinitionUse(store,'draft',snap);assert.equal(await savedVersionInUse(store,snap),'draft');
  assert.equal(await savedVersionInUse(store,{workspace:{importMode:'INITIAL_ACCEPTANCE',acceptedBaseline:{definition:{a:[1,2],b:null}}}}),'draft');
  assert.equal(await savedVersionInUse(store,{workspace:{importMode:'MANUAL_EDIT_PUBLICATION',acceptedBaseline:{definition:{a:[1,2],b:null}}}}),'draft');
  assert.equal(await savedVersionInUse(store,{workspace:{importMode:'INITIAL_DEFINITION',initialDefinition:{a:[1,2]}}}),null);
  assert.equal(await savedVersionInUse(store,{workspace:{importMode:'ACCEPTED_BASELINE',acceptedBaseline:{definition:{a:[1,2],b:null}}}}),null);
  assert.equal(await savedVersionInUse(store,{workspace:{importMode:'REPAIR_ACCEPTANCE',acceptedBaseline:{definition:{a:[2,1],b:null}}}}),null);
  value.documentText+=' ';store.setItem(prefix+'draft',JSON.stringify(value));assert.equal(await savedVersionInUse(store,snap),null);
  store.removeItem(prefix+'draft');await assert.rejects(()=>savedVersionInUse(store,snap));assert.ok(store.getItem(useKey));
});
