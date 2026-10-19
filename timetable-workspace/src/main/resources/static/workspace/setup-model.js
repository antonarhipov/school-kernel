import { audienceReadiness, activeAudienceClass } from './setup-audience-readiness.js';
import { coordinationIssues, readableCoordination } from './setup-coordination.js';
import { gridIssues } from './setup-grid.js';
import { subjectIssues, classIssues } from './setup-catalog.js';
import { teacherIssues } from './setup-teachers.js';
import { planIssues } from './setup-plans.js';
import { lunchIssues } from './setup-lunch.js';
import { roomIssues } from './setup-rooms.js';
import { blockIssues } from './setup-blocks.js';
import { readableSplits } from './setup-splits.js';
import { resolveRelationships, readableRelationships } from './setup-relationships.js';
import { audiencePlanIssues, readableAudiencePlans } from './setup-audience-plans.js';
import { audienceTeacherIssues } from './setup-audience-teachers.js';
export const sections = ['Timetable grid', 'Subjects', 'Classes', 'Teachers', 'Rooms', 'Review'];
export const entityKeys = { Subjects: 'subjects', Classes: 'classes', Teachers: 'teachers', Rooms: 'rooms' };
export const identity = () => crypto.randomUUID();
export const copy = value => structuredClone(value);
export function emptyDraft() {
  return { formatVersion: 1, id: identity(), source: null, name: '', revision: 0,
    section: sections[0], focused: {}, searches: {}, pendingInput: {}, validation: 'Incomplete',
    grid: { activeDays: [], intervals: [] }, subjects: [], teachers: [], classes: [], rooms: [], plans: [],
    retained: null, createdAt: new Date().toISOString() };
}
export function sourceDraft(school) {
  const draft = school.authoring ? copy(school.authoring) : emptyDraft();
  draft.id = identity(); draft.source = { id: school.id, name: school.name, revision: school.definitionRevision ?? null };
  draft.name = school.name; draft.section = sections[0]; draft.revision = 0; draft.validation = 'Unchecked';
  if (!school.authoring) {
    draft.retained = copy(school.definition);
    for (const [section, key] of Object.entries(entityKeys)) {
      draft[key] = copy(school.definition[key === 'classes' ? 'cohorts' : key]);
    }
  }
  return draft;
}
export function readableDraft(draft) {
  const object=value=>value&&typeof value==='object'&&!Array.isArray(value);
  const optionalArray=(row,key)=>!Object.hasOwn(row,key)||Array.isArray(row[key]);
  const references=row=>['availablePeriodIds','undesirablePeriodIds','lunchPeriodIds','qualifiedSubjectIds','capabilityIds','requiredRoomCapabilityIds','preferredRoomIds'].every(key=>optionalArray(row,key)&&(!row[key]||row[key].every(id=>typeof id==='string')))
    &&(!row.availability||object(row.availability)&&['availablePeriodIds','unavailablePeriodIds'].every(key=>optionalArray(row.availability,key)&&(!row.availability[key]||row.availability[key].every(id=>typeof id==='string'))));
  return (!draft?.advanced || object(draft.advanced) && typeof draft.advanced.text==='string' && typeof draft.advanced.dirty==='boolean')
    && draft?.formatVersion === 1 && typeof draft.id === 'string' && typeof draft.name === 'string'
    && Number.isInteger(draft.revision) && sections.includes(draft.section)
    && ['focused','searches','pendingInput'].every(key=>object(draft[key]))
    && draft.grid && Array.isArray(draft.grid.activeDays) && draft.grid.activeDays.every(day=>typeof day==='string')
    && Array.isArray(draft.grid.intervals) && draft.grid.intervals.every(i=>object(i)&&['id','day','kind','label','start','end'].every(k=>typeof i[k]==='string'))
    && ['subjects', 'teachers', 'classes', 'rooms', 'plans'].every(key => Array.isArray(draft[key])
      && draft[key].every(row => object(row) && typeof row.id === 'string'&&references(row)))
    && draft.classes.every(c=>readableSplits(c)&&readableRelationships(c)) && readableAudiencePlans(draft) && readableCoordination(draft)
    && draft.teachers.every(t=>!t.eligibleClassIdsBySubject||object(t.eligibleClassIdsBySubject)&&Object.values(t.eligibleClassIdsBySubject).every(ids=>Array.isArray(ids)&&ids.every(id=>typeof id==='string')))
    && draft.plans.every(p=>['allocations','occurrences','blockRules','protectedReasons'].every(key=>optionalArray(p,key))
      &&(p.allocations??[]).every(object)&&(p.blockRules??[]).every(object)&&(p.occurrences??[]).every(o=>object(o)&&typeof o.id==='string'&&references(o)))
    &&(!draft.roomAssignments||Array.isArray(draft.roomAssignments)&&draft.roomAssignments.every(p=>object(p)&&optionalArray(p,'allowedRoomIds')));
}
export function draftIssues(draft) {
  const issues = [];
  if (!draft.name.trim()) issues.push({ section: 'Review', message: 'Enter a school name.' });
  if (draft.retained) return issues;
  issues.push(...gridIssues(draft));
  if(!draft.plans.some(p=>(p.allocations??[]).some(a=>Number.isInteger(a.count)&&a.count>0))&&!(draft.audiencePlans??[]).some(p=>p.portions?.some(q=>q.sessionIds.length)))issues.push({section:'Classes',message:'Allocate at least one lesson.'});
  issues.push(...subjectIssues(draft));
  const activeSplits=new Set(draft.classes.flatMap(c=>[...activeAudienceClass(draft,c).required]));
  issues.push(...classIssues(draft).filter(i=>!i.split||activeSplits.has(i.split)));
  issues.push(...audienceReadiness(draft).issues);
  issues.push(...teacherIssues(draft));
  issues.push(...planIssues(draft));
  issues.push(...audiencePlanIssues(draft));
  issues.push(...audienceTeacherIssues(draft));
  issues.push(...coordinationIssues(draft));
  issues.push(...lunchIssues(draft));
  issues.push(...roomIssues(draft));
  issues.push(...blockIssues(draft));
  for (const [section, key] of Object.entries(entityKeys)) {
    if (!draft[key].length) issues.push({ section, message: `Add ${section.toLowerCase()}.` });
  }
  return issues;
}
