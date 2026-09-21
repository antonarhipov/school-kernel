import { M } from './messages.js';

let etag;
let csrf;
let pollTimer;
let acceptedModel;

const view = {
  day: null, search: '', cohortId: '', teacherId: '', roomId: '', periodId: '',
  density: 'comfortable', selectedLessonId: null, focusedType: null, focusedId: null,
  narrow: window.matchMedia('(max-width: 700px)').matches
};

const stateCard = document.querySelector('#state-card');
const importCard = document.querySelector('#import-card');
const currentLabel = document.querySelector('#current-label');
const importStatus = document.querySelector('#import-status');

localizeShell();

async function load() {
  const [csrfResponse, workspaceResponse] = await Promise.all([fetch('/api/csrf'), fetch('/api/workspace')]);
  csrf = await csrfResponse.json();
  const snapshot = await workspaceResponse.json();
  etag = workspaceResponse.headers.get('ETag');
  render(snapshot);
}

function render(snapshot) {
  clearTimeout(pollTimer);
  const school = snapshot.workspace.school;
  if (snapshot.state === 'EMPTY') {
    currentLabel.textContent = M.noAccepted;
    stateCard.className = 'card';
    stateCard.innerHTML = `<span class="state">${M.emptyState}</span><h2>${M.emptyHeading}</h2><p>${M.emptyDetail}</p>`;
    importCard.hidden = false;
    return;
  }
  importCard.hidden = true;
  const schoolName = school?.displayName || M.nameUnavailable;
  if (snapshot.state === 'INITIAL_DRAFT') {
    currentLabel.textContent = M.noAccepted;
    const definition = snapshot.workspace.initialDefinition;
    const lastRun = snapshot.workspace.lastRun;
    stateCard.className = 'card';
    stateCard.innerHTML = `
      <span class="state">${M.initialDraft}</span><h2>${escapeHtml(schoolName)}</h2><p>${M.draftDetail}</p>
      ${summary(definition)}
      ${lastRun?.message ? `<p class="notice" role="status"><strong>${escapeHtml(lastRun.message)}</strong> ${M.noAcceptedAfterFailure}</p>` : ''}
      <p class="muted">${M.definitionRevision} <code>${escapeHtml(snapshot.workspace.definitionRevision)}</code></p>
      <div class="actions"><button id="start-plan">${M.createProposal}</button></div>
      <form id="replace-definition" class="replace-form"><label>${M.replaceDefinition} <input name="definition" type="file" accept="application/json,.json" required></label><button type="submit" class="secondary">${M.validateReplace}</button></form>`;
    bindInitialActions();
  } else if (snapshot.state === 'SOLVING_INITIAL') {
    currentLabel.textContent = M.noAccepted;
    const run = snapshot.workspace.run;
    stateCard.className = 'card';
    stateCard.innerHTML = `<span class="state running">${M.planning}</span><h2>${escapeHtml(schoolName)}</h2><p>${M.planningDetail}</p><dl><div><dt>${M.executionLimit}</dt><dd>${escapeHtml(run.limit)}</dd></div><div><dt>${M.status}</dt><dd>${M.running}</dd></div></dl><div class="actions"><button id="cancel-run" class="danger">${M.cancelRun}</button></div>`;
    document.querySelector('#cancel-run').addEventListener('click', () => cancelRun(run.id));
    pollTimer = setTimeout(load, 300);
  } else if (snapshot.state === 'INITIAL_PROPOSAL') {
    currentLabel.textContent = M.noAccepted;
    const proposal = snapshot.workspace.proposal;
    const result = proposal.result;
    stateCard.className = 'card';
    stateCard.innerHTML = `<span class="state proposal">${M.initialProposal}</span><h2>${escapeHtml(schoolName)}</h2><p>${M.proposalDetail}</p>
      <dl><div><dt>${M.lessons}</dt><dd>${result.timetable.assignments.length}</dd></div><div><dt>${M.terminationReason}</dt><dd>${escapeHtml(result.terminationReason)}</dd></div><div><dt>${M.executionLimit}</dt><dd>${escapeHtml(proposal.limit)}</dd></div><div><dt>${M.timetableRevision}</dt><dd><code>${escapeHtml(proposal.proposedTimetableRevision)}</code></dd></div></dl>
      ${assignmentTable(result.timetable.assignments)}
      <label class="confirmation"><input id="confirm-accept" type="checkbox"> ${M.confirmInitial}</label><div class="actions"><button id="accept-proposal" disabled>${M.acceptCurrent}</button><button id="discard-proposal" class="secondary">${M.discardProposal}</button></div>`;
    const confirmation = document.querySelector('#confirm-accept');
    const accept = document.querySelector('#accept-proposal');
    confirmation.addEventListener('change', () => { accept.disabled = !confirmation.checked; });
    accept.addEventListener('click', () => mutate('/api/proposal/accept', 'POST'));
    document.querySelector('#discard-proposal').addEventListener('click', () => mutate('/api/proposal', 'DELETE'));
  } else {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeModel(snapshot.workspace.acceptedBaseline);
    if (!view.day || !acceptedModel.weekdays.includes(view.day)) view.day = acceptedModel.weekdays[0] || null;
    if (view.narrow && !view.focusedType) { view.focusedType = 'cohortId'; view.focusedId = acceptedModel.definition.cohorts[0]?.id || null; }
    renderAccepted(snapshot, schoolName);
  }
}

