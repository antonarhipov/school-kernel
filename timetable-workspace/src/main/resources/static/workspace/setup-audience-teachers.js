import {eligible,nameOf} from './setup-teachers.js';
const own=(v,k)=>Object.hasOwn(v??{},k);
const validCount=n=>Number.isInteger(n)&&n>0;
export function policyCandidates(draft,plan,portion){
 const eligibleIds=draft.teachers.filter(t=>eligible(draft,t,plan.subjectId,plan.classId)).map(t=>t.id);
 const inherited=own(plan,'candidateTeacherIds')?eligibleIds.filter(id=>plan.candidateTeacherIds.includes(id)):eligibleIds;
 return portion&&own(portion.teacherPolicy,'candidateTeacherIds')?inherited.filter(id=>portion.teacherPolicy.candidateTeacherIds.includes(id)):inherited;
}
export function audienceTeacherIssues(draft){
 const issues=[];
 for(const p of draft.audiencePlans??[]){
  const add=message=>issues.push({section:'Classes',record:p.classId,audiencePlan:p.id,message});
  for(const id of p.candidateTeacherIds??[])if(!eligible(draft,draft.teachers.find(t=>t.id===id),p.subjectId,p.classId))add(`Candidate ${id} is not qualified/class-eligible for this teaching requirement.`);
  for(const q of p.portions){
   const policy=q.teacherPolicy,choices=policyCandidates(draft,p,q);
   if(!choices.length)add(`Audience ${q.audienceId}: no eligible teacher candidates; an explicit empty restriction never means unrestricted.`);
   for(const id of policy.candidateTeacherIds??[])if(!policyCandidates(draft,p).includes(id))add(`Audience ${q.audienceId}: narrowed candidate ${id} is outside the inherited eligible set.`);
   if(policy.mode==='fixed'&&!choices.includes(policy.fixedTeacherId))add(`Audience ${q.audienceId}: fixed teacher ${policy.fixedTeacherId??'(missing)'} is not eligible within the selected candidates.`);
   if(policy.mode==='sharing'){
    if(!policy.shares?.length)add(`Audience ${q.audienceId}: declare positive teacher-fixed weekly portions.`);
    for(const share of policy.shares??[]){if(!validCount(share.count))add(`Teacher portion ${share.id}: use a positive weekly count.`);if(!choices.includes(share.teacherId))add(`Teacher portion ${share.id}: teacher ${share.teacherId??'(missing)'} is not eligible.`);}
    const total=(policy.shares??[]).reduce((n,s)=>n+(validCount(s.count)?s.count:0),0);
    if(total!==q.sessionIds.length)add(`Audience ${q.audienceId}: teacher portions total ${total}, required teaching ${q.sessionIds.length}.`);
   }
  }
 }
 return issues;
}
function audienceName(draft,p,q){return q.audienceId===`class:${p.classId}`?'Whole class':draft.classes.find(c=>c.id===p.classId)?.splits?.flatMap(s=>s.groups).find(g=>g.id===q.audienceId)?.displayName??q.audienceId;}
export function audienceTeacherContent(draft,p,html){
 const teachers=draft.teachers.filter(t=>eligible(draft,t,p.subjectId,p.classId));
 const options=(selected,choices)=>`<option value="">Choose an eligible teacher</option>${selected&&!choices.includes(selected)?`<option value="${html(selected)}" selected>Missing/ineligible: ${html(selected)}</option>`:''}${choices.map(id=>`<option value="${html(id)}" ${id===selected?'selected':''}>${html(nameOf(draft.teachers.find(t=>t.id===id)))}</option>`).join('')}`;
 return `<details class="setup-teacher-policies"><summary>Teacher settings · weekly continuity</summary>
  <p>Automatic selection chooses one eligible teacher for each audience portion throughout the week. Availability is hard. Reference workloads are monitoring values.</p>
  <label>Class-subject candidates<select data-plan-candidate-mode><option value="inherited" ${!own(p,'candidateTeacherIds')?'selected':''}>All qualified and class-eligible teachers</option><option value="restricted" ${own(p,'candidateTeacherIds')?'selected':''}>Narrow the eligible set</option></select></label>
  ${own(p,'candidateTeacherIds')?teachers.map(t=>`<label><input type="checkbox" data-plan-candidate="${html(t.id)}" ${p.candidateTeacherIds.includes(t.id)?'checked':''}>${html(nameOf(t))}</label>`).join(''):''}
  ${p.portions.map(q=>{const policy=q.teacherPolicy,choices=policyCandidates(draft,p,q);return `<fieldset data-teacher-portion="${html(q.id)}"><legend>${html(audienceName(draft,p,q))} · ${q.sessionIds.length} sessions</legend>
    <label>Teacher policy<select data-teacher-policy-mode><option value="automatic" ${policy.mode==='automatic'?'selected':''}>Automatic · one weekly teacher</option><option value="fixed" ${policy.mode==='fixed'?'selected':''}>Fixed teacher · hard assignment</option><option value="sharing" ${policy.mode==='sharing'?'selected':''}>Deliberate weekly sharing · fixed portions</option></select></label>
    <label>Candidates<select data-portion-candidate-mode><option value="inherited" ${!own(policy,'candidateTeacherIds')?'selected':''}>Inherit class-subject candidates</option><option value="restricted" ${own(policy,'candidateTeacherIds')?'selected':''}>Narrow candidates for this audience</option></select></label>
    <p>Eligible candidates: ${choices.map(id=>html(nameOf(draft.teachers.find(t=>t.id===id)))).join(', ')||'None — correct eligibility or restrictions'}</p>
    ${own(policy,'candidateTeacherIds')?policyCandidates(draft,p).map(id=>`<label><input type="checkbox" data-portion-candidate="${html(id)}" ${policy.candidateTeacherIds.includes(id)?'checked':''}>${html(nameOf(draft.teachers.find(t=>t.id===id)))}</label>`).join(''):''}
    ${policy.mode==='fixed'?`<label>Fixed teacher<select data-fixed-audience-teacher>${options(policy.fixedTeacherId,choices)}</select></label>`:''}
    ${policy.mode==='sharing'?`<p>Counts refer only to this portion; whole-class teaching is not counted twice. A teacher must cover a required consecutive block in full.</p>${(policy.shares??[]).map(s=>`<div data-teacher-share="${html(s.id)}" class="setup-context-fields"><label>Teacher<select data-share-teacher>${options(s.teacherId,choices)}</select></label><label>Weekly sessions<input data-share-count inputmode="numeric" value="${html(s.count)}"></label><button data-remove-teacher-share class="secondary">Remove weekly portion</button></div>`).join('')}<button data-add-teacher-share class="secondary">Add fixed weekly portion</button>`:''}</fieldset>`;}).join('')}
  <div data-audience-teacher-issues role="status">${audienceTeacherIssues(draft).filter(i=>i.audiencePlan===p.id).map(i=>`<p>${html(i.message)}</p>`).join('')}</div></details>`;
}
export function createAudienceTeacherActions({getDraft,commit,html,notify,confirm}){
 function edit(event){const node=event.target,root=node.closest('[data-audience-plan]');if(!root)return false;
  const draft=getDraft(),p=draft.audiencePlans.find(p=>p.id===root.dataset.audiencePlan),portion=node.closest('[data-teacher-portion]'),q=portion?p.portions.find(q=>q.id===portion.dataset.teacherPortion):null;
  const toggle=(object,key,id,checked)=>{object[key]=(object[key]??[]).filter(v=>v!==id);if(checked)object[key].push(id);};let render=true;
  if(node.hasAttribute('data-plan-candidate-mode')){if(node.value==='inherited')delete p.candidateTeacherIds;else p.candidateTeacherIds=policyCandidates(draft,p);}
  else if(node.dataset.planCandidate){toggle(p,'candidateTeacherIds',node.dataset.planCandidate,node.checked);}
  else if(node.hasAttribute('data-teacher-policy-mode'))q.teacherPolicy.mode=node.value;
  else if(node.hasAttribute('data-portion-candidate-mode')){if(node.value==='inherited')delete q.teacherPolicy.candidateTeacherIds;else q.teacherPolicy.candidateTeacherIds=policyCandidates(draft,p,q);}
  else if(node.dataset.portionCandidate)toggle(q.teacherPolicy,'candidateTeacherIds',node.dataset.portionCandidate,node.checked);
  else if(node.hasAttribute('data-fixed-audience-teacher'))q.teacherPolicy.fixedTeacherId=node.value;
  else if(node.hasAttribute('data-share-teacher'))q.teacherPolicy.shares.find(s=>s.id===node.closest('[data-teacher-share]').dataset.teacherShare).teacherId=node.value;
  else if(node.hasAttribute('data-share-count')){q.teacherPolicy.shares.find(s=>s.id===node.closest('[data-teacher-share]').dataset.teacherShare).count=/^\d+$/.test(node.value)&&Number.isSafeInteger(Number(node.value))?Number(node.value):node.value;render=false;}
  else return false;
  commit(draft,render);
  if(render){const details=[...document.querySelectorAll('[data-audience-plan]')].find(n=>n.dataset.audiencePlan===p.id)?.querySelector('.setup-teacher-policies');if(details)details.open=true;}
  else root.querySelector('[data-audience-teacher-issues]').innerHTML=audienceTeacherIssues(draft).filter(i=>i.audiencePlan===p.id).map(i=>`<p>${html(i.message)}</p>`).join('');return true;
 }
 return {input:event=>event.target.hasAttribute('data-share-count')?edit(event):false,change:event=>event.target.hasAttribute('data-share-count')?false:edit(event),click(event){
  const button=event.target.closest('button');if(!button)return false;
  if(button.hasAttribute('data-adopt-automatic-teacher')){
    const draft=getDraft(),p=draft.plans.find(p=>p.id===button.closest('[data-plan]').dataset.plan);
    try{adoptFixedPlan(draft,p);}catch(error){notify(error.message);return true;}
    confirm('Adopt automatic weekly teacher selection?',`Currently fixed: ${(p.allocations??[]).map(a=>`${nameOf(draft.teachers.find(t=>t.id===a.teacherId))}: ${a.count} sessions`).join('; ')}. Replace these fixed assignments with one automatically selected eligible weekly teacher. Adopt contract2/catalog11: actual anonymous attendance; internal student-gap periods at priority above ordinary preferences (weight1, replacing the older weighted gap preference); ordinary uncovered-student-period cost at weight1; class late-start, week-balance and same-series costs counted per student with retained ordinary weights. The selected sessions form one audience/subject series. Existing explicit hard gap limits remain; omitted legacy hard gap limits on existing taught classes are materialized as zero. Consecutive runs still require explicit permission; none is inferred. Existing lesson identities remain; individual labels and original fixed/series values remain in authoring adoption provenance. Periods and rooms will be chosen on generation. Original source and saved timetables remain unchanged.`,()=>{const next=adoptFixedPlan(getDraft(),getDraft().plans.find(row=>row.id===p.id));next.pendingInput.audiencePlanClassId=p.classId??p.cohortId;delete next.pendingInput.planClassId;commit(next);},'Adopt automatic selection');return true;
  }
  if(!button.hasAttribute('data-add-teacher-share')&&!button.hasAttribute('data-remove-teacher-share'))return false;
  const draft=getDraft(),p=draft.audiencePlans.find(p=>p.id===button.closest('[data-audience-plan]').dataset.audiencePlan),q=p.portions.find(q=>q.id===button.closest('[data-teacher-portion]').dataset.teacherPortion);
  q.teacherPolicy.shares??=[];
  if(button.hasAttribute('data-add-teacher-share'))q.teacherPolicy.shares.push({id:crypto.randomUUID(),teacherId:'',count:''});
  else q.teacherPolicy.shares=q.teacherPolicy.shares.filter(s=>s.id!==button.closest('[data-teacher-share]').dataset.teacherShare);
  commit(draft);const detail=[...document.querySelectorAll('[data-audience-plan]')].find(n=>n.dataset.audiencePlan===p.id)?.querySelector('.setup-teacher-policies');if(detail)detail.open=true;return true;
 }};
}
export function adoptFixedPlan(draft,plan){
 if(!plan)throw new Error('The fixed teaching plan is no longer current.');
 const supported=['id','classId','cohortId','subjectId','requiredCount','allocations','occurrences','seriesId','nextOccurrence','blockRules','occurrenceTemplate'];
 const unknown=Object.keys(plan).filter(k=>!supported.includes(k));
 const settings=(plan.occurrences??[]).flatMap(o=>Object.keys(o).filter(k=>!['id','displayName','teacherId','seriesId','subjectId','cohortId'].includes(k)).map(k=>`${o.id}/${k}`));
 settings.push(...Object.keys(plan.occurrenceTemplate??{}).filter(k=>k!=='seriesId').map(k=>`occurrenceTemplate/${k}`));
 if(unknown.length||settings.length||plan.blockRules?.length)throw new Error(`Automatic adoption cannot yet preserve these retained settings: ${[...unknown,...settings,...(plan.blockRules?.length?['consecutive block rules']:[])].join('; ')}. Keep the fixed policy or restructure explicitly in Advanced.`);
 if(!Number.isInteger(plan.requiredCount)||plan.requiredCount<=0||plan.requiredCount!==plan.occurrences?.length)throw new Error('Correct fixed demand/occurrences before adoption.');
 if((draft.audiencePlans??[]).some(p=>p.classId===(plan.classId??plan.cohortId)&&p.subjectId===plan.subjectId))throw new Error('An audience requirement already exists for this class and subject. Neither plan was replaced.');
 const next=structuredClone(draft),classId=plan.classId??plan.cohortId;
 if(!next.audiencePlans?.length)for(const c of next.classes)if(!Object.hasOwn(c,'maxDailyGaps')&&next.plans.some(p=>(p.classId??p.cohortId)===c.id))c.maxDailyGaps=0;
 (next.audiencePlans??=[]).push({id:plan.id,classId,subjectId:plan.subjectId,weeklyTotal:plan.requiredCount,delivery:'whole',wholeClassCount:0,overrides:{},sourceFixedPlan:structuredClone(plan),portions:[{id:crypto.randomUUID(),audienceId:`class:${classId}`,sessionIds:plan.occurrences.map(o=>o.id),teacherPolicy:{mode:'automatic'}}]});
 next.plans=next.plans.filter(p=>p.id!==plan.id);return next;
}
