const sections = {
  School: [['School identity', null], ['Catalog', null], ['Advanced Preferences', 'softConstraintOverrides']],
  Grid: [['Periods', 'periods'], ['Reservations', 'reservedPeriodIds']],
  Subjects: [['Subjects', 'subjects']],
  Teachers: [['Teachers', 'teachers']],
  Classes: [['Classes', 'cohorts']],
  Rooms: [['Rooms', 'rooms']],
  'Room policies': [['Room policies', 'roomAssignments']],
  'Weekly requirements': [['Series', 'series'], ['Ungrouped lessons', 'ungrouped'], ['Occurrence details', 'lessons']],
  Review: []
};
let section = 'Review';
let draftIdentity;
const el = (tag, text, className) => {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
};

export function resetAuthoringSection(value = 'Review') { section = value; draftIdentity = null; }

export function renderDefinitionEditor(host, snapshot, narrow, inspectCurrent) {
  const draft = snapshot.workspace.definitionDraft;
  const definition = draft.definition;
  const identity = draft.openedRevision || draft.draftRevision || JSON.stringify(definition);
  if (draftIdentity !== identity) { draftIdentity = identity; }
  host.className = 'card definition-authoring';
  host.replaceChildren();
  host.append(el('span', 'Saved school-definition draft', 'state'));
  host.append(el('h2', definition.displayName || 'New school'));
  host.append(el('p', 'Saved — Your saved school-definition changes are preserved.', 'notice'));
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
    button.addEventListener('click', () => { section = name; renderDefinitionEditor(host, snapshot, narrow, inspectCurrent); });
    nav.append(button);
  }
  if (snapshot.acceptedTimetable) {
    const button = el('button', 'Inspect Current', 'secondary'); button.id = 'inspect-authoring-current';
    button.addEventListener('click', inspectCurrent); nav.append(button);
    const link = el('a', 'Export Current'); link.href = '/api/accepted/export'; link.id = 'export-accepted'; nav.append(link);
  }
  host.append(nav);
  const content = el('section'); content.id = 'authoring-section'; content.append(el('h3', section));
  if (section === 'Review') {
    content.append(el('p', 'Saved draft inspection. Definition verification is required before finishing.'));
    if (!definition.lessons?.length) content.append(el('p', 'No lessons configured; planning will produce an empty timetable'));
    for (const issue of issues) content.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId || ''} • ${issue.field}: ${issue.message}`));
    for (const adjustment of draft.openingAdjustments || []) content.append(el('p', `Catalog adjustment: ${adjustment}`));
    for (const pin of draft.expiredPins || []) content.append(el('p', `Expired attempt-scoped pin: ${pin.lessonId} ${pin.field} = ${JSON.stringify(pin.sourceValue)}. Exact source provenance is retained.`));
    if (!issues.length) content.append(el('p', 'No opening issues. This does not establish timetable feasibility.'));
  } else {
    for (const [label, key] of sections[section]) {
      const block = el('details'); block.open = true; block.append(el('summary', label));
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
