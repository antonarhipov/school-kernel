import { available } from './setup-availability.js';
import { suitableRooms } from './setup-rooms.js';
import { gridIssues } from './setup-grid.js';
const clone=value=>structuredClone(value),uid=()=>crypto.randomUUID(),positive=v=>Number.isInteger(v)&&v>0&&v<=2147483647;
export const blockConsumption=plan=>(plan.blockRules??[]).reduce((sum,r)=>sum+(positive(r.length)&&positive(r.count)?r.length*r.count:0),0);
export function consecutiveWindows(draft,length){
  if(!positive(length))return [];
  const result=[];
  for(const day of draft.grid.activeDays){
    let segment=[];
    const finish=()=>{for(let n=0;n+length<=segment.length;n++)result.push(segment.slice(n,n+length));segment=[];};
    for(const interval of draft.grid.intervals.filter(i=>i.day===day)){
      if(interval.kind==='Lunch')finish();
      else if(['Lesson','Reserved lesson'].includes(interval.kind))segment.push(interval);
    }
    finish();
  }
  return result;
}
export function blockIssues(draft){
  const issues=[];
  for(const plan of draft.plans){
    const classId=plan.classId??plan.cohortId,cohort=draft.classes.find(c=>c.id===classId),subject=draft.subjects.find(s=>s.id===plan.subjectId),seen=new Set();
    const add=(rule,message)=>issues.push({section:'Classes',record:classId,plan:plan.id,rule:rule?.id,message:`Row ${plan.id}${rule?`, block ${rule.id??rule.length}`:''}: ${message}`});
    for(const rule of plan.blockRules??[]){
      if(!positive(rule.length)||rule.length<2||!positive(rule.count)||!['EXACTLY','AT_LEAST'].includes(rule.operator)){add(rule,'block length must be an integer of at least two, weekly count positive, and operator Exactly or At least.');continue;}
      if(seen.has(rule.length))add(rule,`duplicate length ${rule.length}; each length needs one separate rule.`);seen.add(rule.length);
      if(!cohort||!subject){add(rule,'class or subject is missing.');continue;}
      if(gridIssues(draft).length)add(rule,'the teaching grid is incomplete or invalid. Correct the located grid issues.');
      if(Number.isInteger(subject.maxDailyLessonsPerCohort)&&subject.maxDailyLessonsPerCohort<rule.length)add(rule,`subject daily maximum ${subject.maxDailyLessonsPerCohort} is below block length ${rule.length}.`);
      const allocations=(plan.allocations??[]).filter(a=>positive(a.count)&&a.count>=rule.length),supply=allocations.reduce((n,a)=>n+Math.floor(a.count/rule.length),0);
      if(supply<rule.count)add(rule,`teacher allocations cannot supply ${rule.count} whole blocks of length ${rule.length}. One teacher must supply each complete block.`);
      const windows=consecutiveWindows(draft,rule.length),compatible=new Set();
      for(const allocation of allocations){
        const teacher=draft.teachers.find(t=>t.id===allocation.teacherId);if(!teacher)continue;
        const rooms=suitableRooms(draft,plan,teacher.id,plan.occurrenceTemplate??plan.occurrences?.find(o=>o.teacherId===teacher.id)??{});
        for(const window of windows){
          if(window.some(p=>!available(cohort,p.periodId)||!available(teacher,p.periodId)||p.kind==='Reserved lesson'&&!subject.reservedPeriodsAllowed))continue;
          if(Number.isInteger(subject.maxWeeklyReservedLessonsPerCohort)&&window.filter(p=>p.kind==='Reserved lesson').length>subject.maxWeeklyReservedLessonsPerCohort)continue;
          if(rooms.some(room=>window.every(p=>available(room,p.periodId))))compatible.add(window.map(p=>p.periodId).join('/'));
        }
      }
      if(compatible.size<rule.count)add(rule,`no sufficient compatible consecutive window of ${rule.length} teaching periods with a common teacher and room (${compatible.size} windows for ${rule.count} required blocks). Empty teaching periods, class lunch and school-wide lunch cannot be skipped; ordinary breaks preserve adjacency.`);
    }
    if(plan.blockRules?.length&&plan.blockEditsProtected)add(null,plan.blockEditsProtected);
    if(positive(plan.requiredCount)&&blockConsumption(plan)>plan.requiredCount)add(null,`minimum block consumption ${blockConsumption(plan)} exceeds weekly demand ${plan.requiredCount}; no demand was increased.`);
  }
  return issues;
}
export function blockContent(plan,html){
  const consumed=blockConsumption(plan),remaining=positive(plan.requiredCount)?plan.requiredCount-consumed:'needs valid demand';
  return `<div class="setup-block-rules"><h4>Consecutive lesson blocks · hard rules</h4><p>Undeclared consecutive runs are forbidden. Each rule permits only its exact length. Maximal runs count once; a triple is not an overlapping pair of doubles. A block shares one teacher and one room throughout and consumes existing occurrences.</p>
    <p class="setup-block-consumption">Minimum consumed: ${consumed} lessons · Remaining demand: ${html(remaining)}. Additional blocks of the same length are allowed only by At least; other lengths need separate rules.</p>
    ${(plan.blockRules??[]).map((r,index)=>`<fieldset data-block-index="${index}"><legend>Block rule ${index+1}</legend><label>Exact block length<input data-block-field="length" inputmode="numeric" value="${html(r.length)}"></label><label>Weekly frequency operator<select data-block-field="operator"><option value="EXACTLY" ${r.operator==='EXACTLY'?'selected':''}>Exactly</option><option value="AT_LEAST" ${r.operator==='AT_LEAST'?'selected':''}>At least</option></select></label><label>Blocks per week<input data-block-field="count" inputmode="numeric" value="${html(r.count)}"></label><button data-remove-block="${index}" class="secondary">Remove block rule…</button></fieldset>`).join('')}
    <button data-add-block class="secondary">Add consecutive block rule</button></div>`;
}
export function createBlockActions({getDraft,commit,notify,confirm,protectedReasons,html,allIssues}){
  const rowOf=target=>getDraft().plans.find(p=>p.id===target.closest('[data-plan]')?.dataset.plan);
  function permitted(plan){const reasons=[...protectedReasons(plan),...(plan.blockEditsProtected?[plan.blockEditsProtected]:[])];if(reasons.length){notify(`Block change refused for ${plan.id}: ${reasons.join('; ')}. Resolve protected imported series/occurrence settings through advanced JSON first.`);return false;}return true;}
  function edit(event){
    const field=event.target.dataset.blockField;if(!field)return false;
    const plan=rowOf(event.target),index=Number(event.target.closest('[data-block-index]').dataset.blockIndex),previous=plan.blockRules[index][field];
    if(!permitted(plan)){event.target.value=previous;return true;}
    const next=clone(getDraft()),row=next.plans.find(p=>p.id===plan.id),raw=event.target.value;
    row.blockRules[index][field]=field==='operator'?raw:/^-?\d+$/.test(raw)&&Number.isSafeInteger(Number(raw))?Number(raw):raw;commit(next,false);
    event.target.closest('[data-plan]').querySelector('.setup-block-consumption').textContent=`Minimum consumed: ${blockConsumption(row)} lessons · Remaining demand: ${positive(row.requiredCount)?row.requiredCount-blockConsumption(row):'needs valid demand'}. One teacher and one room throughout.`;
    event.target.closest('[data-plan]').querySelector('.setup-plan-issues').innerHTML=allIssues(next).filter(i=>i.plan===plan.id).map(i=>`<p>${html(i.message)}</p>`).join('');return true;
  }
  return{input:edit,change:edit,click(event){
    const button=event.target.closest('button');if(!button||!button.hasAttribute('data-add-block')&&!button.hasAttribute('data-remove-block'))return false;
    const plan=rowOf(button);if(!permitted(plan))return true;
    if(button.hasAttribute('data-add-block')){const next=clone(getDraft()),row=next.plans.find(p=>p.id===plan.id);row.blockRules??=[];row.blockRules.push({id:uid(),length:'',operator:'EXACTLY',count:1});commit(next);return true;}
    const index=Number(button.dataset.removeBlock),rule=plan.blockRules[index];
    confirm('Remove consecutive block rule?',`Remove permission and requirement for runs of length ${rule.length}? Runs of that length become hard-forbidden unless another explicit rule authorizes them. Other lengths, occurrences and weekly demand are unchanged.`,()=>{const next=clone(getDraft());next.plans.find(p=>p.id===plan.id).blockRules.splice(index,1);commit(next);},'Remove block rule');return true;
  }};
}
