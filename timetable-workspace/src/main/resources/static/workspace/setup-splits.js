// Class-local audience identities never depend on a label or display position.
export const splitLimits = { groups: 128, students: 2147483647 };
export const readableSplits = c => !Object.hasOwn(c,'splits') || Array.isArray(c.splits) && c.splits.every(s =>
  s && typeof s.id==='string' && typeof s.displayName==='string' && ['number','string'].includes(typeof s.groupCount)
  && Object.keys(s).every(k=>['id','displayName','groupCount','groups','subjectIds'].includes(k))
  && Array.isArray(s.subjectIds) && s.subjectIds.every(id=>typeof id==='string') && Array.isArray(s.groups)
  && s.groups.every(g=>g && typeof g.id==='string' && typeof g.displayName==='string' && ['number','string'].includes(typeof g.size)
    && Object.keys(g).every(k=>['id','displayName','size'].includes(k))));
const integer = (v, min, max) => Number.isInteger(v) && v >= min && v <= max;
const numeric = raw => /^\d+$/.test(raw) && Number.isSafeInteger(Number(raw)) ? Number(raw) : raw;
export function balancedGroups(size, count) {
  if (!integer(size, 1, splitLimits.students) || !integer(count, 2, Math.min(size, splitLimits.groups))) return null;
  return Array.from({length: count}, (_, n) => ({id: crypto.randomUUID(), displayName: `Group ${n + 1}`, size: Math.floor(size / count) + (n < size % count ? 1 : 0)}));
}
export function splitReferences(draft, split) {
  const ids = new Set([split.id, ...(split.groups ?? []).map(g => g.id)]), found = [];
  function visit(value, path) {
    if (value === split) return;
    if (Array.isArray(value)) value.forEach((v, i) => visit(v, `${path}/${i}`));
    else if (value && typeof value === 'object') for (const [key, v] of Object.entries(value)) {
      if (['focused','pendingInput','check','advanced','source'].includes(key)) continue;
      if(ids.has(key))found.push(`${path}/${key}`);
      visit(v, `${path}/${key}`);
    }
    else if (ids.has(value)) found.push(path);
  }
  visit(draft, ''); return found;
}
export function splitIssues(draft) {
  const issues = [], ids = new Set();
  for (const c of draft.classes) for (const s of c.splits ?? []) {
    const add = (message, field) => issues.push({section: 'Classes', record: c.id, split: s.id, field,
      message: `${c.displayName ?? c.id} / ${s.displayName || 'Unnamed split'}: ${message}`});
    for (const row of [s, ...(s.groups ?? [])]) {
      if (!row.id || ids.has(row.id)) add('Split and group identifiers must be nonempty and unique.', 'id');
      ids.add(row.id);
    }
    if (!s.displayName.trim()) add('Enter a split name.', 'displayName');
    if (!integer(s.groupCount, 2, Math.min(c.size, splitLimits.groups))) add(`Group count must be from 2 to ${splitLimits.groups}, and no greater than the positive integer class size.`, 'groupCount');
    if ((s.groups ?? []).length !== s.groupCount) add('Supply balanced defaults or explicitly correct the group count; existing groups are preserved.', 'groupCount');
    for (const g of s.groups ?? []) {
      if (!g.displayName.trim()) add('Enter a label for every group.', `${g.id}/displayName`);
      if (!integer(g.size, 1, splitLimits.students)) add(`${g.displayName || 'Unnamed group'} needs a positive integer student count.`, `${g.id}/size`);
    }
    const total = (s.groups ?? []).reduce((n, g) => n + (Number.isInteger(g.size) ? g.size : 0), 0);
    if (total !== c.size) add(`Group total ${total} must equal class total ${c.size}.`, 'total');
    for (const id of s.subjectIds ?? []) if (!draft.subjects.some(subject => subject.id === id)) add(`Reused subject ${id} is missing. Correct its reference explicitly.`, 'subjectIds');
  }
  return issues;
}
export function splitsContent(draft, c, html) {
  return `<section class="setup-splits" data-split-class="${html(c.id)}" aria-label="Reusable groups"><h3>Reusable groups</h3>
    <p>Each subject chooses whether to teach the whole class or use one of these arrangements. Reuse an arrangement when the same students attend together. No individual student records are needed. Class total: <strong>${html(c.size)}</strong>. Up to ${splitLimits.groups} groups; class size up to ${splitLimits.students}.</p>
    ${(c.splits ?? []).map(s => {
      const refs = splitReferences(draft, s), locked = refs.length && draft.pendingInput.splitEditing !== s.id;
      const field = (key, label, value, group) => `<label>${label}<input data-split-field="${key}" ${group ? `data-group="${html(group)}"` : ''} value="${html(value)}" ${locked ? 'disabled' : ''} ${['size','groupCount'].includes(key) ? 'inputmode="numeric"' : ''} aria-describedby="split-issues-${html(s.id)}"></label>`;
      return `<fieldset data-split="${html(s.id)}"><legend>${html(s.displayName || 'New split')}</legend>
        ${refs.length ? `<p>Affected references: ${refs.map(html).join('; ')}</p>${locked ? '<button data-edit-split class="secondary">Review and edit referenced split…</button>' : '<p>Referenced values remain attached to their stable identities. Correct affected relationships and teaching before readiness.</p>'}` : ''}
        <div class="setup-context-fields">${field('displayName','Split name',s.displayName)}${field('groupCount','Group count',s.groupCount)}</div>
        <button data-balance-split ${locked ? 'disabled' : ''}>${s.groups.length ? 'Apply group count and balanced sizes…' : 'Supply balanced groups'}</button>
        <div class="setup-split-groups">${s.groups.map(g => `<div class="setup-context-fields">${field('displayName','Group label',g.displayName,g.id)}${field('size','Student count',g.size,g.id)}</div>`).join('')}</div>
        <p data-split-total>Group total: ${s.groups.reduce((n,g)=>n+(Number.isInteger(g.size)?g.size:0),0)} / Class total: ${html(c.size)}</p>
        <details><summary>Subjects reusing this split</summary>${draft.subjects.map(subject => `<label><input type="checkbox" data-split-subject="${html(subject.id)}" ${(s.subjectIds ?? []).includes(subject.id) ? 'checked' : ''} ${locked ? 'disabled' : ''}>${html(subject.displayName ?? subject.id)}</label>`).join('') || '<p>Add subjects in Subjects, then select them here.</p>'}</details>
        <div id="split-issues-${html(s.id)}" data-split-issues role="status">${splitIssues(draft).filter(i=>i.split===s.id).map(i=>`<p>${html(i.message)}</p>`).join('')}</div>
        <button data-remove-split class="secondary">Remove split…</button></fieldset>`;
    }).join('')}<button id="setup-add-split" class="secondary">Add split</button></section>`;
}
export function splitsPreview(draft, c, html) {
  const definition=draft.retained ?? draft;
  const plans=(definition.audiencePlans ?? []).filter(p=>p.classId===c.id);
  return `<section class="setup-splits-preview" aria-label="Reusable groups"><h3>Reusable groups</h3><p>Each subject chooses its own teaching arrangement. These groups do not split every lesson.</p>
    ${(c.splits ?? []).map(s=>`<section data-preview-split="${html(s.id)}"><h4>${html(s.displayName)}</h4>
      <table><thead><tr><th>Group</th><th>Students</th></tr></thead><tbody>${s.groups.map(g=>`<tr><td>${html(g.displayName)}</td><td>${html(g.size)}</td></tr>`).join('')}</tbody></table>
      <p>${(s.subjectIds ?? []).map(id=>html(draft.subjects.find(subject=>subject.id===id)?.displayName ?? id)).join(', ')}</p>
      ${plans.filter(p=>p.splitId===s.id).map(p=>`<p>${html(draft.subjects.find(subject=>subject.id===p.subjectId)?.displayName ?? p.subjectId)}: ${html(p.weeklyTotal)} lessons per week${p.wholeClassCount ? `, including ${html(p.wholeClassCount)} with the full class` : ' in groups'}.</p>`).join('')}</section>`).join('') || '<p>No groups defined for this class.</p>'}
    </section>`;
}
export function refreshSplitFeedback(draft, c, root, html) {
  for (const container of root.querySelectorAll('[data-split]')) {
    const s=c.splits?.find(s=>s.id===container.dataset.split);if(!s)continue;
    container.querySelector('legend').textContent=s.displayName||'New split';
    container.querySelector('[data-split-total]').textContent=`Group total: ${s.groups.reduce((n,g)=>n+(Number.isInteger(g.size)?g.size:0),0)} / Class total: ${c.size}`;
    container.querySelector('[data-split-issues]').innerHTML=splitIssues(draft).filter(i=>i.split===s.id).map(i=>`<p>${html(i.message)}</p>`).join('');
  }
}
export function createSplitActions({getDraft, commit, notify, confirm, html}) {
  function edit(event) {
    const node = event.target, container = node.closest('[data-split]');
    if (!container || (!node.dataset.splitField && !node.dataset.splitSubject)) return false;
    const draft = getDraft(), c = draft.classes.find(c=>c.id===node.closest('[data-split-class]').dataset.splitClass);
    const s = c.splits.find(s=>s.id===container.dataset.split);
    if (splitReferences(draft,s).length && draft.pendingInput.splitEditing !== s.id) { notify('Review affected references before editing this split.'); return true; }
    if (node.dataset.splitSubject) {
      s.subjectIds = (s.subjectIds ?? []).filter(id=>id!==node.dataset.splitSubject);
      if (node.checked) s.subjectIds.push(node.dataset.splitSubject);
    } else {
      const row = node.dataset.group ? s.groups.find(g=>g.id===node.dataset.group) : s;
      row[node.dataset.splitField] = ['size','groupCount'].includes(node.dataset.splitField) ? numeric(node.value) : node.value;
    }
    commit(draft,false);
    refreshSplitFeedback(draft,c,container.parentElement,html);
    return true;
  }
  return {input: edit, change: event => event.target.type === 'checkbox' ? edit(event) : false,
    click(event) {
      const button = event.target.closest('button'), parent = button?.closest('[data-split-class]');
      if (!parent || !(button.id==='setup-add-split' || button.closest('[data-split]'))) return false;
      const draft = getDraft(), c = draft.classes.find(c=>c.id===parent.dataset.splitClass);
      if (button.id==='setup-add-split') {
        const subjectId = draft.section==='Subjects' && draft.pendingInput.subjectArrangement && draft.pendingInput.subjectTeachingClassId===c.id ? draft.pendingInput.subjectTeachingSubjectId : null;
        (c.splits ??= []).push({id:crypto.randomUUID(),displayName:'',groupCount:2,groups:balancedGroups(c.size,2) ?? [],subjectIds:subjectId ? [subjectId] : []});
        commit(draft); document.querySelector('[data-split]:last-of-type [data-split-field=displayName]')?.focus(); return true;
      }
      const s = c.splits.find(s=>s.id===button.closest('[data-split]').dataset.split), refs = splitReferences(draft,s);
      if (button.hasAttribute('data-edit-split')) {
        confirm('Edit referenced split?', `Changes affect ${refs.join('; ')}. Stable references will be preserved, including incomplete work. Correct all affected relationships, teaching and rules explicitly before readiness.`, ()=>{draft.pendingInput.splitEditing=s.id;commit(draft);}, 'Edit referenced split'); return true;
      }
      if (button.hasAttribute('data-balance-split')) {
        const groups=balancedGroups(c.size,s.groupCount);
        if (!groups) {notify(`Cannot balance groups: enter a positive integer class size and 2–${splitLimits.groups} groups, no greater than the class size. Unfinished values are retained.`);return true;}
        if (refs.length) {notify(`Correct affected references before resizing groups: ${refs.join('; ')}.`);return true;}
        if (!s.groups.length) {s.groups=groups;commit(draft);return true;}
        confirm('Apply balanced sizes?', `Use sizes ${groups.map(g=>g.size).join(', ')} for class total ${c.size}. Existing entered counts will be replaced; removed groups will be deleted. Surviving group labels and identities remain.`, ()=>{
          s.groups=groups.map((g,i)=>({...g,...(s.groups[i]?{id:s.groups[i].id,displayName:s.groups[i].displayName}:{} )}));commit(draft);
        }, 'Apply balanced sizes');return true;
      }
      if (button.hasAttribute('data-remove-split')) {
        if (refs.length) {notify(`Cannot remove this split. Explicitly correct or remove these references first: ${refs.join('; ')}.`);return true;}
        confirm('Remove split?', `Remove ${s.displayName || 'unnamed split'} and its ${s.groups.length} groups? Other splits, schools and timetables are preserved.`, ()=>{c.splits=c.splits.filter(v=>v.id!==s.id);commit(draft);}, 'Remove split');return true;
      }
      return false;
    }
  };
}
