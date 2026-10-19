import { activeAudienceClass } from './setup-audience-readiness.js';
import { draftIssues } from './setup-model.js';
import { available } from './setup-availability.js';
import { allocated, planLimits } from './setup-plans.js';
import { teaching } from './setup-grid.js';
const clone=v=>structuredClone(v);
const pick=(value,keys)=>Object.fromEntries(keys.filter(k=>Object.hasOwn(value,k)).map(k=>[k,clone(value[k])]));
const common=['id','displayName'];
const subjectFields=[...common,'reservedPeriodsAllowed','maxWeeklyReservedLessonsPerCohort','dayEdgeOnly','maxDailyLessonsPerCohort','curatorLesson'];
const classFields=[...common,'size','undesirablePeriodIds','maxDailyLessonSpread','maxDailyGaps','latestStartSlot','preferredLatestStartSlot','dailyLessonSpreadLimit','curatorTeacherId','homeRoomId'];
const lessonFields=[...common,'teacherId','seriesId','requiredRoomCapabilityIds','preferredRoomIds','undesirablePeriodIds','periodLock','roomLock'];
function resource(value,keys,periods){
 const result=pick(value,keys);
 // Preserve omitted all-period intent and explicit whitelist, including empty. Materialize exclusions only when authored.
 if(Object.hasOwn(value,'availablePeriodIds')&&!value.availability&&!value.lunchPeriodIds?.length)result.availablePeriodIds=clone(value.availablePeriodIds);
 else if(value.availability?.intent==='whitelist'||Object.hasOwn(value,'availablePeriodIds')||value.availability?.unavailablePeriodIds?.length||value.lunchPeriodIds?.length)
  result.availablePeriodIds=periods.filter(p=>available(value,p.id)).map(p=>p.id);
 return result;
}
export function compileDraft(draft,{allowIssues=false}={}){
 if(draft.retained)throw new Error('Explicit conversion and adoption are required.');
 const issues=draftIssues(draft);if(!allowIssues&&issues.length)throw new Error(issues.map(i=>i.message).join(' '));
 if(Object.hasOwn(draft,'audiencePlans')||Object.hasOwn(draft,'coordinationRules')||Object.hasOwn(draft,'sessionLocks'))return compileAudienceDraft(draft);
 const definition={...(clone(draft.kernelExtras??{})),schemaVersion:1,catalogVersion:10,schoolId:draft.kernelSchoolId??draft.id,displayName:draft.name.trim()};
 definition.periods=[];
 for(const day of draft.grid.activeDays){
  const intervals=draft.grid.intervals.filter(i=>i.day===day),lessons=intervals.filter(teaching),keepOrder=lessons.every((p,n)=>Number.isInteger(p.order)&&(n===0||p.order>lessons[n-1].order));
  for(let index=0;index<lessons.length;index++){
   const interval=lessons[index],next=lessons[index+1],between=next?intervals.slice(intervals.indexOf(interval)+1,intervals.indexOf(next)):[];
   const period={id:interval.periodId,displayName:interval.label,weekday:day,order:keepOrder?interval.order:index+1,adjacentToNext:!!next&&!between.some(i=>i.kind==='Lunch')&&!interval.boundaryAfter};
   if(interval.start||interval.end){period.startTime=interval.start;period.endTime=interval.end;}
   definition.periods.push(period);
  }
 }
 const reserved=draft.grid.intervals.filter(i=>i.kind==='Reserved lesson').map(i=>i.periodId);
 if(reserved.length||Object.hasOwn(draft.kernelExtras??{},'reservedPeriodIds'))definition.reservedPeriodIds=reserved;
 definition.subjects=draft.subjects.map(s=>pick(s,subjectFields));
 definition.teachers=draft.teachers.map(t=>resource(t,[...common,'qualifiedSubjectIds','undesirablePeriodIds'],definition.periods));
 definition.cohorts=draft.classes.map(c=>resource(c,classFields,definition.periods));
 definition.rooms=draft.rooms.map(r=>resource(r,[...common,'capacity','capabilityIds'],definition.periods));
 definition.lessons=draft.plans.flatMap(plan=>(plan.occurrences??[]).map(occurrence=>({...pick(occurrence,lessonFields),subjectId:plan.subjectId,cohortId:plan.classId??plan.cohortId,...(plan.fixedRoomId?{roomLock:plan.fixedRoomId}:{})})));
 const rules=draft.plans.flatMap(p=>(p.blockRules??[]).map(r=>({id:r.id,cohortId:p.classId??p.cohortId,subjectId:p.subjectId,length:r.length,operator:r.operator,weeklyCount:r.count})));
 if(rules.length||Object.hasOwn(draft.kernelExtras??{},'consecutiveBlocks'))definition.consecutiveBlocks=rules;
 if(draft.roomAssignments)definition.roomAssignments=clone(draft.roomAssignments);
 for(const plan of draft.plans){
  if((plan.occurrences??[]).length!==allocated(plan))throw new Error(`Row ${plan.id}: occurrences do not match allocations.`);
  for(const a of plan.allocations??[])if(plan.occurrences.filter(o=>o.teacherId===a.teacherId).length!==a.count)throw new Error(`Row ${plan.id}: occurrence teacher counts do not match allocations.`);
 }
 const text=JSON.stringify(definition),{lessonMax,workMax}=planLimits();
 if(new TextEncoder().encode(text).length>2*1024*1024)throw new Error('Definition exceeds the 2 MiB UTF-8 limit.');
 if(definition.lessons.length>lessonMax||definition.lessons.length*definition.periods.length*definition.rooms.length>workMax)throw new Error(`Definition exceeds configured limits: ${lessonMax} lessons; placement work ${workMax}.`);
 return definition;
}
function compileAudienceDraft(draft){
 const legacy=clone(draft);delete legacy.audiencePlans;delete legacy.coordinationRules;delete legacy.sessionLocks;
 const definition=compileDraft(legacy,{allowIssues:true});
 for(const period of definition.periods){const interval=draft.grid.intervals.find(i=>i.periodId===period.id),sameDay=draft.grid.intervals.filter(i=>i.day===interval.day),index=sameDay.indexOf(interval),next=sameDay.slice(index+1).find(teaching);if(Object.hasOwn(interval,'gapBoundaryAfter'))period.gapBoundaryAfter=interval.gapBoundaryAfter;else if(!!next&&sameDay.slice(index+1,sameDay.indexOf(next)).some(i=>i.kind==='Lunch'))period.gapBoundaryAfter=true;}
 definition.schemaVersion=2;definition.catalogVersion=11;definition.audiencePlans=clone(draft.audiencePlans??[]);
 // Adoption provenance belongs to the full school document, outside the public scheduling contract.
 for(const plan of definition.audiencePlans)delete plan.sourceFixedPlan;
 // Legacy gap defaults are materialized when audience teaching is adopted.
 // Compilation must preserve omitted settings, including after document import.
 if(Object.hasOwn(draft,'coordinationRules'))definition.coordinationRules=clone(draft.coordinationRules);
 if(Object.hasOwn(draft,'sessionLocks'))definition.sessionLocks=clone(draft.sessionLocks);
 for(const c of definition.cohorts){const authored=activeAudienceClass(draft,draft.classes.find(row=>row.id===c.id)).active;for(const key of ['splits','relationships'])if(Object.hasOwn(authored,key))c[key]=clone(authored[key]);}
 for(const t of definition.teachers){const authored=draft.teachers.find(row=>row.id===t.id);if(Object.hasOwn(authored,'eligibleClassIdsBySubject'))t.eligibleClassIdsBySubject=clone(authored.eligibleClassIdsBySubject);if(Object.hasOwn(authored,'referenceWeeklyMaximum'))t.referenceWeeklyMaximum=authored.referenceWeeklyMaximum;}
 const sessions=definition.lessons.length+definition.audiencePlans.reduce((n,p)=>n+p.portions.reduce((m,q)=>m+q.sessionIds.length,0),0),{lessonMax,workMax}=planLimits();
 if(sessions>lessonMax||sessions*definition.periods.length*definition.rooms.length>workMax)throw new Error('Audience definition exceeds the configured session/placement-work limits.');
 if(new TextEncoder().encode(JSON.stringify(definition)).length>2*1024*1024)throw new Error('Audience definition exceeds the 2 MiB UTF-8 limit.');
 return definition;
}
// Exact kernel canonical definition revision; mirrors RevisionService normalization.
export async function definitionRevision(value){
 const d=clone(value),entities=['subjects','teachers','cohorts','rooms','periods','lessons'];
 const sort=(v,key)=>{if(Array.isArray(v[key]))v[key].sort();};
 for(const key of entities){d[key]?.sort((a,b)=>a.id<b.id?-1:a.id>b.id?1:0);for(const row of d[key]??[])for(const field of ['qualifiedSubjectIds','availablePeriodIds','undesirablePeriodIds','capabilityIds','requiredRoomCapabilityIds','preferredRoomIds'])sort(row,field);}
 d.softConstraintOverrides?.sort((a,b)=>a.constraintId<b.constraintId?-1:a.constraintId>b.constraintId?1:0);sort(d,'reservedPeriodIds');
 const canonical=v=>Array.isArray(v)?v.map(canonical):v&&typeof v==='object'?Object.fromEntries(Object.keys(v).sort().map(k=>[k,canonical(v[k])])):v;
 const digest=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(JSON.stringify(canonical(d))));return 'sha256:'+Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,'0')).join('');
}
