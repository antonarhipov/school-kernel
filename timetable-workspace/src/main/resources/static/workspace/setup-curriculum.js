const clone=value=>structuredClone(value),uid=()=>crypto.randomUUID();
const nameOf=row=>row?.displayName??row?.name??'(missing)';
export function copyCurriculum(draft,sourceId,destinationId){
  if(sourceId===destinationId||!draft.classes.some(c=>c.id===sourceId)||!draft.classes.some(c=>c.id===destinationId))throw new Error('Choose distinct existing source and destination classes.');
  if(draft.plans.some(p=>(p.classId??p.cohortId)===destinationId))throw new Error('The destination already has teaching rows. Curriculum copy requires an empty destination; nothing was replaced.');
  const source=draft.plans.filter(p=>(p.classId??p.cohortId)===sourceId);
  if(!source.length)throw new Error('The source has no curriculum to copy.');
  const invalid=source.filter(p=>!Number.isInteger(p.requiredCount)||p.requiredCount<=0||p.requiredCount>2147483647||!draft.subjects.some(s=>s.id===p.subjectId));
  if(invalid.length)throw new Error(`Correct source subjects and positive weekly demand first: ${invalid.map(p=>p.id).join(', ')}. No rows were copied.`);
  const next=clone(draft);
  next.plans.push(...source.map(p=>({id:uid(),classId:destinationId,subjectId:p.subjectId,requiredCount:p.requiredCount,allocations:[],occurrences:[],seriesId:uid(),nextOccurrence:0,blockRules:[]})));
  return next;
}
export function curriculumContent(draft,classId,html){
  return `<details><summary>Copy curriculum to another class</summary><p>Copies only subjects and weekly demand. Teachers, class settings and row rules are not copied. The destination must have no teaching rows.</p>
    <label>Destination class<select id="setup-copy-curriculum-destination"><option value="">Choose a different class…</option>${draft.classes.filter(c=>c.id!==classId).map(c=>`<option value="${html(c.id)}" ${draft.pendingInput?.copyDestination===c.id?'selected':''}>${html(nameOf(c))}</option>`).join('')}</select></label><button id="setup-preview-curriculum" class="secondary">Preview curriculum copy…</button></details>`;
}
export function createCurriculumActions({getDraft,commit,contextOnly,notify,confirm}){
  return {
    change(event){if(event.target.id!=='setup-copy-curriculum-destination')return false;const next=clone(getDraft());next.pendingInput.copyDestination=event.target.value;contextOnly(next,false);return true;},
    click(event){
      if(event.target.closest('button')?.id!=='setup-preview-curriculum')return false;
      const draft=getDraft(),sourceId=document.querySelector('#setup-class-plan').dataset.planClass,destinationId=document.querySelector('#setup-copy-curriculum-destination').value;
      let next;
      try{next=copyCurriculum(draft,sourceId,destinationId);}catch(error){notify(error.message);return true;}
      const preview=draft.plans.filter(p=>(p.classId??p.cohortId)===sourceId).map(p=>`${nameOf(draft.subjects.find(s=>s.id===p.subjectId))}: ${p.requiredCount} lessons per week`).join('; ');
      confirm('Copy curriculum?',`From ${nameOf(draft.classes.find(c=>c.id===sourceId))} to ${nameOf(draft.classes.find(c=>c.id===destinationId))}: ${preview}. Only subjects and weekly counts are copied. Teachers, eligibility, curator, home room, availability, lunch, block/room rules, locks and occurrence identities are not copied. All destination demand is unassigned.`,()=>{commit(next);notify('Independent destination curriculum created with unassigned demand. Source teaching and workloads are unchanged.');},'Copy curriculum');return true;
    }
  };
}
