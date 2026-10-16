import { enhanceDefinitionForm } from './definition-form-controls.js';
const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const title = day => { const value = String(day ?? 'Unspecified'); return value.charAt(0) + value.slice(1).toLowerCase(); };
let working;
let selectedPeriodId;
let inspectedPeriodId;
let identity;
let preview;
let busy = false;
let error;
const el = (tag, text, id) => { const n = document.createElement(tag); if (text !== undefined) n.textContent = text; if (id) n.id = id; return n; };
export function resetGrid() { working = null; identity = null; preview = null; error = null; selectedPeriodId = inspectedPeriodId = null; }
export function gridDirty() { return !!working?.dirty; }
export function choosePeriod(id) { selectedPeriodId = id; }
export function discardGrid(definition) { working = values(definition); preview = null; error = null; }
const gridValues = definition => ({ periods: structuredClone(definition.periods), ...(Object.hasOwn(definition, 'reservedPeriodIds') ? { reservedPeriodIds: [...definition.reservedPeriodIds] } : {}) });
const values = (definition, version) => ({ ...gridValues(definition), base: gridValues(definition), baseVersion: version, conflicts: [], dirty: false });
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
function mergeNewer(definition, version) {
  if (working.baseVersion === undefined || working.baseVersion === version) return;
  const newest = gridValues(definition), conflicts = [];
  const baseRows = new Map(working.base.periods.map(row => [row.id, row]));
  const newRows = new Map(newest.periods.map(row => [row.id, row]));
  for (const row of working.periods) {
    const base = baseRows.get(row.id), target = newRows.get(row.id);
    if (!target) {
      if (!base) newest.periods.push(structuredClone(row));
      else if (!same(row, base)) { newest.periods.push(structuredClone(row)); conflicts.push({ id: row.id, field: '$removed', latest: undefined, working: row }); }
      continue;
    }
    for (const field of new Set([...Object.keys(row), ...Object.keys(base || {})])) {
      if (field === 'id' || same(row[field], base?.[field])) continue;
      if (!same(target[field], base?.[field]) && !same(target[field], row[field])) conflicts.push({ id: row.id, field, latest: target[field], working: row[field] });
      if (Object.hasOwn(row, field)) target[field] = structuredClone(row[field]); else delete target[field];
    }
  }
  if (!same(working.reservedPeriodIds, working.base.reservedPeriodIds)) {
    if (!same(newest.reservedPeriodIds, working.base.reservedPeriodIds) && !same(newest.reservedPeriodIds, working.reservedPeriodIds)) conflicts.push({ id: 'School', field: 'reservedPeriodIds', latest: newest.reservedPeriodIds, working: working.reservedPeriodIds });
    if (Object.hasOwn(working, 'reservedPeriodIds')) newest.reservedPeriodIds = [...working.reservedPeriodIds]; else delete newest.reservedPeriodIds;
  }
  working = { ...newest, base: gridValues(definition), baseVersion: version, conflicts, dirty: true };
  preview = null; error = null;
}
function renderConflicts(host, snapshot, actions) {
  if (!working.conflicts?.length) return;
  const block = el('section', undefined, 'grid-conflicts'); block.className = 'notice';
  block.append(el('h4', 'Newer saved fields conflict with working edits'), el('p', 'Untouched fields use the newer saved values. Resolve these fields explicitly before another preview/save.'));
  for (const conflict of working.conflicts) block.append(el('p', `${conflict.id} ${conflict.field}: latest ${JSON.stringify(conflict.latest) ?? '(removed/omitted)'}; working ${JSON.stringify(conflict.working) ?? '(omitted)'}`));
  const keep = el('button', 'Keep my reviewed conflicting values', 'grid-keep-conflicts');
  keep.disabled = working.conflicts.some(conflict => conflict.field === '$removed');
  keep.addEventListener('click', () => { working.conflicts = []; edited(); actions.render(snapshot); });
  const latest = el('button', 'Use newer saved conflicting values', 'grid-use-latest'); latest.className = 'secondary';
  latest.addEventListener('click', () => {
    for (const conflict of working.conflicts) {
      if (conflict.field === '$removed') { working.periods = working.periods.filter(row => row.id !== conflict.id); continue; }
      const target = conflict.id === 'School' ? working : working.periods.find(row => row.id === conflict.id);
      if (conflict.latest === undefined) delete target[conflict.field]; else target[conflict.field] = structuredClone(conflict.latest);
    }
    working.conflicts = []; edited(); actions.render(snapshot);
  });
  block.append(keep, latest); host.append(block);
}
const payload = () => ({ periods: working.periods, ...(Object.hasOwn(working, 'reservedPeriodIds') ? { reservedPeriodIds: working.reservedPeriodIds } : {}) });
const edited = status => { working.dirty = true; preview = null; error = null; if (status) status.textContent = 'Unsaved — Working grid differs from the saved draft.'; document.querySelector('#grid-preview')?.remove(); };

