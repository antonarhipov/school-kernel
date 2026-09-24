import { M } from './messages.js';
import { makeAcceptedModel } from './accepted-model.js';
import { createProposalComparison } from './proposal-comparison.js';
import { createInspectionState } from './inspection-state.js';
import { renderDayMatrix } from './day-renderer.js';
import { renderFocusedSchedule } from './focused-renderer.js';
import { renderWeekMatrix } from './week-renderer.js';

let etag;
let csrf;
let pollTimer;
let acceptedModel;
let proposedModel;
let comparison;
let currentSnapshot;
let bulkPreview;
let dayMatrices = new Map();
let preferenceSchoolId;
let inspectionState;
let canvasScroll;
let draftSaveFailed = false;
let selectionResetNotice = null;
const boundLessonButtons = new WeakSet();

const view = {
  day: null, search: '', cohortId: '', teacherId: '', roomId: '', periodId: '',
  range: 'WEEK', selectedLessonId: null, focusedType: null, focusedId: null,
  subjectInvestigationId: null, teacherInvestigationId: null, subjectOnly: false, teacherOnly: false,
  narrow: window.matchMedia('(max-width: 700px)').matches
};

const stateCard = document.querySelector('#state-card');
const importCard = document.querySelector('#import-card');
const currentLabel = document.querySelector('#current-label');
const importStatus = document.querySelector('#import-status');

localizeShell();

async function load(force = false) {
  const [csrfResponse, workspaceResponse] = await Promise.all([fetch('/api/csrf'), fetch('/api/workspace')]);
  csrf = await csrfResponse.json();
  const snapshot = await workspaceResponse.json();
  const unchangedRun = currentSnapshot?.state === 'SOLVING_REPAIR' && snapshot.state === 'SOLVING_REPAIR'
    && currentSnapshot.workspace.run.id === snapshot.workspace.run.id && etag === workspaceResponse.headers.get('ETag');
  etag = workspaceResponse.headers.get('ETag');
  if (unchangedRun && !force) { pollTimer = setTimeout(load, 300); return; }
  render(snapshot);
}

function render(snapshot) {
  const matrix = stateCard.querySelector('.matrix-wrap');
  if (matrix) canvasScroll = { left: matrix.scrollLeft, top: matrix.scrollTop };
  const previousState = currentSnapshot?.state;
  currentSnapshot = snapshot;
  clearTimeout(pollTimer);
  const school = snapshot.workspace.school;
  document.body.classList.toggle('operational', ['ACCEPTED_BASELINE', 'REPAIR_DRAFT', 'SOLVING_REPAIR', 'REPAIR_PROPOSAL'].includes(snapshot.state));
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
    syncInspectionState(inspectionState.enterLifecycle(snapshot.state));
    const selected = acceptedModel.assignmentMap.get(view.selectedLessonId);
    if (view.selectedLessonId && !isRepresented(selected)) {
      selectionResetNotice = !selected ? M.comparisonSelectionCleared
        : view.range === 'DAY' && selected.period?.weekday !== view.day ? M.selectionOutsideDay : M.selectionOutsideFilters;
      syncInspectionState(inspectionState.closeLesson());
    }
    if (view.narrow && !view.focusedType) {
      syncInspectionState(inspectionState.openFocused('cohortId', acceptedModel.definition.cohorts[0]?.id, null).state);
    }
    renderAccepted(snapshot, schoolName);
  } else if (snapshot.state === 'REPAIR_DRAFT') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeAcceptedModel(snapshot.workspace.acceptedBaseline);
    initializeInspectionState(snapshot.workspace.school?.id);
    syncInspectionState(inspectionState.enterLifecycle(snapshot.state));
    if (previousState === 'SOLVING_REPAIR' && snapshot.workspace.lastRun?.kind === 'REPAIR'
      && snapshot.workspace.lastRun.status !== 'CANCELLED') {
      syncInspectionState(inspectionState.setInspectorOpen(true));
      syncInspectionState(inspectionState.setUtilitiesOpen(true));
    }
    if (inspectionState.current().mode === 'CURRENT') renderAccepted(snapshot, schoolName);
    else renderRepair(snapshot, schoolName);
  } else if (snapshot.state === 'SOLVING_REPAIR') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeAcceptedModel(snapshot.workspace.acceptedBaseline);
    initializeInspectionState(snapshot.workspace.school?.id);
    syncInspectionState(inspectionState.enterLifecycle(snapshot.state));
    if (inspectionState.current().mode === 'CURRENT') renderAccepted(snapshot, schoolName);
    else renderSolving(snapshot, schoolName);
    pollTimer = setTimeout(load, 300);
  } else if (snapshot.state === 'REPAIR_PROPOSAL') {
    currentLabel.textContent = M.acceptedTimetable(schoolName);
    acceptedModel = makeAcceptedModel(snapshot.workspace.acceptedBaseline);
    initializeInspectionState(snapshot.workspace.school?.id);
    syncInspectionState(inspectionState.enterLifecycle(snapshot.state));
    if (view.narrow) syncInspectionState(inspectionState.selectMode('PROPOSAL').state);
    proposedModel = makeAcceptedModel({ definition: snapshot.workspace.proposal.definition, result: snapshot.workspace.proposal.result });
    comparison = createProposalComparison(acceptedModel, proposedModel, snapshot.workspace.proposal.review);
    if (inspectionState.current().mode === 'CURRENT') renderAccepted(snapshot, schoolName);
    else if (inspectionState.current().mode === 'DRAFT') renderRepair(snapshot, schoolName);
    else renderRepairProposal(snapshot, schoolName);
  }
  renderModeNavigation();
  if (selectionResetNotice) {
    const notice = document.querySelector('#inspection-notice') || document.querySelector('.narrow-banner');
    if (notice) notice.textContent = selectionResetNotice;
    selectionResetNotice = null;
  }
}

function renderModeNavigation() {
  if (!inspectionState || !['ACCEPTED_BASELINE', 'REPAIR_DRAFT', 'SOLVING_REPAIR', 'REPAIR_PROPOSAL'].includes(currentSnapshot.state) || view.narrow) return;
  const modes = { ACCEPTED_BASELINE: ['CURRENT'], REPAIR_DRAFT: ['CURRENT', 'DRAFT'],
    SOLVING_REPAIR: ['CURRENT', 'DRAFT', 'SOLVING'], REPAIR_PROPOSAL: ['CURRENT', 'DRAFT', 'PROPOSAL'] }[currentSnapshot.state];
  const labels = { CURRENT: M.modeCurrent, DRAFT: M.modeDraft, SOLVING: M.modeSolving, PROPOSAL: M.modeProposal };
  stateCard.querySelector('.accepted-heading')?.insertAdjacentHTML('beforeend', `<nav id="workbench-modes" aria-label="${M.presentationModes}">${modes.map(mode => `<button type="button" data-mode="${mode}" aria-pressed="${inspectionState.current().mode === mode}">${labels[mode]}</button>`).join('')}</nav>`);
  stateCard.querySelectorAll('[data-mode]').forEach(button => button.addEventListener('click', () => {
    if (!inspectionState.selectMode(button.dataset.mode).changed) return;
    if (button.dataset.mode !== 'PROPOSAL' && view.selectedLessonId && !acceptedModel.assignmentMap.has(view.selectedLessonId)) {
      syncInspectionState(inspectionState.closeLesson());
      selectionResetNotice = M.comparisonSelectionCleared;
    }
    render(currentSnapshot);
  }));
}

function renderRepairProposal(snapshot, schoolName) {
  const reviewStarted = performance.now();
  const proposal = snapshot.workspace.proposal;
  if (view.narrow) {
    stateCard.className = 'card workspace-card';
    stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state proposal">${M.repairProposal}</span><h2>${escapeHtml(schoolName)}</h2></div></div><p class="narrow-banner">${M.narrowNotice} ${M.acceptedStillCurrent}</p><div id="accepted-view"></div>`;
    if (!view.focusedType) { view.focusedType = 'cohortId'; view.focusedId = acceptedModel.definition.cohorts[0]?.id || null; }
    renderFocused(); return;
  }
  stateCard.className = 'card workspace-card review-mode compact-density';
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state proposal">${M.repairProposal}</span><h2>${escapeHtml(schoolName)}</h2><p class="revision">${escapeHtml(M.acceptedRevision(snapshot.workspace.acceptedBaseline.result.timetableRevision))}</p></div><p class="mode-note">${M.acceptedStillCurrent}</p></div><p>${M.repairProposalDetail}</p><div id="accepted-view"></div>`;
  if (view.focusedType) renderFocused(); else renderWholeSchool();
  window.__workspaceProposalReviewMs = performance.now() - reviewStarted;
}

function reviewLessonName(id) { return entityName(proposalModeActive() ? proposedModel.maps.lessons.get(id) || acceptedModel.maps.lessons.get(id) : acceptedModel.maps.lessons.get(id), id); }
function proposalModeActive() { return currentSnapshot.state === 'REPAIR_PROPOSAL' && inspectionState.current().mode === 'PROPOSAL'; }
function representedAssignment(id) { return proposalModeActive() ? comparison.entries.get(id)?.old || comparison.entries.get(id)?.proposed : acceptedModel.assignmentMap.get(id); }
function reviewGroup(key, title) {
  const groups = currentSnapshot.workspace.proposal.review.groupings[key];
  const label = value => value === 'OLD' ? M.oldContext : value === 'PROPOSED' ? M.proposedContext : M.bothContexts;
  const map = key === 'classes' ? proposedModel.maps.cohorts : key === 'teachers' ? proposedModel.maps.teachers : key === 'rooms' ? proposedModel.maps.rooms : null;
  return `<details class="review-groups"><summary>${title} · ${groups.length}</summary>${groups.length ? `<ul>${groups.map(group => `<li><strong>${escapeHtml(key === 'days' ? (M.days[group.id] || group.id) : entityName(map?.get(group.id) || acceptedGroupEntity(key, group.id), group.id))}</strong> <span class="context-label">${label(group.context)}</span> · ${group.lessonIds.length}<ul>${group.lessonIds.map(id => `<li><button type="button" class="link-button" data-review-lesson="${escapeAttribute(id)}">${escapeHtml(reviewLessonName(id))}</button></li>`).join('')}</ul></li>`).join('')}</ul>` : `<p>${M.emptyCategory}</p>`}</details>`;
}
function acceptedGroupEntity(key, id) { const map = key === 'classes' ? acceptedModel.maps.cohorts : key === 'teachers' ? acceptedModel.maps.teachers : acceptedModel.maps.rooms; return map.get(id); }

