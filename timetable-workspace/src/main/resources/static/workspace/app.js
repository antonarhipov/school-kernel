import { M } from './messages.js';
import { makeAcceptedModel } from './accepted-model.js';

let etag;
let csrf;
let pollTimer;
let acceptedModel;
let proposedModel;
let currentSnapshot;
let bulkPreview;
let dayMatrices = new Map();
let preferenceSchoolId;
const boundLessonButtons = new WeakSet();

const view = {
  day: null, search: '', cohortId: '', teacherId: '', roomId: '', periodId: '',
  range: 'WEEK', selectedLessonId: null, focusedType: null, focusedId: null,
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
  currentSnapshot = snapshot;
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
  } else if (snapshot.state === 'ACCEPTED_BASELINE') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeAcceptedModel(snapshot.workspace.acceptedBaseline);
    initializeInspectionState(snapshot.workspace.school?.id);
    if (view.narrow && !view.focusedType) { view.focusedType = 'cohortId'; view.focusedId = acceptedModel.definition.cohorts[0]?.id || null; }
    renderAccepted(snapshot, schoolName);
  } else if (snapshot.state === 'REPAIR_DRAFT') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeModel(snapshot.workspace.acceptedBaseline);
    if (!view.day || !acceptedModel.weekdays.includes(view.day)) view.day = acceptedModel.weekdays[0] || null;
    renderRepair(snapshot, schoolName);
  } else if (snapshot.state === 'SOLVING_REPAIR') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeModel(snapshot.workspace.acceptedBaseline);
    if (!view.day || !acceptedModel.weekdays.includes(view.day)) view.day = acceptedModel.weekdays[0] || null;
    const run = snapshot.workspace.run;
    stateCard.className = 'card workspace-card compact-density';
    stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state running">${M.repairRunning}</span><h2>${escapeHtml(schoolName)}</h2></div><p class="mode-note">${M.acceptedStillCurrent}</p></div><p>${M.repairRunningDetail}</p><dl><div><dt>${M.executionLimit}</dt><dd>${escapeHtml(run.limit)}</dd></div><div><dt>${M.status}</dt><dd>${M.running}</dd></div></dl><div class="actions"><button id="cancel-run" class="danger">${M.cancelRun}</button></div><div id="accepted-view"></div>`;
    document.querySelector('#cancel-run').addEventListener('click', () => cancelRun(run.id));
    renderWholeSchool();
    pollTimer = setTimeout(load, 300);
  } else if (snapshot.state === 'REPAIR_PROPOSAL') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeModel(snapshot.workspace.acceptedBaseline);
    proposedModel = makeModel({ definition: snapshot.workspace.proposal.definition, result: snapshot.workspace.proposal.result });
    renderRepairProposal(snapshot, schoolName);
  }
}

