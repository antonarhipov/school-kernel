import { schoolDocumentSchema } from './school-document-schema.js';
import { emptyDraft, draftIssues } from './setup-model.js';
import { strictParse } from './strict-json.js';
import { syntaxFeedback } from './demo-definitions.js';
export { schoolDocumentSchema };
const clone = value => structuredClone(value);
const fields = Object.keys(schoolDocumentSchema.properties).filter(k => !['documentType', 'formatVersion'].includes(k));
const pointer = value => String(value).replace(/~/g, '~0').replace(/\//g, '~1');
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);

// The schema uses only these JSON Schema keywords. Keeping the evaluator local makes
// structural validation available offline and uses the same schema as schema download.
export function structureIssues(value, schema = schoolDocumentSchema, path = '') {
  const issues = [], add = message => issues.push({ path: path || '/', message });
  const types = schema.type ? [].concat(schema.type) : [];
  const matches = type => type === 'object' ? object(value) : type === 'array' ? Array.isArray(value)
    : type === 'integer' ? Number.isSafeInteger(value) : typeof value === type;
  if (types.length && !types.some(matches)) { add(`Expected ${types.join(' or ')}.`); return issues; }
  if (Object.hasOwn(schema, 'const') && value !== schema.const) add(`Expected ${JSON.stringify(schema.const)}.`);
  if (typeof value==='number' && schema.minimum!==undefined && value<schema.minimum) add(`Use a value of at least ${schema.minimum}.`);
  if (schema.enum && !schema.enum.includes(value)) add(`Choose one of ${schema.enum.join(', ')}.`);
  if (object(value)) {
    for (const key of schema.required ?? []) if (!Object.hasOwn(value, key)) issues.push({ path: `${path}/${pointer(key)}`, message: 'Required field is missing.' });
    for (const [key, item] of Object.entries(value)) {
      const child = schema.properties && Object.hasOwn(schema.properties,key) ? schema.properties[key] : schema.additionalProperties;
      if (child === false) issues.push({ path: `${path}/${pointer(key)}`, message: 'Unsupported field. Remove it or use a supported document version.' });
      else if (object(child)) issues.push(...structureIssues(item, child, `${path}/${pointer(key)}`));
    }
  }
  if (Array.isArray(value) && schema.items) value.forEach((item, index) => issues.push(...structureIssues(item, schema.items, `${path}/${index}`)));
  return issues;
}
export function parseSchoolDocument(text) {
  if (new TextEncoder().encode(text).length > 16 * 1024 * 1024) throw new Error('/: School document exceeds 16 MiB.');
  const syntax = syntaxFeedback(text); if (syntax) throw new Error(syntax);
  const value = strictParse(text), issues = structureIssues(value);
  if (issues.length) throw new Error(issues.map(i => `${i.path}: ${i.message}`).join('\n'));
  return value;
}
export function schoolDocument(draft) {
  if (draft.retained) throw new Error('Review and accept the source conversion before editing its school document.');
  const document = { documentType: 'school-setup', formatVersion: 1,
    ...Object.fromEntries(fields.filter(key => Object.hasOwn(draft, key)).map(key => [key, clone(draft[key])])) };
  document.kernelSchoolId ??= draft.id;
  // Keep each setting in one editable place. Internal solver-presence markers are
  // explicit, but generated period/block/identity values never compete with forms.
  const extras = document.kernelExtras ?? {};
  const present = ['reservedPeriodIds','consecutiveBlocks'].filter(key => Object.hasOwn(extras,key));
  for (const key of ['schemaVersion','schoolId','reservedPeriodIds','consecutiveBlocks']) delete extras[key];
  if (Object.keys(extras).length) document.kernelExtras = extras; else delete document.kernelExtras;
  if (present.length) document.kernelCollectionPresence = present;
  for (const plan of document.plans) for (const occurrence of plan.occurrences ?? []) { delete occurrence.subjectId; delete occurrence.cohortId; }
  return document;
}
export function applySchoolDocument(draft, document) {
  const issues = structureIssues(document);
  if (issues.length) throw new Error(issues.map(i => `${i.path}: ${i.message}`).join('\n'));
  const next = clone(draft);
  for (const key of fields) { delete next[key]; if (Object.hasOwn(document, key)) next[key] = clone(document[key]); }
  for (const key of document.kernelCollectionPresence ?? []) { next.kernelExtras ??= {}; next.kernelExtras[key] = []; }
  delete next.kernelCollectionPresence;
  delete next.retained; delete next.importedDefinition; delete next.check; delete next.checkIssues;
  delete next.advanced;
  next.validation = 'Incomplete';
  // Selection state may reference records that JSON removed. Reset it, never data.
  next.focused = {}; next.searches = {}; next.pendingInput = {};
  return next;
}
export function importSchoolDocument(document) { return applySchoolDocument(emptyDraft(), document); }
export function locatedDraftIssues(draft) {
  const issues = draftIssues(draft).map(issue => {
    const key = ({ Subjects: 'subjects', Teachers: 'teachers', Classes: 'classes', Rooms: 'rooms' })[issue.section];
    const index = key ? draft[key].findIndex(row => row.id === issue.record) : -1;
    const plan = draft.plans.findIndex(row => row.id === issue.plan);
    const interval = draft.grid.intervals.findIndex(row => row.id === issue.record);
    return { ...issue, path: plan >= 0 ? `/plans/${plan}` : interval >= 0 ? `/grid/intervals/${interval}` : index >= 0 ? `/${key}/${index}` : key ? `/${key}` : issue.section === 'Timetable grid' ? '/grid' : '/name' };
  });
  const add = (path, message) => {
    const parts=path.split('/'),key=parts[1],row=draft[key]?.[Number(parts[2])];
    const section=({subjects:'Subjects',teachers:'Teachers',classes:'Classes',rooms:'Rooms',plans:'Classes',grid:'Timetable grid'})[key]??'Review';
    const interval=key==='grid'&&parts[2]==='intervals'?draft.grid.intervals[Number(parts[3])]:null;
    issues.push({path,section,record:key==='plans'?(row?.classId??row?.cohortId):interval?.id??row?.id, ...(key==='plans'?{plan:row?.id}:{}),message});
  };
  const ids = {};
  for (const key of ['subjects', 'teachers', 'classes', 'rooms', 'plans']) {
    ids[key] = new Set();
    draft[key].forEach((row, index) => { if (!row.id || ids[key].has(row.id)) add(`/${key}/${index}/id`, 'Use a nonempty unique identifier.'); ids[key].add(row.id); });
  }
  const integer = (value, min) => Number.isInteger(value) && value >= min && value <= 2147483647;
  const numericFields = {subjects:{maxWeeklyReservedLessonsPerCohort:1,maxDailyLessonsPerCohort:1},classes:{size:1,maxDailyGaps:0,latestStartSlot:1,preferredLatestStartSlot:1,dailyLessonSpreadLimit:0,maxDailyLessonSpread:0},rooms:{capacity:0}};
  for (const [key, rules] of Object.entries(numericFields)) draft[key].forEach((row,index) => {
    for (const [field,min] of Object.entries(rules)) if ((Object.hasOwn(row,field) || ['size','capacity'].includes(field)) && !integer(row[field],min)) add(`/${key}/${index}/${field}`, `Enter an integer from ${min} to 2147483647.`);
  });
  for (const key of ['subjects','teachers','classes','rooms']) draft[key].forEach((row,index) => {
    if (!(row.displayName ?? row.name ?? '').trim()) add(`/${key}/${index}/displayName`, 'Enter a name.');
  });
  const periods = new Set(), intervals = new Set(), occurrences = new Set();
  draft.grid.intervals.forEach((row, index) => {
    if (intervals.has(row.id)) add(`/grid/intervals/${index}/id`, 'Duplicate interval identifier.'); intervals.add(row.id);
    if (['Lesson', 'Reserved lesson'].includes(row.kind)) {
      if (!row.periodId || periods.has(row.periodId)) add(`/grid/intervals/${index}/periodId`, 'Teaching intervals need a unique period identifier.');
      periods.add(row.periodId);
    }
  });
  function reference(value, collection, path) { if (!collection.has(value)) add(path, `Unknown reference ${JSON.stringify(value)}.`); }
  for (const key of ['teachers', 'classes', 'rooms']) draft[key].forEach((row, index) => {
    for (const field of ['availablePeriodIds', 'undesirablePeriodIds', 'lunchPeriodIds']) row[field]?.forEach((id, n) => reference(id, periods, `/${key}/${index}/${field}/${n}`));
    for (const field of ['availablePeriodIds', 'unavailablePeriodIds']) row.availability?.[field]?.forEach((id, n) => reference(id, periods, `/${key}/${index}/availability/${field}/${n}`));
    row.qualifiedSubjectIds?.forEach((id, n) => reference(id, ids.subjects, `/${key}/${index}/qualifiedSubjectIds/${n}`));
    for (const [subject, classes] of Object.entries(row.eligibleClassIdsBySubject ?? {})) {
      reference(subject, ids.subjects, `/${key}/${index}/eligibleClassIdsBySubject/${pointer(subject)}`);
      classes.forEach((id, n) => reference(id, ids.classes, `/${key}/${index}/eligibleClassIdsBySubject/${pointer(subject)}/${n}`));
    }
  });
  draft.plans.forEach((plan, index) => {
    if (!integer(plan.requiredCount,1)) add(`/plans/${index}/requiredCount`, 'Enter a positive weekly lesson count.');
    if (plan.classId && plan.cohortId && plan.classId !== plan.cohortId) add(`/plans/${index}/cohortId`, 'Class references disagree. Use one class identifier.');
    plan.allocations?.forEach((a,n) => { if(!integer(a.count,1)) add(`/plans/${index}/allocations/${n}/count`, 'Enter a positive allocation count.'); });
    plan.blockRules?.forEach((rule,n) => { for(const [field,min] of [['length',2],['count',1]]) if(!integer(rule[field],min)) add(`/plans/${index}/blockRules/${n}/${field}`, `Enter an integer of at least ${min}.`); });
    const total=(plan.allocations??[]).reduce((sum,a)=>sum+(integer(a.count,1)?a.count:0),0);
    if(integer(plan.requiredCount,1)&&total!==plan.requiredCount)add(`/plans/${index}/allocations`, `${Math.abs(plan.requiredCount-total)} ${total<plan.requiredCount?'unassigned':'over-allocated'} lessons. Required demand and allocations must match before saving.`);
    if ((plan.occurrences??[]).length!==total) add(`/plans/${index}/occurrences`, 'Individual lesson count must match allocations. Correct the occurrences or allocations explicitly.');
    for(const [n,a] of (plan.allocations??[]).entries()) if((plan.occurrences??[]).filter(o=>o.teacherId===a.teacherId).length!==a.count) add(`/plans/${index}/allocations/${n}/count`, 'Allocation count does not match this teacher’s individual lessons.');
    reference(plan.classId ?? plan.cohortId, ids.classes, `/plans/${index}/classId`);
    reference(plan.subjectId, ids.subjects, `/plans/${index}/subjectId`);
    plan.allocations?.forEach((a, n) => reference(a.teacherId, ids.teachers, `/plans/${index}/allocations/${n}/teacherId`));
    plan.occurrences?.forEach((o, n) => {
      if (occurrences.has(o.id)) add(`/plans/${index}/occurrences/${n}/id`, 'Duplicate lesson identifier.'); occurrences.add(o.id);
      reference(o.teacherId, ids.teachers, `/plans/${index}/occurrences/${n}/teacherId`);
      if(plan.fixedRoomId&&o.roomLock&&plan.fixedRoomId!==o.roomLock)add(`/plans/${index}/occurrences/${n}/roomLock`, 'The individual room lock conflicts with the row fixed room. Resolve it explicitly.');
    });
  });
  return issues;
}

