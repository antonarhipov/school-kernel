import { eligible, qualification, nameOf } from './setup-teachers.js';
import { curriculumContent } from './setup-curriculum.js';
import { rowRoomContent, roomIssues } from './setup-rooms.js';
import { blockContent, blockIssues } from './setup-blocks.js';
const clone=value=>structuredClone(value),uid=()=>crypto.randomUUID();
let limits={lessonMax:2500,workMax:11000000};
export const planLimits=()=>({...limits});
export function configurePlanLimits(value){
  if(Number.isInteger(value.lessonMax)&&value.lessonMax>0)limits.lessonMax=value.lessonMax;
  if(Number.isSafeInteger(value.workMax)&&value.workMax>0)limits.workMax=value.workMax;
}
export const positive=value=>Number.isInteger(value)&&value>0&&value<=2147483647;
export const allocated=plan=>(plan.allocations??[]).reduce((n,a)=>n+(positive(a.count)?a.count:0),0);
export const protectedReasons=plan=>[...(plan.protectedReasons??[]),...(plan.protected?['Protected imported row']:[]),...(plan.occurrences??[]).filter(o=>o.protected||o.periodLock||o.roomLock||o.exception).map(o=>`Occurrence ${o.id}: ${o.periodLock?'period lock ':''}${o.roomLock?'room lock ':''}${o.protected||o.exception?'retained exception':''}`)];
export function planIssues(draft){
  const issues=[];
  for(const p of draft.plans){
    const classId=p.classId??p.cohortId,c=draft.classes.find(c=>c.id===classId),s=draft.subjects.find(s=>s.id===p.subjectId);
    const add=message=>issues.push({section:'Classes',record:classId,plan:p.id,message:`${nameOf(c)} / ${nameOf(s)}: ${message}`});
    if(!c||!s)add('class or subject reference is missing.');
    if(!positive(p.requiredCount))add('weekly demand must be a positive integer.');
    if(allocated(p)>limits.lessonMax||draft.plans.reduce((n,row)=>n+allocated(row),0)>limits.lessonMax)add(`allocated demand exceeds the configured demo limit of ${limits.lessonMax} lessons; no extra occurrences are materialized.`);
    if(draft.plans.filter(row=>(row.classId??row.cohortId)===classId&&row.subjectId===p.subjectId).length>1)add('duplicate class-subject teaching row.');
    const seen=new Set();
    for(const a of p.allocations??[]){
      if(!positive(a.count))add(`allocation for ${a.teacherId} needs a positive integer.`);
      if(seen.has(a.teacherId))add(`teacher ${a.teacherId} has duplicate allocations.`);seen.add(a.teacherId);
      if(!eligible(draft,draft.teachers.find(t=>t.id===a.teacherId),p.subjectId,classId))add(`teacher ${a.teacherId} lacks valid qualification/class eligibility or the required curator nomination; allocated work is retained.`);
    }
    if(positive(p.requiredCount)){
      const difference=p.requiredCount-allocated(p);
      if(difference>0)add(`${difference} unassigned lessons block generation.`);
      if(difference<0)add(`over-allocation: ${-difference} lessons above weekly demand.`);
    }
  }
  return issues;
}
// Surviving occurrences keep their exact identities, labels and settings. No unassigned placeholder lessons exist.
export function synchronizeOccurrences(plan,draft){
  if(protectedReasons(plan).length)return clone(plan);
  const next=clone(plan),allocations=next.allocations??[];
  if(allocated(next)>limits.lessonMax||draft.plans.reduce((n,row)=>n+(row.id===next.id?allocated(next):allocated(row)),0)>limits.lessonMax)return next;
  if(allocations.some(a=>!positive(a.count)||!draft.teachers.some(t=>t.id===a.teacherId))||new Set(allocations.map(a=>a.teacherId)).size!==allocations.length)return next;
  const old=next.occurrences??[],used=new Set(),result=[],needed=[];
  for(const a of allocations){
    const keep=old.filter(o=>o.teacherId===a.teacherId).slice(0,a.count);
    for(const o of keep){result.push(o);used.add(o.id);}
    for(let n=keep.length;n<a.count;n++)needed.push(a.teacherId);
  }
  const spare=old.filter(o=>!used.has(o.id));
  next.nextOccurrence=next.nextOccurrence??old.length;
  const subject=draft.subjects.find(s=>s.id===next.subjectId),cohort=draft.classes.find(c=>c.id===(next.classId??next.cohortId));
  for(const teacherId of needed){
    const reused=spare.shift();
    if(reused){result.push({...reused,teacherId});continue;}
    const ordinal=++next.nextOccurrence;
    result.push({...clone(next.occurrenceTemplate??{}),id:uid(),displayName:`${nameOf(subject)} · ${nameOf(cohort)} · ${ordinal}`,teacherId,
      ...(Object.hasOwn(next,'seriesId')?{seriesId:next.seriesId}:{})});
  }
  // Keep creation order so later safe reductions remove the newest surviving occurrences first.
  const previousOrder=new Map(old.map((o,i)=>[o.id,i]));
  next.occurrences=result.sort((a,b)=>(previousOrder.get(a.id)??Number.MAX_SAFE_INTEGER)-(previousOrder.get(b.id)??Number.MAX_SAFE_INTEGER));
  return next;
}
export function plansContent(draft,classId,html){
  const cohort=draft.classes.find(c=>c.id===classId),plans=draft.plans.filter(p=>(p.classId??p.cohortId)===classId);
  const options=draft.subjects.filter(s=>!plans.some(p=>p.subjectId===s.id));
  return `<div id="setup-class-plan" data-plan-class="${html(classId)}"><h3>${html(nameOf(cohort))} · weekly teaching</h3><button id="setup-back-class" class="secondary">Class details</button>
    <p>Each subject has one weekly demand row. Allocate existing occurrences to teachers; unassigned demand blocks generation. Undeclared consecutive lessons are hard-forbidden under the new rules.</p>
    <label>Add subject to class<select id="setup-plan-subject"><option value="">Choose a subject…</option>${options.map(s=>`<option value="${html(s.id)}">${html(nameOf(s))}</option>`).join('')}</select></label><button id="setup-add-plan">Add weekly subject</button>
    ${curriculumContent(draft,classId,html)}
    ${plans.map(p=>{
      const total=allocated(p),delta=positive(p.requiredCount)?p.requiredCount-total:null;
      const choices=draft.teachers.filter(t=>eligible(draft,t,p.subjectId,classId)&&!p.allocations?.some(a=>a.teacherId===t.id));
      const issues=[...planIssues(draft),...roomIssues(draft),...blockIssues(draft)].filter(i=>i.plan===p.id);
      return `<fieldset data-plan="${html(p.id)}"><legend>${html(nameOf(draft.subjects.find(s=>s.id===p.subjectId)))}</legend>
      <label>Required lessons per week<input data-plan-demand inputmode="numeric" value="${html(p.requiredCount??'')}"></label>
      <p class="setup-plan-totals">Required: ${html(p.requiredCount??'unfinished')} · Allocated: ${total} · ${delta===null?'Unassigned: needs valid demand':delta<0?`Over-allocated: ${-delta}`:`Unassigned: ${delta}`}</p>
      <div class="setup-plan-allocations">${(p.allocations??[]).map((a,index)=>`<div data-allocation="${index}"><label>${html(nameOf(draft.teachers.find(t=>t.id===a.teacherId)))} · allocated lessons<input data-allocation-count inputmode="numeric" value="${html(a.count)}"></label><button data-remove-allocation="${index}" class="secondary">Remove allocation</button></div>`).join('')}</div>
      <label>Eligible teacher<select data-plan-eligible><option value="">Choose an eligible teacher…</option>${choices.map(t=>`<option value="${html(t.id)}" ${draft.pendingInput?.allocTeacher?.[p.id]===t.id?'selected':''}>${html(nameOf(t))}</option>`).join('')}</select></label><button data-add-allocation>Add teacher allocation</button>
      ${!choices.length?'<p>No additional eligible teacher is available. Keep demand unassigned or explicitly allow a teacher below.</p>':''}
      <details><summary>Allow a teacher for this class and subject</summary><label>Intended teacher<select data-plan-intended><option value="">Choose a teacher…</option>${draft.teachers.filter(t=>!eligible(draft,t,p.subjectId,classId)).map(t=>`<option value="${html(t.id)}" ${draft.pendingInput?.intendedTeacher?.[p.id]===t.id?'selected':''}>${html(nameOf(t))}</option>`).join('')}</select></label><button data-grant-teacher class="secondary">Preview eligibility change…</button></details>
      <details><summary>Retained occurrence settings (${p.occurrences?.length??0} lessons)</summary><p>Surviving lesson identities, labels, series and settings are retained.</p><pre>${html(JSON.stringify(p.occurrences??[],null,2))}</pre>${protectedReasons(p).map(r=>`<p>${html(r)} · use advanced JSON before changing demand or allocations.</p>`).join('')}</details>
      <details class="setup-row-settings"><summary>Row scheduling and room settings</summary>${rowRoomContent(draft,p,html)}${blockContent(p,html)}</details>
      <button data-remove-plan class="secondary">Remove weekly row…</button>
      <div class="setup-plan-issues" role="status">${issues.map(i=>`<p>${html(i.message)}</p>`).join('')}</div></fieldset>`;
    }).join('')}</div>`;
}
export function createPlanActions({getDraft,commit,contextOnly,notify,confirm,html}){
  const rowOf=event=>getDraft().plans.find(p=>p.id===event.target.closest('[data-plan]')?.dataset.plan);
  function changePlan(plan,edit,render=true){
    const reasons=protectedReasons(plan);
    if(reasons.length){notify(`Change refused for row ${plan.id}: ${reasons.join('; ')}. Resolve through advanced JSON. Existing occurrences, allocations and demand are unchanged.`);return false;}
    const draft=clone(getDraft()),index=draft.plans.findIndex(p=>p.id===plan.id);edit(draft.plans[index]);
    draft.plans[index]=synchronizeOccurrences(draft.plans[index],draft);commit(draft,render);return true;
  }
  const integer=raw=>/^-?\d+$/.test(raw)&&Number.isSafeInteger(Number(raw))?Number(raw):raw;
  function totals(plan,element){
    const total=allocated(plan),delta=positive(plan.requiredCount)?plan.requiredCount-total:null;
    element.querySelector('.setup-plan-totals').textContent=`Required: ${plan.requiredCount??'unfinished'} · Allocated: ${total} · ${delta===null?'Unassigned: needs valid demand':delta<0?`Over-allocated: ${-delta}`:`Unassigned: ${delta}`}`;
    element.querySelector('.setup-plan-issues').innerHTML=[...planIssues(getDraft()),...roomIssues(getDraft()),...blockIssues(getDraft())].filter(i=>i.plan===plan.id).map(i=>`<p>${html(i.message)}</p>`).join('');
  }
  return {
    input(event){
      if(!event.target.hasAttribute('data-plan-demand')&&!event.target.hasAttribute('data-allocation-count'))return false;
      const plan=rowOf(event),container=event.target.closest('[data-plan]');
      const previous=event.target.hasAttribute('data-plan-demand')?plan.requiredCount:plan.allocations[Number(event.target.closest('[data-allocation]').dataset.allocation)].count;
      const changed=changePlan(plan,p=>{if(event.target.hasAttribute('data-plan-demand'))p.requiredCount=integer(event.target.value);else p.allocations[Number(event.target.closest('[data-allocation]').dataset.allocation)].count=integer(event.target.value);},false);
      if(!changed)event.target.value=previous??'';else totals(rowOf(event),container);return true;
    },
    change(event){
      if(!event.target.hasAttribute('data-plan-eligible')&&!event.target.hasAttribute('data-plan-intended'))return false;
      const draft=clone(getDraft()),plan=rowOf(event),key=event.target.hasAttribute('data-plan-eligible')?'allocTeacher':'intendedTeacher';
      draft.pendingInput[key]??={};draft.pendingInput[key][plan.id]=event.target.value;contextOnly(draft,false);return true;
    },
    click(event){
      const button=event.target.closest('button');if(!button)return false;
      const draft=getDraft();
      if(button.dataset.openClassPlan){const next=clone(draft);next.pendingInput.planClassId=button.dataset.openClassPlan;contextOnly(next);return true;}
      if(button.id==='setup-back-class'){const next=clone(draft);delete next.pendingInput.planClassId;contextOnly(next);return true;}
      if(button.id==='setup-add-plan'){
        const subjectId=document.querySelector('#setup-plan-subject').value,classId=document.querySelector('#setup-class-plan').dataset.planClass;
        if(!subjectId||draft.plans.some(p=>(p.classId??p.cohortId)===classId&&p.subjectId===subjectId)){notify('Choose a subject without an existing row for this class. No teaching row was created.');return true;}
        const next=clone(draft);next.plans.push({id:uid(),classId,subjectId,requiredCount:'',allocations:[],occurrences:[],seriesId:uid(),nextOccurrence:0,blockRules:[]});commit(next);return true;
      }
      const plan=rowOf(event);if(!plan)return false;
      if(button.hasAttribute('data-add-allocation')){
        const teacherId=button.closest('[data-plan]').querySelector('[data-plan-eligible]').value;
        if(!teacherId||!eligible(draft,draft.teachers.find(t=>t.id===teacherId),plan.subjectId,plan.classId??plan.cohortId)){notify('Choose an eligible teacher. Selection grants no qualification or eligibility.');return true;}
        changePlan(plan,p=>{if(!p.allocations.some(a=>a.teacherId===teacherId))p.allocations.push({teacherId,count:''});});return true;
      }
      if(button.hasAttribute('data-remove-allocation')){changePlan(plan,p=>p.allocations.splice(Number(button.dataset.removeAllocation),1));return true;}
      if(button.hasAttribute('data-grant-teacher')){
        const teacherId=button.closest('[data-plan]').querySelector('[data-plan-intended]').value,t=draft.teachers.find(t=>t.id===teacherId),classId=plan.classId??plan.cohortId;
        if(!t){notify('Choose the intended teacher before previewing a change.');return true;}
        const s=draft.subjects.find(s=>s.id===plan.subjectId),c=draft.classes.find(c=>c.id===classId);
        if(s.curatorLesson&&c.curatorTeacherId!==t.id){notify('This curator lesson requires the nominated class curator. Eligibility cannot override that hard rule.');return true;}
        const needsQualification=!qualification(draft,t,s.id,c.id);
        confirm('Allow teacher for this class?',`Allow ${nameOf(t)} to teach ${nameOf(s)} in ${nameOf(c)}? ${needsQualification?'This also adds the subject qualification.':'Existing qualification, including own-class curator qualification, is kept.'} No teaching is allocated by this permission.`,()=>{
          const next=clone(getDraft()),teacher=next.teachers.find(t=>t.id===teacherId);teacher.eligibleClassIdsBySubject??={};teacher.eligibleClassIdsBySubject[s.id]??=[];
          if(!teacher.eligibleClassIdsBySubject[s.id].includes(c.id))teacher.eligibleClassIdsBySubject[s.id].push(c.id);
          if(needsQualification){teacher.qualifiedSubjectIds??=[];if(!teacher.qualifiedSubjectIds.includes(s.id))teacher.qualifiedSubjectIds.push(s.id);}commit(next);
        },'Allow teacher');return true;
      }
      if(button.hasAttribute('data-remove-plan')){
        if(protectedReasons(plan).length){notify(`Cannot remove protected row ${plan.id}: ${protectedReasons(plan).join('; ')}. Use advanced JSON.`);return true;}
        confirm('Remove weekly teaching row?',`Remove ${nameOf(draft.subjects.find(s=>s.id===plan.subjectId))}: demand ${plan.requiredCount}, allocations ${JSON.stringify(plan.allocations)}, blocks ${JSON.stringify(plan.blockRules??[])}, fixed room ${plan.fixedRoomId??'none'} and all ${plan.occurrences?.length??0} occurrences?`,()=>{const next=clone(getDraft());next.plans=next.plans.filter(p=>p.id!==plan.id);commit(next);},'Remove weekly row');return true;
      }
      if(button.hasAttribute('data-plan-settings')){notify('Undeclared consecutive lessons are hard-forbidden under the new rules. Consecutive blocks and fixed-room settings are separate explicit row actions. Retained source restrictions are preserved.');return true;}
      return false;
    }
  };
}
