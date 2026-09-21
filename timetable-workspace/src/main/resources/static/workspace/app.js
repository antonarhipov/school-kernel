let etag;
let csrf;
let pollTimer;

const stateCard = document.querySelector('#state-card');
const importCard = document.querySelector('#import-card');
const currentLabel = document.querySelector('#current-label');
const importStatus = document.querySelector('#import-status');

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
    currentLabel.textContent = 'No accepted timetable';
    stateCard.innerHTML = '<span class="state">Empty workspace</span><h2>Bring this school into the workspace</h2><p>No draft or timetable has been imported.</p>';
    importCard.hidden = false;
    return;
  }
  importCard.hidden = true;
  const schoolName = school?.displayName || 'School';
  if (snapshot.state === 'INITIAL_DRAFT') {
    currentLabel.textContent = 'No accepted timetable';
    const definition = snapshot.workspace.initialDefinition;
    const lastRun = snapshot.workspace.lastRun;
    stateCard.innerHTML = `
      <span class="state">Initial draft</span>
      <h2>${escapeHtml(schoolName)}</h2>
      <p>The definition is verified and durably stored. Review the summary before requesting the first timetable.</p>
      ${summary(definition)}
      ${lastRun?.message ? `<p class="notice" role="status"><strong>${escapeHtml(lastRun.message)}</strong> The initial draft is unchanged and no timetable is accepted.</p>` : ''}
      <p class="muted">Definition revision <code>${escapeHtml(snapshot.workspace.definitionRevision)}</code></p>
      <div class="actions"><button id="start-plan">Create 30-second proposal</button></div>
      <form id="replace-definition" class="replace-form">
        <label>Replace initial definition <input name="definition" type="file" accept="application/json,.json" required></label>
        <button type="submit" class="secondary">Validate and replace draft</button>
      </form>`;
    bindInitialActions();
  } else if (snapshot.state === 'SOLVING_INITIAL') {
    currentLabel.textContent = 'No accepted timetable';
    const run = snapshot.workspace.run;
    stateCard.innerHTML = `
      <span class="state running">Planning</span>
      <h2>${escapeHtml(schoolName)}</h2>
      <p>School Kernel is creating an initial proposal. Navigation remains available and no timetable is accepted yet.</p>
      <dl><div><dt>Execution limit</dt><dd>${escapeHtml(run.limit)}</dd></div><div><dt>Status</dt><dd>Running</dd></div></dl>
      <div class="actions"><button id="cancel-run" class="danger">Cancel run</button></div>`;
    document.querySelector('#cancel-run').addEventListener('click', () => cancelRun(run.id));
    pollTimer = setTimeout(load, 300);
  } else if (snapshot.state === 'INITIAL_PROPOSAL') {
    currentLabel.textContent = 'No accepted timetable';
    const proposal = snapshot.workspace.proposal;
    const result = proposal.result;
    stateCard.innerHTML = `
      <span class="state proposal">Initial proposal · feasible</span>
      <h2>${escapeHtml(schoolName)}</h2>
      <p>This complete timetable is a proposal. No timetable is accepted yet.</p>
      <dl>
        <div><dt>Lessons</dt><dd>${result.timetable.assignments.length}</dd></div>
        <div><dt>Termination reason</dt><dd>${escapeHtml(result.terminationReason)}</dd></div>
        <div><dt>Execution limit</dt><dd>${escapeHtml(proposal.limit)}</dd></div>
        <div><dt>Timetable revision</dt><dd><code>${escapeHtml(proposal.proposedTimetableRevision)}</code></dd></div>
      </dl>
      ${assignmentTable(result.timetable.assignments)}
      <label class="confirmation"><input id="confirm-accept" type="checkbox"> I confirm this proposal should become the initial accepted timetable.</label>
      <div class="actions"><button id="accept-proposal" disabled>Accept as current</button><button id="discard-proposal" class="secondary">Discard proposal</button></div>`;
    const confirmation = document.querySelector('#confirm-accept');
    const accept = document.querySelector('#accept-proposal');
    confirmation.addEventListener('change', () => { accept.disabled = !confirmation.checked; });
    accept.addEventListener('click', () => mutate('/api/proposal/accept', 'POST'));
    document.querySelector('#discard-proposal').addEventListener('click', () => mutate('/api/proposal', 'DELETE'));
  } else {
    currentLabel.textContent = `Accepted timetable · ${schoolName}`;
    const accepted = snapshot.workspace.acceptedBaseline;
    stateCard.innerHTML = `
      <span class="state accepted">Accepted baseline</span>
      <h2>${escapeHtml(schoolName)}</h2>
      <p>The complete definition and matching feasible result are now the current accepted timetable.</p>
      <p class="muted">Timetable revision <code>${escapeHtml(snapshot.workspace.timetableRevision)}</code></p>
      ${assignmentTable(accepted?.result?.timetable?.assignments || [])}`;
  }
}

