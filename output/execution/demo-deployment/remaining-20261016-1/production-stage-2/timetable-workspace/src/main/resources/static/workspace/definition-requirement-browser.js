import { renderBrowser, referenceName } from './definition-browser.js';

export function renderRequirementBrowser(host, snapshot, model, options) {
  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  const name = (collection, id) => referenceName(definition, collection, id);
  const rows = model.requirements.map(item => {
    const className = name('cohorts', item.cohortId), subject = name('subjects', item.subjectId);
    const teacher = item.mixedFields?.includes('teacherId') ? 'Mixed' : name('teachers', item.defaults?.teacherId);
    const count = Number.isInteger(item.count) && item.count >= 0 ? String(item.count) : `Incomplete: ${JSON.stringify(item.count)}`;
    const exceptions = Object.values(item.exceptions || {}).filter(fields => fields?.length).length;
    const teacherIds = [...new Set([item.defaults?.teacherId, ...item.occurrences.map(row => row.teacherId)].filter(Boolean))];
    const lessonIds = new Set(item.occurrences.map(row => row.id));
    const issues = (draft.issues || []).filter(issue => (['Requirement', 'Series'].includes(issue.ownerType) && issue.ownerId === item.seriesId) || (issue.ownerType === 'Lesson' && lessonIds.has(issue.ownerId)) || issue.relatedIds?.includes(item.seriesId));
    return { id: item.seriesId, name: className, title: `${className} · ${subject}`, subject, teacher, count, exceptions, issues, cohortId: item.cohortId, subjectId: item.subjectId, teacherIds,
      summary: `${subject} · ${teacher} · ${count} lessons/week · ${exceptions} exceptions · ${teacherIds.map(id => name('teachers', id)).join(', ')}` };
  });
  const filters = [['class', 'Class', 'cohorts', (row, value) => row.cohortId === value], ['subject', 'Subject', 'subjects', (row, value) => row.subjectId === value], ['teacher', 'Teacher', 'teachers', (row, value) => row.teacherIds.includes(value)]].map(([key, label, collection, matches]) => ({ key, label, choices: definition[collection].map(row => [row.id, row.displayName || row.id]), matches }));
  const columns = [{ key: 'name', label: 'Class' }, { key: 'subject', label: 'Subject', value: row => row.subject }, { key: 'teacher', label: 'Teacher', value: row => row.teacher }, { key: 'count', label: 'Lessons/week', value: row => row.count }, { key: 'exceptions', label: 'Exceptions', value: row => String(row.exceptions) }, { key: 'issues', label: 'Issues', value: row => row.issues.length ? `${row.issues.filter(issue => issue.severity === 'BLOCKING').length} blocking · ${row.issues.filter(issue => issue.severity !== 'BLOCKING').length} advisories` : 'None' }];
  return renderBrowser(host, { ...options, key: 'requirements', rows, singular: 'Requirement', plural: 'Weekly requirements', addLabel: 'Add weekly requirement', filters, columns: options.narrow ? undefined : columns });
}
