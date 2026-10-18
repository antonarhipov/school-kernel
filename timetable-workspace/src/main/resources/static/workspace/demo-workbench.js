import { M } from './messages.js';
import { resolvePdfScope } from './pdf-scope.js';
import { makeAcceptedModel } from './accepted-model.js';
import { renderWeekMatrix } from './week-renderer.js';
import { renderDayMatrix } from './day-renderer.js';
import { renderMobileMatrix } from './mobile-matrix.js';
import { createTimetableStorage, deleteLocalTimetableData, draftKey, isTimetable, isSchoolDefinition } from './timetable-storage.js';
import { demoRequest } from './demo-transport.js';
import { conflictItems, warningItems } from './demo-feedback.js';
import { definitionRevision } from './setup-compile.js';
import { createSetupEditor } from './setup-editor.js';

const html = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const clone = value => structuredClone(value);
const blankFilters = () => ({ cohortId: '', teacherId: '', roomId: '', subjectId: '', periodId: '', search: '', range: 'WEEK', day: '' });
const mobileLayout = window.matchMedia('(max-width: 700px)');
let mounted = false, repository, browserStorage, deletingLocalData = false, build, schools = new Map(), selectedSchool, selectedTimetable, model, selectedLesson = null;
let showSavedDraft = false;
let filters = blankFilters(), notice = '';
const reviews = new Map(), timers = new Map();
const fields = ['teacherId', 'roomId', 'periodId'];
let activeGeneration = null, config = { lessonThreshold: 200, quickSeconds: 5, thoroughSeconds: 60 };
let setupEditor;
let pdfBusy = false;
const canonical = value => Array.isArray(value) ? value.map(canonical) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])])) : value;

export async function mountTimetableDemo() {
  if (mounted) return;
  mounted = true;
  document.title = 'School timetables';
  document.body.classList.add('timetable-demo');
  document.body.innerHTML = `<header class="demo-header">
    <div class="demo-brand"><span class="eyebrow">School Kernel</span><span class="demo-tag">Interactive demo</span></div>
    <div class="demo-heading-row"><div class="demo-selectors">
      <label>School<select id="school-selector" aria-label="School"><option>Loading schools…</option></select></label>
      <label>Timetable<select id="timetable-selector" aria-label="Timetable" disabled><option>Loading timetables…</option></select></label>
    </div></div>
  </header>
  <main class="demo-main">
    <p id="demo-notice" class="notice" role="status" hidden></p>
    <p id="generation-indicator" class="notice" role="status" hidden></p>
    <section class="demo-timetable current-mode compact-density" aria-label="Timetable">
      <button id="school-setup-entry" class="school-setup-entry" type="button" disabled>School setup</button>
      <div class="demo-view-heading"><div><h1 id="timetable-title">Timetable</h1><p id="timetable-description" class="muted">Loading verified examples…</p></div><button id="resume-timetable-draft" class="secondary" type="button" hidden>Resume saved changes</button>
        <button id="export-timetable-pdf" class="secondary pdf-export-button" type="button" disabled aria-label="Export to PDF" title="Export to PDF">
          <svg viewBox="0 0 24 28" aria-hidden="true" focusable="false"><path d="M14 1.5H4a2 2 0 0 0-2 2v21a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-15Z" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M14 1.5v8h8" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><text x="12" y="21" text-anchor="middle" fill="currentColor" font-family="sans-serif" font-size="7" font-weight="700">PDF</text></svg>
        </button>
      </div>
      <p id="pdf-export-status" class="muted" role="status" hidden></p>
      <div id="timetable-filters"></div>
      <div id="draft-feedback" hidden></div>
      <div id="timetable-matrix" aria-busy="true"></div>
    </section>
    <footer class="demo-footer">Fictional staff and schools · Your saved timetables and drafts stay in this browser on this device.
      <details><summary>About storage</summary><p>Browser data survives demo restarts and deployments. Clearing this site’s browser data removes saved schools, timetables and drafts. Generation and validation use a temporary private server session. Server sessions and temporary files expire; complete identical inputs and checked results may remain in a bounded shared memory cache for up to an hour. No account or cross-device recovery is provided.</p></details>
    </footer>
  </main>
  <dialog id="lesson-inspector" class="demo-inspector" aria-labelledby="inspector-title"></dialog>
  <dialog id="delete-local-data-dialog" class="demo-delete-dialog" aria-labelledby="delete-local-data-title" aria-describedby="delete-local-data-description">
    <h2 id="delete-local-data-title">Delete local schools and timetables?</h2>
    <p id="delete-local-data-description">This deletes this browser’s local school definitions, timetables, candidates, setup/manual/JSON drafts, authoring snapshots, recovery metadata and artifact selections. Bundled examples remain available. This cannot be undone.</p>
    <p id="delete-local-data-error" class="demo-error" role="alert" hidden></p>
    <div class="demo-editor-actions"><button id="cancel-delete-local-data" class="secondary" type="button" autofocus>Cancel</button>
      <button id="confirm-delete-local-data" class="danger" type="button">Delete local data</button></div>
  </dialog>
  <section id="timetable-save-bar" class="demo-save-bar" aria-label="Save timetable changes" hidden></section>`;
  let storage;
  try { storage = window.localStorage; } catch { storage = { getItem() { throw new Error(); } }; }
  browserStorage = storage;
  repository = createTimetableStorage(storage);
  notice = repository.notices.join(' ');
  try {
    const response = await fetch('/api/demo/timetables');
    if (!response.ok) throw new Error('The example timetables could not be loaded. Reload to try again.');
    const bundle = await response.json(); build = bundle.build;
    for (const sample of bundle.schools) {
      const timetables = new Map(sample.timetables.map(t => [t.id, { ...t, definition: sample.definition, bundled: true }]));
      schools.set(sample.identifier, { id: sample.identifier, name: sample.title, definition: sample.definition, bundled: true, timetables });
    }
    const saved = repository.state();
    for (const [id, school] of Object.entries(saved.schools)) {
      if (!isSchoolDefinition(school?.definition) || typeof school.name !== 'string' || schools.has(id)) { unreadable('school'); continue; }
      schools.set(id, { ...school, id, timetables: new Map() });
    }
    for (const [schoolId, versions] of Object.entries(saved.timetables)) {
      const school = schools.get(schoolId);
      if (!school || !versions || typeof versions !== 'object') { unreadable('timetable'); continue; }
      for (const [id, timetable] of Object.entries(versions)) {
        if (!isTimetable(timetable) || timetable.id !== id || school.timetables.has(id)) { unreadable('timetable'); continue; }
        school.timetables.set(id, timetable);
      }
    }
    // Retired individual-key examples are never promoted into the school catalog.
    // Their browser data stays untouched. Only explicitly saved schools above are loaded.
    for (const [key, draft] of Object.entries(saved.drafts)) {
      if (!draft?.generated || !isTimetable(draft.source)) continue;
      const schoolId = key.slice(0, key.lastIndexOf('/')), school = schools.get(schoolId);
      if (school && key === draftKey(schoolId, draft.source.id) && !school.timetables.has(draft.source.id)) school.timetables.set(draft.source.id, draft.source);
    }
    // Every arrival starts from the bundled, precomputed baseline. Saved work is
    // available through the selectors and Resume, never applied during startup.
    selectedSchool = schools.has('full') ? 'full' : bundle.schools[0].identifier;
    selectedTimetable = [...schools.get(selectedSchool).timetables.values()].find(t => t.bundled).id;
    bindShell(); render();
    demoRequest('/api/demo/config').then(({ value }) => { config = value; setupEditor.configure(value); renderGeneration(); }).catch(() => {});
    const interrupted = [...schools.values()].find(s => ['PREPARING','QUEUED', 'RUNNING', 'CHECKING_RESULT'].includes(s.generation?.status));
    if (interrupted) {
      const g=interrupted.generation;
      if(g.runId&&g.pin?.build===build&&JSON.stringify(canonical(g.pin.definition))===JSON.stringify(canonical(interrupted.definition))){activeGeneration={schoolId:interrupted.id,runId:g.runId,pin:g.pin};pollGeneration(activeGeneration);}
      else generationState(interrupted.id,{status:'INTERRUPTED',message:'The previous search could not be resumed. Your school is saved; you can generate a timetable again.'},true);
    }
  } catch (error) {
    notice = error.message; renderNotice();
    document.querySelector('#timetable-matrix').setAttribute('aria-busy', 'false');
  }
}

