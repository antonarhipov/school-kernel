const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const title = day => day[0] + day.slice(1).toLowerCase();
let working;
let identity;
let preview;
let busy = false;
let error;
const el = (tag, text, id) => { const n = document.createElement(tag); if (text !== undefined) n.textContent = text; if (id) n.id = id; return n; };
export function resetGrid() { working = null; identity = null; preview = null; error = null; }
export function gridDirty() { return !!working?.dirty; }
export function discardGrid(definition) { working = values(definition); preview = null; error = null; }
const values = definition => ({ periods: structuredClone(definition.periods), ...(Object.hasOwn(definition, 'reservedPeriodIds') ? { reservedPeriodIds: [...definition.reservedPeriodIds] } : {}), dirty: false });
const payload = () => ({ periods: working.periods, ...(Object.hasOwn(working, 'reservedPeriodIds') ? { reservedPeriodIds: working.reservedPeriodIds } : {}) });
const edited = status => { working.dirty = true; preview = null; error = null; if (status) status.textContent = 'Unsaved — Working grid differs from the saved draft.'; document.querySelector('#grid-preview')?.remove(); };

export async function saveGrid(snapshot, actions) {
  if (busy) return false;
  busy = true; error = null; actions.render(snapshot);
  try {
    // Save and continue also obtains the same authoritative preview; no durable write occurs until explicit save.
    if (!preview || preview.workspaceVersion !== snapshot.version) preview = await actions.grid(payload(), snapshot.version, true);
    const result = await actions.grid(payload(), preview.workspaceVersion);
    working = values(result.workspace.definitionDraft.definition); preview = null; busy = false;
    actions.render(result); return result;
  } catch (failure) {
    busy = false; working.dirty = true; error = `No change was committed. ${failure.message} Working rows remain Unsaved.`;
    actions.render(snapshot); return false;
  }
}

