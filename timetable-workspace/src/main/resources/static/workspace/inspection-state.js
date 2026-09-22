const PREFERENCE_VERSION = 1;
const PREFERENCE_LIMIT = 1024;

export function createInspectionState({ schoolId, weekdays, subjectIds = [], teacherIds = [], cohortIds = [], roomIds = [], periodIds = [], storage = window.localStorage }) {
  const firstWeekday = weekdays[0] || null;
  let state = { range: 'WEEK', weekdayId: firstWeekday, selectedLessonId: null,
    subjectId: null, teacherId: null, subjectOnly: false, teacherOnly: false,
    searchQuery: '', cohortId: null, teacherFilterId: null, roomId: null, periodId: null,
    focusedType: null, focusedId: null, scrollContext: null };

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
    selectSubject: subjectId => {
      if (subjectId !== null && !subjectIds.includes(subjectId)) return { changed: false, state };
      state = { ...state, subjectId, subjectOnly: subjectId ? state.subjectOnly : false };
      return { changed: true, state };
    },
    selectTeacher: teacherId => {
      if (teacherId !== null && !teacherIds.includes(teacherId)) return { changed: false, state };
      state = { ...state, teacherId, teacherOnly: teacherId ? state.teacherOnly : false };
      return { changed: true, state };
    },
    setSubjectOnly: subjectOnly => {
      if (typeof subjectOnly !== 'boolean' || (subjectOnly && !state.subjectId)) return { changed: false, state };
      state = { ...state, subjectOnly };
      return { changed: true, state };
    },
    setTeacherOnly: teacherOnly => {
      if (typeof teacherOnly !== 'boolean' || (teacherOnly && !state.teacherId)) return { changed: false, state };
      state = { ...state, teacherOnly };
      return { changed: true, state };
    },
    resetInvestigationFilters: () => {
      state = { ...state, subjectOnly: false, teacherOnly: false };
      return { changed: true, state };
    },
    setSearch: searchQuery => {
      if (typeof searchQuery !== 'string') return { changed: false, state };
      state = { ...state, searchQuery };
      return { changed: true, state };
    },
    selectFilter: (filter, id) => {
      const validIds = filter === 'cohortId' ? cohortIds : filter === 'teacherFilterId' ? teacherIds
        : filter === 'roomId' ? roomIds : filter === 'periodId' ? periodIds : null;
      if (!validIds || (id !== null && !validIds.includes(id))) return { changed: false, state };
      state = { ...state, [filter]: id };
      return { changed: true, state };
    },
    resetFilters: () => {
      state = { ...state, searchQuery: '', cohortId: null, teacherFilterId: null, roomId: null, periodId: null,
        subjectOnly: false, teacherOnly: false };
      return { changed: true, state };
    },
    openFocused: (focusedType, focusedId, scrollContext) => {
      const validIds = focusedType === 'cohortId' ? cohortIds : focusedType === 'teacherId' ? teacherIds
        : focusedType === 'roomId' ? roomIds : null;
      if (!validIds || !validIds.includes(focusedId)) return { changed: false, state };
      state = { ...state, focusedType, focusedId, scrollContext: scrollContext || null };
      return { changed: true, state };
    },
    changeFocusedType: (focusedType, focusedId) => {
      const validIds = focusedType === 'cohortId' ? cohortIds : focusedType === 'teacherId' ? teacherIds
        : focusedType === 'roomId' ? roomIds : null;
      if (!validIds || !validIds.includes(focusedId)) return { changed: false, state };
      state = { ...state, focusedType, focusedId };
      return { changed: true, state };
    },
    returnToWholeSchool: () => {
      state = { ...state, focusedType: null, focusedId: null };
      return { changed: true, state };
    },
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