function renderRepairProposal(snapshot, schoolName) {
  const reviewStarted = performance.now();
  const proposal = snapshot.workspace.proposal;
  if (view.narrow) {
    stateCard.className = 'card workspace-card';
    stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state proposal">${M.repairProposal}</span><h2>${escapeHtml(schoolName)}</h2></div></div><p class="narrow-banner">${M.narrowNotice}</p><div id="accepted-view"></div>`;
    if (!view.focusedType) { view.focusedType = 'cohortId'; view.focusedId = acceptedModel.definition.cohorts[0]?.id || null; }
    renderFocused(); return;
  }
  const review = proposal.review;
  const categoryHtml = review.categories.map(category => `<section class="review-category" data-category="${escapeAttribute(category.id)}"><h4>${escapeHtml(M[category.id])} <span>${category.count}</span></h4>${category.lessonIds.length ? `<ul>${category.lessonIds.map(id => `<li><button type="button" class="link-button" data-review-lesson="${escapeAttribute(id)}">${escapeHtml(reviewLessonName(id))}</button></li>`).join('')}</ul>` : `<p>${M.emptyCategory}</p>`}</section>`).join('');
  const changedHtml = review.changedLessons.map(change => reviewLessonCard(change)).join('');
  const unchanged = acceptedModel.assignments.filter(item => !review.changedLessons.some(change => change.lessonId === item.lessonId));
  stateCard.className = 'card workspace-card review-mode';
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state proposal">${M.repairProposal}</span><h2>${escapeHtml(schoolName)}</h2></div><p class="mode-note">${M.acceptedStillCurrent}</p></div><p>${M.repairProposalDetail}</p><p class="notice">${M.repairPriority}</p>
    <dl class="proposal-facts"><div><dt>${M.uniqueChangedLessons}</dt><dd>${review.uniqueChangedLessonCount}</dd></div><div><dt>${M.periodMoves}</dt><dd>${categoryCount(review, 'periodMoves')}</dd></div><div><dt>${M.roomOnlyMoves}</dt><dd>${categoryCount(review, 'roomOnlyMoves')}</dd></div><div><dt>${M.forcedChanges}</dt><dd>${categoryCount(review, 'forcedMoves')}</dd></div><div><dt>${M.terminationReason}</dt><dd>${escapeHtml(proposal.terminationReason)}</dd></div><div><dt>${M.executionLimit}</dt><dd>${escapeHtml(proposal.limit)}</dd></div><div><dt>${M.elapsedTime}</dt><dd>${M.milliseconds(proposal.elapsedTimeMs)}</dd></div></dl>
    <section aria-labelledby="impact-title"><h3 id="impact-title">${M.proposalImpact}</h3><div class="impact-totals"><span class="direct-label">${M.directEffectChanges}: ${review.directEffectChangedCount}</span><span class="ripple-label">${M.rippleEffectChanges}: ${review.rippleEffectCount}</span></div>
    <h3>${M.changeCategories}</h3><div class="review-categories">${categoryHtml}</div>
    <h3>${M.impactGroups}</h3>${reviewGroup('classes', M.groupClasses)}${reviewGroup('teachers', M.groupTeachers)}${reviewGroup('rooms', M.groupRooms)}${reviewGroup('days', M.groupDays)}
    <h3>${M.changedLessonDetails}</h3><div class="changed-lessons">${changedHtml || `<p>${M.emptyCategory}</p>`}</div></section>
    ${unchanged.length ? `<section class="unchanged-review"><h3>${M.unchangedLesson}</h3><div class="actions"><select id="unchanged-lesson">${unchanged.map(item => `<option value="${escapeAttribute(item.lessonId)}">${escapeHtml(entityName(item.lesson, item.lessonId))}</option>`).join('')}</select><button type="button" id="show-unchanged" class="secondary">${M.showUnchanged}</button></div><div id="unchanged-host"></div></section>` : ''}
    <p class="acceptance-warning"><strong>${M.acceptanceAdvancesBaseline}</strong></p><label class="confirmation"><input id="confirm-repair-accept" type="checkbox"> ${M.confirmRepairAcceptance}</label><div class="actions"><button id="accept-repair" disabled>${M.acceptRepair}</button><button id="revise-proposal" class="secondary">${M.reviseIntent}</button><button id="discard-proposal" class="danger">${M.discardRepairProposal}</button></div>`;
  bindRepairReview();
  window.__workspaceProposalReviewMs = performance.now() - reviewStarted;
}

function categoryCount(review, id) { return review.categories.find(category => category.id === id)?.count || 0; }
function reviewLessonName(id) { return entityName(proposedModel.maps.lessons.get(id) || acceptedModel.maps.lessons.get(id), id); }
function reviewGroup(key, title) {
  const groups = currentSnapshot.workspace.proposal.review.groupings[key];
  const label = value => value === 'OLD' ? M.oldContext : value === 'PROPOSED' ? M.proposedContext : M.bothContexts;
  const map = key === 'classes' ? proposedModel.maps.cohorts : key === 'teachers' ? proposedModel.maps.teachers : key === 'rooms' ? proposedModel.maps.rooms : null;
  return `<details class="review-groups"><summary>${title} · ${groups.length}</summary>${groups.length ? `<ul>${groups.map(group => `<li><strong>${escapeHtml(key === 'days' ? (M.days[group.id] || group.id) : entityName(map?.get(group.id) || acceptedGroupEntity(key, group.id), group.id))}</strong> <span class="context-label">${label(group.context)}</span> · ${group.lessonIds.length}</li>`).join('')}</ul>` : `<p>${M.emptyCategory}</p>`}</details>`;
}
function acceptedGroupEntity(key, id) { const map = key === 'classes' ? acceptedModel.maps.cohorts : key === 'teachers' ? acceptedModel.maps.teachers : acceptedModel.maps.rooms; return map.get(id); }

function reviewLessonCard(change) {
  const direct = change.directEffect ? `<span class="direct-label">${M.directEffectChanges}</span>` : `<span class="ripple-label">${M.rippleEffectChanges}</span>`;
  return `<article class="change-card" id="review-${escapeAttribute(change.lessonId)}"><header><h4>${escapeHtml(reviewLessonName(change.lessonId))}</h4>${direct}</header><div class="before-after">${reviewSide(M.oldAssignment, change.old, change.changedDimensions, acceptedModel)}${reviewSide(M.proposedAssignment, change.proposed, change.changedDimensions, proposedModel)}</div><div class="context-actions"><button type="button" class="secondary" data-review-context="${escapeAttribute(change.lessonId)}">${M.openWholeContext}</button><button type="button" class="secondary" data-review-focus="cohortId" data-review-context="${escapeAttribute(change.lessonId)}">${M.openClassContext}</button><button type="button" class="secondary" data-review-focus="teacherId" data-review-context="${escapeAttribute(change.lessonId)}">${M.openTeacherContext}</button><button type="button" class="secondary" data-review-focus="roomId" data-review-context="${escapeAttribute(change.lessonId)}">${M.openRoomContext}</button></div></article>`;
}

function reviewSide(title, side, changed, model) {
  if (!side) return `<section><h5>${title}</h5><p>${M.notPresent}</p></section>`;
  const values = [['subjectId', M.subject, model.maps.subjects], ['cohortId', M.class, model.maps.cohorts], ['teacherId', M.teacher, model.maps.teachers], ['periodId', M.period, model.maps.periods], ['roomId', M.room, model.maps.rooms]];
  return `<section><h5>${title}</h5><dl>${values.map(([field, label, map]) => `<div class="${changed.includes(field) ? 'changed-dimension' : ''}"><dt>${label}${changed.includes(field) ? ` · ${M.changedDimension}` : ''}</dt><dd>${escapeHtml(entityName(map.get(side[field]), side[field] || ''))}</dd></div>`).join('')}</dl></section>`;
}

function bindRepairReview() {
  const confirmation = document.querySelector('#confirm-repair-accept'); const accept = document.querySelector('#accept-repair');
  confirmation.addEventListener('change', () => { accept.disabled = !confirmation.checked; });
  accept.addEventListener('click', () => mutate('/api/proposal/accept', 'POST'));
  document.querySelector('#revise-proposal').addEventListener('click', () => mutate('/api/proposal', 'DELETE'));
  document.querySelector('#discard-proposal').addEventListener('click', () => mutate('/api/proposal', 'DELETE'));
  document.querySelectorAll('[data-review-lesson]').forEach(button => button.addEventListener('click', () => { document.querySelector(`#review-${CSS.escape(button.dataset.reviewLesson)}`)?.scrollIntoView(); document.querySelector(`#review-${CSS.escape(button.dataset.reviewLesson)} h4`)?.focus(); }));
  document.querySelectorAll('[data-review-context]').forEach(button => button.addEventListener('click', () => showReviewContext(button.dataset.reviewContext, button.dataset.reviewFocus)));
  document.querySelector('#show-unchanged')?.addEventListener('click', () => { const item = acceptedModel.assignmentMap.get(document.querySelector('#unchanged-lesson').value); document.querySelector('#unchanged-host').innerHTML = `<p class="quiet-state">${M.visuallyQuiet}</p>${lessonDetails(item)}`; bindCloseDetails(); });
}

