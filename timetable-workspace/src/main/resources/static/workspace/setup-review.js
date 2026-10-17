import { draftIssues } from './setup-model.js';
import { compileDraft, definitionRevision } from './setup-compile.js';
import { teaching } from './setup-grid.js';
import { available } from './setup-availability.js';
import { nameOf, workload } from './setup-teachers.js';
import { allocated } from './setup-plans.js';
export function reviewContent(draft,html){
 const periods=draft.grid.intervals.filter(teaching),required=draft.plans.reduce((n,p)=>n+(Number.isInteger(p.requiredCount)?p.requiredCount:0),0),assigned=draft.plans.reduce((n,p)=>n+allocated(p),0);
 const issues=[...draftIssues(draft),...(draft.checkIssues??[])];
 return `<p id="setup-readiness" role="status">${html(draft.validation)}. Readiness checks necessary conditions; only generation can find a complete timetable.</p>
 <button id="setup-check">Check current setup</button><p id="setup-totals">Required: ${required} · Allocated: ${assigned} · Unassigned: ${Math.max(0,required-assigned)} · Over-allocated: ${Math.max(0,assigned-required)}</p>
 <h3>Class demand and teaching capacity</h3>${draft.classes.map(c=>{const plans=draft.plans.filter(p=>(p.classId??p.cohortId)===c.id),demand=plans.reduce((n,p)=>n+(Number.isInteger(p.requiredCount)?p.requiredCount:0),0);return `<p>${html(nameOf(c))}: ${demand} lessons · Regular capacity ${periods.filter(p=>p.kind==='Lesson'&&available(c,p.periodId)).length} · Reserved capacity ${periods.filter(p=>p.kind==='Reserved lesson'&&available(c,p.periodId)).length} (only permitted subjects)</p>`;}).join('')}
 <h3>Teacher loads</h3>${draft.teachers.map(t=>`<p>${html(nameOf(t))}: ${workload(draft,t.id)} allocated individual lessons</p>`).join('')}
 <h3>Blocking issues</h3><div id="setup-review-issues">${issues.map((i,n)=>`<p><button data-review-issue="${n}">${html(i.section)}: ${html(i.message)}</button></p>`).join('')||'<p>No known blocking issue.</p>'}</div>
 <details class="setup-review-rules"><summary>Scheduling rules, preferences and demo limits</summary><h3>Hard requirements</h3><p>Availability and class lunch; room capacity, capabilities, policies and fixed rooms; eligibility, qualifications and curator rules; reserved permissions and maxima; daily maxima, gap/start/load bounds; undeclared consecutive runs forbidden; declared exact lengths, common teacher/room and weekly frequencies.</p>
 <h3>Soft preferences</h3><p>Teacher/class gaps, preferred start, daily balance, undesirable periods, home/preferred room and spreading. Mandatory valid blocks receive only their required spreading exemption. Soft cost never makes a hard-invalid arrangement valid.</p>
 <p>Limit: 2 MiB UTF-8; configured lesson and placement-work limits apply. Spare teaching capacity is allowed. Readiness is not proof of feasibility.</p></details>`;
}
export function createReadiness({getDraft,build,request,render,flush,isDisposed}){
 let active;
 async function check(){
  const draft=getDraft(),token={id:draft.id,revision:draft.revision,build:build(),data:JSON.stringify(draft)};active=token;
  draft.validation='Checking';delete draft.check;delete draft.checkIssues;render();
  const current=()=>!isDisposed()&&active===token&&getDraft()?.id===token.id&&getDraft().revision===token.revision&&build()===token.build;
  try{
   const issues=draftIssues(draft);
   if(issues.length){if(current()){draft.validation='Blocked';render();flush();}return null;}
   const definition=compileDraft(draft),exact=JSON.stringify(definition),expected=await definitionRevision(definition);
   if(!current())return null;
   const {value}=await request('/api/demo/definition/check',{method:'POST',body:exact,headers:{'Content-Type':'text/plain'}});
   if(!current())return null;
   if(value.build!==token.build||value.definitionRevision!==expected)throw new Error('Definition/build provenance did not match this exact setup. Review again.');
   draft.validation='Ready';draft.check={authoringRevision:token.revision,definitionRevision:expected,build:value.build,catalogVersion:10,definition,checkedAt:new Date().toISOString()};render();flush();return draft.check;
  }catch(error){if(current()){draft.validation=[400,413,422].includes(error.status)?'Blocked':'Check failed';
    const located=/\/(subjects|teachers|cohorts|rooms|periods|lessons)\/(\d+)/.exec(error.message),section=located?({subjects:'Subjects',teachers:'Teachers',cohorts:'Classes',rooms:'Rooms',periods:'Timetable grid',lessons:'Classes'})[located[1]]:'Review';
    let source;try{source=located?compileDraft(draft,{allowIssues:true})[located[1]]?.[Number(located[2])]:null;}catch{}
    const record=located?.[1]==='periods'?draft.grid.intervals.find(i=>i.periodId===source?.id)?.id:located?.[1]==='lessons'?source?.cohortId:source?.id;
    draft.checkIssues=[{section,record,message:error.message}];render();flush();}return null;}
 }
 return{check,cancel(){active=null;}};
}
