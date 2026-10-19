import { audienceReadiness } from './setup-audience-readiness.js';
import { audienceDemand } from './setup-audience-plans.js';
import { policyCandidates } from './setup-audience-teachers.js';
import { locatedDraftIssues as draftIssues, locateKernelFeedback } from './school-document.js';
import { compileDraft, definitionRevision } from './setup-compile.js';
import { teaching } from './setup-grid.js';
import { available } from './setup-availability.js';
import { nameOf, workload } from './setup-teachers.js';
import { allocated } from './setup-plans.js';
export function reviewContent(draft,html){
 const periods=draft.grid.intervals.filter(teaching),required=draft.plans.reduce((n,p)=>n+(Number.isInteger(p.requiredCount)?p.requiredCount:0),0),assigned=draft.plans.reduce((n,p)=>n+allocated(p),0);
 const issues=[...draftIssues(draft),...(draft.checkIssues??[])],audience=audienceReadiness(draft);
 const demands=(draft.audiencePlans??[]).map(p=>audienceDemand(draft,p)),sessions=demands.reduce((n,d)=>n+d.sessions,0),received=demands.reduce((n,d)=>n+(d.received??0),0);
 return `<p id="setup-readiness" role="status">${html(draft.validation)}. Readiness checks necessary conditions; only generation can find a complete timetable.</p>
 ${draft.checkNotice?`<p id="setup-check-notice" role="status">${html(draft.checkNotice)}</p>`:''}
 <button id="setup-check">Check current setup</button>${draft.plans.length||!draft.audiencePlans?`<p id="setup-totals">Fixed-teacher plans · Required: ${required} · Allocated: ${assigned} · Unassigned: ${Math.max(0,required-assigned)} · Over-allocated: ${Math.max(0,assigned-required)}</p>`:''}
 ${draft.audiencePlans?`<div id="setup-audience-totals"><h3>Audience demand</h3><p>Actual audience sessions: ${sessions} · Student teaching periods: ${received} · Active anonymous blocks: ${[...audience.classes.values()].reduce((n,c)=>n+c.blocks.length,0)}</p><p>Internal student gaps are soft by default and are minimized before ordinary preferences. Explicit hard gap limits, gap-free days and strict coverage remain hard.</p></div>`:''}
 <h3>Class demand and teaching capacity</h3>${draft.classes.map(c=>{const plans=draft.plans.filter(p=>(p.classId??p.cohortId)===c.id),legacy=plans.reduce((n,p)=>n+(Number.isInteger(p.requiredCount)?p.requiredCount:0),0),audiencePlans=(draft.audiencePlans??[]).filter(p=>p.classId===c.id).map(p=>audienceDemand(draft,p)),demand=legacy+audiencePlans.reduce((n,d)=>n+d.sessions,0),received=legacy*c.size+audiencePlans.reduce((n,d)=>n+(d.received??0),0);return `<p>${html(nameOf(c))}: ${demand} actual sessions · ${received} student teaching periods · Regular capacity ${periods.filter(p=>p.kind==='Lesson'&&available(c,p.periodId)).length} · Reserved capacity ${periods.filter(p=>p.kind==='Reserved lesson'&&available(c,p.periodId)).length} (only permitted subjects)</p>`;}).join('')}
 <h3>Teacher loads (nonblocking monitoring)</h3>${draft.teachers.map(t=>{
 const fixed=(draft.audiencePlans??[]).reduce((n,p)=>n+p.portions.reduce((m,q)=>m+(q.teacherPolicy.mode==='fixed'&&q.teacherPolicy.fixedTeacherId===t.id?q.sessionIds.length:q.teacherPolicy.mode==='sharing'?(q.teacherPolicy.shares??[]).filter(s=>s.teacherId===t.id&&Number.isInteger(s.count)).reduce((n,s)=>n+s.count,0):0),0),0)+workload(draft,t.id);
 const automatic=(draft.audiencePlans??[]).reduce((n,p)=>n+p.portions.filter(q=>q.teacherPolicy.mode==='automatic'&&policyCandidates(draft,p,q).includes(t.id)).reduce((m,q)=>m+q.sessionIds.length,0),0);
 return `<p>${html(nameOf(t))}: ${fixed} fixed individual lessons · automatic selection pending for up to ${automatic} eligible lessons${Object.hasOwn(t,'referenceWeeklyMaximum')?` · reference weekly maximum ${html(t.referenceWeeklyMaximum)}${fixed>t.referenceWeeklyMaximum?' — exceeds reference (nonblocking)':''}`:''}</p>`;}).join('')}
 <h3>Retained unused draft settings (nonblocking)</h3><div id="setup-review-warnings">${audience.warnings.map(i=>`<p>${html(i.message)}</p>`).join('')||'<p>No unused draft issue.</p>'}</div>
 <h3>Blocking issues</h3><div id="setup-review-issues">${issues.map((i,n)=>`<p><button data-review-issue="${n}">${html(i.path??i.section)}: ${html(i.message)}</button></p>`).join('')||'<p>No known blocking issue.</p>'}</div>
 <details class="setup-review-rules"><summary>Scheduling rules, preferences and demo limits</summary><h3>Hard requirements</h3><p>Availability and class lunch; room capacity, capabilities, policies and fixed rooms; eligibility, qualifications and curator rules; reserved permissions and maxima; daily maxima, gap/start/load bounds; undeclared consecutive runs forbidden; declared exact lengths, common teacher/room and weekly frequencies.</p>
 <h3>Soft preferences</h3><p>Teacher/class gaps, preferred start, daily balance, undesirable periods, home/preferred room and spreading. Mandatory valid blocks receive only their required spreading exemption. Soft cost never makes a hard-invalid arrangement valid.</p>
 <p>Limit: 2 MiB UTF-8; configured lesson and placement-work limits apply. Spare teaching capacity is allowed. Readiness is not proof of feasibility.</p></details>`;
}
export function createReadiness({getDraft,build,request,render,flush,isDisposed}){
 let active;
 async function check(){
  const draft=getDraft(),token={id:draft.id,revision:draft.revision,build:build(),data:JSON.stringify(draft)};active=token;
  draft.validation='Checking';delete draft.check;delete draft.checkIssues;delete draft.checkNotice;render();
  const current=()=>!isDisposed()&&active===token&&getDraft()?.id===token.id&&getDraft().revision===token.revision&&build()===token.build;
  const stale=()=>{if(!isDisposed()&&active===token&&getDraft()?.id===token.id&&getDraft().revision!==token.revision){getDraft().checkNotice=`The completed check belongs to older revision ${token.revision}; current revision ${getDraft().revision} needs its own check.`;render();flush();}return null;};
  try{
   const issues=draftIssues(draft);
   if(issues.length){if(current()){draft.validation='Blocked';render();flush();}return null;}
   const definition=compileDraft(draft),exact=JSON.stringify(definition),expected=await definitionRevision(definition);
   if(!current())return stale();
   const {value}=await request('/api/demo/definition/check',{method:'POST',body:exact,headers:{'Content-Type':'text/plain'}});
   if(!current())return stale();
   if(value.build!==token.build||value.definitionRevision!==expected)throw new Error('Definition/build provenance did not match this exact setup. Review again.');
   draft.validation='Ready';draft.check={authoringRevision:token.revision,definitionRevision:expected,build:value.build,catalogVersion:definition.catalogVersion,definition,checkedAt:new Date().toISOString()};render();flush();return draft.check;
  }catch(error){if(current()){draft.validation=[400,413,422].includes(error.status)?'Blocked':'Check failed';
    const located=/\/(subjects|teachers|cohorts|rooms|periods|lessons)\/(\d+)/.exec(error.message),section=located?({subjects:'Subjects',teachers:'Teachers',cohorts:'Classes',rooms:'Rooms',periods:'Timetable grid',lessons:'Classes'})[located[1]]:'Review';
    let source;try{source=located?compileDraft(draft,{allowIssues:true})[located[1]]?.[Number(located[2])]:null;}catch{}
    const record=located?.[1]==='periods'?draft.grid.intervals.find(i=>i.periodId===source?.id)?.id:located?.[1]==='lessons'?source?.cohortId:source?.id;
    let message=error.message;try{message=locateKernelFeedback(draft,compileDraft(draft,{allowIssues:true}),message);}catch{}
    draft.checkIssues=[{section,record,message}];render();flush();}return null;}
 }
 return{check,cancel(){active=null;}};
}