function reviewSide(title, side, changed, model) {
  if (!side) return `<section><h5>${title}</h5><p>${M.notPresent}</p></section>`;
  const values = [['subjectId', M.subject, model.maps.subjects], ['cohortId', M.class, model.maps.cohorts], ['teacherId', M.teacher, model.maps.teachers], ['periodId', M.period, model.maps.periods], ['roomId', M.room, model.maps.rooms]];
  const day = model.maps.periods.get(side.periodId)?.weekday;
  return `<section><h5>${title}</h5><dl>${detail(M.weekdayLabel, day ? M.days[day] || day : M.nameUnavailable)}${values.map(([field, label, map]) => `<div class="${changed.includes(field) ? 'changed-dimension' : ''}"><dt>${label}${changed.includes(field) ? ` · ${M.changedDimension}` : ''}</dt><dd>${escapeHtml(entityName(map.get(side[field]), side[field] || ''))}<small>${escapeHtml(side[field] || M.nameUnavailable)}</small></dd></div>`).join('')}</dl></section>`;
}

function proposalContext() {
  const proposal = currentSnapshot.workspace.proposal;
  const review = proposal.review;
  const protectedIds = [...new Set([...currentSnapshot.workspace.repairDraft.intent.pins.map(pin => pin.lessonId),
    ...acceptedModel.assignments.filter(item => comparisonProtection(item.lessonId)).map(item => item.lessonId)])].sort();
  const categories = review.categories.map(category => `<section class="review-category" data-category="${escapeAttribute(category.id)}"><h4>${escapeHtml(M[category.id])} <span>${category.count}</span></h4>${category.lessonIds.length ? `<ul>${category.lessonIds.map(id => `<li><button type="button" class="link-button" data-review-lesson="${escapeAttribute(id)}">${escapeHtml(reviewLessonName(id))}</button></li>`).join('')}</ul>` : `<p>${M.emptyCategory}</p>`}</section>`).join('');
  const unchanged = acceptedModel.assignments.filter(item => !comparison.entries.get(item.lessonId)?.change);
  return `<section id="proposal-context" aria-label="${M.proposalImpact}"><h3>${M.proposalImpact}</h3><p class="notice">${M.repairPriority}</p>
    <dl class="proposal-facts"><div><dt>${M.uniqueChangedLessons}</dt><dd>${review.uniqueChangedLessonCount}</dd></div><div><dt>${M.protectedAssignments}</dt><dd>${protectedIds.length}</dd></div><div><dt>${M.terminationReason}</dt><dd>${escapeHtml(proposal.terminationReason)}</dd></div><div><dt>${M.executionLimit}</dt><dd>${escapeHtml(proposal.limit)}</dd></div><div><dt>${M.elapsedTime}</dt><dd>${M.milliseconds(proposal.elapsedTimeMs)}</dd></div></dl>
    <details class="review-groups"><summary>${M.protectedAssignments} · ${protectedIds.length}</summary>${protectedIds.length ? `<ul>${protectedIds.map(id => `<li><button type="button" class="link-button" data-review-lesson="${escapeAttribute(id)}">${escapeHtml(reviewLessonName(id))}</button> · ${escapeHtml(comparisonProtection(id))}</li>`).join('')}</ul>` : `<p>${M.emptyCategory}</p>`}</details>
    <div class="impact-totals"><span class="direct-label">${M.directEffectChanges}: ${review.directEffectChangedCount}</span><span class="ripple-label">${M.rippleEffectChanges}: ${review.rippleEffectCount}</span></div><p>${M.overlappingTotals}</p>
    <h4>${M.changeCategories}</h4><div class="review-categories">${categories}</div>
    <h4>${M.impactGroups}</h4>${reviewGroup('classes', M.groupClasses)}${reviewGroup('teachers', M.groupTeachers)}${reviewGroup('rooms', M.groupRooms)}${reviewGroup('days', M.groupDays)}
    ${unchanged.length ? `<section class="unchanged-review"><h4>${M.unchangedLesson}</h4><select id="unchanged-lesson">${unchanged.map(item => `<option value="${escapeAttribute(item.lessonId)}">${escapeHtml(entityName(item.lesson, item.lessonId))}</option>`).join('')}</select><button type="button" id="show-unchanged" class="secondary">${M.showUnchanged}</button></section>` : ''}
    <p class="acceptance-warning"><strong>${M.acceptanceAdvancesBaseline}</strong></p><label class="confirmation"><input id="confirm-repair-accept" type="checkbox"> ${M.confirmRepairAcceptance}</label><div class="actions"><button id="accept-repair" disabled>${M.acceptRepair}</button><button id="revise-proposal" class="secondary">${M.reviseIntent}</button><button id="discard-proposal" class="danger">${M.discardRepairProposal}</button></div></section>`;
}

function bindRepairReview() {
  const confirmation = document.querySelector('#confirm-repair-accept'); const accept = document.querySelector('#accept-repair');
  confirmation.addEventListener('change', () => { accept.disabled = !confirmation.checked; });
  accept.addEventListener('click', () => mutate('/api/proposal/accept', 'POST'));
  document.querySelector('#revise-proposal').addEventListener('click', () => mutate('/api/proposal', 'DELETE'));
  document.querySelector('#discard-proposal').addEventListener('click', () => mutate('/api/proposal', 'DELETE'));
  document.querySelectorAll('[data-review-lesson]').forEach(button => button.addEventListener('click', () => selectReviewLesson(button.dataset.reviewLesson)));
  document.querySelector('#show-unchanged')?.addEventListener('click', () => selectReviewLesson(document.querySelector('#unchanged-lesson').value));
}

function selectReviewLesson(id) {
  const representations = comparison.assignmentsById.get(id) || [];
  const representation = representations.find(item => isRepresented(item)) || representations[0];
  if (!representation || !isRepresented(representation) || view.focusedType) {
    syncInspectionState(inspectionState.resetFilters().state);
    if (view.focusedType) syncInspectionState(inspectionState.returnToWholeSchool().state);
    if (representation && view.range === 'DAY' && view.day !== representation.period?.weekday && representation.period?.weekday) syncInspectionState(inspectionState.selectDay(representation.period.weekday, representation.period.weekday).state);
    renderWholeSchool();
    document.querySelector('#inspection-notice').textContent = M.comparisonNavigationReset;
  }
  const button = document.querySelector(`[data-lesson-id="${CSS.escape(id)}"]`);
  if (button) { button.scrollIntoView({ block: 'nearest', inline: 'nearest' }); selectLesson(button); }
  else { syncInspectionState(inspectionState.selectLesson(id)); document.querySelector('#lesson-details-host').innerHTML = selectedLessonDetails(id); toggleInspector(true); }
}

function initializeInspectionState(schoolId) {
  if (preferenceSchoolId === schoolId && inspectionState) return;
  preferenceSchoolId = schoolId;
  inspectionState = createInspectionState({ schoolId, weekdays: acceptedModel.weekdays,
    subjectIds: acceptedModel.definition.subjects.map(subject => subject.id),
    teacherIds: acceptedModel.definition.teachers.map(teacher => teacher.id),
    cohortIds: acceptedModel.definition.cohorts.map(cohort => cohort.id),
    roomIds: acceptedModel.definition.rooms.map(room => room.id),
    periodIds: acceptedModel.definition.periods.map(period => period.id) });
  syncInspectionState(inspectionState.current());
}

function syncInspectionState(state) {
  view.range = state.range;
  view.day = state.weekdayId;
  view.selectedLessonId = state.selectedLessonId;
  view.subjectInvestigationId = state.subjectId;
  view.teacherInvestigationId = state.teacherId;
  view.subjectOnly = state.subjectOnly;
  view.teacherOnly = state.teacherOnly;
  view.search = state.searchQuery;
  view.cohortId = state.cohortId || '';
  view.teacherId = state.teacherFilterId || '';
  view.roomId = state.roomId || '';
  view.periodId = state.periodId || '';
  view.focusedType = state.focusedType;
  view.focusedId = state.focusedId;
  view.scrollContext = state.scrollContext;
}

function setRange(range, day = view.day, manualDay = false) {
  if (!inspectionState) return;
  const selectedWeekday = proposalModeActive() && comparison.assignmentsById.get(view.selectedLessonId)?.some(item => item.period?.weekday === day)
    ? day : representedAssignment(view.selectedLessonId)?.period?.weekday;
  const transition = manualDay ? inspectionState.selectDay(day, selectedWeekday) : inspectionState.selectRange(range, day);
  if (!transition.changed) return;
  syncInspectionState(transition.state);
  const persisted = inspectionState.persist();
  renderWholeSchool();
  if (manualDay && transition.selectionCleared) {
    const notice = document.querySelector('#inspection-notice');
    if (notice) notice.textContent = M.lessonOutsideDay;
  }
  if (!persisted) {
    const notice = document.querySelector('#inspection-notice');
    if (notice) notice.textContent = M.preferenceUnavailable;
  }
}

function renderAccepted(snapshot, schoolName) {
  stateCard.className = 'card workspace-card compact-density current-mode';
  const lifecycle = { ACCEPTED_BASELINE: M.acceptedState, REPAIR_DRAFT: M.repairDraft,
    SOLVING_REPAIR: M.repairRunning, REPAIR_PROPOSAL: M.repairProposal }[snapshot.state];
  stateCard.innerHTML = `<div class="accepted-heading"><div class="school-identity"><h2>${escapeHtml(schoolName)}</h2><span class="state accepted">✓ ${M.currentAccepted}</span><span class="lifecycle-label">${lifecycle}</span><p class="revision">${escapeHtml(M.acceptedRevision(snapshot.workspace.acceptedBaseline.result.timetableRevision || snapshot.workspace.timetableRevision))}</p></div><div class="header-utilities">${view.narrow ? '' : snapshot.state === 'ACCEPTED_BASELINE' ? exportAccepted(snapshot) : runUtilities(snapshot.workspace.lastRun)}</div></div>
    <h3 class="sr-only">${M.timetableDetails}</h3><div id="accepted-view"></div>`;
  bindUtilities();
  if (view.focusedType || view.narrow) renderFocused(); else renderWholeSchool();
}

function exportAccepted(snapshot) {
  const baseline = snapshot.workspace.acceptedBaseline;
  return `<details id="utilities" class="utility-disclosure"><summary>${M.utilities}</summary><section class="export-baseline" aria-labelledby="export-title"><h3 id="export-title">${M.exportAccepted}</h3><p>${M.exportDetail}</p><dl><div><dt>${M.school}</dt><dd>${escapeHtml(snapshot.workspace.school.displayName)}</dd></div><div><dt>${M.definitionRevision}</dt><dd><code>${escapeHtml(baseline.result.inputRevision)}</code></dd></div><div><dt>${M.timetableRevision}</dt><dd><code>${escapeHtml(baseline.result.timetableRevision)}</code></dd></div></dl><a id="export-accepted" class="button-link" href="/api/accepted/export" download="accepted-baseline.zip">${M.downloadAccepted}</a><p id="export-status" role="status"></p></section></details>`;
}

