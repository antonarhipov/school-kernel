import { prefix, useKey, maxBytes, bytes, validIdentifier, syntaxFeedback, validateText, readEnvelope, savedRows, deleteVersions, savedVersionInUse, rememberDefinitionUse } from './demo-definitions.js';
let panel, selection, editor, feedback, use, changed, entries = [], selected = 'full', storedText = '', origin;
let api, current, preview, inUse, useHint, loading = false, using = false, selectionRequest = 0, fileRequest = 0, markRequest = 0, sourceRaw = null, stale = false, rows = [];
let sourceDescription = '';
const normalized = text => text.replace(/\r\n?/g, '\n');
const dirty = () => editor.value !== normalized(storedText);
const exactText = () => dirty() ? editor.value : storedText;
const message = text => { feedback.textContent = text; };

export async function mountDemoPanel(actions, config) {
  api = actions;
  const banner = document.createElement('section');
  banner.id = 'demo-retention'; banner.className = 'card notice';
  banner.innerHTML = `<strong>Temporary public demo · export accepted timetables to keep them.</strong>
    <p>Workspaces expire after ${Number(config.idleMinutes)} minutes without activity or ${Number(config.absoluteHours)} hours in total, and on every restart or deployment. Unfinished repair and manual work disappears.</p>
    <details><summary>Privacy and storage</summary><p>Your workspace is held in memory. Private temporary kernel files are cleaned up. Complete identical inputs, results and successful verification may remain in a shared memory cache for up to one hour after computation, independently of your session, subject to eviction; a restart clears them.</p>
    <p>Saved definitions stay on this origin, browser and device until removed or evicted. Other users of a shared browser can read them. Download is the durable copy. Site-data clearing, browser eviction, or private-window closure can remove browser versions.</p>
    <p>Railway edge logs include IP address, user agent and URL and cannot be disabled or redacted. Hobby viewing lasts seven days; provider staff report 90-day underlying storage, deletion afterward, in US West even for EU services.</p>
    <p>Only the necessary application session cookie is used; application logs exclude school data. Do not submit personal data you are not permitted to share. Keep documents and personal details out of URLs.</p></details>`;
  panel = document.createElement('section'); panel.id = 'demo-definition-panel'; panel.className = 'card';
  panel.innerHTML = `<h2>School definition</h2><label>Choose a definition<select id="demo-definition"></select></label>
    <p id="demo-sample-preview"></p><p id="demo-in-use" role="status"></p><label>Definition JSON<textarea id="demo-json" spellcheck="false" rows="10"></textarea></label>
    <p id="demo-edit-status" role="status"></p><p id="demo-storage-status" role="status"></p><button id="demo-refresh-version" class="secondary" hidden>Reload saved version</button><ul id="demo-unreadable"></ul><div class="actions"><button id="demo-use">Use this sample</button>
    <button id="demo-save" class="secondary">Save as</button><button id="demo-revert" class="secondary">Revert changes</button>
    <button id="demo-download" class="secondary">Download JSON</button><button id="demo-delete" class="secondary">Delete saved version</button>
    <button id="demo-delete-all" class="secondary" title="Deletes only definitions saved for this site in this browser. Bundled samples and your active workspace are kept. The JSON editor text is retained.">Delete all definitions saved in this browser</button></div>
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
    editor.value = storedText; update(); message('Exact opened text restored.');
  });
  panel.querySelector('#demo-download').addEventListener('click', () => {
    const url = URL.createObjectURL(new Blob([exactText()], { type: 'application/json' }));
    const link = document.createElement('a'); link.href = url; link.download = `${selected || 'definition'}.json`; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });
  panel.querySelector('#demo-delete').addEventListener('click', () => removeSaved([selected]));
  panel.querySelector('#demo-delete-all').addEventListener('click', () => {
    try { removeSaved(savedRows(localStorage).map(row => row.id), true); }
    catch { message('Browser storage could not be read. Your text remains available to download.'); }
  });
  panel.querySelector('#demo-refresh-version').addEventListener('click', () => select(selected));
  panel.querySelector('#demo-file').addEventListener('change', async event => {
    const file = event.target.files[0]; event.target.value = ''; if (!file) return;
    const request = ++fileRequest;
    if (file.size > maxBytes) { message('The definition must be at most 2 MiB UTF-8.'); return; }
    try {
      const text = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(await file.arrayBuffer());
      if (request !== fileRequest) return;
      if (bytes(text) > maxBytes) throw new Error('The definition must be at most 2 MiB UTF-8.');
      if (dirty() && !confirm('Discard the unsaved definition text and open this file?')) return;
      ++selectionRequest; loading = false;
      selected = ''; origin = { kind: 'file' }; storedText = text; sourceRaw = null; stale = false;
      editor.value = text; selection.value = '';
      sourceDescription = 'Opened file · save a browser version before use.'; preview.textContent = sourceDescription;
      panel.dataset.fileSuggestion = file.name.replace(/\.json$/i, '').toLowerCase().replace(/[^a-z0-9-]/g, '-').slice(0, 40);
      update(); message('Opened locally. Save a browser version before use.');
    } catch (error) { message(`${error.message || 'This file could not be read.'} Previous selection and text were retained.`); }
  });
  use.addEventListener('click', async () => {
    if (use.disabled) return;
    const isSample = entries.some(entry => entry.identifier === selected);
    let saved, raw;
    if (!isSample) {
      try {
        saved = readEnvelope(localStorage, selected); raw = localStorage.getItem(prefix + selected);
        if (raw !== sourceRaw || saved.documentText !== exactText()) throw new Error('The saved version changed. Reload it or Save as first.');
      } catch (error) { refreshList(); update(); message(error.message); return; }
    }
    let confirmed = false;
    if (current?.state !== 'EMPTY') {
      const accepted = current?.acceptedTimetable ? ' Export Current first if you want to keep it; Cancel and use Export accepted baseline in Utilities.' : '';
      const work = { ACCEPTED_BASELINE: 'accepted timetable', INITIAL_DRAFT: 'initial draft', INITIAL_PROPOSAL: 'initial draft and unaccepted proposal', REPAIR_DRAFT: 'accepted timetable and repair draft', REPAIR_PROPOSAL: 'accepted timetable, repair draft and unaccepted proposal', MANUAL_DRAFT: 'accepted timetable and manual draft' }[current.state] || current.state;
      const title = current.workspace?.school?.displayName || 'this school';
      confirmed = confirm(`Replace ${work} for "${title}" with ${isSample ? entries.find(entry => entry.identifier === selected).title : 'a new initial draft from the selected saved definition'}?${accepted}`);
      if (!confirmed) return;
    }
    using = true; update();
    try {
      const loaded = isSample ? await api.useSample(selected, confirmed)
        : await api.useDefinition(saved.documentText, confirmed, safeSource(saved));
      try { await rememberDefinitionUse(localStorage, selected, loaded); } catch { /* actual server sample origin still identifies Current */ }
      message(isSample ? 'The accepted sample is in use. No search was started.' : 'The definition is in use as a new initial draft. No timetable is Current and no search was started.');
    }
    catch (error) {
      message(`${error.message} The selected definition was not used. Reload current workspace to check or retry.`);
      panel.querySelector('#demo-reload').hidden = false;
    }
    finally { using = false; update(); }
  });
  window.addEventListener('storage', event => {
    if (event.key !== null && event.key !== useKey && !event.key.startsWith(prefix)) return;
    refreshList(); update();
  });
}

function refreshList() {
  selection.replaceChildren();
  const samples = document.createElement('optgroup'); samples.label = 'Sample schools';
  for (const entry of entries) {
    const option = document.createElement('option'); option.value = entry.identifier; option.textContent = entry.title; samples.append(option);
  }
  const saved = document.createElement('optgroup'); saved.label = 'Saved in this browser';
  const unreadable = panel.querySelector('#demo-unreadable'); unreadable.replaceChildren();
  try {
    rows = savedRows(localStorage);
    for (const row of rows) {
      const option = document.createElement('option'); option.value = row.id;
      option.textContent = row.value ? `${row.id} · ${row.value.savedAt}` : `${row.id} · unreadable`;
      option.disabled = !row.value; saved.append(option);
      if (!row.value) {
        const item = document.createElement('li'), button = document.createElement('button');
        item.append(document.createTextNode(`${row.id} · unreadable. `));
        button.textContent = `Delete unreadable version "${row.id}"`; button.className = 'secondary';
        button.addEventListener('click', () => removeSaved([row.id])); item.append(button); unreadable.append(item);
      }
    }
    if (selected && origin?.kind === 'saved') stale = localStorage.getItem(prefix + selected) !== sourceRaw;
  } catch { message('Browser storage is unavailable. Your text can still be downloaded; deleting older versions may free space.'); }
  selection.append(samples, saved);
  if (selected && !Array.from(selection.options).some(option => option.value === selected)) {
    const retained = document.createElement('option'); retained.value = selected; retained.textContent = `${selected} · removed elsewhere (text retained)`;
    retained.disabled = true; saved.append(retained);
  }
  selection.value = selected;
}

function removeSaved(ids, all = false) {
  if (!ids.length) return;
  const warning = dirty() ? ' The unsaved editor text will be retained.' : '';
  if (!confirm(all
    ? `Delete all definitions saved for this site in this browser? Bundled samples and your active workspace will be kept. The editor text will be retained.`
    : `Delete saved version "${ids[0]}"?${warning} Your active workspace is kept.`)) return;
  try {
    deleteVersions(localStorage, ids);
    // Deletion never discards open text, including edits or a version removed from another tab.
    if (origin?.kind === 'saved' && ids.includes(selected)) { selected = ''; sourceRaw = null; stale = false;
      storedText = ''; sourceDescription = 'Deleted browser version · editor text retained; save or download to keep it.';
      preview.textContent = sourceDescription; }
    refreshList(); update(); message('Deleted from this browser. Your workspace and editor text were retained.');
  } catch { refreshList(); update(); message('Browser storage could not be changed. Your text remains available to download.'); }
}

async function select(id, check = true) {
  if (check && (dirty() || stale) && !confirm('Discard the unsaved definition text?')) return;
  ++fileRequest;
  const request = ++selectionRequest;
  loading = true; update(); message('');
  const sample = entries.find(entry => entry.identifier === id);
  try {
    let text, nextOrigin, description, raw = null;
    if (sample) {
      const response = await fetch(`/api/samples/${encodeURIComponent(id)}`);
      if (!response.ok) throw new Error('This sample could not be read.');
      const body = await response.json(); text = JSON.stringify(body.definition, null, 2) + '\n';
      nextOrigin = { kind: 'sample', identifier: id };
      const effort = sample.lessons > api.lessonThreshold ? 'Thorough' : 'Quick';
      const seconds = sample.lessons > api.lessonThreshold ? api.thoroughSeconds : api.quickSeconds;
      description = `${sample.description} ${sample.lessons} lessons · ${sample.cohorts} classes · ${sample.teachers} teachers · ${sample.rooms} rooms · ${sample.periods} periods. ${effort} search (${seconds} seconds) is the default for a later solve. Selection only previews.`;
    } else {
      const value = readEnvelope(localStorage, id); raw = localStorage.getItem(prefix + id); text = value.documentText; nextOrigin = { kind: 'saved', identifier: id };
      description = `Saved ${value.savedAt} · derived from ${value.derivedFrom.kind}${value.derivedFrom.identifier ? ` ${value.derivedFrom.identifier}` : ''}.`;
    }
    if (request !== selectionRequest) return;
    selected = id; selection.value = id; storedText = text; origin = nextOrigin; sourceRaw = raw; stale = false;
    sourceDescription = description; preview.textContent = description; editor.disabled = false; editor.value = storedText;
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
  const syntax = syntaxFeedback(editor.value);
  if (syntax) changed.textContent += ` · ${syntax}`;
  panel.querySelector('#demo-storage-status').textContent = stale ? 'This saved version changed or was removed in another tab. Your open text is retained. Reload it explicitly or Save as to keep your text.' : '';
  panel.querySelector('#demo-refresh-version').hidden = !stale || !rows.some(row => row.id === selected && row.value);
  const running = current?.state?.startsWith('SOLVING_');
  const narrow = window.matchMedia('(max-width: 700px)').matches;
  editor.readOnly = narrow;
  use.textContent = isSample ? 'Use this sample' : 'Use this definition';
  const isSaved = origin?.kind === 'saved' && rows.some(row => row.id === selected && row.value);
  selection.disabled = using; editor.disabled = using || loading;
  use.disabled = loading || using || dirty() || stale || editor.disabled || running || current?.state === 'SESSION_ENDED' || (!isSample && !isSaved);
  use.title = loading ? 'Loading sample preview.' : using ? 'Validating and opening the selected definition.' : running ? 'Wait for or cancel the active run first.' : stale ? 'Reload the saved version or Save as first.' : dirty() ? 'Save or revert the changed text first.' : !isSample && !isSaved ? 'Save a browser version before use.' : '';
  useHint.textContent = use.title;
  updateInUse();
  panel.querySelector('#demo-delete').disabled = using || isSample || !selected;
  panel.querySelector('#demo-revert').disabled = !dirty();
  panel.querySelector('#demo-save').disabled = editor.disabled || narrow;
  panel.querySelector('#demo-file').disabled = narrow || using;
  panel.querySelector('#demo-download').disabled = loading;
  panel.querySelector('#demo-delete-all').disabled = using || !rows.length;
  if (narrow) changed.textContent += ' · Definition editing requires a wider view.';
}

async function updateInUse() {
  const request = ++markRequest, snapshot = current;
  let active = snapshot?.workspace?.importMode === 'PACKAGED_SAMPLE' && snapshot?.acceptedTimetable ? snapshot.workspace.origin : null;
  if (!active) {
    try { active = await savedVersionInUse(localStorage, snapshot); } catch { /* missing/corrupt/unavailable mark is never authority */ }
  }
  if (request !== markRequest) return;
  for (const option of selection.options) {
    const entry = option.parentElement.label === 'Sample schools' ? entries.find(entry => entry.identifier === option.value) : null, row = rows.find(row => row.id === option.value);
    if (entry) option.textContent = entry.title + (entry.identifier === active ? ' · In use' : '');
    else if (row?.value) option.textContent = `${row.id} · ${row.value.savedAt}${row.id === active ? ' · In use' : ''}`;
  }
  inUse.textContent = active ? `${entries.find(entry => entry.identifier === active)?.title || active} · In use${snapshot.acceptedTimetable ? ' as Current' : ' as initial draft'}`
    : snapshot?.acceptedTimetable ? `Current origin: ${snapshot.workspace.importMode === 'REPAIR_ACCEPTANCE' ? 'accepted repair' : snapshot.workspace.importMode === 'MANUAL_EDIT_PUBLICATION' ? 'manual changes' : snapshot.workspace.importMode === 'ACCEPTED_BASELINE' ? 'imported bundle' : 'custom definition'}.` : '';
}

async function save() {
  try { validateText(exactText()); } catch (error) { message(`Save refused: ${error.message}`); return; }
  const isSample = entries.some(entry => entry.identifier === selected);
  const id = prompt('Identifier for the version saved in this browser', isSample ? `${selected}-1` : selected || panel.dataset.fileSuggestion || 'my-school');
  if (id === null) return;
  if (!validIdentifier(id)) { message('Use 1–40 lowercase letters, numbers or hyphens, starting with a letter or number. Sample identifiers are reserved.'); return; }
  try {
    const previous = localStorage.getItem(prefix + id);
    if (previous !== null && !confirm(`Replace saved version "${id}"?`)) return;
    const value = { formatVersion: 1, identifier: id, savedAt: new Date().toISOString(), derivedFrom: origin, documentText: exactText() };
    // setItem is atomic: a quota/security failure retains the existing complete value.
    const raw = JSON.stringify(value); localStorage.setItem(prefix + id, raw);
    storedText = value.documentText; selected = id; origin = { kind: 'saved', identifier: id }; sourceRaw = raw; stale = false;
    sourceDescription = `Saved ${value.savedAt} · derived from ${value.derivedFrom.kind}${value.derivedFrom.identifier ? ` ${value.derivedFrom.identifier}` : ''}.`;
    preview.textContent = sourceDescription;
    refreshList(); update(); message('Saved in this browser. Download is the durable copy; saving does not check kernel validity.');
  } catch { message('Browser storage is unavailable or full. Nothing was saved. Your edited text is retained. Download it or delete older versions.'); }
}

export function updateDemoPanel(snapshot) {
  if (snapshot.acceptedTimetable && snapshot.workspace?.importMode !== 'PACKAGED_SAMPLE' && feedback?.textContent.includes('sample is in use')) message('Current is accepted. Export the accepted timetable to keep it.');
  if (snapshot.acceptedTimetable && feedback?.textContent.includes('No timetable is Current')) message('Current is accepted. Export the accepted timetable to keep it.');
  current = snapshot; update();
}

function safeSource(value) {
  const seen = new Set();
  while (value?.derivedFrom) {
    const source = value.derivedFrom;
    if (source.kind === 'sample' && entries.some(entry => entry.identifier === source.identifier)) return source.identifier;
    if (source.kind === 'file') return 'file';
    if (source.kind !== 'saved' || seen.has(source.identifier)) break;
    seen.add(source.identifier);
    try { value = readEnvelope(localStorage, source.identifier); } catch { break; }
  }
  return 'custom';
}
