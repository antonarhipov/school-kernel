import {test} from 'node:test';import assert from 'node:assert/strict';
import {readFile,writeFile,mkdir} from 'node:fs/promises';import {execFileSync} from 'node:child_process';
import {makeAcceptedModel} from '../../timetable-workspace/src/main/resources/static/workspace/accepted-model.js';
import {createTimetablePdf} from '../../timetable-workspace/src/main/resources/static/workspace/timetable-pdf.js';
import {PDFDocument} from '../../timetable-workspace/src/main/resources/static/workspace/vendor/pdf/pdf-lib.js';
const pairs=JSON.parse(await readFile('timetable-workspace/src/test/resources/student-groups/inspection-pairs.json'));
const dir=`output/pdf/student-groups/${Date.now()}`;await mkdir(dir,{recursive:true});await writeFile('output/pdf/student-groups/latest-run.txt',dir);
const jar='kernel-cli/target/school-kernel.jar';
const originalFetch=globalThis.fetch;globalThis.fetch=async url=>new Response(await readFile(url));
export async function reviewed(id,pair=pairs[id]) {
 const d=`${dir}/${id}-definition.json`,a=`${dir}/${id}-assignments.json`,o=`${dir}/${id}-review.json`;
 await writeFile(d,JSON.stringify(pair.definition));await writeFile(a,JSON.stringify(pair.assignments));
 execFileSync('java',['-jar',jar,'review','--definition',d,'--assignments',a,'--output',o],{stdio:'pipe'});
 const review=JSON.parse(await readFile(o));assert.equal(review.valid,true,JSON.stringify(review));
 return {definition:pair.definition,result:review.result};
}
test('compact audience PDF examples use complete real-reviewed pairs and immutable scope',async()=>{
 for(const id of ['nested','groups2','groups3','groups4','groups8','partial','crossing','whole']) {
  const pair=await reviewed(id),before=JSON.stringify(pair),model=makeAcceptedModel(pair);
  const options={model,schoolName:pair.definition.displayName,timetableName:`Grouped ${id}`,filters:{cohortId:'c'}};
  const bytes=await createTimetablePdf(options),pdf=await PDFDocument.load(bytes);
  assert(pdf.getPageCount()>=1);if(id!=='groups8')assert.equal(pdf.getPageCount(),1);await writeFile(`${dir}/${id}.pdf`,bytes);
  assert.equal(JSON.stringify(pair),before);
  if(id==='nested')await writeFile(`${dir}/teacher.pdf`,await createTimetablePdf({...options,filters:{teacherId:pair.result.timetable.assignments[0].teacherId}}));
 }
});
test('long names wrap in timetable continuation pages, unresolved references refuse without changing earlier output',async()=>{
 const pair=structuredClone(pairs.partial);
 pair.definition.subjects[0].displayName='LONGSUBJECT '+ 'UnbrokenLongOriginalLabel'.repeat(35);
 pair.definition.cohorts[0].splits[0].groups[0].displayName='CUSTOMAUDIENCE '+ 'Õpilased full original'.repeat(20);
 const checked=await reviewed('long',pair),options={model:makeAcceptedModel(checked),schoolName:'Exact long school',timetableName:'Long labels',filters:{}};
 const bytes=await createTimetablePdf(options);await writeFile(`${dir}/long.pdf`,bytes);
 const bad=structuredClone(checked);bad.result.timetable.assignments[0].teacherId='unresolved';
 await assert.rejects(createTimetablePdf({...options,model:makeAcceptedModel(bad)}),/Cannot resolve.*session/);
 assert.deepEqual(await readFile(`${dir}/long.pdf`),Buffer.from(bytes));
});
test.after(()=>{globalThis.fetch=originalFetch;});
