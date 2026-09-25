export function renderWeekMatrix({ cohorts, weekdays, assignmentsByCell, periodsForDay, labels, entityName, periodLabel, lessonMarkup, escapeHtml, escapeAttribute }) {
  const headers = weekdays.map(day => `<th scope="col">${escapeHtml(labels.days[day] || day)}</th>`).join('');
  const dayPeriods = weekdays.map(day => new Map(periodsForDay(day).map(period => [period.order, period])));
  const orders = [...new Set(dayPeriods.flatMap(periods => [...periods.keys()]))].sort((a, b) => a - b);
  const rows = cohorts.map(cohort => `<tbody data-cohort-id="${escapeAttribute(cohort.id)}">${(orders.length ? orders : [null]).map((order, index) => {
    const periods = dayPeriods.map(day => day.get(order)).filter(Boolean);
    const times = periods.map(period => period.startTime ? labels.optionalTime(period.startTime.slice(0, 5), period.endTime.slice(0, 5)) : '');
    const sharedTime = times.length && times.every(time => time === times[0]) ? times[0] : '';
    const classCell = index === 0 ? `<th scope="rowgroup" rowspan="${orders.length || 1}" class="week-class" title="${escapeAttribute(entityName(cohort))}">${escapeHtml(entityName(cohort))}</th>` : '';
    const periodCell = periods.length ? `<th scope="row" class="week-period" title="${escapeAttribute(periodLabel(periods[0]))}"><span>${escapeHtml(order)}</span>${sharedTime ? `<small>${escapeHtml(sharedTime)}</small>` : ''}</th>` : `<th scope="row" class="week-period"></th>`;
    const slots = weekdays.map((day, dayIndex) => {
      const period = dayPeriods[dayIndex].get(order);
      if (!period) return `<td data-weekday="${escapeAttribute(day)}"><span class="empty-cell">${labels.emptyCell}</span></td>`;
      const items = assignmentsByCell.get(`${cohort.id}\u0000${period.id}`) || [];
      const time = period.startTime ? labels.optionalTime(period.startTime.slice(0, 5), period.endTime.slice(0, 5)) : '';
      return `<td data-weekday="${escapeAttribute(day)}" title="${escapeAttribute(periodLabel(period))}"><div class="week-slot">${time && time !== sharedTime ? `<small class="week-time">${escapeHtml(time)}</small>` : ''}${items.length ? items.map(lessonMarkup).join('') : `<span class="empty-cell">${labels.emptyCell}</span>`}</div></td>`;
    }).join('');
    return `<tr>${classCell}${periodCell}${slots}</tr>`;
  }).join('')}</tbody>`).join('');
  return `<div class="matrix-wrap week-wrap" aria-label="${labels.weekRange}"><table class="matrix week-matrix"><thead><tr><th scope="col">${labels.class}</th><th scope="col">${labels.period}</th>${headers}</tr></thead>${rows}</table></div>`;
}
