export function renderDayMatrix({ cohorts, periods, assignmentsByCell, periodId, labels, entityName, periodLabel, lessonMarkup, escapeHtml }) {
  const selectedPeriods = periodId ? periods.filter(period => period.id === periodId) : periods;
  const headers = selectedPeriods.map(period => `<th scope="col"><span>${escapeHtml(period.displayName)}</span>${period.startTime ? `<small>${escapeHtml(labels.optionalTime(period.startTime, period.endTime))}</small>` : ''}</th>`).join('');
  const rows = cohorts.map(cohort => `<tr data-cohort-id="${escapeHtml(cohort.id)}"><th scope="row"><span>${escapeHtml(entityName(cohort))}</span><small>${escapeHtml(cohort.id)}</small></th>${selectedPeriods.map(period => {
    const items = assignmentsByCell.get(`${cohort.id}\u0000${period.id}`) || [];
    return items.length ? `<td>${items.map(lessonMarkup).join('')}<span class="empty-cell" hidden>${labels.emptyCell}</span></td>` : `<td><span class="empty-cell">${labels.emptyCell}</span></td>`;
  }).join('')}</tr>`).join('');
  return `<div class="matrix-wrap" aria-label="${labels.completePopulation}"><table class="matrix"><thead><tr><th scope="col">${labels.class}</th>${headers}</tr></thead><tbody>${rows}</tbody></table></div>`;
}
