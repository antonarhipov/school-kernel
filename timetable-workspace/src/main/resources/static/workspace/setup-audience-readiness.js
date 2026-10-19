import {audienceDemand} from './setup-audience-plans.js';
import {policyCandidates} from './setup-audience-teachers.js';
import {resolveRelationships} from './setup-relationships.js';
import {splitIssues} from './setup-splits.js';
import {available,teachingPeriods} from './setup-availability.js';

// Keep inactive, unfinished declarations in authoring; publish only the attendance model needed by teaching.
export function activeAudienceClass(draft,c){
 const plans=(draft.audiencePlans??[]).filter(p=>p.classId===c.id&&p.delivery!=='whole');
 const used=[...new Set(plans.filter(p=>(p.portions??[]).some(q=>q.audienceId!==`class:${c.id}`&&q.sessionIds?.length)).map(p=>p.splitId))];
 const required=new Set(plans.map(p=>p.splitId));let relationships;
 if(used.length>1&&c.relationships){
  relationships=structuredClone(c.relationships);
  if(relationships.mode==='nested'){
   required.add(relationships.constituentSplitId);
   relationships.splitIds=relationships.splitIds.filter(id=>required.has(id));
   const parentIds=new Set((c.splits??[]).filter(s=>required.has(s.id)&&s.id!==relationships.constituentSplitId).flatMap(s=>s.groups.map(g=>g.id)));
   relationships.unions=Object.fromEntries(Object.entries(relationships.unions??{}).filter(([id])=>parentIds.has(id)));
   // The public relationship shape retains its explicit intersections list in nested mode.
  }else for(const id of relationships.splitIds)required.add(id);
 }
 const active={...c,splits:(c.splits??[]).filter(s=>required.has(s.id))};delete active.relationships;if(relationships)active.relationships=relationships;
 if(!Object.hasOwn(c,'splits') && !active.splits.length)delete active.splits;
 return {active,used,required};
}
export function audienceReadiness(draft){
 const issues=[],warnings=[],classes=new Map(),teacherDemand=new Map(),teacherSlots=new Map();
 const periods=teachingPeriods(draft),add=(p,message)=>issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message});
 const addTeacher=(id,count,slots)=>{teacherDemand.set(id,(teacherDemand.get(id)??0)+count);if(!teacherSlots.has(id))teacherSlots.set(id,new Set());for(const slot of slots)teacherSlots.get(id).add(slot);};
 for(const c of draft.classes){
  const {active,used,required}=activeAudienceClass(draft,c);
  for(const issue of splitIssues({...draft,classes:[c]}))if(!required.has(issue.split))warnings.push({...issue,message:`Unused draft arrangement: ${issue.message}`});
  let attendance;
  if(used.length>1&&(!active.relationships||used.some(id=>!active.relationships.splitIds.includes(id))))attendance={blocks:[],issues:[{section:'Classes',record:c.id,relationship:true,message:`${c.displayName}: used splits ${used.join(', ')} need a complete relationship. Reuse a split, declare nested groups or supply joint overlap counts.`}]};
  else attendance=resolveRelationships(active,used.length>1?active.relationships.splitIds:used);
  issues.push(...attendance.issues);classes.set(c.id,{...attendance,demand:new Map(),opportunities:new Map()});
  // Unrelated unfinished relationships are visible but cannot block unused arrangements.
  if(used.length<2&&c.relationships)warnings.push(...resolveRelationships(c).issues.map(i=>({...i,message:`Unused draft relationship: ${i.message}`})));
 }
 for(const p of draft.audiencePlans??[]){
  const c=draft.classes.find(c=>c.id===p.classId),s=draft.subjects.find(s=>s.id===p.subjectId),attendance=classes.get(p.classId),demand=audienceDemand(draft,p);if(!c||!s)continue;
  for(const q of p.portions){
   const row=demand.portions.find(row=>row.audienceId===q.audienceId);if(!row)continue;const candidates=policyCandidates(draft,p,q),policy=q.teacherPolicy;
   const teachers=policy.mode==='fixed'?[policy.fixedTeacherId]:policy.mode==='sharing'?[...new Set((policy.shares??[]).map(s=>s.teacherId))]:candidates;
   const policies=teacher=>(draft.roomAssignments??[]).filter(r=>r.subjectId===s.id&&(!r.teacherId||r.teacherId===teacher));
   const slotsByTeacher=new Map(),roomsByTeacher=new Map();
   for(const teacherId of teachers){const t=draft.teachers.find(t=>t.id===teacherId);if(!t||!candidates.includes(teacherId))continue;
    const suitable=draft.rooms.filter(r=>r.capacity>=row.size&&(p.requiredRoomCapabilityIds??[]).every(id=>r.capabilityIds?.includes(id))
     &&(!p.allowedRoomIds||p.allowedRoomIds.includes(r.id))&&(!q.allowedRoomIds||q.allowedRoomIds.includes(r.id))&&(!q.fixedRoomId||q.fixedRoomId===r.id)
     &&(!s.curatorLesson||!c.homeRoomId||r.id===c.homeRoomId)&&policies(teacherId).every(policy=>policy.useHomeRoom?r.id===c.homeRoomId:policy.allowedRoomIds?.includes(r.id)));
    roomsByTeacher.set(teacherId,suitable);
    slotsByTeacher.set(teacherId,periods.filter(period=>available(c,period.periodId)&&available(t,period.periodId)&&(s.reservedPeriodsAllowed||period.kind!=='Reserved lesson')&&suitable.some(r=>available(r,period.periodId))).map(p=>p.periodId));
   }
   const slots=new Set([...slotsByTeacher.values()].flat());
   if(![...roomsByTeacher.values()].some(rooms=>rooms.length))add(p,`${c.displayName} / ${s.displayName} / ${row.displayName}: no compatible room for ${row.size} students, required capabilities, inherited/narrowed restrictions and eligible teachers.`);
   if(slots.size<q.sessionIds.length)add(p,`${c.displayName} / ${s.displayName} / ${row.displayName}: ${q.sessionIds.length} sessions exceed ${slots.size} compatible available teaching periods.`);
   if(policy.mode==='automatic'&&teachers.length&&!teachers.some(id=>(slotsByTeacher.get(id)?.length??0)>=q.sessionIds.length))add(p,`${c.displayName} / ${s.displayName} / ${row.displayName}: no one eligible teacher has capacity for the automatic weekly allocation.`);
   if(policy.mode==='fixed')addTeacher(policy.fixedTeacherId,q.sessionIds.length,slotsByTeacher.get(policy.fixedTeacherId)??[]);
   if(policy.mode==='automatic'&&teachers.length===1)addTeacher(teachers[0],q.sessionIds.length,slotsByTeacher.get(teachers[0])??[]);
   if(policy.mode==='sharing')for(const share of policy.shares??[]){if(!Number.isInteger(share.count))continue;const options=slotsByTeacher.get(share.teacherId)??[];if(options.length<share.count)add(p,`${row.displayName}: fixed teacher ${share.teacherId} cannot place ${share.count} sharing sessions in ${options.length} compatible periods.`);addTeacher(share.teacherId,share.count,options);}
   for(const b of attendance?.blocks??[])if(q.audienceId===`class:${c.id}`||b.groupIds.includes(q.audienceId)){
    attendance.demand.set(b.id,(attendance.demand.get(b.id)??0)+q.sessionIds.length);if(!attendance.opportunities.has(b.id))attendance.opportunities.set(b.id,new Set());for(const slot of slots)attendance.opportunities.get(b.id).add(slot);
   }
  }
 }
 // Legacy sessions still attend every anonymous class block and consume each resource once.
 for(const p of draft.plans){const c=classes.get(p.classId??p.cohortId);for(const b of c?.blocks??[]){c.demand.set(b.id,(c.demand.get(b.id)??0)+(p.occurrences?.length??0));if(!c.opportunities.has(b.id))c.opportunities.set(b.id,new Set());for(const period of periods)if(available(draft.classes.find(c=>c.id===(p.classId??p.cohortId)),period.periodId))c.opportunities.get(b.id).add(period.periodId);}for(const allocation of p.allocations??[])if(Number.isInteger(allocation.count)){const t=draft.teachers.find(t=>t.id===allocation.teacherId);if(t)addTeacher(t.id,allocation.count,periods.filter(p=>available(t,p.periodId)).map(p=>p.periodId));}}
 for(const [id,c] of classes)for(const b of c.blocks)if((c.demand.get(b.id)??0)>(c.opportunities.get(b.id)?.size??0))issues.push({section:'Classes',record:id,relationship:true,message:`${draft.classes.find(c=>c.id===id)?.displayName}: audience ${b.groupIds.join(' / ')||'Whole class'} requires ${c.demand.get(b.id)} received lessons but has only ${c.opportunities.get(b.id)?.size??0} compatible teaching opportunities.`});
 for(const [id,count] of teacherDemand)if(count>(teacherSlots.get(id)?.size??0))issues.push({section:'Teachers',record:id,message:`Teacher ${id}: ${count} fixed/narrowed sessions exceed ${teacherSlots.get(id)?.size??0} compatible periods across the weekly curriculum.`});
 return {issues,warnings,classes,teacherDemand};
}
