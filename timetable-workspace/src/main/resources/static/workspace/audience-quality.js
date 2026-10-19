import { blockDescription } from './audience-model.js';

export function renderAudienceQuality(model, html, { embedded = false } = {}) {
  if (model.definition.schemaVersion !== 2) return '';
  const result = model.result, q = result.quality;
  if (!q || !Number.isInteger(result.score?.internalGapStudentPeriods)) return '<p>Audience quality is unavailable. Review this exact pair before using totals.</p>';
  const coherent = Array.isArray(q.gaps) && Array.isArray(q.teacherWorkload)
    && (q.gapDetailsTruncated || q.gaps.reduce((n,g) => n + g.studentPeriods,0) === result.score.internalGapStudentPeriods)
    && q.gaps.every(g => { const b=model.audienceClasses.get(g.classId)?.blocks.find(b=>b.id===g.blockId); return b?.size===g.studentCount && g.studentPeriods===g.studentCount*g.periodIds.length && g.periodIds.every(id=>model.maps.periods.get(id)?.weekday===g.day); })
    && q.teacherWorkload.length === model.definition.teachers.length
    && new Set(q.teacherWorkload.map(w=>w.teacherId)).size === q.teacherWorkload.length
    && q.teacherWorkload.every(w => {const t=model.maps.teachers.get(w.teacherId);return t && w.actualSessions===model.assignments.filter(a=>a.teacherId===w.teacherId).length && (w.referenceWeeklyMaximum ?? null)===(t.referenceWeeklyMaximum ?? null);});
  if (!coherent) return '<p>Audience quality is stale or cannot be reconciled. Review this exact pair again before using totals.</p>';
  return `${embedded ? '<section class="audience-quality">' : `<details class="audience-quality"><summary>Student gaps and teacher workload · ${result.score.internalGapStudentPeriods} student-periods</summary>`}
    <p>Internal student gaps: <strong>${result.score.internalGapStudentPeriods} student-periods</strong>. Counted once per anonymous student block across all subjects. Later starts and earlier finishes are excluded.</p>
    <p>Ordinary simultaneous coverage: ${q.coverageStudentPeriods} uncovered student-periods. This is separate from internal gaps and teacher gaps.</p>
    ${q.gaps.map(g => { const context = model.audienceClasses.get(g.classId), b = context?.blocks.find(b => b.id === g.blockId);
      if (!b || b.size !== g.studentCount || g.periodIds.some(id => !model.maps.periods.has(id))) return '<p>Gap detail cannot be reconciled. Recompute this exact pair.</p>';
      return `<p>${html(model.maps.cohorts.get(g.classId).displayName)} · ${html(blockDescription(context, b))} · ${g.studentCount} students · ${g.studentPeriods} student-periods ${g.periodIds.map(id => `<button class="secondary" data-quality-class="${html(g.classId)}" data-quality-period="${html(id)}">${html(g.day)} · ${html(model.maps.periods.get(id).displayName)}</button>`).join(' ')}</p>`;
    }).join('')}
    ${q.gapDetailsTruncated ? `<p>Showing ${q.gaps.length} of ${q.gapDetailCount} gap details; the aggregate includes all gaps.</p>` : ''}
    <h3>Teacher workload</h3><p>Reference differences are monitoring indicators. They do not block saving or affect optimization.</p>
    ${q.teacherWorkload.map(w => `<div class="audience-workload"><button class="secondary" data-quality-teacher="${html(w.teacherId)}">${html(model.maps.teachers.get(w.teacherId)?.displayName ?? w.teacherId)}</button>: ${w.actualSessions} actual taught periods · ${w.referenceWeeklyMaximum == null ? 'No reference set' : `Reference ${w.referenceWeeklyMaximum} · ${Math.max(0, w.actualSessions - w.referenceWeeklyMaximum)} above reference (nonblocking)`}</div>`).join('')}
    <p>Search termination: ${html(result.terminationReason)}. ${html(result.limit?.duration ?? '')} No global optimality claim.</p>${embedded ? '</section>' : '</details>'}`;
}
