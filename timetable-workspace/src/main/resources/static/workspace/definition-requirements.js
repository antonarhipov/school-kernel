const fields = [['teacherId', 'Common teacher', 'teachers'], ['requiredRoomCapabilityIds', 'Required room capabilities', 'tokens'], ['preferredRoomIds', 'Preferred rooms', 'rooms'], ['undesirablePeriodIds', 'Undesirable periods', 'periods'], ['periodLock', 'Persistent period lock', 'periods'], ['roomLock', 'Persistent room lock', 'rooms']];
const arrayFields = new Set(['requiredRoomCapabilityIds', 'preferredRoomIds', 'undesirablePeriodIds']);
const el = (tag, text, id) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (id) node.id = id; return node; };
const parse = raw => /^-?\d+$/.test(raw) && Number.isSafeInteger(Number(raw)) ? Number(raw) : raw;
let working, model, loading, busy, error, resolveReview, loadFailed;
export const requirementsBusy = () => !!busy;
export const requirementsDirty = () => !!working?.dirty;
export function resetRequirements() { working = model = loading = error = null; busy = false; loadFailed = false; resolveReview?.(false); resolveReview = null; }
function suggestion(definition, cohort, subject, draft) {
  const name = (kind, id) => definition[kind].find(row => row.id === id)?.displayName || id;
  const base = (`${name('cohorts', cohort)} ${name('subjects', subject)}`.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9._-]/g, '-').replace(/-+/g, '-').replace(/^[^a-z0-9]+/, '').replace(/-+$/, '') || 'series').slice(0, 128);
  const source = draft.sourceDocument?.initialDefinition || draft.sourceDocument?.acceptedBaseline?.definition;
  const reserved = new Set([...(model?.requirements || []).map(row => row.seriesId), ...(source?.lessons || []).map(row => row.seriesId), ...(draft.removedIdentities || []).filter(row => row.ownerType === 'Series').map(row => row.id)]);
  let id = base; for (let n = 2; reserved.has(id); n++) { const end = `-${n}`; id = base.slice(0, 128 - end.length) + end; } return id;
}
function select(snapshot, id = '') {
  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  const row = id ? model.requirements.find(row => row.seriesId === id) : { seriesId: suggestion(definition, '', '', draft), cohortId: '', subjectId: '', count: 0, defaults: {}, exceptions: {}, mixedFields: [], occurrences: [], sourcePlacements: [] };
  working = { originalId: id, row: structuredClone(row), request: { originalSeriesId: id, seriesId: row.seriesId, cohortId: row.cohortId, subjectId: row.subjectId, count: row.count, changes: {}, followers: {}, applyToAllFields: [], removeIds: [] }, dirty: false, override: false, preview: null, version: snapshot.version, controls: {} }; error = null;
}
export function discardRequirements(snapshot) { select(snapshot, working?.originalId && model.requirements.some(row => row.seriesId === working.originalId) ? working.originalId : ''); }
export async function saveRequirement(snapshot, actions) {
  if (busy) return false; busy = true; error = null; working.preview = null; actions.render(snapshot);
  try { const preview = await actions.requirement(working.request, snapshot.version, true); busy = false; working.preview = preview; actions.render(snapshot); return await new Promise(resolve => { resolveReview = resolve; }); }
  catch (failure) { busy = false; error = `No change was committed. ${failure.message} Working choices remain Unsaved.`; working.dirty = true; actions.render(snapshot); return false; }
}
const describe = (lesson, row) => {
  const exceptions = new Set(row.exceptions?.[lesson.id] || []);
  return `${lesson.displayName || '(blank name)'} (${lesson.id}) • class ${lesson.cohortId ?? '(missing)'} • subject ${lesson.subjectId ?? '(missing)'} • ${fields.map(([field, label]) => `${label}: ${Object.hasOwn(lesson, field) ? JSON.stringify(lesson[field]) : '(omitted)'} [${exceptions.has(field) ? 'explicit exception' : row.mixedFields?.includes(field) ? 'Mixed' : 'follows shared setting'}]`).join('; ')}`;
};
export function renderRequirements(host, snapshot, narrow, actions, status, leave) {
  if (!model || model.version !== snapshot.version) {
    if (loadFailed) { const message = el('p', error); message.setAttribute('role', 'alert'); host.append(message); const retry = el('button', 'Retry saved requirement inspection'); retry.addEventListener('click', () => { loadFailed = false; actions.render(snapshot); }); host.append(retry); return; }
    if (!loading) {
      loading = actions.requirementsView().then(result => { loading = null; model = result; if (working?.dirty) { working.preview = null; working.version = result.version; error = 'Saved draft refreshed. Working choices remain; preview the newer before/after values before saving.'; } else working = null; if (result.version !== snapshot.version) actions.refresh(); else actions.render(snapshot); }).catch(failure => { loading = null; loadFailed = true; error = failure.message; actions.render(snapshot); });
    }
    host.append(el('p', 'Loading saved weekly requirements…')); return;
  }
  if (!working) select(snapshot, model.requirements[0]?.seriesId || '');
  if (busy) status.textContent = 'Reviewing… Working requirement choices are not yet durable.';
  else if (working.dirty) status.textContent = narrow ? 'Unsaved working choices retained; read-only view shows saved requirements.' : 'Unsaved — Working requirement differs from the saved draft.';
  if (narrow) {
    for (const row of model.requirements) { const block = el('section'); block.append(el('h4', `Requirement ${row.seriesId}`), el('p', `Class ${row.cohortId}; subject ${row.subjectId}; saved count ${JSON.stringify(row.count)}.`)); for (const lesson of row.occurrences) block.append(el('p', describe(lesson, row))); host.append(block); }
    if (!model.requirements.length) host.append(el('p', 'No saved weekly requirements.')); for (const lesson of model.ungrouped) host.append(el('p', `Ungrouped: ${lesson.displayName} (${lesson.id}). Edit individually in occurrence details (UC-9).`)); return;
  }
  const changed = () => { working.dirty = true; working.preview = null; error = null; status.textContent = 'Unsaved — Working requirement differs from the saved draft.'; host.querySelector('#requirement-error')?.remove(); };
  const list = el('div'); list.className = 'actions';
  for (const row of model.requirements) { const button = el('button', `Edit requirement: ${row.seriesId}`); button.disabled = busy; button.addEventListener('click', () => leave(latest => { select(latest, row.seriesId); actions.render(latest); })); list.append(button); }
  const add = el('button', 'Add weekly requirement', 'add-requirement'); add.disabled = busy; add.addEventListener('click', () => leave(latest => { select(latest); actions.render(latest); })); list.append(add); host.append(list);
  const form = el('form', undefined, 'requirement-form'); form.className = 'definition-entity-form'; form.noValidate = true;
  for (const [field, label, collection] of [['seriesId', working.originalId ? 'Series identifier (read-only)' : 'Series identifier suggestion / advanced override', null], ['cohortId', 'Requirement class identifier', 'cohorts'], ['subjectId', 'Requirement subject identifier', 'subjects'], ['count', 'Weekly lesson count', null]]) {
    const wrapper = el('label', label), input = el('input', undefined, `requirement-${field}`); input.value = working.request[field] ?? ''; input.disabled = busy; input.readOnly = field === 'seriesId' && !!working.originalId;
    if (collection) { const choices = el('datalist', undefined, `requirement-${field}-choices`); input.setAttribute('list', choices.id); for (const row of snapshot.workspace.definitionDraft.definition[collection]) { const option = el('option', row.displayName); option.value = row.id; choices.append(option); } wrapper.append(choices); }
    if (field === 'count') input.inputMode = 'numeric';
    input.addEventListener('input', () => { working.request[field] = field === 'count' ? parse(input.value) : input.value; if (field === 'seriesId') working.override = true;
      if (['cohortId', 'subjectId'].includes(field) && !working.originalId && !working.override) { working.request.seriesId = suggestion(snapshot.workspace.definitionDraft.definition, working.request.cohortId, working.request.subjectId, snapshot.workspace.definitionDraft); form.querySelector('#requirement-seriesId').value = working.request.seriesId; } changed(); }); wrapper.append(input); form.append(wrapper);
  }
  if (!Number.isInteger(working.row.count) || working.row.count < 0 || working.row.count > 2147483647) form.append(el('p', 'Saved incomplete count work: shared choices are retained, while existing occurrences remain unchanged until the count is completed.'));
  form.append(el('p', 'A weekly requirement forms a series with a common class and subject. The ordinary same-day spreading preference does not require adjacency or one lesson per day. Existing occurrence identifiers, names and exceptions stay stable. Count zero keeps editing context; invalid count work leaves existing occurrences unchanged.'));
  for (const [field, label, collection] of fields) {
    const block = el('fieldset'); block.append(el('legend', label)); const state = working.controls[field] ||= { mode: 'keep', raw: arrayFields.has(field) ? (Array.isArray(working.row.defaults[field]) ? working.row.defaults[field].join(', ') : '') : working.row.defaults[field] ?? '', scope: working.row.mixedFields.includes(field) ? 'selected' : 'followers', selected: [] };
    const mixed = working.row.mixedFields.includes(field); block.append(el('p', mixed ? 'Mixed — imported values disagree. No default is guessed; explicitly choose followers when establishing one.' : `Saved shared value: ${Object.hasOwn(working.row.defaults, field) ? JSON.stringify(working.row.defaults[field]) : '(omitted)'}. Explicit exceptions remain exceptions even when equal.`));
    const mode = el('select', undefined, `requirement-${field}-mode`); mode.setAttribute('aria-label', `${label} choice`); for (const [value, text] of [['keep', 'Keep saved setting'], ['omitted', field === 'teacherId' ? 'Missing teacher (incomplete work)' : 'Use omitted default'], ['value', arrayFields.has(field) ? 'Explicit selection (empty means none)' : 'Explicit value']]) { const option = el('option', text); option.value = value; mode.append(option); } mode.value = state.mode; mode.disabled = busy;
    const input = el('input', undefined, `requirement-${field}`); input.setAttribute('aria-label', `${label} ${arrayFields.has(field) ? 'identifiers (comma-separated)' : 'identifier'}`); input.value = state.raw; input.disabled = busy || state.mode !== 'value';
    if (collection !== 'tokens' && !arrayFields.has(field)) { const choices = el('datalist', undefined, `requirement-${field}-choices`); input.setAttribute('list', choices.id); for (const row of snapshot.workspace.definitionDraft.definition[collection]) { const option = el('option', row.displayName); option.value = row.id; choices.append(option); } block.append(choices); }
    const scope = el('select', undefined, `requirement-${field}-scope`); scope.setAttribute('aria-label', `${label} change scope`); for (const [value, text] of [['followers', 'Existing followers; preserve exceptions'], ['selected', 'Choose occurrences to follow'], ['all', 'Apply to all; overwrite named exceptions']]) { const option = el('option', text); option.value = value; scope.append(option); } scope.value = state.scope; scope.disabled = busy || state.mode === 'keep';
    const update = () => { delete working.request.changes[field]; delete working.request.followers[field]; working.request.applyToAllFields = working.request.applyToAllFields.filter(value => value !== field);
      if (state.mode !== 'keep') { working.request.changes[field] = state.mode === 'omitted' ? { mode: 'omitted' } : { mode: 'value', value: arrayFields.has(field) ? state.raw.split(',').map(value => value.trim()).filter(Boolean) : state.raw };
        if (state.scope === 'selected') working.request.followers[field] = [...state.selected]; else if (state.scope === 'all') working.request.applyToAllFields.push(field); } changed(); };
    mode.addEventListener('change', () => { state.mode = mode.value; update(); actions.render(snapshot); }); input.addEventListener('input', () => { state.raw = input.value; update(); }); scope.addEventListener('change', () => { state.scope = scope.value; update(); actions.render(snapshot); }); block.append(mode, input, scope);
    if (state.scope === 'selected' && state.mode !== 'keep') for (const lesson of working.row.occurrences) { const wrapper = el('label'), checkbox = el('input'); checkbox.type = 'checkbox'; checkbox.checked = state.selected.includes(lesson.id); checkbox.disabled = busy; checkbox.setAttribute('aria-label', `${label} follower: ${lesson.displayName} (${lesson.id})`); checkbox.addEventListener('change', () => { state.selected = state.selected.filter(value => value !== lesson.id); if (checkbox.checked) state.selected.push(lesson.id); update(); }); wrapper.append(checkbox, document.createTextNode(lesson.displayName + ' (' + lesson.id + ')')); block.append(wrapper); }
    form.append(block);
  }
  const occurrences = el('fieldset'); occurrences.append(el('legend', 'Existing occurrences and cancellation choices'));
  for (const lesson of working.row.occurrences) { const wrapper = el('label'), checkbox = el('input'); checkbox.type = 'checkbox'; checkbox.disabled = busy; checkbox.checked = working.request.removeIds.includes(lesson.id); checkbox.setAttribute('aria-label', `Cancel occurrence: ${lesson.displayName} (${lesson.id})`); checkbox.addEventListener('change', () => { working.request.removeIds = working.request.removeIds.filter(value => value !== lesson.id); if (checkbox.checked) working.request.removeIds.push(lesson.id); changed(); }); wrapper.append(checkbox, document.createTextNode(describe(lesson, working.row))); occurrences.append(wrapper);
    const placement = working.row.sourcePlacements.find(row => row.lessonId === lesson.id); if (placement) occurrences.append(el('p', `Current source placement: ${placement.periodId}, room ${placement.roomId}. Current will remain unchanged.`)); }
  if (!working.row.occurrences.length) occurrences.append(el('p', 'No saved occurrences in this requirement.')); form.append(occurrences);
  const controls = el('div'); controls.className = 'actions'; const preview = el('button', 'Preview requirement changes', 'preview-requirement'); preview.type = 'submit'; preview.disabled = busy;
  const cancel = el('button', 'Discard working requirement choices', 'cancel-requirement'); cancel.type = 'button'; cancel.disabled = busy; cancel.addEventListener('click', () => { discardRequirements(snapshot); actions.render(snapshot); }); controls.append(preview, cancel); form.append(controls);
  form.addEventListener('submit', event => { event.preventDefault(); saveRequirement(snapshot, actions); }); host.append(form);
  if (error) { const message = el('p', error, 'requirement-error'); message.className = 'error'; message.setAttribute('role', 'alert'); host.append(message); const refresh = el('button', 'Review newer saved draft for retry', 'requirement-refresh'); refresh.addEventListener('click', actions.refresh); host.append(refresh); }
  for (const lesson of model.ungrouped) host.append(el('p', `Ungrouped occurrence ${lesson.displayName} (${lesson.id}) stays separate. Customize or group explicitly in occurrence details (UC-9).`));
  for (const issue of snapshot.workspace.definitionDraft.issues || []) if (['Requirement', 'Lesson', 'Teacher', 'Cohort'].includes(issue.ownerType)) host.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
  if (working.preview && !narrow) renderPreview(host, snapshot, actions);
}
function renderPreview(host, snapshot, actions) {
  const preview = working.preview, dialog = el('dialog', undefined, 'requirement-preview'); dialog.append(el('h4', 'Review weekly requirement operation'), el('p', `Saved draft version ${preview.version}. Resulting count: ${JSON.stringify(preview.requirement.count)}. Additions ${preview.added.length}; cancellations ${preview.removed.length}.`));
  if (!Number.isInteger(preview.requirement.count) || preview.requirement.count < 0 || preview.requirement.count > 2147483647) dialog.append(el('p', 'This count cannot be expanded. Saving retains incomplete requirement work with a blocker; existing occurrences remain unchanged.'));
  for (const [title, rows] of [['Before — saved occurrences', preview.before], ['After — proposed occurrences', preview.after], ['Selected cancellations', preview.removed], ['Fresh additions', preview.added]]) { const block = el('section'); block.append(el('h5', title)); for (const lesson of rows) block.append(el('p', describe(lesson, (title.startsWith('Before') || title === 'Selected cancellations') && preview.previousRequirement?.seriesId ? preview.previousRequirement : preview.requirement))); if (!rows.length) block.append(el('p', 'None.')); dialog.append(block); }
  if (preview.overwrittenExceptions.length) { const block = el('section'); block.append(el('h5', 'Apply to all will overwrite these explicit exceptions')); for (const row of preview.overwrittenExceptions) block.append(el('p', `${row.lessonId} • ${fields.find(([field]) => field === row.field)?.[1]}: previous ${JSON.stringify(row.previous)}.`)); dialog.append(block); }
  for (const placement of preview.sourcePlacements) dialog.append(el('p', `Current source placement ${placement.lessonId}: ${placement.periodId}, ${placement.roomId}. Unchanged by this operation.`));
  for (const issue of preview.issues) dialog.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
  const close = () => { working.preview = null; dialog.close(); dialog.remove(); resolveReview?.(false); resolveReview = null; };
  const cancel = el('button', 'Cancel requirement review', 'cancel-requirement-review'); cancel.addEventListener('click', close);
  const save = el('button', 'Save reviewed requirement', 'save-requirement'); save.addEventListener('click', async () => {
    dialog.close(); dialog.remove(); busy = true; const resolve = resolveReview; resolveReview = null; actions.render(snapshot);
    try { const result = await actions.requirement({ request: working.request, token: preview.token }, preview.version); const id = working.request.seriesId; busy = false; const draft = result.workspace.definitionDraft; model = { version: result.version, requirements: Object.values(draft.editingContext.requirements).map(row => ({ ...row, occurrences: draft.definition.lessons.filter(lesson => lesson.seriesId === row.seriesId), sourcePlacements: (draft.sourceDocument?.acceptedBaseline?.result?.timetable?.assignments || []).filter(placement => draft.definition.lessons.some(lesson => lesson.seriesId === row.seriesId && lesson.id === placement.lessonId)) })), ungrouped: draft.definition.lessons.filter(lesson => !Object.hasOwn(lesson, 'seriesId')) }; select(result, id); actions.render(result); resolve?.(result); }
    catch (failure) { busy = false; working.preview = null; working.dirty = true; error = `No change was committed. ${failure.message} Working choices remain Unsaved; review again before retrying.`; actions.render(snapshot); resolve?.(false); }
  }); dialog.append(cancel, save); dialog.addEventListener('cancel', event => { event.preventDefault(); close(); }); host.append(dialog); queueMicrotask(() => { if (dialog.isConnected && !dialog.open) dialog.showModal(); });
}
window.addEventListener('beforeunload', event => { if (requirementsDirty()) { event.preventDefault(); event.returnValue = ''; } });
