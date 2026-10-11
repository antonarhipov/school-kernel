import { renderPolicies, policiesBusy, policiesDirty, savePolicy, discardPolicies, resetPolicies } from './definition-room-policies.js';
import { renderOccurrences, occurrencesBusy, occurrencesDirty, saveOccurrences, discardOccurrences, resetOccurrences, chooseOccurrences } from './definition-occurrences.js';
import { renderRequirements, requirementsBusy, requirementsDirty, saveRequirement, discardRequirements, resetRequirements } from './definition-requirements.js';
import { renderRooms, roomsDirty, saveRoom, discardRooms, resetRooms } from './definition-rooms.js';
import { renderClasses, classesDirty, saveCohort, discardClasses, resetClasses } from './definition-cohorts.js';
import { renderTeachers, teachersDirty, saveTeacher, discardTeachers, resetTeachers } from './definition-teachers.js';
import { renderSubjects, subjectsDirty, saveSubject, discardSubjects, resetSubjects } from './definition-subjects.js';
import { renderGrid, gridDirty, saveGrid, discardGrid, resetGrid } from './definition-grid.js';
const sections = {
  School: [['School identity', null], ['Catalog', null], ['Advanced Preferences', 'softConstraintOverrides']],
  Grid: [['Periods', 'periods'], ['Reservations', 'reservedPeriodIds']],
  Subjects: [['Subjects', 'subjects']],
  Teachers: [['Teachers', 'teachers']],
  Classes: [['Classes', 'cohorts']],
  Rooms: [['Rooms', 'rooms']],
  'Room policies': [['Room policies', 'roomAssignments']],
  'Weekly requirements': [['Series', 'series'], ['Ungrouped lessons', 'ungrouped'], ['Occurrence details', 'lessons']],
  'Occurrence details': [['Occurrences', 'lessons']],
  Review: []
};
let section = 'Review';
let draftIdentity;
let schoolForm;
let saving = false;
let saveError = null;
const el = (tag, text, className) => {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
};

export function resetAuthoringSection(value = 'Review') { section = value; draftIdentity = null; schoolForm = null; saveError = null; resetGrid(); resetSubjects(); resetTeachers(); resetClasses(); resetRooms(); resetRequirements(); resetOccurrences(); resetPolicies(); }

