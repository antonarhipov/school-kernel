const prefix = 'school-kernel.definition.v1.';
const maxBytes = 2 * 1024 * 1024;
const validId = /^[a-z0-9][a-z0-9-]{0,39}$/;
let panel, selection, editor, feedback, use, changed, entries = [], selected = 'full', storedText = '', origin;
let api, current, preview, inUse, useHint, loading = false, using = false, selectionRequest = 0;
const bytes = text => new TextEncoder().encode(text).length;
const dirty = () => editor.value !== storedText;
const message = text => { feedback.textContent = text; };

export async function mountDemoPanel(actions, config) {
  api = actions;
  const banner = document.createElement('section');
  banner.id = 'demo-retention'; banner.className = 'card notice';
  banner.innerHTML = `<strong>Temporary public demo · export accepted timetables to keep them.</strong>
    <p>Workspaces expire after ${Number(config.idleMinutes)} minutes without activity or ${Number(config.absoluteHours)} hours in total, and on every restart or deployment. Unfinished repair and manual work disappears.</p>
    <details><summary>Privacy and storage</summary><p>Your workspace is held in memory. Private temporary kernel files are cleaned up. Complete identical inputs, results and successful verification may remain in a shared memory cache for up to one hour after computation, independently of your session, subject to eviction; a restart clears them.</p>
    <p>Saved definitions stay on this origin, browser and device until removed or evicted. Other users of a shared browser can read them. Download is the durable copy.</p>
    <p>Railway edge logs include IP address, user agent and URL and cannot be disabled or redacted. Hobby viewing lasts seven days; provider staff report 90-day underlying storage, deletion afterward, in US West even for EU services.</p>
    <p>Only the necessary application session cookie is used; application logs exclude school data. Do not submit personal data you are not permitted to share. Keep documents and personal details out of URLs.</p></details>`;
  panel = document.createElement('section'); panel.id = 'demo-definition-panel'; panel.className = 'card';
  panel.innerHTML = `<h2>School definition</h2><label>Choose a definition<select id="demo-definition"></select></label>
    <p id="demo-sample-preview"></p><p id="demo-in-use" role="status"></p><label>Definition JSON<textarea id="demo-json" spellcheck="false" rows="10"></textarea></label>
    <p id="demo-edit-status" role="status"></p><div class="actions"><button id="demo-use">Use this sample</button>
    <button id="demo-save" class="secondary">Save as</button><button id="demo-revert" class="secondary">Revert changes</button>
    <button id="demo-download" class="secondary">Download JSON</button><button id="demo-delete" class="secondary">Delete saved version</button>
    <button id="demo-delete-all" class="secondary" title="Deletes only definitions saved for this site in this browser. Bundled samples and your active workspace are kept. The JSON editor resets to the Full sample preview.">Delete all definitions saved in this browser</button></div>
    <label>Open a definition file<input id="demo-file" type="file" accept="application/json,.json"></label>
    <p id="demo-use-hint" role="status"></p><button id="demo-reload" class="secondary" hidden>Reload current workspace</button><p>Browser versions have no cross-device sync. Download a copy to keep it.</p><p id="demo-feedback" role="status"></p>`;
  const main = document.querySelector('main'); main.prepend(panel); main.prepend(banner);
  selection = panel.querySelector('#demo-definition'); editor = panel.querySelector('#demo-json');
  feedback = panel.querySelector('#demo-feedback'); use = panel.querySelector('#demo-use');
  changed = panel.querySelector('#demo-edit-status'); preview = panel.querySelector('#demo-sample-preview');
  inUse = panel.querySelector('#demo-in-use'); useHint = panel.querySelector('#demo-use-hint');
  panel.querySelector('#demo-reload').addEventListener('click', async () => {
    try { await api.reload(); panel.querySelector('#demo-reload').hidden = true; message('Current workspace reloaded. Your selected definition and text were retained.'); }
    catch (error) { message(error.message); }
  });
  const response = await fetch('/api/samples');
  if (!response.ok) throw new Error('The sample catalog is unavailable.');
  entries = await response.json();
  refreshList(); await select('full', false);
  selection.addEventListener('change', async () => {
    const next = selection.value;
    if (dirty() && !confirm('Discard the unsaved definition text?')) { selection.value = selected; return; }
    await select(next, false);
  });
  editor.addEventListener('input', update);
  window.matchMedia('(max-width: 700px)').addEventListener('change', update);
  panel.querySelector('#demo-save').addEventListener('click', save);
  panel.querySelector('#demo-revert').addEventListener('click', () => {
    if (dirty() && !confirm('Discard the unsaved text and restore the exact stored definition?')) return;
    editor.value = storedText; update();
  });
  panel.querySelector('#demo-download').addEventListener('click', () => {
    const url = URL.createObjectURL(new Blob([editor.value], { type: 'application/json' }));
    const link = document.createElement('a'); link.href = url; link.download = `${selected || 'definition'}.json`; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });
  panel.querySelector('#demo-delete').addEventListener('click', async () => {
    if (entries.some(entry => entry.identifier === selected)) return;
    if (!confirm(`Delete saved version "${selected}"?`)) return;
    try { localStorage.removeItem(prefix + selected); refreshList(); await select('full', false); }
    catch { message('Browser storage could not be changed. Your text remains available to download.'); }
  });
  panel.querySelector('#demo-delete-all').addEventListener('click', async () => {
    if (!confirm('Delete all definitions saved for this site in this browser? Bundled samples and your active workspace will be kept. The JSON editor will reset to the Full sample preview; any unsaved text will be discarded.')) return;
    try {
      for (const key of Object.keys(localStorage).filter(key => key.startsWith(prefix))) localStorage.removeItem(key);
      refreshList(); await select('full', false);
    } catch { message('Browser storage could not be changed. Your text remains available to download.'); }
  });
  panel.querySelector('#demo-file').addEventListener('change', async event => {
    const file = event.target.files[0]; if (!file) return;
    if (file.size > maxBytes) { message('The definition must be at most 2 MiB UTF-8.'); return; }
    try {
      const text = await file.text();
      if (bytes(text) > maxBytes) throw new Error('The definition must be at most 2 MiB UTF-8.');
      if (dirty() && !confirm('Discard the unsaved definition text and open this file?')) return;
      selected = ''; origin = { kind: 'file' }; storedText = ''; editor.disabled = false; editor.value = text;
      selection.value = ''; preview.textContent = 'Opened file · save a browser version before use.';
      panel.dataset.fileSuggestion = file.name.replace(/\.json$/i, '').toLowerCase().replace(/[^a-z0-9-]/g, '-').slice(0, 40);
      update();
    } catch (error) { message(error.message || 'This file could not be read.'); }
  });
  use.addEventListener('click', async () => {
    if (using || loading || dirty() || current?.state?.startsWith('SOLVING_') || !entries.some(entry => entry.identifier === selected)) return;
    let confirmed = false;
    if (current?.state !== 'EMPTY') {
      const accepted = current?.acceptedTimetable ? ' Export Current first if you want to keep it; Cancel and use Export accepted baseline in Utilities.' : '';
      const work = { ACCEPTED_BASELINE: 'accepted timetable', INITIAL_DRAFT: 'initial draft', INITIAL_PROPOSAL: 'initial draft and unaccepted proposal', REPAIR_DRAFT: 'accepted timetable and repair draft', REPAIR_PROPOSAL: 'accepted timetable, repair draft and unaccepted proposal', MANUAL_DRAFT: 'accepted timetable and manual draft' }[current.state] || current.state;
      const title = current.workspace?.school?.displayName || 'this school';
      confirmed = confirm(`Replace ${work} for "${title}" with ${entries.find(entry => entry.identifier === selected).title}?${accepted}`);
      if (!confirmed) return;
    }
    using = true; update();
    try { await api.useSample(selected, confirmed); message('The accepted sample is in use. No search was started.'); }
    catch (error) {
      message(`${error.message} The sample was not opened. Reload current workspace to check or retry.`);
      panel.querySelector('#demo-reload').hidden = false;
    }
    finally { using = false; update(); }
  });
  window.addEventListener('storage', event => {
    if (event.key !== null && !event.key.startsWith(prefix)) return;
    refreshList(); update();
  });
}