function unreadable(kind) { notice = `Some saved ${kind} data could not be displayed and has been retained in browser storage. Other schools remain available.`; }
function persist(change, atomic = false) {
  if (deletingLocalData) return false;
  try { repository.write(change, { atomic }); return true; }
  catch (error) { notice = error.message; renderNotice(); return false; }
}
function currentSchool() { return schools.get(selectedSchool); }
function currentTimetable() { return currentSchool()?.timetables.get(selectedTimetable); }
function currentDraft() {
  if (!showSavedDraft) return null;
  const draft = repository.state().drafts[draftKey(selectedSchool, selectedTimetable)];
  if (!draft) return null;
  const source = draft.source || currentTimetable();
  if (!isTimetable(source) || !draft.changes || typeof draft.changes !== 'object' || Array.isArray(draft.changes)
      || Object.entries(draft.changes).some(([id, patch]) => !source.definition.lessons.some(l => l.id === id)
        || !patch || typeof patch !== 'object' || Object.entries(patch).some(([field, value]) =>
          !['teacherId', 'roomId', 'periodId'].includes(field) || typeof value !== 'string'
          || !source.definition[{ teacherId: 'teachers', roomId: 'rooms', periodId: 'periods' }[field]].some(e => e.id === value)))) {
    unreadable('draft'); renderNotice(); return null;
  }
  return draft;
}
function candidate(timetable = currentTimetable(), draft = currentDraft()) {
  if (!timetable) return null;
  const source = draft?.source || timetable;
  const baseline = { definition: clone(source.definition), result: clone(source.result) };
  if (!draft?.changes || typeof draft.changes !== 'object') return baseline;
  for (const assignment of baseline.result.timetable.assignments) {
    const change = draft.changes[assignment.lessonId];
    if (!change) continue;
    for (const field of ['teacherId', 'roomId', 'periodId']) if (typeof change[field] === 'string') assignment[field] = change[field];
    const lesson = baseline.definition.lessons.find(l => l.id === assignment.lessonId);
    if (lesson) lesson.teacherId = assignment.teacherId;
  }
  return baseline;
}

