let working;
let busy = false;
let error;
const el = (tag, text, id) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (id) node.id = id; return node; };
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
export const roomsDirty = () => !!working?.dirty;
export function resetRooms() { working = null; busy = false; error = null; }
function suggestion(name, draft) {
  const base = (name.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9._-]/g, '-').replace(/-+/g, '-').replace(/^[^a-z0-9]+/, '').replace(/-+$/, '') || 'room').slice(0, 128);
  const source = draft.sourceDocument?.initialDefinition || draft.sourceDocument?.acceptedBaseline?.definition;
  const reserved = new Set([...(draft.definition.rooms || []), ...(source?.rooms || []), ...(draft.removedIdentities || []).filter(row => row.ownerType === 'Room')].map(row => row.id));
  let id = base;
  for (let n = 2; reserved.has(id); n++) { const end = `-${n}`; id = base.slice(0, 128 - end.length) + end; }
  return id;
}
function select(snapshot, id = '') {
  const draft = snapshot.workspace.definitionDraft;
  const item = id ? draft.definition.rooms.find(row => row.id === id) : { id: suggestion('', draft), displayName: '', capacity: '', capabilityIds: [] };
  working = { originalId: id, item: structuredClone(item), base: structuredClone(item), version: snapshot.version, dirty: false, override: false, conflicts: [] }; error = null;
}
export function discardRooms(snapshot) { select(snapshot, snapshot.workspace.definitionDraft.definition.rooms.some(row => row.id === working?.originalId) ? working.originalId : ''); }
function mergeNewer(snapshot) {
  if (working.version === snapshot.version) return;
  if (!working.originalId) { working.version = snapshot.version; return; }
  const newest = snapshot.workspace.definitionDraft.definition.rooms.find(row => row.id === working.originalId);
  if (!newest) { working.conflicts = [{ field: '$removed', latest: undefined, value: working.item }]; working.version = snapshot.version; error = 'This room was removed in the newer draft. Discard working edits to select another room.'; return; }
  const merged = structuredClone(newest), conflicts = [];
  for (const field of new Set([...Object.keys(working.base), ...Object.keys(working.item)])) {
    if (same(working.item[field], working.base[field])) continue;
    if (!same(newest[field], working.base[field]) && !same(newest[field], working.item[field])) conflicts.push({ field, latest: newest[field], value: working.item[field] });
    if (Object.hasOwn(working.item, field)) merged[field] = working.item[field]; else delete merged[field];
  }
  working.item = merged; working.base = structuredClone(newest); working.version = snapshot.version; working.conflicts = conflicts; error = null;
}
export async function saveRoom(snapshot, actions) {
  if (busy) return false;
  busy = true; error = null; actions.render(snapshot);
  try {
    if (working.conflicts.length) throw new Error('Resolve the newer saved conflicts before saving.');
    const result = await actions.entity('rooms', { originalId: working.originalId, item: working.item }, snapshot.version);
    select(result, working.item.id); busy = false; actions.render(result); return result;
  } catch (failure) { busy = false; error = `No change was committed. ${failure.message} Working values remain Unsaved.`; working.dirty = true; actions.render(snapshot); return false; }
}
export function renderRooms(host, snapshot, narrow, actions, status, leave) {
 if (!narrow) { const removal = el('button', 'Guided removal'); removal.type = 'button'; removal.addEventListener('click', actions.removal); host.append(removal); }

  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  if (!working || !working.dirty) select(snapshot, working && !working.originalId ? '' : definition.rooms.some(row => row.id === working?.originalId) ? working.originalId : definition.rooms[0]?.id || '');
  else mergeNewer(snapshot);
  if (busy) status.textContent = 'Saving… Working room values are not yet durable.';
  else if (working.dirty) status.textContent = narrow ? 'Unsaved working edits retained; read-only view shows saved rooms.' : 'Unsaved — Working room differs from the saved draft.';
  if (narrow) {
    for (const item of definition.rooms) {
      const block = el('section'); block.append(el('h4', item.displayName || item.id));
      for (const [field, value] of Object.entries(item)) block.append(el('p', `${field}: ${JSON.stringify(value)}`));
      if (!Object.hasOwn(item, 'availablePeriodIds')) block.append(el('p', 'Availability: All declared periods (omitted).'));
      host.append(block);
    }
    if (!definition.rooms.length) host.append(el('p', 'No saved rooms.')); return;
  }
  const list = el('div'); list.className = 'actions';
  for (const item of definition.rooms) {
    const button = el('button', `Edit room: ${item.displayName || item.id}`); button.type = 'button'; button.disabled = busy; button.dataset.roomId = item.id;
    button.addEventListener('click', () => leave(latest => { select(latest, item.id); actions.render(latest); })); list.append(button);
  }
  const add = el('button', 'Add room', 'add-room'); add.type = 'button'; add.disabled = busy;
  add.addEventListener('click', () => leave(latest => { select(latest); actions.render(latest); })); list.append(add); host.append(list);
  const form = el('form', undefined, 'room-form'); form.className = 'definition-entity-form'; form.noValidate = true;
  const changed = () => { working.dirty = true; error = null; status.textContent = 'Unsaved — Working room differs from the saved draft.'; document.querySelector('#room-save-error')?.remove(); };
  for (const [field, label] of [['displayName', 'Room name'], ['id', working.originalId ? 'Room identifier (read-only)' : 'Room identifier suggestion / advanced override']]) {
    const wrapper = el('label', label), input = el('input', undefined, `room-${field}`); input.value = working.item[field] ?? ''; input.readOnly = field === 'id' && !!working.originalId; input.disabled = busy;
    input.addEventListener('input', () => { working.item[field] = input.value; if (field === 'id') working.override = true;
      if (field === 'displayName' && !working.originalId && !working.override) { working.item.id = suggestion(input.value, draft); form.querySelector('#room-id').value = working.item.id; } changed(); });
    wrapper.append(input); form.append(wrapper);
  }
  form.append(el('p', 'Saved identifiers are permanent. Capability tokens are open identifiers, matched exactly, with no separate catalog. Capacity zero and empty capabilities are supported. Room edits never rewrite class home rooms, lessons or policies.'));
  const capacityLabel = el('label', 'Room capacity'), capacity = el('input', undefined, 'room-capacity');
  capacity.type = 'text'; capacity.inputMode = 'numeric'; capacity.value = working.item.capacity ?? ''; capacity.disabled = busy;
  capacity.addEventListener('input', () => { const raw = capacity.value; working.item.capacity = /^-?\d+$/.test(raw) && Number.isSafeInteger(Number(raw)) ? Number(raw) : raw; changed(); }); capacityLabel.append(capacity); form.append(capacityLabel);
  const tokens = [...new Set([...(definition.rooms || []).flatMap(row => Array.isArray(row.capabilityIds) ? row.capabilityIds : []), ...(definition.lessons || []).flatMap(row => Array.isArray(row.requiredRoomCapabilityIds) ? row.requiredRoomCapabilityIds : [])])].filter(value => typeof value === 'string').map(id => ({ id, displayName: id }));
  for (const [field, label, rows, optional, defaultText] of [
    ['capabilityIds', 'Capability tokens', tokens, false, ''],
    ['availablePeriodIds', 'Room availability', definition.periods, true, 'All declared periods (omitted)']
  ]) {
    const block = el('fieldset'); block.append(el('legend', label));
    let explicit = Object.hasOwn(working.item, field);
    if (optional) {
      const wrapper = el('label', `${label} mode`), mode = el('select', undefined, `room-${field}-mode`);
      for (const [value, text] of [['default', defaultText], ['explicit', 'Explicit selection (empty means none)']]) { const option = el('option', text); option.value = value; mode.append(option); }
      mode.value = explicit ? 'explicit' : 'default'; mode.disabled = busy;
      mode.addEventListener('change', () => { if (mode.value === 'default') delete working.item[field]; else working.item[field] = []; changed(); actions.render(snapshot); }); wrapper.append(mode); block.append(wrapper);
    } else explicit = true;
    const values = Array.isArray(working.item[field]) ? working.item[field] : [];
    if (Object.hasOwn(working.item, field) && !Array.isArray(working.item[field])) block.append(el('p', `Saved invalid selection: ${JSON.stringify(working.item[field])}. Retained until you explicitly choose a corrected selection.`));
    if (!rows.length) block.append(el('p', field === 'capabilityIds' ? 'No existing capability tokens. Enter any valid token below.' : 'No declared periods. Configure the weekly grid before choosing periods.'));
    for (const row of rows) {
      const wrapper = el('label'), input = el('input'); input.type = 'checkbox'; input.dataset.field = field; input.dataset.referenceId = row.id;
      input.checked = values.includes(row.id); input.disabled = busy || !explicit;
      const title = field === 'capabilityIds' ? `${row.displayName || row.id} (${row.id})` : `${row.displayName || row.id} (${row.id}) • ${row.weekday} ${row.order}${definition.reservedPeriodIds?.includes(row.id) ? ' • Reserved' : ''}`;
      input.setAttribute('aria-label', `${label}: ${title}`); wrapper.append(input, document.createTextNode(title));
      input.addEventListener('change', () => { const old = Array.isArray(working.item[field]) ? working.item[field] : []; working.item[field] = old.filter(value => value !== row.id); if (input.checked) working.item[field].push(row.id); changed(); }); block.append(wrapper);
    }
    const known = new Set(rows.map(row => row.id)), unresolved = values.filter(value => !known.has(value));
    const wrapper = el('label', field === 'capabilityIds' ? 'Additional capability tokens (comma-separated)' : `Additional or unresolved ${label.toLowerCase()} identifiers (comma-separated)`), extra = el('input', undefined, `room-${field}-extra`);
    extra.value = unresolved.map(value => typeof value === 'string' ? value : JSON.stringify(value)).join(', '); extra.disabled = busy || !explicit;
    extra.addEventListener('input', () => { const selected = Array.isArray(working.item[field]) ? working.item[field].filter(value => known.has(value)) : []; working.item[field] = [...selected, ...extra.value.split(',').map(value => value.trim()).filter(Boolean)]; changed(); }); wrapper.append(extra); block.append(wrapper); form.append(block);
  }
  if (working.conflicts.length) {
    const block = el('section', undefined, 'room-conflicts'); block.className = 'notice'; block.append(el('h4', 'Newer saved fields conflict with working edits'), el('p', 'Untouched fields use the newer saved values. Choose explicitly before retrying.'));
    for (const row of working.conflicts) block.append(el('p', `${row.field}: latest ${JSON.stringify(row.latest) ?? '(removed/omitted)'}; working ${JSON.stringify(row.value) ?? '(omitted)'}`));
    for (const [id, label, useLatest] of [['room-keep-conflicts', 'Keep my reviewed conflicting values', false], ['room-use-latest', 'Use newer saved conflicting values', true]]) {
      const button = el('button', label, id); button.type = 'button'; button.disabled = busy || working.conflicts.some(row => row.field === '$removed');
      button.addEventListener('click', () => { if (useLatest) for (const row of working.conflicts) { if (row.latest === undefined) delete working.item[row.field]; else working.item[row.field] = row.latest; } working.conflicts = []; changed(); actions.render(snapshot); }); block.append(button);
    }
    form.append(block);
  }
  const controls = el('div'); controls.className = 'actions';
  const save = el('button', 'Save room', 'save-room'); save.type = 'submit'; save.disabled = busy || !!working.conflicts.length;
  const cancel = el('button', 'Discard working room edits', 'cancel-room'); cancel.type = 'button'; cancel.disabled = busy; cancel.addEventListener('click', () => { discardRooms(snapshot); actions.render(snapshot); }); controls.append(save, cancel); form.append(controls);
  form.addEventListener('submit', event => { event.preventDefault(); saveRoom(snapshot, actions); });
  if (error) { const alert = el('p', error, 'room-save-error'); alert.className = 'error'; alert.setAttribute('role', 'alert'); form.append(alert);
    const refresh = el('button', 'Review newer saved draft for retry', 'room-refresh'); refresh.type = 'button'; refresh.addEventListener('click', actions.refresh); form.append(refresh); }
  host.append(form);
  if (working.originalId) {
    const id = working.originalId, effects = el('section', undefined, 'room-effects'); effects.append(el('h4', 'Saved room effects'));
    for (const cohort of definition.cohorts.filter(row => row.homeRoomId === id)) effects.append(el('p', `Class home room: ${cohort.displayName} (${cohort.id}), size ${cohort.size}.`));
    for (const lesson of definition.lessons) effects.append(el('p', `Lesson ${lesson.id}: required capabilities ${JSON.stringify(lesson.requiredRoomCapabilityIds ?? [])}; room lock ${lesson.roomLock ?? '(none)'}. Requirements and locks remain saved.`));
    for (const policy of definition.roomAssignments || []) effects.append(el('p', `Room policy ${policy.id}: ${JSON.stringify(policy)}`));
    for (const issue of draft.issues || []) if ((issue.ownerType === 'Room' && issue.ownerId === id) || issue.ownerType === 'Lesson' || issue.ownerType === 'Cohort') effects.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
    effects.append(el('p', 'Remove this room through guided dependency resolution (UC-13); restore source values through source restoration (UC-14). Removing a capability can expose a feasibility advisory; explicit invalid policies/locks are blockers. No dependent record changes through room save.')); host.append(effects);
  }
}
window.addEventListener('beforeunload', event => { if (roomsDirty()) { event.preventDefault(); event.returnValue = ''; } });
