import { emptyDraft, readableDraft } from './setup-model.js';
import { compileDraft, definitionRevision } from './setup-compile.js';
import { validateText } from './demo-definitions.js';
const clone=v=>structuredClone(v),uid=()=>crypto.randomUUID();
export function strictParse(text){
 const value=JSON.parse(text),tokens=[...text.matchAll(/"(?:\\.|[^"\\])*"|[{}\[\]:,]|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|true|false|null/g)],stack=[];
 for(let i=0;i<tokens.length;i++){const token=tokens[i][0];if(token==='{')stack.push(new Set());else if(token==='[')stack.push(null);else if(token==='}'||token===']')stack.pop();else if(token.startsWith('"')&&tokens[i+1]?.[0]===':'){const key=JSON.parse(token),seen=stack.at(-1);if(seen?.has(key))throw new Error(`Duplicate field ${key} at character ${tokens[i].index+1}.`);seen?.add(key);}}
 return value;
}
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
  const classes=teacher.eligibleClassIdsBySubject[occurrence.subjectId]??=[];if(!classes.includes(occurrence.cohortId))classes.push(occurrence.cohortId);
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
export function createExchange({getDraft,activate,flush,notify,confirm,request,build,getAdvanced,setAdvanced,isDisposed}){
 let pending;
 async function preview(text,source){
  const origin=getDraft()?.id;
  try{
   if(new TextEncoder().encode(text).length>16*1024*1024)throw new Error('Recovery input exceeds 16 MiB.');
   const document=strictParse(text);let mapped;
   if(document.documentType==='school-setup-recovery'){
    if(document.formatVersion!==1||!readableDraft(document.draft))throw new Error('Unsupported or unreadable authoring recovery version; input retained.');
    mapped={draft:clone(document.draft),changes:['Restore the complete authoring snapshot into a separate recovered draft. Invalid and unfinished input is retained.']};
    mapped.draft.recoveredFrom=mapped.draft.id;mapped.draft.id=uid();mapped.draft.validation='Unchecked';delete mapped.draft.check;
   }else{
    validateText(text);const {value}=await request('/api/demo/definition/check',{method:'POST',body:text,headers:{'Content-Type':'text/plain'}});
    if(isDisposed()||getDraft()?.id!==origin)return;
    if(value.build!==build()||value.definitionRevision!==await definitionRevision(document))throw new Error('Source check provenance did not match.');
    mapped=mapDefinition(document,source);
    if(getDraft()?.retained)mapped.draft.section=getDraft().section;
   }
   if(isDisposed()||getDraft()?.id!==origin)return;
   const {draft,changes}=mapped;pending=draft;
   confirm('Review conversion into a separate draft',`${draft.subjects.length} subjects; ${draft.teachers.length} teachers; ${draft.classes.length} classes; ${draft.rooms.length} rooms; ${draft.grid.intervals.filter(i=>i.periodId).length} periods; ${draft.plans.reduce((n,p)=>n+(p.occurrences?.length??0),0)} individual lessons. ${changes.join(' ')} Retained settings: ${draft.plans.filter(p=>p.protectedReasons?.length||p.blockEditsProtected).length} protected/grouping-restricted rows. Existing source and working draft remain independent.`,()=>{if(isDisposed())return;flush();if(pending.conversion)pending.conversion.acceptedAt=new Date().toISOString();activate(pending,true);},'Accept conversion into new draft');
  }catch(error){notify(`Import refused: ${error.message}`);}
 }
 function exportKernel(transfer=false){
  try{flush();const definition=compileDraft(getDraft()),text=JSON.stringify(definition,null,2);confirm('Export kernel definition', 'Includes catalog 10 adjacency, explicit block policy, individual lessons and preserved kernel settings. Excludes unfinished demand, extended eligibility, named breaks/lunch and other editor-only intent. Use Download recovery for full authoring data.',()=>{try{if(transfer)setAdvanced(text);else downloadDocument(text,'school-definition.json');notify(transfer?'Explicitly transferred kernel JSON to the advanced buffer.':'Kernel JSON download requested.');}catch(error){notify(`Export failed: ${error.message}`);}},transfer?'Transfer to advanced buffer':'Download kernel JSON');}catch(error){notify(`Kernel export refused: ${error.message} Download recovery to preserve all unfinished work.`);}
 }
 return{preview,exportKernel,click(button){if(button.id==='setup-convert'){preview(JSON.stringify(getDraft().retained),getDraft().source);return true;}if(button.id==='setup-import-advanced'){preview(getAdvanced(),{id:'advanced-buffer',name:'Advanced JSON buffer',revision:null});return true;}if(button.id==='setup-export'){exportKernel();return true;}if(button.id==='setup-transfer'){exportKernel(true);return true;}return false;}};
}
export function downloadDocument(text,name){const blob=new Blob([text],{type:'application/json'}),url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