function showReviewContext(lessonId, focusType) {
  const change = currentSnapshot.workspace.proposal.review.changedLessons.find(item => item.lessonId === lessonId);
  const side = change?.proposed || change?.old;
  const model = change?.proposed ? proposedModel : acceptedModel;
  const item = model.assignmentMap.get(lessonId);
  stateCard.innerHTML = `<button id="return-review" type="button" class="secondary">← ${M.returnImpactReview}</button><h2>${M.reviewContext}</h2><p class="mode-note">${M.acceptedStillCurrent}</p><div id="review-context-host"></div>`;
  const host = document.querySelector('#review-context-host');
  if (focusType && side) {
    const field = focusType; const selected = side[field];
    const items = model.assignments.filter(candidate => candidate[field] === selected);
    host.innerHTML = `<h3>${escapeHtml(entityName((field === 'cohortId' ? model.maps.cohorts : field === 'teacherId' ? model.maps.teachers : model.maps.rooms).get(selected), selected))}</h3>${items.map(candidate => `<p>${escapeHtml(reviewLessonName(candidate.lessonId))} · ${escapeHtml(periodLabel(candidate.period))}</p>`).join('')}`;
  } else host.innerHTML = item ? lessonDetails(item) : `<p>${M.notPresent}</p>`;
  document.querySelector('#return-review').addEventListener('click', () => renderRepairProposal(currentSnapshot, currentSnapshot.workspace.school.displayName));
}

function makeModel(baseline) {
  const definition = baseline.definition;
  const maps = Object.fromEntries(['subjects', 'teachers', 'cohorts', 'rooms', 'periods', 'lessons']
    .map(key => [key, new Map(definition[key].map(item => [item.id, item]))]));
  const weekdays = [...new Set(definition.periods.map(period => period.weekday))];
  const assignments = baseline.result.timetable.assignments.map(assignment => ({ ...assignment,
    subject: maps.subjects.get(assignment.subjectId), teacher: maps.teachers.get(assignment.teacherId), cohort: maps.cohorts.get(assignment.cohortId),
    room: maps.rooms.get(assignment.roomId), period: maps.periods.get(assignment.periodId), lesson: maps.lessons.get(assignment.lessonId) }));
  const assignmentMap = new Map(assignments.map(item => [item.lessonId, item]));
  const assignmentsByCell = new Map();
  for (const assignment of assignments) {
    const key = `${assignment.cohortId}\u0000${assignment.periodId}`;
    const cell = assignmentsByCell.get(key);
    if (cell) cell.push(assignment); else assignmentsByCell.set(key, [assignment]);
  }
  return { definition, assignments, assignmentMap, assignmentsByCell, maps, weekdays };
}

function preferenceKey(schoolId) { return `school-kernel.inspection.v1.${schoolId}`; }

function initializeInspectionState(schoolId) {
  const firstDay = acceptedModel.weekdays[0] || null;
  if (preferenceSchoolId === schoolId && view.day && acceptedModel.weekdays.includes(view.day)) return;
  preferenceSchoolId = schoolId;
  view.range = 'WEEK';
  view.day = firstDay;
  if (!schoolId || !firstDay) return;
  try {
    const raw = window.localStorage.getItem(preferenceKey(schoolId));
    if (!raw || raw.length > 1024) return;
    const preference = JSON.parse(raw);
    if (Object.keys(preference).length !== 3 || preference.version !== 1
        || !['WEEK', 'DAY'].includes(preference.range)
        || typeof preference.weekdayId !== 'string'
        || !acceptedModel.weekdays.includes(preference.weekdayId)) return;
    view.range = preference.range;
    view.day = preference.weekdayId;
  } catch (_) {
    // Browser storage is an optional presentation enhancement, never workspace authority.
  }
}

function persistInspectionPreference() {
  if (!preferenceSchoolId || !view.day) return true;
  try {
    window.localStorage.setItem(preferenceKey(preferenceSchoolId), JSON.stringify({ version: 1, range: view.range, weekdayId: view.day }));
    return true;
  } catch (_) { return false; }
}

function setRange(range, day = view.day, manualDay = false) {
  if (!['WEEK', 'DAY'].includes(range) || !acceptedModel.weekdays.includes(day)) return;
  const hadSelection = Boolean(view.selectedLessonId);
  let selectionCleared = false;
  view.range = range;
  view.day = day;
  if (manualDay && view.selectedLessonId) {
    const selected = acceptedModel.assignmentMap.get(view.selectedLessonId);
    if (selected?.period?.weekday !== day) { view.selectedLessonId = null; selectionCleared = true; }
  }
  const persisted = persistInspectionPreference();
  renderWholeSchool();
  if (manualDay && hadSelection && selectionCleared) {
    const notice = document.querySelector('#inspection-notice');
    if (notice) notice.textContent = M.lessonOutsideDay;
  }
  if (!persisted) {
    const notice = document.querySelector('#inspection-notice');
    if (notice) notice.textContent = M.preferenceUnavailable;
  }
}