function makeModel(baseline) {
  const definition = baseline.definition;
  const assignments = baseline.result.timetable.assignments;
  const maps = Object.fromEntries(['subjects', 'teachers', 'cohorts', 'rooms', 'periods', 'lessons'].map(key => [key, new Map(definition[key].map(item => [item.id, item]))]));
  const weekdays = [...new Set(definition.periods.map(period => period.weekday))];
  const enriched = assignments.map(assignment => ({ ...assignment,
    subject: maps.subjects.get(assignment.subjectId), teacher: maps.teachers.get(assignment.teacherId), cohort: maps.cohorts.get(assignment.cohortId),
    room: maps.rooms.get(assignment.roomId), period: maps.periods.get(assignment.periodId), lesson: maps.lessons.get(assignment.lessonId) }));
  const assignmentMap = new Map(enriched.map(item => [item.lessonId, item]));
  return { definition, assignments: enriched, assignmentMap, maps, weekdays };
}

function renderAccepted(snapshot, schoolName) {
  stateCard.className = `card workspace-card density-${view.density}`;
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state accepted">✓ ${M.acceptedState}</span><h2>${escapeHtml(schoolName)}</h2></div><p class="mode-note">${view.narrow ? M.narrowNotice : M.desktopNotice}</p></div>
    <p>${M.acceptedDetail} ${M.inspectionIntro}</p><p class="muted revision">${M.timetableRevision} <code>${escapeHtml(snapshot.workspace.timetableRevision)}</code></p><h3 class="sr-only">${M.timetableDetails}</h3><div id="accepted-view"></div>`;
  if (view.focusedType || view.narrow) renderFocused(); else renderWholeSchool();
}

function renderWholeSchool() {
  const host = document.querySelector('#accepted-view');
  const model = acceptedModel;
  const dayPeriods = periodsForDay(view.day);
  const matches = filteredAssignments();
  const active = activeCriteria();
  const visibleCohorts = active.length === 0 ? model.definition.cohorts : model.definition.cohorts.filter(cohort => matches.some(item => item.cohortId === cohort.id));
  host.innerHTML = `<div class="inspection-toolbar" aria-label="${M.wholeSchool}">
      <label class="search-control"><span>${M.searchLabel}</span><input id="lesson-search" type="search" value="${escapeAttribute(view.search)}" placeholder="${M.searchPlaceholder}"></label>
      ${selectControl('weekday', M.weekdayLabel, model.weekdays.map(id => [id, M.days[id] || id]), view.day)}
      ${selectControl('cohort-filter', M.classFilter, [['', M.allClasses], ...options(model.definition.cohorts)], view.cohortId)}
      ${selectControl('teacher-filter', M.teacherFilter, [['', M.allTeachers], ...options(model.definition.teachers)], view.teacherId)}
      ${selectControl('room-filter', M.roomFilter, [['', M.allRooms], ...options(model.definition.rooms)], view.roomId)}
      ${selectControl('period-focus', M.periodFocus, [['', M.allPeriods], ...dayPeriods.map(period => [period.id, periodLabel(period)])], view.periodId)}
      <fieldset class="density-control"><legend>${M.density}</legend><button type="button" data-density="comfortable" aria-pressed="${view.density === 'comfortable'}">${M.comfortable}</button><button type="button" data-density="compact" aria-pressed="${view.density === 'compact'}">${M.compact}</button></fieldset>
      <button id="reset-view" type="button" class="secondary">${M.reset}</button></div>
    <div class="filter-status" role="status"><strong id="filter-title">${active.length ? M.filteredMatrix : M.completeMatrix}</strong><span id="matrix-summary">${M.matrixSummary(visibleCohorts.length, model.definition.cohorts.length)}</span><span class="criteria-label">${M.activeFilters}:</span><span id="active-criteria" class="criteria">${active.length ? active.map(item => `<span>${escapeHtml(item)}</span>`).join('') : M.noFilters}</span></div>
    ${model.assignments.length === 0 ? `<p class="empty-message" role="status">${M.emptyAccepted}</p>` : ''}
    <p id="no-matches" class="empty-message" role="status"${active.length && matches.length === 0 ? '' : ' hidden'}>${M.noMatches} <button id="reset-empty" type="button" class="link-button">${M.reset}</button></p>
    ${matrix(model.definition.cohorts, dayPeriods, model.assignments, false)}
    <div id="lesson-details-host">${view.selectedLessonId ? lessonDetails(model.assignmentMap.get(view.selectedLessonId)) : ''}</div>
    <div class="focused-entry"><h3>${M.focusedSchedules}</h3><button type="button" data-open-focus="cohortId" class="secondary">${M.openClass}</button><button type="button" data-open-focus="teacherId" class="secondary">${M.openTeacher}</button><button type="button" data-open-focus="roomId" class="secondary">${M.openRoom}</button></div>`;
  bindInspectionControls();
  applyFiltersInPlace();
}

function matrix(cohorts, periods, assignments, filtered) {
  const selectedPeriods = view.periodId ? periods.filter(period => period.id === view.periodId) : periods;
  const assignmentsByCell = new Map();
  for (const assignment of assignments) {
    const key = `${assignment.cohortId}\u0000${assignment.periodId}`;
    const cell = assignmentsByCell.get(key);
    if (cell) cell.push(assignment); else assignmentsByCell.set(key, [assignment]);
  }
  const headers = selectedPeriods.map(period => `<th scope="col"><span>${escapeHtml(period.displayName)}</span>${period.startTime ? `<small>${escapeHtml(M.optionalTime(period.startTime, period.endTime))}</small>` : ''}</th>`).join('');
  const rows = cohorts.map(cohort => `<tr><th scope="row"><span>${escapeHtml(entityName(cohort))}</span><small>${escapeHtml(cohort.id)}</small></th>${selectedPeriods.map(period => {
    const items = assignmentsByCell.get(`${cohort.id}\u0000${period.id}`) || [];
    return items.length ? `<td>${items.map(item => lessonButton(item, filtered)).join('')}<span class="empty-cell" hidden>${M.emptyCell}</span></td>` : `<td><span class="empty-cell">${M.emptyCell}</span></td>`;
  }).join('')}</tr>`).join('');
  return `<div class="matrix-wrap" tabindex="0" aria-label="${filtered ? M.filteredMatrix : M.completeMatrix}"><table class="matrix"><thead><tr><th scope="col">${M.class}</th>${headers}</tr></thead><tbody>${rows}</tbody></table></div>`;
}

function lessonButton(item, matched) {
  const selected = item.lessonId === view.selectedLessonId;
  return `<button type="button" class="lesson-cell${matched ? ' match' : ''}${selected ? ' selected' : ''}" data-lesson-id="${escapeAttribute(item.lessonId)}" aria-pressed="${selected}"><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(item.teacher, item.teacherId))}</span><span>${escapeHtml(entityName(item.room, item.roomId))}</span><em class="match-label"${matched ? '' : ' hidden'}>${M.match}</em><em class="selected-label"${selected ? '' : ' hidden'}>${M.selected}</em></button>`;
}

function lessonDetails(item) {
  if (!item) return '';
  return `<aside class="lesson-panel" aria-labelledby="lesson-panel-title"><div><span class="state accepted">✓ ${M.acceptedAssignment}</span><h3 id="lesson-panel-title" tabindex="-1">${escapeHtml(entityName(item.lesson, item.lessonId))}</h3></div><button id="close-details" type="button" class="secondary">${M.closeDetails}</button>
    <dl>${detail(M.subject, entityName(item.subject, item.subjectId))}${detail(M.class, entityName(item.cohort, item.cohortId))}${detail(M.teacher, entityName(item.teacher, item.teacherId))}${detail(M.period, entityName(item.period, item.periodId))}${detail(M.room, entityName(item.room, item.roomId))}</dl>
    <details><summary>${M.technicalDetails}</summary><p>${M.technicalMapping}</p><dl class="technical">${idDetail(M.lesson, item.lessonId)}${idDetail(M.subject, item.subjectId)}${idDetail(M.class, item.cohortId)}${idDetail(M.teacher, item.teacherId)}${idDetail(M.period, item.periodId)}${idDetail(M.room, item.roomId)}</dl></details></aside>`;
}

function renderFocused() {
  const host = document.querySelector('#accepted-view');
  const type = view.focusedType || 'cohortId';
  const source = type === 'cohortId' ? acceptedModel.definition.cohorts : type === 'teacherId' ? acceptedModel.definition.teachers : acceptedModel.definition.rooms;
  if (!source.some(item => item.id === view.focusedId)) view.focusedId = source[0]?.id || null;
  const title = type === 'cohortId' ? M.classSchedule : type === 'teacherId' ? M.teacherSchedule : M.roomSchedule;
  const choose = type === 'cohortId' ? M.chooseClass : type === 'teacherId' ? M.chooseTeacher : M.chooseRoom;
  const items = acceptedModel.assignments.filter(item => item[type] === view.focusedId).sort((a, b) => acceptedModel.weekdays.indexOf(a.period?.weekday) - acceptedModel.weekdays.indexOf(b.period?.weekday) || (a.period?.order ?? 0) - (b.period?.order ?? 0));
  host.innerHTML = `${view.narrow ? `<p class="narrow-banner" role="status">${M.narrowNotice}</p>` : `<button id="return-matrix" type="button" class="secondary">← ${M.returnWholeSchool}</button>`}
    <div class="focused-toolbar"><div class="view-tabs" role="group" aria-label="${M.focusedSchedules}"><button type="button" data-focus-type="cohortId" aria-pressed="${type === 'cohortId'}">${M.classes}</button><button type="button" data-focus-type="teacherId" aria-pressed="${type === 'teacherId'}">${M.teachers}</button><button type="button" data-focus-type="roomId" aria-pressed="${type === 'roomId'}">${M.rooms}</button></div>${selectControl('focus-entity', choose, options(source), view.focusedId)}</div>
    <section class="focused-schedule"><h3>${title} · ${escapeHtml(entityName(source.find(item => item.id === view.focusedId), view.focusedId))}</h3>${items.length ? acceptedModel.weekdays.map(day => focusedDay(day, items)).join('') : `<p class="empty-message">${M.noFocusedLessons}</p>`}</section>`;
  document.querySelector('#return-matrix')?.addEventListener('click', () => { view.focusedType = null; renderWholeSchool(); });
  document.querySelectorAll('[data-focus-type]').forEach(button => button.addEventListener('click', () => { view.focusedType = button.dataset.focusType; view.focusedId = null; renderFocused(); }));
  document.querySelector('#focus-entity')?.addEventListener('change', event => { view.focusedId = event.target.value; renderFocused(); });
}

function focusedDay(day, items) {
  const dayItems = items.filter(item => item.period?.weekday === day);
  return `<div class="focused-day"><h4>${escapeHtml(M.days[day] || day)}</h4>${dayItems.length ? dayItems.map(item => `<article class="focused-lesson"><time>${escapeHtml(periodLabel(item.period))}</time><div><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(item.cohort, item.cohortId))} · ${escapeHtml(entityName(item.teacher, item.teacherId))} · ${escapeHtml(entityName(item.room, item.roomId))}</span></div><span class="accepted-text">✓ ${M.acceptedAssignment}</span></article>`).join('') : `<p class="empty-cell">${M.emptyCell}</p>`}</div>`;
}

function bindInspectionControls() {
  const rerender = () => renderWholeSchool();
  document.querySelector('#lesson-search').addEventListener('input', event => { view.search = event.target.value; applyFiltersInPlace(); });
  for (const [id, key] of [['cohort-filter', 'cohortId'], ['teacher-filter', 'teacherId'], ['room-filter', 'roomId']]) document.querySelector(`#${id}`).addEventListener('change', event => { view[key] = event.target.value; applyFiltersInPlace(); });
  for (const [id, key] of [['weekday', 'day'], ['period-focus', 'periodId']]) document.querySelector(`#${id}`).addEventListener('change', event => { view[key] = event.target.value; if (key === 'day') view.periodId = ''; rerender(); });
  document.querySelectorAll('[data-density]').forEach(button => button.addEventListener('click', () => { view.density = button.dataset.density; stateCard.className = `card workspace-card density-${view.density}`; rerender(); }));
  document.querySelector('#reset-view').addEventListener('click', resetView);
  document.querySelector('#reset-empty')?.addEventListener('click', resetView);
  document.querySelectorAll('[data-lesson-id]').forEach(button => button.addEventListener('click', () => selectLesson(button)));
  bindCloseDetails();
  document.querySelectorAll('[data-open-focus]').forEach(button => button.addEventListener('click', () => {
    view.focusedType = button.dataset.openFocus;
    const key = view.focusedType === 'cohortId' ? 'cohorts' : view.focusedType === 'teacherId' ? 'teachers' : 'rooms';
    view.focusedId = view[view.focusedType] || acceptedModel.definition[key][0]?.id || null;
    renderFocused();
  }));
}

