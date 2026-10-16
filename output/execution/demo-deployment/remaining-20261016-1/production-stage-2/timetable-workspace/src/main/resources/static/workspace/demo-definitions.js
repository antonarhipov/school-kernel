export const prefix = 'school-kernel.definition.v1.';
export const useKey = 'school-kernel.definition-use.v1';
export const maxBytes = 2 * 1024 * 1024;
export const bytes = text => new TextEncoder().encode(text).length;
export const validIdentifier = id => typeof id === 'string' && /^[a-z0-9][a-z0-9-]{0,39}$/.test(id)
  && !['small', 'two-class', 'full'].includes(id);
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);
const fields = (value, names) => object(value) && Object.keys(value).sort().join() === [...names].sort().join();

// JSON.parse remains the syntax authority. The scanner only locates the first error consistently across engines.
export function syntaxFeedback(text) {
  try { JSON.parse(text); return ''; } catch { /* locate below */ }
  let i = 0, error = null;
  const stack = ['root'];
  const fail = () => { error = i; };
  const string = () => {
    i++;
    while (i < text.length) {
      const c = text[i++];
      if (c === '"') return;
      if (c.charCodeAt(0) < 32) { i--; return fail(); }
      if (c === '\\') {
        if (i === text.length) return fail();
        const escape = text[i++];
        if (escape === 'u') {
          for (let n = 0; n < 4; n++, i++) if (!/[0-9a-f]/i.test(text[i] || '')) return fail();
        } else if (!'"\\/bfnrt'.includes(escape)) { i--; return fail(); }
      }
    }
    fail();
  };
  while (stack.length && error === null) {
    while (/[\x20\t\r\n]/.test(text[i] || '\0')) i++;
    const state = stack.pop(), c = text[i];
    if (state === 'end') { if (i !== text.length) fail(); }
    else if (state === 'colon') { if (c !== ':') fail(); else i++; }
    else if (state === 'key' || state === 'keyOrEnd') {
      if (c === '}' && state === 'keyOrEnd') i++;
      else if (c !== '"') fail();
      else { string(); stack.push('objectNext', 'value', 'colon'); }
    } else if (state === 'objectNext' || state === 'arrayNext') {
      const end = state === 'objectNext' ? '}' : ']';
      if (c === end) i++;
      else if (c !== ',') fail();
      else { i++; stack.push(state === 'objectNext' ? 'key' : 'arrayValue'); }
    } else {
      if (state === 'root') stack.push('end');
      if (c === ']' && state === 'arrayFirst') { i++; continue; }
      if (state === 'arrayFirst' || state === 'arrayValue') stack.push('arrayNext');
      if (c === '{') { i++; stack.push('keyOrEnd'); }
      else if (c === '[') { i++; stack.push('arrayFirst'); }
      else if (c === '"') string();
      else if (c === '-' || /[0-9]/.test(c || '')) {
        if (text[i] === '-') i++;
        if (text[i] === '0') i++;
        else if (/[1-9]/.test(text[i] || '')) { while (/[0-9]/.test(text[i] || '')) i++; }
        else fail();
        if (error === null && text[i] === '.') {
          i++; if (!/[0-9]/.test(text[i] || '')) fail();
          while (/[0-9]/.test(text[i] || '')) i++;
        }
        if (error === null && /[eE]/.test(text[i] || '')) {
          i++; if (/[+-]/.test(text[i] || '')) i++;
          if (!/[0-9]/.test(text[i] || '')) fail();
          while (/[0-9]/.test(text[i] || '')) i++;
        }
      } else {
        const literal = ['true', 'false', 'null'].find(word => word[0] === c);
        if (!literal) fail();
        else for (const letter of literal) { if (text[i] !== letter) { fail(); break; } i++; }
      }
    }
  }
  const position = error ?? i;
  const before = text.slice(0, position).split('\n');
  return `JSON syntax error at line ${before.length}, column ${before.at(-1).length + 1}.`;
}

export function validateText(text) {
  if (bytes(text) > maxBytes) throw new Error('Use at most 2 MiB UTF-8.');
  const syntax = syntaxFeedback(text);
  if (syntax) throw new Error(syntax);
  if (!object(JSON.parse(text))) throw new Error('Save requires a JSON object.');
}