function envelope(id) {
  const value = JSON.parse(localStorage.getItem(prefix + id));
  const fields = ['formatVersion', 'identifier', 'savedAt', 'derivedFrom', 'documentText'];
  if (!value || Object.keys(value).sort().join() !== fields.sort().join()
      || value.formatVersion !== 1 || value.identifier !== id || !validId.test(id)
      || entries.some(entry => entry.identifier === id) || typeof value.documentText !== 'string'
      || bytes(value.documentText) > maxBytes || !Number.isFinite(Date.parse(value.savedAt))
      || !value.derivedFrom || !['sample', 'saved', 'file'].includes(value.derivedFrom.kind)) throw new Error('Unreadable saved version');
  const parsed = JSON.parse(value.documentText);
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error('Unreadable saved version');
  return value;
}

function refreshList() {
  selection.replaceChildren();
  const samples = document.createElement('optgroup'); samples.label = 'Sample schools';
  for (const entry of entries) {
    const option = document.createElement('option'); option.value = entry.identifier; option.textContent = entry.title; samples.append(option);
  }
  const saved = document.createElement('optgroup'); saved.label = 'Saved in this browser';
  try {
    const rows = Object.keys(localStorage).filter(key => key.startsWith(prefix)).map(key => {
      const id = key.slice(prefix.length); try { return { id, value: envelope(id) }; } catch { return { id }; }
    }).sort((left, right) => (right.value?.savedAt || '').localeCompare(left.value?.savedAt || '') || left.id.localeCompare(right.id));
    for (const row of rows) {
      const option = document.createElement('option'); option.value = row.id;
      option.textContent = row.value ? `${row.id} · ${row.value.savedAt}` : `${row.id} · unreadable`; saved.append(option);
    }
  } catch { message('Browser storage is unavailable. Your text can still be downloaded.'); }
  selection.append(samples, saved); selection.value = selected;
}

