let actions, current;

export function mountDemoImport(api) {
  actions = api;
  const panel = document.createElement('section');
  panel.id = 'demo-bundle-panel'; panel.className = 'card';
  panel.innerHTML = `<details><summary>Open an accepted timetable</summary>
    <p>Import an accepted ZIP bundle, or its matching complete definition and feasible result. This verifies the documents without solving. Definition JSON alone belongs in School definition above.</p>
    <form id="demo-bundle-form"><label>Accepted bundle ZIP (up to 5 MiB)<input id="demo-bundle-archive" name="archive" type="file" accept=".zip,application/zip"></label>
    <p>Or choose both JSON documents (up to 2 MiB each):</p>
    <label>School definition<input id="demo-bundle-definition" name="definition" type="file" accept=".json,application/json"></label>
    <label>Feasible timetable result<input id="demo-bundle-result" name="result" type="file" accept=".json,application/json"></label>
    <button id="demo-bundle-submit" type="submit">Verify and open accepted timetable</button><p id="demo-bundle-status" role="status"></p></form></details>`;
  document.querySelector('#demo-definition-panel').after(panel);
  panel.querySelector('form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = panel.querySelector('#demo-bundle-status'), button = panel.querySelector('button');
    const files = ['archive', 'definition', 'result'].map(name => [name, panel.querySelector(`[name=${name}]`).files[0]]);
    const archive = files[0][1], definition = files[1][1], result = files[2][1];
    if (archive && (definition || result)) { status.textContent = 'Choose the ZIP archive or both JSON documents, not both modes.'; return; }
    if (!archive && (!definition || !result)) { status.textContent = 'Choose an accepted archive, or both the matching complete definition and feasible result.'; return; }
    if (files.some(([name, file]) => file && file.size > (name === 'archive' ? 5 : 2) * 1024 * 1024)) { status.textContent = 'The archive must be at most 5 MiB; each JSON document at most 2 MiB. Existing work is unchanged.'; return; }
    let confirmed = false;
    if (current?.state !== 'EMPTY') {
      const work = { ACCEPTED_BASELINE: 'accepted timetable', INITIAL_DRAFT: 'initial draft', INITIAL_PROPOSAL: 'initial draft and unaccepted proposal', REPAIR_DRAFT: 'accepted timetable and repair draft', REPAIR_PROPOSAL: 'accepted timetable, repair draft and unaccepted proposal', MANUAL_DRAFT: 'accepted timetable and manual draft' }[current?.state] || current?.state;
      confirmed = confirm(`Replace ${work} with the imported accepted timetable?${current?.acceptedTimetable ? ' Export Current first to keep it; Cancel and use Export accepted baseline in Utilities.' : ''} Nothing is replaced until verification succeeds.`);
      if (!confirmed) return;
    }
    const form = new FormData();
    for (const [name, file] of files) if (file) form.append(name, file);
    form.append('confirmed', String(confirmed));
    button.disabled = true; status.textContent = 'Checking the complete documents. Current remains unchanged until import succeeds.';
    try { await actions.importBundle(form); status.textContent = 'Accepted bundle opened without solving. Export Current to keep it.'; }
    catch (error) { status.textContent = `${error.message} Nothing was imported. Existing work is retained; reload current workspace before retrying a stale request.`; }
    finally { button.disabled = current?.state?.startsWith('SOLVING_') || current?.state === 'SESSION_ENDED'; }
  });
}

export function updateDemoImport(snapshot) {
  current = snapshot;
  const button = document.querySelector('#demo-bundle-submit');
  if (!button) return;
  button.disabled = snapshot.state?.startsWith('SOLVING_') || snapshot.state === 'SESSION_ENDED';
  button.title = button.disabled ? 'Wait for or cancel the active run, or start a new session, before importing.' : '';
}
