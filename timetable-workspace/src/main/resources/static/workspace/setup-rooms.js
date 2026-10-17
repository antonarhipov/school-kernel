import { available,teachingPeriods } from './setup-availability.js';
const clone=value=>structuredClone(value),uid=()=>crypto.randomUUID(),label=row=>row?.displayName??row?.name??'(missing)';
let workMax=11000000;
export function configureRoomLimits(value){if(Number.isSafeInteger(value.workMax)&&value.workMax>0)workMax=value.workMax;}
export function roomIssues(draft){
  const issues=[];
  for(const r of draft.rooms){
    const add=message=>issues.push({section:'Rooms',record:r.id,message:`${label(r)||'Unnamed room'}: ${message}`});
    if(!label(r).trim())add('enter a room name.');
    if(!Number.isInteger(r.capacity)||r.capacity<0||r.capacity>2147483647)add('capacity must be a nonnegative integer.');
    if(!Array.isArray(r.capabilityIds)||r.capabilityIds.some(id=>!/^([A-Za-z0-9][A-Za-z0-9._-]{0,127})$/.test(id))||new Set(r.capabilityIds).size!==r.capabilityIds.length)add('capabilities need unique valid identifiers, separated by commas.');
  }
  for(const policy of draft.roomAssignments??[]){
    const add=message=>issues.push({section:'Subjects',record:policy.subjectId,message:`Room policy ${policy.id}: ${message}`});
    if(!policy.useHomeRoom&&(!Array.isArray(policy.allowedRoomIds)||!policy.allowedRoomIds.length))add('a restricted allowed-room pool must not be empty.');
    for(const id of policy.allowedRoomIds??[])if(!draft.rooms.some(r=>r.id===id))add(`room ${id} is missing.`);
  }
  const lessons=draft.plans.reduce((n,p)=>n+(p.allocations??[]).reduce((m,a)=>m+(Number.isInteger(a.count)&&a.count>0?a.count:0),0),0);
  const work=lessons*teachingPeriods(draft).length*draft.rooms.length;
  if(work>workMax){issues.push({section:'Classes',message:`Placement work ${work} exceeds the configured demo limit ${workMax}. Reduce lesson, period or room counts before checking placement.`});return issues;}
  for(const p of draft.plans){
    if(p.fixedRoomId&&!draft.rooms.some(r=>r.id===p.fixedRoomId))issues.push({section:'Classes',record:p.classId??p.cohortId,plan:p.id,message:`Row ${p.id}: fixed room ${p.fixedRoomId} is missing.`});
    for(const a of p.allocations??[]){
      const occurrences=(p.occurrences??[]).filter(o=>o.teacherId===a.teacherId);
      for(const occurrence of occurrences.length?occurrences:[p.occurrenceTemplate??{}])if(!suitableRooms(draft,p,a.teacherId,occurrence).length){
        const policies=(draft.roomAssignments??[]).filter(r=>r.subjectId===p.subjectId&&(!r.teacherId||r.teacherId===a.teacherId));
        issues.push({section:'Classes',record:p.classId??p.cohortId,plan:p.id,message:`Row ${p.id}, teacher ${a.teacherId}${occurrence.id?`, occurrence ${occurrence.id}`:''}: no suitable room in the intersection of capacity, capabilities, availability, fixed room ${p.fixedRoomId??'none'}, curator home-room rule and policies ${policies.map(r=>r.id).join(', ')||'none'}.`});
        break;
      }
    }
  }
  const teachers=new Map(),classes=new Map(),periods=teachingPeriods(draft);
  const collect=(map,id,count,ids)=>{let item=map.get(id);if(!item){item={count:0,periods:new Set()};map.set(id,item);}item.count+=count;for(const p of ids)item.periods.add(p);};
  for(const plan of draft.plans){
    const cohort=draft.classes.find(c=>c.id===(plan.classId??plan.cohortId)),subject=draft.subjects.find(s=>s.id===plan.subjectId);
    if(!cohort||!subject)continue;
    for(const allocation of plan.allocations??[]){
      const teacher=draft.teachers.find(t=>t.id===allocation.teacherId);if(!teacher||!Number.isInteger(allocation.count)||allocation.count<1)continue;
      const possible=new Set(),occurrences=(plan.occurrences??[]).filter(o=>o.teacherId===teacher.id);
      for(const occurrence of occurrences.length?occurrences:[plan.occurrenceTemplate??{}]){
        const rooms=suitableRooms(draft,plan,teacher.id,occurrence);
        for(const period of periods)if(available(teacher,period.periodId)&&available(cohort,period.periodId)
          &&(period.kind!=='Reserved lesson'||subject.reservedPeriodsAllowed)&&(!occurrence.periodLock||occurrence.periodLock===period.periodId)
          &&rooms.some(room=>available(room,period.periodId)))possible.add(period.periodId);
      }
      if(allocation.count>possible.size)issues.push({section:'Classes',record:cohort.id,plan:plan.id,message:`Row ${plan.id}, teacher ${teacher.id}: ${allocation.count} allocated lessons exceed ${possible.size} compatible teaching periods. Reserved permission, availability, lunch, locks and rooms all apply.`});
      collect(teachers,teacher.id,allocation.count,possible);collect(classes,cohort.id,allocation.count,possible);
    }
  }
  for(const [section,groups] of [['Teachers',teachers],['Classes',classes]])for(const [id,item] of groups)if(item.count>item.periods.size)issues.push({section,record:id,message:`${id}: ${item.count} allocated lessons exceed ${item.periods.size} distinct compatible teaching periods across the assigned curriculum.`});
  return issues;
}
export function suitableRooms(draft,plan,teacherId,occurrence={}){
  const c=draft.classes.find(c=>c.id===(plan.classId??plan.cohortId)),s=draft.subjects.find(s=>s.id===plan.subjectId),t=draft.teachers.find(t=>t.id===teacherId);
  if(!c||!s||!t)return [];
  const policies=(draft.roomAssignments??[]).filter(r=>r.subjectId===s.id&&(!r.teacherId||r.teacherId===teacherId));
  const required=occurrence.requiredRoomCapabilityIds??plan.occurrenceTemplate?.requiredRoomCapabilityIds??[];
  return draft.rooms.filter(r=>Number.isInteger(r.capacity)&&Number.isInteger(c.size)&&r.capacity>=c.size
    &&required.every(id=>r.capabilityIds?.includes(id))&&(!plan.fixedRoomId||r.id===plan.fixedRoomId)&&(!occurrence.roomLock||r.id===occurrence.roomLock)
    &&(!s.curatorLesson||!c.homeRoomId||r.id===c.homeRoomId)
    &&policies.every(policy=>policy.useHomeRoom?r.id===c.homeRoomId:policy.allowedRoomIds?.includes(r.id))
    &&teachingPeriods(draft).some(p=>available(r,p.periodId)&&available(t,p.periodId)&&available(c,p.periodId)&&(s.reservedPeriodsAllowed||p.kind!=='Reserved lesson')&&(!occurrence.periodLock||p.periodId===occurrence.periodLock)));
}
export function roomFields(draft,room,html){
  return `<label>Room capacity<input data-catalog-field="capacity" data-value-type="required-integer" inputmode="numeric" value="${html(room.capacity??'')}"></label>
    <label>Capabilities (identifiers separated by commas)<input data-room-capabilities value="${html(draft.pendingInput?.roomCapabilities?.[room.id]??(room.capabilityIds??[]).join(', '))}"></label>
    <p>Capacity and required capabilities are hard requirements. A capacity of zero is allowed. Suitable-room selection is automatic unless a hard policy restricts it.</p><button data-room-availability="${html(room.id)}" class="secondary">Room availability</button>`;
}
export function subjectRoomContent(draft,subject,html){
  const policies=(draft.roomAssignments??[]).filter(p=>p.subjectId===subject.id),schoolWide=policies.filter(p=>!p.teacherId),editable=schoolWide.length<=1&&!schoolWide[0]?.useHomeRoom;
  const policy=schoolWide[0];
  return `<details><summary>Subject room placement</summary><p>An allowed-room pool is a hard restriction shared by all classes. It intersects teacher-subject policies, capacity, required capabilities, availability and fixed-room rules.</p>
    <label>Subject room choice<select data-subject-room-mode ${editable?'':'disabled'}><option value="automatic" ${!policy?'selected':''}>Automatic suitable rooms</option><option value="restricted" ${policy?'selected':''}>Restrict to selected rooms</option></select></label>
    ${policy&&editable?`<div class="setup-eligibility-list">${draft.rooms.map(r=>`<label><input type="checkbox" data-subject-room="${html(r.id)}" ${policy.allowedRoomIds?.includes(r.id)?'checked':''}>${html(label(r))}</label>`).join('')||'<p>Add rooms before choosing a nonempty pool.</p>'}</div>`:''}
    ${!editable?'<p>These retained policies cannot be replaced faithfully by one pool. Resolve their exact values through advanced JSON first.</p>':''}
    ${policies.length?`<p>Applicable retained policy values:</p><pre>${html(JSON.stringify(policies,null,2))}</pre>`:''}</details>`;
}
export function rowRoomContent(draft,plan,html){
  const policies=(draft.roomAssignments??[]).filter(p=>p.subjectId===plan.subjectId),required=[...new Set((plan.occurrences??[]).flatMap(o=>o.requiredRoomCapabilityIds??[]))];
  return `<label>Fixed room for every weekly occurrence (hard)<select data-plan-fixed-room><option value="">Automatic suitable room</option>${plan.fixedRoomId&&!draft.rooms.some(r=>r.id===plan.fixedRoomId)?`<option value="${html(plan.fixedRoomId)}" selected>Missing: ${html(plan.fixedRoomId)}</option>`:''}${draft.rooms.map(r=>`<option value="${html(r.id)}" ${plan.fixedRoomId===r.id?'selected':''}>${html(label(r))}</option>`).join('')}</select></label>
    <p>A fixed room applies across split teachers and blocks. No soft home-room preference overrides hard room requirements. Required retained capabilities: ${html(required.join(', ')||'none')}.</p>${policies.length?`<p>Retained subject and teacher-subject policies:</p><pre>${html(JSON.stringify(policies,null,2))}</pre>`:''}`;
}
export function createRoomActions({getDraft,commit,notify,protectedReasons}){
  function subjectContext(target){const draft=getDraft(),id=target.closest('[data-catalog-record]').dataset.catalogRecord,policies=(draft.roomAssignments??[]).filter(p=>p.subjectId===id&&!p.teacherId);return{draft,id,policies};}
  return{
    input(event){if(!event.target.hasAttribute('data-room-capabilities'))return false;const next=clone(getDraft()),id=event.target.closest('[data-catalog-record]').dataset.catalogRecord,row=next.rooms.find(r=>r.id===id);next.pendingInput.roomCapabilities??={};next.pendingInput.roomCapabilities[id]=event.target.value;row.capabilityIds=event.target.value===''?[]:event.target.value.split(',').map(v=>v.trim());commit(next,false);document.querySelector('#setup-record-issues').textContent=roomIssues(next).filter(i=>i.record===id).map(i=>i.message).join(' ');return true;},
    change(event){
      if(event.target.hasAttribute('data-subject-room-mode')||event.target.dataset.subjectRoom){
        const{draft,id,policies}=subjectContext(event.target);
        if(policies.length>1||policies[0]?.useHomeRoom){notify('Retained subject policies cannot be replaced by one normal room pool. Use advanced JSON; no policy changed.');return true;}
        const next=clone(draft);next.roomAssignments??=[];
        if(event.target.hasAttribute('data-subject-room-mode')){
          if(event.target.value==='automatic')next.roomAssignments=next.roomAssignments.filter(p=>p.id!==policies[0]?.id);
          else if(!policies.length)next.roomAssignments.push({id:uid(),subjectId:id,allowedRoomIds:[]});
          commit(next);return true;
        }
        if(!policies[0])return true;
        const policy=next.roomAssignments.find(p=>p.id===policies[0].id),roomId=event.target.dataset.subjectRoom;
        policy.allowedRoomIds=(policy.allowedRoomIds??[]).filter(id=>id!==roomId);if(event.target.checked)policy.allowedRoomIds.push(roomId);
        commit(next,false);document.querySelector('#setup-record-issues').textContent=roomIssues(next).filter(i=>i.record===id).map(i=>i.message).join(' ');return true;
      }
      if(event.target.hasAttribute('data-plan-fixed-room')){
        const draft=getDraft(),plan=draft.plans.find(p=>p.id===event.target.closest('[data-plan]').dataset.plan),reasons=protectedReasons(plan);
        if(reasons.length){event.target.value=plan.fixedRoomId??'';notify(`Fixed-room change refused: ${reasons.join('; ')}. Resolve protected occurrence settings in advanced JSON first.`);return true;}
        const next=clone(draft),row=next.plans.find(p=>p.id===plan.id);
        if(event.target.value)row.fixedRoomId=event.target.value;else delete row.fixedRoomId;commit(next);return true;
      }
      return false;
    }
  };
}
