const PREFERENCE_VERSION = 1;
const PREFERENCE_LIMIT = 1024;

export function createInspectionState({ schoolId, weekdays, storage = window.localStorage }) {
  const firstWeekday = weekdays[0] || null;
  let state = { range: 'WEEK', weekdayId: firstWeekday, selectedLessonId: null };

  if (schoolId && firstWeekday) {
    try {
      const raw = storage.getItem(preferenceKey(schoolId));
      if (raw && raw.length <= PREFERENCE_LIMIT) {
        const preference = JSON.parse(raw);
        if (validPreference(preference, weekdays)) state = { ...state, range: preference.range, weekdayId: preference.weekdayId };
      }
    } catch (_) {
      // Local display preference is optional and never blocks inspection.
    }
  }

  return Object.freeze({
    current: () => Object.freeze({ ...state }),
    selectLesson: lessonId => { state = { ...state, selectedLessonId: lessonId || null }; return state; },
    closeLesson: () => { state = { ...state, selectedLessonId: null }; return state; },
    selectRange: (range, selectedWeekday) => {
      if (!['WEEK', 'DAY'].includes(range)) return { changed: false, state };
      const weekdayId = range === 'DAY' && weekdays.includes(selectedWeekday) ? selectedWeekday : state.weekdayId;
      state = { ...state, range, weekdayId };
      return { changed: true, state };
    },
    selectDay: (weekdayId, selectedLessonWeekday) => {
      if (!weekdays.includes(weekdayId)) return { changed: false, selectionCleared: false, state };
      const selectionCleared = Boolean(state.selectedLessonId && selectedLessonWeekday !== weekdayId);
      state = { ...state, range: 'DAY', weekdayId, selectedLessonId: selectionCleared ? null : state.selectedLessonId };
      return { changed: true, selectionCleared, state };
    },
    persist: () => persistPreference(storage, schoolId, state),
    resetEphemeral: () => { state = { ...state, selectedLessonId: null }; return state; }
  });
}

export function preferenceKey(schoolId) { return `school-kernel.inspection.v1.${schoolId}`; }

function validPreference(value, weekdays) {
  return value && typeof value === 'object' && !Array.isArray(value)
    && Object.keys(value).length === 3 && value.version === PREFERENCE_VERSION
    && ['WEEK', 'DAY'].includes(value.range) && typeof value.weekdayId === 'string'
    && weekdays.includes(value.weekdayId);
}

function persistPreference(storage, schoolId, state) {
  if (!schoolId || !state.weekdayId) return true;
  try {
    storage.setItem(preferenceKey(schoolId), JSON.stringify({ version: PREFERENCE_VERSION, range: state.range, weekdayId: state.weekdayId }));
    return true;
  } catch (_) { return false; }
}