// Server validation speaks the generated solver contract. Point errors back to
// the complete school document the administrator is actually editing.
export function schoolDocumentPath(draft, definition, path) {
  const [, collection, rawIndex, ...rest] = path.split('/'), index = Number(rawIndex);
  const tail = suffix => suffix.length ? '/'+suffix.join('/') : '';
  if (collection === 'cohorts') return '/classes'+(rawIndex===undefined?'':`/${rawIndex}${tail(rest)}`);
  if (collection === 'schoolId') return '/kernelSchoolId';
  if (collection === 'displayName') return '/name';
  if (collection === 'softConstraintOverrides') return `/kernelExtras${path}`;
  if (collection === 'periods') {
    const period = definition.periods?.[index], n = draft.grid.intervals.findIndex(i => i.periodId === period?.id);
    if (n < 0) return '/grid';
    const fields = { id:'periodId',displayName:'label',weekday:'day',startTime:'start',endTime:'end',adjacentToNext:'boundaryAfter' };
    return `/grid/intervals/${n}${tail(rest.map((field,i)=>i===0?(fields[field]??field):field))}`;
  }
  if (collection === 'reservedPeriodIds') {
    const n = draft.grid.intervals.findIndex(i=>i.periodId===definition.reservedPeriodIds?.[index]);
    return n < 0 ? '/grid/intervals' : `/grid/intervals/${n}/kind`;
  }
  if (collection === 'lessons') {
    const lesson = definition.lessons?.[index];
    const pi = draft.plans.findIndex(p=>p.occurrences?.some(o=>o.id===lesson?.id));
    if (pi < 0) return '/plans';
    const plan = draft.plans[pi], oi = plan.occurrences.findIndex(o=>o.id===lesson.id);
    if (rest[0] === 'subjectId') return `/plans/${pi}/subjectId`;
    if (rest[0] === 'cohortId') return `/plans/${pi}/${Object.hasOwn(plan,'classId')?'classId':'cohortId'}`;
    if (rest[0] === 'roomLock' && plan.fixedRoomId) return `/plans/${pi}/fixedRoomId`;
    return `/plans/${pi}/occurrences/${oi}${tail(rest)}`;
  }
  if (collection === 'consecutiveBlocks') {
    const rule = definition.consecutiveBlocks?.[index], pi = draft.plans.findIndex(p=>p.blockRules?.some(r=>r.id===rule?.id));
    if (pi < 0) return '/plans';
    const plan=draft.plans[pi], ri=plan.blockRules.findIndex(r=>r.id===rule.id);
    if (rest[0] === 'cohortId') return `/plans/${pi}/${Object.hasOwn(plan,'classId')?'classId':'cohortId'}`;
    if (rest[0] === 'subjectId') return `/plans/${pi}/subjectId`;
    return `/plans/${pi}/blockRules/${ri}${tail(rest.map(field=>field==='weeklyCount'?'count':field))}`;
  }
  return path;
}
export function locateKernelFeedback(draft, definition, message) {
  return message.replace(/\/(?:subjects|teachers|cohorts|rooms|periods|lessons|consecutiveBlocks|softConstraintOverrides|reservedPeriodIds|schoolId|displayName)(?:\/[A-Za-z0-9_.~-]+)*/g,
    path => schoolDocumentPath(draft, definition, path));
}