export function renderGrid(host, snapshot, narrow, actions, status) {
  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  const nextIdentity = draft.openedRevision || draft.draftRevision;
  if (identity !== nextIdentity) { identity = nextIdentity; working = null; preview = null; error = null; }
  if (!working || !working.dirty) working = values(definition);
  host.append(el('p', 'Periods are explicit teaching slots. Reservations are separate from resource availability. Existing IDs stay fixed; removal requires guided dependency resolution.'));
  if (narrow) { renderRows(host, values(definition), true, status, definition); return; }
  const generate = el('details'); generate.open = definition.periods.length === 0;
  generate.append(el('summary', definition.periods.length ? 'Add generated slots (existing grid is preserved)' : 'Generate weekly grid'));
  const counts = el('div'); counts.className = 'grid-generator-days';
  const dayInputs = [];
  for (let index = 0; index < weekdays.length; index++) {
    const label = el('label', `${title(weekdays[index])} slots`), input = el('input', undefined, `grid-count-${weekdays[index].toLowerCase()}`);
    input.type = 'number'; input.min = '0'; input.max = '2147483647'; input.value = index < 5 ? '2' : '0'; label.append(input); counts.append(label); dayInputs.push(input);
  }
  generate.append(counts, el('p', 'Zero omits that weekday. For an existing grid, counts add slots after each day’s last order; they never replace or remove saved rows.'));
  const bells = el('div'); bells.className = 'grid-generator-bells';
  const starts = [], ends = [];
  const updateBells = () => {
    const count = Math.max(0, ...dayInputs.map(i => Math.min(2147483647, Number(i.value) || 0)));
    const oldStarts = starts.map(i => i.value), oldEnds = ends.map(i => i.value);
    bells.replaceChildren(); starts.length = 0; ends.length = 0;
    for (let i = 0; i < count; i++) {
      const pair = el('div'); pair.className = 'grid-bell-pair';
      for (const [field, items, old] of [['start', starts, oldStarts], ['end', ends, oldEnds]]) {
        const label = el('label', `Slot ${i + 1} ${field} (optional)`), input = el('input', undefined, `grid-bell-${field}-${i + 1}`);
        input.type = 'text'; input.placeholder = 'HH:mm or HH:mm:ss'; input.value = old[i] || ''; label.append(input); pair.append(label); items.push(input);
      }
      bells.append(pair);
    }
  };
  for (const input of dayInputs) input.addEventListener('change', updateBells);
  updateBells(); generate.append(bells);
  const add = el('button', definition.periods.length ? 'Preview additions' : 'Generate working rows', 'generate-grid'); add.type = 'button'; add.disabled = busy;
  add.addEventListener('click', () => {
    if (dayInputs.some(i => !Number.isInteger(Number(i.value)) || Number(i.value) < 0 || Number(i.value) > 2147483647)) { error = 'Use integer generation counts from 0 to 2147483647 per day. Working rows and saved grid are unchanged.'; actions.render(snapshot); return; }
    const used = new Set([...working.periods.map(p => p.id), ...(draft.removedIdentities || []).filter(i => i.ownerType === 'Period').map(i => i.id)]);
    weekdays.forEach((day, dayIndex) => {
      const existingOrders = working.periods.filter(p => p.weekday === day).map(p => Number(p.order)).filter(Number.isFinite);
      let order = Math.max(0, ...existingOrders);
      for (let i = 0; i < Number(dayInputs[dayIndex].value); i++) {
        order++;
        const base = `${day.toLowerCase()}-${order}`; let id = base, suffix = 2;
        while (used.has(id)) id = `${base}-${suffix++}`; used.add(id);
        const row = { id, displayName: `${title(day)} ${order}`, weekday: day, order };
        if (starts[i]?.value) row.startTime = starts[i].value;
        if (ends[i]?.value) row.endTime = ends[i].value;
        working.periods.push(row);
      }
    });
    edited(status); actions.render(snapshot);
  }); generate.append(add); host.append(generate);
  renderRows(host, working, false, status, definition);
  const reservation = el('label', 'Reservation representation'), mode = el('select', undefined, 'grid-reservation-mode');
  for (const [value, label] of [['default', 'No reservations (omitted)'], ['explicit', 'Explicit reservation membership']]) { const option = el('option', label); option.value = value; mode.append(option); }
  mode.value = Object.hasOwn(working, 'reservedPeriodIds') ? 'explicit' : 'default'; mode.disabled = busy;
  mode.addEventListener('change', () => { if (mode.value === 'default') delete working.reservedPeriodIds; else working.reservedPeriodIds = []; edited(status); actions.render(snapshot); });
  reservation.append(mode); host.append(reservation);
  const controls = el('div'); controls.className = 'actions';
  const review = el('button', 'Review grid operation', 'preview-grid'); review.disabled = busy;
  review.addEventListener('click', async () => {
    busy = true; error = null;
    try { preview = await actions.grid(payload(), snapshot.version, true); }
    catch (failure) { error = `No change was committed. ${failure.message} Working rows remain available.`; }
    busy = false; actions.render(snapshot);
  });
  const cancel = el('button', 'Cancel working grid edits', 'cancel-grid'); cancel.className = 'secondary'; cancel.disabled = busy;
  cancel.addEventListener('click', () => { discardGrid(definition); actions.render(snapshot); });
  controls.append(review, cancel); host.append(controls);
  if (error) { const alert = el('p', error, 'grid-save-error'); alert.className = 'error'; alert.setAttribute('role', 'alert'); host.append(alert);
    const reload = el('button', 'Review newer saved draft for retry', 'grid-refresh'); reload.addEventListener('click', actions.refresh); host.append(reload); }
  if (preview) {
    const block = el('section', undefined, 'grid-preview'); block.append(el('h4', `Grid preview • saved workspace version ${preview.workspaceVersion}`));
    block.append(el('p', 'All shown working rows and reservations will save together. Invalid supported rows remain unfinishable until corrected. Source and Current remain unchanged.'));
    renderRows(block, preview, true, status, definition);
    for (const effect of preview.availabilityEffects) block.append(el('p', `${effect.displayName} (${effect.ownerId}): ${effect.periodId} — ${effect.effect}`));
    renderImpacts(block, preview.timingImpacts);
    for (const issue of preview.issues) block.append(el('p', `${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
    const save = el('button', 'Save reviewed grid', 'save-grid'); save.disabled = busy; save.addEventListener('click', () => saveGrid(snapshot, actions)); block.append(save); host.append(block);
  }
  renderImpacts(host, draft.gridTimingImpacts);
  for (const issue of draft.issues || []) if (issue.ownerType === 'Period' || issue.field === 'reservedPeriodIds') host.append(el('p', `Saved blocker: ${issue.ownerId} ${issue.field} — ${issue.message}`));
}
function renderRows(host, grid, readonly, status, definition) {
  const container = el('div'); container.className = 'grid-period-rows';
  const existing = new Set(definition.periods.map(p => p.id));
  grid.periods.forEach((row, index) => {
    const card = el('fieldset'); card.className = 'grid-period-row'; card.dataset.periodId = row.id;
    card.append(el('legend', `${row.displayName || '(incomplete period)'} • ${row.id}`));
    for (const field of ['id', 'displayName', 'weekday', 'order', 'startTime', 'endTime']) {
      const label = el('label', ({ id: 'Identifier', displayName: 'Name', weekday: 'Weekday', order: 'Order', startTime: 'Start time (optional)', endTime: 'End time (optional)' })[field]);
      if (readonly) { label.append(el('span', String(row[field] ?? '(omitted)'))); card.append(label); continue; }
      const input = el(field === 'weekday' ? 'select' : 'input'); input.dataset.field = field; input.setAttribute('aria-label', `${row.id} ${field}`); input.disabled = busy;
      if (field === 'weekday') { for (const day of [...new Set([row.weekday || '', ...weekdays])]) { const option = el('option', title(day) || 'Choose weekday'); option.value = day; input.append(option); } }
      input.value = row[field] ?? ''; if (field === 'id') input.readOnly = existing.has(row.id);
      input.addEventListener('input', () => { if ((field === 'startTime' || field === 'endTime') && input.value === '') delete row[field]; else if (field === 'order') row[field] = /^\d+$/.test(input.value) && Number.isSafeInteger(Number(input.value)) ? Number(input.value) : input.value; else row[field] = input.value; edited(status); });
      label.append(input); card.append(label);
    }
    const label = el('label', 'Reserved period');
    if (readonly) label.append(el('span', (grid.reservedPeriodIds || []).includes(row.id) ? 'Yes (explicit membership)' : 'No'));
    else { const input = el('input'); input.type = 'checkbox'; input.dataset.field = 'reserved'; input.checked = (grid.reservedPeriodIds || []).includes(row.id); input.disabled = busy;
      input.addEventListener('change', () => { grid.reservedPeriodIds = (grid.reservedPeriodIds || []).filter(id => id !== row.id); if (input.checked) grid.reservedPeriodIds.push(row.id); edited(status); }); label.append(input); }
    card.append(label); container.append(card);
  });
  if (!grid.periods.length) container.append(el('p', 'No periods configured.'));
  host.append(container);
}
function renderImpacts(host, impacts = []) {
  if (!impacts?.length) return;
  host.append(el('h4', 'Teaching-time impacts (distinct from period-ID moves)'));
  for (const impact of impacts) host.append(el('p', `${impact.lessonName} (${impact.lessonId}) • ${impact.periodId}: ${format(impact.before)} → ${format(impact.after)}`));
}
const format = p => `${p.weekday} order ${p.order}, ${p.startTime ?? '(no start time)'}–${p.endTime ?? '(no end time)'}`;
window.addEventListener('beforeunload', event => { if (gridDirty()) { event.preventDefault(); event.returnValue = ''; } });