export function renderDefinitionEditor(host, snapshot, narrow, inspectCurrent, actions) {
  const draft = snapshot.workspace.definitionDraft;
  const definition = draft.definition;
  const identity = draft.openedRevision || draft.draftRevision || JSON.stringify(definition);
  if (draftIdentity !== identity) { draftIdentity = identity; schoolForm = null; saveError = null; resetGrid(); resetSubjects(); resetTeachers(); resetClasses(); resetRooms(); resetRequirements(); resetOccurrences(); resetPolicies(); }
  if (!schoolForm || !schoolForm.dirty) schoolForm = schoolValues(definition, draft);
  const rerender = () => actions.render(snapshot);
  const save = async () => {
    if (saving || narrow) return false;
    saving = true; saveError = null; rerender();
    try {
      const result = await actions.save({ schoolId: schoolForm.schoolId, displayName: schoolForm.displayName }, snapshot.version);
      snapshot = result;
      schoolForm = schoolValues(result.workspace.definitionDraft.definition, result.workspace.definitionDraft);
      saving = false;
      actions.render(result);
      return true;
    } catch (error) {
      saving = false; saveError = `No change was committed. ${error.message} Working values remain Unsaved.`;
      schoolForm.dirty = true; rerender(); return false;
    }
  };
  const leave = next => {
    if (saving || requirementsBusy() || occurrencesBusy() || policiesBusy()) return;
    const leavingGrid = section === 'Grid' && gridDirty();
    const leavingSubject = section === 'Subjects' && subjectsDirty();
    const leavingTeacher = section === 'Teachers' && teachersDirty();
    const leavingPolicy = section === 'Room policies' && policiesDirty();
    const leavingOccurrence = section === 'Occurrence details' && occurrencesDirty();
    const leavingRequirement = section === 'Weekly requirements' && requirementsDirty();
    const leavingRoom = section === 'Rooms' && roomsDirty();
    const leavingClass = section === 'Classes' && classesDirty();
    if ((!schoolForm.dirty && !leavingGrid && !leavingSubject && !leavingTeacher && !leavingClass && !leavingRoom && !leavingRequirement && !leavingOccurrence && !leavingPolicy) || narrow) { next(snapshot); return; }
    const dialog = el('dialog'); dialog.id = 'school-unsaved-dialog';
    dialog.append(el('h3', leavingPolicy ? 'Unsaved room policy' : leavingOccurrence ? 'Unsaved occurrence choices' : leavingRequirement ? 'Unsaved weekly requirement' : leavingRoom ? 'Unsaved room' : leavingClass ? 'Unsaved class' : leavingTeacher ? 'Unsaved teacher' : leavingSubject ? 'Unsaved subject' : leavingGrid ? 'Unsaved weekly grid' : 'Unsaved school identity'), el('p', 'Save, discard working edits, or stay in this form. Saved review includes only saved values.'));
    const close = () => { dialog.close(); dialog.remove(); };
    for (const [label, id, handler] of [
      ['Save and continue', 'school-save-continue', async () => { close(); if (leavingGrid || leavingSubject || leavingTeacher || leavingClass || leavingRoom || leavingRequirement || leavingOccurrence || leavingPolicy) { const result = leavingPolicy ? await savePolicy(snapshot, actions) : leavingOccurrence ? await saveOccurrences(snapshot, actions) : leavingRequirement ? await saveRequirement(snapshot, actions) : leavingRoom ? await saveRoom(snapshot, actions) : leavingClass ? await saveCohort(snapshot, actions) : leavingTeacher ? await saveTeacher(snapshot, actions) : leavingSubject ? await saveSubject(snapshot, actions) : await saveGrid(snapshot, actions); if (result) { snapshot = result; next(snapshot); } } else if (await save()) next(snapshot); }],
      ['Discard working edits', 'school-discard-working', () => { if (leavingPolicy) discardPolicies(snapshot); else if (leavingOccurrence) discardOccurrences(snapshot); else if (leavingRequirement) discardRequirements(snapshot); else if (leavingRoom) discardRooms(snapshot); else if (leavingClass) discardClasses(snapshot); else if (leavingTeacher) discardTeachers(snapshot); else if (leavingSubject) discardSubjects(snapshot); else if (leavingGrid) discardGrid(definition); else schoolForm = schoolValues(definition, draft); saveError = null; close(); next(snapshot); }],
      ['Stay', 'school-stay', () => { close(); rerender(); }]
    ]) {
      const button = el('button', label); button.id = id; button.addEventListener('click', handler); dialog.append(button);
    }
    dialog.addEventListener('cancel', () => { dialog.remove(); rerender(); }); host.append(dialog); dialog.showModal();
  };
  host.className = 'card definition-authoring';
  host.replaceChildren();
  host.append(el('span', 'Saved school-definition draft', 'state'));
  host.append(el('h2', definition.displayName || 'New school'));
  const status = el('p', saving ? 'Saving… Working values are not yet durable.' : schoolForm.dirty ? 'Unsaved — Working school identity differs from the saved draft.' : 'Saved — Your saved school-definition changes are preserved.', 'notice');
  status.id = 'school-save-status'; status.setAttribute('role', 'status'); host.append(status);
  if (saveError) { const error = el('p', saveError, 'error'); error.id = 'school-save-error'; error.setAttribute('role', 'alert'); host.append(error); }
  const sourceState = draft.sourceState || (snapshot.acceptedTimetable ? 'ACCEPTED_BASELINE' : 'INITIAL_DRAFT');
  host.append(el('p', `Draft • Source: ${sourceState} • Catalog ${definition.catalogVersion} • ${snapshot.acceptedTimetable ? 'Current remains unchanged' : 'No Current timetable'}`, 'authoring-identity'));
  host.append(el('p', `Source identity: ${draft.sourceDocument?.school?.displayName || draft.sourceDocument?.initialDefinition?.displayName || draft.sourceDocument?.acceptedBaseline?.definition?.displayName || '(new school)'} • ${draft.sourceDocument?.definitionRevision || '(no source revision)'}`, 'authoring-identity'));
  if (draft.draftRevision) host.append(el('p', `Saved revision: ${draft.draftRevision}`, 'authoring-revision'));
  host.append(el('p', 'One definition draft is open. Planning, repair, manual editing, import, replacement, and clearing are locked until authoring is finished or discarded.'));
  if (narrow) host.append(el('p', 'Read-only inspection. Editing requires a wider view (more than 700 CSS px).', 'narrow-banner'));
  const issues = draft.issues || [];
  host.append(el('p', `${definition.subjects?.length || 0} subjects • ${definition.teachers?.length || 0} teachers • ${definition.cohorts?.length || 0} classes • ${definition.rooms?.length || 0} rooms • ${definition.periods?.length || 0} periods • ${definition.lessons?.length || 0} occurrences`));
  host.append(el('p', `Incomplete: ${issues.filter(issue => issue.severity === 'BLOCKING').length} blocking issues; ${issues.filter(issue => issue.severity === 'ADVISORY').length} advisories.`, 'authoring-counts'));
  const nav = el('nav'); nav.className = 'authoring-navigation'; nav.setAttribute('aria-label', 'Definition sections');
  for (const name of Object.keys(sections)) {
    const button = el('button', name, name === section ? 'selected' : 'secondary');
    button.type = 'button'; button.dataset.section = name; button.setAttribute('aria-current', name === section ? 'page' : 'false');
    button.addEventListener('click', () => leave(() => { section = name; rerender(); }));
    nav.append(button);
  }
  if (snapshot.acceptedTimetable) {
    const button = el('button', 'Inspect Current', 'secondary'); button.id = 'inspect-authoring-current';
    button.addEventListener('click', () => leave(inspectCurrent)); nav.append(button);
    const link = el('a', 'Export Current'); link.href = '/api/accepted/export'; link.id = 'export-accepted'; nav.append(link);
  }
  host.append(nav);
  const content = el('section'); content.id = 'authoring-section'; content.append(el('h3', section));
  const occurrenceActions = { ...actions, openOccurrences: ids => leave(latest => { chooseOccurrences(ids); section = 'Occurrence details'; actions.render(latest); }) };
  if (section === 'Room policies') {
    renderPolicies(content, snapshot, narrow, actions, status, leave);
  } else if (section === 'Occurrence details') {
    renderOccurrences(content, snapshot, narrow, actions, status, leave);
  } else if (section === 'Grid') {
    if (gridDirty()) status.textContent = 'Unsaved — Working grid differs from the saved draft.';
    renderGrid(content, snapshot, narrow, actions, status);
  } else if (section === 'Weekly requirements') {
    renderRequirements(content, snapshot, narrow, occurrenceActions, status, leave);
  } else if (section === 'Rooms') {
    renderRooms(content, snapshot, narrow, actions, status, leave);
  } else if (section === 'Classes') {
    renderClasses(content, snapshot, narrow, actions, status, leave);
  } else if (section === 'Teachers') {
    renderTeachers(content, snapshot, narrow, occurrenceActions, status, leave);
  } else if (section === 'Subjects') {
    renderSubjects(content, snapshot, narrow, actions, status, leave);
  } else if (section === 'Review') {
    content.append(el('p', 'Saved draft inspection. Definition verification is required before finishing.'));
    if (!definition.lessons?.length) content.append(el('p', 'No lessons configured; planning will produce an empty timetable'));
    for (const issue of issues) content.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId || ''} • ${issue.field}: ${issue.message}`));
    for (const adjustment of draft.openingAdjustments || []) content.append(el('p', `Catalog adjustment: ${adjustment}`));
    for (const pin of draft.expiredPins || []) content.append(el('p', `Expired attempt-scoped pin: ${pin.lessonId} ${pin.field} = ${JSON.stringify(pin.sourceValue)}. Exact source provenance is retained.`));
    if (!issues.length) content.append(el('p', 'No opening issues. This does not establish timetable feasibility.'));
  } else {
    for (const [label, key] of sections[section]) {
      const block = el('details'); block.open = true; block.append(el('summary', label));
      if (label === 'School identity' && !narrow) {
        renderSchoolForm(block, definition, draft, schoolForm, saving, status, save, actions, saveError);
        for (const issue of issues) block.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId || ''} • ${issue.field}: ${issue.message}`));
        content.append(block); continue;
      }
      let values;
      if (key === 'series') values = Object.entries(Object.groupBy(definition.lessons?.filter(lesson => lesson.seriesId) || [], lesson => lesson.seriesId));
      else if (key === 'ungrouped') values = definition.lessons?.filter(lesson => !lesson.seriesId) || [];
      else if (key) values = definition[key];
      else if (label === 'Catalog') values = { catalogVersion: definition.catalogVersion };
      else values = { schoolId: definition.schoolId ?? '(not entered)', displayName: definition.displayName ?? '(not entered)' };
      renderSavedValues(block, values);
      content.append(block);
    }
  }
  host.append(content);
}

