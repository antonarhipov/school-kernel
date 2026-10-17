const clone=value=>structuredClone(value);
export const markings=['Available','Unavailable','Prefer to avoid'];
export const teachingPeriods=draft=>draft.grid.intervals.filter(i=>['Lesson','Reserved lesson'].includes(i.kind));
export function baseAvailable(entity,periodId){
  if(entity.availability?.intent==='whitelist')return entity.availability.availablePeriodIds?.includes(periodId)??false;
  if(entity.availability?.intent==='all')return !entity.availability.unavailablePeriodIds?.includes(periodId);
  return !Object.hasOwn(entity,'availablePeriodIds')||entity.availablePeriodIds.includes(periodId);
}
export const available=(entity,periodId)=>!entity.lunchPeriodIds?.includes(periodId)&&baseAvailable(entity,periodId);
export const avoided=(entity,periodId)=>!!entity.undesirablePeriodIds?.includes(periodId);
export function markAvailability(entity,periodIds,marking){
  if(!markings.includes(marking))throw new Error('Choose a supported availability marking.');
  const next=clone(entity),unavailable=marking==='Unavailable';
  if(!next.availability){next.availability=Object.hasOwn(next,'availablePeriodIds')?{intent:'whitelist',availablePeriodIds:clone(next.availablePeriodIds)}:{intent:'all',unavailablePeriodIds:[]};}
  for(const id of periodIds){
    if(next.availability.intent==='whitelist'){
      next.availability.availablePeriodIds=(next.availability.availablePeriodIds??[]).filter(p=>p!==id);
      if(!unavailable)next.availability.availablePeriodIds.push(id);
      if(Object.hasOwn(next,'availablePeriodIds'))next.availablePeriodIds=clone(next.availability.availablePeriodIds);
    }else{
      next.availability.unavailablePeriodIds=(next.availability.unavailablePeriodIds??[]).filter(p=>p!==id);
      if(unavailable)next.availability.unavailablePeriodIds.push(id);
    }
    if(Object.hasOwn(next,'undesirablePeriodIds')||marking==='Prefer to avoid'){
      next.undesirablePeriodIds=(next.undesirablePeriodIds??[]).filter(p=>p!==id);
      if(marking==='Prefer to avoid')next.undesirablePeriodIds.push(id);
    }
  }
  return next;
}
export function availabilityCapacityIssues(draft,teacher){
  const load=draft.plans.reduce((sum,p)=>sum+(p.allocations??[]).filter(a=>a.teacherId===teacher.id&&Number.isInteger(a.count)&&a.count>0).reduce((n,a)=>n+a.count,0),0);
  const capacity=teachingPeriods(draft).filter(p=>available(teacher,p.periodId)).length;
  return load>capacity?[{section:'Teachers',record:teacher.id,message:`${teacher.displayName??teacher.name??teacher.id}: ${load} allocated lessons exceed ${capacity} hard-available teaching periods. Allocations and markings are retained.`}]:[];
}
export function availabilityContent(draft,kind,entity,html){
  const ctx=draft.pendingInput.availability,day=ctx.day??draft.grid.activeDays[0]??'MONDAY',periods=teachingPeriods(draft).filter(p=>p.day===day),tool=ctx.tool??'Unavailable';
  const state=p=>!available(entity,p.periodId)?`Unavailable${avoided(entity,p.periodId)?' · also Prefer to avoid (retained)':''}`:avoided(entity,p.periodId)?'Prefer to avoid':'Available';
  return `<div id="setup-availability" data-availability-kind="${kind}" data-availability-entity="${html(entity.id)}"><h3>${html(entity.displayName??entity.name)} · availability and preferences</h3>
    <button id="setup-back-availability" class="secondary">Back to profile</button>
    <p>Unavailable is a hard exclusion. Prefer to avoid is advisory and still permits lessons. Available clears both authored restrictions in selected periods. Reserved permission remains separate.</p>
    <p>Imported simultaneous hard and soft facts are shown together; explicitly replacing a marking changes both facts for those periods.</p>
    <label>Marking tool<select id="setup-availability-tool">${(kind==='rooms'?markings.slice(0,2):markings).map(m=>`<option ${tool===m?'selected':''}>${m}</option>`).join('')}</select></label>
    <nav class="setup-day-tabs" aria-label="Availability weekday">${draft.grid.activeDays.map(d=>`<button data-availability-day="${d}" ${day===d?'aria-current="true"':''}>${d.slice(0,3)}</button>`).join('')}</nav>
    <h4>${day}</h4><div class="setup-marking-grid">${periods.map(p=>`<div class="setup-marking-cell"><label><input type="checkbox" data-select-mark="${html(p.periodId)}" ${(ctx.selected??[]).includes(p.periodId)?'checked':''}>Select ${html(p.label)}</label>
      <button data-mark-period="${html(p.periodId)}" aria-label="${html(p.label)} ${html(p.start||'Time unspecified')} ${html(state(p))}"><span>${html(p.label)}${p.kind==='Reserved lesson'?' · Reserved':''}</span><time>${html(p.start||'Unspecified')}–${html(p.end||'Unspecified')}</time><strong>${html(state(p))}</strong></button></div>`).join('')||'<p>No teaching opportunities on this day. Breaks and lunch cannot be marked. Add teaching periods in Timetable grid.</p>'}</div>
    <div class="setup-actions"><button id="setup-mark-selected" class="secondary">Apply to selected periods (${ctx.selected?.length??0})</button><button id="setup-mark-day" class="secondary">Apply to whole day</button></div>
    <fieldset><legend>Defined afternoon range on this day</legend><label>From<input id="setup-afternoon-start" placeholder="HH:MM" value="${html(ctx.start??'13:00')}"></label><label>Until<input id="setup-afternoon-end" placeholder="HH:MM" value="${html(ctx.end??'23:59')}"></label><button id="setup-mark-afternoon" class="secondary">Apply to afternoon range</button></fieldset>
    <div role="status">${kind==='teachers'?availabilityCapacityIssues(draft,entity).map(i=>`<p>${html(i.message)}</p>`).join(''):''}</div></div>`;
}
export function createAvailabilityActions({getDraft,commit,contextOnly,notify}){
  function apply(ids){
    const draft=getDraft(),ctx=draft.pendingInput.availability,known=new Set(teachingPeriods(draft).map(p=>p.periodId)),valid=[...new Set(ids)].filter(id=>known.has(id));
    if(!valid.length){notify('No teaching periods in this selection or timed range. No marking changed. Return to Timetable grid to add teaching opportunities.');return;}
    if(ctx.kind==='rooms'&&ctx.tool==='Prefer to avoid'){notify('Rooms support hard availability only. Choose Available or Unavailable; nothing changed.');return;}
    const next=clone(draft),index=next[ctx.kind].findIndex(e=>e.id===ctx.id);
    next[ctx.kind][index]=markAvailability(next[ctx.kind][index],valid,ctx.tool??'Unavailable');commit(next);
  }
  return {
    input(event){
      if(!['setup-afternoon-start','setup-afternoon-end'].includes(event.target.id))return false;
      const next=clone(getDraft());next.pendingInput.availability[event.target.id==='setup-afternoon-start'?'start':'end']=event.target.value;contextOnly(next,false);return true;
    },
    change(event){
      if(event.target.id!=='setup-availability-tool'&&!event.target.dataset.selectMark)return false;
      const next=clone(getDraft()),ctx=next.pendingInput.availability;
      if(event.target.id==='setup-availability-tool')ctx.tool=event.target.value;
      else{ctx.selected=(ctx.selected??[]).filter(id=>id!==event.target.dataset.selectMark);if(event.target.checked)ctx.selected.push(event.target.dataset.selectMark);}
      contextOnly(next,false);
      const count=document.querySelector('#setup-mark-selected');if(count)count.textContent=`Apply to selected periods (${ctx.selected?.length??0})`;return true;
    },
    click(event){
      const button=event.target.closest('button');if(!button)return false;const draft=getDraft();
      if(button.dataset.teacherAvailability){const next=clone(draft);next.pendingInput.availability={kind:'teachers',id:button.dataset.teacherAvailability};contextOnly(next);return true;}
      if(button.dataset.roomAvailability){const next=clone(draft);next.pendingInput.availability={kind:'rooms',id:button.dataset.roomAvailability};contextOnly(next);return true;}
      if(button.id==='setup-back-availability'){const next=clone(draft);delete next.pendingInput.availability;contextOnly(next);return true;}
      const ctx=draft.pendingInput.availability;if(!ctx)return false;
      if(button.dataset.availabilityDay){const next=clone(draft);next.pendingInput.availability.day=button.dataset.availabilityDay;contextOnly(next);return true;}
      if(button.dataset.markPeriod){apply([button.dataset.markPeriod]);return true;}
      const day=ctx.day??draft.grid.activeDays[0]??'MONDAY',periods=teachingPeriods(draft).filter(p=>p.day===day);
      if(button.id==='setup-mark-selected'){apply(ctx.selected??[]);return true;}
      if(button.id==='setup-mark-day'){apply(periods.map(p=>p.periodId));return true;}
      if(button.id==='setup-mark-afternoon'){
        const start=ctx.start??'13:00',end=ctx.end??'23:59',time=/^([01]\d|2[0-3]):[0-5]\d$/;
        if(!time.test(start)||!time.test(end)||end<=start){notify('Enter an afternoon range with valid HH:MM times and an end after its start. No marking changed.');return true;}
        apply(periods.filter(p=>p.start&&p.start>=start&&p.start<end).map(p=>p.periodId));return true;
      }
      return false;
    }
  };
}