function selectLesson(button) {
  view.selectedLessonId = button.dataset.lessonId;
  document.querySelectorAll('[data-lesson-id]').forEach(candidate => {
    const selected = candidate === button;
    candidate.classList.toggle('selected', selected);
    candidate.setAttribute('aria-pressed', String(selected));
    candidate.querySelector('.selected-label').hidden = !selected;
  });
  document.querySelector('#lesson-details-host').innerHTML = lessonDetails(acceptedModel.assignmentMap.get(view.selectedLessonId));
  bindCloseDetails();
  document.querySelector('#lesson-panel-title')?.focus();
}

function bindCloseDetails() {
  document.querySelector('#close-details')?.addEventListener('click', () => {
    view.selectedLessonId = null;
    document.querySelector('#lesson-details-host').replaceChildren();
    document.querySelectorAll('[data-lesson-id]').forEach(candidate => {
      candidate.classList.remove('selected');
      candidate.setAttribute('aria-pressed', 'false');
      candidate.querySelector('.selected-label').hidden = true;
    });
  });
}

function applyFiltersInPlace() {
  const active = activeCriteria();
  let matchingLessons = 0;
  let visibleRows = 0;
  document.querySelectorAll('.lesson-cell[data-lesson-id]').forEach(button => {
    const item = acceptedModel.assignmentMap.get(button.dataset.lessonId);
    const matches = assignmentMatches(item);
    button.hidden = active.length > 0 && !matches;
    button.classList.toggle('match', active.length > 0 && matches);
    button.querySelector('.match-label').hidden = active.length === 0 || !matches;
    if (matches) matchingLessons++;
  });
  document.querySelectorAll('.matrix tbody tr').forEach(row => {
    const hasMatch = Boolean(row.querySelector('.lesson-cell:not([hidden])'));
    row.hidden = active.length > 0 && !hasMatch;
    if (!row.hidden) visibleRows++;
    row.querySelectorAll('td').forEach(cell => {
      const visibleLesson = cell.querySelector('.lesson-cell:not([hidden])');
      const empty = cell.querySelector('.empty-cell');
      if (empty) empty.hidden = Boolean(visibleLesson);
    });
  });
  document.querySelector('#filter-title').textContent = active.length ? M.filteredMatrix : M.completeMatrix;
  document.querySelector('#matrix-summary').textContent = M.matrixSummary(visibleRows, acceptedModel.definition.cohorts.length);
  document.querySelector('#active-criteria').innerHTML = active.length ? active.map(item => `<span>${escapeHtml(item)}</span>`).join('') : M.noFilters;
  document.querySelector('#no-matches').hidden = !(active.length > 0 && matchingLessons === 0);
  document.querySelector('.matrix-wrap').setAttribute('aria-label', active.length ? M.filteredMatrix : M.completeMatrix);
}