async function select(id, check = true) {
  if (check && dirty() && !confirm('Discard the unsaved definition text?')) return;
  const request = ++selectionRequest;
  loading = true; update(); message('');
  const sample = entries.find(entry => entry.identifier === id);
  try {
    let text, nextOrigin, description;
    if (sample) {
      const response = await fetch(`/api/samples/${encodeURIComponent(id)}`);
      if (!response.ok) throw new Error('This sample could not be read.');
      const body = await response.json(); text = JSON.stringify(body.definition, null, 2) + '\n';
      nextOrigin = { kind: 'sample', identifier: id };
      const effort = sample.lessons > api.lessonThreshold ? 'Thorough' : 'Quick';
      const seconds = sample.lessons > api.lessonThreshold ? api.thoroughSeconds : api.quickSeconds;
      description = `${sample.description} ${sample.lessons} lessons · ${sample.cohorts} classes · ${sample.teachers} teachers · ${sample.rooms} rooms · ${sample.periods} periods. ${effort} search (${seconds} seconds) is the default for a later solve. Selection only previews.`;
    } else {
      const value = envelope(id); text = value.documentText; nextOrigin = { kind: 'saved', identifier: id };
      description = `Saved ${value.savedAt} · derived from ${value.derivedFrom.kind}${value.derivedFrom.identifier ? ` ${value.derivedFrom.identifier}` : ''}.`;
    }
    if (request !== selectionRequest) return;
    selected = id; selection.value = id; storedText = text; origin = nextOrigin;
    preview.textContent = description; editor.disabled = false; editor.value = storedText;
  } catch (error) {
    if (request !== selectionRequest) return;
    selection.value = selected;
    message(`${error.message} Previous selection and text were retained.`);
  } finally {
    if (request === selectionRequest) { loading = false; update(); }
  }
}