const fieldNames = {
  schoolId: 'School identifier', displayName: 'Name', catalogVersion: 'Scheduling catalog',
  id: 'Identifier', qualifiedSubjectIds: 'Qualified subjects', availablePeriodIds: 'Selected available periods',
  undesirablePeriodIds: 'Undesirable periods', capacity: 'Capacity', capabilityIds: 'Capabilities',
  size: 'Class size', weekday: 'Weekday', order: 'Order', startTime: 'Start time', endTime: 'End time',
  subjectId: 'Subject', cohortId: 'Class', teacherId: 'Teacher', seriesId: 'Series',
  requiredRoomCapabilityIds: 'Required room capabilities', preferredRoomIds: 'Preferred rooms',
  periodLock: 'Persistent period lock', roomLock: 'Persistent room lock', curatorTeacherId: 'Curator', homeRoomId: 'Home room',
  constraintId: 'Preference', weight: 'Weight', allowedRoomIds: 'Allowed rooms', useHomeRoom: 'Use class home room',
  maxDailyGaps: 'Maximum daily gaps', maxDailyLessonSpread: 'Preferred daily lesson spread',
  dailyLessonSpreadLimit: 'Hard daily lesson spread', latestStartSlot: 'Latest start slot',
  preferredLatestStartSlot: 'Preferred latest start slot', reservedPeriodsAllowed: 'Reserved periods allowed',
  maxWeeklyReservedLessonsPerCohort: 'Maximum reserved lessons per class per week', dayEdgeOnly: 'Day edges only',
  maxDailyLessonsPerCohort: 'Maximum lessons per class per day', curatorLesson: 'Curator subject'
};
function renderSavedValues(host, values) {
  if (values === undefined) { host.append(el('p', 'Use catalog default (omitted)')); return; }
  if (Array.isArray(values)) {
    if (!values.length) host.append(el('p', 'None configured (saved empty selection)'));
    else for (const item of values) {
      if (Array.isArray(item)) { const group = el('details'); group.append(el('summary', item[0])); renderSavedValues(group, item[1]); host.append(group); }
      else if (typeof item === 'object' && item !== null) {
        const group = el('details'); group.append(el('summary', item.displayName || item.id || item.constraintId));
        renderSavedValues(group, item); host.append(group);
      } else host.append(el('p', String(item)));
    }
    return;
  }
  const list = el('dl'); list.className = 'authoring-fields';
  for (const [field, value] of Object.entries(values)) {
    const row = el('div'); row.append(el('dt', fieldNames[field] || field));
    row.append(el('dd', Array.isArray(value) ? (value.length ? value.join(', ') : 'None (explicit empty selection)') : value === 2147483647 ? 'No limit' : String(value)));
    list.append(row);
  }
  host.append(list);
}