function bindShell() {
  document.querySelector('#export-timetable-pdf').addEventListener('click', exportPdf);
  const setupEntry = document.querySelector('#school-setup-entry');
  setupEditor = createSetupEditor({ repository, school: currentSchool, storage: browserStorage, entry: setupEntry, build:()=>build, generationInfo:id=>schools.get(id)?.generation, onGenerate:(id,effort)=>{const s=schools.get(id);if(s){s.effort=effort;generateTimetable(id);}}, onCancel:cancelGeneration, knownSchools:()=>[...schools.values()], onSaved(school){schools.set(school.id,{...school,timetables:new Map()});selectedSchool=school.id;selectedTimetable=undefined;filters=blankFilters();closeInspector();notice='School copy saved. No timetable yet; generation is an explicit action.';render();}, onCleanup(){document.querySelector('#delete-local-data-error').hidden=true;document.querySelector('#delete-local-data-dialog').showModal();} });
  document.querySelector('#resume-timetable-draft').addEventListener('click', () => { showSavedDraft = true; render(); });
  setupEntry.addEventListener('click', () => { setupEditor.open(); });
  setupEntry.disabled = false;
  document.querySelector('#school-selector').addEventListener('change', event => {
    showSavedDraft = true; closeInspector(); selectedSchool = event.target.value; filters = blankFilters();
    const school = currentSchool(), last = repository.state().lastTimetable[selectedSchool];
    selectedTimetable = school.timetables.has(last) ? last : school.timetables.keys().next().value;
    rememberSelection(); render();
  });
  document.querySelector('#timetable-selector').addEventListener('change', event => {
    showSavedDraft = true; closeInspector(); selectedTimetable = event.target.value; rememberSelection(); render();
  });
  document.querySelector('#cancel-delete-local-data').addEventListener('click', () => document.querySelector('#delete-local-data-dialog').close());
  document.querySelector('#confirm-delete-local-data').addEventListener('click', () => {
    if (deletingLocalData) return;
    try {
      deleteLocalTimetableData(browserStorage);
      // Pending responses must not restore deleted artifacts while the clean page is loading.
      deletingLocalData = true;
      setupEditor.stop();
      for (const timer of timers.values()) clearTimeout(timer);
      timers.clear(); reviews.clear(); activeGeneration = null;
      window.location.reload();
    } catch (error) {
      const message = document.querySelector('#delete-local-data-error');
      message.textContent = error.message; message.hidden = false;
    }
  });
  document.querySelector('#generation-indicator').addEventListener('click', event => {
    if (event.target.closest('[data-show-generation]')) {
      const origin=activeGeneration?.schoolId ?? selectedSchool;
      if(schools.has(origin)){selectedSchool=origin;selectedTimetable=currentSchool().timetables.keys().next().value;filters=blankFilters();render();setupEditor.open({review:true});}
    }
    const view = event.target.closest('[data-view-generated]');
    if (view) {
      showSavedDraft = true; closeInspector(); selectedSchool = view.dataset.viewGenerated; filters = blankFilters();
      selectedTimetable = repository.state().lastTimetable[selectedSchool]; rememberSelection(); render();
    }
  });
  document.querySelector('#timetable-matrix').addEventListener('click', event => {
    const cell = event.target.closest('[data-lesson-id]');
    if (cell) openInspector(cell.dataset.lessonId);
  });
  document.querySelector('#lesson-inspector').addEventListener('click', event => {
    if (event.target.closest('[data-close-inspector]')) closeInspector();
  });
  document.querySelector('#draft-feedback').addEventListener('click', event => {
    const button = event.target.closest('[data-conflict-lesson]');
    if (button) {
      filters = blankFilters(); renderFilters(); renderMatrix(); openInspector(button.dataset.conflictLesson);
      [...document.querySelectorAll('.demo-lesson')].find(e => e.dataset.lessonId === selectedLesson)?.scrollIntoView({ block: 'center', inline: 'nearest' });
      positionInspector();
    }
    if (event.target.closest('[data-retry-review]')) queueReview(true);
  });
  document.querySelector('#timetable-save-bar').addEventListener('input', event => {
    if (event.target.id === 'new-timetable-name') {
      const key = draftKey(selectedSchool, selectedTimetable);
      persist(state => { if (state.drafts[key]) state.drafts[key].name = event.target.value; });
      const error = document.querySelector('#timetable-name-error'); if (error) error.textContent = '';
    }
  });
  document.querySelector('#timetable-save-bar').addEventListener('click', event => {
    if (event.target.closest('#discard-timetable-changes')) {
      const key = draftKey(selectedSchool, selectedTimetable);
      const draft = currentDraft(), school = currentSchool();
      if (persist(state => {
        delete state.drafts[key];
        if (draft?.generated && state.schools[selectedSchool]) state.schools[selectedSchool].generation = { status: 'DISCARDED', message: 'Generated candidate discarded. You can generate another timetable.' };
      }, true)) {
        if (draft?.generated) { school.timetables.delete(selectedTimetable); school.generation = { status: 'DISCARDED', message: 'Generated candidate discarded. You can generate another timetable.' }; selectedTimetable = school.timetables.keys().next().value; }
        reviews.delete(key); closeInspector(); render();
      }
    }
    if (event.target.closest('#save-new-timetable')) saveTimetable();
    if (event.target.closest('[data-retry-review]')) queueReview(true);
  });
  document.addEventListener('keydown', event => { if (event.key === 'Escape') closeInspector(); });
  window.addEventListener('resize', positionInspector);
  mobileLayout.addEventListener('change', () => {
    if (mobileLayout.matches) {
      setupEditor.close({ immediate: true });
      document.querySelector('#delete-local-data-dialog').close();
    }
    const disclosure = document.querySelector('#timetable-filter-disclosure');
    if (disclosure) disclosure.open = !mobileLayout.matches;
    renderMatrix(); positionInspector();
  });
  new ResizeObserver(() => {
    const bar = document.querySelector('#timetable-save-bar');
    document.body.style.setProperty('--save-bar-height', `${bar.hidden ? 0 : bar.getBoundingClientRect().height + 35}px`);
    positionInspector();
  }).observe(document.querySelector('#timetable-save-bar'));
}

function rememberSelection() {
  persist(state => {
    state.selected = { schoolId: selectedSchool, timetableId: selectedTimetable || null };
    if (selectedTimetable) state.lastTimetable[selectedSchool] = selectedTimetable;
  });
}
function renderNotice() {
  const element = document.querySelector('#demo-notice'); element.hidden = !notice; element.textContent = notice;
}
function option(id, label, selected) { return `<option value="${html(id)}"${id === selected ? ' selected' : ''}>${html(label)}</option>`; }
function render() {
  const school = currentSchool(), timetable = currentTimetable();
  renderNotice();
  document.querySelector('#school-selector').innerHTML = [...schools.values()].map(s => option(s.id, s.name, selectedSchool)).join('');
  const drafts = repository.state().drafts;
  document.querySelector('#timetable-selector').innerHTML = school.timetables.size ? [...school.timetables.values()]
    .map(t => option(t.id, t.name + (showSavedDraft && drafts[draftKey(school.id, t.id)] ? ' · unsaved changes' : ''), selectedTimetable)).join('') : option('', 'No timetable yet', '');
  document.querySelector('#timetable-selector').disabled = !school.timetables.size;
  document.querySelector('#resume-timetable-draft').hidden = showSavedDraft || !drafts[draftKey(school.id, selectedTimetable)];
  document.querySelector('#timetable-title').textContent = timetable?.name || school.name;
  const baseline = candidate();
  model = baseline ? makeAcceptedModel(baseline) : null;
  document.querySelector('#export-timetable-pdf').disabled = !model || pdfBusy;
  document.querySelector('#timetable-description').textContent = model
    ? `${school.name} · ${model.definition.cohorts.length} classes · ${model.assignments.length} lessons per week`
    : 'This school has no timetable yet. Open School setup → Review on desktop to generate a timetable.';
  renderFilters(); renderMatrix(); renderGeneration(); renderDraft(); queueReview();
  document.querySelector('#timetable-matrix').setAttribute('aria-busy', 'false');
}

