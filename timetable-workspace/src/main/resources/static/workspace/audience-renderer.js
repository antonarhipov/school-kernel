import { audienceCell, blockDescription } from './audience-model.js';

export function renderAudienceCell({ model, row, period, items, lessonMarkup, html }) {
  if (model.definition.schemaVersion !== 2 || row.kind !== 'CLASS' || period.nonTeaching) return null;
  const cell = audienceCell(model, row.id, period.id, items);
  if (!cell.all.length) return null;
  const count = cell.context.lanes?.length ?? 0;
  const summary = `${cell.all.length} lesson${cell.all.length === 1 ? '' : 's'}${cell.untaught.length ? ' · partial coverage' : ''}${cell.filtered ? ' · filtered' : ''}`;
  return `<div class="audience-cell" data-lane-count="${count}" data-force-summary="${!cell.spans || cell.filtered}">
    <button class="audience-summary" data-audience-class="${html(row.id)}" data-audience-period="${html(period.id)}" aria-label="${html(summary)}"><strong>${cell.all.length}</strong><small>${cell.all.length === 1 ? 'lesson' : 'lessons'}</small>${cell.untaught.length ? '<small>Partial</small>' : ''}${cell.filtered ? '<small>Filtered</small>' : ''}</button>
    <div class="audience-sections" hidden>${(cell.spans ?? []).map(span => `<div class="audience-span" data-span-start="${span.start + 1}" data-span-length="${span.length}">${span.item ? lessonMarkup(span.item) : `<button class="audience-untaught" data-audience-class="${html(row.id)}" data-audience-period="${html(period.id)}" aria-label="${html(span.audience.displayName)} · ${span.audience.size} students · No lesson">${html(span.audience.displayName)}<small>No lesson</small></button>`}</div>`).join('')}</div>
  </div>`;
}
export function fitAudienceCells(parent) {
  const observer = new ResizeObserver(entries => {
    for (const { target, contentRect } of entries) {
      const spans = [...target.querySelectorAll('[data-span-length]')];
      const shortest = Math.min(...spans.map(span => Number(span.dataset.spanLength)));
      const fallback = target.dataset.forceSummary === 'true' || contentRect.width * shortest / Number(target.dataset.laneCount) < 72;
      target.querySelector('.audience-summary').hidden = !fallback;
      target.querySelector('.audience-sections').hidden = fallback;
    }
  });
  parent.querySelectorAll('.audience-cell').forEach(cell => {
    cell.style.setProperty('--audience-lanes', cell.dataset.laneCount || '1');
    cell.querySelectorAll('[data-span-start]').forEach(span => { span.style.gridColumn = `${span.dataset.spanStart} / span ${span.dataset.spanLength}`; span.style.gridRow = '1'; });
    observer.observe(cell);
  });
  return observer;
}
export function audienceDetails(item, definition, html) {
  const group = item.audienceId && !item.audienceId.startsWith('class:') ? item.audience?.displayName : '';
  const day = item.period.weekday[0] + item.period.weekday.slice(1).toLowerCase();
  const label = item.period.displayName || String(item.period.order);
  const slot = label.toLowerCase().startsWith(day.toLowerCase()) ? label : `${day} · ${label}`;
  const times = [item.period.startTime, item.period.endTime].filter(Boolean).map(t => t.slice(0,5)).join('–');
  return `<p class="audience-description">${html(item.cohort.displayName)}${group ? `<br>${html(group)}` : ''}</p>
    <p>${html(item.teacher.displayName)}<br>${html(slot)}${times ? ` · ${html(times)}` : ''}</p>`;
}
export function audienceSummaryDetails(model, classId, periodId, visible, html) {
  const cell = audienceCell(model, classId, periodId, visible);
  return `<h2 id="inspector-title" tabindex="-1">${html(model.maps.cohorts.get(classId).displayName)} · ${html(model.maps.periods.get(periodId).displayName)}</h2>
    ${cell.filtered ? '<p>Filtered view. Hidden teaching is included below and is not untaught.</p>' : ''}
    ${cell.all.map(item => `<div class="audience-detail-row"><button class="secondary" data-inspect-audience-lesson="${html(item.lessonId)}">${html(item.subject.displayName)}${item.audienceId && !item.audienceId.startsWith('class:') ? ` · ${html(item.audience.displayName)}` : ''}${visible.some(v => v.lessonId === item.lessonId) ? '' : ' · hidden by filter'}</button>${audienceDetails(item, model.definition, html)}</div>`).join('')}
    ${cell.untaught.map(b => `<p class="audience-detail-row">${html(blockDescription(cell.context, b))} · ${b.size} students · No lesson</p>`).join('')}`;
}