export function readEnvelope(storage, id) {
  const value = JSON.parse(storage.getItem(prefix + id));
  if (!validIdentifier(id) || !fields(value, ['formatVersion', 'identifier', 'savedAt', 'derivedFrom', 'documentText'])
      || value.formatVersion !== 1 || value.identifier !== id || typeof value.documentText !== 'string'
      || typeof value.savedAt !== 'string' || !/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z$/.test(value.savedAt)
      || !Number.isFinite(Date.parse(value.savedAt)) || new Date(value.savedAt).toISOString() !== value.savedAt)
    throw new Error('Unreadable saved version.');
  const source = value.derivedFrom;
  if (!object(source) || (source.kind === 'file' ? !fields(source, ['kind'])
      : !fields(source, ['kind', 'identifier']) || (source.kind === 'sample'
        ? !['small', 'two-class', 'full'].includes(source.identifier)
        : source.kind !== 'saved' || !validIdentifier(source.identifier)))) throw new Error('Unreadable saved origin.');
  validateText(value.documentText);
  return value;
}

export function savedRows(storage) {
  return Object.keys(storage).filter(key => key.startsWith(prefix)).map(key => {
    const id = key.slice(prefix.length);
    try { return { id, value: readEnvelope(storage, id) }; } catch { return { id }; }
  }).sort((a, b) => (b.value?.savedAt || '').localeCompare(a.value?.savedAt || '') || a.id.localeCompare(b.id));
}

// Browser Storage writes/removals are synchronous. Restore prior entries if a multi-key removal fails.
export function deleteVersions(storage, ids) {
  const previous = ids.map(id => [prefix + id, storage.getItem(prefix + id)]);
  try { for (const [key] of previous) storage.removeItem(key); }
  catch (error) {
    for (const [key, value] of previous) if (value !== null && storage.getItem(key) !== value) storage.setItem(key, value);
    throw error;
  }
}

const canonical = value => Array.isArray(value) ? value.map(canonical) : object(value)
  ? Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])])) : value;
const digest = async text => Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))))
  .map(byte => byte.toString(16).padStart(2, '0')).join('');
const definition = snapshot => snapshot?.workspace?.acceptedBaseline?.definition || snapshot?.workspace?.initialDefinition;

// Called only after successful use, with the returned real snapshot. UC-5 consumes the saved-version branch.
export async function rememberDefinitionUse(storage, identifier, snapshot) {
  const sample = ['small', 'two-class', 'full'].includes(identifier), loaded = definition(snapshot);
  const saved = sample ? null : readEnvelope(storage, identifier);
  if (!loaded || (sample ? snapshot.workspace.importMode !== 'PACKAGED_SAMPLE' || snapshot.workspace.origin !== identifier
      : JSON.stringify(canonical(JSON.parse(saved.documentText))) !== JSON.stringify(canonical(loaded))))
    throw new Error('The loaded definition does not match this version.');
  const mark = { kind: sample ? 'sample' : 'saved', identifier,
    envelope: sample ? null : await digest(JSON.stringify(saved)), definition: await digest(JSON.stringify(canonical(loaded))) };
  storage.setItem(useKey, JSON.stringify(mark));
}

export async function savedVersionInUse(storage, snapshot) {
  const mark = JSON.parse(storage.getItem(useKey));
  // Accepted file imports never inherit a browser label, even when their definition is identical.
  if (!mark || !['PACKAGED_SAMPLE', 'INITIAL_DEFINITION', 'INITIAL_ACCEPTANCE', 'MANUAL_EDIT_PUBLICATION', 'REPAIR_ACCEPTANCE'].includes(snapshot?.workspace?.importMode)
      || !definition(snapshot) || mark.definition !== await digest(JSON.stringify(canonical(definition(snapshot))))) return null;
  if (mark.kind === 'sample') return ['small', 'two-class', 'full'].includes(mark.identifier) ? mark.identifier : null;
  if (mark.kind !== 'saved') return null;
  const saved = readEnvelope(storage, mark.identifier);
  return mark.envelope === await digest(JSON.stringify(saved)) ? mark.identifier : null;
}