function runUtilities(run) {
  if (run?.kind !== 'REPAIR' || run.status === 'FEASIBLE' || run.status === 'CANCELLED') return '';
  return `<details id="utilities" class="utility-disclosure"><summary>${M.utilities}</summary>${repairRunFeedback(run)}</details>`;
}

function bindUtilities() {
  const utilities = document.querySelector('#utilities');
  if (!utilities) return;
  utilities.open = inspectionState.current().utilitiesOpen;
  utilities.addEventListener('toggle', () => inspectionState.setUtilitiesOpen(utilities.open));
  document.querySelector('#export-accepted')?.addEventListener('click', downloadAccepted);
}

async function downloadAccepted(event) {
  event.preventDefault();
  const status = document.querySelector('#export-status');
  try {
    const response = await fetch('/api/accepted/export');
    if (!response.ok) throw new Error(M.exportFailed);
    const archive = await response.blob();
    if (!archive.size) throw new Error(M.exportFailed);
    const url = URL.createObjectURL(archive);
    const link = document.createElement('a');
    link.href = url; link.download = 'accepted-baseline.zip';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
    status.textContent = '';
  } catch (_) { status.className = 'error'; status.textContent = M.exportFailed; }
}

function startRepairForm() {
  const periods = acceptedModel.definition.periods;
  return `<details class="repair-entry" open><summary>${M.startRepair}</summary><form id="start-repair-form">
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
  const fill = () => { const source = type.value === 'TEACHER' ? acceptedModel.definition.teachers : acceptedModel.definition.rooms; resource.innerHTML = options(source).map(([value, text]) => `<option value="${escapeAttribute(value)}">${escapeHtml(text)}</option>`).join(''); const selected = acceptedModel.assignmentMap.get(view.selectedLessonId); const preferred = type.value === 'TEACHER' ? selected?.teacherId || (view.focusedType === 'teacherId' ? view.focusedId : null) : selected?.roomId || (view.focusedType === 'roomId' ? view.focusedId : null); if (preferred) resource.value = preferred; };
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
    stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state draft-state">${M.repairDraft}</span><h2>${escapeHtml(schoolName)}</h2></div></div><p class="narrow-banner">${M.narrowNotice} ${M.acceptedStillCurrent}</p><div id="accepted-view"></div>`;
    renderFocused(); return;
  }
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state draft-state">${M.repairDraft}</span><h2>${escapeHtml(schoolName)}</h2><p class="revision">${escapeHtml(M.acceptedRevision(snapshot.workspace.acceptedBaseline.result.timetableRevision || snapshot.workspace.timetableRevision))}</p></div><div class="header-utilities"><p class="mode-note">${M.acceptedStillCurrent}</p>${runUtilities(snapshot.workspace.lastRun)}</div></div>
    <p>${snapshot.state === 'REPAIR_PROPOSAL' ? M.proposalDraftDetail : M.repairDraftIntro}</p><div id="accepted-view"></div>`;
  bindUtilities();
  if (view.focusedType) renderFocused(); else renderWholeSchool();
}

function renderSolving(snapshot, schoolName) {
  const frozenDraft = inspectionState.current().mode === 'DRAFT';
  stateCard.className = 'card workspace-card compact-density';
  stateCard.innerHTML = `<div class="accepted-heading"><div><span class="state running">${M.repairRunning}</span><h2>${escapeHtml(schoolName)}</h2><p class="revision">${escapeHtml(M.acceptedRevision(snapshot.workspace.acceptedBaseline.result.timetableRevision || snapshot.workspace.timetableRevision))}</p></div><p class="mode-note">${M.acceptedStillCurrent}</p></div><p>${view.narrow ? M.narrowNotice : frozenDraft ? M.frozenDraftDetail : M.repairRunningDetail}</p><div id="accepted-view"></div>`;
  if (view.focusedType || view.narrow) renderFocused(); else renderWholeSchool();
}

function runContext() {
  const run = currentSnapshot.workspace.run;
  return `<section id="run-context" class="draft-context" aria-label="${M.repairRunning}"><h3>${M.repairRunning}</h3><p>${M.repairRunningDetail}</p><dl><div><dt>${M.executionLimit}</dt><dd>${escapeHtml(run.limit)}</dd></div><div><dt>${M.status}</dt><dd>${M.running}</dd></div></dl><div class="actions"><button id="cancel-run" type="button" class="danger">${M.cancelRun}</button></div></section>`;
}

function draftContext(frozen = false) {
  const draft = currentSnapshot.workspace.repairDraft;
  const lastRun = currentSnapshot.workspace.lastRun;
  const retryAvailable = lastRun?.kind === 'REPAIR' && lastRun.code === 'NO_FEASIBLE_SOLUTION_FOUND' && lastRun.intentRevision === draft.intentRevision;
  const changes = draft.intent.changes.map(change => repairChange(change)).join('');
  const conflicts = draft.conflicts.length ? `<div class="conflict-list" role="alert"><h4>${M.blockingConflicts}</h4>${draft.conflicts.map(item => `<p><button type="button" class="link-button" data-draft-conflict="${escapeAttribute(item.lessonId)}">${escapeHtml(entityName(acceptedModel.maps.lessons.get(item.lessonId), item.lessonId))}</button> · ${escapeHtml(item.message)}</p>`).join('')}</div>` : `<p class="ready-state">✓ ${M.readyToSolve}</p>`;
  if (frozen) return `<section class="draft-context" aria-label="${M.frozenDraft}"><h3>${M.frozenDraft}</h3>${changes || `<p>${M.noWeeklyChanges}</p>`}<dl><div><dt>${M.directEffects}</dt><dd>${draft.directEffectLessonIds.length}</dd></div><div><dt>${M.attemptPins}</dt><dd>${draft.intent.pins.length}</dd></div><div><dt>${M.conflicts}</dt><dd>${draft.conflicts.length}</dd></div></dl><p>${M.frozenDraftDetail}</p></section>`;
  return `<section class="draft-context" aria-label="${M.draftCounts}"><h3>${M.weeklyChanges}</h3>${changes || `<p>${M.noWeeklyChanges}</p>`}
    <dl><div><dt>${M.directEffects}</dt><dd>${draft.directEffectLessonIds.length}</dd></div><div><dt>${M.attemptPins}</dt><dd id="attempt-pin-count">${draft.intent.pins.length}</dd></div><div><dt>${M.conflicts}</dt><dd id="draft-conflict-count">${draft.conflicts.length}</dd></div></dl>
    ${draft.directEffectLessonIds.length ? '' : `<p class="notice">${M.noDirectEffects}</p>`}${conflicts}
    <details class="repair-entry"><summary>${M.stageChange}</summary>${repairIntentForm(draft)}</details>
    <details class="repair-controls"${bulkPreview ? ' open' : ''}><summary>${M.bulkPin}</summary><div class="repair-grid">${selectControl('bulk-scope', M.bulkScope, [['DAY', M.day], ['CLASS', M.class], ['UNAFFECTED', M.allUnaffected]], 'UNAFFECTED')}<label><span>${M.scopeValue}</span><select id="bulk-scope-id"></select></label></div>${pinDimensionControls('bulk')}<button id="preview-bulk" type="button" class="secondary">${M.previewBulk}</button><div id="bulk-preview-host">${bulkPreviewHtml()}</div></details>
    <div class="bulk-history">${draft.intent.bulkActions.map(action => `<p><span>${M.bulkApplied(action.lessonIds.length)}</span><button type="button" class="secondary" data-undo-bulk="${escapeAttribute(action.id)}">${M.undoBulk}</button></p>`).join('')}</div>
    ${lastRun?.kind === 'REPAIR' && lastRun.status !== 'FEASIBLE' && lastRun.status !== 'CANCELLED' ? repairRunFeedback(lastRun) : ''}
    ${draftSaveFailed ? `<p class="error" role="alert">${M.draftSaveFailed}</p>` : ''}<label class="confirmation"><input id="confirm-discard-draft" type="checkbox"> ${M.confirmDiscardDraft}</label><div class="actions"><button id="discard-draft" class="danger" disabled>${M.discardDraft}</button><button id="solve-draft"${draft.readyToSolve && !draftSaveFailed ? '' : ' disabled'}>${draft.readyToSolve && !draftSaveFailed ? M.createRepairProposal : M.resolveConflicts}</button>${retryAvailable && !draftSaveFailed ? `<button id="retry-repair" class="secondary">${M.retryRepair}</button>` : ''}</div></section>`;
}

function repairIntentForm(draft) {
  const change = draft.intent.changes[0];
  return `<form id="stage-repair-form"><p>${M.startRepairIntro}</p>${selectControl('stage-resource-type', M.resourceType, [['TEACHER', M.teacher], ['ROOM', M.room]], change?.resourceType || 'TEACHER')}
    <label><span>${M.resource}</span><select id="stage-resource"></select></label><fieldset class="period-choices"><legend>${M.weeklyUnavailablePeriods}</legend>${acceptedModel.definition.periods.map(period => `<label><input type="checkbox" name="stage-period" value="${escapeAttribute(period.id)}"${change?.unavailablePeriodIds.includes(period.id) ? ' checked' : ''}> <span>${escapeHtml(periodLabel(period))} · ${escapeHtml(M.days[period.weekday] || period.weekday)}</span></label>`).join('')}</fieldset><button type="submit">${M.stageChange}</button></form>`;
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
  const form = document.querySelector('#stage-repair-form');
  const type = form.querySelector('#stage-resource-type'); const resource = form.querySelector('#stage-resource');
  const fill = () => { const items = type.value === 'TEACHER' ? acceptedModel.definition.teachers : acceptedModel.definition.rooms; resource.innerHTML = options(items).map(([value, text]) => `<option value="${escapeAttribute(value)}">${escapeHtml(text)}</option>`).join(''); const staged = currentSnapshot.workspace.repairDraft.intent.changes.find(change => change.resourceType === type.value); if (staged) resource.value = staged.resourceId; };
  fill(); type.addEventListener('change', fill);
  form.addEventListener('submit', event => { event.preventDefault(); mutateJson('/api/repair-draft', 'PATCH', { action: 'STAGE_UNAVAILABILITY', resourceType: type.value, resourceId: resource.value, periodIds: [...form.querySelectorAll('[name=stage-period]:checked')].map(input => input.value) }); });
  const scope = document.querySelector('#bulk-scope'); const scopeId = document.querySelector('#bulk-scope-id');
  const fillScope = () => { const values = scope.value === 'DAY' ? acceptedModel.weekdays.map(day => [day, M.days[day] || day]) : scope.value === 'CLASS' ? options(acceptedModel.definition.cohorts) : [['', M.snapshotCurrentUnaffected]]; scopeId.innerHTML = values.map(([value, text]) => `<option value="${escapeAttribute(value)}">${escapeHtml(text)}</option>`).join(''); scopeId.disabled = scope.value === 'UNAFFECTED'; };
  fillScope(); scope.addEventListener('change', fillScope);
  document.querySelector('#preview-bulk').addEventListener('click', async () => {
    const dimensions = [...document.querySelectorAll('[name=bulk-dimension]:checked')].map(input => input.value);
    const payload = { scope: scope.value, dimensions }; if (scope.value !== 'UNAFFECTED') payload.scopeId = scopeId.value;
    const response = await commandJson('/api/repair-draft/bulk-pin-preview', 'POST', payload);
    if (response) { bulkPreview = response; render(currentSnapshot); }
  });
  document.querySelector('#confirm-bulk')?.addEventListener('click', () => { const preview = bulkPreview; bulkPreview = null; mutateJson('/api/repair-draft', 'PATCH', { action: 'CONFIRM_BULK_PIN', preview }); });
  document.querySelector('#cancel-bulk')?.addEventListener('click', () => { bulkPreview = null; render(currentSnapshot); });
  document.querySelectorAll('[data-undo-bulk]').forEach(button => button.addEventListener('click', () => mutateJson('/api/repair-draft', 'PATCH', { action: 'UNDO_BULK_PIN', bulkActionId: button.dataset.undoBulk })));
  const confirmation = document.querySelector('#confirm-discard-draft'); const discard = document.querySelector('#discard-draft');
  confirmation.addEventListener('change', () => { discard.disabled = !confirmation.checked; });
  discard.addEventListener('click', () => mutateJson('/api/repair-draft', 'DELETE', { confirmed: true }));
  document.querySelector('#solve-draft').addEventListener('click', () => mutateJson('/api/runs', 'POST', { limit: 'PT30S' }));
  document.querySelector('#retry-repair')?.addEventListener('click', () => mutateJson('/api/runs', 'POST', { limit: 'PT2M' }));
  document.querySelectorAll('[data-draft-conflict]').forEach(button => button.addEventListener('click', () => selectDraftLesson(button.dataset.draftConflict)));
}

function selectDraftLesson(id) {
  const item = acceptedModel.assignmentMap.get(id);
  if (!item) return;
  if (!isRepresented(item)) {
    syncInspectionState(inspectionState.resetFilters().state);
    if (view.range === 'DAY' && view.day !== item.period.weekday) syncInspectionState(inspectionState.selectDay(item.period.weekday, item.period.weekday).state);
    renderWholeSchool();
    document.querySelector('#inspection-notice').textContent = M.conflictNavigationReset;
  }
  const button = document.querySelector(`[data-lesson-id="${CSS.escape(id)}"]`);
  if (button) { button.scrollIntoView({ block: 'nearest', inline: 'nearest' }); selectLesson(button); }
}

function repairRunFeedback(run) {
  const diagnostics = run.searchDiagnostics?.constraints || [];
  const rows = diagnostics.map(item => `<li><strong>${escapeHtml(item.constraintId)}</strong> · ${M.diagnosticMatches(item.matchCount)} ${item.examples.flat().map(id => `<button type="button" class="link-button" data-diagnostic-id="${escapeAttribute(id)}">${escapeHtml(id)}</button>`).join(' ')}</li>`).join('');
  const validation = run.validationReport?.errors || [];
  const validationRows = validation.map(item => `<li>${escapeHtml(item.message || item.code || M.actionFailed)} ${(item.entityIds || []).map(id => `<button type="button" class="link-button" data-diagnostic-id="${escapeAttribute(id)}">${escapeHtml(id)}</button>`).join(' ')}</li>`).join('');
  return `<section class="conflict-list" role="status"><h3>${M.runDiagnostics}</h3><p><strong>${escapeHtml(run.code === 'INTERRUPTED' ? M.repairInterrupted : run.message || M.actionFailed)}</strong></p>${rows || validationRows ? `<ul>${rows}${validationRows}</ul>` : ''}<p>${M.diagnosticsCaution}</p></section>`;
}

function changeCountSummary(counts) {
  const categories = [['additions', M.additions], ['cancellations', M.cancellations], ['teacherChanges', M.teacherChanges], ['forcedMoves', M.forcedMoves], ['periodMoves', M.periodMoves], ['roomOnlyMoves', M.roomOnlyMoves]];
  return `<section><h3>${M.runDiagnostics}</h3><dl>${categories.map(([key, label]) => `<div><dt>${label}</dt><dd>${counts[key]}</dd></div>`).join('')}</dl></section>`;
}

function renderWholeSchool() {
  const host = document.querySelector('#accepted-view');
  const model = acceptedModel;
  const proposalMode = currentSnapshot.state === 'REPAIR_PROPOSAL' && inspectionState.current().mode === 'PROPOSAL';
  const displayed = proposalMode ? comparison : model;
  const dayPeriods = periodsForDay(view.day);
  const active = activeCriteria();
  const narrowed = hasNarrowingCriteria();
  const investigation = investigationSummary();
  const matches = narrowed ? investigation.represented : displayed.assignments.filter(isInSelectedRange);
  const visibleCohorts = narrowed && matches.length ? model.definition.cohorts.filter(cohort => matches.some(item => item.cohortId === cohort.id)) : model.definition.cohorts;
  const mode = inspectionState.current().mode;
  const draftMode = currentSnapshot.state === 'REPAIR_DRAFT' && mode === 'DRAFT';
  const frozenMode = currentSnapshot.state === 'SOLVING_REPAIR' && mode !== 'CURRENT';
  const proposalDraft = currentSnapshot.state === 'REPAIR_PROPOSAL' && mode === 'DRAFT';
  const repairSetupAvailable = currentSnapshot.state === 'ACCEPTED_BASELINE' && mode === 'CURRENT';
  const repairSetupOpen = repairSetupAvailable && inspectionState.current().taskAreaOpen.CURRENT;
  host.innerHTML = `<div class="inspection-toolbar" aria-label="${M.wholeSchool}">
      <fieldset class="range-control"><legend>${M.rangeLabel}</legend><button type="button" data-range="WEEK" aria-pressed="${view.range === 'WEEK'}">${M.week}</button><button type="button" data-range="DAY" aria-pressed="${view.range === 'DAY'}">${M.dayView}</button></fieldset>
      ${selectControl('weekday', M.weekdayLabel, model.weekdays.map(id => [id, M.days[id] || id]), view.day)}
      <label class="search-control"><span>${M.searchLabel}</span><input id="lesson-search" type="search" value="${escapeAttribute(view.search)}" placeholder="${M.searchPlaceholder}"></label>
      ${selectControl('subject-investigation', M.subjectInvestigation, [['', M.noSubjectSelected], ...options(model.definition.subjects)], view.subjectInvestigationId || '')}
      ${selectControl('teacher-investigation', M.teacherInvestigation, [['', M.noTeacherSelected], ...options(model.definition.teachers)], view.teacherInvestigationId || '')}
      ${view.range === 'DAY' ? `<button type="button" id="previous-day" class="secondary day-step">${M.previousDay}</button><button type="button" id="next-day" class="secondary day-step">${M.nextDay}</button>` : ''}</div>
    <details id="filters" class="filters-disclosure"${inspectionState.current().filtersOpen ? ' open' : ''}><summary>${M.filters}</summary>
      <div class="filter-controls">
        ${selectControl('cohort-filter', M.classFilter, [['', M.allClasses], ...options(model.definition.cohorts)], view.cohortId)}
        ${selectControl('teacher-filter', M.teacherFilter, [['', M.allTeachers], ...options(model.definition.teachers)], view.teacherId)}
        ${selectControl('room-filter', M.roomFilter, [['', M.allRooms], ...options(model.definition.rooms)], view.roomId)}
        ${selectControl('period-focus', M.periodFocus, [['', M.allPeriods], ...(view.range === 'DAY' ? dayPeriods : model.definition.periods).map(period => [period.id, periodLabel(period)])], view.periodId)}
        <label class="match-mode"><input id="subject-only" type="checkbox"${view.subjectOnly ? ' checked' : ''}${view.subjectInvestigationId ? '' : ' disabled'}> <span>${M.showOnlyMatches}</span></label>
        <label class="match-mode"><input id="teacher-only" type="checkbox"${view.teacherOnly ? ' checked' : ''}${view.teacherInvestigationId ? '' : ' disabled'}> <span>${M.showOnlyMatches}</span></label>
        <button id="clear-subject" type="button" class="secondary"${view.subjectInvestigationId ? '' : ' disabled'}>${M.clearSubject}</button>
        <button id="clear-teacher" type="button" class="secondary"${view.teacherInvestigationId ? '' : ' disabled'}>${M.clearTeacher}</button>
      </div></details>
    <div class="filter-status" role="status"><strong id="filter-title">${narrowed ? M.filteredMatrix : M.completePopulation}</strong><span id="range-summary">${view.range === 'WEEK' ? M.weekRange : M.dayRange(M.days[view.day] || view.day)}</span><span id="matrix-summary">${M.matrixSummary(visibleCohorts.length, model.definition.cohorts.length)}</span><span id="represented-lesson-count">${M.representedLessonCount(investigation.represented.length)}</span><span id="search-summary"${view.search.trim() ? '' : ' hidden'}>${view.search.trim() ? M.searchMatchCount(investigation.searchCount) : ''}</span><span class="criteria-label">${M.activeFilters}:</span><span id="active-criteria" class="criteria">${active.length ? active.map(item => `<span>${escapeHtml(item)}</span>`).join('') : M.noFilters}</span><button id="clear-filters" type="button" class="link-button"${narrowed ? '' : ' hidden'}>${M.clearFilters}</button><button id="reset-view" type="button" class="link-button">${M.reset}</button></div>
    <div class="investigation-summary" role="status"><span>${M.subjectMatchCount(investigation.subjectCount)}</span><span>${M.teacherMatchCount(investigation.teacherCount)}</span><span>${M.dualMatchCount(investigation.dualCount)}</span></div>
    ${renderTeacherRibbon(investigation.periods)}
    <p id="inspection-notice" class="notice" role="status"></p>
    ${displayed.assignments.length === 0 ? `<p class="empty-message" role="status">${M.emptyAccepted}</p>` : ''}
    <p id="no-matches" class="empty-message" role="status"${narrowed && matches.length === 0 ? '' : ' hidden'}>${M.noMatches} <button id="reset-empty" type="button" class="link-button">${M.reset}</button></p>
    <div class="workbench-layout${inspectionState.current().inspectorOpen ? '' : ' inspector-collapsed'}">
      <div class="canvas-region">${view.range === 'WEEK' ? renderWeekMatrix({ cohorts: model.definition.cohorts, weekdays: model.weekdays, assignmentsByCell: displayed.assignmentsByCell, periodsForDay, labels: M, entityName, periodLabel, lessonMarkup: weekLessonButton, escapeHtml, escapeAttribute }) : renderDayMatrix({ cohorts: model.definition.cohorts, periods: dayPeriods, assignmentsByCell: displayed.assignmentsByCell, periodId: view.periodId, labels: M, entityName, periodLabel, lessonMarkup: item => lessonButton(item, false), escapeHtml })}</div>
      <aside id="workbench-inspector" aria-label="${M.inspector}"${inspectionState.current().inspectorOpen ? '' : ' hidden'}><button id="toggle-inspector" type="button" class="secondary">${M.collapseInspector}</button><div id="lesson-details-host">${view.selectedLessonId ? selectedLessonDetails(view.selectedLessonId) : `<p>${M.noLessonSelected}</p>`}</div>${draftMode ? draftContext() : frozenMode ? draftContext(true) : proposalDraft ? `<section class="draft-context"><h3>${M.proposalDraftDetail}</h3>${currentSnapshot.workspace.repairDraft.intent.changes.map(repairChange).join('')}${M.attemptPins}: ${currentSnapshot.workspace.repairDraft.intent.pins.length}</section>` : ''}${currentSnapshot.state === 'SOLVING_REPAIR' ? runContext() : ''}${proposalMode ? proposalContext() : ''}</aside>
      <div id="inspector-summary"${inspectionState.current().inspectorOpen ? ' hidden' : ''}><span>${view.selectedLessonId ? escapeHtml(M.selectedSummary(reviewLessonName(view.selectedLessonId))) : M.noLessonSelected}</span> <button id="reopen-inspector" type="button" class="secondary">${M.reopenInspector}</button></div>
    </div>
    ${repairSetupAvailable ? `<div class="task-launch"><button id="open-repair-setup" type="button" class="secondary" aria-expanded="${repairSetupOpen}" aria-controls="workbench-task-area"${repairSetupOpen ? ' hidden' : ''}>${M.startRepair}</button></div><section id="workbench-task-area" class="task-area" aria-label="${M.repairSetup}"${repairSetupOpen ? '' : ' hidden'}><div class="task-area-heading"><h3>${M.repairSetup}</h3><button id="close-repair-setup" type="button" class="secondary">${M.closeRepairSetup}</button></div>${startRepairForm()}</section>` : ''}
    <div class="focused-entry"><h3>${M.focusedSchedules}</h3><button type="button" data-open-focus="cohortId" class="secondary">${M.openClass}</button><button type="button" data-open-focus="teacherId" class="secondary">${M.openTeacher}</button><button type="button" data-open-focus="roomId" class="secondary">${M.openRoom}</button></div>`;
  if (view.range === 'WEEK') { bindInspectionControls(); if (draftMode) bindRepairControls(); bindRunControls(); if (proposalMode) bindRepairReview(); applyFiltersInPlace(); restoreCanvasScroll(); return; }
  dayMatrices = new Map([[view.day, host.querySelector('.matrix-wrap')]]);
  for (const day of model.weekdays) {
    if (day === view.day) continue;
    const template = document.createElement('template');
    template.innerHTML = renderDayMatrix({ cohorts: model.definition.cohorts, periods: periodsForDay(day), assignmentsByCell: displayed.assignmentsByCell, periodId: '', labels: M, entityName, periodLabel, lessonMarkup: item => lessonButton(item, false), escapeHtml });
    dayMatrices.set(day, template.content.firstElementChild);
  }
  bindInspectionControls();
  if (draftMode) bindRepairControls();
  bindRunControls();
  if (proposalMode) bindRepairReview();
  applyFiltersInPlace();
  restoreCanvasScroll();
}

function bindRunControls() {
  document.querySelector('#cancel-run')?.addEventListener('click', () => cancelRun(currentSnapshot.workspace.run.id));
  document.querySelectorAll('[data-diagnostic-id]').forEach(button => button.addEventListener('click', () => selectDraftLesson(button.dataset.diagnosticId)));
}

function restoreCanvasScroll() {
  if (!canvasScroll) return;
  document.querySelector('.matrix-wrap')?.scrollTo(canvasScroll.left, canvasScroll.top);
  canvasScroll = null;
}

function switchWholeSchoolDay(day) {
  if (inspectionState) syncInspectionState(inspectionState.selectFilter('periodId', null).state); else view.periodId = '';
  setRange('DAY', day, true);
}

function weekLessonButton(item) {
  if (proposalModeActive()) return comparisonLessonButton(item, true);
  const selected = item.lessonId === view.selectedLessonId;
  const cues = lessonCues(item);
  const draft = repairLessonState(item.lessonId);
  const label = [entityName(item.subject, item.subjectId), entityName(item.teacher, item.teacherId), entityName(item.room, item.roomId), entityName(item.cohort, item.cohortId), M.days[item.period?.weekday] || item.period?.weekday || M.nameUnavailable, periodLabel(item.period), item.lessonId, ...cues.accessible, ...draft.accessible].join(' · ');
  return `<button type="button" class="lesson-cell week-lesson${selected ? ' selected' : ''}${cues.classes}${draft.direct ? ' directly-affected' : ''}${draft.conflict ? ' conflicting' : ''}" data-lesson-id="${escapeAttribute(item.lessonId)}" aria-label="${escapeAttribute(label)}" aria-pressed="${selected}"><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span class="week-room">${escapeHtml(entityName(item.room, item.roomId))}</span>${draft.labels}${cues.weekMarkup}<em class="selected-label"${selected ? '' : ' hidden'}>${M.selected}</em></button>`;
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
  if (proposalModeActive()) return comparisonLessonButton(item, false);
  const selected = item.lessonId === view.selectedLessonId;
  const cues = lessonCues(item);
  const draftState = repairLessonState(item.lessonId);
  return `<button type="button" class="lesson-cell${matched ? ' match' : ''}${selected ? ' selected' : ''}${cues.classes}${draftState.direct ? ' directly-affected' : ''}${draftState.conflict ? ' conflicting' : ''}" data-lesson-id="${escapeAttribute(item.lessonId)}" aria-label="${escapeAttribute(lessonAccessibleName(item, [...cues.accessible, ...draftState.accessible]))}" aria-pressed="${selected}"><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(item.teacher, item.teacherId))}</span><span>${escapeHtml(entityName(item.room, item.roomId))}</span>${draftState.labels}${cues.markup}<em class="match-label"${matched ? '' : ' hidden'}>${M.match}</em><em class="selected-label"${selected ? '' : ' hidden'}>${M.selected}</em></button>`;
}

function comparisonLessonButton(item, week) {
  const side = item.comparisonSide;
  const selected = item.lessonId === view.selectedLessonId;
  const cues = lessonCues(item);
  const matched = comparisonMatchedSides(item.lessonId);
  const state = side === 'accepted' ? item.change.proposed ? M.acceptedOrigin : M.cancellationCue
    : side === 'proposed' ? item.change.old ? M.proposedDestination : M.additionCue
      : side === 'combined' ? M.combinedChange : M.visuallyQuiet;
  const effects = item.change ? `${item.change.directEffect ? `<em class="direct-label">${M.directlyAffected}</em>` : ''}${item.change.rippleEffect ? `<em class="ripple-label">${M.rippleEffectChanges}</em>` : ''}` : '';
  const protection = comparisonProtection(item.lessonId);
  const label = [lessonAccessibleName(item, [...cues.accessible, state, ...matched]), protection].filter(Boolean).join(' · ');
  return `<button type="button" class="lesson-cell${week ? ' week-lesson' : ''} comparison-${side}${selected ? ' selected' : ''}${cues.classes}" data-lesson-id="${escapeAttribute(item.lessonId)}" data-comparison-side="${side}" aria-label="${escapeAttribute(label)}" aria-pressed="${selected}"><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(week ? item.room : item.teacher, week ? item.roomId : item.teacherId))}</span>${week ? '' : `<span>${escapeHtml(entityName(item.room, item.roomId))}</span>`}<em class="comparison-cue">${state}</em>${effects}${protection ? `<em class="pin-label">${escapeHtml(protection)}</em>` : ''}${cues.markup}<em class="selected-label"${selected ? '' : ' hidden'}>${M.selected}</em></button>`;
}

function comparisonProtection(id) {
  const pin = currentSnapshot.workspace.repairDraft?.intent?.pins?.find(item => item.lessonId === id);
  const lesson = acceptedModel.maps.lessons.get(id);
  const lock = currentSnapshot.workspace.acceptedBaseline.manifest?.locks?.find(item => item.lessonId === id);
  return [pin?.periodSources?.length ? M.periodPinned : null, pin?.roomSources?.length ? M.roomPinned : null,
    lesson?.periodLock || lock?.periodLockOrigin === 'PERSISTENT_POLICY' ? M.policyPeriodLock : null,
    lesson?.roomLock || lock?.roomLockOrigin === 'PERSISTENT_POLICY' ? M.policyRoomLock : null].filter(Boolean).join(' · ');
}

function comparisonMatchedSides(id) {
  const pair = comparison.entries.get(id);
  if (!view.search.trim() && !hasNarrowingCriteria() && !view.subjectInvestigationId && !view.teacherInvestigationId) return [];
  const investigationOnly = !view.search.trim() && !hasNarrowingCriteria();
  const matches = item => item && sideMatches(item) && (!view.search.trim() || searchMatches(item))
    && (!investigationOnly || item.subjectId === view.subjectInvestigationId || item.teacherId === view.teacherInvestigationId);
  const old = matches(pair?.old);
  const proposed = matches(pair?.proposed);
  return [old && M.acceptedSideMatch, proposed && M.proposedSideMatch].filter(Boolean);
}

function selectedLessonDetails(id) {
  if (!proposalModeActive()) return lessonDetails(acceptedModel.assignmentMap.get(id));
  const pair = comparison.entries.get(id);
  if (!pair) return '';
  const change = pair.change;
  return `<section class="comparison-details">${pair.old ? `<span class="state accepted">✓ ${M.acceptedAssignment}</span>` : ''}<h3 id="lesson-panel-title" tabindex="-1">${escapeHtml(reviewLessonName(id))}</h3><p>${change ? M.proposalChange : M.visuallyQuiet}</p><p>${escapeHtml(id)}</p>
    ${change?.directEffect ? `<p class="direct-label">${M.directlyAffected}</p>` : ''}${change?.rippleEffect ? `<p class="ripple-label">${M.rippleEffectChanges}</p>` : ''}
    ${change ? `<p>${change.categories.map(category => M[category]).join(', ')}</p>` : ''}${comparisonMatchedSides(id).length ? `<p>${comparisonMatchedSides(id).join(' · ')}</p>` : ''}${comparisonProtection(id) ? `<p>${escapeHtml(comparisonProtection(id))}</p>` : ''}
    <div class="before-after">${reviewSide(M.oldAssignment, change ? change.old : pair.old, change?.changedDimensions || [], acceptedModel)}${reviewSide(M.proposedAssignment, change ? change.proposed : pair.proposed, change?.changedDimensions || [], proposedModel)}</div>
    <button id="close-details" type="button" class="secondary">${M.closeDetails}</button></section>`;
}

function lessonDetails(item) {
  if (!item) return '';
  const draftState = repairLessonState(item.lessonId);
  const repairActions = currentSnapshot?.state === 'REPAIR_DRAFT' && inspectionState?.current().mode === 'DRAFT' ? `<section class="pin-actions"><h4>${M.protectAcceptedAssignment}</h4>${pinDimensionControls('lesson')}<div class="actions"><button type="button" id="apply-pin">${M.applyPin}</button><button type="button" id="remove-pin" class="secondary">${M.removePin}</button></div><p>${draftState.labels}${draftState.periodPinned || draftState.roomPinned ? '' : `<span class="state unpinned-state">${M.unpinned}</span>`} <span class="state ${draftState.periodPinned ? 'pin-label' : 'unpinned-state'}">${draftState.periodPinned ? M.periodPinned : M.periodUnpinned}</span> <span class="state ${draftState.roomPinned ? 'pin-label' : 'unpinned-state'}">${draftState.roomPinned ? M.roomPinned : M.roomUnpinned}</span></p>${draftState.intent ? `<p>${escapeHtml(draftState.intent)}</p>` : ''}${draftState.conflictMessages.map(message => `<p class="error">${escapeHtml(message)}</p>`).join('')}</section>` : '';
  return `<aside class="lesson-panel" aria-labelledby="lesson-panel-title"><div><span class="state accepted">✓ ${M.acceptedAssignment}</span><h3 id="lesson-panel-title" tabindex="-1">${escapeHtml(entityName(item.lesson, item.lessonId))}</h3></div><button id="close-details" type="button" class="secondary">${M.closeDetails}</button>
    <dl>${detail(M.subject, entityName(item.subject, item.subjectId))}${detail(M.class, entityName(item.cohort, item.cohortId))}${detail(M.teacher, entityName(item.teacher, item.teacherId))}${detail(M.weekdayLabel, M.days[item.period?.weekday] || item.period?.weekday || M.nameUnavailable)}${detail(M.period, entityName(item.period, item.periodId))}${detail(M.room, entityName(item.room, item.roomId))}</dl>
    ${repairActions}<details><summary>${M.technicalDetails}</summary><p>${M.technicalMapping}</p><dl class="technical">${idDetail(M.lesson, item.lessonId)}${idDetail(M.subject, item.subjectId)}${idDetail(M.class, item.cohortId)}${idDetail(M.teacher, item.teacherId)}${idDetail(M.period, item.periodId)}${idDetail(M.room, item.roomId)}</dl></details></aside>`;
}

function repairLessonState(lessonId) {
  if (!['REPAIR_DRAFT', 'SOLVING_REPAIR'].includes(currentSnapshot?.state)
    || !['DRAFT', 'SOLVING'].includes(inspectionState?.current().mode)) return { direct: false, conflict: false, labels: '', accessible: [], conflictMessages: [] };
  const draft = currentSnapshot.workspace.repairDraft;
  const direct = draft.directEffectLessonIds.includes(lessonId);
  const conflictMessages = draft.conflicts.filter(item => item.lessonId === lessonId).map(item => item.message);
  const conflict = conflictMessages.length > 0;
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
  const intent = draft.intent.changes.filter(change => change.unavailablePeriodIds.includes(acceptedModel.assignmentMap.get(lessonId)?.periodId)
    && acceptedModel.assignmentMap.get(lessonId)?.[change.resourceType === 'TEACHER' ? 'teacherId' : 'roomId'] === change.resourceId).map(change => {
      const resources = change.resourceType === 'TEACHER' ? acceptedModel.maps.teachers : acceptedModel.maps.rooms;
      return `${M.directIntent}: ${entityName(resources.get(change.resourceId), change.resourceId)} · ${change.unavailablePeriodIds.map(id => entityName(acceptedModel.maps.periods.get(id), id)).join(', ')}`;
    });
  return { direct, conflict, labels: labels.join(''), accessible: labels.map(label => label.replace(/<[^>]+>/g, '')), conflictMessages, intent: intent.join('; '), periodPinned: Boolean(pin?.periodSources?.length), roomPinned: Boolean(pin?.roomSources?.length) };
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
  if (!response.ok) { draftSaveFailed = true; stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${M.draftSaveFailed} ${escapeHtml(result.message || M.actionFailed)}</p>`); document.querySelector('#solve-draft').disabled = true; if (response.headers.get('ETag')) etag = response.headers.get('ETag'); return; }
  draftSaveFailed = false;
  etag = response.headers.get('ETag');
  if (result.repairDraft) currentSnapshot.workspace.repairDraft = result.repairDraft; else currentSnapshot = result;
  const draft = currentSnapshot.workspace.repairDraft;
  if (acceptedModel.assignments.length < 200 || draft.conflicts.length) { render(currentSnapshot); return; }
  const item = acceptedModel.assignmentMap.get(payload.lessonId);
  const oldButton = document.querySelector(`[data-lesson-id="${CSS.escape(payload.lessonId)}"]`);
  if (oldButton) { const holder = document.createElement('div'); holder.innerHTML = view.range === 'WEEK' ? weekLessonButton(item) : lessonButton(item, false); const replacement = holder.firstElementChild; replacement.addEventListener('click', () => selectLesson(replacement)); oldButton.replaceWith(replacement); }
  const host = document.querySelector('#lesson-details-host');
  host.innerHTML = lessonDetails(item); bindCloseDetails(); bindPinActions();
  document.querySelector('#attempt-pin-count').textContent = draft.intent.pins.length;
  document.querySelector('#draft-conflict-count').textContent = draft.conflicts.length;
  const context = document.querySelector('.draft-context');
  if (context) { const replacement = document.createElement('div'); replacement.innerHTML = draftContext(); context.replaceWith(replacement.firstElementChild); bindRepairControls(); }
  document.querySelector('#lesson-panel-title')?.focus();
}

