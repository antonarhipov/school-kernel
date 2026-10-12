const states = new Map();
const pageSize = 25;
const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });
const normalize = value => String(value ?? '').normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase();
const el = (tag, text, className) => {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
};
export function resetBrowsers() { states.clear(); }
export const referenceName = (definition, collection, id) => definition[collection]?.find(row => row.id === id)?.displayName || id || 'Not set';

function entityRows(draft, collection, ownerType) {
  const definition = draft.definition;
  const name = (kind, id) => referenceName(definition, kind, id);
  return (definition[collection] || []).map(item => {
    let summary = '', filterValues = [];
    if (collection === 'subjects') {
      const count = Object.keys(item).filter(key => !['id', 'displayName'].includes(key)).length;
      summary = count ? `${count} configured constraints` : 'Catalog defaults';
    } else if (collection === 'teachers') {
      filterValues = Array.isArray(item.qualifiedSubjectIds) ? item.qualifiedSubjectIds : [];
      summary = `${filterValues.map(id => name('subjects', id)).join(', ') || 'No qualified subjects'} · ${definition.lessons.filter(row => row.teacherId === item.id).length} lessons/week`;
    } else if (collection === 'cohorts') {
      summary = `${item.size ?? 'Unknown'} students · Curator: ${name('teachers', item.curatorTeacherId)} · Home room: ${name('rooms', item.homeRoomId)}`;
    } else if (collection === 'rooms') {
      filterValues = Array.isArray(item.capabilityIds) ? item.capabilityIds.filter(value => typeof value === 'string') : [];
      summary = `Capacity: ${item.capacity ?? 'Not set'} · ${filterValues.join(', ') || 'No capabilities'}`;
    }
    const issues = (draft.issues || []).filter(issue => (issue.ownerType === ownerType && issue.ownerId === item.id) || issue.relatedIds?.includes(item.id));
    return { id: item.id, name: item.displayName || '(Unnamed)', summary, filterValues, issues };
  });
}

/** Browsing updates only its own DOM, never the working form or durable workspace. */
export function renderEntityBrowser(host, snapshot, options) {
  const { collection, ownerType, singular, plural, selectedId, narrow, busy, onSelect, onAdd } = options;
  const draft = snapshot.workspace.definitionDraft;
  const rows = entityRows(draft, collection, ownerType);
  const filters = collection === 'teachers' ? [{ key: 'subject', label: 'Qualified subject', choices: draft.definition.subjects.map(row => [row.id, row.displayName || row.id]), matches: (row, value) => row.filterValues.includes(value) }]
    : collection === 'rooms' ? [{ key: 'capability', label: 'Capability', choices: [...new Set(rows.flatMap(row => row.filterValues))].sort(collator.compare).map(value => [value, value]), matches: (row, value) => row.filterValues.includes(value) }] : [];
  return renderBrowser(host, { key: collection, rows, singular, plural, selectedId, narrow, busy, filters, onSelect, onAdd });
}

