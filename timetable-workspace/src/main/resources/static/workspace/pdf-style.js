import { rgb } from './vendor/pdf/pdf-lib.js';
import { M } from './messages.js';

export const PDF_SLOT_WIDTH = 100;
const PALETTE = [[.91,.95,.92], [.92,.94,.98], [.98,.94,.87], [.96,.91,.94], [.90,.96,.96], [.95,.94,.89]];
export const periodTime = period => [period?.startTime, period?.endTime].filter(Boolean).map(t => t.slice(0,5)).join(' - ');
export function lessonColor(model, lesson) {
  const index = model.definition.subjects.findIndex(s => s.id === lesson?.subjectId);
  return lesson ? rgb(...PALETTE[Math.max(0, index) % PALETTE.length]) : undefined;
}

// Common bells appear once. Different daily bells remain explicit in the left column.
export function slotLabels(selected, order) {
  const periods = selected.days.map((day, i) => ({ day, period: selected.periods[i].find(p => p.order === order) }));
  const times = [...new Set(periods.map(p => periodTime(p.period)).filter(Boolean))];
  return [Number.isInteger(order) ? String(order) : '', ...(times.length <= 1 ? times :
    periods.filter(p => periodTime(p.period)).map(p => `${(M.days[p.day] ?? p.day).slice(0,3)} ${periodTime(p.period)}`))];
}
