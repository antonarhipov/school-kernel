import { baseAvailable,available,teachingPeriods } from './setup-availability.js';
const clone=value=>structuredClone(value);
export function markLunch(cohort,periodId,marked){
  const next=clone(cohort);next.lunchPeriodIds=(next.lunchPeriodIds??[]).filter(id=>id!==periodId);if(marked)next.lunchPeriodIds.push(periodId);return next;
}
export function lunchIssues(draft){
  const issues=[];
  for(const c of draft.classes){
    const periods=teachingPeriods(draft),ids=new Set(periods.map(p=>p.periodId));
    for(const id of c.lunchPeriodIds??[])if(!ids.has(id))issues.push({section:'Classes',record:c.id,message:`${c.displayName??c.name??c.id}: lunch period ${id} is missing or not teaching time.`});
    const demand=draft.plans.filter(p=>(p.classId??p.cohortId)===c.id).reduce((n,p)=>n+(Number.isInteger(p.requiredCount)&&p.requiredCount>0?p.requiredCount:0),0),capacity=periods.filter(p=>available(c,p.periodId)).length;
    if(demand>capacity)issues.push({section:'Classes',record:c.id,message:`${c.displayName??c.name??c.id}: required ${demand} lessons exceed ${capacity} available teaching periods including reserved capacity. Lunch and other hard restrictions remain; reserved capacity requires subject permission.`});
  }
  return issues;
}
export function lunchContent(draft,c,html){
  const ctx=draft.pendingInput.lunch,day=ctx.day??draft.grid.activeDays[0]??'MONDAY',rows=draft.grid.intervals.filter(i=>i.day===day);
  return `<div id="setup-class-lunch"><h3>${html(c.displayName??c.name)} · class lunch</h3><button id="setup-back-lunch" class="secondary">Class details</button>
    <p>Class lunch is a hard exclusion for this class only. It interrupts consecutive lessons and is excluded from internal-gap accounting. School-wide lunch keeps its own interval and bell times.</p>
    <nav class="setup-day-tabs" aria-label="Class lunch weekday">${draft.grid.activeDays.map(d=>`<button data-lunch-day="${d}" ${d===day?'aria-current="true"':''}>${d.slice(0,3)}</button>`).join('')}</nav>
    <div class="setup-marking-grid">${rows.map(i=>['Lesson','Reserved lesson'].includes(i.kind)?`<div class="setup-marking-cell"><button data-class-lunch="${html(i.periodId)}" aria-pressed="${c.lunchPeriodIds?.includes(i.periodId)?'true':'false'}"><span>${html(i.label)}${i.kind==='Reserved lesson'?' · Reserved':''}</span><time>${html(i.start||'Unspecified')}–${html(i.end||'Unspecified')}</time><strong>${c.lunchPeriodIds?.includes(i.periodId)?'Class lunch':'No class lunch'}</strong>${!baseAvailable(c,i.periodId)?'<small>Also unavailable from another retained class restriction; clearing lunch will keep it unavailable.</small>':''}</button></div>`:`<p>${html(i.kind)}: ${html(i.label)} <time>${html(i.start||'Unspecified')}–${html(i.end||'Unspecified')}</time> · school-wide, not a class marking</p>`).join('')||'<p>No intervals yet. Arrange Timetable grid first.</p>'}</div>
    <div role="status">${lunchIssues(draft).filter(i=>i.record===c.id).map(i=>`<p>${html(i.message)}</p>`).join('')}</div></div>`;
}
export function createLunchActions({getDraft,commit,contextOnly,notify}){
  return {click(event){
    const button=event.target.closest('button');if(!button)return false;const draft=getDraft();
    if(button.dataset.openClassLunch){const next=clone(draft);next.pendingInput.lunch={classId:button.dataset.openClassLunch};contextOnly(next);return true;}
    const ctx=draft.pendingInput.lunch;if(!ctx)return false;
    if(button.id==='setup-back-lunch'){const next=clone(draft);delete next.pendingInput.lunch;contextOnly(next);return true;}
    if(button.dataset.lunchDay){const next=clone(draft);next.pendingInput.lunch.day=button.dataset.lunchDay;contextOnly(next);return true;}
    if(button.dataset.classLunch){
      if(!teachingPeriods(draft).some(p=>p.periodId===button.dataset.classLunch)){notify('Class lunch can mark only existing teaching periods. Nothing changed.');return true;}
      const next=clone(draft),index=next.classes.findIndex(c=>c.id===ctx.classId),previous=next.classes[index],wasMarked=previous.lunchPeriodIds?.includes(button.dataset.classLunch);
      next.classes[index]=markLunch(previous,button.dataset.classLunch,!wasMarked);commit(next);
      if(wasMarked&&!baseAvailable(previous,button.dataset.classLunch))notify('Lunch was cleared. This period remains unavailable because another class restriction still applies.');
      return true;
    }
    return false;
  }};
}