export function renderBrowser(host, options) {
  const { key, rows, singular, plural, selectedId, narrow, busy, filters = [], onSelect, onAdd, columns, addLabel, addId, selection } = options;
  const state = states.get(key) || { query: '', issuesOnly: false, filters: {}, page: 0, scroll: 0, inspectionId: null };
  states.set(key, state);
  const inspectionId = rows.some(row => row.id === state.inspectionId) ? state.inspectionId : selectedId || rows[0]?.id;
  const currentId = narrow ? inspectionId : selectedId;
  const layout = el('div', undefined, `definition-browser-layout${columns ? ' definition-browser-table-layout' : ''}`);
  const browser = el('section', undefined, 'definition-browser'); browser.setAttribute('aria-label', `${plural} browser`);
  const toolbar = el('div', undefined, 'definition-browser-toolbar');
  const searchLabel = el('label', `Search ${plural.toLowerCase()}`);
  const search = el('input'); search.type = 'search'; search.id = `${key}-search`; search.name = search.id; search.autocomplete = 'off'; search.value = state.query; search.placeholder = 'Name or identifier…'; searchLabel.append(search); toolbar.append(searchLabel);
  for (const filter of filters) {
    const label = el('label', filter.label), select = el('select'); select.id = `${key}-filter-${filter.key}`; select.name = select.id;
    for (const [value, text] of [['', 'All'], ...filter.choices]) { const option = el('option', text); option.value = value; select.append(option); }
    select.value = state.filters[filter.key] || '';
    select.addEventListener('change', () => { state.filters[filter.key] = select.value; state.page = 0; state.scroll = 0; update(); }); label.append(select); toolbar.append(label);
  }
  const issueLabel = el('label', undefined, 'definition-inline-check'), issueCheckbox = el('input'); issueCheckbox.type = 'checkbox'; issueCheckbox.checked = state.issuesOnly; issueCheckbox.id = `${key}-issues-only`;
  issueLabel.append(issueCheckbox, document.createTextNode('With issues')); toolbar.append(issueLabel);
  issueCheckbox.addEventListener('change', () => { state.issuesOnly = issueCheckbox.checked; state.page = 0; state.scroll = 0; update(); });
  if (!narrow && onAdd) { const add = el('button', addLabel || `Add ${singular.toLowerCase()}`); add.id = addId || `add-${key === 'cohorts' ? 'class' : key === 'requirements' ? 'requirement' : singular.toLowerCase()}`; add.type = 'button'; add.disabled = busy; add.addEventListener('click', onAdd); toolbar.append(add); }
  const scope = selection ? el('p', undefined, 'definition-selection-scope') : null;
  const count = el('p', undefined, 'definition-browser-count'); count.setAttribute('role', 'status');
  const results = el('div', undefined, 'definition-browser-results'); results.addEventListener('scroll', () => { state.scroll = results.scrollTop; });
  const pager = el('div', undefined, 'definition-browser-pager');
  browser.append(toolbar, count); if (scope) { scope.setAttribute('role', 'status'); browser.append(scope); } browser.append(results, pager);
  const detail = el('section', undefined, 'definition-browser-detail'); detail.setAttribute('aria-label', `${singular} editor`);
  const selected = rows.find(row => row.id === currentId);
  const heading = el('h4', selected ? `${narrow ? 'Inspecting' : 'Editing'} ${singular.toLowerCase()}: ${selected.title || selected.name}` : narrow ? `No saved ${plural.toLowerCase()}` : `New ${singular.toLowerCase()}`);
  heading.tabIndex = -1; heading.className = 'definition-editor-title'; detail.append(heading);
  if (selected) detail.append(el('p', selected.id, 'definition-record-id'));
  layout.append(browser, detail); host.append(layout);
  function update() {
    const query = normalize(state.query.trim());
    const filtered = rows.filter(row => (!query || normalize([row.name, row.id, row.summary].join(' ')).includes(query)) && (!state.issuesOnly || row.issues.length) && filters.every(filter => !state.filters[filter.key] || filter.matches(row, state.filters[filter.key]))).sort((a, b) => collator.compare(a.group || '', b.group || '') || collator.compare(a.name, b.name) || collator.compare(a.id, b.id));
    const pages = Math.max(1, Math.ceil(filtered.length / pageSize)); state.page = Math.min(state.page, pages - 1);
    const visible = filtered.slice(state.page * pageSize, (state.page + 1) * pageSize);
    count.textContent = `${filtered.length} of ${rows.length} ${plural.toLowerCase()}${selected && !filtered.some(row => row.id === currentId) ? ' · Open record is outside these results' : ''}`;
    if (scope) { const inResults = selection.ids.filter(id => filtered.some(row => row.id === id)).length, onPage = selection.ids.filter(id => visible.some(row => row.id === id)).length; scope.textContent = `${selection.ids.length} selected · ${selection.ids.length - inResults} outside filters · ${selection.ids.length - onPage} outside this page. Filtering never changes the selected population.`; }
    results.replaceChildren(); pager.replaceChildren();
    if (!visible.length) results.append(el('p', rows.length ? 'No matches. Change the search or filters.' : `No saved ${plural.toLowerCase()}.`));
    const list = columns ? el('table', undefined, 'definition-record-table') : el('ul', undefined, 'definition-record-list');
    let body = list;
    if (columns) { const head = el('thead'), row = el('tr'); for (const column of columns) { const cell = el('th', column.label); cell.scope = 'col'; row.append(cell); } head.append(row); body = el('tbody'); list.append(head, body); }
    const buttons = []; let previousGroup;
    for (const [index, row] of visible.entries()) {
      if (!columns && row.group && row.group !== previousGroup) { const group=el('li',undefined,'definition-record-group');group.append(el('h5',row.group));body.append(group);previousGroup=row.group; }
      const container = el(columns ? 'tr' : 'li'); if (row.id === currentId) container.className = 'selected';
      const button = el('button', undefined, 'definition-record-choice'); button.type = 'button'; button.disabled = busy;
      button.dataset[`${key === 'cohorts' ? 'class' : singular.toLowerCase()}Id`] = row.id;
      button.setAttribute('aria-label', `Edit ${singular.toLowerCase()}: ${row.name} (${row.id})`); button.setAttribute('aria-pressed', String(row.id === currentId));
      button.tabIndex = row.id === currentId || (!visible.some(item => item.id === currentId) && index === 0) ? 0 : -1;
      button.append(el('span', row.name, 'definition-record-name'), el('span', row.id, 'definition-record-id'));
      button.addEventListener('click', () => { if (row.id === currentId && (!selection || selection.ids.length === 1)) return; if (narrow) state.inspectionId = row.id; onSelect(row.id); });
      button.addEventListener('keydown', event => {
        const next = event.key === 'ArrowDown' ? Math.min(index + 1, buttons.length - 1) : event.key === 'ArrowUp' ? Math.max(index - 1, 0) : event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1 : -1;
        if (next >= 0) { event.preventDefault(); for (const choice of buttons) choice.tabIndex = -1; buttons[next].tabIndex = 0; buttons[next].focus(); }
      }); buttons.push(button);
      const checkbox = selection && !narrow ? el('input') : null;
      if (checkbox) { checkbox.type = 'checkbox'; checkbox.checked = selection.ids.includes(row.id); checkbox.disabled = busy; checkbox.setAttribute('aria-label', `Select occurrence: ${row.name} (${row.id})`); checkbox.addEventListener('change', () => { const checked = checkbox.checked; checkbox.checked = selection.ids.includes(row.id); selection.onToggle(row.id, checked); }); }
      if (columns) { for (const column of columns) { const cell = el('td'); if (column.key === 'name') { if (checkbox) cell.append(checkbox); cell.append(button); } else cell.textContent = column.value(row); container.append(cell); } }
      else { button.append(el('span', row.summary, 'definition-record-summary')); if (row.issues.length) button.append(el('span', `${row.issues.filter(issue => issue.severity === 'BLOCKING').length} blocking · ${row.issues.filter(issue => issue.severity !== 'BLOCKING').length} advisories`, 'definition-issue-badge')); if (checkbox) container.append(checkbox); container.append(button); }
      body.append(container);
    }
    if (visible.length) results.append(list);
    for (const [label, delta] of [['Previous', -1], ['Next', 1]]) { const button = el('button', label, 'secondary'); button.type = 'button'; button.disabled = delta < 0 ? state.page === 0 : state.page === pages - 1; button.addEventListener('click', () => { state.page += delta; state.scroll = 0; update(); results.querySelector('button')?.focus(); }); pager.append(button); }
    pager.insertBefore(el('span', `Page ${state.page + 1} of ${pages}`), pager.lastChild);
    results.scrollTop = state.scroll;
  }
  search.addEventListener('input', () => { state.query = search.value; state.page = 0; state.scroll = 0; update(); });
  search.addEventListener('keydown', event => { if (event.key === 'ArrowDown') { event.preventDefault(); results.querySelector('button')?.focus(); } });
  update();
  return { detail, selectedId: currentId };
}