function renderFocused() {
  const host = document.querySelector('#accepted-view');
  const type = view.focusedType || 'cohortId';
  const source = type === 'cohortId' ? acceptedModel.definition.cohorts : type === 'teacherId' ? acceptedModel.definition.teachers : acceptedModel.definition.rooms;
  if (!source.some(item => item.id === view.focusedId)) {
    const fallback = source[0]?.id || null;
    if (inspectionState && fallback) syncInspectionState(inspectionState.changeFocusedType(type, fallback).state);
    else view.focusedId = fallback;
  }
  const relatedLessonIds = proposalModeActive() ? new Set(comparison.assignments.filter(item => item[type] === view.focusedId).map(item => item.lessonId)) : null;
  host.innerHTML = renderFocusedSchedule({ narrow: view.narrow, type, focusedId: view.focusedId, source, relatedLessonIds,
    assignments: proposalModeActive() ? comparison.assignments : acceptedModel.assignments, weekdays: acceptedModel.weekdays,
    lessonMarkup: proposalModeActive() ? item => `<article class="focused-lesson" data-lesson-id="${escapeAttribute(item.lessonId)}" data-comparison-side="${item.comparisonSide}"><time>${escapeHtml(periodLabel(item.period))}</time><div><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(item.cohort, item.cohortId))} · ${escapeHtml(entityName(item.teacher, item.teacherId))} · ${escapeHtml(entityName(item.room, item.roomId))}</span></div><span class="accepted-text">${item.comparisonSide === 'accepted' ? item.change.proposed ? M.acceptedOrigin : M.cancellationCue : item.comparisonSide === 'proposed' ? item.change.old ? M.proposedDestination : M.additionCue : item.comparisonSide === 'combined' ? M.combinedChange : M.visuallyQuiet}</span>${item[type] !== view.focusedId ? `<span class="context-label">${M.linkedComparisonSide}</span>` : ''}</article>` : null,
    labels: M, entityName, periodLabel, selectControl, options, escapeHtml });
  if (!view.narrow && currentSnapshot.state === 'SOLVING_REPAIR') {
    host.insertAdjacentHTML('beforeend', `<aside id="workbench-inspector" aria-label="${M.inspector}">${inspectionState.current().mode === 'DRAFT' ? draftContext(true) : ''}${runContext()}</aside>`);
    bindRunControls();
  }
  if (!view.narrow && proposalModeActive()) {
    host.insertAdjacentHTML('beforeend', `<aside id="workbench-inspector" aria-label="${M.inspector}">${view.selectedLessonId ? selectedLessonDetails(view.selectedLessonId) : ''}${proposalContext()}</aside>`);
    bindRepairReview(); bindCloseDetails();
  }
  document.querySelector('#return-matrix')?.addEventListener('click', () => {
    if (inspectionState) syncInspectionState(inspectionState.returnToWholeSchool().state); else view.focusedType = null;
    renderWholeSchool();
    const matrix = document.querySelector('.matrix-wrap');
    if (matrix && view.scrollContext) matrix.scrollTo(view.scrollContext.left, view.scrollContext.top);
  });
  document.querySelectorAll('[data-focus-type]').forEach(button => button.addEventListener('click', () => {
    const nextType = button.dataset.focusType;
    const nextSource = nextType === 'cohortId' ? acceptedModel.definition.cohorts : nextType === 'teacherId' ? acceptedModel.definition.teachers : acceptedModel.definition.rooms;
    if (inspectionState) syncInspectionState(inspectionState.changeFocusedType(nextType, nextSource[0]?.id).state);
    else { view.focusedType = nextType; view.focusedId = nextSource[0]?.id || null; }
    renderFocused();
  }));
  document.querySelector('#focus-entity')?.addEventListener('change', event => {
    if (inspectionState) syncInspectionState(inspectionState.changeFocusedType(type, event.target.value).state);
    else view.focusedId = event.target.value;
    renderFocused();
  });
}

