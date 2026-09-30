export function renderFocusedSchedule({ type, focusedId, source, assignments, weekdays, lessonMarkup, relatedLessonIds, labels, entityName, periodLabel, subjectColorClass, selectControl, options, escapeHtml }) {
  const title = type === 'cohortId' ? labels.classSchedule : type === 'teacherId' ? labels.teacherSchedule : labels.roomSchedule;
  const choose = type === 'cohortId' ? labels.chooseClass : type === 'teacherId' ? labels.chooseTeacher : labels.chooseRoom;
  const items = assignments.filter(item => item[type] === focusedId || relatedLessonIds?.has(item.lessonId))
    .sort((a, b) => weekdays.indexOf(a.period?.weekday) - weekdays.indexOf(b.period?.weekday) || (a.period?.order ?? 0) - (b.period?.order ?? 0));
  const dayMarkup = day => {
    const dayItems = items.filter(item => item.period?.weekday === day);
    return `<div class="focused-day"><h4>${escapeHtml(labels.days[day] || day)}</h4>${dayItems.length ? dayItems.map(item => lessonMarkup ? lessonMarkup(item) : `<article class="focused-lesson ${subjectColorClass(item)}"><time>${escapeHtml(periodLabel(item.period))}</time><div><strong>${escapeHtml(entityName(item.subject, item.subjectId))}</strong><span>${escapeHtml(entityName(item.cohort, item.cohortId))} · ${escapeHtml(entityName(item.teacher, item.teacherId))} · ${escapeHtml(entityName(item.room, item.roomId))}</span></div><span class="accepted-text">✓ ${labels.acceptedAssignment}</span></article>`).join('') : `<p class="empty-cell">${labels.emptyCell}</p>`}</div>`;
  };
  return `<p class="narrow-banner" role="status">${labels.narrowNotice}</p>
    <div class="focused-toolbar"><div class="view-tabs" role="group" aria-label="${labels.focusedSchedules}"><button type="button" data-focus-type="cohortId" aria-pressed="${type === 'cohortId'}">${labels.classes}</button><button type="button" data-focus-type="teacherId" aria-pressed="${type === 'teacherId'}">${labels.teachers}</button><button type="button" data-focus-type="roomId" aria-pressed="${type === 'roomId'}">${labels.rooms}</button></div>${selectControl('focus-entity', choose, options(source), focusedId)}</div>
    <section class="focused-schedule"><h3>${title} · ${escapeHtml(entityName(source.find(item => item.id === focusedId), focusedId))}</h3>${items.length ? weekdays.map(dayMarkup).join('') : `<p class="empty-message">${labels.noFocusedLessons}</p>`}</section>`;
}
