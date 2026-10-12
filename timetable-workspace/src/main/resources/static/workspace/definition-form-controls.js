import { referenceName } from './definition-browser.js';

const expansion = new Map();
const observers = [];
const normalize = value => String(value ?? '').normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase();
const el = (tag, text, className) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; if (className) node.className = className; return node; };
export function beginFormRender() {
  // The native toggle event is queued; capture the actual state before replacing the form.
  for (const details of document.querySelectorAll('.definition-settings-group[data-expansion-key]')) expansion.set(details.dataset.expansionKey, details.open);
  for (const observer of observers) observer.disconnect(); observers.length = 0;
}
export function resetFormControls() { beginFormRender(); expansion.clear(); }
export function availabilitySummary(item) {
  const count = field => !Object.hasOwn(item, field) ? field === 'availablePeriodIds' ? 'All periods (including new periods)' : 'No undesirable periods' : Array.isArray(item[field]) ? `${item[field].length} ${field === 'availablePeriodIds' ? 'available' : 'undesirable'} periods (explicit selection)` : `Invalid saved ${field} selection`;
  return count('availablePeriodIds') + (Object.hasOwn(item, 'undesirablePeriodIds') ? ` · ${count('undesirablePeriodIds')}` : '');
}

export function appendPeriodGrid(block, definition, field, prefix, label, values, disabled, onChange) {
  const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  const periods = definition.periods || [];
  const days = [...new Set(periods.map(row => String(row.weekday ?? 'Unspecified')))].sort((a,b) => weekdays.indexOf(a) - weekdays.indexOf(b));
  const orders = [...new Set(periods.map(row => row.order))].sort((a,b) => Number(a) - Number(b));
  if (!periods.length) return;
  const wrap = el('div', undefined, 'definition-week-grid-wrap'), table = el('table', undefined, 'definition-week-grid');
  const caption = el('caption', `${label} — select individual teaching slots`); table.append(caption);
  const head = el('thead'), header = el('tr'); const orderHeader = el('th', 'Slot'); orderHeader.scope = 'col'; header.append(orderHeader);
  for (const day of days) { const cell = el('th', day.slice(0,3)); cell.scope = 'col'; cell.title = day; header.append(cell); } head.append(header); table.append(head);
  const body = el('tbody');
  for (const order of orders) {
    const line = el('tr'), slot = el('th', String(order)); slot.scope = 'row'; line.append(slot);
    for (const day of days) {
      const cell = el('td');
      for (const row of periods.filter(item => String(item.weekday ?? 'Unspecified') === day && item.order === order)) {
        const reserved = definition.reservedPeriodIds?.includes(row.id), wrapper = el('label', undefined, 'definition-period-cell'), input = el('input');
        input.type = 'checkbox'; input.id = `${prefix}-${field}-period-${row.id}`; input.dataset.field = field; input.dataset.referenceId = row.id; input.checked = values.includes(row.id); input.disabled = disabled;
        const title = `${row.displayName || row.id} (${row.id}) · ${row.weekday} ${row.order}${row.startTime ? ` · ${row.startTime}–${row.endTime || '?'}` : ''}${reserved ? ' · Reserved' : ''}`;
        input.setAttribute('aria-label', `${label}: ${title}`); wrapper.title = title;
        input.addEventListener('change', () => onChange(row.id, input.checked));
        wrapper.append(input, el('span', row.startTime ? String(row.startTime).slice(0,5) : String(order), 'definition-period-time'));
        if (reserved) wrapper.append(el('span', 'R', 'definition-reserved-marker'));
        cell.append(wrapper);
      }
      if (!cell.childNodes.length) cell.append(document.createTextNode('—'));
      line.append(cell);
    }
    body.append(line);
  }
  table.append(body); wrap.append(table); block.append(wrap, el('p', 'R = Reserved period. Availability does not grant permission to use reservations.', 'definition-control-help'));
}

