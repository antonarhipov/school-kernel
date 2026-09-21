let etag;
let csrf;

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
  const school = snapshot.workspace.school;
  if (snapshot.state === 'EMPTY') {
    currentLabel.textContent = 'No accepted timetable';
    stateCard.innerHTML = '<span class="state">Empty workspace</span><h2>Bring this school into the workspace</h2><p>No draft or timetable has been imported.</p>';
    importCard.hidden = false;
    return;
  }
  importCard.hidden = true;
  const schoolName = school?.displayName || school?.id || 'School';
  const missingName = school?.displayNameAvailable === false
    ? '<p class="muted">School name unavailable; showing the stable school ID.</p>'
    : '';
  if (snapshot.state === 'INITIAL_DRAFT') {
    currentLabel.textContent = 'No accepted timetable';
    stateCard.innerHTML = `<span class="state">Initial draft</span><h2>${escapeHtml(schoolName)}</h2>${missingName}<p>The definition is verified and durably stored. It is awaiting initial planning; no timetable is accepted yet.</p><p class="muted">Definition revision <code>${escapeHtml(snapshot.workspace.definitionRevision)}</code></p>`;
  } else {
    currentLabel.textContent = `Accepted timetable · ${schoolName}`;
    stateCard.innerHTML = `<span class="state">Accepted baseline</span><h2>${escapeHtml(schoolName)}</h2>${missingName}<p>The complete definition and matching feasible result are the current accepted timetable.</p><p class="muted">Timetable revision <code>${escapeHtml(snapshot.workspace.timetableRevision)}</code></p>`;
  }
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