async function exportPdf() {
  if (!model || pdfBusy) return;
  const timetable = currentTimetable();
  // Capture the selected schedule and filters before asynchronous PDF resources load.
  const options = { model, filters: clone(filters), schoolName: currentSchool().name,
    timetableName: timetable.name, draft: !!currentDraft(),
    authoring: clone(timetable.authoring ?? timetable.provenance?.source?.authoring) };
  const { type, entityId } = resolvePdfScope(options.filters);
  const entity = options.model.maps[type === 'teacher' ? 'teachers' : 'cohorts'].get(entityId);
  const scope = type === 'school' ? 'full school' : `${type} ${entity?.displayName || entityId}`;
  const button = document.querySelector('#export-timetable-pdf'), status = document.querySelector('#pdf-export-status');
  pdfBusy = true; button.disabled = true; button.setAttribute('aria-busy', 'true');
  status.hidden = false; status.className = 'muted'; status.textContent = `Preparing ${options.timetableName} / ${scope} / whole week…`;
  try {
    const { downloadTimetablePdf } = await import('./timetable-pdf.js');
    await downloadTimetablePdf(options);
    status.textContent = `PDF downloaded: ${options.timetableName} / ${scope}${options.draft ? ' - labelled as an unpublished draft' : ''}.`;
  } catch (error) {
    status.className = 'demo-error'; status.textContent = error.message || 'The PDF could not be created. Please try again.';
  } finally {
    pdfBusy = false; button.disabled = !model; button.setAttribute('aria-busy', 'false');
  }
}

function renderFilters() {
  const parent = document.querySelector('#timetable-filters');
  if (!model) { parent.innerHTML = ''; return; }
  const expanded = parent.querySelector('details')?.open ?? !mobileLayout.matches;
  const select = (id, title, collection, key) => `<label>${title}<select id="${id}" data-filter="${key}">${option('', `All ${title.toLowerCase()}`, filters[key])}${collection.map(e => option(e.id, e.displayName || e.id, filters[key])).join('')}</select></label>`;
  parent.innerHTML = `<details id="timetable-filter-disclosure" class="demo-filter-disclosure"${expanded ? ' open' : ''}><summary>Filters <span id="timetable-filter-summary"></span></summary><div class="demo-filters">
    <label>Day<select id="day-filter" data-filter="day">${option('', 'Whole week', filters.day)}${model.weekdays.map(day => option(day, M.days[day] || day, filters.day)).join('')}</select></label>
    ${select('cohort-filter', 'Classes', model.definition.cohorts, 'cohortId')}
    ${select('teacher-filter', 'Teachers', model.definition.teachers, 'teacherId')}
    ${select('room-filter', 'Rooms', model.definition.rooms, 'roomId')}
    ${select('subject-filter', 'Subjects', model.definition.subjects, 'subjectId')}
    ${select('period-filter', 'Time slots', model.definition.periods.map(p => ({ ...p, displayName: periodLabel(p) })), 'periodId')}
    <label class="demo-search">Highlight lessons<input id="lesson-search" type="search" placeholder="Subject, teacher or class" value="${html(filters.search)}"></label>
    <button id="clear-timetable-filters" class="secondary" type="button">Clear filters</button></div></details>`;
  updateFilterSummary();
  parent.querySelectorAll('[data-filter]').forEach(select => select.addEventListener('change', event => {
    const key = select.dataset.filter; filters[key] = event.target.value;
    if (key === 'day') filters.range = filters.day ? 'DAY' : 'WEEK';
    if (key === 'teacherId' && filters.teacherId) filters.roomId = '';
    if (key === 'roomId' && filters.roomId) filters.teacherId = '';
    if (key === 'periodId' && filters.periodId) { filters.range = 'DAY'; filters.day = model.maps.periods.get(filters.periodId)?.weekday || model.weekdays[0]; }
    closeInspector(); renderFilters(); renderMatrix();
  }));
  parent.querySelector('#lesson-search').addEventListener('input', event => { filters.search = event.target.value; updateFilterSummary(); renderMatrix(); });
  parent.querySelector('#clear-timetable-filters').addEventListener('click', () => { filters = blankFilters(); closeInspector(); renderFilters(); renderMatrix(); });
}

function updateFilterSummary() {
  const count = ['cohortId', 'teacherId', 'roomId', 'subjectId', 'periodId', 'day', 'search'].filter(key => filters[key]).length;
  document.querySelector('#timetable-filter-summary').textContent = count ? `${count} active` : 'All lessons';
}