function update() {
  if (!editor) return;
  const isSample = entries.some(entry => entry.identifier === selected);
  changed.textContent = dirty() ? 'Changed, not saved' : 'Unchanged';
  if (dirty()) {
    try { JSON.parse(editor.value); }
    catch (error) { changed.textContent += ` · ${error.message}`; }
  }
  const running = current?.state?.startsWith('SOLVING_');
  const narrow = window.matchMedia('(max-width: 700px)').matches;
  editor.readOnly = narrow;
  use.textContent = isSample ? 'Use this sample' : 'Use this definition';
  // Custom use is implemented by the initial-definition UC; preserving a local version does not validate it.
  selection.disabled = using; editor.disabled = using || loading;
  use.disabled = loading || using || dirty() || editor.disabled || running || current?.state === 'SESSION_ENDED' || !isSample;
  use.title = loading ? 'Loading sample preview.' : using ? 'Opening the selected sample.' : running ? 'Wait for or cancel the active run first.' : dirty() ? 'Save or revert the changed text first.' : !isSample ? 'This local version is available to edit and download.' : '';
  useHint.textContent = use.title;
  const active = current?.workspace?.importMode === 'PACKAGED_SAMPLE' && current?.acceptedTimetable ? current.workspace.origin : null;
  for (const option of selection.querySelectorAll('optgroup:first-child option')) {
    const entry = entries.find(entry => entry.identifier === option.value);
    option.textContent = entry.title + (entry.identifier === active ? ' · In use' : '');
  }
  inUse.textContent = active ? `${entries.find(entry => entry.identifier === active)?.title || active} · In use as Current` : '';
  panel.querySelector('#demo-delete').disabled = isSample || !selected;
  panel.querySelector('#demo-revert').disabled = !dirty();
  panel.querySelector('#demo-save').disabled = editor.disabled || narrow;
  panel.querySelector('#demo-file').disabled = narrow;
  if (narrow) changed.textContent += ' · Definition editing requires a wider view.';
}

async function save() {
  let parsed;
  try { parsed = JSON.parse(editor.value); }
  catch (error) { message(`Save refused: ${error.message}`); return; }
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') { message('Save requires a JSON object.'); return; }
  if (bytes(editor.value) > maxBytes) { message('Save requires at most 2 MiB UTF-8.'); return; }
  const isSample = entries.some(entry => entry.identifier === selected);
  const id = prompt('Identifier for the version saved in this browser', isSample ? `${selected}-1` : selected || panel.dataset.fileSuggestion || 'my-school');
  if (id === null) return;
  if (!validId.test(id) || entries.some(entry => entry.identifier === id)) { message('Use 1–40 lowercase letters, numbers or hyphens, starting with a letter or number. Sample identifiers are reserved.'); return; }
  try {
    if (localStorage.getItem(prefix + id) !== null && !confirm(`Replace saved version "${id}"?`)) return;
    const value = { formatVersion: 1, identifier: id, savedAt: new Date().toISOString(), derivedFrom: origin, documentText: editor.value };
    localStorage.setItem(prefix + id, JSON.stringify(value));
    storedText = editor.value; selected = id; origin = { kind: 'saved', identifier: id };
    refreshList(); update(); message('Saved in this browser. Download is the durable copy; saving does not check kernel validity.');
  } catch { message('Browser storage is unavailable or full. Your edited text is retained. Download it or delete older versions.'); }
}

export function updateDemoPanel(snapshot) { current = snapshot; update(); }
