// Rows are class row groups or one lens row group; the caller supplies each cell's items and empty-cell markup.
export function renderDayMatrix({ rows, rowHeading, periods, cellItems, emptyMarkup, periodId, labels, lessonMarkup, escapeHtml }) {
  const selectedPeriods = periodId ? periods.filter(period => period.id === periodId) : periods;
  const headers = selectedPeriods.map(period => `<th scope="col"><span>${escapeHtml(period.displayName)}</span>${period.startTime ? `<small>${escapeHtml(labels.optionalTime(period.startTime, period.endTime))}</small>` : ''}</th>`).join('');
  const body = rows.map(row => `<tr data-row-kind="${row.kind}" data-row-id="${escapeHtml(row.id)}"${row.kind === 'CLASS' ? ` data-cohort-id="${escapeHtml(row.id)}"` : ''}><th scope="row"><span>${escapeHtml(row.label)}</span><small>${escapeHtml(row.id)}</small></th>${selectedPeriods.map(period => {
    const items = cellItems(row, period);
    return items.length ? `<td>${items.map(lessonMarkup).join('')}${emptyMarkup(row, period, true)}</td>` : `<td>${emptyMarkup(row, period, false)}</td>`;
  }).join('')}</tr>`).join('');
  return `<div class="matrix-wrap" aria-label="${labels.completePopulation}"><table class="matrix"><thead><tr><th scope="col">${rowHeading}</th>${headers}</tr></thead><tbody>${body}</tbody></table></div>`;
}