function periodLabel(period) {
  return `${M.days[period.weekday] || period.weekday} · ${period.order}. ${period.startTime ? period.startTime.slice(0, 5) + '–' + period.endTime.slice(0, 5) : period.displayName || ''}`;
}
function matches(item) {
  return (!filters.cohortId || item.cohortId === filters.cohortId) && (!filters.teacherId || item.teacherId === filters.teacherId)
    && (!filters.roomId || item.roomId === filters.roomId) && (!filters.subjectId || item.subjectId === filters.subjectId)
    && (!filters.periodId || item.periodId === filters.periodId);
}
function renderMatrix() {
  const parent = document.querySelector('#timetable-matrix');
  if (!model) { parent.innerHTML = '<p class="demo-empty">No timetable yet. Open School setup → Review on desktop to generate a timetable.</p>'; return; }
  const existing = parent.querySelector('.matrix-wrap'), scroll = { left: existing?.scrollLeft || 0, top: existing?.scrollTop || 0 };
  const teachers = model.maps.teachers, rooms = model.maps.rooms;
  const rows = filters.teacherId ? [{ kind: 'TEACHER', id: filters.teacherId, label: teachers.get(filters.teacherId)?.displayName || filters.teacherId }]
    : filters.roomId ? [{ kind: 'ROOM', id: filters.roomId, label: rooms.get(filters.roomId)?.displayName || filters.roomId }]
    : model.definition.cohorts.filter(c => !filters.cohortId || c.id === filters.cohortId).map(c => ({ kind: 'CLASS', id: c.id, label: c.displayName || c.id }));
  const items = model.assignments.filter(matches);
  const byPeriod = new Map();
  for (const item of items) {
    const key = `${filters.teacherId || filters.roomId ? '' : item.cohortId}/${item.periodId}`;
    if (!byPeriod.has(key)) byPeriod.set(key, []); byPeriod.get(key).push(item);
  }
  const cellItems = (row, period) => byPeriod.get(`${row.kind === 'CLASS' ? row.id : ''}/${period.id}`) || [];
  const snapshot=currentTimetable()?.authoring??currentTimetable()?.provenance?.source?.authoring;
  const periodsForDay = day => {
    const periods=model.definition.periods.filter(p=>p.weekday===day).sort((a,b)=>a.order-b.order);
    if(!snapshot?.grid)return periods;
    const intervals=snapshot.grid.intervals.filter(i=>i.day===day),result=[];let last=0,between=0;
    for(const interval of intervals){const period=periods.find(p=>p.id===interval.periodId);if(period){last=period.order;between=0;result.push(period);}else if(['Break','Lunch'].includes(interval.kind)){result.push({id:'interval-'+interval.id,displayName:interval.label,weekday:day,order:last+(++between)/1000,startTime:interval.start,endTime:interval.end,nonTeaching:interval.kind});}}
    return result.sort((a,b)=>a.order-b.order);
  };
  const emptyMarkup=(row,period)=>period.nonTeaching?`<span class="setup-bell-interval">${html(period.nonTeaching)}: ${html(period.displayName)} <time>${html(period.startTime)}–${html(period.endTime)}</time></span>`:row.kind==='CLASS'&&snapshot?.classes.find(c=>c.id===row.id)?.lunchPeriodIds?.includes(period.id)?'<span class="setup-class-lunch">Class lunch</span>':'<span class="empty-cell">—</span>';
  const props = { rows, rowHeading: filters.teacherId ? 'Teacher' : filters.roomId ? 'Room' : 'Class', weekdays: model.weekdays,
    cellItems, emptyMarkup, periodsForDay, labels: M,
    periodLabel, lessonMarkup, escapeHtml: html, escapeAttribute: html };
  parent.innerHTML = mobileLayout.matches
    ? renderMobileMatrix({ ...props, weekdays: filters.range === 'DAY' ? [filters.day || model.weekdays[0]] : model.weekdays,
        periodsForDay: day => periodsForDay(day).filter(p => !filters.periodId || p.id === filters.periodId) })
    : filters.range === 'DAY'
    ? renderDayMatrix({ ...props, periods: periodsForDay(filters.day || model.weekdays[0]), periodId: filters.periodId })
    : renderWeekMatrix(props);
  const matrix = parent.querySelector('.matrix-wrap'); matrix.tabIndex = 0; matrix.scrollLeft = scroll.left; matrix.scrollTop = scroll.top;
}
function lessonMarkup(item) {
  const text = [item.subject?.displayName, item.teacher?.displayName, item.cohort?.displayName, item.room?.displayName].join(' · ');
  const highlighted = filters.search && text.toLocaleLowerCase().includes(filters.search.toLocaleLowerCase());
  const color = model.definition.subjects.findIndex(s => s.id === item.subjectId) % 24;
  const week = filters.range === 'WEEK';
  const teacher = `<span class="demo-tile-teacher${week ? ' week-meta' : ''}" title="${html(item.teacher?.displayName || item.teacherId)}">${html(item.teacher?.displayName || item.teacherId)}</span>`;
  const room = `<span class="demo-tile-room${week ? ' week-room' : ''}" title="${html(item.room?.displayName || item.roomId)}">${html(item.room?.displayName || item.roomId)}</span>`;
  const cohort = `<span class="demo-tile-class${week ? ' week-meta' : ''}" title="${html(item.cohort?.displayName || item.cohortId)}">${html(item.cohort?.displayName || item.cohortId)}</span>`;
  const details = filters.teacherId ? room + cohort : filters.roomId ? teacher + cohort : week ? room : teacher + room;
  const conflicts = conflictItems(reviews.get(draftKey(selectedSchool, selectedTimetable))?.evidence, model);
  const invalid = conflicts.some(c => c.lessonIds.includes(item.lessonId));
  return `<button type="button" class="lesson-cell demo-lesson subject-color-${color}${week ? ' week-lesson' : ''}${highlighted ? ' match' : ''}${invalid ? ' has-conflict' : ''}" data-lesson-id="${html(item.lessonId)}" aria-label="${html((invalid ? 'Must fix · ' : '') + text + ' · ' + periodLabel(item.period))}">${invalid ? '<span class="demo-conflict-mark">Must fix</span>' : ''}<strong title="${html(item.subject?.displayName || item.subjectId)}">${html(item.subject?.displayName || item.subjectId)}</strong>${details}</button>`;
}

function openInspector(lessonId) {
  const item = model?.assignmentMap.get(lessonId); if (!item) return;
  selectedLesson = lessonId;
  const inspector = document.querySelector('#lesson-inspector');
  inspector.innerHTML = `<div class="demo-inspector-heading"><div><p class="eyebrow">${html(item.cohort?.displayName)}</p><h2 id="inspector-title" tabindex="-1">${html(item.subject?.displayName)}</h2></div><button type="button" class="secondary" data-close-inspector aria-label="Close lesson inspector">✕</button></div>
    <label>Teacher<select id="inspector-teacher">${model.definition.teachers.map(t => option(t.id, (t.displayName || t.id) + (eligible(t, item) ? '' : ' · not eligible for this lesson'), item.teacherId)).join('')}</select></label>
    <p class="muted">Only eligible teachers can be saved. Busy teachers remain selectable so you can swap lessons.</p>
    <label>Room<select id="inspector-room">${model.definition.rooms.map(r => option(r.id, `${r.displayName || r.id} · ${r.capacity} seats${r.capacity < item.cohort.size ? ' · too small' : !(item.lesson.requiredRoomCapabilityIds || []).every(id => r.capabilityIds?.includes(id)) ? ' · missing required equipment' : ''}`, item.roomId)).join('')}</select></label>
    <label>Time slot<select id="inspector-period">${model.definition.periods.map(p => option(p.id, periodLabel(p) + (item.lesson.periodLock && item.lesson.periodLock !== p.id ? ' · outside fixed time' : item.teacher.availablePeriodIds && !item.teacher.availablePeriodIds.includes(p.id) ? ' · teacher unavailable' : item.cohort.availablePeriodIds && !item.cohort.availablePeriodIds.includes(p.id) ? ' · class unavailable' : ''), item.periodId)).join('')}</select></label>
    <div id="inspector-feedback"></div>`;
  for (const [field, id] of [['teacherId', 'inspector-teacher'], ['roomId', 'inspector-room'], ['periodId', 'inspector-period']]) {
    inspector.querySelector('#' + id).addEventListener('change', event => editLesson(lessonId, field, event.target.value));
  }
  const problems = conflictItems(reviews.get(draftKey(selectedSchool, selectedTimetable))?.evidence, model).filter(c => c.lessonIds.includes(lessonId));
  inspector.querySelector('#inspector-feedback').innerHTML = problems.map(c => `<p class="demo-error"><strong>Must fix: ${html(c.title)}</strong><br>${html(c.message)}</p>`).join('');
  if (!inspector.open) inspector.show();
  positionInspector(); inspector.querySelector('#inspector-title').focus({ preventScroll: true });
}
function eligible(teacher, item) {
  return item.subject?.curatorLesson ? item.cohort?.curatorTeacherId === teacher.id : teacher.qualifiedSubjectIds?.includes(item.subjectId);
}