/** A name search chooses a stable reference; the original ID input remains available for unresolved work. */
export function addReferencePicker(input, rows, label) {
  input.removeAttribute('list');
  let originalParent = input.parentElement;
  if (originalParent.tagName === 'LABEL') {
    const container = el('div', undefined, 'definition-reference-field');
    for (const child of [...originalParent.childNodes]) if (child.nodeType !== Node.TEXT_NODE) container.append(child);
    originalParent.replaceWith(container); originalParent = container;
  }
  const picker = el('div', undefined, 'definition-reference-picker'), wrapper = el('label', label), search = el('input');
  search.type = 'search'; search.id = `${input.id}-picker`; search.name = search.id; search.autocomplete = 'off'; search.placeholder = 'Search by name or identifier…'; search.setAttribute('role', 'combobox'); search.setAttribute('aria-autocomplete', 'list'); search.setAttribute('aria-expanded', 'false');
  const list = el('div', undefined, 'definition-reference-options'); list.id = `${input.id}-options`; list.setAttribute('role', 'listbox'); list.setAttribute('aria-label', `${label} matches`); list.hidden = true; search.setAttribute('aria-controls', list.id);
  const sync = () => { const row = rows.find(item => item.id === input.value); search.value = input.disabled && input.dataset.savedMixed === 'true' ? 'Mixed saved values' : row ? row.displayName || row.id : input.value ? `Unresolved: ${input.value}` : ''; search.disabled = input.disabled; };
  const close = () => { list.hidden = true; search.setAttribute('aria-expanded', 'false'); };
  const update = () => {
    const query = normalize(search.value); const matches = rows.filter(row => normalize(`${row.displayName || ''} ${row.id}`).includes(query));
    list.replaceChildren();
    for (const row of matches.slice(0,20)) {
      const button = el('button', `${row.displayName || '(Unnamed)'} · ${row.id}`, 'secondary'); button.type = 'button'; button.setAttribute('role', 'option'); button.setAttribute('aria-selected', String(row.id === input.value));
      button.addEventListener('click', () => { input.value = row.id; input.dispatchEvent(new Event('input', { bubbles: true })); sync(); close(); search.focus(); });
      button.addEventListener('keydown', event => { const buttons = [...list.querySelectorAll('button')], index = buttons.indexOf(button); if (event.key === 'ArrowDown' || event.key === 'ArrowUp') { event.preventDefault(); buttons[Math.max(0, Math.min(buttons.length - 1, index + (event.key === 'ArrowDown' ? 1 : -1)))]?.focus(); } if (event.key === 'Escape') { close(); search.focus(); } });
      list.append(button);
    }
    list.append(el('p', matches.length ? `${Math.min(matches.length,20)} of ${matches.length} matches. Refine the search if needed.` : 'No matches. Use the identifier field for unresolved work.', 'definition-control-help'));
    list.hidden = false; search.setAttribute('aria-expanded', 'true');
  };
  search.addEventListener('input', update);
  search.addEventListener('keydown', event => { if (event.key === 'ArrowDown') { event.preventDefault(); if (list.hidden) { search.value = ''; update(); } list.querySelector('button')?.focus(); } if (event.key === 'Escape') { close(); sync(); } if (event.key === 'Enter') { event.preventDefault(); if (!list.hidden && list.querySelectorAll('button').length === 1) list.querySelector('button').click(); } });
  picker.addEventListener('focusout', event => { if (!picker.contains(event.relatedTarget)) { close(); sync(); } });
  input.addEventListener('input', sync);
  wrapper.append(search); picker.append(wrapper, list); originalParent.insertBefore(picker, input);
  const advanced = el('details', undefined, 'definition-reference-id'); advanced.append(el('summary', 'Identifier or unresolved value')); originalParent.insertBefore(advanced, input); advanced.append(input);
  input.setAttribute('aria-label', `${label} identifier`); input.name = input.id; input.autocomplete = 'off';
  const mode = originalParent.querySelector('select'); mode?.addEventListener('change', sync);
  sync(); return picker;
}

function topChild(form, control) { let node = control; while (node && node.parentElement !== form) node = node.parentElement; return node; }
function reveal(control) { for (let parent = control.parentElement; parent; parent = parent.parentElement) if (parent.tagName === 'DETAILS') parent.open = true; control.scrollIntoView({ block: 'center' }); control.focus(); }
export function focusDefinitionField(prefix, field) { const input = document.getElementById(`${prefix}-${field}`), mode = document.getElementById(`${prefix}-${field}-mode`); const control = input && !input.disabled ? input : mode || input; if (control) reveal(control); }

