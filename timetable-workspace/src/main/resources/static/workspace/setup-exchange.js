import { schoolDocument, parseSchoolDocument, importSchoolDocument } from './school-document.js';
import { emptyDraft, readableDraft } from './setup-model.js';
import { definitionRevision } from './setup-compile.js';
import { validateText, syntaxFeedback } from './demo-definitions.js';
const clone=v=>structuredClone(v),uid=()=>crypto.randomUUID();
import { strictParse } from './strict-json.js';
export { strictParse } from './strict-json.js';
export function mapDefinition(definition,source=null){
 if(!Number.isInteger(definition.catalogVersion)||definition.catalogVersion<1||definition.catalogVersion>10)throw new Error('Unsupported scheduling catalog.');
 if(Object.hasOwn(definition,'basedOnRevision'))throw new Error('/basedOnRevision: repair ancestry cannot be converted for initial generation.');
 const draft=emptyDraft(),changes=[];draft.name=definition.displayName;draft.source=source;draft.kernelSchoolId=definition.schoolId;
 draft.kernelExtras=Object.fromEntries(Object.entries(clone(definition)).filter(([k])=>!['subjects','teachers','cohorts','rooms','periods','lessons','roomAssignments','consecutiveBlocks','displayName','catalogVersion'].includes(k)));
 draft.importedDefinition=clone(definition);draft.validation='Unchecked';draft.section='Review';
 draft.subjects=clone(definition.subjects);draft.teachers=clone(definition.teachers);draft.classes=clone(definition.cohorts);draft.rooms=clone(definition.rooms);
 if(Object.hasOwn(definition,'roomAssignments'))draft.roomAssignments=clone(definition.roomAssignments);
 if(Object.hasOwn(definition,'consecutiveBlocks'))draft.kernelExtras.consecutiveBlocks=[];
 const ordered=[...definition.periods].sort((a,b)=>['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].indexOf(a.weekday)-['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].indexOf(b.weekday)||a.order-b.order);
 draft.grid.activeDays=[...new Set(ordered.map(p=>p.weekday))];
 draft.grid.intervals=ordered.map(p=>({id:uid(),periodId:p.id,day:p.weekday,order:p.order,kind:definition.reservedPeriodIds?.includes(p.id)?'Reserved lesson':'Lesson',label:p.displayName,start:p.startTime??'',end:p.endTime??'',unspecifiedTimes:!Object.hasOwn(p,'startTime'),...(definition.catalogVersion===10&&!p.adjacentToNext?{boundaryAfter:true}:{})}));
 for(const teacher of draft.teachers)teacher.eligibleClassIdsBySubject={};
 const groups=new Map();
 for(const occurrence of definition.lessons){
  const key=JSON.stringify([occurrence.cohortId,occurrence.subjectId]);let plan=groups.get(key);
  if(!plan){plan={id:uid(),classId:occurrence.cohortId,subjectId:occurrence.subjectId,requiredCount:0,allocations:[],occurrences:[],blockRules:[],nextOccurrence:0};groups.set(key,plan);}
  plan.requiredCount++;plan.occurrences.push(clone(occurrence));
  let allocation=plan.allocations.find(a=>a.teacherId===occurrence.teacherId);if(!allocation){allocation={teacherId:occurrence.teacherId,count:0};plan.allocations.push(allocation);}allocation.count++;
  const teacher=draft.teachers.find(t=>t.id===occurrence.teacherId);if(!teacher)throw new Error(`Missing teacher ${occurrence.teacherId}`);
  if(!Object.hasOwn(teacher.eligibleClassIdsBySubject,occurrence.subjectId))teacher.eligibleClassIdsBySubject={...teacher.eligibleClassIdsBySubject,[occurrence.subjectId]:[]};
  const classes=teacher.eligibleClassIdsBySubject[occurrence.subjectId];if(!classes.includes(occurrence.cohortId))classes.push(occurrence.cohortId);
 }
 draft.plans=[...groups.values()];
 for(const plan of draft.plans){
  const template=o=>Object.fromEntries(Object.entries(o).filter(([k])=>!['id','displayName','teacherId','subjectId','cohortId'].includes(k)));
  const templates=plan.occurrences.map(template),series=[...new Set(plan.occurrences.map(o=>o.seriesId))];
  if(series.length===1&&series[0]!==undefined)plan.seriesId=series[0];
  if(templates.every(t=>JSON.stringify(t)===JSON.stringify(templates[0])))plan.occurrenceTemplate=clone(templates[0]);
  else plan.protectedReasons=['Imported occurrences have differing restrictions or mixed series membership. Retained individual values must be resolved through advanced JSON.'];
  plan.blockRules=(definition.consecutiveBlocks??[]).filter(r=>r.cohortId===plan.classId&&r.subjectId===plan.subjectId).map(r=>({id:r.id,length:r.length,operator:r.operator,count:r.weeklyCount}));
  if(series.length!==1||series[0]===undefined)plan.blockEditsProtected='Imported series grouping cannot support block scoring without regrouping. Use advanced JSON.';
 }
 if(definition.catalogVersion<10){
  changes.push('Adopt catalog 10: consecutive runs of two or more are forbidden unless an explicit exact-length rule is declared. No block rules are inferred. Ordered teaching opportunities are adjacent; JSON alone supplies no named break/lunch intent.');
  if(definition.catalogVersion<5){for(const cohort of draft.classes)if(!Object.hasOwn(cohort,'maxDailyGaps'))cohort.maxDailyGaps=2147483647;changes.push('Materialize omitted maximum daily gaps as Unlimited (2147483647), preserving the older hard behavior.');}
  const weights=['soft.teacher-gap','soft.cohort-gap','soft.cohort-late-start','soft.cohort-week-balance','soft.series-same-day','soft.undesirable-period','soft.non-preferred-room'];
  const disabled=weights.filter(id=>definition.catalogVersion===1&&['soft.cohort-gap','soft.cohort-late-start','soft.cohort-week-balance'].includes(id)||definition.catalogVersion===2&&id==='soft.cohort-late-start');
  if(disabled.length){draft.kernelExtras.softConstraintOverrides??=[];for(const id of disabled)if(!draft.kernelExtras.softConstraintOverrides.some(w=>w.constraintId===id))draft.kernelExtras.softConstraintOverrides.push({constraintId:id,weight:0});changes.push(`Materialize inactive older preferences at weight zero: ${disabled.join(', ')}.`);}
 }
 changes.push('Preserve identifiers, exact individual lessons, optional fields and explicit empty sets. Reconstruct only observed teacher/class eligibility. Missing times remain unspecified. JSON supplies no extended eligibility, unassigned demand or named class lunch.');
 draft.conversion={fromCatalog:definition.catalogVersion,toCatalog:10,changes:clone(changes),acceptedAt:null};
 return{draft,changes};
}
export function createExchange({getDraft,activate,flush,notify,confirm,request,build,isDisposed}){
 async function preview(text,source){
  const origin=getDraft()?.id, revision=getDraft()?.revision;
  try{
   if(new TextEncoder().encode(text).length>16*1024*1024)throw new Error('Recovery input exceeds 16 MiB.');
   if(getDraft()?.advanced?.dirty && source?.id!=='pasted-json')throw new Error('Apply or discard unapplied JSON changes before importing.');
   const syntax=syntaxFeedback(text);if(syntax)throw new Error(syntax);
   const document=strictParse(text);if(!document||typeof document!=='object'||Array.isArray(document))throw new Error('/: Expected a JSON object.');let mapped;
   if(document.documentType==='school-setup-recovery'){
    if(document.formatVersion!==1||!readableDraft(document.draft))throw new Error('Unsupported or unreadable authoring recovery version; input retained.');
    mapped={draft:clone(document.draft),changes:['Restore the complete authoring snapshot into a separate recovered draft. Invalid and unfinished input is retained.']};
    mapped.draft.recoveredFrom=mapped.draft.id;mapped.draft.id=uid();mapped.draft.validation='Unchecked';delete mapped.draft.check;
   }else if(document.documentType==='school-setup'){
    mapped={draft:importSchoolDocument(parseSchoolDocument(text)),changes:['Import the complete school document without changing its authored settings. Incomplete work remains a draft.']};
   }else{
    if(Object.hasOwn(document,'documentType'))throw new Error('/documentType: Unsupported school document format.');
    validateText(text);const {value}=await request('/api/demo/definition/check',{method:'POST',body:text,headers:{'Content-Type':'text/plain'}});
    if(isDisposed()||getDraft()?.id!==origin||getDraft()?.revision!==revision)return;
    if(value.build!==build()||value.definitionRevision!==await definitionRevision(document))throw new Error('Source check provenance did not match.');
    mapped=mapDefinition(document,source);
    if(getDraft()?.retained)mapped.draft.section=getDraft().section;
   }
   if(isDisposed()||getDraft()?.id!==origin||getDraft()?.revision!==revision)return;
   const {draft,changes}=mapped;
   confirm('Review conversion into a separate draft',`${draft.subjects.length} subjects; ${draft.teachers.length} teachers; ${draft.classes.length} classes; ${draft.rooms.length} rooms; ${draft.grid.intervals.filter(i=>i.periodId).length} periods; ${draft.plans.reduce((n,p)=>n+(p.occurrences?.length??0),0)} individual lessons. ${changes.join(' ')} Retained settings: ${draft.plans.filter(p=>p.protectedReasons?.length||p.blockEditsProtected).length} protected/grouping-restricted rows. Existing source and working draft remain independent.`,()=>{if(isDisposed()||getDraft()?.id!==origin||getDraft()?.revision!==revision){notify('Setup changed during import preview. Preview the file again.');return;}if(!flush())return;if(draft.conversion)draft.conversion.acceptedAt=new Date().toISOString();activate(draft,true);},'Accept conversion into new draft');
  }catch(error){notify(`Import refused: ${error.message}`);}
 }
 function exportDocument(){
  try{
   if(getDraft().advanced?.dirty)throw new Error('Apply or discard unapplied JSON before export, or download recovery to preserve it.');
   downloadDocument(JSON.stringify(schoolDocument(getDraft()),null,2),'school-setup.json');
   notify('Complete school JSON download requested. Incomplete authoring settings are included.');
  }catch(error){notify(`Export refused: ${error.message}`);}
 }
 return{preview,exportDocument,click(button){
  if(button.id==='setup-convert'){preview(JSON.stringify(getDraft().retained),getDraft().source);return true;}
  if(button.id==='setup-export'){exportDocument();return true;}return false;
 }};
}
export function downloadDocument(text,name){const blob=new Blob([text],{type:'application/json'}),url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
