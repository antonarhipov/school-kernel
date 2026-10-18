import { availabilityCapacityIssues } from './setup-availability.js';
export const nameOf = row => row?.displayName ?? row?.name ?? '(missing)';
export const classesFor = (teacher, subjectId) => Object.hasOwn(teacher?.eligibleClassIdsBySubject??{},subjectId) ? teacher.eligibleClassIdsBySubject[subjectId] : [];
export function qualification(draft, teacher, subjectId, classId) {
  const subject=draft.subjects.find(s=>s.id===subjectId),cohort=draft.classes.find(c=>c.id===classId);
  return teacher?.qualifiedSubjectIds?.includes(subjectId) || !!(subject?.curatorLesson && cohort?.curatorTeacherId===teacher?.id);
}
export function eligible(draft, teacher, subjectId, classId) {
  if(draft.subjects.find(s=>s.id===subjectId)?.curatorLesson && draft.classes.find(c=>c.id===classId)?.curatorTeacherId!==teacher?.id)return false;
  return classesFor(teacher,subjectId).includes(classId) && qualification(draft,teacher,subjectId,classId);
}
export function workload(draft, teacherId) {
  return draft.plans.reduce((sum,p)=>sum+(p.allocations??[]).filter(a=>a.teacherId===teacherId&&Number.isInteger(a.count)&&a.count>0).reduce((n,a)=>n+a.count,0),0);
}
export function teacherIssues(draft) {
  const issues=[];
  for(const t of draft.teachers){
    issues.push(...availabilityCapacityIssues(draft,t));
    const add=message=>issues.push({section:'Teachers',record:t.id,message:`${nameOf(t)||'Unnamed teacher'}: ${message}`});
    if(!nameOf(t).trim())add('enter a teacher name.');
    for(const id of t.qualifiedSubjectIds??[])if(!draft.subjects.some(s=>s.id===id))add(`qualified subject ${id} is missing.`);
    for(const [subjectId,classes] of Object.entries(t.eligibleClassIdsBySubject??{})){
      if(!draft.subjects.some(s=>s.id===subjectId))add(`eligibility subject ${subjectId} is missing.`);
      for(const classId of classes){
        if(!draft.classes.some(c=>c.id===classId))add(`eligible class ${classId} is missing.`);
        else if(!qualification(draft,t,subjectId,classId))add(`eligibility for ${subjectId} in class ${classId} lacks qualification; own-class curator qualification is separate.`);
      }
    }
    for(const p of draft.plans)if((p.allocations??[]).some(a=>a.teacherId===t.id)&&!eligible(draft,t,p.subjectId,p.classId??p.cohortId))add(`allocated row ${p.id} (${p.subjectId}, class ${p.classId??p.cohortId}) lacks qualification or class eligibility. Allocations are retained; correct the class plan explicitly.`);
  }
  return issues;
}
export function teacherFields(draft,t,html){
  const plans=draft.plans.filter(p=>(p.allocations??[]).some(a=>a.teacherId===t.id));
  return `<h3>Qualification and class eligibility</h3><p>Choose a subject, then its eligible classes. Curators are also qualified for their own class’s curator lessons.</p>
    ${draft.subjects.map(s=>`<fieldset><legend>${html(nameOf(s))}</legend><label><input type="checkbox" data-teacher-qualification="${html(s.id)}" ${t.qualifiedSubjectIds?.includes(s.id)?'checked':''}>Qualified for ${html(nameOf(s))}</label>
      <details><summary>Eligible classes for ${html(nameOf(s))} (${classesFor(t,s.id).length})</summary>
      <div class="setup-eligibility-list">${draft.classes.map(c=>`<label><input type="checkbox" data-teacher-subject="${html(s.id)}" data-teacher-class="${html(c.id)}" ${classesFor(t,s.id).includes(c.id)?'checked':''}>${html(nameOf(c))}${s.curatorLesson&&c.curatorTeacherId===t.id?' · Own-class implicit curator qualification':''}</label>`).join('')||'<p>Add a class to record eligibility.</p>'}</div></details></fieldset>`).join('')||'<p>Add subjects before recording qualification or eligibility.</p>'}
    <h3>Derived weekly workload</h3><p id="setup-teacher-workload">${workload(draft,t.id)} allocated lessons per week</p>
    <p>Workload comes from class teaching plans. Assign lessons there.</p>
    ${plans.map(p=>`<p><button data-teacher-plan="${html(p.id)}">${html(nameOf(draft.classes.find(c=>c.id===(p.classId??p.cohortId))))}: ${html(nameOf(draft.subjects.find(s=>s.id===p.subjectId)))} · ${(p.allocations??[]).filter(a=>a.teacherId===t.id).map(a=>a.count).join(', ')} lessons</button></p>`).join('')}
    <button data-teacher-availability="${html(t.id)}" class="secondary">Availability and preferences</button>`;
}