function schoolValues(definition, draft) {
  return { schoolId: definition.schoolId ?? suggestSchoolId(definition.displayName ?? '', draft),
    displayName: definition.displayName ?? '', override: false, dirty: false };
}
function suggestSchoolId(name, draft) {
  const base = name.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase()
    .replace(/[^a-z0-9._-]/g, '-').replace(/-+/g, '-').replace(/^[^a-z0-9]+/, '').replace(/-+$/, '');
  const suggestion = (base || 'school').slice(0, 128);
  const reserved = new Set((draft.removedIdentities || []).filter(item => item.ownerType === 'School').map(item => item.id));
  if (!reserved.has(suggestion)) return suggestion;
  for (let suffix = 2; ; suffix++) {
    const ending = `-${suffix}`;
    const candidate = suggestion.slice(0, 128 - ending.length) + ending;
    if (!reserved.has(candidate)) return candidate;
  }
}
function renderSchoolForm(host, definition, draft, values, busy, status, save, actions, error) {
  const form = el('form'); form.id = 'school-identity-form';
  const nameLabel = el('label', 'School name');
  const name = el('input'); name.id = 'school-display-name'; name.name = 'displayName'; name.value = values.displayName; name.disabled = busy;
  nameLabel.append(name); form.append(nameLabel);
  const idLabel = el('label', definition.schoolId === undefined ? 'School identifier suggestion / advanced override' : 'School identifier (read-only)');
  const id = el('input'); id.id = 'school-identifier'; id.name = 'schoolId'; id.value = values.schoolId;
  id.readOnly = definition.schoolId !== undefined; id.disabled = busy; idLabel.append(id); form.append(idLabel);
  form.append(el('p', definition.schoolId === undefined ? 'Review the readable suggestion or enter an advanced override. The identifier becomes permanent on its first save.' : 'The established identifier stays the same when the school name changes.'));
  const changed = () => {
    values.dirty = true;
    status.textContent = 'Unsaved — Working school identity differs from the saved draft.';
    const staleError = document.querySelector('#school-save-error'); if (staleError) staleError.remove();
  };
  name.addEventListener('input', () => {
    values.displayName = name.value;
    if (definition.schoolId === undefined && !values.override) { values.schoolId = suggestSchoolId(name.value, draft); id.value = values.schoolId; }
    changed();
  });
  id.addEventListener('input', () => { values.schoolId = id.value; values.override = true; changed(); });
  const button = el('button', 'Save school identity'); button.id = 'save-school-identity'; button.type = 'submit'; button.disabled = busy; form.append(button);
  form.addEventListener('submit', event => { event.preventDefault(); save(); });
  if (error) {
    const refresh = el('button', 'Review newer saved draft for retry', 'secondary'); refresh.type = 'button'; refresh.id = 'school-refresh';
    refresh.addEventListener('click', () => actions.refresh()); form.append(refresh);
  }
  host.append(form);
}
window.addEventListener('beforeunload', event => {
  if (schoolForm?.dirty) { event.preventDefault(); event.returnValue = ''; }
});