function resetView() { view.search = ''; view.cohortId = ''; view.teacherId = ''; view.roomId = ''; view.periodId = ''; view.selectedLessonId = null; renderWholeSchool(); }

function filteredAssignments() {
  return acceptedModel.assignments.filter(assignmentMatches);
}

function assignmentMatches(item) {
  const query = view.search.trim().toLocaleLowerCase();
  return (!view.cohortId || item.cohortId === view.cohortId) && (!view.teacherId || item.teacherId === view.teacherId) && (!view.roomId || item.roomId === view.roomId) && (!view.periodId || item.periodId === view.periodId) && (!query || searchable(item).some(value => value.toLocaleLowerCase().includes(query)));
}

function searchable(item) {
  return [item.lessonId, item.subjectId, item.cohortId, item.teacherId, item.periodId, item.roomId, item.lesson?.displayName, item.subject?.displayName, item.cohort?.displayName, item.teacher?.displayName, item.period?.displayName, item.room?.displayName].filter(Boolean).map(String);
}

function activeCriteria() {
  const criteria = [];
  if (view.search.trim()) criteria.push(M.searchCriterion(view.search.trim()));
  if (view.cohortId) criteria.push(M.classCriterion(entityName(acceptedModel.maps.cohorts.get(view.cohortId), view.cohortId)));
  if (view.teacherId) criteria.push(M.teacherCriterion(entityName(acceptedModel.maps.teachers.get(view.teacherId), view.teacherId)));
  if (view.roomId) criteria.push(M.roomCriterion(entityName(acceptedModel.maps.rooms.get(view.roomId), view.roomId)));
  if (view.periodId) criteria.push(M.periodCriterion(entityName(acceptedModel.maps.periods.get(view.periodId), view.periodId)));
  return criteria;
}

