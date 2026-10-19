import { subjectTeachingContent } from './setup-subject-teaching.js';
import { teacherIssues, teacherFields, classesFor } from './setup-teachers.js';
import { roomIssues, roomFields, subjectRoomContent } from './setup-rooms.js';
import { splitsContent, splitIssues, refreshSplitFeedback } from './setup-splits.js';
const clone = value => structuredClone(value);
const uid = () => crypto.randomUUID();
export const subjectControls = [
  ['reservedPeriodsAllowed', 'Reserved-slot permission', 'boolean', 'Hard permission for all reserved periods; it does not force their use. Default: not allowed.'],
  ['maxWeeklyReservedLessonsPerCohort', 'Maximum reserved lessons per class per week', 'positive', 'Hard weekly maximum across teachers. Unlimited when omitted; requires reserved-slot permission.'],
  ['maxDailyLessonsPerCohort', 'Maximum subject lessons per class per day', 'positive', 'Hard daily maximum across teachers, independent of block permission. Unlimited when omitted.'],
  ['dayEdgeOnly', 'Day-edge-only', 'boolean', 'Hard: first or last occupied lesson of the class day, not a fixed clock slot. Default: off.'],
  ['curatorLesson', 'Curator lesson', 'boolean', 'Hard: the class curator teaches this subject in its configured home room. Nomination creates no teaching demand. Default: off.']
];
export const display = row => row.displayName ?? row.name ?? '';
export const classControls = [
  ['maxDailyGaps', 'Maximum internal gaps per day', 0, 0, 'Unlimited', 'Hard: unused available teaching opportunities inside an occupied day block; lunch and unavailable periods are excluded.'],
  ['latestStartSlot', 'Latest start', 1, 2147483647, 'Unlimited', 'Hard: first lesson must start by this regular teaching opportunity; reserved periods do not increment its position.'],
  ['preferredLatestStartSlot', 'Preferred latest start', 1, 3, 'No preference', 'Soft: later starts incur an advisory penalty.'],
  ['dailyLessonSpreadLimit', 'Maximum difference in daily lesson counts', 0, 2147483647, 'Unlimited', 'Hard: compares every pair of weekdays with available regular teaching, including untaught days.'],
  ['maxDailyLessonSpread', 'Preferred difference in daily lesson counts', 0, 1, 'No preference', 'Soft: each pair of such weekdays incurs advisory cost beyond this tolerance.']
];
export function classIssues(draft) {
  const issues=[];
  for(const row of draft.classes) {
    const add=message=>issues.push({section:'Classes',record:row.id,message:`${display(row)||'Unnamed class'}: ${message}`});
    if(!display(row).trim()) add('enter a class name.');
    if(!Number.isInteger(row.size)||row.size<1||row.size>2147483647) add('student count must be a positive integer.');
    for(const [field,label,min] of classControls) if(Object.hasOwn(row,field)&&(!Number.isInteger(row[field])||row[field]<min||row[field]>2147483647)) add(`${label} needs an integer of at least ${min}, or its labelled unrestricted option.`);
    if(Object.hasOwn(row,'curatorTeacherId')&&!draft.teachers.some(t=>t.id===row.curatorTeacherId)) add(`curator ${row.curatorTeacherId} is missing.`);
    if(Object.hasOwn(row,'homeRoomId')&&!draft.rooms.some(r=>r.id===row.homeRoomId)) add(`home room ${row.homeRoomId} is missing.`);
  }
  return [...issues, ...splitIssues(draft)];
}
export function protectedPlans(draft, classId) {
  return draft.plans.filter(p=>(p.classId??p.cohortId)===classId&&(p.protected||p.protectedReasons?.length||(p.occurrences??[]).some(o=>o.protected||o.periodLock||o.roomLock||o.exception)));
}
function classFields(draft, row, html) {
  const reference=(field,label,values)=>`<label>${label}<select data-catalog-field="${field}" data-value-type="reference"><option value="">None</option>${row[field]&&!values.some(v=>v.id===row[field])?`<option value="${html(row[field])}" selected>Missing: ${html(row[field])}</option>`:''}${values.map(v=>`<option value="${html(v.id)}" ${v.id===row[field]?'selected':''}>${html(display(v))}</option>`).join('')}</select></label>`;
  return `<div class="setup-context-fields"><label>Student count<input data-catalog-field="size" data-value-type="required-integer" inputmode="numeric" value="${html(row.size??'')}"></label>
    ${reference('curatorTeacherId','Curator (optional)',draft.teachers)}${reference('homeRoomId','Preferred home room (optional)',draft.rooms)}</div>
    <p>A curator can look after several classes; choosing one doesn’t add lessons. The home room is a preference unless a specific lesson or room rule requires it.</p>
    <details><summary>Class scheduling settings</summary><div class="setup-context-fields">${classControls.map(([field,label,min,defaultValue,special,help])=>{
      const present=Object.hasOwn(row,field),mode=!present?'default':row[field]===2147483647?'unrestricted':'value';
      const soft=field==='maxDailyGaps'&&Object.hasOwn(draft,'audiencePlans');
      const effective=soft?'Soft student gaps (no hard bound)':defaultValue===2147483647?special:String(defaultValue);
      return `<label>${label}<select data-class-mode="${field}"><option value="default" ${mode==='default'?'selected':''}>Inherited default: ${effective}</option><option value="value" ${mode==='value'?'selected':''}>Explicit value</option><option value="unrestricted" ${mode==='unrestricted'?'selected':''}>${special}</option></select>
        <input data-catalog-field="${field}" data-value-type="required-integer" inputmode="numeric" aria-label="${label} value" value="${html(mode==='value'?row[field]:'')}" ${mode==='value'?'':'disabled'}><small>${soft?'Internal student gaps are minimized before ordinary preferences; an explicit value remains hard.':help} Default: ${effective}; minimum explicit value: ${min}.</small></label>`;
    }).join('')}</div></details>
    ${['availablePeriodIds','undesirablePeriodIds','availability','lunchPeriodIds'].some(f=>Object.hasOwn(row,f))?`<details><summary>Retained availability and preferences</summary><p>These exact restrictions remain active. Lunch reasons remain separate. Use the corresponding grid controls or the advanced JSON route to change them.</p><pre>${html(JSON.stringify(Object.fromEntries(['availablePeriodIds','undesirablePeriodIds','availability','lunchPeriodIds'].filter(f=>Object.hasOwn(row,f)).map(f=>[f,row[f]])),null,2))}</pre></details>`:''}
    <button data-open-audience-plans="${html(row.id)}">Whole-class and group teaching</button><button data-open-class-plan="${html(row.id)}" class="secondary">Open class teaching plan</button><button data-open-class-lunch="${html(row.id)}" class="secondary">Class lunch</button>${splitsContent(draft,row,html)}${row.splits?.length?`<button data-open-relationships="${html(row.id)}" class="secondary">Split relationships</button>`:''}`;
}
export function subjectIssues(draft) {
  const issues = [];
  for (const s of draft.subjects) {
    const add = message => issues.push({ section: 'Subjects', record: s.id, message: `${display(s) || 'Unnamed subject'}: ${message}` });
    if (!display(s).trim()) add('enter a subject name.');
    for (const [field, label, type] of subjectControls) if (Object.hasOwn(s, field)) {
      if (type === 'boolean' && typeof s[field] !== 'boolean') add(`${label} must be true or false.`);
      if (type === 'positive' && (!Number.isInteger(s[field]) || s[field] <= 0 || s[field] > 2147483647)) add(`${label} must be a positive integer or Unlimited.`);
    }
    if (Object.hasOwn(s, 'maxWeeklyReservedLessonsPerCohort') && s.reservedPeriodsAllowed !== true) add('a reserved maximum requires explicit reserved-slot permission.');
    for (const plan of draft.plans.filter(p => p.subjectId === s.id)) for (const rule of plan.blockRules ?? []) {
      if (Number.isInteger(s.maxDailyLessonsPerCohort) && Number(rule.length) > s.maxDailyLessonsPerCohort)
        add(`daily maximum ${s.maxDailyLessonsPerCohort} is below required block length ${rule.length} in class ${plan.classId ?? plan.cohortId}.`);
    }
  }
  return issues;
}
export function referencesTo(draft, collection, id) {
  const patterns = { subjects: /subject(ids|id)$/i, teachers: /teacher(ids|id)$/i, classes: /(class|cohort)(ids|id)$/i, rooms: /room(ids|id|lock)$/i };
  const found = [];
  function visit(value, path) {
    if (!value || typeof value !== 'object') return;
    for (const [key, child] of Object.entries(value)) {
      if (path.length === 0 && [collection, 'source', 'check', 'pendingInput', 'focused', 'searches'].includes(key)) continue;
      const next = [...path, key];
      if (patterns[collection].test(key) && (child === id || Array.isArray(child) && child.includes(id))) found.push(next.join('.'));
      if (collection === 'subjects' && key === 'eligibleClassIdsBySubject' && Object.hasOwn(child ?? {}, id)) found.push([...next,id].join('.'));
      if (collection === 'classes' && path.at(-1) === 'eligibleClassIdsBySubject' && Array.isArray(child) && child.includes(id)) found.push(next.join('.'));
      visit(child, next);
    }
  }
  visit(draft, []); return [...new Set(found)];
}
export function controlFields(record, fields, html) {
  return fields.map(([field,label,type,help]) => `<label>${html(label)}${type === 'boolean'
    ? `<select data-catalog-field="${field}" data-value-type="boolean"><option value="default" ${!Object.hasOwn(record,field)?'selected':''}>Use default (off)</option><option value="true" ${record[field]===true?'selected':''}>On · hard rule</option><option value="false" ${record[field]===false?'selected':''}>Off · explicit</option></select>`
    : `<input data-catalog-field="${field}" data-value-type="integer" inputmode="numeric" placeholder="Unlimited" value="${html(record[field] ?? '')}">`}
    <small>${html(help)}</small></label>`).join('');
}
export function catalogDetail(draft, key, record, html) {
  if (!record) return '<p>Select a record or add one.</p>';
  const singular={subjects:'Subject',classes:'Class',teachers:'Teacher',rooms:'Room'}[key];
  const issueList = catalogIssues(draft,key).filter(i=>i.record===record.id&&!i.split);
  return `<div data-catalog-record="${html(record.id)}"><label>${singular} name<input id="setup-record-name" data-catalog-field="displayName" value="${html(display(record))}"></label>
    <p class="sr-only">Identifier: ${html(record.id)}</p>
    ${key === 'subjects' ? `<details><summary>Subject scheduling rules</summary><p>These rules apply to every class using this subject. Consecutive permission is set separately in each class plan.</p><div class="setup-context-fields">${controlFields(record,subjectControls,html)}</div></details>` : ''}
    ${key === 'classes'?classFields(draft,record,html):''}
    ${key === 'teachers'?teacherFields(draft,record,html):''}
    ${key === 'rooms'?roomFields(draft,record,html):''}
    ${key === 'subjects'?subjectTeachingContent(draft,record,html)+subjectRoomContent(draft,record,html):''}
    <div id="setup-record-issues" role="status">${issueList.map(i=>`<p>${html(i.message)}</p>`).join('')}</div>
    <button id="setup-remove-record" class="secondary">Remove ${singular.toLowerCase()}…</button></div>`;
}
function catalogIssues(draft,key){
  const section={subjects:'Subjects',classes:'Classes',teachers:'Teachers',rooms:'Rooms'}[key];
  return [...(key==='subjects'?subjectIssues(draft):key==='classes'?classIssues(draft):key==='teachers'?teacherIssues(draft):[]),...roomIssues(draft).filter(i=>i.section===section)];
}
export function createCatalogActions({ getDraft, getKey, commit, contextOnly, notify, confirm, html }) {
  function edit(event) {
    const field = event.target.dataset.catalogField||event.target.dataset.classMode;
    if(!field&&(event.target.dataset.teacherQualification||event.target.dataset.teacherClass)){
      const draft=getDraft(),row=draft.teachers.find(t=>t.id===event.target.closest('[data-catalog-record]').dataset.catalogRecord);
      if(event.target.dataset.teacherQualification){
        const id=event.target.dataset.teacherQualification;row.qualifiedSubjectIds??=[];
        row.qualifiedSubjectIds=row.qualifiedSubjectIds.filter(s=>s!==id);if(event.target.checked)row.qualifiedSubjectIds.push(id);
      }else{
        const subjectId=event.target.dataset.teacherSubject,classId=event.target.dataset.teacherClass;
        row.eligibleClassIdsBySubject={...row.eligibleClassIdsBySubject,[subjectId]:classesFor(row,subjectId).filter(c=>c!==classId)};
        if(event.target.checked)row.eligibleClassIdsBySubject[subjectId].push(classId);
        const summary=event.target.closest('details').querySelector('summary');summary.textContent=`Eligible classes for ${display(draft.subjects.find(s=>s.id===subjectId))} (${row.eligibleClassIdsBySubject[subjectId].length})`;
      }
      commit(draft,false);document.querySelector('#setup-record-issues').innerHTML=teacherIssues(draft).filter(i=>i.record===row.id).map(i=>`<p>${html(i.message)}</p>`).join('');return true;
    }
    if (!field) return false;
    const draft = getDraft(), key = getKey(), row = draft[key].find(r=>r.id===event.target.closest('[data-catalog-record]').dataset.catalogRecord);
    const raw = event.target.value;
    if(event.target.dataset.classMode){
      if(raw==='default')delete row[field];else row[field]=raw==='unrestricted'?2147483647:'';
      const input=event.target.parentElement.querySelector('input');input.disabled=raw!=='value';input.value='';
    } else if(event.target.dataset.valueType==='reference'){if(raw==='')delete row[field];else row[field]=raw;}
    else if (event.target.dataset.valueType === 'boolean') { if (raw === 'default') delete row[field]; else row[field] = raw === 'true'; }
    else if (['integer','required-integer'].includes(event.target.dataset.valueType)) {
      if (raw === ''&&event.target.dataset.valueType==='integer') delete row[field]; else row[field] = /^-?\d+$/.test(raw) && Number.isSafeInteger(Number(raw)) ? Number(raw) : raw;
    } else row[field] = raw;
    commit(draft, false);
    const issues = document.querySelector('#setup-record-issues');
    if (issues) issues.innerHTML = catalogIssues(draft,key).filter(i=>i.record===row.id&&!i.split).map(i=>`<p>${html(i.message)}</p>`).join('');
    if(key==='classes')refreshSplitFeedback(draft,row,event.target.closest('[data-catalog-record]'),html);
    const button = [...document.querySelectorAll('[data-record]')].find(b=>b.dataset.record===row.id);
    if (button) button.textContent=display(row) || '(unnamed)';
    return true;
  }
  return {
    input: edit,
    change: edit,
    click(event) {
      const button = event.target.closest('button'); if (!button) return false;
      const draft = getDraft(), key = getKey();
      if (button.id === 'setup-add-record' && ['subjects','classes','teachers','rooms'].includes(key)) {
        const next=clone(draft), row={id:uid(), displayName:'',...(key==='classes'?{size:''}:key==='teachers'?{qualifiedSubjectIds:[],eligibleClassIdsBySubject:{},availability:{intent:'all',unavailablePeriodIds:[]}}:key==='rooms'?{capacity:'',capabilityIds:[],availability:{intent:'all',unavailablePeriodIds:[]}}:{})}; next[key].push(row);next.focused[key]=row.id;next.searches[key]='';commit(next);
        document.querySelector('#setup-record-name')?.focus();return true;
      }
      if(button.id==='setup-remove-record'&&key==='rooms'){
        const id=button.closest('[data-catalog-record]').dataset.catalogRecord,row=draft.rooms.find(r=>r.id===id),refs=referencesTo(draft,key,id);
        if(refs.length){notify(`Cannot remove ${display(row)||id}. Resolve home-room, policy, preference and occurrence references first: ${refs.join('; ')}.`);return true;}
        confirm('Remove room?',`Remove ${display(row)||id}, capacity, capabilities and availability? No remaining home-room, policy, preference or occurrence reference uses this room.`,()=>{const next=clone(getDraft());next.rooms=next.rooms.filter(r=>r.id!==id);delete next.focused.rooms;commit(next);},'Remove room');return true;
      }
      if(button.dataset.teacherAvailability){notify('Teacher availability and preferences use the school teaching grid. New teachers are available in every teaching period. Availability marking is a separate action.');return true;}
      if(button.dataset.teacherPlan){
        const plan=draft.plans.find(p=>p.id===button.dataset.teacherPlan);const next=clone(draft);
        next.section='Classes';next.focused.classes=plan.classId??plan.cohortId;next.pendingInput.planId=plan.id;next.pendingInput.planClassId=plan.classId??plan.cohortId;contextOnly(next);
        notify('Weekly allocation belongs to this class teaching plan. Teacher qualification and eligibility were not changed.');return true;
      }
      if(button.id==='setup-remove-record'&&key==='teachers'){
        const id=button.closest('[data-catalog-record]').dataset.catalogRecord,row=draft.teachers.find(t=>t.id===id),refs=referencesTo(draft,key,id);
        if(refs.length){notify(`Cannot remove ${display(row)||id}. Resolve allocations, curator nominations and retained rules first: ${refs.join('; ')}.`);return true;}
        confirm('Remove teacher?',`Remove ${display(row)||id}, qualification, eligibility and availability? No teaching, curator or retained rule references this teacher.`,()=>{const next=clone(getDraft());next.teachers=next.teachers.filter(t=>t.id!==id);delete next.focused.teachers;commit(next);},'Remove teacher');return true;
      }
      if(button.dataset.openClassPlan){notify('This class owns its weekly subjects and teacher allocations. Continue with its teaching plan to allocate weekly demand. No demand or teacher eligibility has been created.');return true;}
      if(button.id==='setup-remove-record'&&key==='classes'){
        const id=button.closest('[data-catalog-record]').dataset.catalogRecord,row=draft.classes.find(r=>r.id===id),plans=draft.plans.filter(p=>(p.classId??p.cohortId)===id);
        const protectedRows=protectedPlans(draft,id);
        if(protectedRows.length){notify(`Cannot remove ${display(row)||id}. Protected imported occurrence settings in rows ${protectedRows.map(p=>p.id).join(', ')} must be resolved through advanced JSON first.`);return true;}
        const remaining=clone(draft);remaining.plans=remaining.plans.filter(p=>(p.classId??p.cohortId)!==id);
        for(const t of remaining.teachers)for(const values of Object.values(t.eligibleClassIdsBySubject??{})){const index=values.indexOf(id);if(index>=0)values.splice(index,1);}
        const refs=referencesTo(remaining,'classes',id);
        if(refs.length){notify(`Cannot remove ${display(row)||id}. Resolve retained references first: ${refs.join('; ')}.`);return true;}
        const scope=plans.map(p=>`Row ${p.id}: subject ${p.subjectId}, weekly demand ${p.requiredCount??p.weeklyCount??'unfinished'}, allocations ${JSON.stringify(p.allocations??[])}, blocks ${JSON.stringify(p.blockRules??[])}, fixed room ${p.fixedRoomId??'none'}`).join('; ');
        confirm('Remove class and its teaching plan?',`Remove ${display(row)||id}, its ${plans.length} teaching rows, its lunch markings and all class-specific teacher eligibility. ${scope}. Other teachers, rooms, subjects and classes are kept.`,()=>{remaining.classes=remaining.classes.filter(r=>r.id!==id);delete remaining.focused.classes;commit(remaining);},'Remove class and plan');return true;
      }
      if (button.id === 'setup-remove-record' && key === 'subjects') {
        const id=button.closest('[data-catalog-record]').dataset.catalogRecord, row=draft[key].find(r=>r.id===id), refs=referencesTo(draft,key,id);
        if (refs.length) { notify(`Cannot remove ${display(row) || id}. Resolve references first: ${refs.join('; ')}.`); return true; }
        confirm('Remove subject?', `Remove ${display(row) || id}? No class plan, qualification, eligibility or retained rule references it.`, ()=>{
          const next=clone(getDraft());next[key]=next[key].filter(r=>r.id!==id);delete next.focused[key];commit(next);
        }, 'Remove subject');return true;
      }
      return false;
    }
  };
}