function bindInspectionControls() {
  const rerender = () => renderWholeSchool();
  document.querySelector('#toggle-inspector')?.addEventListener('click', () => toggleInspector(false));
  document.querySelector('#reopen-inspector')?.addEventListener('click', () => toggleInspector(true));
  document.querySelector('#filters')?.addEventListener('toggle', event => inspectionState.setFiltersOpen(event.currentTarget.open));
  document.querySelector('#open-repair-setup')?.addEventListener('click', () => {
    syncInspectionState(inspectionState.setTaskAreaOpen('CURRENT', true).state);
    document.querySelector('#workbench-task-area').hidden = false;
    document.querySelector('#open-repair-setup').hidden = true;
    document.querySelector('#close-repair-setup').focus();
  });
  document.querySelector('#close-repair-setup')?.addEventListener('click', () => {
    syncInspectionState(inspectionState.setTaskAreaOpen('CURRENT', false).state);
    document.querySelector('#workbench-task-area').hidden = true;
    document.querySelector('#open-repair-setup').hidden = false;
    document.querySelector('#open-repair-setup').focus();
  });
  document.querySelector('#lesson-search').addEventListener('input', event => {
    if (inspectionState) syncInspectionState(inspectionState.setSearch(event.target.value).state); else view.search = event.target.value;
    applyFiltersInPlace();
  });
  for (const [id, key] of [['cohort-filter', 'cohortId'], ['teacher-filter', 'teacherFilterId'], ['room-filter', 'roomId']]) document.querySelector(`#${id}`).addEventListener('change', event => {
    if (inspectionState) syncInspectionState(inspectionState.selectFilter(key, event.target.value || null).state);
    else view[key === 'teacherFilterId' ? 'teacherId' : key] = event.target.value;
    const selectionCleared = clearSelectedLessonOutsideRepresentation();
    applyFiltersInPlace();
    if (selectionCleared) document.querySelector('#inspection-notice').textContent = M.selectionOutsideFilters;
  });
  document.querySelector('#weekday')?.addEventListener('change', event => switchWholeSchoolDay(event.target.value));
  document.querySelector('#period-focus')?.addEventListener('change', event => {
    if (inspectionState) syncInspectionState(inspectionState.selectFilter('periodId', event.target.value || null).state); else view.periodId = event.target.value;
    const selectionCleared = clearSelectedLessonOutsideRepresentation();
    rerender();
    if (selectionCleared) document.querySelector('#inspection-notice').textContent = M.selectionOutsideFilters;
  });
  document.querySelector('#subject-investigation')?.addEventListener('change', event => setInvestigation('subject', event.target.value || null));
  document.querySelector('#teacher-investigation')?.addEventListener('change', event => setInvestigation('teacher', event.target.value || null));
  document.querySelector('#subject-only')?.addEventListener('change', event => setInvestigationMode('subject', event.target.checked));
  document.querySelector('#teacher-only')?.addEventListener('change', event => setInvestigationMode('teacher', event.target.checked));
  document.querySelector('#clear-subject')?.addEventListener('click', () => setInvestigation('subject', null));
  document.querySelector('#clear-teacher')?.addEventListener('click', () => setInvestigation('teacher', null));
  document.querySelectorAll('[data-range]').forEach(button => button.addEventListener('click', () => {
    const requested = button.dataset.range;
    const selected = view.selectedLessonId && representedAssignment(view.selectedLessonId);
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
  document.querySelector('#clear-filters').addEventListener('click', clearNarrowing);
  document.querySelector('#reset-empty')?.addEventListener('click', clearNarrowing);
  bindStartRepair();
  bindLessonButtons(document.querySelector('.matrix-wrap'));
  bindCloseDetails();
  bindPinActions();
  document.querySelectorAll('[data-open-focus]').forEach(button => button.addEventListener('click', () => {
    const type = button.dataset.openFocus;
    const key = type === 'cohortId' ? 'cohorts' : type === 'teacherId' ? 'teachers' : 'rooms';
    const lesson = acceptedModel.assignmentMap.get(view.selectedLessonId);
    const selected = type === 'cohortId' ? view.cohortId || lesson?.cohortId
      : type === 'teacherId' ? view.teacherId || view.teacherInvestigationId || lesson?.teacherId
        : view.roomId || lesson?.roomId;
    const focusedId = selected || acceptedModel.definition[key][0]?.id || null;
    const matrix = document.querySelector('.matrix-wrap');
    const scrollContext = matrix ? { left: matrix.scrollLeft, top: matrix.scrollTop } : null;
    if (inspectionState) syncInspectionState(inspectionState.openFocused(type, focusedId, scrollContext).state);
    else { view.focusedType = type; view.focusedId = focusedId; view.scrollContext = scrollContext; }
    renderFocused();
  }));
}

function setInvestigation(kind, id) {
  if (!inspectionState) return;
  const transition = kind === 'subject' ? inspectionState.selectSubject(id) : inspectionState.selectTeacher(id);
  if (!transition.changed) return;
  syncInspectionState(transition.state);
  renderWholeSchool();
}

function setInvestigationMode(kind, only) {
  if (!inspectionState) return;
  const transition = kind === 'subject' ? inspectionState.setSubjectOnly(only) : inspectionState.setTeacherOnly(only);
  if (!transition.changed) return;
  syncInspectionState(transition.state);
  const selectionCleared = clearSelectedLessonOutsideRepresentation();
  renderWholeSchool();
  if (selectionCleared) document.querySelector('#inspection-notice').textContent = M.selectionOutsideFilters;
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
  if (inspectionState) syncInspectionState(inspectionState.selectLesson(button.dataset.lessonId));
  else view.selectedLessonId = button.dataset.lessonId;
  if (document.querySelector('#workbench-inspector')) toggleInspector(true);
  document.querySelectorAll('.lesson-cell[data-lesson-id]').forEach(candidate => {
    const selected = candidate.dataset.lessonId === view.selectedLessonId;
    candidate.classList.toggle('selected', selected);
    candidate.setAttribute('aria-pressed', String(selected));
    candidate.querySelector('.selected-label').hidden = !selected;
  });
  document.querySelector('#lesson-details-host').innerHTML = selectedLessonDetails(view.selectedLessonId);
  bindCloseDetails(); bindPinActions();
  document.querySelector('#lesson-panel-title')?.focus();
}

function toggleInspector(open) {
  syncInspectionState(inspectionState.setInspectorOpen(open));
  document.querySelector('.workbench-layout')?.classList.toggle('inspector-collapsed', !open);
  document.querySelector('#workbench-inspector').hidden = !open;
  document.querySelector('#inspector-summary').hidden = open;
  document.querySelector('#inspector-summary span').textContent = view.selectedLessonId ? M.selectedSummary(reviewLessonName(view.selectedLessonId)) : M.noLessonSelected;
}

function bindCloseDetails() {
  document.querySelector('#close-details')?.addEventListener('click', () => {
    if (inspectionState) syncInspectionState(inspectionState.closeLesson());
    else view.selectedLessonId = null;
    if (document.querySelector('#lesson-details-host')) document.querySelector('#lesson-details-host').textContent = M.noLessonSelected;
    else document.querySelector('.comparison-details')?.remove();
    document.querySelectorAll('[data-lesson-id]').forEach(candidate => {
      candidate.classList.remove('selected');
      candidate.setAttribute('aria-pressed', 'false');
      if (candidate.querySelector('.selected-label')) candidate.querySelector('.selected-label').hidden = true;
    });
  });
}

function applyFiltersInPlace() {
  const active = activeCriteria();
  const narrowed = hasNarrowingCriteria();
  const investigation = investigationSummary();
  let visibleRows = 0;
  document.querySelectorAll('.lesson-cell[data-lesson-id]').forEach(button => {
    const item = proposalModeActive() ? comparison.assignmentsById.get(button.dataset.lessonId)?.find(candidate => candidate.comparisonSide === button.dataset.comparisonSide) : acceptedModel.assignmentMap.get(button.dataset.lessonId);
    const matches = isRepresented(item);
    const cues = lessonCues(item);
    button.hidden = !matches;
    button.classList.toggle('search-match', proposalModeActive() ? Boolean(comparison.entries.get(item.lessonId)?.old && searchMatches(comparison.entries.get(item.lessonId).old) || comparison.entries.get(item.lessonId)?.proposed && searchMatches(comparison.entries.get(item.lessonId).proposed)) : searchMatches(item));
    button.setAttribute('aria-label', proposalModeActive() ? lessonAccessibleName(item, [...cues.accessible, ...comparisonMatchedSides(item.lessonId)]) : lessonAccessibleName(item, [...cues.accessible, ...repairLessonState(item.lessonId).accessible]));
    const searchLabel = button.querySelector('.search-match-label');
    if (view.search.trim() && searchMatches(item) && !searchLabel) button.insertAdjacentHTML('beforeend', button.classList.contains('week-lesson')
      ? `<em class="search-match-label" title="${M.searchMatch}">${M.searchCue}</em>` : `<em class="search-match-label">${M.searchMatch}</em>`);
    if (!view.search.trim() || !searchMatches(item)) searchLabel?.remove();
    if (proposalModeActive()) {
      button.querySelector('.side-match-label')?.remove();
      if (comparisonMatchedSides(item.lessonId).length) button.insertAdjacentHTML('beforeend', `<em class="side-match-label">${comparisonMatchedSides(item.lessonId).join(' · ')}</em>`);
    }
    button.classList.toggle('match', narrowed && matches);
    const matchLabel = button.querySelector('.match-label');
    if (matchLabel) matchLabel.hidden = !narrowed || !matches;
  });
  document.querySelectorAll('.matrix tbody tr').forEach(row => {
    const hasMatch = Boolean(row.querySelector('.lesson-cell:not([hidden])'));
    row.hidden = narrowed && investigation.represented.length > 0 && !hasMatch;
    if (!row.hidden) visibleRows++;
    row.querySelectorAll('td').forEach(cell => {
      const visibleLesson = cell.querySelector('.lesson-cell:not([hidden])');
      const empty = cell.querySelector('.empty-cell');
      if (empty) empty.hidden = Boolean(visibleLesson);
    });
  });
  document.querySelector('#filter-title').textContent = narrowed ? M.filteredMatrix : M.completePopulation;
  document.querySelector('#matrix-summary').textContent = M.matrixSummary(visibleRows, acceptedModel.definition.cohorts.length);
  document.querySelector('#represented-lesson-count').textContent = M.representedLessonCount(investigation.represented.length);
  document.querySelector('#active-criteria').innerHTML = active.length ? active.map(item => `<span>${escapeHtml(item)}</span>`).join('') : M.noFilters;
  document.querySelector('#clear-filters').hidden = !narrowed;
  document.querySelector('#search-summary').textContent = view.search.trim() ? M.searchMatchCount(investigation.searchCount) : '';
  document.querySelector('#search-summary').hidden = !view.search.trim();
  document.querySelector('#no-matches').hidden = !(narrowed && investigation.represented.length === 0);
  document.querySelector('.matrix-wrap').setAttribute('aria-label', narrowed ? M.filteredMatrix : M.completeMatrix);
}

function resetView() {
  if (inspectionState) syncInspectionState(inspectionState.resetFilters().state);
  else { view.search = ''; view.cohortId = ''; view.teacherId = ''; view.roomId = ''; view.periodId = ''; }
  renderWholeSchool();
}

function clearNarrowing() {
  syncInspectionState(inspectionState.clearNarrowing().state);
  renderWholeSchool();
}

function filteredAssignments() {
  return proposalModeActive() ? [...new Map(comparison.assignments.filter(isRepresented).map(item => [item.lessonId, item])).values()]
    : acceptedModel.assignments.filter(isRepresented);
}

function baseFilterMatches(item) {
  return (!view.cohortId || item.cohortId === view.cohortId) && (!view.teacherId || item.teacherId === view.teacherId)
    && (!view.roomId || item.roomId === view.roomId) && (!view.periodId || item.periodId === view.periodId);
}

function isInSelectedRange(item) { return view.range === 'WEEK' || item.period?.weekday === view.day; }
function isInvestigationMatch(item) {
  return (!view.subjectOnly || item.subjectId === view.subjectInvestigationId)
    && (!view.teacherOnly || item.teacherId === view.teacherInvestigationId);
}
function sideMatches(item) { return item && baseFilterMatches(item) && isInvestigationMatch(item); }
function isRepresented(item) {
  if (!item || !isInSelectedRange(item)) return false;
  if (!proposalModeActive()) return sideMatches(item);
  const pair = comparison.entries.get(item.lessonId);
  return sideMatches(pair.old) || sideMatches(pair.proposed);
}

function clearSelectedLessonOutsideRepresentation() {
  const selected = proposalModeActive() ? comparison.assignmentsById.get(view.selectedLessonId) || [] : [acceptedModel.assignmentMap.get(view.selectedLessonId)];
  if (!selected.some(Boolean) || selected.some(isRepresented)) return false;
  if (inspectionState) syncInspectionState(inspectionState.closeLesson()); else view.selectedLessonId = null;
  return true;
}

function searchable(item) {
  return [item.lessonId, item.subjectId, item.cohortId, item.teacherId, item.periodId, item.roomId, item.lesson?.displayName, item.subject?.displayName, item.cohort?.displayName, item.teacher?.displayName, item.period?.displayName, item.room?.displayName].filter(Boolean).map(String);
}

function activeCriteria() {
  const criteria = [];
  if (view.cohortId) criteria.push(M.classCriterion(entityName(acceptedModel.maps.cohorts.get(view.cohortId), view.cohortId)));
  if (view.teacherId) criteria.push(M.teacherCriterion(entityName(acceptedModel.maps.teachers.get(view.teacherId), view.teacherId)));
  if (view.roomId) criteria.push(M.roomCriterion(entityName(acceptedModel.maps.rooms.get(view.roomId), view.roomId)));
  if (view.periodId) criteria.push(M.periodCriterion(entityName(acceptedModel.maps.periods.get(view.periodId), view.periodId)));
  if (view.subjectOnly && view.subjectInvestigationId) criteria.push(M.subjectCriterion(entityName(acceptedModel.maps.subjects.get(view.subjectInvestigationId), view.subjectInvestigationId)));
  if (view.teacherOnly && view.teacherInvestigationId) criteria.push(M.teacherCriterion(entityName(acceptedModel.maps.teachers.get(view.teacherInvestigationId), view.teacherInvestigationId)));
  return criteria;
}

function hasNarrowingCriteria() {
  return Boolean(view.cohortId || view.teacherId || view.roomId || view.periodId || view.subjectOnly || view.teacherOnly);
}

function investigationSummary() {
  const represented = filteredAssignments();
  const count = predicate => new Set(represented.filter(item => proposalModeActive()
    ? [comparison.entries.get(item.lessonId)?.old, comparison.entries.get(item.lessonId)?.proposed].filter(Boolean).some(predicate)
    : predicate(item)).map(item => item.lessonId)).size;
  const subjectCount = count(item => view.subjectInvestigationId && item.subjectId === view.subjectInvestigationId);
  const teacherCount = count(item => view.teacherInvestigationId && item.teacherId === view.teacherInvestigationId);
  const dualCount = count(item => view.subjectInvestigationId && view.teacherInvestigationId
    && item.subjectId === view.subjectInvestigationId && item.teacherId === view.teacherInvestigationId);
  const searchCount = count(searchMatches);
  return { represented, subjectCount, teacherCount, dualCount, searchCount,
    periods: (proposalModeActive() ? proposedModel : acceptedModel).definition.periods.filter(period => view.range === 'WEEK' || period.weekday === view.day) };
}

function searchMatches(item) {
  const query = view.search.trim().toLocaleLowerCase();
  return Boolean(query) && searchable(item).some(value => value.toLocaleLowerCase().includes(query));
}

function lessonCues(item) {
  const search = searchMatches(item);
  const subject = Boolean(view.subjectInvestigationId && item.subjectId === view.subjectInvestigationId);
  const teacher = Boolean(view.teacherInvestigationId && item.teacherId === view.teacherInvestigationId);
  const accessible = [...(search ? [M.searchMatch] : []), ...(teacher && subject ? [M.dualMatch] : []), ...(teacher || subject ? [] : []), ...(subject && !teacher ? [M.subjectMatch] : []), ...(teacher && !subject ? [M.teacherMatch] : [])];
  const classes = `${search ? ' search-match' : ''}${subject ? ' subject-match' : ''}${teacher ? ' teacher-match' : ''}${subject && teacher ? ' dual-match' : ''}`;
  const investigationMarkup = subject && teacher ? `<em class="dual-match-label">${M.dualMatch}</em>`
    : `${subject ? `<em class="subject-match-label">${M.subjectMatch}</em>` : ''}${teacher ? `<em class="teacher-match-label">${M.teacherMatch}</em>` : ''}`;
  const markup = `${search ? `<em class="search-match-label">${M.searchMatch}</em>` : ''}${investigationMarkup}`;
  const weekMarkup = `${search ? `<em class="search-match-label" title="${M.searchMatch}">${M.searchCue}</em>` : ''}${subject && teacher
    ? `<em class="dual-match-label" title="${M.dualMatch}">${M.dualCue}</em>`
    : `${subject ? `<em class="subject-match-label" title="${M.subjectMatch}">${M.subjectCue}</em>` : ''}${teacher ? `<em class="teacher-match-label" title="${M.teacherMatch}">${M.teacherCue}</em>` : ''}`}`;
  return { accessible, classes, markup, weekMarkup };
}

function lessonAccessibleName(item, cues) {
  return [entityName(item.subject, item.subjectId), entityName(item.teacher, item.teacherId), entityName(item.room, item.roomId),
    entityName(item.cohort, item.cohortId), M.days[item.period?.weekday] || item.period?.weekday || M.nameUnavailable,
    periodLabel(item.period), item.lessonId, ...cues].join(' · ');
}

function renderTeacherRibbon(periods) {
  if (!view.teacherInvestigationId) return '';
  const model = proposalModeActive() ? proposedModel : acceptedModel;
  const teacher = model.maps.teachers.get(view.teacherInvestigationId);
  const availability = Array.isArray(teacher?.availablePeriodIds) ? new Set(teacher.availablePeriodIds) : null;
  const slots = periods.slice().sort((a, b) => model.weekdays.indexOf(a.weekday) - model.weekdays.indexOf(b.weekday) || a.order - b.order)
    .map(period => {
      const assigned = model.assignments.some(item => item.teacherId === view.teacherInvestigationId && item.periodId === period.id);
      const state = assigned ? 'assigned' : availability === null || availability.has(period.id) ? 'available' : 'unavailable';
      const label = state === 'assigned' ? M.assigned : state === 'available' ? M.availableUnassigned : M.unavailable;
      return `<li class="ribbon-${state}"><strong>${escapeHtml(periodLabel(period))}</strong><span>${label}</span></li>`;
    }).join('');
  return `<section class="teacher-ribbon" aria-labelledby="teacher-ribbon-title"><h4 id="teacher-ribbon-title">${proposalModeActive() ? M.proposedAvailability : M.teacherRibbon} · ${escapeHtml(entityName(teacher, view.teacherInvestigationId))}</h4><ul>${slots}</ul></section>`;
}

function periodsForDay(day) { return acceptedModel.definition.periods.filter(period => period.weekday === day).sort((a, b) => a.order - b.order); }
function options(items) { return items.map(item => [item.id, entityName(item)]); }
function entityName(item, fallback = '') { return item?.displayName || `${fallback} (${M.nameUnavailable})`; }
function periodLabel(period) { return period ? `${entityName(period, period.id)}${period.startTime ? ` · ${M.optionalTime(period.startTime, period.endTime)}` : ''}` : M.nameUnavailable; }
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
  try {
    const headers = { [csrf.headerName]: csrf.token, 'If-Match': etag };
    if (typeof body === 'string') headers['Content-Type'] = 'application/json';
    const response = await fetch(path, { method, headers, body });
    const result = await response.json();
    if (!response.ok) {
      if (path === '/api/repair-draft' && method !== 'DELETE') { draftSaveFailed = true; document.querySelector('#solve-draft')?.setAttribute('disabled', ''); }
      if (path === '/api/proposal/accept' && result.code === 'STALE_PROPOSAL') {
        await load(true);
        stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${escapeHtml(result.message || M.actionFailed)} ${M.acceptedStillCurrent}</p>`);
        return;
      }
      stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${path === '/api/repair-draft' && method !== 'DELETE' ? M.draftSaveFailed + ' ' : ''}${escapeHtml(result.message || M.actionFailed)} ${path === '/api/proposal/accept' ? M.acceptanceNotAdvanced : ''}</p>`);
      if (response.headers.get('ETag')) etag = response.headers.get('ETag'); return;
    }
    if (path === '/api/repair-draft') draftSaveFailed = false;
    bulkPreview = null;
    etag = response.headers.get('ETag'); render(result);
  } catch (_) {
    if (path === '/api/repair-draft' && method !== 'DELETE') { draftSaveFailed = true; document.querySelector('#solve-draft')?.setAttribute('disabled', ''); }
    stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${path === '/api/repair-draft' && method !== 'DELETE' ? M.draftSaveFailed : M.actionFailed}</p>`);
  }
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
    if (inspectionState) {
      if (event.matches && !view.focusedType) syncInspectionState(inspectionState.openFocused('cohortId', acceptedModel.definition.cohorts[0]?.id, null).state);
      if (!event.matches) syncInspectionState(inspectionState.returnToWholeSchool().state);
    } else {
      if (event.matches && !view.focusedType) { view.focusedType = 'cohortId'; view.focusedId = acceptedModel.definition.cohorts[0]?.id || null; }
      if (!event.matches) view.focusedType = null;
    }
    load(true);
  }
});

function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character]); }
function escapeAttribute(value) { return escapeHtml(value); }

load().catch(() => { currentLabel.textContent = M.unavailable; stateCard.innerHTML = `<p class="error">${M.unavailableDetail}</p>`; });
