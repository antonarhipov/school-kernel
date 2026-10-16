import { renderBrowser, referenceName } from './definition-browser.js';

export function renderOccurrenceBrowser(host, snapshot, options) {
  const draft = snapshot.workspace.definitionDraft, definition = draft.definition;
  const rows = definition.lessons.map(lesson => {
    const context = options.context[lesson.seriesId];
    const exceptions = context?.exceptions?.[lesson.id] || [];
    const className = referenceName(definition, 'cohorts', lesson.cohortId), subject = referenceName(definition, 'subjects', lesson.subjectId), teacher = referenceName(definition, 'teachers', lesson.teacherId);
    return { id: lesson.id, name: lesson.displayName || '(Unnamed)', lesson, className, subject, teacher, exceptionCount:exceptions.length,
      summary: `${className} · ${subject} · ${teacher} · ${lesson.seriesId || 'Ungrouped'} · ${exceptions.length} exceptions`,
      issues: (draft.issues || []).filter(issue => issue.ownerType === 'Lesson' && issue.ownerId === lesson.id || issue.relatedIds?.includes(lesson.id)) };
  });
  const filters = [['class','Class','cohorts','cohortId'],['subject','Subject','subjects','subjectId'],['teacher','Teacher','teachers','teacherId']].map(([key,label,collection,field]) => ({key,label,choices:definition[collection].map(row => [row.id,row.displayName || row.id]),matches:(row,value)=>row.lesson[field] === value}));
  filters.push({ key:'grouping', label:'Membership', choices:[['grouped','Grouped in a series'],['ungrouped','Ungrouped']], matches:(row,value)=>value === 'grouped' ? !!row.lesson.seriesId : !row.lesson.seriesId });
  return renderBrowser(host, { key:'occurrences', rows, singular:'Occurrence', plural:'Occurrences', selectedId:options.ids[0], narrow:options.narrow, busy:options.busy, filters,
    columns: options.narrow ? undefined : [{key:'name',label:'Occurrence'},{key:'class',label:'Class / subject',value:row=>`${row.className} · ${row.subject}`},{key:'teacher',label:'Teacher',value:row=>row.teacher},{key:'series',label:'Series / issues',value:row=>`${row.lesson.seriesId || 'Ungrouped'} · ${row.exceptionCount} exceptions · ${row.issues.length} issues`}],
    selection:{ ids:options.ids, onToggle:options.onToggle }, onSelect:options.onSelect });
}