export async function saveGrid(snapshot, actions) {
  if (busy) return false;
  busy = true; error = null; actions.render(snapshot);
  try {
    if (working.conflicts?.length) throw new Error('Resolve the newer saved field conflicts before saving.');
    // Save and continue also obtains the same authoritative preview; no durable write occurs until explicit save.
    if (!preview || preview.workspaceVersion !== snapshot.version) preview = await actions.grid(payload(), snapshot.version, true);
    const result = await actions.grid(payload(), preview.workspaceVersion);
    working = values(result.workspace.definitionDraft.definition, result.version); preview = null; busy = false;
    actions.render(result); return result;
  } catch (failure) {
    busy = false; working.dirty = true; error = `No change was committed. ${failure.message} Working rows remain Unsaved.`;
    actions.render(snapshot); return false;
  }
}

export function renderGrid(host, snapshot, narrow, actions, status) {
 if (!narrow) { const removal = el('button', 'Guided removal'); removal.type = 'button'; removal.addEventListener('click', actions.removal); host.append(removal); }

  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  const nextIdentity = draft.openedRevision || draft.draftRevision;
  if (identity !== nextIdentity) { identity = nextIdentity; working = null; preview = null; error = null; }
  if (!working || !working.dirty) working = values(definition, snapshot.version);
  else mergeNewer(definition, snapshot.version);
  host.append(el('p', 'Periods are explicit teaching slots. Reservations are separate from teacher, class and room availability. Existing IDs stay fixed; removal requires guided dependency resolution.'));
  if (narrow) { const grid = values(definition); renderGridOverview(host, grid, true, snapshot, actions, draft); renderRows(host, grid, true, status, definition, inspectedPeriodId); return; }
  if (working.dirty) status.textContent = 'Unsaved — Working grid differs from the saved draft.';
  if (busy) status.textContent = 'Reviewing or saving… Working grid is not yet durable.';
  const form = el('form', undefined, 'grid-form'); form.className = 'definition-grid-form'; form.noValidate = true; form.addEventListener('submit', event => event.preventDefault()); host.append(form); host = form;
  renderConflicts(host, snapshot, actions);
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
  renderGridOverview(host, working, false, snapshot, actions, draft);
  renderRows(host, working, false, status, definition, selectedPeriodId);
  const reservation = el('label', 'Reservation representation'), mode = el('select', undefined, 'grid-reservation-mode');
  for (const [value, label] of [['default', 'No reservations (omitted)'], ['explicit', 'Explicit reservation membership']]) { const option = el('option', label); option.value = value; mode.append(option); }
  mode.value = Object.hasOwn(working, 'reservedPeriodIds') ? 'explicit' : 'default'; mode.disabled = busy;
  mode.addEventListener('change', () => { if (mode.value === 'default') delete working.reservedPeriodIds; else working.reservedPeriodIds = []; edited(status); actions.render(snapshot); });
  reservation.append(mode); const reservations = el('details', undefined, 'grid-reservation-details'); reservations.append(el('summary', `Reservation representation — ${Object.hasOwn(working, 'reservedPeriodIds') ? `${working.reservedPeriodIds.length} explicit memberships` : 'No reservations (omitted)'}`), reservation); host.append(reservations);
  if (working.reservedPeriodIds?.some(id => !working.periods.some(row => row.id === id))) reservations.append(el('p', `Unresolved reservation IDs retained: ${working.reservedPeriodIds.filter(id => !working.periods.some(row => row.id === id)).join(', ')}.`));
  const controls = el('div'); controls.className = 'actions';
  const review = el('button', 'Review grid operation', 'preview-grid'); review.type = 'button'; review.disabled = busy;
  review.addEventListener('click', async () => {
    busy = true; error = null;
    try { if (working.conflicts?.length) throw new Error('Resolve the newer saved field conflicts before reviewing.'); preview = await actions.grid(payload(), snapshot.version, true); }
    catch (failure) { error = `No change was committed. ${failure.message} Working rows remain available.`; }
    busy = false; actions.render(snapshot);
  });
  const cancel = el('button', 'Cancel working grid edits', 'cancel-grid'); cancel.type = 'button'; cancel.className = 'secondary'; cancel.disabled = busy;
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
    const save = el('button', 'Save reviewed grid', 'save-grid'); save.type = 'button'; save.disabled = busy; save.addEventListener('click', () => saveGrid(snapshot, actions)); block.append(save); host.append(block);
  }
  renderImpacts(host, draft.gridTimingImpacts);
  const issues = el('details'); issues.append(el('summary', 'Saved grid issues')); for (const issue of draft.issues || []) if (issue.ownerType === 'Period' || issue.field === 'reservedPeriodIds') { const link = el('button', `${issue.ownerId} ${issue.field} — ${issue.message}`); link.type = 'button'; link.addEventListener('click', () => { selectedPeriodId = issue.ownerId; actions.render(snapshot); requestAnimationFrame(() => document.querySelector(`.grid-period-row [data-field="${issue.field}"]`)?.focus()); }); issues.append(link); } if (issues.childNodes.length > 1) host.append(issues);
  enhanceDefinitionForm(form, { prefix: 'grid', ownerType: 'School', item: working, draft: { ...draft, issues: [] }, status });
}
function renderGridOverview(host, grid, narrow, snapshot, actions, draft) {
  const picked = narrow ? inspectedPeriodId : selectedPeriodId;
  const selected = grid.periods.find(row => row.id === picked)?.id || grid.periods[0]?.id;
  if (narrow) inspectedPeriodId = selected; else selectedPeriodId = selected;
  const wrap = el('section', undefined, 'grid-overview'); wrap.className = 'definition-grid-overview';
  wrap.append(el('h4', 'Weekly teaching slots'), el('p', `${grid.periods.length} declared slots · ${(grid.reservedPeriodIds || []).length} reserved memberships. Select a slot to ${narrow ? 'inspect saved values' : 'edit it; all working rows save together'}.`));
  const scroll = el('div'); scroll.className = 'definition-week-grid-wrap'; const table = el('table'); table.className = 'definition-slot-table';
  const days = [...new Set(grid.periods.map(row => String(row.weekday ?? 'Unspecified')))].sort((a,b) => weekdays.indexOf(a) - weekdays.indexOf(b));
  const orders = [...new Set(grid.periods.map(row => row.order))].sort((a,b) => new Intl.Collator(undefined,{numeric:true}).compare(String(a),String(b)));
  const head = el('thead'), hr = el('tr'), corner = el('th','Slot');corner.scope='col';hr.append(corner);
  for (const day of days) { const th = el('th', title(day)); th.scope='col';hr.append(th); }head.append(hr);table.append(head);
  const body = el('tbody');
  for (const order of orders) { const tr = el('tr'), slot=el('th',String(order));slot.scope='row';tr.append(slot);
    for (const day of days) { const td=el('td');for (const row of grid.periods.filter(row=>String(row.weekday??'Unspecified')===day&&row.order===order)) {
      const reserved=(grid.reservedPeriodIds||[]).includes(row.id), issues=(draft.issues||[]).filter(issue=>issue.ownerType==='Period'&&issue.ownerId===row.id);
      const button=el('button');button.type='button';button.dataset.gridPeriodId=row.id;button.disabled=busy;button.setAttribute('aria-pressed',String(row.id===selected));button.setAttribute('aria-label',`${narrow?'Inspect':'Edit'} slot: ${row.displayName||row.id} (${row.id})`);
      button.append(el('span',row.displayName||row.id),el('small',`${row.startTime??'No time'}${row.endTime?'–'+row.endTime:''}${reserved?' · Reserved':''}${issues.length?' · '+issues.length+' issues':''}`));
      button.addEventListener('click',()=>{if(narrow)inspectedPeriodId=row.id;else selectedPeriodId=row.id;actions.render(snapshot);requestAnimationFrame(()=>document.querySelector('.grid-period-row legend')?.focus());});td.append(button);
    }if(!td.childNodes.length)td.textContent='—';tr.append(td);}body.append(tr);
  }
  table.append(body);scroll.append(table);wrap.append(scroll);if(!grid.periods.length)wrap.append(el('p','No slots yet. Generate a grid above.'));host.append(wrap);
}
function renderRows(host, grid, readonly, status, definition, picked) {
  const container = el('div'); container.className = 'grid-period-rows';
  // Reviews show a compact complete table; authoring and narrow inspection show one period.
  if (readonly && picked === undefined) {
    const scroll=el('div');scroll.className='definition-week-grid-wrap';const table=el('table');table.className='definition-record-table';const head=el('thead'),tr=el('tr');for(const text of ['Slot / ID','Day','Order','Start','End','Reserved']){const th=el('th',text);th.scope='col';tr.append(th);}head.append(tr);table.append(head);const body=el('tbody');
    for(const row of grid.periods){const line=el('tr');for(const value of [`${row.displayName||'(incomplete)'} · ${row.id}`,row.weekday,row.order,row.startTime??'(omitted)',row.endTime??'(omitted)',(grid.reservedPeriodIds||[]).includes(row.id)?'Yes':'No'])line.append(el('td',String(value)));body.append(line);}table.append(body);scroll.append(table);container.append(scroll);host.append(container);return;
  }
  const existing = new Set(definition.periods.map(p => p.id));
  grid.periods.forEach((row, index) => {
    if (row.id !== picked) return;
    const card = el('fieldset'); card.className = 'grid-period-row'; card.dataset.periodId = row.id;
    const legend=el('legend', `${readonly?'Inspecting':'Editing'} ${row.displayName || '(incomplete period)'} • ${row.id}`);legend.tabIndex=-1;card.append(legend);
    for (const field of ['id', 'displayName', 'weekday', 'order', 'startTime', 'endTime']) {
      const label = el('label', ({ id: 'Identifier', displayName: 'Name', weekday: 'Weekday', order: 'Order', startTime: 'Start time (optional)', endTime: 'End time (optional)' })[field]);
      if (readonly) { label.append(el('span', String(row[field] ?? '(omitted)'))); card.append(label); continue; }
      const input = el(field === 'weekday' ? 'select' : 'input', undefined, `grid-period-${index}-${field}`); input.dataset.field = field; input.name=input.id;input.autocomplete='off';input.setAttribute('aria-label', `${row.id} ${field}`); input.disabled = busy;
      if (field === 'weekday') { for (const day of [...new Set([row.weekday || '', ...weekdays])]) { const option = el('option', day ? title(day) : 'Choose weekday'); option.value = day; input.append(option); } }
      input.value = row[field] ?? ''; if (field === 'id') input.readOnly = existing.has(row.id);
      input.addEventListener('input', () => { if ((field === 'startTime' || field === 'endTime') && input.value === '') delete row[field]; else if (field === 'order') row[field] = /^\d+$/.test(input.value) && Number.isSafeInteger(Number(input.value)) ? Number(input.value) : input.value; else row[field] = input.value; if(field==='id')selectedPeriodId=row.id;edited(status); });
      label.append(input); card.append(label);
    }
    const label = el('label', 'Reserved period');
    if (readonly) label.append(el('span', (grid.reservedPeriodIds || []).includes(row.id) ? 'Yes (explicit membership)' : 'No'));
    else { const input = el('input',undefined,`grid-period-${index}-reserved`); input.type = 'checkbox'; input.dataset.field = 'reserved'; input.name=input.id;input.checked = (grid.reservedPeriodIds || []).includes(row.id); input.disabled = busy;
      input.addEventListener('change', () => { grid.reservedPeriodIds = (grid.reservedPeriodIds || []).filter(id => id !== row.id); if (input.checked) grid.reservedPeriodIds.push(row.id); edited(status); }); label.append(input); }
    card.append(label); container.append(card);
  });
  if (!grid.periods.length) container.append(el('p', 'No periods configured.'));
  host.append(container);
}
function renderImpacts(host, impacts = []) {
  if (!impacts?.length) return;
  const group=el('details');group.append(el('summary', `Teaching-time impacts (distinct from period-ID moves) — ${impacts.length} occurrences`));host.append(group);
  for (const impact of impacts) group.append(el('p', `${impact.lessonName} (${impact.lessonId}) • ${impact.periodId}: ${format(impact.before)} → ${format(impact.after)}`));
}
const format = p => `${p.weekday} order ${p.order}, ${p.startTime ?? '(no start time)'}–${p.endTime ?? '(no end time)'}`;
window.addEventListener('beforeunload', event => { if (gridDirty()) { event.preventDefault(); event.returnValue = ''; } });