function summary(definition) {
  return `<dl>
    <div><dt>Lessons</dt><dd>${definition.lessons.length}</dd></div>
    <div><dt>Teachers</dt><dd>${definition.teachers.length}</dd></div>
    <div><dt>Classes</dt><dd>${definition.cohorts.length}</dd></div>
    <div><dt>Rooms</dt><dd>${definition.rooms.length}</dd></div>
    <div><dt>Weekly periods</dt><dd>${definition.periods.length}</dd></div>
  </dl>`;
}

function assignmentTable(assignments) {
  const rows = assignments.map(item => `<tr><td>${escapeHtml(item.lessonId)}</td><td>${escapeHtml(item.subjectId)}</td><td>${escapeHtml(item.cohortId)}</td><td>${escapeHtml(item.teacherId)}</td><td>${escapeHtml(item.periodId)}</td><td>${escapeHtml(item.roomId)}</td></tr>`).join('');
  return `<div class="table-wrap"><table><caption>Timetable details</caption><thead><tr><th>Lesson</th><th>Subject</th><th>Class</th><th>Teacher</th><th>Period</th><th>Room</th></tr></thead><tbody>${rows}</tbody></table></div>`;
}

function bindInitialActions() {
  document.querySelector('#start-plan').addEventListener('click', () => mutate('/api/runs', 'POST'));
  document.querySelector('#replace-definition').addEventListener('submit', async event => {
    event.preventDefault();
    await mutate('/api/initial-draft/replace', 'POST', new FormData(event.currentTarget));
  });
}

async function cancelRun(id) {
  await mutate(`/api/runs/${encodeURIComponent(id)}`, 'DELETE');
}

async function mutate(path, method, body) {
  const response = await fetch(path, {
    method,
    headers: { [csrf.headerName]: csrf.token, 'If-Match': etag },
    body
  });
  const result = await response.json();
  if (!response.ok) {
    stateCard.insertAdjacentHTML('beforeend', `<p class="error" role="alert">${escapeHtml(result.message || 'The action did not complete.')}</p>`);
    if (response.headers.get('ETag')) etag = response.headers.get('ETag');
    return;
  }
  etag = response.headers.get('ETag');
  render(result);
}

async function submit(form) {
  importStatus.className = '';
  importStatus.textContent = 'Verifying locally…';
  const button = form.querySelector('button');
  button.disabled = true;
  try {
    const response = await fetch('/api/import', {
      method: 'POST',
      headers: { [csrf.headerName]: csrf.token, 'If-Match': etag },
      body: new FormData(form)
    });
    const body = await response.json();
    if (!response.ok) throw new Error(body.message || 'Import did not complete.');
    etag = response.headers.get('ETag');
    render(body);
    importStatus.textContent = '';
  } catch (error) {
    importStatus.className = 'error';
    importStatus.textContent = error.message;
  } finally {
    button.disabled = false;
  }
}

document.querySelector('#json-import').addEventListener('submit', event => {
  event.preventDefault();
  submit(event.currentTarget);
});
document.querySelector('#archive-import').addEventListener('submit', event => {
  event.preventDefault();
  submit(event.currentTarget);
});

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[character]);
}

load().catch(() => {
  currentLabel.textContent = 'Workspace unavailable';
  stateCard.innerHTML = '<p class="error">The local workspace could not be loaded.</p>';
});
