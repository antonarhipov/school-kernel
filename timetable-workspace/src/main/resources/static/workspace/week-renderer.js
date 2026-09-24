export function renderWeekMatrix({ cohorts, weekdays, assignmentsByCell, periodsForDay, labels, entityName, periodLabel, lessonMarkup, escapeHtml, escapeAttribute }) {
  const headers = weekdays.map(day => `<th scope="col">${escapeHtml(labels.days[day] || day)}</th>`).join('');
  const rows = cohorts.map(cohort => `<tr><th scope="row"><span>${escapeHtml(entityName(cohort))}</span><small>${escapeHtml(cohort.id)}</small></th>${weekdays.map(day => {
    const slots = periodsForDay(day).map(period => {
      const items = assignmentsByCell.get(`${cohort.id}\u0000${period.id}`) || [];
      return `<div class="week-slot"><span class="week-period" title="${escapeAttribute(periodLabel(period))}">${escapeHtml(`${period.order} · ${period.displayName}`)}</span>${items.length ? items.map(lessonMarkup).join('') : `<span class="empty-cell">${labels.emptyCell}</span>`}</div>`;
    }).join('');
    return `<td data-weekday="${escapeAttribute(day)}">${slots}</td>`;
  }).join('')}</tr>`).join('');
  return `<div class="matrix-wrap week-wrap" aria-label="${labels.weekRange}"><table class="matrix week-matrix"><thead><tr><th scope="col">${labels.class}</th>${headers}</tr></thead><tbody>${rows}</tbody></table></div>`;
}
