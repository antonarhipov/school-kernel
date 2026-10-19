import { shortGroupName } from './audience-model.js';

const subjectTitles = new Map([
  ['Inglise keel', 'Ingl. k.'], ['Flight Simulation', 'Flight sim.'],
  ['Starship Systems', 'Systems'], ['Starfighter Operations', 'Starfighter'],
  ['Fleet Tactics', 'Fleet tac.'], ['Mission Planning', 'Mission'],
]);
export const isGroupLesson = item => !!item.audienceId && !item.audienceId.startsWith('class:');
export function shortSubjectTitle(name) {
  if (subjectTitles.has(name)) return subjectTitles.get(name);
  if (name.length <= 12) return name;
  const words = name.trim().split(/\s+/u);
  return words.length === 1 ? `${name.slice(0, 10)}.` : words.map(word => `${word.slice(0, 3)}.`).join(' ');
}
export function shortRoomTitle(item) {
  const name = item.room?.displayName || item.roomId || '—';
  return name
    .replace(/^(?:Klassiruum|Cadet Classroom)\s+/iu, '')
    .replace(/^Tactical Holosuite\s+/u, 'Holosuite ')
    .replace(/^Intelligence Suite\s+/u, 'Intel. ')
    .replace(/^Flight Simulator Bay\s+/u, 'Simulator ');
}
export function groupLessonContent(item, html, context = '') {
  const subject = item.subject?.displayName || item.subjectId;
  const room = item.room?.displayName || item.roomId || '—';
  return `<strong class="split-title" title="${html(subject)}">${html(shortSubjectTitle(subject))}</strong><span class="audience-short" title="${html(item.audience.displayName)}">${html(shortGroupName(item.audience))}</span><span class="split-room" title="${html(room)}">${html(shortRoomTitle(item))}</span>${context ? `<span class="split-context" title="${html(context)}">${html(context)}</span>` : ''}`;
}
export function wholeLessonContent(item, html, context = '') {
  const subject = item.subject?.displayName || item.subjectId;
  const room = item.room?.displayName || item.roomId || '—';
  const teacher = item.teacher?.displayName || item.teacherId || '—';
  return `<strong class="whole-title" title="${html(subject)}">${html(subject)}</strong><span class="whole-room" title="${html(room)}">${html(room)}</span><span class="whole-teacher" title="${html(teacher)}">${html(teacher)}</span>${context ? `<span class="split-context" title="${html(context)}">${html(context)}</span>` : ''}`;
}
