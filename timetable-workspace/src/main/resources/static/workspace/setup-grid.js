const copy = value => structuredClone(value);
const identity = () => crypto.randomUUID();
export const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
export const intervalKinds = ['Lesson', 'Reserved lesson', 'Break', 'Lunch'];
export const teaching = interval => ['Lesson', 'Reserved lesson'].includes(interval.kind);
export function gridIssues(draft) {
  const issues = [], grid = draft.grid;
  const add = (message, record) => issues.push({ section: 'Timetable grid', record, message });
  if (!grid.activeDays.length) add('Choose at least one active weekday.');
  if (!grid.intervals.some(teaching)) add('Add at least one teaching period.');
  for (const day of grid.activeDays) {
    let previousEnd = -1;
    for (const row of grid.intervals.filter(i => i.day === day)) {
      if(row.unspecifiedTimes&&!row.start&&!row.end)continue;
      const time = v => /^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(v ?? '') ? Number(v.slice(0, 2)) * 60 + Number(v.slice(3,5)) + Number(v.slice(6)||0)/60 : NaN;
      const start = time(row.start), end = time(row.end);
      if (!(row.label ?? '').trim()) add(`${day}: enter an interval name.`, row.id);
      if (!intervalKinds.includes(row.kind)) add(`${day}: choose an interval kind.`, row.id);
      if (!Number.isFinite(start) || !Number.isFinite(end)) add(`${day} ${row.label || 'interval'}: enter both times.`, row.id);
      else {
        if (end <= start) add(`${day} ${row.label || 'interval'}: end must be after start.`, row.id);
        if (start < previousEnd) add(`${day} ${row.label || 'interval'}: times overlap or are out of order.`, row.id);
        previousEnd = end;
      }
    }
  }
  for (const row of grid.intervals) if (!grid.activeDays.includes(row.day) || !weekdays.includes(row.day)) add(`Interval ${row.label || row.id} has an inactive or invalid weekday.`, row.id);
  return issues;
}
const dayName = day => day.slice(0,1) + day.slice(1).toLowerCase();
export function gridContent(draft, html) {
  const day = draft.grid.activeDays.includes(draft.pendingInput?.gridDay) ? draft.pendingInput.gridDay : draft.grid.activeDays[0] ?? 'MONDAY';
  const rows=draft.grid.intervals.filter(i=>i.day===day);
  return `<details id="setup-school-days" ${!draft.grid.activeDays.length?'open':''}><summary>School days · ${draft.grid.activeDays.length} selected</summary><fieldset class="setup-active-days"><legend>School days</legend><div class="setup-actions">${weekdays.map(d=>`<label><input type="checkbox" data-active-day="${d}" ${draft.grid.activeDays.includes(d)?'checked':''}>${dayName(d).slice(0,3)}</label>`).join('')}</div></fieldset></details>
    <nav class="setup-day-tabs" aria-label="Edit weekday">${(draft.grid.activeDays.length?draft.grid.activeDays:weekdays).map(d=>`<button data-grid-day="${d}" ${day===d?'aria-current="true"':''}>${dayName(d).slice(0,3)}</button>`).join('')}</nav>
    <div class="setup-pattern-layout"><div class="setup-day-pattern">
    <div class="setup-interval-head" aria-hidden="true"><span>Interval</span><span>Starts</span><span>Ends</span><span></span></div>
    <div class="setup-intervals">${rows.map((i,index)=>`<div class="setup-interval" data-kind="${html(i.kind)}" data-interval="${html(i.id)}">
      <label><span class="setup-mobile-label">Interval</span><select aria-label="Interval ${index+1} kind" data-interval-kind>${intervalKinds.map(k=>`<option ${i.kind===k?'selected':''}>${k}</option>`).join('')}</select></label>
      <label><span class="setup-mobile-label">Starts</span><input aria-label="Interval ${index+1} start" type="text" inputmode="numeric" placeholder="HH:MM" data-interval-field="start" value="${html(i.start)}"></label>
      <label><span class="setup-mobile-label">Ends</span><input aria-label="Interval ${index+1} end" type="text" inputmode="numeric" placeholder="HH:MM" data-interval-field="end" value="${html(i.end)}"></label>
      <div class="setup-interval-actions"><button data-remove-interval aria-label="Remove interval ${index+1}" title="Remove interval">×</button></div>
      ${i.boundaryAfter?'<p class="setup-interval-note">Retained boundary: consecutive lessons stop here. Change it through advanced JSON.</p>':''}${i.unspecifiedTimes&&!i.start&&!i.end?'<p class="setup-interval-note">Imported times unspecified; order is retained.</p>':''}</div>`).join('')||'<div class="setup-empty-pattern"><strong>Start with your first interval</strong><p>Add lessons, breaks, lunch or reserved teaching time.</p></div>'}</div>
    <button id="setup-add-interval" class="secondary" ${!draft.grid.activeDays.includes(day)?'disabled':''}>+ Add interval</button>
    ${!draft.grid.activeDays.includes(day)?'<p>Choose this weekday under School days to add intervals.</p>':''}
    <fieldset class="setup-copy-day"><legend>Copy ${dayName(day)} to…</legend><div class="setup-actions">${weekdays.filter(d=>d!==day&&(draft.grid.activeDays.includes(d)||weekdays.indexOf(d)<5)).map(d=>`<label><input type="checkbox" data-copy-day="${d}">${dayName(d).slice(0,3)}</label>`).join('')}</div><button id="setup-copy-day" class="secondary">Copy day pattern…</button></fieldset>
    ${rows.length?`<details id="setup-interval-options"><summary>Interval names &amp; order</summary>${rows.map((i,index)=>`<div class="setup-interval-option" data-interval="${html(i.id)}"><label>Interval ${index+1} name<input data-interval-field="label" value="${html(i.label)}"></label><button data-move-interval="-1" aria-label="Move interval ${index+1} earlier">↑</button><button data-move-interval="1" aria-label="Move interval ${index+1} later">↓</button></div>`).join('')}</details>`:''}
    <div id="setup-grid-issues" role="status">${gridIssues(draft).map(i=>`<p>${html(i.message)}</p>`).join('')}</div></div>
    <aside class="setup-week-overview" aria-label="Week overview"><h3>Week overview</h3>${weekdays.filter(d=>draft.grid.activeDays.includes(d)).map(d=>{
      const intervals=draft.grid.intervals.filter(i=>i.day===d),regular=intervals.filter(i=>i.kind==='Lesson').length,reserved=intervals.filter(i=>i.kind==='Reserved lesson').length;
      return `<button data-overview-day="${d}" ${day===d?'aria-pressed="true"':''}><strong>${dayName(d).slice(0,3)}</strong><span class="setup-preview-bars" aria-hidden="true">${intervals.map(i=>`<i data-kind="${html(i.kind)}"></i>`).join('')}</span><small>${regular} lessons${reserved?` · ${reserved} reserved`:''}${intervals.length?'':' · Empty'}</small></button>`;
    }).join('')||'<p>Select school days to see the week here.</p>'}<details><summary>About intervals</summary><p class="setup-grid-key"><span>Lesson</span><span>Reserved lesson</span><span>Break / Lunch</span></p><p>Breaks keep lessons adjacent. Lunch interrupts consecutive lessons.</p></details></aside></div>`;
}
// Enumerate actual authoring references; grid identity definitions are excluded. Exact property paths remain visible.
export function periodReferences(draft, removed) {
  const references = [];
  function visit(value, path) {
    if (!value || typeof value !== 'object') return;
    for (const [key, child] of Object.entries(value)) {
      if (path.length === 0 && ['grid', 'retained', 'source', 'check'].includes(key)) continue;
      const next = [...path, key];
      if (/period(id|ids|lock)$/i.test(key)) {
        if (typeof child === 'string' && removed.has(child)) references.push({ path: next, periodId: child });
        if (Array.isArray(child)) child.forEach((id, index) => { if (removed.has(id)) references.push({ path: [...next, index], periodId: id }); });
      } else visit(child, next);
    }
  }
  visit(draft, []); return references;
}
export function applyReferenceDecisions(draft, references, decisions) {
  const next = copy(draft), removals = new Map();
  const periods = new Set(draft.grid.intervals.filter(teaching).map(i => i.periodId));
  references.forEach((ref, index) => {
    const target = decisions[index];
    if (target === undefined || target === '') throw new Error('Every affected reference needs an explicit decision.');
    if (target !== '__clear__' && !periods.has(target)) throw new Error('Choose a surviving teaching period for every remap.');
    const owner = ref.path.slice(0, -1).reduce((node, key) => node[key], next), key = ref.path.at(-1);
    if (target === '__clear__') { if (!removals.has(owner)) removals.set(owner, []); removals.get(owner).push(key); }
    else owner[key] = target;
  });
  // Descending array indices avoid shifting references not yet handled.
  for (const [owner, keys] of removals) {
    keys.sort((a,b) => Number(b)-Number(a)).forEach(key => { if (Array.isArray(owner)) owner.splice(key, 1); else delete owner[key]; });
  }
  return next;
}
export function createGridActions({ getDraft, commit, contextOnly, notify, html }) {
  function replace(next, description) {
    const draft = getDraft(), surviving = new Set(next.grid.intervals.filter(teaching).map(i => i.periodId));
    const removed = new Set(draft.grid.intervals.filter(teaching).map(i => i.periodId).filter(id => !surviving.has(id)));
    const refs = periodReferences(draft, removed), box = document.createElement('dialog');
    box.className = 'setup-confirm'; box.setAttribute('aria-label', 'Review grid replacement');
    const options = next.grid.intervals.filter(teaching).map(i => `<option value="${html(i.periodId)}">${html(i.day)} ${html(i.label || i.periodId)} ${html(i.start)}</option>`).join('');
    box.innerHTML = `<h2>Review grid replacement</h2><p>${html(description)}</p><p>${removed.size} teaching periods removed. ${refs.length} affected references. Availability markings are not copied. Surviving interval and period identities are retained.</p>
      ${refs.map((r, index) => `<label>${html(r.path.join('.'))}: ${html(r.periodId)}<select data-reference="${index}"><option value="">Choose a decision…</option><option value="__clear__">Clear this reference</option>${options}</select></label>`).join('')}
      <p class="setup-remap-error" role="alert"></p><div class="setup-actions"><button data-cancel autofocus>Cancel</button><button data-apply>Apply grid change</button></div>`;
    document.body.append(box); box.showModal();
    box.querySelector('[data-cancel]').onclick = () => box.close();
    box.querySelector('[data-apply]').onclick = () => {
      try {
        const decisions = [...box.querySelectorAll('[data-reference]')].map(n => n.value);
        const resolved = applyReferenceDecisions(next, refs, decisions);
        box.close(); commit(resolved); notify('Grid changed. Previous readiness evidence is invalidated; block requirements and lesson placements were not rewritten.');
      } catch (error) { box.querySelector('.setup-remap-error').textContent = error.message; }
    };
    box.onclose = () => box.remove();
  }
  return {
    input(event) {
      const field = event.target.dataset.intervalField; if (!field) return false;
      const draft = getDraft(), id = event.target.closest('[data-interval]').dataset.interval;
      const row = draft.grid.intervals.find(i => i.id === id);
      row[field] = event.target.value;
      if(field==='label')delete row.autoLabel;
      commit(draft, false);
      document.querySelector('#setup-grid-issues').innerHTML = gridIssues(draft).map(i => `<p>${html(i.message)}</p>`).join('');
      return true;
    },
    change(event) {
      const draft = getDraft(), next = copy(draft), day = draft.grid.activeDays.includes(draft.pendingInput?.gridDay) ? draft.pendingInput.gridDay : draft.grid.activeDays[0] ?? 'MONDAY';
      if (event.target.dataset.activeDay) {
        const target = event.target.dataset.activeDay;
        if (event.target.checked) { if (!next.grid.activeDays.includes(target)) next.grid.activeDays.push(target); next.grid.activeDays.sort((a,b) => weekdays.indexOf(a)-weekdays.indexOf(b)); commit(next); }
        else { next.grid.activeDays = next.grid.activeDays.filter(d => d !== target); next.grid.intervals = next.grid.intervals.filter(i => i.day !== target); event.target.checked = true; replace(next, `Deactivate ${target} and remove its intervals?`); }
        return true;
      }
      if (event.target.hasAttribute('data-interval-kind')) {
        const id = event.target.closest('[data-interval]').dataset.interval, row = next.grid.intervals.find(i => i.id === id), previous = copy(row);
        row.kind = event.target.value;
        if(row.autoLabel)row.label = `${row.kind} ${next.grid.intervals.filter(i=>i.day===day&&i.kind===row.kind).indexOf(row)+1}`;
        if (teaching(row) && !row.periodId) row.periodId = identity();
        if (!teaching(row)) delete row.periodId;
        if (teaching(previous) && !teaching(row)) { event.target.value = previous.kind; replace(next, `Change ${previous.label || 'this teaching period'} to ${row.kind}?`); }
        else commit(next);
        return true;
      }
      return false;
    },
    click(event) {
      const button = event.target.closest('button'); if (!button) return false;
      const draft = getDraft(), next = copy(draft), day = draft.grid.activeDays.includes(draft.pendingInput?.gridDay) ? draft.pendingInput.gridDay : draft.grid.activeDays[0] ?? 'MONDAY';
      if (button.dataset.gridDay || button.dataset.overviewDay) { next.pendingInput ??= {}; next.pendingInput.gridDay = button.dataset.gridDay || button.dataset.overviewDay; contextOnly(next); return true; }
      if (button.id === 'setup-add-interval') {
        if (!next.grid.activeDays.includes(day)) return true;
        next.grid.intervals.push({ id: identity(), periodId: identity(), day, kind: 'Lesson', autoLabel: true, label: `Lesson ${next.grid.intervals.filter(i=>i.day===day&&i.kind==='Lesson').length+1}`, start: '', end: '' }); commit(next); return true;
      }
      const interval = button.closest('[data-interval]');
      if (interval && (button.hasAttribute('data-remove-interval') || button.dataset.moveInterval)) {
        const id = interval.dataset.interval;
        if (button.hasAttribute('data-remove-interval')) { next.grid.intervals = next.grid.intervals.filter(i => i.id !== id); replace(next, 'Remove this interval and explicitly resolve its references?'); }
        else {
          const index = next.grid.intervals.findIndex(i => i.id === id), rows = next.grid.intervals.map((i,n) => ({ i,n })).filter(x => x.i.day === day);
          const position = rows.findIndex(x => x.i.id === id), target = rows[position + Number(button.dataset.moveInterval)];
          if (target) { [next.grid.intervals[index], next.grid.intervals[target.n]] = [next.grid.intervals[target.n], next.grid.intervals[index]]; commit(next); }
        }
        return true;
      }
      if (button.id === 'setup-copy-day') {
        const destinations = [...document.querySelectorAll('[data-copy-day]:checked')].map(n => n.dataset.copyDay);
        if (!destinations.length) { notify('Select at least one destination weekday. No intervals changed.'); return true; }
        const source = draft.grid.intervals.filter(i => i.day === day);
        next.grid.intervals = next.grid.intervals.filter(i => !destinations.includes(i.day));
        for (const target of destinations) {
          if (!next.grid.activeDays.includes(target)) next.grid.activeDays.push(target);
          next.grid.intervals.push(...source.map(i => ({ ...copy(i), id: identity(), ...(teaching(i) ? { periodId: identity() } : {}), day: target })));
        }
        next.grid.activeDays.sort((a,b) => weekdays.indexOf(a)-weekdays.indexOf(b));
        replace(next, `Copy ${source.length} intervals from ${day} to ${destinations.join(', ')} and replace all their existing intervals?`); return true;
      }
      return false;
    }
  };
}
