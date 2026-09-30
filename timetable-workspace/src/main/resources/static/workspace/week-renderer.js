// Rows are class row groups or one lens row group; the caller supplies each cell's items and empty-cell markup.
export function renderWeekMatrix({ rows, rowHeading, weekdays, cellItems, emptyMarkup, periodsForDay, labels, periodLabel, lessonMarkup, escapeHtml, escapeAttribute }) {
  const headers = weekdays.map(day => `<th scope="col">${escapeHtml(labels.days[day] || day)}</th>`).join('');
  const dayPeriods = weekdays.map(day => new Map(periodsForDay(day).map(period => [period.order, period])));
  const orders = [...new Set(dayPeriods.flatMap(periods => [...periods.keys()]))].sort((a, b) => a - b);
  const body = rows.map(row => `<tbody data-row-kind="${row.kind}" data-row-id="${escapeAttribute(row.id)}"${row.kind === 'CLASS' ? ` data-cohort-id="${escapeAttribute(row.id)}"` : ''}>${(orders.length ? orders : [null]).map((order, index) => {
    const periods = dayPeriods.map(day => day.get(order)).filter(Boolean);
    const times = periods.map(period => period.startTime ? labels.optionalTime(period.startTime.slice(0, 5), period.endTime.slice(0, 5)) : '');
    const sharedTime = times.length && times.every(time => time === times[0]) ? times[0] : '';
    const classCell = index === 0 ? `<th scope="rowgroup" rowspan="${orders.length || 1}" class="week-class" title="${escapeAttribute(row.label)}">${escapeHtml(row.label)}</th>` : '';
    const periodCell = periods.length ? `<th scope="row" class="week-period" title="${escapeAttribute(periodLabel(periods[0]))}"><span>${escapeHtml(order)}</span>${sharedTime ? `<small>${escapeHtml(sharedTime)}</small>` : ''}</th>` : `<th scope="row" class="week-period"></th>`;
    const slots = weekdays.map((day, dayIndex) => {
      const period = dayPeriods[dayIndex].get(order);
      if (!period) return `<td data-weekday="${escapeAttribute(day)}"><span class="empty-cell">${labels.emptyCell}</span></td>`;
      const items = cellItems(row, period);
      const time = period.startTime ? labels.optionalTime(period.startTime.slice(0, 5), period.endTime.slice(0, 5)) : '';
      const content = items.length ? `${items.map(lessonMarkup).join('')}${row.kind === 'CLASS' ? '' : emptyMarkup(row, period, true)}` : emptyMarkup(row, period, false);
      return `<td data-weekday="${escapeAttribute(day)}" title="${escapeAttribute(periodLabel(period))}"><div class="week-slot">${time && time !== sharedTime ? `<small class="week-time">${escapeHtml(time)}</small>` : ''}${content}</div></td>`;
    }).join('');
    return `<tr>${classCell}${periodCell}${slots}</tr>`;
  }).join('')}</tbody>`).join('');
  return `<div class="matrix-wrap week-wrap" aria-label="${labels.weekRange}"><table class="matrix week-matrix"><thead><tr><th scope="col">${rowHeading}</th><th scope="col">${labels.period}</th>${headers}</tr></thead>${body}</table></div>`;
}