function renderAccepted(snapshot, schoolName) {
  stateCard.className = 'card workspace-card compact-density';
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state accepted">✓ ${M.currentAccepted}</span><h2>${escapeHtml(schoolName)}</h2></div><p class="mode-note">${view.narrow ? M.narrowNotice : M.desktopNotice}</p></div>
    <p class="accepted-context">${M.acceptedState}</p>
    <p>${M.acceptedDetail} ${M.inspectionIntro}</p>${exportAccepted(snapshot)}
    ${view.narrow ? '' : startRepairForm()}<h3 class="sr-only">${M.timetableDetails}</h3><div id="accepted-view"></div>`;
  bindStartRepair();
  if (view.focusedType || view.narrow) renderFocused(); else renderWholeSchool();
}

function exportAccepted(snapshot) {
  const baseline = snapshot.workspace.acceptedBaseline;
  return `<section class="export-baseline" aria-labelledby="export-title"><h3 id="export-title">${M.exportAccepted}</h3><p>${M.exportDetail}</p><dl><div><dt>${M.school}</dt><dd>${escapeHtml(snapshot.workspace.school.displayName)}</dd></div><div><dt>${M.definitionRevision}</dt><dd><code>${escapeHtml(baseline.result.inputRevision)}</code></dd></div><div><dt>${M.timetableRevision}</dt><dd><code>${escapeHtml(baseline.result.timetableRevision)}</code></dd></div></dl><a id="export-accepted" class="button-link" href="/api/accepted/export" download="accepted-baseline.zip">${M.downloadAccepted}</a></section>`;
}

function startRepairForm() {
  const periods = acceptedModel.definition.periods;
  return `<details class="repair-entry"><summary>${M.startRepair}</summary><form id="start-repair-form">
    <p>${M.startRepairIntro}</p><div class="repair-grid">
    ${selectControl('repair-resource-type', M.resourceType, [['TEACHER', M.teacher], ['ROOM', M.room]], 'TEACHER')}
    <label><span>${M.resource}</span><select id="repair-resource"></select></label></div>
    <fieldset class="period-choices"><legend>${M.weeklyUnavailablePeriods}</legend>${periods.map(period => `<label><input type="checkbox" name="period" value="${escapeAttribute(period.id)}"> <span>${escapeHtml(periodLabel(period))} · ${escapeHtml(M.days[period.weekday] || period.weekday)}</span></label>`).join('')}</fieldset>
    <div class="actions"><button type="submit">${M.stageChange}</button></div><p class="muted">${M.unsupportedRepairActions}</p></form></details>`;
}

function bindStartRepair() {
  const form = document.querySelector('#start-repair-form');
  if (!form) return;
  const type = form.querySelector('#repair-resource-type');
  const resource = form.querySelector('#repair-resource');
  const fill = () => { const source = type.value === 'TEACHER' ? acceptedModel.definition.teachers : acceptedModel.definition.rooms; resource.innerHTML = options(source).map(([value, text]) => `<option value="${escapeAttribute(value)}">${escapeHtml(text)}</option>`).join(''); };
  fill(); type.addEventListener('change', fill);
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const periodIds = [...form.querySelectorAll('[name=period]:checked')].map(input => input.value);
    await mutateJson('/api/repair-draft', 'POST', { resourceType: type.value, resourceId: resource.value, periodIds });
  });
}

function renderRepair(snapshot, schoolName) {
  const draft = snapshot.workspace.repairDraft;
  stateCard.className = 'card workspace-card repair-mode compact-density';
  if (view.narrow) {
    stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state draft-state">${M.repairDraft}</span><h2>${escapeHtml(schoolName)}</h2></div></div><p class="narrow-banner">${M.narrowNotice}</p><div id="accepted-view"></div>`;
    renderFocused(); return;
  }
  const changes = draft.intent.changes.map(change => repairChange(change)).join('');
  const lastRun = snapshot.workspace.lastRun;
  const retryAvailable = lastRun?.kind === 'REPAIR' && lastRun.code === 'NO_FEASIBLE_SOLUTION_FOUND' && lastRun.intentRevision === draft.intentRevision;
  const runFeedback = lastRun?.kind === 'REPAIR' && lastRun.status !== 'FEASIBLE' ? repairRunFeedback(lastRun) : '';
  const conflicts = draft.conflicts.length ? `<div class="conflict-list" role="alert"><h3>${M.blockingConflicts}</h3>${draft.conflicts.map(item => `<p><strong>${escapeHtml(entityName(acceptedModel.maps.lessons.get(item.lessonId), item.lessonId))}</strong> · ${escapeHtml(item.message)}</p>`).join('')}</div>` : `<p class="ready-state">✓ ${M.readyToSolve}</p>`;
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state draft-state">${M.repairDraft}</span><h2>${escapeHtml(schoolName)}</h2></div><p class="mode-note">${M.acceptedStillCurrent}</p></div>
    <p>${M.repairDraftIntro}</p><div class="draft-summary"><section><h3>${M.weeklyChanges}</h3>${changes || `<p>${M.noWeeklyChanges}</p>`}</section><section><h3>${M.draftCounts}</h3><dl><div><dt>${M.directEffects}</dt><dd>${draft.directEffectLessonIds.length}</dd></div><div><dt>${M.attemptPins}</dt><dd id="attempt-pin-count">${draft.intent.pins.length}</dd></div><div><dt>${M.conflicts}</dt><dd id="draft-conflict-count">${draft.conflicts.length}</dd></div></dl></section></div>${conflicts}
    <div class="repair-controls"><h3>${M.bulkPin}</h3><div class="repair-grid">${selectControl('bulk-scope', M.bulkScope, [['DAY', M.day], ['CLASS', M.class], ['UNAFFECTED', M.allUnaffected]], 'UNAFFECTED')}<label><span>${M.scopeValue}</span><select id="bulk-scope-id"></select></label></div>${pinDimensionControls('bulk')}<button id="preview-bulk" type="button" class="secondary">${M.previewBulk}</button><div id="bulk-preview-host">${bulkPreviewHtml()}</div></div>
    <div class="repair-day"><label><span>${M.weekdayLabel}</span><select id="repair-weekday">${acceptedModel.weekdays.map(day => `<option value="${escapeAttribute(day)}"${day === view.day ? ' selected' : ''}>${escapeHtml(M.days[day] || day)}</option>`).join('')}</select></label></div>
    ${matrix(acceptedModel.definition.cohorts, periodsForDay(view.day), acceptedModel.assignmentsByCell, false)}
    <div id="lesson-details-host">${view.selectedLessonId ? lessonDetails(acceptedModel.assignmentMap.get(view.selectedLessonId)) : ''}</div>
    <div class="bulk-history">${draft.intent.bulkActions.map(action => `<p><span>${M.bulkApplied(action.lessonIds.length)}</span><button type="button" class="secondary" data-undo-bulk="${escapeAttribute(action.id)}">${M.undoBulk}</button></p>`).join('')}</div>
    ${runFeedback}<label class="confirmation"><input id="confirm-discard-draft" type="checkbox"> ${M.confirmDiscardDraft}</label><div class="actions"><button id="discard-draft" class="danger" disabled>${M.discardDraft}</button><button id="solve-draft"${draft.readyToSolve ? '' : ' disabled'}>${draft.readyToSolve ? M.createRepairProposal : M.resolveConflicts}</button>${retryAvailable ? `<button id="retry-repair" class="secondary">${M.retryRepair}</button>` : ''}</div>`;
  bindRepairControls();
}

function repairChange(change) {
  const map = change.resourceType === 'TEACHER' ? acceptedModel.maps.teachers : acceptedModel.maps.rooms;
  const periods = change.unavailablePeriodIds.map(id => entityName(acceptedModel.maps.periods.get(id), id)).join(', ');
  return `<p><span class="state direct-state">${M.directIntent}</span> <strong>${escapeHtml(entityName(map.get(change.resourceId), change.resourceId))}</strong> · ${escapeHtml(periods)}</p>`;
}

function pinDimensionControls(prefix) {
  return `<fieldset class="pin-dimensions"><legend>${M.pinDimensions}</legend><label><input type="checkbox" name="${prefix}-dimension" value="PERIOD" checked> ${M.acceptedPeriod}</label><label><input type="checkbox" name="${prefix}-dimension" value="ROOM"> ${M.acceptedRoom}</label></fieldset>`;
}

function bulkPreviewHtml() {
  if (!bulkPreview) return '';
  return `<section class="bulk-preview" aria-live="polite"><h4>${M.bulkPreview}</h4><p>${M.previewCount(bulkPreview.count)} · ${bulkPreview.dimensions.map(value => value === 'PERIOD' ? M.acceptedPeriod : M.acceptedRoom).join(', ')}</p><ul>${bulkPreview.lessonIds.map(id => `<li>${escapeHtml(entityName(acceptedModel.maps.lessons.get(id), id))}</li>`).join('')}</ul>${bulkPreview.conflicts.length ? `<p class="error">${M.previewConflicts(bulkPreview.conflicts.length)}</p>` : ''}<div class="actions"><button id="confirm-bulk" type="button">${M.confirmSnapshot}</button><button id="cancel-bulk" type="button" class="secondary">${M.cancelPreview}</button></div></section>`;
}

function bindRepairControls() {
  document.querySelector('#repair-weekday').addEventListener('change', event => { view.day = event.target.value; renderRepair(currentSnapshot, currentSnapshot.workspace.school.displayName); });
  document.querySelectorAll('[data-lesson-id]').forEach(button => button.addEventListener('click', () => selectLesson(button)));
  bindCloseDetails(); bindPinActions();
  const scope = document.querySelector('#bulk-scope'); const scopeId = document.querySelector('#bulk-scope-id');
  const fillScope = () => { const values = scope.value === 'DAY' ? acceptedModel.weekdays.map(day => [day, M.days[day] || day]) : scope.value === 'CLASS' ? options(acceptedModel.definition.cohorts) : [['', M.snapshotCurrentUnaffected]]; scopeId.innerHTML = values.map(([value, text]) => `<option value="${escapeAttribute(value)}">${escapeHtml(text)}</option>`).join(''); scopeId.disabled = scope.value === 'UNAFFECTED'; };
  fillScope(); scope.addEventListener('change', fillScope);
  document.querySelector('#preview-bulk').addEventListener('click', async () => {
    const dimensions = [...document.querySelectorAll('[name=bulk-dimension]:checked')].map(input => input.value);
    const payload = { scope: scope.value, dimensions }; if (scope.value !== 'UNAFFECTED') payload.scopeId = scopeId.value;
    const response = await commandJson('/api/repair-draft/bulk-pin-preview', 'POST', payload);
    if (response) { bulkPreview = response; renderRepair(currentSnapshot, currentSnapshot.workspace.school.displayName); }
  });
  document.querySelector('#confirm-bulk')?.addEventListener('click', () => { const preview = bulkPreview; bulkPreview = null; mutateJson('/api/repair-draft', 'PATCH', { action: 'CONFIRM_BULK_PIN', preview }); });
  document.querySelector('#cancel-bulk')?.addEventListener('click', () => { bulkPreview = null; renderRepair(currentSnapshot, currentSnapshot.workspace.school.displayName); });
  document.querySelectorAll('[data-undo-bulk]').forEach(button => button.addEventListener('click', () => mutateJson('/api/repair-draft', 'PATCH', { action: 'UNDO_BULK_PIN', bulkActionId: button.dataset.undoBulk })));
  const confirmation = document.querySelector('#confirm-discard-draft'); const discard = document.querySelector('#discard-draft');
  confirmation.addEventListener('change', () => { discard.disabled = !confirmation.checked; });
  discard.addEventListener('click', () => mutateJson('/api/repair-draft', 'DELETE', { confirmed: true }));
  document.querySelector('#solve-draft').addEventListener('click', () => mutateJson('/api/runs', 'POST', { limit: 'PT30S' }));
  document.querySelector('#retry-repair')?.addEventListener('click', () => mutateJson('/api/runs', 'POST', { limit: 'PT2M' }));
  document.querySelectorAll('[data-diagnostic-id]').forEach(button => button.addEventListener('click', () => {
    const id = button.dataset.diagnosticId;
    if (acceptedModel.assignmentMap.has(id)) { view.selectedLessonId = id; renderRepair(currentSnapshot, currentSnapshot.workspace.school.displayName); }
  }));
}

function repairRunFeedback(run) {
  const diagnostics = run.searchDiagnostics?.constraints || [];
  const rows = diagnostics.map(item => `<li><strong>${escapeHtml(item.constraintId)}</strong> · ${item.matchCount} matches ${item.examples.flat().map(id => `<button type="button" class="link-button" data-diagnostic-id="${escapeAttribute(id)}">${escapeHtml(id)}</button>`).join(' ')}</li>`).join('');
  const validation = run.validationReport?.errors || [];
  const validationRows = validation.map(item => `<li>${escapeHtml(item.message || item.code || M.actionFailed)} ${(item.entityIds || []).map(id => `<button type="button" class="link-button" data-diagnostic-id="${escapeAttribute(id)}">${escapeHtml(id)}</button>`).join(' ')}</li>`).join('');
  return `<section class="conflict-list" role="status"><h3>${M.runDiagnostics}</h3><p><strong>${escapeHtml(run.message || M.actionFailed)}</strong></p>${rows || validationRows ? `<ul>${rows}${validationRows}</ul>` : ''}<p>${M.diagnosticsCaution}</p></section>`;
}

function changeCountSummary(counts) {
  const categories = [['additions', M.additions], ['cancellations', M.cancellations], ['teacherChanges', M.teacherChanges], ['forcedMoves', M.forcedMoves], ['periodMoves', M.periodMoves], ['roomOnlyMoves', M.roomOnlyMoves]];
  return `<section><h3>${M.runDiagnostics}</h3><dl>${categories.map(([key, label]) => `<div><dt>${label}</dt><dd>${counts[key]}</dd></div>`).join('')}</dl></section>`;
}

function renderWholeSchool() {
  const host = document.querySelector('#accepted-view');
  const model = acceptedModel;
  const dayPeriods = periodsForDay(view.day);
  const active = activeCriteria();
  const matches = active.length ? filteredAssignments() : model.assignments;
  const visibleCohorts = active.length === 0 ? model.definition.cohorts : model.definition.cohorts.filter(cohort => matches.some(item => item.cohortId === cohort.id));
  host.innerHTML = `<div class="inspection-toolbar" aria-label="${M.wholeSchool}">
      <fieldset class="range-control"><legend>${M.rangeLabel}</legend><button type="button" data-range="WEEK" aria-pressed="${view.range === 'WEEK'}">${M.week}</button><button type="button" data-range="DAY" aria-pressed="${view.range === 'DAY'}">${M.dayView}</button></fieldset>
      <label class="search-control"><span>${M.searchLabel}</span><input id="lesson-search" type="search" value="${escapeAttribute(view.search)}" placeholder="${M.searchPlaceholder}"></label>
      ${view.range === 'DAY' ? `<button type="button" id="previous-day" class="secondary">${M.previousDay}</button>${selectControl('weekday', M.weekdayLabel, model.weekdays.map(id => [id, M.days[id] || id]), view.day)}<button type="button" id="next-day" class="secondary">${M.nextDay}</button>` : ''}
      ${selectControl('cohort-filter', M.classFilter, [['', M.allClasses], ...options(model.definition.cohorts)], view.cohortId)}
      ${selectControl('teacher-filter', M.teacherFilter, [['', M.allTeachers], ...options(model.definition.teachers)], view.teacherId)}
      ${selectControl('room-filter', M.roomFilter, [['', M.allRooms], ...options(model.definition.rooms)], view.roomId)}
      ${view.range === 'DAY' ? selectControl('period-focus', M.periodFocus, [['', M.allPeriods], ...dayPeriods.map(period => [period.id, periodLabel(period)])], view.periodId) : ''}
      <button id="reset-view" type="button" class="secondary">${M.reset}</button></div>
    <div class="filter-status" role="status"><strong id="filter-title">${active.length ? M.filteredMatrix : M.completePopulation}</strong><span id="range-summary">${view.range === 'WEEK' ? M.weekRange : M.dayRange(M.days[view.day] || view.day)}</span><span id="matrix-summary">${M.matrixSummary(visibleCohorts.length, model.definition.cohorts.length)}</span><span class="criteria-label">${M.activeFilters}:</span><span id="active-criteria" class="criteria">${active.length ? active.map(item => `<span>${escapeHtml(item)}</span>`).join('') : M.noFilters}</span></div>
    <p id="inspection-notice" class="notice" role="status"></p>
    ${model.assignments.length === 0 ? `<p class="empty-message" role="status">${M.emptyAccepted}</p>` : ''}
    <p id="no-matches" class="empty-message" role="status"${active.length && matches.length === 0 ? '' : ' hidden'}>${M.noMatches} <button id="reset-empty" type="button" class="link-button">${M.reset}</button></p>
    ${view.range === 'WEEK' ? weekMatrix(model.definition.cohorts, model.weekdays, model.assignmentsByCell) : matrix(model.definition.cohorts, dayPeriods, model.assignmentsByCell, false)}
    <div id="lesson-details-host">${view.selectedLessonId ? lessonDetails(model.assignmentMap.get(view.selectedLessonId)) : ''}</div>
    <div class="focused-entry"><h3>${M.focusedSchedules}</h3><button type="button" data-open-focus="cohortId" class="secondary">${M.openClass}</button><button type="button" data-open-focus="teacherId" class="secondary">${M.openTeacher}</button><button type="button" data-open-focus="roomId" class="secondary">${M.openRoom}</button></div>`;
  if (view.range === 'WEEK') { bindInspectionControls(); applyFiltersInPlace(); return; }
  dayMatrices = new Map([[view.day, host.querySelector('.matrix-wrap')]]);
  for (const day of model.weekdays) {
    if (day === view.day) continue;
    const template = document.createElement('template');
    template.innerHTML = matrix(model.definition.cohorts, periodsForDay(day), model.assignmentsByCell, false);
    dayMatrices.set(day, template.content.firstElementChild);
  }
  bindInspectionControls();
  applyFiltersInPlace();
}

function switchWholeSchoolDay(day) {
  view.periodId = '';
  setRange('DAY', day, true);
}

function weekMatrix(cohorts, weekdays, assignmentsByCell) {
  const headers = weekdays.map(day => `<th scope="col">${escapeHtml(M.days[day] || day)}</th>`).join('');
  const rows = cohorts.map(cohort => `<tr><th scope="row"><span>${escapeHtml(entityName(cohort))}</span><small>${escapeHtml(cohort.id)}</small></th>${weekdays.map(day => {
    const slots = periodsForDay(day).map(period => {
      const items = assignmentsByCell.get(`${cohort.id}\u0000${period.id}`) || [];
      return `<div class="week-slot"><span class="week-period">${escapeHtml(period.displayName)}</span>${items.length ? items.map(item => weekLessonButton(item)).join('') : `<span class="empty-cell">${M.emptyCell}</span>`}</div>`;
    }).join('');
    return `<td data-weekday="${escapeAttribute(day)}">${slots}</td>`;
  }).join('')}</tr>`).join('');
  return `<div class="matrix-wrap week-wrap" aria-label="${M.weekRange}"><table class="matrix week-matrix"><thead><tr><th scope="col">${M.class}</th>${headers}</tr></thead><tbody>${rows}</tbody></table></div>`;
}

function weekLessonButton(item) {
  const selected = item.lessonId === view.selectedLessonId;
  const label = [entityName(item.subject, item.subjectId), entityName(item.teacher, item.teacherId), entityName(item.room, item.roomId), entityName(item.cohort, item.cohortId), periodLabel(item.period), item.lessonId].join(' · ');
  return `<button type="button" class="lesson-cell week-lesson${selected ? ' selected' : ''}" data-lesson-id="${escapeAttribute(item.lessonId)}" aria-label="${escapeAttribute(label)}" aria-pressed="${selected}"><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span class="week-room">${escapeHtml(entityName(item.room, item.roomId))}</span><em class="selected-label"${selected ? '' : ' hidden'}>${M.selected}</em></button>`;
}

function matrix(cohorts, periods, assignmentsByCell, filtered) {
  const selectedPeriods = view.periodId ? periods.filter(period => period.id === view.periodId) : periods;
  const headers = selectedPeriods.map(period => `<th scope="col"><span>${escapeHtml(period.displayName)}</span>${period.startTime ? `<small>${escapeHtml(M.optionalTime(period.startTime, period.endTime))}</small>` : ''}</th>`).join('');
  const rows = cohorts.map(cohort => `<tr><th scope="row"><span>${escapeHtml(entityName(cohort))}</span><small>${escapeHtml(cohort.id)}</small></th>${selectedPeriods.map(period => {
    const items = assignmentsByCell.get(`${cohort.id}\u0000${period.id}`) || [];
    return items.length ? `<td>${items.map(item => lessonButton(item, filtered)).join('')}<span class="empty-cell" hidden>${M.emptyCell}</span></td>` : `<td><span class="empty-cell">${M.emptyCell}</span></td>`;
  }).join('')}</tr>`).join('');
  return `<div class="matrix-wrap" tabindex="0" aria-label="${filtered ? M.filteredMatrix : M.completeMatrix}"><table class="matrix"><thead><tr><th scope="col">${M.class}</th>${headers}</tr></thead><tbody>${rows}</tbody></table></div>`;
}

function lessonButton(item, matched) {
  const selected = item.lessonId === view.selectedLessonId;
  const draftState = repairLessonState(item.lessonId);
  return `<button type="button" class="lesson-cell${matched ? ' match' : ''}${selected ? ' selected' : ''}${draftState.direct ? ' directly-affected' : ''}${draftState.conflict ? ' conflicting' : ''}" data-lesson-id="${escapeAttribute(item.lessonId)}" aria-pressed="${selected}"><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(item.teacher, item.teacherId))}</span><span>${escapeHtml(entityName(item.room, item.roomId))}</span>${draftState.labels}<em class="match-label"${matched ? '' : ' hidden'}>${M.match}</em><em class="selected-label"${selected ? '' : ' hidden'}>${M.selected}</em></button>`;
}

function lessonDetails(item) {
  if (!item) return '';
  const draftState = repairLessonState(item.lessonId);
  const repairActions = currentSnapshot?.state === 'REPAIR_DRAFT' ? `<section class="pin-actions"><h4>${M.protectAcceptedAssignment}</h4>${pinDimensionControls('lesson')}<div class="actions"><button type="button" id="apply-pin">${M.applyPin}</button><button type="button" id="remove-pin" class="secondary">${M.removePin}</button></div><p>${draftState.labels || `<span class="state unpinned-state">${M.unpinned}</span>`}</p></section>` : '';
  return `<aside class="lesson-panel" aria-labelledby="lesson-panel-title"><div><span class="state accepted">✓ ${M.acceptedAssignment}</span><h3 id="lesson-panel-title" tabindex="-1">${escapeHtml(entityName(item.lesson, item.lessonId))}</h3></div><button id="close-details" type="button" class="secondary">${M.closeDetails}</button>
    <dl>${detail(M.subject, entityName(item.subject, item.subjectId))}${detail(M.class, entityName(item.cohort, item.cohortId))}${detail(M.teacher, entityName(item.teacher, item.teacherId))}${detail(M.period, entityName(item.period, item.periodId))}${detail(M.room, entityName(item.room, item.roomId))}</dl>
    ${repairActions}<details><summary>${M.technicalDetails}</summary><p>${M.technicalMapping}</p><dl class="technical">${idDetail(M.lesson, item.lessonId)}${idDetail(M.subject, item.subjectId)}${idDetail(M.class, item.cohortId)}${idDetail(M.teacher, item.teacherId)}${idDetail(M.period, item.periodId)}${idDetail(M.room, item.roomId)}</dl></details></aside>`;
}

function repairLessonState(lessonId) {
  if (currentSnapshot?.state !== 'REPAIR_DRAFT') return { direct: false, conflict: false, labels: '' };
  const draft = currentSnapshot.workspace.repairDraft;
  const direct = draft.directEffectLessonIds.includes(lessonId);
  const conflict = draft.conflicts.some(item => item.lessonId === lessonId);
  const pin = draft.intent.pins.find(item => item.lessonId === lessonId);
  const lesson = acceptedModel.maps.lessons.get(lessonId);
  const manifestLock = currentSnapshot.workspace.acceptedBaseline.manifest?.locks?.find(item => item.lessonId === lessonId);
  const labels = [];
  if (direct) labels.push(`<em class="direct-label">${M.directlyAffected}</em>`);
  if (conflict) labels.push(`<em class="conflict-label">${M.conflict}</em>`);
  if (lesson?.periodLock || manifestLock?.periodLockOrigin === 'PERSISTENT_POLICY') labels.push(`<em class="policy-label">${M.policyPeriodLock}</em>`);
  if (lesson?.roomLock || manifestLock?.roomLockOrigin === 'PERSISTENT_POLICY') labels.push(`<em class="policy-label">${M.policyRoomLock}</em>`);
  if (pin?.periodSources?.length) labels.push(`<em class="pin-label">${M.periodPinned}</em>`);
  if (pin?.roomSources?.length) labels.push(`<em class="pin-label">${M.roomPinned}</em>`);
  return { direct, conflict, labels: labels.join('') };
}

function bindPinActions() {
  const item = acceptedModel.assignmentMap.get(view.selectedLessonId);
  if (!item) return;
  const dimensions = () => [...document.querySelectorAll('[name=lesson-dimension]:checked')].map(input => input.value);
  document.querySelector('#apply-pin')?.addEventListener('click', () => mutatePin({ action: 'PIN', lessonId: item.lessonId, dimensions: dimensions() }));
  document.querySelector('#remove-pin')?.addEventListener('click', () => mutatePin({ action: 'UNPIN', lessonId: item.lessonId, dimensions: dimensions() }));
}

async function mutatePin(payload) {
  const response = await fetch('/api/repair-draft', { method: 'PATCH', headers: { [csrf.headerName]: csrf.token, 'If-Match': etag, 'Content-Type': 'application/json', 'Prefer': 'return=minimal' }, body: JSON.stringify(payload) });
  const result = await response.json();
  if (!response.ok) { stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${escapeHtml(result.message || M.actionFailed)}</p>`); if (response.headers.get('ETag')) etag = response.headers.get('ETag'); return; }
  etag = response.headers.get('ETag');
  if (result.repairDraft) currentSnapshot.workspace.repairDraft = result.repairDraft; else currentSnapshot = result;
  const draft = currentSnapshot.workspace.repairDraft;
  if (acceptedModel.assignments.length < 200 || draft.conflicts.length) { render(currentSnapshot); return; }
  const item = acceptedModel.assignmentMap.get(payload.lessonId);
  const oldButton = document.querySelector(`[data-lesson-id="${CSS.escape(payload.lessonId)}"]`);
  if (oldButton) { const holder = document.createElement('div'); holder.innerHTML = lessonButton(item, false); const replacement = holder.firstElementChild; replacement.addEventListener('click', () => selectLesson(replacement)); oldButton.replaceWith(replacement); }
  const host = document.querySelector('#lesson-details-host');
  host.innerHTML = lessonDetails(item); bindCloseDetails(); bindPinActions();
  document.querySelector('#attempt-pin-count').textContent = draft.intent.pins.length;
  document.querySelector('#draft-conflict-count').textContent = draft.conflicts.length;
  document.querySelector('#lesson-panel-title')?.focus();
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
  document.querySelector('#weekday')?.addEventListener('change', event => switchWholeSchoolDay(event.target.value));
  document.querySelector('#period-focus')?.addEventListener('change', event => { view.periodId = event.target.value; rerender(); });
  document.querySelectorAll('[data-range]').forEach(button => button.addEventListener('click', () => {
    const requested = button.dataset.range;
    const selected = view.selectedLessonId && acceptedModel.assignmentMap.get(view.selectedLessonId);
    const day = requested === 'DAY' && selected ? selected.period?.weekday : view.day;
    setRange(requested, day, false);
  }));
  document.querySelector('#previous-day')?.addEventListener('click', () => {
    const index = acceptedModel.weekdays.indexOf(view.day);
    switchWholeSchoolDay(acceptedModel.weekdays[(index - 1 + acceptedModel.weekdays.length) % acceptedModel.weekdays.length]);
  });
  document.querySelector('#next-day')?.addEventListener('click', () => {
    const index = acceptedModel.weekdays.indexOf(view.day);
    switchWholeSchoolDay(acceptedModel.weekdays[(index + 1) % acceptedModel.weekdays.length]);
  });
  document.querySelector('#reset-view').addEventListener('click', resetView);
  document.querySelector('#reset-empty')?.addEventListener('click', resetView);
  bindLessonButtons(document.querySelector('.matrix-wrap'));
  bindCloseDetails();
  document.querySelectorAll('[data-open-focus]').forEach(button => button.addEventListener('click', () => {
    view.focusedType = button.dataset.openFocus;
    const key = view.focusedType === 'cohortId' ? 'cohorts' : view.focusedType === 'teacherId' ? 'teachers' : 'rooms';
    view.focusedId = view[view.focusedType] || acceptedModel.definition[key][0]?.id || null;
    renderFocused();
  }));
}