function periodsForDay(day) { return acceptedModel.definition.periods.filter(period => period.weekday === day).sort((a, b) => a.order - b.order); }
function options(items) { return items.map(item => [item.id, entityName(item)]); }
function entityName(item, fallback = '') { return item?.displayName || `${fallback} (${M.nameUnavailable})`; }
function periodLabel(period) { return period ? `${period.displayName}${period.startTime ? ` · ${M.optionalTime(period.startTime, period.endTime)}` : ''}` : M.nameUnavailable; }
function detail(label, value) { return `<div><dt>${label}</dt><dd>${escapeHtml(value)}</dd></div>`; }
function idDetail(label, value) { return detail(M.idLabel(label), value); }
function selectControl(id, label, values, selected) { return `<label><span>${label}</span><select id="${id}">${values.map(([value, text]) => `<option value="${escapeAttribute(value)}"${value === selected ? ' selected' : ''}>${escapeHtml(text)}</option>`).join('')}</select></label>`; }

function summary(definition) { return `<dl><div><dt>${M.lessons}</dt><dd>${definition.lessons.length}</dd></div><div><dt>${M.teachers}</dt><dd>${definition.teachers.length}</dd></div><div><dt>${M.classes}</dt><dd>${definition.cohorts.length}</dd></div><div><dt>${M.rooms}</dt><dd>${definition.rooms.length}</dd></div><div><dt>${M.weeklyPeriods}</dt><dd>${definition.periods.length}</dd></div></dl>`; }

