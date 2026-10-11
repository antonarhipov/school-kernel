const parse = raw => /^-?\d+$/.test(raw) && Number.isSafeInteger(Number(raw)) ? Number(raw) : raw;
const scalarDefaults = [['maxDailyLessonSpread', 'Preferred daily lesson spread', 4, '1'], ['maxDailyGaps', 'Maximum daily gaps', 5, '0'], ['latestStartSlot', 'Latest start slot', 6, 'unrestricted'], ['preferredLatestStartSlot', 'Preferred latest start slot', 6, '3'], ['dailyLessonSpreadLimit', 'Hard daily lesson spread', 7, 'unrestricted'], ['curatorTeacherId', 'Curator teacher', 8, 'none'], ['homeRoomId', 'Home room', 8, 'none']];
let working;
let busy = false;
let error;
const el = (tag, text, id) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (id) node.id = id; return node; };
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
export const classesDirty = () => !!working?.dirty;
export function resetClasses() { working = null; busy = false; error = null; }
function suggestion(name, draft) {
  const base = (name.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9._-]/g, '-').replace(/-+/g, '-').replace(/^[^a-z0-9]+/, '').replace(/-+$/, '') || 'cohort').slice(0, 128);
  const source = draft.sourceDocument?.initialDefinition || draft.sourceDocument?.acceptedBaseline?.definition;
  const reserved = new Set([...(draft.definition.cohorts || []), ...(source?.cohorts || []), ...(draft.removedIdentities || []).filter(row => row.ownerType === 'Cohort')].map(row => row.id));
  let id = base;
  for (let n = 2; reserved.has(id); n++) { const end = `-${n}`; id = base.slice(0, 128 - end.length) + end; }
  return id;
}
function select(snapshot, id = '') {
  const draft = snapshot.workspace.definitionDraft;
  const item = id ? draft.definition.cohorts.find(row => row.id === id) : { id: suggestion('', draft), displayName: '', size: '' };
  working = { originalId: id, item: structuredClone(item), base: structuredClone(item), version: snapshot.version, dirty: false, override: false, conflicts: [] }; error = null;
}
export function discardClasses(snapshot) { select(snapshot, snapshot.workspace.definitionDraft.definition.cohorts.some(row => row.id === working?.originalId) ? working.originalId : ''); }
function mergeNewer(snapshot) {
  if (working.version === snapshot.version) return;
  if (!working.originalId) { working.version = snapshot.version; return; }
  const newest = snapshot.workspace.definitionDraft.definition.cohorts.find(row => row.id === working.originalId);
  if (!newest) { working.conflicts = [{ field: '$removed', latest: undefined, value: working.item }]; working.version = snapshot.version; error = 'This class was removed in the newer draft. Discard working edits to select another class.'; return; }
  const merged = structuredClone(newest), conflicts = [];
  for (const field of new Set([...Object.keys(working.base), ...Object.keys(working.item)])) {
    if (same(working.item[field], working.base[field])) continue;
    if (!same(newest[field], working.base[field]) && !same(newest[field], working.item[field])) conflicts.push({ field, latest: newest[field], value: working.item[field] });
    if (Object.hasOwn(working.item, field)) merged[field] = working.item[field]; else delete merged[field];
  }
  working.item = merged; working.base = structuredClone(newest); working.version = snapshot.version; working.conflicts = conflicts; error = null;
}
export async function saveCohort(snapshot, actions) {
  if (busy) return false;
  busy = true; error = null; actions.render(snapshot);
  try {
    if (working.conflicts.length) throw new Error('Resolve the newer saved conflicts before saving.');
    const result = await actions.entity('cohorts', { originalId: working.originalId, item: working.item }, snapshot.version);
    select(result, working.item.id); busy = false; actions.render(result); return result;
  } catch (failure) { busy = false; error = `No change was committed. ${failure.message} Working values remain Unsaved.`; working.dirty = true; actions.render(snapshot); return false; }
}
export function renderClasses(host, snapshot, narrow, actions, status, leave) {
 if (!narrow && snapshot.workspace.definitionDraft.definition.catalogVersion < 8) { const upgrade = el('button', 'Review catalog upgrade'); upgrade.type = 'button'; upgrade.addEventListener('click', actions.catalog); host.append(upgrade); }

  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  if (!working || !working.dirty) select(snapshot, working && !working.originalId ? '' : definition.cohorts.some(row => row.id === working?.originalId) ? working.originalId : definition.cohorts[0]?.id || '');
  else mergeNewer(snapshot);
  if (busy) status.textContent = 'Saving… Working class values are not yet durable.';
  else if (working.dirty) status.textContent = narrow ? 'Unsaved working edits retained; read-only view shows saved classes.' : 'Unsaved — Working class differs from the saved draft.';
  if (narrow) {
    for (const item of definition.cohorts) {
      const block = el('section'); block.append(el('h4', item.displayName || item.id));
      for (const [field, value] of Object.entries(item)) block.append(el('p', `${field}: ${JSON.stringify(value)}`));
      if (!Object.hasOwn(item, 'availablePeriodIds')) block.append(el('p', 'Availability: All declared periods (omitted).'));
      if (!Object.hasOwn(item, 'undesirablePeriodIds')) block.append(el('p', 'Undesirable periods: None (omitted).'));
      for (const [field, label, since, value] of scalarDefaults) if (!Object.hasOwn(item, field)) block.append(el('p', `${label}: ${definition.catalogVersion < since ? `Unavailable until catalog ${since}` : `Use catalog default (omitted: ${value})`}.`));
      host.append(block);
    }
    if (!definition.cohorts.length) host.append(el('p', 'No saved classes.')); return;
  }
  const list = el('div'); list.className = 'actions';
  for (const item of definition.cohorts) {
    const button = el('button', `Edit class: ${item.displayName || item.id}`); button.type = 'button'; button.disabled = busy; button.dataset.classId = item.id;
    button.addEventListener('click', () => leave(latest => { select(latest, item.id); actions.render(latest); })); list.append(button);
  }
  const add = el('button', 'Add class', 'add-class'); add.type = 'button'; add.disabled = busy;
  add.addEventListener('click', () => leave(latest => { select(latest); actions.render(latest); })); list.append(add); host.append(list);
  const form = el('form', undefined, 'class-form'); form.className = 'definition-entity-form'; form.noValidate = true;
  const changed = () => { working.dirty = true; error = null; status.textContent = 'Unsaved — Working class differs from the saved draft.'; document.querySelector('#class-save-error')?.remove(); };
  for (const [field, label] of [['displayName', 'Class name'], ['id', working.originalId ? 'Class identifier (read-only)' : 'Class identifier suggestion / advanced override']]) {
    const wrapper = el('label', label), input = el('input', undefined, `class-${field}`); input.value = working.item[field] ?? ''; input.readOnly = field === 'id' && !!working.originalId; input.disabled = busy;
    input.addEventListener('input', () => { working.item[field] = input.value; if (field === 'id') working.override = true;
      if (field === 'displayName' && !working.originalId && !working.override) { working.item.id = suggestion(input.value, draft); form.querySelector('#class-id').value = working.item.id; } changed(); });
    wrapper.append(input); form.append(wrapper);
  }
  form.append(el('p', 'Saved identifiers are permanent. Curator and home-room changes never rewrite lessons or policies. A home room is hard for curator lessons and a preference fallback for other eligible lessons. Matching Use home room policies require a declared home room.'));
  const numeric = (field, label, since, minimum, defaultText, noLimit = false) => {
    const wrapper = el('label', label), available = definition.catalogVersion >= since;
    const input = el('input', undefined, `class-${field}`); input.type = 'text'; input.inputMode = 'numeric'; input.setAttribute('aria-label', `${label} value`); input.value = working.item[field] ?? '';
    let mode;
    if (field !== 'size') {
      mode = el('select', undefined, `class-${field}-mode`); mode.setAttribute('aria-label', `${label} mode`);
      for (const [value, text] of [['default', `Use catalog default (omitted: ${defaultText})`], ['value', 'Explicit value'], ...(noLimit ? [['unlimited', 'No limit (explicit 2147483647)']] : [])]) { const option = el('option', text); option.value = value; mode.append(option); }
      mode.value = !Object.hasOwn(working.item, field) ? 'default' : noLimit && working.item[field] === 2147483647 ? 'unlimited' : 'value'; mode.disabled = busy || !available;
      mode.addEventListener('change', () => { if (mode.value === 'default') delete working.item[field]; else if (mode.value === 'unlimited') { working.item[field] = 2147483647; input.value = '2147483647'; } else working.item[field] = parse(input.value); input.disabled = mode.value !== 'value'; changed(); }); wrapper.append(mode);
    }
    input.disabled = busy || !available || mode && mode.value !== 'value';
    input.addEventListener('input', () => { working.item[field] = parse(input.value); changed(); }); wrapper.append(input);
    wrapper.append(el('span', field === 'size' ? 'Required student count for room capacity.' : `Integer ${minimum}–2147483647. Default: ${defaultText}.`));
    if (!available) wrapper.append(el('span', `Requires catalog ${since}. Enable a newer scheduling catalog in School (UC-12).`));
    form.append(wrapper);
  };
  numeric('size', 'Class size', 1, 1, 'required');
  numeric('maxDailyLessonSpread', 'Preferred daily lesson spread', 4, 0, '1');
  numeric('maxDailyGaps', 'Maximum daily gaps', 5, 0, definition.catalogVersion >= 5 ? '0' : 'unrestricted', true);
  numeric('latestStartSlot', 'Latest start slot', 6, 1, 'unrestricted', true);
  numeric('preferredLatestStartSlot', 'Preferred latest start slot', 6, 1, '3', true);
  numeric('dailyLessonSpreadLimit', 'Hard daily lesson spread', 7, 0, 'unrestricted', true);
  for (const [field, label, rows] of [['curatorTeacherId', 'Curator teacher', definition.teachers], ['homeRoomId', 'Home room', definition.rooms]]) {
    const wrapper = el('label', label), mode = el('select', undefined, `class-${field}-mode`); mode.setAttribute('aria-label', `${label} mode`);
    for (const [value, text] of [['default', `No ${label.toLowerCase()} (omitted)`], ['value', 'Explicit identifier']]) { const option = el('option', text); option.value = value; mode.append(option); }
    mode.value = Object.hasOwn(working.item, field) ? 'value' : 'default'; mode.disabled = busy || definition.catalogVersion < 8;
    const input = el('input', undefined, `class-${field}`); input.value = working.item[field] ?? ''; input.setAttribute('aria-label', `${label} identifier`); input.disabled = busy || definition.catalogVersion < 8 || mode.value === 'default';
    const choices = el('datalist', undefined, `class-${field}-choices`); input.setAttribute('list', choices.id);
    for (const row of rows) { const option = el('option', row.displayName); option.value = row.id; choices.append(option); }
    mode.addEventListener('change', () => { if (mode.value === 'default') delete working.item[field]; else working.item[field] = input.value; input.disabled = mode.value === 'default'; changed(); });
    input.addEventListener('input', () => { working.item[field] = input.value; changed(); }); wrapper.append(mode, input, choices, el('span', `Available links: ${rows.map(row => `${row.displayName} (${row.id})`).join(', ') || '(none configured)'}. Unresolved work can be saved with blockers.`));
    if (definition.catalogVersion < 8) wrapper.append(el('span', 'Requires catalog 8. Enable a newer scheduling catalog in School (UC-12).'));
    form.append(wrapper);
  }
  for (const [field, label, rows, optional, defaultText] of [
    ['availablePeriodIds', 'Class availability', definition.periods, true, 'All declared periods (omitted)'],
    ['undesirablePeriodIds', 'Undesirable periods', definition.periods, true, 'No undesirable periods (omitted)']
  ]) {
    const block = el('fieldset'); block.append(el('legend', label));
    let explicit = Object.hasOwn(working.item, field);
    if (optional) {
      const wrapper = el('label', `${label} mode`), mode = el('select', undefined, `class-${field}-mode`);
      for (const [value, text] of [['default', defaultText], ['explicit', 'Explicit selection (empty means none)']]) { const option = el('option', text); option.value = value; mode.append(option); }
      mode.value = explicit ? 'explicit' : 'default'; mode.disabled = busy;
      mode.addEventListener('change', () => { if (mode.value === 'default') delete working.item[field]; else working.item[field] = []; changed(); actions.render(snapshot); }); wrapper.append(mode); block.append(wrapper);
    } else explicit = true;
    const values = Array.isArray(working.item[field]) ? working.item[field] : [];
    if (Object.hasOwn(working.item, field) && !Array.isArray(working.item[field])) block.append(el('p', `Saved invalid selection: ${JSON.stringify(working.item[field])}. Retained until you explicitly choose a corrected selection.`));
    if (!rows.length) block.append(el('p',  'No declared periods. Configure the weekly grid before choosing periods.'));
    for (const row of rows) {
      const wrapper = el('label'), input = el('input'); input.type = 'checkbox'; input.dataset.field = field; input.dataset.referenceId = row.id;
      input.checked = values.includes(row.id); input.disabled = busy || !explicit;
      const title = `${row.displayName || row.id} (${row.id}) • ${row.weekday} ${row.order}${definition.reservedPeriodIds?.includes(row.id) ? ' • Reserved' : ''}`;
      input.setAttribute('aria-label', `${label}: ${title}`); wrapper.append(input, document.createTextNode(title));
      input.addEventListener('change', () => { const old = Array.isArray(working.item[field]) ? working.item[field] : []; working.item[field] = old.filter(value => value !== row.id); if (input.checked) working.item[field].push(row.id); changed(); }); block.append(wrapper);
    }
    const known = new Set(rows.map(row => row.id)), unresolved = values.filter(value => !known.has(value));
    const wrapper = el('label', `Additional or unresolved ${label.toLowerCase()} identifiers (comma-separated)`), extra = el('input', undefined, `class-${field}-extra`);
    extra.value = unresolved.map(value => typeof value === 'string' ? value : JSON.stringify(value)).join(', '); extra.disabled = busy || !explicit;
    extra.addEventListener('input', () => { const selected = Array.isArray(working.item[field]) ? working.item[field].filter(value => known.has(value)) : []; working.item[field] = [...selected, ...extra.value.split(',').map(value => value.trim()).filter(Boolean)]; changed(); }); wrapper.append(extra); block.append(wrapper); form.append(block);
  }
  if (working.conflicts.length) {
    const block = el('section', undefined, 'class-conflicts'); block.className = 'notice'; block.append(el('h4', 'Newer saved fields conflict with working edits'), el('p', 'Untouched fields use the newer saved values. Choose explicitly before retrying.'));
    for (const row of working.conflicts) block.append(el('p', `${row.field}: latest ${JSON.stringify(row.latest) ?? '(removed/omitted)'}; working ${JSON.stringify(row.value) ?? '(omitted)'}`));
    for (const [id, label, useLatest] of [['class-keep-conflicts', 'Keep my reviewed conflicting values', false], ['class-use-latest', 'Use newer saved conflicting values', true]]) {
      const button = el('button', label, id); button.type = 'button'; button.disabled = busy || working.conflicts.some(row => row.field === '$removed');
      button.addEventListener('click', () => { if (useLatest) for (const row of working.conflicts) { if (row.latest === undefined) delete working.item[row.field]; else working.item[row.field] = row.latest; } working.conflicts = []; changed(); actions.render(snapshot); }); block.append(button);
    }
    form.append(block);
  }
  const controls = el('div'); controls.className = 'actions';
  const save = el('button', 'Save class', 'save-class'); save.type = 'submit'; save.disabled = busy || !!working.conflicts.length;
  const cancel = el('button', 'Discard working class edits', 'cancel-class'); cancel.type = 'button'; cancel.disabled = busy; cancel.addEventListener('click', () => { discardClasses(snapshot); actions.render(snapshot); }); controls.append(save, cancel); form.append(controls);
  form.addEventListener('submit', event => { event.preventDefault(); saveCohort(snapshot, actions); });
  if (error) { const alert = el('p', error, 'class-save-error'); alert.className = 'error'; alert.setAttribute('role', 'alert'); form.append(alert);
    const refresh = el('button', 'Review newer saved draft for retry', 'class-refresh'); refresh.type = 'button'; refresh.addEventListener('click', actions.refresh); form.append(refresh); }
  host.append(form);
  if (working.originalId) {
    const id = working.originalId, effects = el('section', undefined, 'class-effects'); effects.append(el('h4', 'Saved class effects'));
    const lessons = definition.lessons.filter(row => row.cohortId === id);
    for (const lesson of lessons) effects.append(el('p', `Lesson ${lesson.id}: subject ${lesson.subjectId}; teacher ${lesson.teacherId}; period lock ${lesson.periodLock ?? '(none)'}; room lock ${lesson.roomLock ?? '(none)'}. Assignment remains saved.`));
    for (const policy of definition.roomAssignments || []) if (lessons.some(lesson => lesson.subjectId === policy.subjectId && (!Object.hasOwn(policy, 'teacherId') || lesson.teacherId === policy.teacherId))) effects.append(el('p', `Matched room policy ${policy.id}: ${JSON.stringify(policy)}`));
    for (const issue of draft.issues || []) if ((issue.ownerType === 'Cohort' && issue.ownerId === id) || issue.relatedIds?.includes(id)) effects.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
    effects.append(el('p', 'No home room is invented or required merely by opening a class. Ordinary home-room fallback is a preference; curator/home-room policies are hard obligations. Remove through guided dependency resolution (UC-13), restore source values through source restoration (UC-14).')); host.append(effects);
  }

}
window.addEventListener('beforeunload', event => { if (classesDirty()) { event.preventDefault(); event.returnValue = ''; } });