export function enhanceDefinitionForm(form, options) {
  const { prefix, ownerType, id, item, draft, status, groups = [], referenceFields = [], relatedLessonIds = [], fieldAliases = {} } = options;
  for (const [field, collection, label] of referenceFields) { const input = form.querySelector(`#${prefix}-${field}`); if (input) addReferencePicker(input, draft.definition[collection] || [], label); }
  for (const input of form.querySelectorAll('input, select')) { input.name ||= input.id || `${prefix}-${input.dataset.field || 'choice'}-${input.dataset.referenceId || 'search'}`; if (input.tagName === 'INPUT') input.autocomplete = 'off'; }
  const issues = (draft.issues || []).filter(issue => (issue.ownerType === ownerType && issue.ownerId === id) || issue.relatedIds?.includes(id) || (issue.ownerType === 'Lesson' && relatedLessonIds.includes(issue.ownerId)));
  for (const group of groups) {
    const nodes = [...new Set(group.fields.flatMap(field => { const control = form.querySelector(`#${prefix}-${field}`) || form.querySelector(`#${prefix}-${field}-mode`); return control ? [topChild(form, control)] : []; }))];
    if (!nodes.length) continue;
    const details = el('details', undefined, 'definition-settings-group'); details.id = `${prefix}-${group.key}`;
    const summary = el('summary'), title = el('span', group.title), description = el('span', undefined, 'definition-settings-summary'); summary.append(title, description); details.append(summary);
    const content = el('div', undefined, 'definition-settings-content'); details.append(content);
    const key = `${prefix}:${id || 'new'}:${group.key}`;
    details.dataset.expansionKey = key;
    const containsIssue = issues.some(issue => group.fields.includes(issue.field?.split(/[.\[]/)[0]));
    details.open = containsIssue || !!group.open || !!expansion.get(key);
    const describe = () => { description.textContent = group.describe ? group.describe(item) : group.fields.filter(field => Object.hasOwn(item, field)).map(field => `${group.labels?.[field] || field}: ${Array.isArray(item[field]) ? `${item[field].length} selected` : JSON.stringify(item[field])}`).join(' · ') || 'Catalog defaults'; };
    describe(); details.addEventListener('toggle', () => { if (details.isConnected) expansion.set(key, details.open); describe(); }); form.addEventListener('input', describe); form.addEventListener('change', describe);
    form.insertBefore(details, nodes[0]); for (const node of nodes) content.append(node);
  }
  if (issues.length) {
    const summary = el('section', undefined, 'definition-editor-issues'); summary.setAttribute('aria-label', 'Saved issues for this record'); summary.append(el('h5', 'Saved issues for this record'));
    for (const [index, issue] of issues.entries()) {
      const field = issue.field?.split(/[.\[]/)[0], controlField = fieldAliases[field] || field, control = form.querySelector(`#${prefix}-${controlField}`) || form.querySelector(`#${prefix}-${controlField}-mode`);
      const text = `${issue.severity}: ${issue.ownerType} ${issue.ownerId} · ${issue.field}: ${issue.message}`;
      if (control) {
        const link = el('a', text); link.href = `#${control.id}`; link.addEventListener('click', event => { event.preventDefault(); reveal(control); }); summary.append(link);
        const message = el('p', `Saved issue: ${issue.message}`, issue.severity === 'BLOCKING' ? 'error definition-field-issue' : 'notice definition-field-issue'); message.id = `${prefix}-field-issue-${index}`;
        control.setAttribute('aria-describedby', [control.getAttribute('aria-describedby'), message.id].filter(Boolean).join(' ')); if (issue.severity === 'BLOCKING') control.setAttribute('aria-invalid', 'true');
        control.parentElement.append(message); for (let parent = control.parentElement; parent && parent !== form; parent = parent.parentElement) if (parent.tagName === 'DETAILS') parent.open = true;
      } else summary.append(el('p', text));
    }
    form.prepend(summary);
  }
  const actions = form.querySelector(':scope > .actions');
  if (actions) {
    actions.classList.add('definition-editor-actions'); const localStatus = el('p', status.textContent, 'definition-editor-status'); localStatus.id = `${prefix}-local-status`; localStatus.setAttribute('aria-live', 'off'); actions.prepend(localStatus);
    const observer = new MutationObserver(() => { localStatus.textContent = status.textContent; }); observer.observe(status, { childList: true, characterData: true, subtree: true }); observers.push(observer); form.append(actions);
    form.addEventListener('focusin', event => requestAnimationFrame(() => {
      if (!event.target.isConnected || actions.contains(event.target) || !event.target.getBoundingClientRect) return;
      const target = event.target.getBoundingClientRect(), footer = actions.getBoundingClientRect();
      if (target.bottom > footer.top && target.top < footer.bottom) event.target.scrollIntoView({ block: 'center' });
    }));
  }
}

export function makeChecklistSearchable(block, label) {
  const choices = [...block.querySelectorAll(':scope > label')].filter(node => node.querySelector('input[type=checkbox]'));
  if (!choices.length) return;
  const wrapper = el('label', `Search ${label.toLowerCase()}`), search = el('input'); search.type = 'search'; search.autocomplete = 'off'; search.placeholder = 'Name or identifier…'; wrapper.append(search);
  const list = el('div', undefined, 'definition-checklist');
  block.insertBefore(wrapper, choices[0]); wrapper.after(list);
  for (const choice of choices) list.append(choice);
  const status = el('p', undefined, 'definition-control-help'); status.setAttribute('role', 'status'); list.after(status);
  const update = () => { const query = normalize(search.value); for (const choice of choices) choice.hidden = !normalize(choice.textContent).includes(query); status.textContent = `${choices.filter(node => !node.hidden).length} of ${choices.length} choices. Selected values stay selected when filtered.`; };
  search.addEventListener('input', update); update();
}
