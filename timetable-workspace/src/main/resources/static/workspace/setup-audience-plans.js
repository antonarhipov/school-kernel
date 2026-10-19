import { audienceSchedulingContent, readableScheduling } from './setup-coordination.js';
import { audienceTeacherContent } from './setup-audience-teachers.js';
const integer=value=>Number.isInteger(value)&&value>=0&&value<=2147483647;
const uid=()=>crypto.randomUUID();
const own=(object,key)=>Object.hasOwn(object??{},key);
export function audienceDemand(draft,plan){
  const c=draft.classes.find(c=>c.id===plan.classId),split=c?.splits?.find(s=>s.id===plan.splitId),issues=[];
  const add=message=>issues.push({section:'Classes',record:plan.classId,audiencePlan:plan.id,message:`${c?.displayName??plan.classId} / ${draft.subjects.find(s=>s.id===plan.subjectId)?.displayName??plan.subjectId}: ${message}`});
  if(!c||!draft.subjects.some(s=>s.id===plan.subjectId))add('A class or subject reference is missing.');
  if(!integer(plan.weeklyTotal))add('Weekly teaching total must be a nonnegative integer.');
  if(!['whole','grouped','mixed'].includes(plan.delivery))add('Choose whole-class, grouped or mixed teaching.');
  const whole=plan.delivery==='whole'?plan.weeklyTotal:plan.delivery==='grouped'?0:plan.wholeClassCount;
  if(!integer(whole))add('Whole-class contribution must be a nonnegative integer.');
  if(plan.delivery!=='whole'&&!split)add('Choose an existing class split, or declare one in Class splits.');
  const rows=plan.delivery==='whole'?[{audienceId:`class:${plan.classId}`,displayName:'Whole class',size:c?.size,total:plan.weeklyTotal,inherited:true,outstanding:0}]:
    (split?.groups??[]).map(g=>({audienceId:g.id,displayName:g.displayName,size:g.size,total:own(plan.overrides,g.id)?plan.overrides[g.id]:plan.weeklyTotal,inherited:!own(plan.overrides,g.id)}));
  for(const row of rows){
    if(!integer(row.total))add(`${row.displayName}: total must be a nonnegative integer.`);
    if(integer(row.total)&&integer(whole)&&whole>row.total)add(`${row.displayName}: whole-class contribution ${whole} exceeds total ${row.total}.`);
    row.outstanding=plan.delivery==='whole'?0:integer(row.total)&&integer(whole)?row.total-whole:null;
  }
  for(const id of Object.keys(plan.overrides??{}))if(!split?.groups.some(g=>g.id===id))add(`Retained override for missing/inactive group ${id}; correct its reference explicitly.`);
  const portions=[];
  if(integer(whole)&&whole>0)portions.push({audienceId:`class:${plan.classId}`,displayName:'Whole class',size:c?.size,count:whole});
  for(const row of rows)if(plan.delivery!=='whole'&&integer(row.outstanding)&&row.outstanding>0)portions.push({...row,count:row.outstanding});
  const sessions=portions.reduce((n,p)=>n+p.count,0);
  if(sessions>2500)add('Actual sessions exceed the disclosed 2500-session plan limit; no excess sessions are materialized.');
  const received=rows.every(r=>integer(r.total)&&integer(r.size))?rows.reduce((n,r)=>n+r.size*r.total,0):null;
  return {rows,portions,whole,sessions,received,issues};
}
export function synchronizeAudienceSessions(draft,plan,{reduce=false}={}){
  const demand=audienceDemand(draft,plan);if(demand.issues.length)return;
  plan.portions??=[];
  for(const wanted of demand.portions){
    let portion=plan.portions.find(p=>p.audienceId===wanted.audienceId);
    if(!portion){portion={id:uid(),audienceId:wanted.audienceId,sessionIds:[],teacherPolicy:{mode:'automatic'}};plan.portions.push(portion);}
    if(reduce)portion.sessionIds=portion.sessionIds.slice(0,wanted.count);
    while(portion.sessionIds.length<wanted.count)portion.sessionIds.push(uid());
  }
  if(reduce)plan.portions=plan.portions.filter(p=>demand.portions.some(w=>w.audienceId===p.audienceId));
}
export function audiencePlanIssues(draft){
  const issues=[];
  for(const p of draft.audiencePlans??[]){
    const demand=audienceDemand(draft,p);issues.push(...demand.issues);
    const add=message=>issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message});
    if((draft.audiencePlans??[]).filter(q=>q.classId===p.classId&&q.subjectId===p.subjectId).length>1||draft.plans.some(q=>(q.classId??q.cohortId)===p.classId&&q.subjectId===p.subjectId))add('Duplicate class/subject demand. Explicitly restructure the fixed-teacher or audience plan; neither is silently replaced.');
    for(const portion of p.portions??[]){const desired=demand.portions.find(row=>row.audienceId===portion.audienceId)?.count??0;
      if(portion.sessionIds.length!==desired)add(`Retained ${portion.sessionIds.length} lessons for this audience, required ${desired}. Review changed teaching explicitly.`);
    }
    for(const desired of demand.portions)if(!p.portions?.some(q=>q.audienceId===desired.audienceId))add(`Audience ${desired.displayName}: required sessions have not yet been materialized.`);
  }
  return issues;
}
export function audiencePlansContent(draft,c,html,{subjectId}={}){
  const plans=(draft.audiencePlans??[]).filter(p=>p.classId===c.id&&(!subjectId||p.subjectId===subjectId));
  return `<div data-audience-plan-class="${html(c.id)}">${subjectId?'<button data-back-subject-teaching class="secondary">← Subject teaching arrangements</button>':'<button id="setup-back-audience-plans" class="secondary">← Class details</button>'}<h3>${subjectId?`${html(draft.subjects.find(s=>s.id===subjectId)?.displayName)} · ${html(c.displayName)} · Teaching arrangement`:`Teaching requirements · ${html(c.displayName)}`}</h3>
    <p>Weekly totals describe the teaching received by each audience. One whole-class session contributes to every group and uses one teacher and one room.</p>
    ${subjectId?'':`<label>Add subject<select id="setup-audience-subject"><option value="">Choose a subject</option>${draft.subjects.filter(s=>!plans.some(p=>p.subjectId===s.id)).map(s=>`<option value="${html(s.id)}">${html(s.displayName)}</option>`).join('')}</select></label><button id="setup-add-audience-plan">Add teaching requirement</button>`}
    ${subjectId?'<button data-manage-subject-groups class="secondary">Create or edit groups…</button>':`<button data-go-class-splits class="secondary">Reusable groups and relationships</button><button data-open-coordination="${html(c.id)}" class="secondary">Hard scheduling and coordination</button>`}
    ${plans.map(p=>{const demand=audienceDemand(draft,p);return `<fieldset data-audience-plan="${html(p.id)}"><legend>${html(draft.subjects.find(s=>s.id===p.subjectId)?.displayName??p.subjectId)}</legend>
      <div class="setup-context-fields"><label>Weekly teaching total<input data-audience-field="weeklyTotal" inputmode="numeric" value="${html(p.weeklyTotal)}" aria-describedby="audience-issues-${html(p.id)}"></label>
      <label>Delivery<select data-audience-field="delivery"><option value="whole" ${p.delivery==='whole'?'selected':''}>Whole class</option><option value="grouped" ${p.delivery==='grouped'?'selected':''}>Grouped</option><option value="mixed" ${p.delivery==='mixed'?'selected':''}>Mixed whole class and groups</option></select></label>
      ${p.delivery!=='whole'?`<label>Group arrangement<select data-audience-field="splitId"><option value="">Choose a split</option>${(c.splits??[]).map(s=>`<option value="${html(s.id)}" ${p.splitId===s.id?'selected':''}>${html(s.displayName)}</option>`).join('')}</select></label>`:''}
      ${p.delivery==='mixed'?`<label>Whole-class sessions<input data-audience-field="wholeClassCount" inputmode="numeric" value="${html(p.wholeClassCount)}" aria-describedby="audience-issues-${html(p.id)}"></label>`:''}</div>
      ${p.delivery!=='whole'?demand.rows.map(row=>`<label>${html(row.displayName)} · total override (blank inherits)<input data-audience-override="${html(row.audienceId)}" inputmode="numeric" value="${html(row.inherited?'':row.total)}" aria-describedby="audience-issues-${html(p.id)}"></label>`).join(''):''}
      <div data-audience-summary>${audiencePlanFeedback(draft,p,html)}</div>
      <button data-reconcile-audience-sessions class="secondary">Review teaching changes…</button><button data-remove-audience-plan class="secondary">Remove teaching requirement…</button>
      <p>New portions use automatic weekly teacher selection. </p>
      <div data-audience-teacher-settings>${audienceTeacherContent(draft,p,html)}</div>${audienceSchedulingContent(draft,p,html)}</fieldset>`;}).join('')}</div>`;
}
function audiencePlanFeedback(draft,p,html){
 const d=audienceDemand(draft,p);
 return `<p>Actual sessions: <strong>${d.sessions}</strong> · Student teaching periods: ${html(d.received??'unfinished')} · Whole-class contribution: ${html(d.whole)}</p>
 <ul>${d.rows.map(row=>`<li>${html(row.displayName)}: total ${html(row.total)} (${row.inherited?'inherited':'explicit override'}), whole class ${html(d.whole)}, outstanding group sessions ${html(row.outstanding??'unfinished')}</li>`).join('')}</ul>
 <div id="audience-issues-${html(p.id)}" role="status">${audiencePlanIssues(draft).filter(i=>i.audiencePlan===p.id).map(i=>`<p>${html(i.message)}</p>`).join('')}</div>`;
}
export function audienceReferences(draft,plan){
 const ids=new Set([plan.id,...(plan.portions??[]).flatMap(p=>[p.id,...p.sessionIds])]),refs=[];
 const visit=(v,path)=>{if(v===plan)return;if(Array.isArray(v))v.forEach((v,i)=>visit(v,`${path}/${i}`));else if(v&&typeof v==='object')for(const [k,x]of Object.entries(v)){if(['pendingInput','focused','check','advanced','source'].includes(k))continue;if(ids.has(k))refs.push(`${path}/${k}`);visit(x,`${path}/${k}`);}else if(ids.has(v))refs.push(path);};visit(draft,'');return refs;
}
export function createAudiencePlanActions({getDraft,commit,contextOnly,confirm,notify,html}){
 const numeric=raw=>/^-?\d+$/.test(raw)&&Number.isSafeInteger(Number(raw))?Number(raw):raw;
 function edit(event){
  const node=event.target,root=node.closest('[data-audience-plan]');if(!root||(!node.dataset.audienceField&&!node.dataset.audienceOverride))return false;
  const draft=getDraft(),p=draft.audiencePlans.find(p=>p.id===root.dataset.audiencePlan);
  if(node.dataset.audienceOverride){p.overrides={...p.overrides};if(node.value==='')delete p.overrides[node.dataset.audienceOverride];else p.overrides[node.dataset.audienceOverride]=numeric(node.value);}
  else p[node.dataset.audienceField]=['weeklyTotal','wholeClassCount'].includes(node.dataset.audienceField)?numeric(node.value):node.value;
  synchronizeAudienceSessions(draft,p);const render=node.tagName==='SELECT';commit(draft,render);
  if(!render)root.querySelector('[data-audience-summary]').innerHTML=audiencePlanFeedback(draft,p,html);return true;
 }
 return {input:event=>event.target.tagName==='SELECT'?false:edit(event),change:event=>event.target.tagName==='SELECT'?edit(event):false,
 click(event){const button=event.target.closest('button');if(!button)return false;const draft=getDraft();
  if(button.dataset.openAudiencePlans){draft.pendingInput.audiencePlanClassId=button.dataset.openAudiencePlans;contextOnly(draft);return true;}
  if(button.id==='setup-back-audience-plans'||button.hasAttribute('data-go-class-splits')){delete draft.pendingInput.audiencePlanClassId;contextOnly(draft);return true;}
  if(button.id==='setup-add-audience-plan'){
   const classId=button.closest('[data-audience-plan-class]').dataset.audiencePlanClass,subjectId=document.querySelector('#setup-audience-subject').value;
   if(!subjectId||(draft.audiencePlans??[]).some(p=>p.classId===classId&&p.subjectId===subjectId)){notify('Choose a subject without an existing requirement.');return true;}
   if(!draft.audiencePlans?.length)for(const c of draft.classes)if(!Object.hasOwn(c,'maxDailyGaps')&&draft.plans.some(p=>(p.classId??p.cohortId)===c.id))c.maxDailyGaps=0;
   (draft.audiencePlans??=[]).push({id:uid(),classId,subjectId,weeklyTotal:'',delivery:'whole',wholeClassCount:0,overrides:{},portions:[]});commit(draft);return true;
  }
  const root=button.closest('[data-audience-plan]');if(!root)return false;const p=draft.audiencePlans.find(p=>p.id===root.dataset.audiencePlan);
  if(button.hasAttribute('data-reconcile-audience-sessions')){
   const d=audienceDemand(draft,p);if(d.issues.length){notify(d.issues.map(i=>i.message).join(' '));return true;}
   const refs=audienceReferences(draft,p);if(refs.length){notify(`Correct affected teacher portions, block rules, coordination and session references before reducing demand: ${refs.join('; ')}.`);return true;}
   const changes=(p.portions??[]).map(q=>`${d.portions.find(row=>row.audienceId===q.audienceId)?.displayName??"Former audience"}: ${q.sessionIds.length} → ${d.portions.find(row=>row.audienceId===q.audienceId)?.count??0} lessons`).join('; ');
   confirm('Apply changed teaching demand?',`${changes}. Removed lessons will no longer be scheduled. Surviving lessons and teacher policies remain.`,()=>{synchronizeAudienceSessions(draft,p,{reduce:true});commit(draft);},'Apply teaching demand');return true;
  }
  if(button.hasAttribute('data-remove-audience-plan')){
   const refs=audienceReferences(draft,p);if(refs.length){notify(`Correct references before removal: ${refs.join('; ')}.`);return true;}
   confirm('Remove teaching requirement?',`Remove this requirement and ${p.portions.reduce((n,p)=>n+p.sessionIds.length,0)} session references? Other requirements and saved artifacts are preserved.`,()=>{draft.audiencePlans=draft.audiencePlans.filter(row=>row.id!==p.id);commit(draft);},'Remove requirement');return true;
  }
  return false;
 }};
}
export function readableAudiencePlans(draft){
 const object=v=>v&&typeof v==='object'&&!Array.isArray(v),ids=v=>Array.isArray(v)&&v.every(id=>typeof id==='string');
 return !own(draft,'audiencePlans')||Array.isArray(draft.audiencePlans)&&draft.audiencePlans.every(p=>object(p)&&['id','classId','subjectId','delivery'].every(k=>typeof p[k]==='string')
   &&['weeklyTotal','wholeClassCount'].every(k=>['string','number'].includes(typeof p[k]))&&(!own(p,'splitId')||typeof p.splitId==='string')
   &&readableScheduling(p)&&object(p.overrides)&&Object.values(p.overrides).every(v=>['string','number'].includes(typeof v))
   &&(!own(p,'candidateTeacherIds')||ids(p.candidateTeacherIds))
   &&Array.isArray(p.portions)&&p.portions.every(q=>object(q)&&typeof q.id==='string'&&typeof q.audienceId==='string'&&ids(q.sessionIds)&&readableScheduling(q)&&readableTeacherPolicy(q.teacherPolicy)&&Object.keys(q).every(k=>['id','audienceId','sessionIds','teacherPolicy','blockRules','allowedRoomIds','fixedRoomId'].includes(k)))
   &&(!own(p,'sourceFixedPlan')||object(p.sourceFixedPlan))
   &&Object.keys(p).every(k=>['id','classId','subjectId','weeklyTotal','delivery','wholeClassCount','splitId','overrides','portions','candidateTeacherIds','sourceFixedPlan','blockRules','allowedRoomIds','requiredRoomCapabilityIds'].includes(k)));
}
function readableTeacherPolicy(policy){
 const ids=v=>Array.isArray(v)&&v.every(id=>typeof id==='string');
 return policy&&['automatic','fixed','sharing'].includes(policy.mode)&&(!own(policy,'candidateTeacherIds')||ids(policy.candidateTeacherIds))
 &&(!own(policy,'fixedTeacherId')||typeof policy.fixedTeacherId==='string')
 &&(!own(policy,'shares')||Array.isArray(policy.shares)&&policy.shares.every(s=>s&&typeof s.id==='string'&&typeof s.teacherId==='string'&&['number','string'].includes(typeof s.count)&&Object.keys(s).every(k=>['id','teacherId','count'].includes(k))))
 &&Object.keys(policy).every(k=>['mode','candidateTeacherIds','fixedTeacherId','shares'].includes(k));
}
