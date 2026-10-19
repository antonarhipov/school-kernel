import { sessionDefinitions } from './audience-model.js';
import { prefix as definitionPrefix, useKey as definitionUseKey } from './demo-definitions.js';

export const cleanupEpochKey = 'school-kernel.timetable-demo.epoch.v1';
export const storageKey = 'school-kernel.timetable-demo.v1';
const empty = () => ({ formatVersion: 1, schools: {}, timetables: {}, drafts: {}, selected: null, lastTimetable: {}, editor: null });
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);
export const draftKey = (school, timetable) => `${school}/${timetable}`;

// Delete only this demo's artifacts, including definitions saved by older demo versions.
// Snapshot raw values so unreadable data is deletable and a refused removal can be rolled back.
export function deleteLocalTimetableData(storage) {
  let previous = [];
  try {
    const keys = [storageKey, definitionUseKey];
    for (let i = 0; i < storage.length; i++) {
      const key = storage.key(i);
      if (key?.startsWith(definitionPrefix)) keys.push(key);
    }
    previous = [...new Set([...keys,cleanupEpochKey])].map(key => [key, storage.getItem(key)]);
    storage.setItem(cleanupEpochKey,crypto.randomUUID());
    for (const [key,value] of previous) if(key!==cleanupEpochKey&&value!==null)storage.removeItem(key);
  } catch {
    let restored = true;
    for (const [key, value] of previous) {
      try { if (storage.getItem(key) !== value) { if(value===null)storage.removeItem(key);else storage.setItem(key,value); } }
      catch { restored = false; }
    }
    throw new Error(restored
      ? 'Local data could not be deleted. Existing saved data has been retained. Check this browser’s storage permissions and try again.'
      : 'Deletion could not be completed. Some local data may have been deleted. Reload to check what remains and try again.');
  }
}

// A single atomic browser write keeps a new version and removal of its source draft inseparable.
export function createTimetableStorage(storage) {
  let state = empty(), previous = null, writable = true,epoch;
  const notices = [];
  try {
    epoch=storage.getItem(cleanupEpochKey);
    previous = storage.getItem(storageKey);
    if (previous !== null) {
      const value = JSON.parse(previous);
      if (value.formatVersion !== 1 || !['schools', 'timetables', 'drafts', 'lastTimetable'].every(key => object(value[key])))
        throw new Error('Unsupported browser data');
      state = value;
    }
  } catch {
    writable = false;
    notices.push('Saved browser data could not be opened. It has been retained. You can inspect the examples, but changes cannot be saved until browser storage is available.');
  }
  return {
    state: () => structuredClone(state),
    notices,
    reload() {
      const latestEpoch=storage.getItem(cleanupEpochKey);
      const raw = storage.getItem(storageKey);
      const fresh = createTimetableStorage({ getItem: key => key===storageKey?raw:latestEpoch });
      if (fresh.notices.length) throw new Error(fresh.notices.join(' '));
      if (storage.getItem(storageKey) !== raw||storage.getItem(cleanupEpochKey)!==latestEpoch) throw new Error('Another tab changed browser data during recovery. Retry recovery.');
      state = fresh.state(); previous = raw; epoch=latestEpoch;writable = true;
    },
    write(change, { atomic = false } = {}) {
      const next = structuredClone(state);
      change(next);
      // Keep the working copy on failure so navigation can retain edits even when durable storage is full.
      if (!atomic) state = next;
      if (!writable) throw new Error('Browser storage is unavailable. This change is only in memory and will be lost when this page closes.');
      try {
        if (storage.getItem(storageKey) !== previous||storage.getItem(cleanupEpochKey)!==epoch)
          throw new Error('Another tab changed this browser’s timetables. Reload before saving; your current edits are still on this page.');
        const serialized = JSON.stringify(next);
        storage.setItem(storageKey, serialized);
        previous = serialized;
        state = next;
      } catch (error) {
        if (error.message.startsWith('Another tab')) throw error;
        throw new Error('Browser storage is full or unavailable. Nothing was saved. Existing saved versions are intact; keep this page open and try again after freeing browser storage.');
      }
    }
  };
}

export function isSchoolDefinition(d) {
  if (!object(d) || ![1,2].includes(d.schemaVersion) || d.schemaVersion === 2 && (d.catalogVersion !== 11 || !Array.isArray(d.audiencePlans)) || typeof d.schoolId !== 'string'
      || !['subjects', 'teachers', 'cohorts', 'rooms', 'periods', 'lessons'].every(key => Array.isArray(d[key])
        && d[key].every(item => object(item) && typeof item.id === 'string')
        && new Set(d[key].map(item => item.id)).size === d[key].length)) return false;
  return d.periods.every(p => typeof p.weekday === 'string' && Number.isInteger(p.order));
}
export function isTimetable(value) {
  const d = value?.definition, a = value?.result?.timetable?.assignments;
  if (typeof value?.id !== 'string' || typeof value?.name !== 'string' || !isSchoolDefinition(d) || !Array.isArray(a)) return false;
  const ids = Object.fromEntries(['subjects', 'teachers', 'cohorts', 'rooms', 'periods', 'lessons']
    .map(key => [key, new Set(d[key].map(item => item.id))]));
  const sessions = sessionDefinitions(d);
  ids.lessons = new Set(sessions.keys());
  const seen = new Set();
  return value.result.schemaVersion === d.schemaVersion && a.length === sessions.size && a.every(item => {
    if (seen.has(item.lessonId)) return false;
    seen.add(item.lessonId);
    const lesson = sessions.get(item.lessonId);
    return ['lessonId', 'subjectId', 'teacherId', 'cohortId', 'roomId', 'periodId'].every((key, i) =>
      typeof item[key] === 'string' && ids[['lessons', 'subjects', 'teachers', 'cohorts', 'rooms', 'periods'][i]].has(item[key]))
      && ['subjectId', 'cohortId'].every(key => item[key] === lesson[key])
      && (lesson.portionId ? item.audienceId === lesson.audienceId && typeof item.allocationId === 'string' : item.teacherId === lesson.teacherId);
  });
}
