import {audienceDemand} from './setup-audience-plans.js';
export const coordinationTypes={START_TOGETHER:'Start together',SAME_DAY:'Same day',DIFFERENT_DAYS:'Different days',ALLOWED:'Allowed days/periods',FIXED:'Fixed slot',COVERAGE:'Strict class coverage',GAP_FREE:'Gap-free student day'};
const uid=()=>crypto.randomUUID(),own=(v,k)=>Object.hasOwn(v??{},k),positive=v=>Number.isInteger(v)&&v>0;
export function activityCatalog(draft){
 return (draft.audiencePlans??[]).flatMap(p=>p.portions.map(q=>({plan:p,portion:q,id:q.id,classId:p.classId,
  name:`${draft.classes.find(c=>c.id===p.classId)?.displayName??p.classId} / ${draft.subjects.find(s=>s.id===p.subjectId)?.displayName??p.subjectId} / ${q.audienceId===`class:${p.classId}`?'Whole class':draft.classes.find(c=>c.id===p.classId)?.splits?.flatMap(s=>s.groups).find(g=>g.id===q.audienceId)?.displayName??q.audienceId}`,
  blockRules:q.blockRules??p.blockRules??[]})));
}
export function coordinationIssues(draft){
 const issues=[],activities=activityCatalog(draft),ids=new Set(),periods=draft.grid.intervals.filter(i=>['Lesson','Reserved lesson'].includes(i.kind));
 const add=(rule,message)=>issues.push({section:'Classes',record:rule.classId??activities.find(a=>rule.activities?.some(r=>r.portionId===a.id))?.classId,coordination:rule.id,message});
 for(const p of draft.audiencePlans??[]){
  const d=audienceDemand(draft,p);
  if((p.requiredRoomCapabilityIds??[]).some(id=>!/^([A-Za-z0-9][A-Za-z0-9._-]{0,127})$/.test(id))||new Set(p.requiredRoomCapabilityIds??[]).size!==(p.requiredRoomCapabilityIds??[]).length)issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:'Required capabilities need distinct valid identifiers.'});
  for(const q of p.portions){
   const label=activities.find(a=>a.id===q.id)?.name??q.id, rules=q.blockRules??p.blockRules??[];
   let consumed=0;const lengths=new Set();
   for(const r of rules){if(!positive(r.length)||r.length<2||!positive(r.count)||!['AT_LEAST','EXACTLY'].includes(r.operator)){issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: exact block length ≥2, positive frequency and At least/Exactly are required.`});continue;}
    if(lengths.has(r.length))issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: duplicate consecutive length ${r.length}.`});lengths.add(r.length);consumed+=r.length*r.count;
    if(q.teacherPolicy.mode==='sharing'&&p.delivery!=='mixed'){
     const totals=new Map();for(const share of q.teacherPolicy.shares??[])if(Number.isInteger(share.count))totals.set(share.teacherId,(totals.get(share.teacherId)??0)+share.count);
     if([...totals.values()].reduce((n,total)=>n+Math.floor(total/r.length),0)<r.count)issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: fixed teacher portions cannot provide ${r.count} complete blocks of length ${r.length}.`});
    }
    const subject=draft.subjects.find(s=>s.id===p.subjectId);if(Number.isInteger(subject?.maxDailyLessonsPerCohort)&&subject.maxDailyLessonsPerCohort<r.length)issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: subject daily maximum is below required block length.`});
   }
   const received=q.audienceId===`class:${p.classId}`?d.whole:d.rows.find(row=>row.audienceId===q.audienceId)?.total;
   if(Number.isInteger(received)&&consumed>received)issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: required blocks consume ${consumed}, above ${received} received periods.`});
   const inherited=p.allowedRoomIds??draft.rooms.map(r=>r.id),allowed=own(q,'allowedRoomIds')?inherited.filter(id=>q.allowedRoomIds.includes(id)):inherited;
   if(!allowed.length||q.fixedRoomId&&!allowed.includes(q.fixedRoomId))issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: empty inherited/narrowed room intersection or fixed room outside it.`});
   if(inherited.some(id=>!draft.rooms.some(room=>room.id===id)))issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: unknown inherited allowed room reference.`});
   if((q.allowedRoomIds??[]).some(id=>!inherited.includes(id)))issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message:`${label}: narrowed room is outside the inherited allowed set.`});
  }
 }
 for(const r of draft.coordinationRules??[]){
  if(ids.has(r.id))add(r,'Duplicate coordination identity.');ids.add(r.id);
  if(!coordinationTypes[r.type])add(r,'Unknown hard coordination control.');
  if(r.type==='GAP_FREE'){
   if(!draft.classes.some(c=>c.id===r.classId))add(r,'Choose the class for the all-block gap-free rule.');
   if(own(r,'days')&&(!r.days.length||r.days.some(day=>!draft.grid.activeDays.includes(day))))add(r,'Gap-free days must be nonempty active weekdays; omitted means every active day.');continue;
  }
  if(!positive(r.count))add(r,'Coordination count must be a positive integer.');
  if(!r.activities?.length)add(r,'Select at least one teaching activity.');
  if(['START_TOGETHER','SAME_DAY','DIFFERENT_DAYS','COVERAGE'].includes(r.type)&&r.activities?.length<2)add(r,'This coordination type requires at least two selected activities.');
  const seen=new Set();
  for(const ref of r.activities??[]){
   const a=activities.find(a=>a.id===ref.portionId),key=ref.portionId;if(seen.has(key))add(r,'The same activity is selected twice.');seen.add(key);
   if(!['SESSION','BLOCK'].includes(ref.unit))add(r,'Choose a supported activity unit.');
   if(!a){add(r,`Missing activity ${ref.portionId}. Correct the reference explicitly.`);continue;}
   const block=ref.unit==='BLOCK'?a.blockRules.find(b=>b.id===ref.blockRuleId):null;
   const available=ref.unit==='BLOCK'?block?.count:a.portion.sessionIds.length;
   if(ref.unit==='BLOCK'&&!block)add(r,`Required consecutive block ${ref.blockRuleId} is missing from ${a.name}.`);
   if(positive(r.count)&&(!Number.isInteger(available)||r.count>available))add(r,`${a.name}: requested ${r.count} distinct rounds exceeds ${available??0} available activity occurrences.`);
   if(r.type==='FIXED'&&block){const start=periods.find(p=>p.periodId===r.periodId),day=start?periods.filter(p=>p.day===start.day):[],index=day.indexOf(start);
    if(!start||start.day!==r.day||index+block.length>day.length||day.slice(index,index+block.length-1).some(p=>p.boundaryAfter)||day.slice(index,index+block.length).some((p,n)=>n>0&&draft.grid.intervals.slice(draft.grid.intervals.indexOf(day[index+n-1])+1,draft.grid.intervals.indexOf(p)).some(i=>i.kind==='Lunch')))add(r,`${a.name}: fixed slot cannot contain the complete required block.`);
   }
  }
  if(r.type==='DIFFERENT_DAYS'&&r.activities?.length>draft.grid.activeDays.length)add(r,'Different-days participants exceed the available distinct weekdays.');
  if(r.type==='FIXED'&&(r.count!==1||!periods.some(p=>p.periodId===r.periodId&&p.day===r.day)))add(r,'A fixed-slot declaration selects one occurrence and a known matching weekday/teaching period. Declare repeating fixed occurrences separately.');
  if(r.type==='ALLOWED')for(const [key,valid] of [['days',draft.grid.activeDays],['periodIds',periods.map(p=>p.periodId)]])if(own(r,key)&&(!r[key].length||r[key].some(v=>!valid.includes(v))))add(r,`Allowed ${key} must be nonempty known values; omitted means unrestricted.`);
  if(r.type==='COVERAGE'&&new Set((r.activities??[]).map(ref=>activities.find(a=>a.id===ref.portionId)?.classId)).size>1)add(r,'Strict class coverage selects teaching plans of one parent class.');
 }
 const forced=new Map(),key=ref=>JSON.stringify([ref.portionId,ref.unit,ref.blockRuleId??'']);
 const occurrenceCount=ref=>{const a=activities.find(a=>a.id===ref.portionId);return ref.unit==='BLOCK'?a?.blockRules.find(b=>b.id===ref.blockRuleId)?.count:a?.portion.sessionIds.length;};
 for(const r of draft.coordinationRules??[])if(r.type==='FIXED'&&r.count===1)for(const ref of r.activities??[])if(occurrenceCount(ref)===1){
  const prior=forced.get(key(ref));if(prior&&(prior.day!==r.day||prior.periodId!==r.periodId))add(r,'The only available occurrence is fixed to contradictory slots.');forced.set(key(ref),r);
 }
 for(const r of draft.coordinationRules??[]){
  const fixed=(r.activities??[]).map(ref=>forced.get(key(ref)));
  if(r.type==='ALLOWED')for(const [n,ref] of (r.activities??[]).entries())if(r.count===occurrenceCount(ref)&&fixed[n]&&((own(r,'days')&&!r.days.includes(fixed[n].day))||(own(r,'periodIds')&&!r.periodIds.includes(fixed[n].periodId))))add(r,'The required allowed set excludes a fixed occurrence.');
  if(r.count===1&&fixed.length>1&&fixed.every(Boolean)){
   if(r.type==='START_TOGETHER'&&new Set(fixed.map(f=>f.periodId)).size>1)add(r,'Start-together contradicts the selected fixed starts.');
   if(r.type==='SAME_DAY'&&new Set(fixed.map(f=>f.day)).size>1)add(r,'Same-day contradicts the selected fixed weekdays.');
   if(r.type==='DIFFERENT_DAYS'&&new Set(fixed.map(f=>f.day)).size!==fixed.length)add(r,'Different-days contradicts the selected fixed weekdays.');
  }
 }
 return issues;
}
const blockFields=(rules,scope,html)=>`${rules.map(r=>`<fieldset data-audience-block="${html(r.id)}" ${scope?`data-rule-portion="${html(scope)}"`:''}><legend>Required consecutive block</legend><label>Exact length<input data-audience-block-field="length" inputmode="numeric" value="${html(r.length)}"></label><label>Frequency<select data-audience-block-field="operator"><option value="AT_LEAST" ${r.operator==='AT_LEAST'?'selected':''}>At least</option><option value="EXACTLY" ${r.operator==='EXACTLY'?'selected':''}>Exactly</option></select></label><label>Blocks per week<input data-audience-block-field="count" inputmode="numeric" value="${html(r.count)}"></label><button data-remove-audience-block class="secondary">Remove block requirement…</button></fieldset>`).join('')}<button data-add-audience-block ${scope?`data-rule-portion="${html(scope)}"`:''} class="secondary">Add required consecutive block</button>`;
export function audienceSchedulingContent(draft,p,html){
 return `<details class="setup-audience-scheduling"><summary>Audience scheduling and rooms</summary><p>Undeclared consecutive runs are forbidden. Required blocks consume existing teaching and share one teacher and room. Whole-class sessions contribute to each attending group's inherited requirement.</p>
 <p>Default internal gaps are soft; explicit hard limits remain hard.</p><label>Plan allowed rooms<select data-audience-room-mode><option value="inherited" ${!own(p,'allowedRoomIds')?'selected':''}>All otherwise compatible rooms</option><option value="restricted" ${own(p,'allowedRoomIds')?'selected':''}>Restrict inherited room set</option></select></label>${own(p,'allowedRoomIds')?draft.rooms.map(room=>`<label><input type="checkbox" data-audience-room="${html(room.id)}" ${p.allowedRoomIds.includes(room.id)?'checked':''}>${html(room.displayName)} · capacity ${room.capacity}</label>`).join(''):''}
 <label>Required room capabilities (comma-separated identifiers)<input data-audience-capabilities value="${html((p.requiredRoomCapabilityIds??[]).join(', '))}"></label><h4>Inherited consecutive requirements</h4>${blockFields(p.blockRules??[],null,html)}
 ${activityCatalog(draft).filter(a=>a.plan.id===p.id).map(a=>{const q=a.portion;return `<fieldset data-room-portion="${html(q.id)}"><legend>${html(a.name)}</legend>
  <label>Consecutive rules<select data-audience-block-mode><option value="inherited" ${!own(q,'blockRules')?'selected':''}>Inherit subject-plan requirements</option><option value="override" ${own(q,'blockRules')?'selected':''}>Explicit audience requirements</option></select></label>${own(q,'blockRules')?blockFields(q.blockRules,q.id,html):''}
  <label>Allowed rooms<select data-audience-room-mode><option value="inherited" ${!own(q,'allowedRoomIds')?'selected':''}>Inherit room restrictions</option><option value="restricted" ${own(q,'allowedRoomIds')?'selected':''}>Narrow allowed rooms</option></select></label>
  ${own(q,'allowedRoomIds')?draft.rooms.map(room=>`<label><input type="checkbox" data-audience-room="${html(room.id)}" ${q.allowedRoomIds.includes(room.id)?'checked':''}>${html(room.displayName)} · capacity ${room.capacity}</label>`).join(''):''}
  <label>Fixed room (optional)<select data-audience-fixed-room><option value="">No fixed room</option>${draft.rooms.map(room=>`<option value="${html(room.id)}" ${q.fixedRoomId===room.id?'selected':''}>${html(room.displayName)}</option>`).join('')}</select></label></fieldset>`;}).join('')}
 <div data-audience-scheduling-issues role="status">${coordinationIssues(draft).filter(i=>i.audiencePlan===p.id).map(i=>`<p>${html(i.message)}</p>`).join('')}</div></details>`;
}
export function coordinationContent(draft,c,html){
 const activities=activityCatalog(draft),periods=draft.grid.intervals.filter(i=>['Lesson','Reserved lesson'].includes(i.kind));
 return `<div data-coordination-class="${html(c.id)}"><button id="setup-back-coordination" class="secondary">← Teaching requirements</button><h3>Hard coordination · ${html(c.displayName)}</h3>
 <p>Internal student gaps are permitted by default, with a separate optimization priority above ordinary preferences. Explicit existing hard gap limits remain hard. Optional gap-free rules cover every anonymous block of the selected class.</p>
 <p>Choose only the hard relationships your school needs. Without a rule, compatible placement and cross-day exchanges remain automatic. A count uses distinct occurrences; extra occurrences stay flexible.</p>
 <label>Hard control<select id="setup-coordination-type">${Object.entries(coordinationTypes).map(([id,label])=>`<option value="${id}">${label}</option>`).join('')}</select></label><button id="setup-add-coordination">Add hard control</button>
 ${(draft.coordinationRules??[]).filter(r=>r.classId===c.id).map(r=>`<fieldset data-coordination="${html(r.id)}"><legend>${html(coordinationTypes[r.type]??r.type)}</legend>
 ${r.type!=='GAP_FREE'?`<label>Required weekly count<input data-coordination-field="count" inputmode="numeric" value="${html(r.count)}"></label><fieldset><legend>Participating activities</legend>${activities.map(a=>{const ref=r.activities?.find(ref=>ref.portionId===a.id);return `<div><label><input type="checkbox" data-coordination-activity="${html(a.id)}" ${ref?'checked':''}>${html(a.name)}</label>${ref?`<p>${html(Math.max(0,a.portion.sessionIds.length-(r.count??0)))} single sessions remain outside the requested count; block counts use declared required occurrences.</p><label>Activity unit<select data-coordination-unit="${html(a.id)}"><option value="SESSION" ${ref.unit==='SESSION'?'selected':''}>Single session</option>${a.blockRules.map(b=>`<option value="${html(b.id)}" ${ref.blockRuleId===b.id?'selected':''}>Required block · ${b.length} periods</option>`).join('')}</select></label>`:''}</div>`;}).join('')}</fieldset>`:'<p>Hard zero internal gaps for every block of this class across all subjects and arrangements.</p>'}
 ${['ALLOWED','GAP_FREE'].includes(r.type)?`<label>Applicable days<select data-coordination-set-mode="days"><option value="all" ${!own(r,'days')?'selected':''}>All active days</option><option value="selected" ${own(r,'days')?'selected':''}>Selected days</option></select></label>${own(r,'days')?draft.grid.activeDays.map(day=>`<label><input type="checkbox" data-coordination-day="${day}" ${r.days.includes(day)?'checked':''}>${day}</label>`).join(''):''}`:''}
 ${r.type==='ALLOWED'?`<label>Allowed starts<select data-coordination-set-mode="periodIds"><option value="all" ${!own(r,'periodIds')?'selected':''}>All teaching periods</option><option value="selected" ${own(r,'periodIds')?'selected':''}>Selected teaching periods</option></select></label>${own(r,'periodIds')?periods.map(period=>`<label><input type="checkbox" data-coordination-period="${html(period.periodId)}" ${r.periodIds.includes(period.periodId)?'checked':''}>${html(period.day)} / ${html(period.label)}</label>`).join(''):''}`:''}
 ${r.type==='FIXED'?`<label>Weekday<select data-coordination-field="day"><option value="">Choose a day</option>${draft.grid.activeDays.map(day=>`<option ${r.day===day?'selected':''}>${day}</option>`).join('')}</select></label><label>Start teaching period<select data-coordination-field="periodId"><option value="">Choose a period</option>${periods.map(period=>`<option value="${html(period.periodId)}" ${r.periodId===period.periodId?'selected':''}>${html(period.day)} / ${html(period.label)}</option>`).join('')}</select></label>`:''}
 ${r.type==='START_TOGETHER'?'<p>Equal start, not equal duration. A single ending early does not cover a longer block’s second period.</p>':''}${r.type==='COVERAGE'?'<p>Every class block must be covered exactly once in each occupied round period, using only selected teaching plans. Additional occurrences of those plans may fill the remaining duration.</p>':''}
 <button data-remove-coordination class="secondary">Remove hard control…</button><div data-coordination-issues role="status">${coordinationIssues(draft).filter(i=>i.coordination===r.id).map(i=>`<p>${html(i.message)}</p>`).join('')}</div></fieldset>`).join('')}</div>`;
}

export function readableScheduling(row){
 const ids=v=>Array.isArray(v)&&v.every(id=>typeof id==='string');
 return (!own(row,'requiredRoomCapabilityIds')||ids(row.requiredRoomCapabilityIds))&&(!own(row,'allowedRoomIds')||ids(row.allowedRoomIds))&&(!own(row,'fixedRoomId')||typeof row.fixedRoomId==='string')
 &&(!own(row,'blockRules')||Array.isArray(row.blockRules)&&row.blockRules.every(b=>b&&typeof b.id==='string'&&['length','count'].every(k=>['string','number'].includes(typeof b[k]))&&typeof b.operator==='string'&&Object.keys(b).every(k=>['id','length','count','operator'].includes(k))));
}
export function readableCoordination(draft){
 return !own(draft,'coordinationRules')||Array.isArray(draft.coordinationRules)&&draft.coordinationRules.every(r=>r&&['id','classId','type'].every(k=>typeof r[k]==='string')
 &&(!own(r,'count')||['string','number'].includes(typeof r.count))&&['day','periodId'].every(k=>!own(r,k)||typeof r[k]==='string')
 &&['days','periodIds'].every(k=>!own(r,k)||Array.isArray(r[k])&&r[k].every(v=>typeof v==='string'))
 &&(!own(r,'activities')||Array.isArray(r.activities)&&r.activities.every(a=>a&&typeof a.portionId==='string'&&typeof a.unit==='string'&&(!own(a,'blockRuleId')||typeof a.blockRuleId==='string')&&Object.keys(a).every(k=>['portionId','unit','blockRuleId'].includes(k))))
 &&Object.keys(r).every(k=>['id','classId','type','count','activities','days','periodIds','day','periodId'].includes(k)));
}
export function createCoordinationActions({getDraft,commit,contextOnly,confirm,notify,html}){
 const numeric=v=>/^-?\d+$/.test(v)&&Number.isSafeInteger(Number(v))?Number(v):v;
 const toggle=(row,key,id,checked)=>{const set=new Set(row[key]??[]);checked?set.add(id):set.delete(id);row[key]=[...set];};
 const referenced=(draft,id)=>(draft.coordinationRules??[]).some(r=>r.activities?.some(a=>a.blockRuleId===id));
 function edit(event){
  const n=event.target,d=getDraft(),rr=n.closest('[data-coordination]'),pr=n.closest('[data-audience-plan]');let handled=false;
  if(rr){const r=d.coordinationRules.find(r=>r.id===rr.dataset.coordination);
   if(n.dataset.coordinationField){r[n.dataset.coordinationField]=n.dataset.coordinationField==='count'?numeric(n.value):n.value;handled=true;}
   else if(n.dataset.coordinationActivity){r.activities??=[];if(n.checked)r.activities.push({portionId:n.dataset.coordinationActivity,unit:'SESSION'});else r.activities=r.activities.filter(a=>a.portionId!==n.dataset.coordinationActivity);handled=true;}
   else if(n.dataset.coordinationUnit){const a=r.activities.find(a=>a.portionId===n.dataset.coordinationUnit);a.unit=n.value==='SESSION'?'SESSION':'BLOCK';if(a.unit==='BLOCK')a.blockRuleId=n.value;else delete a.blockRuleId;handled=true;}
   else if(n.dataset.coordinationSetMode){if(n.value==='all')delete r[n.dataset.coordinationSetMode];else r[n.dataset.coordinationSetMode]=[];handled=true;}
   else if(n.dataset.coordinationDay){toggle(r,'days',n.dataset.coordinationDay,n.checked);handled=true;}
   else if(n.dataset.coordinationPeriod){toggle(r,'periodIds',n.dataset.coordinationPeriod,n.checked);handled=true;}
  }else if(pr){const p=d.audiencePlans.find(p=>p.id===pr.dataset.audiencePlan),qr=n.closest('[data-room-portion]'),q=qr?p.portions.find(q=>q.id===qr.dataset.roomPortion):p,br=n.closest('[data-audience-block]');
   if(n.hasAttribute('data-audience-capabilities')){p.requiredRoomCapabilityIds=n.value===''?[]:n.value.split(',').map(v=>v.trim());handled=true;}else if(n.dataset.audienceBlockField&&br){const scope=br.dataset.rulePortion?p.portions.find(q=>q.id===br.dataset.rulePortion):p,b=scope.blockRules.find(b=>b.id===br.dataset.audienceBlock);
    b[n.dataset.audienceBlockField]=n.dataset.audienceBlockField==='operator'?n.value:numeric(n.value);handled=true;
   }else if(n.hasAttribute('data-audience-block-mode')){
    if((q.blockRules??[]).some(b=>referenced(d,b.id))){notify('Correct coordination references before replacing consecutive requirements.');n.value='override';return true;}
    if(n.value==='inherited')delete q.blockRules;else q.blockRules=(p.blockRules??[]).map(b=>({...b,id:uid()}));handled=true;
   }else if(n.hasAttribute('data-audience-room-mode')){if(n.value==='inherited')delete q.allowedRoomIds;else q.allowedRoomIds=[...(q===p?d.rooms.map(r=>r.id):p.allowedRoomIds??d.rooms.map(r=>r.id))];handled=true;}
   else if(n.dataset.audienceRoom){toggle(q,'allowedRoomIds',n.dataset.audienceRoom,n.checked);handled=true;}
   else if(n.hasAttribute('data-audience-fixed-room')){if(n.value)q.fixedRoomId=n.value;else delete q.fixedRoomId;handled=true;}
  }
  if(!handled)return false;
  const render=n.tagName==='SELECT'||n.type==='checkbox';commit(d,render);
  if(!render){const target=rr?.querySelector('[data-coordination-issues]')??pr?.querySelector('[data-audience-scheduling-issues]');if(target)target.innerHTML=coordinationIssues(d).filter(i=>rr?i.coordination===rr.dataset.coordination:i.audiencePlan===pr.dataset.audiencePlan).map(i=>`<p>${html(i.message)}</p>`).join('');}
  return true;
 }
 return {input:e=>e.target.tagName!=='SELECT'&&e.target.type!=='checkbox'?edit(e):false,change:e=>e.target.tagName==='SELECT'||e.target.type==='checkbox'?edit(e):false,
 click(event){const b=event.target.closest('button');if(!b)return false;const d=getDraft();
  if(b.hasAttribute('data-open-coordination')){d.pendingInput.coordinationClassId=b.dataset.openCoordination;contextOnly(d);return true;}
  if(b.id==='setup-back-coordination'){delete d.pendingInput.coordinationClassId;contextOnly(d);return true;}
  if(b.id==='setup-add-coordination'){const type=document.querySelector('#setup-coordination-type').value,classId=b.closest('[data-coordination-class]').dataset.coordinationClass;(d.coordinationRules??=[]).push({id:uid(),classId,type,...(type==='GAP_FREE'?{}:{count:1,activities:[]})});commit(d);return true;}
  const rr=b.closest('[data-coordination]');if(rr&&b.hasAttribute('data-remove-coordination')){confirm('Remove hard control?','Only this explicit control is removed. Other hard rules remain.',()=>{d.coordinationRules=d.coordinationRules.filter(r=>r.id!==rr.dataset.coordination);commit(d);},'Remove control');return true;}
  const pr=b.closest('[data-audience-plan]');if(!pr)return false;const p=d.audiencePlans.find(p=>p.id===pr.dataset.audiencePlan);
  if(b.hasAttribute('data-add-audience-block')){const q=b.dataset.rulePortion?p.portions.find(q=>q.id===b.dataset.rulePortion):p;(q.blockRules??=[]).push({id:uid(),length:2,count:1,operator:'AT_LEAST'});commit(d);return true;}
  if(b.hasAttribute('data-remove-audience-block')){const br=b.closest('[data-audience-block]'),q=br.dataset.rulePortion?p.portions.find(q=>q.id===br.dataset.rulePortion):p,id=br.dataset.audienceBlock;
   if(referenced(d,id)){notify('Correct coordination references before removing this required block.');return true;}
   confirm('Remove consecutive requirement?','Existing sessions remain. Review the changed consecutive policy before generation.',()=>{q.blockRules=q.blockRules.filter(r=>r.id!==id);commit(d);},'Remove block requirement');return true;}
  return false;
 }};
}