function editLesson(lessonId, field, value) {
  const key = draftKey(selectedSchool, selectedTimetable);
  if (!showSavedDraft && repository.state().drafts[key]) { notice = 'Resume your saved changes before editing this timetable.'; renderNotice(); return; }
  showSavedDraft = true;
  const existing = currentDraft();
  if (repository.state().drafts[key] && !existing) { notice = 'This retained draft cannot be edited safely. Select another timetable to continue.'; renderNotice(); return; }
  const source = existing?.source || clone(currentTimetable());
  const original = source.result.timetable.assignments.find(a => a.lessonId === lessonId);
  const draft = existing || { source, changes: {}, name: '', createdAt: new Date().toISOString() };
  draft.changes[lessonId] ||= {};
  if (value === original[field]) delete draft.changes[lessonId][field]; else draft.changes[lessonId][field] = value;
  if (!Object.keys(draft.changes[lessonId]).length) delete draft.changes[lessonId];
  persist(state => {
    if (Object.keys(draft.changes).length || draft.generated) state.drafts[key] = draft;
    else delete state.drafts[key];
  });
  reviews.delete(key); render(); openInspector(lessonId);
}
function signature(value) { return JSON.stringify([build, value?.definition, value?.result?.timetable?.assignments]); }
function queueReview(force = false) {
  const draft = currentDraft(); if (!draft) return;
  const key = draftKey(selectedSchool, selectedTimetable), input = candidate(), exact = signature(input);
  if (!force && reviews.get(key)?.signature === exact) return;
  clearTimeout(timers.get(key));
  const review = { signature: exact, status: 'PENDING' }; reviews.set(key, review); renderDraft();
  timers.set(key, setTimeout(async () => {
    try {
      const { value } = await demoRequest('/api/demo/timetable/review', { method: 'POST', body: input });
      if (reviews.get(key) !== review) return;
      const expectedRevision=await definitionRevision(input.definition);
      if(reviews.get(key)!==review)return;
      if (value.status==='REVIEWED'&&(value.definitionRevision!==expectedRevision||value.valid&&value.result?.inputRevision!==expectedRevision))throw new Error('Review provenance does not match the exact timetable definition.');
      if (value.build !== build || !['REVIEWED', 'INVALID_INPUT'].includes(value.status)
          || value.status === 'REVIEWED' && (typeof value.valid !== 'boolean' || !Array.isArray(value.conflicts)
              || value.valid && (!value.result || value.result.status !== 'FEASIBLE' || value.conflicts.length)))
        throw new Error('Validation returned incomplete or incompatible evidence. Retry the check.');
      review.evidence = value; review.status = value.valid === true ? 'VALID' : 'INVALID';
    } catch (error) { if (reviews.get(key) !== review) return; review.status = 'FAILED'; review.error = error.message; }
    if (draftKey(selectedSchool, selectedTimetable) === key) {
      renderDraft(); renderMatrix(); if (selectedLesson) openInspector(selectedLesson);
    }
  }, 300));
}
function lessonContext(id) {
  const item = model?.assignmentMap.get(id);
  return item ? `${item.cohort.displayName} · ${item.subject.displayName} · ${item.teacher.displayName} · ${item.room.displayName} · ${periodLabel(item.period)}` : id;
}
function renderDraft() {
  const draft = currentDraft(), bar = document.querySelector('#timetable-save-bar'), feedback = document.querySelector('#draft-feedback');
  bar.hidden = !draft; feedback.hidden = !draft;
  if (!draft) { bar.innerHTML = ''; feedback.innerHTML = ''; document.body.style.setProperty('--save-bar-height', '0px'); return; }
  const review = reviews.get(draftKey(selectedSchool, selectedTimetable)), status = review?.status || 'PENDING';
  const conflicts = conflictItems(review?.evidence, model), warnings = warningItems(review?.evidence, draft.source?.result || currentTimetable().result);
  const text = status === 'VALID' ? 'Ready to save' : status === 'INVALID' ? `Must fix · ${conflicts.length} rule${conflicts.length === 1 ? '' : 's'}` : status === 'FAILED' ? 'Validation failed' : 'Checking all rules…';
  feedback.innerHTML = status === 'FAILED' ? `<p class="demo-error">${html(review.error)} <button type="button" data-retry-review>Retry validation</button></p>`
    : conflicts.length ? `<div class="demo-errors"><h2>Must fix</h2><p>Save is blocked until these mandatory rules pass.</p>${conflicts.map(c => `<details open data-rule="${html(c.rule)}"><summary>${html(c.title)}</summary><p>${html(c.message)}</p><ul>${c.lessonIds.map(id => `<li><button type="button" class="demo-conflict-link" data-conflict-lesson="${html(id)}">${html(lessonContext(id))}</button></li>`).join('')}</ul></details>`).join('')}${review.evidence?.truncated || review.evidence?.validationReport?.truncated ? '<p>Diagnostic examples are limited. The entire draft remains invalid; all potentially affected lessons are marked.</p>' : ''}</div>` : '';
  if (warnings.length) feedback.innerHTML += `<details class="demo-warnings"><summary>Could be improved · ${warnings.length} preferences</summary><p>Advisory only. Counts cover the whole timetable; the change from the source is shown separately.</p><ul>${warnings.map(w => `<li><strong>${html(w.title)}</strong>: ${html(w.message)} ${w.count} total; ${w.delta > 0 ? '+' : ''}${w.delta} from source.</li>`).join('')}</ul></details>`;
  bar.innerHTML = `<div class="demo-save-status" role="status"><strong>${html(text)}</strong><span>${draft.generated ? 'Generated timetable' : Object.keys(draft.changes).length + ' changed lesson' + (Object.keys(draft.changes).length === 1 ? '' : 's')}</span></div>
    <label>New timetable name<input id="new-timetable-name" maxlength="120" value="${html(draft.name || '')}" placeholder="Name this alternative"></label>
    <button id="save-new-timetable" type="button"${status !== 'VALID' ? ' disabled' : ''}>Save as new timetable</button>
    <button id="discard-timetable-changes" type="button" class="secondary">Discard changes</button><p id="timetable-name-error" class="demo-error" role="alert"></p>`;
  document.body.style.setProperty('--save-bar-height', `${bar.getBoundingClientRect().height + 35}px`);
  positionInspector();
}
function saveTimetable() {
  const draft = currentDraft(), key = draftKey(selectedSchool, selectedTimetable), review = reviews.get(key), input = candidate();
  if (!draft || review?.status !== 'VALID' || review.signature !== signature(input)) return;
  const name = (draft.name || '').trim(), school = currentSchool();
  if (!name || name.length>120 || [...school.timetables.values()].some(t => t.name.toLocaleLowerCase() === name.toLocaleLowerCase())) {
    document.querySelector('#timetable-name-error').textContent = name.length>120?'Use at most 120 characters.':name ? 'This school already has a timetable with that name.' : 'Enter a name for the new timetable.'; return;
  }
  const id = crypto.randomUUID(), timetable = { id, name, definition: input.definition,
    result: draft.generated && !Object.keys(draft.changes).length ? draft.source.result : review.evidence.result,
    authoring:clone(draft.source.authoring??null), provenance: { sourceSchoolId: selectedSchool, sourceTimetableId: selectedTimetable, source: draft.source, build, savedAt: new Date().toISOString(), review: review.evidence } };
  if (!persist(state => {
    state.timetables[selectedSchool] ||= {}; state.timetables[selectedSchool][id] = timetable;
    delete state.drafts[key]; state.lastTimetable[selectedSchool] = id; state.selected = { schoolId: selectedSchool, timetableId: id };
    if (draft.generated && state.schools[selectedSchool]) state.schools[selectedSchool].generation = { status: 'SAVED', message: `Saved timetable: ${name}.` };
  }, true)) return;
  if (draft.generated) { school.timetables.delete(selectedTimetable); school.generation = { status: 'SAVED', message: `Saved timetable: ${name}.` }; }
  school.timetables.set(id, timetable); reviews.delete(key); selectedTimetable = id; notice = ''; closeInspector(); render();
}

