const el = (tag, text) => { const node = document.createElement(tag); if (text !== undefined) node.textContent = text; return node; };
let activeDialog = null, sending = false;
export const discardBusy = () => activeDialog !== null;

export function renderDiscard(host, snapshot, narrow, actions, busy) {
  if (activeDialog) activeDialog.querySelector('#confirm-discard-authoring').disabled = narrow || sending;
  const button = el('button', 'Discard authoring'); button.id = 'discard-authoring'; button.className = 'danger';
  button.disabled = narrow || busy || discardBusy(); host.append(button);
  button.addEventListener('click', () => {
    if (innerWidth <= 700 || busy || discardBusy()) return;
    const draft = snapshot.workspace.definitionDraft;
    const legacySource = !Object.hasOwn(draft, 'sourceDocument') && typeof draft.sourceDefinitionRevision === 'string';
    const source = draft.sourceDocument ?? (legacySource ? snapshot.workspace : null);
    const name = source?.school?.displayName || source?.initialDefinition?.displayName || source?.acceptedBaseline?.definition?.displayName || (source ? '(no school)' : '(source unavailable)');
    const changes = draft.sourceDocument ? `${snapshot.definitionReview?.counts?.changes ?? 0} saved changes` : 'All saved authoring changes';
    const dialog = el('dialog'); dialog.id = 'discard-authoring-dialog';
    dialog.append(el('h3', 'Discard all authoring changes?'), el('p', `Restore recorded source: ${draft.sourceState || '(unavailable)'} • ${name}.`),
      el('p', `${changes} will be abandoned, together with all unsaved working values. The recorded source content and revisions will be restored exactly.`));
    if (legacySource) dialog.append(el('p', 'This older draft retains its source in the workspace. Discard will restore it only if the recorded source revision still matches. Otherwise nothing will be discarded.'));
    else if (!source || typeof source !== 'object' || Array.isArray(source)) dialog.append(el('p', 'The recorded authoring source is unavailable. Discard cannot restore it; saved work will remain unchanged.'));
    if (draft.sourceState === 'EMPTY') dialog.append(el('p', 'This returns to EMPTY. The newly created school definition will be removed; no school or Current timetable remains.'));
    const feedback = el('p'); feedback.setAttribute('role', 'alert');
    const confirm = el('button', 'Confirm discard authoring'); confirm.id = 'confirm-discard-authoring'; confirm.className = 'danger';
    const cancel = el('button', 'Keep authoring'); cancel.id = 'cancel-discard-authoring';
    // Cancellation never rerenders or clears working editor values.
    const close = () => { dialog.close(); dialog.remove(); activeDialog = null; const current = document.getElementById('discard-authoring'); if (current) current.disabled = innerWidth <= 700 || busy; };
    cancel.addEventListener('click', close);
    dialog.addEventListener('cancel', event => { event.preventDefault(); if (!sending) close(); });
    confirm.addEventListener('click', async () => {
      if (sending || innerWidth <= 700) return;
      sending = true; confirm.disabled = true; cancel.disabled = true; feedback.textContent = 'Restoring the recorded source…';
      try { const result = await actions.discard(snapshot.version); sending = false; close(); actions.render(result); }
      catch (error) { sending = false; feedback.textContent = `Authoring was not discarded. ${error.message}`; confirm.disabled = innerWidth <= 700; cancel.disabled = false; }
    });
    dialog.append(confirm, cancel, feedback); document.body.append(dialog); activeDialog = dialog; dialog.showModal();
  });
}