function bindLessonButtons(root) {
  root.querySelectorAll('[data-lesson-id]').forEach(button => {
    if (boundLessonButtons.has(button)) return;
    boundLessonButtons.add(button);
    button.addEventListener('click', () => selectLesson(button));
  });
}

function syncSelectedLesson(root) {
  root.querySelectorAll('[data-lesson-id]').forEach(button => {
    const selected = button.dataset.lessonId === view.selectedLessonId;
    button.classList.toggle('selected', selected);
    button.setAttribute('aria-pressed', String(selected));
    button.querySelector('.selected-label').hidden = !selected;
  });
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
  bindCloseDetails(); bindPinActions();
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
    const matchLabel = button.querySelector('.match-label');
    if (matchLabel) matchLabel.hidden = active.length === 0 || !matches;
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
  document.querySelector('#filter-title').textContent = active.length ? M.filteredMatrix : M.completePopulation;
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
  const headers = { [csrf.headerName]: csrf.token, 'If-Match': etag };
  if (typeof body === 'string') headers['Content-Type'] = 'application/json';
  const response = await fetch(path, { method, headers, body });
  const result = await response.json();
  if (!response.ok) { stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${escapeHtml(result.message || M.actionFailed)}</p>`); if (response.headers.get('ETag')) etag = response.headers.get('ETag'); return; }
  etag = response.headers.get('ETag'); render(result);
}

async function mutateJson(path, method, payload) {
  return mutate(path, method, JSON.stringify(payload));
}

async function commandJson(path, method, payload) {
  const response = await fetch(path, { method, headers: { [csrf.headerName]: csrf.token, 'If-Match': etag, 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
  const result = await response.json();
  if (!response.ok) { stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${escapeHtml(result.message || M.actionFailed)}</p>`); if (response.headers.get('ETag')) etag = response.headers.get('ETag'); return null; }
  return result;
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