function renderGeneration() {
  if (!currentSchool()) return;
  const school = currentSchool(), large = school.definition.lessons.length > config.lessonThreshold;
  const effort = school.effort || (large ? 'THOROUGH' : 'QUICK');
  const candidateWaiting = [...school.timetables.keys()].some(id => repository.state().drafts[draftKey(school.id, id)]?.generated);
  let text = activeGeneration ? `Generating for ${schools.get(activeGeneration.schoolId)?.name}. ${schools.get(activeGeneration.schoolId)?.generation?.message || 'Preparing…'}`
    : school.generation?.message || (school.bundled ? 'Save a school copy before generating a timetable.' : `Generate for ${school.name} using its saved definition. Save a new school copy to use editor changes.`);
  if (candidateWaiting) text += ' A generated candidate awaits naming in the timetable view. Close the editor to save or discard it.';
  if (large && effort === 'QUICK') text += ' Quick is usually too short for this school size.';
  text += ' Search budgets cover construction and search; queueing, startup and checking take additional time. More time may help but guarantees nothing.';
  setupEditor?.refreshGeneration();
  const indicator = document.querySelector('#generation-indicator');
  const pending = [...schools.values()].find(s => s.id !== selectedSchool && s.generation?.status === 'CANDIDATE');
  indicator.hidden = !activeGeneration && !pending;
  indicator.innerHTML = activeGeneration ? `${html(text.split(' Search budgets')[0])} <button type="button" class="secondary" data-show-generation>View generation in School setup</button>`
    : pending ? `A generated timetable for ${html(pending.name)} awaits naming. <button type="button" class="secondary" data-view-generated="${html(pending.id)}">View generated timetable</button>` : '';
}
function generationState(schoolId, value, durable = false) {
  const school = schools.get(schoolId); if (!school) return;
  value={...(school.generation?.pin?{pin:school.generation.pin}:{}),...value};
  school.generation = value;
  if (durable) persist(state => {
    // Legacy browser definitions remain untouched; promote their complete content only when generation is explicit.
    state.schools[schoolId] ||= { id: school.id, name: school.name, definition: school.definition, legacy: school.legacy };
    state.schools[schoolId].generation = value;
  });
  renderGeneration();
}
function cancelPreparation(token) {
  if (!token.cancelRequested || activeGeneration !== token) return false;
  activeGeneration = null;
  generationState(token.schoolId, { status: 'CANCELLED', message: 'Generation cancelled. The saved school and all timetables are retained.' }, true);
  return true;
}
async function generateTimetable(schoolId=selectedSchool) {
  const school = schools.get(schoolId);
  if (!school || activeGeneration || school.bundled || [...school.timetables.keys()].some(id=>repository.state().drafts[draftKey(school.id,id)]?.generated)) return;
  const pin={schoolId:school.id,definition:clone(school.definition),definitionRevision:school.definitionRevision,build,authoring:school.authoring?clone(school.authoring):null,effort:school.effort||(school.definition.lessons.length>config.lessonThreshold?'THOROUGH':'QUICK')};
  const token = { schoolId: school.id, runId: null,pin }; activeGeneration = token;
  generationState(school.id, { status: 'PREPARING',pin,message: 'Preparing and checking the saved school definition…' },true);
  try {
    const checked=await demoRequest('/api/demo/definition/check',{method:'POST',body:JSON.stringify(pin.definition),headers:{'Content-Type':'text/plain'}});
    if(activeGeneration!==token||cancelPreparation(token))return;
    const expected=await definitionRevision(pin.definition);
    if(checked.value.build!==pin.build||checked.value.definitionRevision!==expected||pin.definitionRevision&&pin.definitionRevision!==expected)throw new Error('Saved definition/build provenance could not be checked. The saved school is retained.');
    pin.definitionRevision=expected;
    const workspace = await demoRequest('/api/workspace');
    if (activeGeneration !== token || cancelPreparation(token)) return;
    if (workspace.value.workspace.run) throw new Error('Another generation is active in this temporary session. Wait for it or reload to recover its controls.');
    const installed = await demoRequest('/api/demo/definition', { method: 'POST', body: JSON.stringify(pin.definition),
      headers: { 'Content-Type': 'text/plain', 'If-Match': workspace.etag, 'X-Demo-Confirmed': 'true' } });
    if (activeGeneration !== token || cancelPreparation(token)) return;
    const started = await demoRequest('/api/runs', { method: 'POST', body: { effort: pin.effort }, headers: { 'If-Match': installed.etag } });
    if (activeGeneration !== token) return;
    token.runId = started.value.workspace.run?.id || started.value.workspace.lastRun?.id;
    if (token.cancelRequested && token.runId) { await cancelGeneration(); return; }
    if (!token.runId) throw new Error('Generation returned no run identity. The school is retained. Retry shortly.');
    generationState(school.id, { runId: token.runId, status: started.value.workspace.run?.status || 'CHECKING_RESULT', message: 'Generation started.' }, true);
    pollGeneration(token);
  } catch (error) {
    if (activeGeneration !== token) return;
    if (cancelPreparation(token)) return;
    activeGeneration = null; generationState(school.id, { status: 'FAILED', message: error.message }, true);
  }
}
async function pollGeneration(token) {
  if (activeGeneration !== token) return;
  try {
    const { value: run } = await demoRequest(`/api/runs/${encodeURIComponent(token.runId)}`);
    if (activeGeneration !== token || token.cancelling) return;
    if (['QUEUED', 'RUNNING', 'CHECKING_RESULT'].includes(run.status)) {
      const message = run.status === 'QUEUED' ? `Queued · position ${run.queuePosition}. Approximate start in ${Math.ceil((run.estimatedStartMs || 0) / 1000)} seconds.`
        : run.status === 'CHECKING_RESULT' ? 'Checking the complete result…' : `Running · ${Math.floor((run.elapsedTimeMs || 0) / 1000)} seconds. Search budget ${run.limit}.`;
      generationState(token.schoolId, { runId: token.runId, status: run.status, message });
      setTimeout(() => pollGeneration(token), 1000); return;
    }
    if (run.status !== 'FEASIBLE') {
      activeGeneration = null;
      const errors = run.validationReport?.errors?.map(e => `${e.location || '/'}: ${e.message}`).join(' ');
      generationState(token.schoolId, { status: run.status, message: (run.message || 'Generation did not produce a complete timetable. The school is retained.') + (errors ? ' ' + errors : '') }, true); return;
    }
    const { value: proposal } = await demoRequest('/api/proposal');
    if (activeGeneration !== token) return;
    const school = schools.get(token.schoolId), { definition, result, ...provenance } = proposal;
    const source = { id: 'generated-' + crypto.randomUUID(), name: 'Generated timetable', definition, result, authoring:clone(token.pin.authoring), provenance:{...provenance,pin:clone(token.pin)} };
    if (proposal.runId !== token.runId || proposal.solveProvenance?.kernelBuild !== build || !isTimetable(source)
        || JSON.stringify(canonical(definition)) !== JSON.stringify(canonical(token.pin.definition)) || result.inputRevision!==token.pin.definitionRevision || result.catalogVersion!==token.pin.definition.catalogVersion || proposal.successorDefinitionRevision!==token.pin.definitionRevision || result.status !== 'FEASIBLE')
      throw new Error('The generated result could not be completely mapped to its school. It has not been saved. Try generation again.');
    const key = draftKey(school.id, source.id), draft = { source, generated: true, changes: {}, name: '', createdAt: new Date().toISOString() };
    const savedCandidate=persist(state => {
      state.drafts[key] = draft; state.lastTimetable[school.id] = source.id;
      if (state.schools[school.id]) state.schools[school.id].generation = { status: 'CANDIDATE', message: 'Generated timetable is ready for naming. Close the editor to save it.' };
      if (selectedSchool === school.id) state.selected = { schoolId: school.id, timetableId: source.id };
    });
    school.timetables.set(source.id, source); school.generation = { status: 'CANDIDATE', message: `${proposal.reused ? 'Result reused from a checked identical search.' : 'Result computed and checked.'} Close the editor to name and save the timetable.` };
    if(!savedCandidate)school.generation.message+=' Only on this page: candidate persistence failed. Keep this page open and retry saving.';
    activeGeneration = null;
    if (selectedSchool === school.id) { showSavedDraft = true; selectedTimetable = source.id; render(); }
    else renderGeneration();
  } catch (error) {
    if (activeGeneration !== token) return;
    activeGeneration = null;
    generationState(token.schoolId, { status: 'INTERRUPTED', message: `Generation could not be recovered: ${error.message} The saved school is retained; generate again when available.` }, true);
  }
}
async function cancelGeneration() {
  const token = activeGeneration; if (!token || token.cancelling) return;
  if (!token.runId) { token.cancelRequested=true; generationState(token.schoolId,{status:'PREPARING',message:'Cancelling preparation…'}); return; }
  token.cancelling = true;
  try {
    const workspace = await demoRequest('/api/workspace');
    await demoRequest(`/api/runs/${encodeURIComponent(token.runId)}`, { method: 'DELETE', headers: { 'If-Match': workspace.etag } });
    if (activeGeneration !== token) return;
    activeGeneration = null; generationState(token.schoolId, { status: 'CANCELLED', message: 'Generation cancelled. The saved school and all timetables are retained.' }, true);
  } catch (error) { token.cancelling = false; notice = error.message; renderNotice(); renderGeneration(); pollGeneration(token); }
}
function positionInspector() {
  const inspector = document.querySelector('#lesson-inspector'); if (!inspector?.open) return;
  const anchor = [...document.querySelectorAll('.demo-lesson')].find(element => element.dataset.lessonId === selectedLesson);
  if (!anchor) return;
  const rect = anchor.getBoundingClientRect(), width = Math.min(390, window.innerWidth - 24);
  inspector.style.left = `${Math.max(12, Math.min(rect.right + 10, window.innerWidth - width - 12))}px`;
  const bar = document.querySelector('#timetable-save-bar'), bottom = bar.hidden ? 20 : window.innerHeight - bar.getBoundingClientRect().top + 12;
  inspector.style.top = `${Math.max(12, Math.min(rect.top, window.innerHeight - Math.min(inspector.scrollHeight, window.innerHeight - bottom - 24) - bottom))}px`;
}
function closeInspector() {
  const inspector = document.querySelector('#lesson-inspector'); if (!inspector?.open) { selectedLesson = null; return; }
  inspector.close();
  const previous = selectedLesson; selectedLesson = null;
  [...document.querySelectorAll('.demo-lesson')].find(e => e.dataset.lessonId === previous)?.focus({ preventScroll: true });
}
