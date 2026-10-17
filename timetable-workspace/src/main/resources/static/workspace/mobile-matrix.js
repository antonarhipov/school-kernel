// One shared weekday header, with the population label above each schedule instead of in a wide frozen column.
export function renderMobileMatrix({ rows, rowHeading, weekdays, cellItems, emptyMarkup, periodsForDay,
  labels, periodLabel, lessonMarkup, escapeHtml: html, escapeAttribute: attr }) {
  const days = weekdays.map(day => new Map(periodsForDay(day).map(period => [period.order, period])));
  const orders = [...new Set(days.flatMap(periods => [...periods.keys()]))].sort((a, b) => a - b);
  const headers = weekdays.map(day => `<th scope="col"><abbr title="${attr(labels.days[day] || day)}">${html((labels.days[day] || day).slice(0, 3))}</abbr></th>`).join('');
  const body = rows.map(row => `<tbody data-row-kind="${row.kind}" data-row-id="${attr(row.id)}"${row.kind === 'CLASS' ? ` data-cohort-id="${attr(row.id)}"` : ''}>
    <tr class="mobile-population-row"><th scope="rowgroup" colspan="${weekdays.length + 1}"><span class="sr-only">${html(rowHeading)} </span>${html(row.label)}</th></tr>
    ${orders.map(order => `<tr><th scope="row" class="mobile-period">${html(order)}</th>${days.map((periods, index) => {
      const period = periods.get(order);
      if (!period) return `<td data-weekday="${attr(weekdays[index])}"><span class="empty-cell">${labels.emptyCell}</span></td>`;
      const items = cellItems(row, period);
      return `<td data-weekday="${attr(weekdays[index])}" title="${attr(periodLabel(period))}"><div class="week-slot">${items.length ? items.map(lessonMarkup).join('') : emptyMarkup(row, period, false)}</div></td>`;
    }).join('')}</tr>`).join('')}</tbody>`).join('');
  return `<p class="mobile-matrix-hint">Tap a lesson for full names and times.</p><div class="matrix-wrap mobile-week-wrap" aria-label="${labels.weekRange}">
    <table class="mobile-week-matrix"><colgroup><col class="mobile-period-column">${weekdays.map(() => '<col>').join('')}</colgroup>
    <thead><tr><th scope="col"><span aria-label="Period">#</span></th>${headers}</tr></thead>${body}</table></div>`;
}