function assignmentTable(assignments) {
  const rows = assignments.map(item => `<tr><td>${escapeHtml(item.lessonId)}</td><td>${escapeHtml(item.subjectId)}</td><td>${escapeHtml(item.cohortId)}</td><td>${escapeHtml(item.teacherId)}</td><td>${escapeHtml(item.periodId)}</td><td>${escapeHtml(item.roomId)}</td></tr>`).join('');
  return `<div class="table-wrap"><table><caption>${M.timetableDetails}</caption><thead><tr><th>${M.lesson}</th><th>${M.subject}</th><th>${M.class}</th><th>${M.teacher}</th><th>${M.period}</th><th>${M.room}</th></tr></thead><tbody>${rows}</tbody></table></div>`;
}

function bindInitialActions() {
  document.querySelector('#start-plan').addEventListener('click', () => mutate('/api/runs', 'POST'));
  document.querySelector('#replace-definition').addEventListener('submit', async event => { event.preventDefault(); await mutate('/api/initial-draft/replace', 'POST', new FormData(event.currentTarget)); });
}
async function cancelRun(id) { await mutate(`/api/runs/${encodeURIComponent(id)}`, 'DELETE'); }

async function mutate(path, method, body) {
  const response = await fetch(path, { method, headers: { [csrf.headerName]: csrf.token, 'If-Match': etag }, body });
  const result = await response.json();
  if (!response.ok) { stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${escapeHtml(result.message || M.actionFailed)}</p>`); if (response.headers.get('ETag')) etag = response.headers.get('ETag'); return; }
  etag = response.headers.get('ETag'); render(result);
}

async function submit(form) {
  importStatus.className = ''; importStatus.textContent = M.verifying;
  const button = form.querySelector('button'); button.disabled = true;
  try {
    const response = await fetch('/api/import', { method: 'POST', headers: { [csrf.headerName]: csrf.token, 'If-Match': etag }, body: new FormData(form) });
    const body = await response.json();
    if (!response.ok) throw new Error(body.message || M.actionFailed);
    etag = response.headers.get('ETag'); render(body); importStatus.textContent = '';
  } catch (error) { importStatus.className = 'error'; importStatus.textContent = error.message; }
  finally { button.disabled = false; }
}

function localizeShell() {
  document.title = M.documentTitle;
  const text = { eyebrow: M.eyebrow, 'page-title': M.pageTitle, 'current-label': M.loading, 'import-heading': M.importHeading, 'import-intro': M.importIntro, 'json-legend': M.jsonDocuments, 'definition-label': M.schoolDefinition, 'result-label': M.matchingResult, 'json-submit': M.importJson, 'archive-legend': M.acceptedArchive, 'archive-label': M.acceptedBundle, 'archive-submit': M.importBundle };
  for (const [id, value] of Object.entries(text)) document.querySelector(`#${id}`).textContent = value;
}

document.querySelector('#json-import').addEventListener('submit', event => { event.preventDefault(); submit(event.currentTarget); });
document.querySelector('#archive-import').addEventListener('submit', event => { event.preventDefault(); submit(event.currentTarget); });
window.matchMedia('(max-width: 700px)').addEventListener('change', event => {
  view.narrow = event.matches;
  if (acceptedModel) {
    if (event.matches && !view.focusedType) { view.focusedType = 'cohortId'; view.focusedId = acceptedModel.definition.cohorts[0]?.id || null; }
    if (!event.matches) view.focusedType = null;
    load();
  }
});

function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character]); }
function escapeAttribute(value) { return escapeHtml(value); }

load().catch(() => { currentLabel.textContent = M.unavailable; stateCard.innerHTML = `<p class="error">${M.unavailableDetail}</p>`; });
