let working;
let busy = false;
let error;
const fields = [
  ['reservedPeriodsAllowed', 'Reserved periods allowed', 6, 'boolean', 'Default false. Explicit permission to use school-reserved periods.'],
  ['maxWeeklyReservedLessonsPerCohort', 'Maximum reserved lessons per class per week', 6, 'number', 'Default unlimited. A value requires explicit reserved-period permission.'],
  ['dayEdgeOnly', 'Day edges only', 6, 'boolean', 'Default false. Hard rule restricting placement to supported class-day edges.'],
  ['maxDailyLessonsPerCohort', 'Maximum lessons per class per day', 6, 'number', 'Default unlimited. Hard daily subject count per class.'],
  ['curatorLesson', 'Curator subject', 8, 'boolean', 'Default false. Lessons use the class curator and its home-room obligations; ordinary qualification is exempt.']
];
const el = (tag, text, id) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (id) node.id = id; return node; };
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
export const subjectsDirty = () => !!working?.dirty;
export function resetSubjects() { working = null; busy = false; error = null; }
const sourceDefinition = draft => draft.sourceDocument?.initialDefinition || draft.sourceDocument?.acceptedBaseline?.definition || {};
function suggestion(name, draft) {
  const base = (name.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9._-]/g, '-').replace(/-+/g, '-').replace(/^[^a-z0-9]+/, '').replace(/-+$/, '') || 'subject').slice(0, 128);
  const reserved = new Set([...(draft.definition.subjects || []), ...(sourceDefinition(draft).subjects || []), ...(draft.removedIdentities || []).filter(row => row.ownerType === 'Subject')].map(row => row.id));
  let id = base;
  for (let n = 2; reserved.has(id); n++) { const end = `-${n}`; id = base.slice(0, 128 - end.length) + end; }
  return id;
}
function select(draft, version, id = '') {
  const item = id ? draft.definition.subjects.find(row => row.id === id) : { id: suggestion('', draft), displayName: '' };
  working = { originalId: id, item: structuredClone(item), base: structuredClone(item), version, dirty: false, override: false, conflicts: [] };
  error = null;
}
export function discardSubjects(snapshot) {
  const draft = snapshot.workspace.definitionDraft;
  select(draft, snapshot.version, draft.definition.subjects.some(row => row.id === working?.originalId) ? working.originalId : '');
}
function mergeNewer(snapshot) {
  if (!working || working.version === snapshot.version) return;
  const newest = snapshot.workspace.definitionDraft.definition.subjects.find(row => row.id === working.originalId);
  if (!working.originalId) { working.version = snapshot.version; return; }
  if (!newest) { working.conflicts = [{ field: '$removed', latest: undefined, value: working.item }]; working.version = snapshot.version; error = 'This subject was removed in the newer draft. Discard working edits to select another subject.'; return; }
  const merged = structuredClone(newest), conflicts = [];
  for (const field of new Set([...Object.keys(working.base), ...Object.keys(working.item)])) {
    if (same(working.item[field], working.base[field])) continue;
    if (!same(newest[field], working.base[field]) && !same(newest[field], working.item[field])) conflicts.push({ field, latest: newest[field], value: working.item[field] });
    if (Object.hasOwn(working.item, field)) merged[field] = working.item[field]; else delete merged[field];
  }
  working.item = merged; working.base = structuredClone(newest); working.version = snapshot.version; working.conflicts = conflicts; error = null;
}
export async function saveSubject(snapshot, actions) {
  if (busy) return false;
  busy = true; error = null; actions.render(snapshot);
  try {
    if (working.conflicts.length) throw new Error('Resolve the newer saved conflicts before saving.');
    const result = await actions.entity('subjects', { originalId: working.originalId, item: working.item }, snapshot.version);
    select(result.workspace.definitionDraft, result.version, working.item.id); busy = false; actions.render(result); return result;
  } catch (failure) {
    busy = false; error = `No change was committed. ${failure.message} Working values remain Unsaved.`; working.dirty = true; actions.render(snapshot); return false;
  }
}
export function renderSubjects(host, snapshot, narrow, actions, status, leave) {
 if (!narrow && snapshot.workspace.definitionDraft.definition.catalogVersion < 8) { const upgrade = el('button', 'Review catalog upgrade'); upgrade.type = 'button'; upgrade.addEventListener('click', actions.catalog); host.append(upgrade); }

  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  if (!working || !working.dirty) select(draft, snapshot.version, working && !working.originalId ? '' : definition.subjects.some(row => row.id === working?.originalId) ? working.originalId : definition.subjects[0]?.id || '');
  else mergeNewer(snapshot);
  if (busy) status.textContent = 'Saving… Working subject values are not yet durable.';
  else if (working.dirty) status.textContent = narrow ? 'Unsaved working edits retained; read-only view shows saved subjects.' : 'Unsaved — Working subject differs from the saved draft.';
  if (narrow) {
    for (const item of definition.subjects) {
      const block = el('section'); block.append(el('h4', item.displayName || item.id));
      for (const [field, value] of Object.entries(item)) block.append(el('p', `${field}: ${JSON.stringify(value)}`));
      for (const [field, label, since, , help] of fields) if (!Object.hasOwn(item, field)) block.append(el('p', `${label}: ${definition.catalogVersion < since ? `Unavailable until catalog ${since}` : 'Use catalog default (omitted)'}. ${help}`));
      host.append(block);
    }
    if (!definition.subjects.length) host.append(el('p', 'No saved subjects.'));
    return;
  }
  const list = el('div'); list.className = 'actions';
  for (const item of definition.subjects) {
    const button = el('button', `Edit subject: ${item.displayName || item.id}`); button.type = 'button'; button.dataset.subjectId = item.id; button.disabled = busy;
    button.addEventListener('click', () => leave(latest => { select(latest.workspace.definitionDraft, latest.version, item.id); actions.render(latest); })); list.append(button);
  }
  const add = el('button', 'Add subject', 'add-subject'); add.type = 'button'; add.disabled = busy;
  add.addEventListener('click', () => leave(latest => { select(latest.workspace.definitionDraft, latest.version); actions.render(latest); })); list.append(add); host.append(list);
  const form = el('form', undefined, 'subject-form'); form.className = 'definition-entity-form'; form.noValidate = true;
  const changed = () => { working.dirty = true; error = null; status.textContent = 'Unsaved — Working subject differs from the saved draft.'; document.querySelector('#subject-save-error')?.remove(); };
  const textField = (field, label, readonly = false) => {
    const wrapper = el('label', label), input = el('input', undefined, `subject-${field}`); input.value = working.item[field] ?? ''; input.readOnly = readonly; input.disabled = busy;
    input.addEventListener('input', () => { working.item[field] = input.value; if (field === 'id') working.override = true;
      if (field === 'displayName' && !working.originalId && !working.override) { working.item.id = suggestion(input.value, draft); form.querySelector('#subject-id').value = working.item.id; } changed(); });
    wrapper.append(input); form.append(wrapper);
  };
  textField('displayName', 'Subject name'); textField('id', working.originalId ? 'Subject identifier (read-only)' : 'Subject identifier suggestion / advanced override', !!working.originalId);
  form.append(el('p', working.originalId ? 'The saved identifier is permanent; renaming changes only the display name.' : 'Review or override the suggestion. Identity becomes permanent on first save.'));
  for (const [field, label, since, type, help] of fields) {
    const wrapper = el('label', label), control = el('select', undefined, `subject-${field}-mode`);
    const choices = type === 'boolean' ? [['default', 'Use catalog default (omitted: false)'], ['true', 'Explicit true'], ['false', 'Explicit false']] : [['default', 'Use catalog default (omitted: unlimited)'], ['value', 'Explicit value']];
    for (const [value, name] of choices) { const option = el('option', name); option.value = value; control.append(option); }
    const available = definition.catalogVersion >= since;
    control.value = !Object.hasOwn(working.item, field) ? 'default' : type === 'boolean' ? String(working.item[field]) : 'value';
    if (type === 'boolean' && Object.hasOwn(working.item, field) && typeof working.item[field] !== 'boolean') { const option = el('option', `Saved invalid value: ${JSON.stringify(working.item[field])}`); option.value = String(working.item[field]); control.append(option); control.value = option.value; }
    control.disabled = busy || !available; wrapper.append(control);
    const input = type === 'number' ? el('input', undefined, `subject-${field}`) : null;
    if (input) { input.setAttribute('aria-label', `${label} value`); input.type = 'text'; input.inputMode = 'numeric'; input.value = working.item[field] ?? ''; input.disabled = busy || !available || control.value === 'default';
      input.addEventListener('input', () => { const raw = input.value; working.item[field] = /^-?\d+$/.test(raw) && Number.isSafeInteger(Number(raw)) ? Number(raw) : raw; changed(); }); wrapper.append(input); }
    control.addEventListener('change', () => {
      if (control.value === 'default') delete working.item[field]; else working.item[field] = type === 'boolean' ? control.value === 'true' : input.value === '' ? '' : /^-?\d+$/.test(input.value) && Number.isSafeInteger(Number(input.value)) ? Number(input.value) : input.value;
      if (input) input.disabled = control.value === 'default'; changed();
    });
    wrapper.append(el('span', help));
    if (!available) wrapper.append(el('span', `Requires catalog ${since}. Enable a newer scheduling catalog in School (UC-12).`));
    form.append(wrapper);
  }
  if (working.conflicts.length) {
    const conflict = el('section', undefined, 'subject-conflicts'); conflict.className = 'notice'; conflict.append(el('h4', 'Newer saved fields conflict with working edits'), el('p', 'Untouched fields use the newer saved values. Choose explicitly before retrying.'));
    for (const item of working.conflicts) conflict.append(el('p', `${item.field}: latest ${JSON.stringify(item.latest) ?? '(removed/omitted)'}; working ${JSON.stringify(item.value) ?? '(omitted)'}`));
    for (const [id, label, useLatest] of [['subject-keep-conflicts', 'Keep my reviewed conflicting values', false], ['subject-use-latest', 'Use newer saved conflicting values', true]]) {
      const button = el('button', label, id); button.type = 'button'; button.disabled = busy || working.conflicts.some(row => row.field === '$removed');
      button.addEventListener('click', () => { if (useLatest) for (const row of working.conflicts) { if (row.latest === undefined) delete working.item[row.field]; else working.item[row.field] = row.latest; } working.conflicts = []; changed(); actions.render(snapshot); }); conflict.append(button);
    }
    form.append(conflict);
  }
  const controls = el('div'); controls.className = 'actions';
  const save = el('button', 'Save subject', 'save-subject'); save.type = 'submit'; save.disabled = busy || !!working.conflicts.length;
  const cancel = el('button', 'Discard working subject edits', 'cancel-subject'); cancel.type = 'button'; cancel.disabled = busy;
  cancel.addEventListener('click', () => { discardSubjects(snapshot); actions.render(snapshot); }); controls.append(save, cancel); form.append(controls);
  form.addEventListener('submit', event => { event.preventDefault(); saveSubject(snapshot, actions); });
  if (error) { const alert = el('p', error, 'subject-save-error'); alert.className = 'error'; alert.setAttribute('role', 'alert'); form.append(alert);
    const refresh = el('button', 'Review newer saved draft for retry', 'subject-refresh'); refresh.type = 'button'; refresh.addEventListener('click', actions.refresh); form.append(refresh); }
  host.append(form);
  const id = working.originalId;
  if (id) {
    const effects = el('section', undefined, 'subject-effects'); effects.append(el('h4', 'Saved subject effects'), el('p', 'Qualifications, lessons and room policies stay as saved. Curator subjects require the class curator and home room; ordinary subjects require teacher qualification.'));
    for (const teacher of definition.teachers.filter(row => row.qualifiedSubjectIds?.includes(id))) effects.append(el('p', `Qualified teacher: ${teacher.displayName} (${teacher.id})`));
    for (const lesson of definition.lessons.filter(row => row.subjectId === id)) effects.append(el('p', `Lesson ${lesson.id}: class ${lesson.cohortId}; teacher ${lesson.teacherId}; period lock ${lesson.periodLock ?? '(none)'}; room lock ${lesson.roomLock ?? '(none)'}`));
    for (const policy of (definition.roomAssignments || []).filter(row => row.subjectId === id)) effects.append(el('p', `Room policy ${policy.id}: ${JSON.stringify(policy)}`));
    for (const issue of draft.issues || []) if ((issue.ownerType === 'Subject' && issue.ownerId === id) || issue.relatedIds?.includes(id)) effects.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
    effects.append(el('p', 'To remove this subject, resolve its references through guided removal (UC-13). To restore source values, use source restoration (UC-14). These operations do not run through subject save.')); host.append(effects);
  }
}
window.addEventListener('beforeunload', event => { if (subjectsDirty()) { event.preventDefault(); event.returnValue = ''; } });
