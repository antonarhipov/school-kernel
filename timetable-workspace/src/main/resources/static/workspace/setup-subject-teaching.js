import { splitsContent } from './setup-splits.js';
const label = row => row?.displayName ?? row?.name ?? row?.id ?? 'Missing';
const named = (rows, id) => label(rows?.find(row => row.id === id) ?? {id});

// Resolve the teaching plan, never infer subject delivery from available class groups.
export function subjectTeachingContent(draft, subject, html) {
  const data = draft.retained ?? draft, classes = draft.classes;
  const audience = (data.audiencePlans ?? []).filter(p => p.subjectId === subject.id);
  const fixed = draft.retained ? [] : (draft.plans ?? []).filter(p => p.subjectId === subject.id);
  const lessons = draft.retained ? (data.lessons ?? []).filter(p => p.subjectId === subject.id && !p.audienceId) : [];
  const classIds = new Set([...audience.map(p => p.classId), ...fixed.map(p => p.classId ?? p.cohortId), ...lessons.map(p => p.cohortId)]);
  const count = value => Number.isInteger(value) ? html(value) : 'unfinished';
  const remainder = (total, whole) => Number.isInteger(total) && Number.isInteger(whole) ? html(total - whole) : 'unfinished';
  const teacher = id => html(named(draft.teachers, id));
  const room = id => id ? html(named(draft.rooms, id)) : 'Room chosen when scheduling';
  const policy = p => {
    const t = p.teacherPolicy;
    if (!t) return 'Teacher policy unfinished';
    if (t.mode === 'fixed') return `Fixed teacher: ${teacher(t.fixedTeacherId)}`;
    if (t?.mode === 'sharing') return `Shared teaching: ${(t.shares ?? []).map(s => `${teacher(s.teacherId)} (${html(s.count)} lessons)`).join('; ')}`;
    return `Automatic weekly teacher selection${t?.candidateTeacherIds?.length ? `: ${t.candidateTeacherIds.map(teacher).join(', ')}` : ''}`;
  };
  const rows = classes.filter(c => classIds.has(c.id)).map(c => {
    const plans = audience.filter(p => p.classId === c.id), ordinary = fixed.filter(p => (p.classId ?? p.cohortId) === c.id), old = lessons.filter(p => p.cohortId === c.id);
    const ordinaryTotal = ordinary.some(p => !Number.isInteger(p.requiredCount)) ? 'Unfinished weekly demand' : ordinary.reduce((n,p) => n + p.requiredCount, 0) + old.length;
    return `<section class="setup-teaching-card" data-subject-class="${html(c.id)}"><h4>${html(label(c))}</h4>${plans.map(p => {
      const split = c.splits?.find(s => s.id === p.splitId), whole = p.delivery === 'whole' ? p.weeklyTotal : p.delivery === 'mixed' ? p.wholeClassCount : 0;
      const portions = p.portions ?? [], ids = new Set(portions.map(q => q.id));
      const parallel = (data.coordinationRules ?? []).filter(r => r.type === 'START_TOGETHER' && r.activities?.some(a => ids.has(a.portionId)));
      return `<p><strong>${({whole:'Whole class',mixed:'Mixed whole class and groups',grouped:'Grouped'})[p.delivery] ?? 'Delivery unfinished'}</strong> · ${count(p.weeklyTotal)} lessons per week${p.delivery === 'mixed' ? `, including ${count(whole)} whole-class sessions` : ''}</p>
        ${p.delivery !== 'whole' ? `<p>Arrangement: ${html(split ? label(split) : 'Missing arrangement')}</p><ul>${(split?.groups ?? []).map(g => `<li>${html(label(g))} · ${count(g.size)} students · ${count(p.overrides?.[g.id] ?? p.weeklyTotal)} lessons per week${p.delivery === 'mixed' ? ` (${count(whole)} whole class + ${remainder(p.overrides?.[g.id] ?? p.weeklyTotal, whole)} group)` : ''}</li>`).join('')}</ul>` : `<p>${html(c.size)} students attend together.</p>`}
        <ul>${portions.map(q => `<li>${html(q.audienceId === `class:${c.id}` ? 'Whole class' : named(split?.groups, q.audienceId))} · ${html(q.sessionIds.length)} sessions · ${policy(q)} · ${room(q.fixedRoomId)}</li>`).join('')}</ul>
        ${parallel.map(r => `<p>Parallel teaching: ${html(r.count)} starts together per week${r.activities.some(a => !ids.has(a.portionId)) ? ' · includes other teaching requirements' : ''}.</p>`).join('') || (p.delivery !== 'whole' ? '<p>No required parallel starts; placement follows availability and other scheduling rules.</p>' : '')}`;
    }).join('')}${ordinary.length || old.length ? `<p><strong>Whole class</strong> · ${html(ordinaryTotal)} lessons per week · ${html(c.size)} students attend together.</p><p>${[...new Set([...ordinary.flatMap(p => (p.allocations ?? []).map(a => a.teacherId)), ...old.map(p => p.teacherId)])].map(teacher).join(', ')}</p>` : ''}
      ${!draft.retained ? `<button data-subject-teaching="${html(c.id)}" data-teaching-subject="${html(subject.id)}">Teaching arrangement · ${html(label(c))}</button>` : ''}</section>`;
  }).join('');
  const available = classes.filter(c => !classIds.has(c.id));
  return `<section class="setup-subject-teaching" aria-label="Teaching arrangements by class"><h3>Teaching arrangements by class</h3><p>Each class and subject chooses whole-class, grouped or mixed teaching. Reusable groups do not split every lesson.</p>${rows || '<p>No teaching requirements for this subject yet.</p>'}
    ${!draft.retained && available.length ? `<label>Add teaching for class<select id="setup-subject-class">${available.map(c => `<option value="${html(c.id)}">${html(label(c))}</option>`).join('')}</select></label><button data-add-subject-teaching="${html(subject.id)}">Add class teaching requirement</button>` : ''}
    ${draft.retained ? '<p>Use “Edit this copy…” to change a teaching arrangement in your own school copy.</p>' : ''}</section>`;
}

export function subjectArrangementContent(draft, c, subject, html) {
  return `<div data-subject-arrangement><button data-back-subject-arrangement class="secondary">← Teaching arrangement</button><h3>${html(label(subject))} · ${html(label(c))} · Groups</h3><p>Create an arrangement here or reuse an existing one when you return to teaching. Changes to a reused arrangement affect every teaching plan that references it.</p>${splitsContent(draft,c,html)}</div>`;
}
