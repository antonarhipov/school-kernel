let working;
let busy = false;
let error;
const el = (tag, text, id) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (id) node.id = id; return node; };
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
export const teachersDirty = () => !!working?.dirty;
export function resetTeachers() { working = null; busy = false; error = null; }
function suggestion(name, draft) {
  const base = (name.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9._-]/g, '-').replace(/-+/g, '-').replace(/^[^a-z0-9]+/, '').replace(/-+$/, '') || 'teacher').slice(0, 128);
  const source = draft.sourceDocument?.initialDefinition || draft.sourceDocument?.acceptedBaseline?.definition;
  const reserved = new Set([...(draft.definition.teachers || []), ...(source?.teachers || []), ...(draft.removedIdentities || []).filter(row => row.ownerType === 'Teacher')].map(row => row.id));
  let id = base;
  for (let n = 2; reserved.has(id); n++) { const end = `-${n}`; id = base.slice(0, 128 - end.length) + end; }
  return id;
}
function select(snapshot, id = '') {
  const draft = snapshot.workspace.definitionDraft;
  const item = id ? draft.definition.teachers.find(row => row.id === id) : { id: suggestion('', draft), displayName: '', qualifiedSubjectIds: [] };
  working = { originalId: id, item: structuredClone(item), base: structuredClone(item), version: snapshot.version, dirty: false, override: false, conflicts: [] }; error = null;
}
export function discardTeachers(snapshot) { select(snapshot, snapshot.workspace.definitionDraft.definition.teachers.some(row => row.id === working?.originalId) ? working.originalId : ''); }
function mergeNewer(snapshot) {
  if (working.version === snapshot.version) return;
  if (!working.originalId) { working.version = snapshot.version; return; }
  const newest = snapshot.workspace.definitionDraft.definition.teachers.find(row => row.id === working.originalId);
  if (!newest) { working.conflicts = [{ field: '$removed', latest: undefined, value: working.item }]; working.version = snapshot.version; error = 'This teacher was removed in the newer draft. Discard working edits to select another teacher.'; return; }
  const merged = structuredClone(newest), conflicts = [];
  for (const field of new Set([...Object.keys(working.base), ...Object.keys(working.item)])) {
    if (same(working.item[field], working.base[field])) continue;
    if (!same(newest[field], working.base[field]) && !same(newest[field], working.item[field])) conflicts.push({ field, latest: newest[field], value: working.item[field] });
    if (Object.hasOwn(working.item, field)) merged[field] = working.item[field]; else delete merged[field];
  }
  working.item = merged; working.base = structuredClone(newest); working.version = snapshot.version; working.conflicts = conflicts; error = null;
}
export async function saveTeacher(snapshot, actions) {
  if (busy) return false;
  busy = true; error = null; actions.render(snapshot);
  try {
    if (working.conflicts.length) throw new Error('Resolve the newer saved conflicts before saving.');
    const result = await actions.entity('teachers', { originalId: working.originalId, item: working.item }, snapshot.version);
    select(result, working.item.id); busy = false; actions.render(result); return result;
  } catch (failure) { busy = false; error = `No change was committed. ${failure.message} Working values remain Unsaved.`; working.dirty = true; actions.render(snapshot); return false; }
}
export function renderTeachers(host, snapshot, narrow, actions, status, leave) {
  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  if (!working || !working.dirty) select(snapshot, working && !working.originalId ? '' : definition.teachers.some(row => row.id === working?.originalId) ? working.originalId : definition.teachers[0]?.id || '');
  else mergeNewer(snapshot);
  if (busy) status.textContent = 'Saving… Working teacher values are not yet durable.';
  else if (working.dirty) status.textContent = narrow ? 'Unsaved working edits retained; read-only view shows saved teachers.' : 'Unsaved — Working teacher differs from the saved draft.';
  if (narrow) {
    for (const item of definition.teachers) {
      const block = el('section'); block.append(el('h4', item.displayName || item.id));
      for (const [field, value] of Object.entries(item)) block.append(el('p', `${field}: ${JSON.stringify(value)}`));
      if (!Object.hasOwn(item, 'availablePeriodIds')) block.append(el('p', 'Availability: All declared periods (omitted).'));
      if (!Object.hasOwn(item, 'undesirablePeriodIds')) block.append(el('p', 'Undesirable periods: None (omitted).'));
      host.append(block);
    }
    if (!definition.teachers.length) host.append(el('p', 'No saved teachers.')); return;
  }
  const list = el('div'); list.className = 'actions';
  for (const item of definition.teachers) {
    const button = el('button', `Edit teacher: ${item.displayName || item.id}`); button.type = 'button'; button.disabled = busy; button.dataset.teacherId = item.id;
    button.addEventListener('click', () => leave(latest => { select(latest, item.id); actions.render(latest); })); list.append(button);
  }
  const add = el('button', 'Add teacher', 'add-teacher'); add.type = 'button'; add.disabled = busy;
  add.addEventListener('click', () => leave(latest => { select(latest); actions.render(latest); })); list.append(add); host.append(list);
  const form = el('form', undefined, 'teacher-form'); form.className = 'definition-entity-form'; form.noValidate = true;
  const changed = () => { working.dirty = true; error = null; status.textContent = 'Unsaved — Working teacher differs from the saved draft.'; document.querySelector('#teacher-save-error')?.remove(); };
  for (const [field, label] of [['displayName', 'Teacher name'], ['id', working.originalId ? 'Teacher identifier (read-only)' : 'Teacher identifier suggestion / advanced override']]) {
    const wrapper = el('label', label), input = el('input', undefined, `teacher-${field}`); input.value = working.item[field] ?? ''; input.readOnly = field === 'id' && !!working.originalId; input.disabled = busy;
    input.addEventListener('input', () => { working.item[field] = input.value; if (field === 'id') working.override = true;
      if (field === 'displayName' && !working.originalId && !working.override) { working.item.id = suggestion(input.value, draft); form.querySelector('#teacher-id').value = working.item.id; } changed(); });
    wrapper.append(input); form.append(wrapper);
  }
  form.append(el('p', 'Saved identifiers are permanent. Qualifications are explicit; empty qualifications allow no ordinary subjects. Curator lessons use only the configured class curator exception. Availability and undesirable periods are independent of reservation membership.'));
  for (const [field, label, rows, optional, defaultText] of [
    ['qualifiedSubjectIds', 'Qualified subjects', definition.subjects, false, ''],
    ['availablePeriodIds', 'Teacher availability', definition.periods, true, 'All declared periods (omitted)'],
    ['undesirablePeriodIds', 'Undesirable periods', definition.periods, true, 'No undesirable periods (omitted)']
  ]) {
    const block = el('fieldset'); block.append(el('legend', label));
    let explicit = Object.hasOwn(working.item, field);
    if (optional) {
      const wrapper = el('label', `${label} mode`), mode = el('select', undefined, `teacher-${field}-mode`);
      for (const [value, text] of [['default', defaultText], ['explicit', 'Explicit selection (empty means none)']]) { const option = el('option', text); option.value = value; mode.append(option); }
      mode.value = explicit ? 'explicit' : 'default'; mode.disabled = busy;
      mode.addEventListener('change', () => { if (mode.value === 'default') delete working.item[field]; else working.item[field] = []; changed(); actions.render(snapshot); }); wrapper.append(mode); block.append(wrapper);
    } else explicit = true;
    const values = Array.isArray(working.item[field]) ? working.item[field] : [];
    if (Object.hasOwn(working.item, field) && !Array.isArray(working.item[field])) block.append(el('p', `Saved invalid selection: ${JSON.stringify(working.item[field])}. Retained until you explicitly choose a corrected selection.`));
    if (!rows.length) block.append(el('p', field === 'qualifiedSubjectIds' ? 'No subjects configured. Add subjects before choosing qualifications.' : 'No declared periods. Configure the weekly grid before choosing periods.'));
    for (const row of rows) {
      const wrapper = el('label'), input = el('input'); input.type = 'checkbox'; input.dataset.field = field; input.dataset.referenceId = row.id;
      input.checked = values.includes(row.id); input.disabled = busy || !explicit;
      const title = field === 'qualifiedSubjectIds' ? `${row.displayName || row.id} (${row.id})` : `${row.displayName || row.id} (${row.id}) • ${row.weekday} ${row.order}${definition.reservedPeriodIds?.includes(row.id) ? ' • Reserved' : ''}`;
      input.setAttribute('aria-label', `${label}: ${title}`); wrapper.append(input, document.createTextNode(title));
      input.addEventListener('change', () => { const old = Array.isArray(working.item[field]) ? working.item[field] : []; working.item[field] = old.filter(value => value !== row.id); if (input.checked) working.item[field].push(row.id); changed(); }); block.append(wrapper);
    }
    const known = new Set(rows.map(row => row.id)), unresolved = values.filter(value => !known.has(value));
    const wrapper = el('label', `Additional or unresolved ${label.toLowerCase()} identifiers (comma-separated)`), extra = el('input', undefined, `teacher-${field}-extra`);
    extra.value = unresolved.map(value => typeof value === 'string' ? value : JSON.stringify(value)).join(', '); extra.disabled = busy || !explicit;
    extra.addEventListener('input', () => { const selected = Array.isArray(working.item[field]) ? working.item[field].filter(value => known.has(value)) : []; working.item[field] = [...selected, ...extra.value.split(',').map(value => value.trim()).filter(Boolean)]; changed(); }); wrapper.append(extra); block.append(wrapper); form.append(block);
  }
  if (working.conflicts.length) {
    const block = el('section', undefined, 'teacher-conflicts'); block.className = 'notice'; block.append(el('h4', 'Newer saved fields conflict with working edits'), el('p', 'Untouched fields use the newer saved values. Choose explicitly before retrying.'));
    for (const row of working.conflicts) block.append(el('p', `${row.field}: latest ${JSON.stringify(row.latest) ?? '(removed/omitted)'}; working ${JSON.stringify(row.value) ?? '(omitted)'}`));
    for (const [id, label, useLatest] of [['teacher-keep-conflicts', 'Keep my reviewed conflicting values', false], ['teacher-use-latest', 'Use newer saved conflicting values', true]]) {
      const button = el('button', label, id); button.type = 'button'; button.disabled = busy || working.conflicts.some(row => row.field === '$removed');
      button.addEventListener('click', () => { if (useLatest) for (const row of working.conflicts) { if (row.latest === undefined) delete working.item[row.field]; else working.item[row.field] = row.latest; } working.conflicts = []; changed(); actions.render(snapshot); }); block.append(button);
    }
    form.append(block);
  }
  const controls = el('div'); controls.className = 'actions';
  const save = el('button', 'Save teacher', 'save-teacher'); save.type = 'submit'; save.disabled = busy || !!working.conflicts.length;
  const cancel = el('button', 'Discard working teacher edits', 'cancel-teacher'); cancel.type = 'button'; cancel.disabled = busy; cancel.addEventListener('click', () => { discardTeachers(snapshot); actions.render(snapshot); }); controls.append(save, cancel); form.append(controls);
  form.addEventListener('submit', event => { event.preventDefault(); saveTeacher(snapshot, actions); });
  if (error) { const alert = el('p', error, 'teacher-save-error'); alert.className = 'error'; alert.setAttribute('role', 'alert'); form.append(alert);
    const refresh = el('button', 'Review newer saved draft for retry', 'teacher-refresh'); refresh.type = 'button'; refresh.addEventListener('click', actions.refresh); form.append(refresh); }
  host.append(form);
  if (working.originalId) {
    const id = working.originalId, effects = el('section', undefined, 'teacher-effects'); effects.append(el('h4', 'Saved teacher effects'));
    for (const lesson of definition.lessons.filter(row => row.teacherId === id)) effects.append(el('p', `Lesson ${lesson.id}: subject ${lesson.subjectId}; class ${lesson.cohortId}; period lock ${lesson.periodLock ?? '(none)'}. Teaching assignment remains saved.`));
    for (const cohort of definition.cohorts.filter(row => row.curatorTeacherId === id)) effects.append(el('p', `Class curator: ${cohort.displayName} (${cohort.id})`));
    for (const policy of (definition.roomAssignments || []).filter(row => row.teacherId === id)) effects.append(el('p', `Room policy ${policy.id}: ${JSON.stringify(policy)}`));
    for (const issue of draft.issues || []) if ((issue.ownerType === 'Teacher' && issue.ownerId === id) || issue.relatedIds?.includes(id)) effects.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
    effects.append(el('p', 'Reassign teaching through occurrence editing (UC-9). Remove this teacher through guided dependency resolution (UC-13); restore source values through source restoration (UC-14). No dependent record changes through teacher save.')); host.append(effects);
  }
}
window.addEventListener('beforeunload', event => { if (teachersDirty()) { event.preventDefault(); event.returnValue = ''; } });
