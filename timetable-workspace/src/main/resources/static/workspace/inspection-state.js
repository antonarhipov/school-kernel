const PREFERENCE_VERSION = 1;
const PREFERENCE_LIMIT = 1024;
const AVAILABLE_MODES = Object.freeze({ INITIAL_PROPOSAL: ['PROPOSAL'], ACCEPTED_BASELINE: ['CURRENT'], REPAIR_DRAFT: ['CURRENT', 'DRAFT'],
  SOLVING_REPAIR: ['CURRENT', 'DRAFT', 'SOLVING'], REPAIR_PROPOSAL: ['CURRENT', 'DRAFT', 'PROPOSAL'],
  MANUAL_DRAFT: ['CURRENT', 'DRAFT'] });

export function createInspectionState({ schoolId, weekdays, subjectIds = [], teacherIds = [], cohortIds = [], roomIds = [], periodIds = [], storage = window.localStorage }) {
  const firstWeekday = weekdays[0] || null;
  let state = { range: 'WEEK', weekdayId: firstWeekday, selectedLessonId: null, reviewTargetSide: null,
    subjectId: null, teacherId: null, subjectOnly: false,
    searchQuery: '', cohortId: null, teacherFilterId: null, roomId: null, periodId: null,
    focusedType: null, focusedId: null, scrollContext: null, lensScrollContext: null,
    lifecycle: null, mode: 'CURRENT', inspectorOpen: window.matchMedia('(min-width: 1280px)').matches,
    filtersOpen: false, utilitiesOpen: false,
    taskAreaOpen: { CURRENT: false, DRAFT: true, SOLVING: true, PROPOSAL: true } };

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

  // Entering a lens records where class rows were scrolled; leaving it returns that position as restoreScroll.
  const leaveOrEnterLens = (next, scrollContext = null) => {
    const wasLens = Boolean(state.teacherFilterId || state.roomId);
    const isLens = Boolean(next.teacherFilterId || next.roomId);
    const restoreScroll = wasLens && !isLens ? state.lensScrollContext : null;
    state = { ...next, lensScrollContext: isLens ? (wasLens ? state.lensScrollContext : scrollContext) : null };
    return { changed: true, state, restoreScroll };
  };

  return Object.freeze({
    current: () => Object.freeze({ ...state, taskAreaOpen: Object.freeze({ ...state.taskAreaOpen }) }),
    enterLifecycle: lifecycle => {
      if (!AVAILABLE_MODES[lifecycle]) return state;
      if (state.lifecycle !== lifecycle) state = { ...state, lifecycle, mode: AVAILABLE_MODES[lifecycle].at(-1),
        utilitiesOpen: false,
        taskAreaOpen: lifecycle === 'SOLVING_REPAIR' ? { ...state.taskAreaOpen, CURRENT: false } : state.taskAreaOpen };
      return state;
    },
    selectMode: mode => {
      if (!AVAILABLE_MODES[state.lifecycle]?.includes(mode)) return { changed: false, state };
      state = { ...state, mode };
      return { changed: true, state };
    },
    setInspectorOpen: inspectorOpen => { state = { ...state, inspectorOpen: Boolean(inspectorOpen) }; return state; },
    setFiltersOpen: filtersOpen => { state = { ...state, filtersOpen: Boolean(filtersOpen) }; return state; },
    setUtilitiesOpen: utilitiesOpen => { state = { ...state, utilitiesOpen: Boolean(utilitiesOpen) }; return state; },
    setTaskAreaOpen: (mode, open) => {
      if (!AVAILABLE_MODES[state.lifecycle]?.includes(mode)) return { changed: false, state };
      state = { ...state, taskAreaOpen: { ...state.taskAreaOpen, [mode]: Boolean(open) } };
      return { changed: true, state };
    },
    selectLesson: (lessonId, reviewTargetSide = null) => {
      state = { ...state, selectedLessonId: lessonId || null, reviewTargetSide: lessonId ? reviewTargetSide : null };
      return state;
    },
    closeLesson: () => { state = { ...state, selectedLessonId: null, reviewTargetSide: null }; return state; },
    selectSubject: subjectId => {
      if (subjectId !== null && !subjectIds.includes(subjectId)) return { changed: false, state };
      state = { ...state, subjectId, subjectOnly: subjectId ? state.subjectOnly : false };
      return { changed: true, state };
    },
    selectTeacher: teacherId => {
      if (teacherId !== null && !teacherIds.includes(teacherId)) return { changed: false, state };
      state = { ...state, teacherId };
      return { changed: true, state };
    },
    setSubjectOnly: subjectOnly => {
      if (typeof subjectOnly !== 'boolean' || (subjectOnly && !state.subjectId)) return { changed: false, state };
      state = { ...state, subjectOnly };
      return { changed: true, state };
    },
    setSearch: searchQuery => {
      if (typeof searchQuery !== 'string') return { changed: false, state };
      state = { ...state, searchQuery };
      return { changed: true, state };
    },
    // scrollContext is the class-row scroll position, recorded only when a lens is entered from class rows.
    selectFilter: (filter, id, scrollContext = null) => {
      const validIds = filter === 'cohortId' ? cohortIds : filter === 'teacherFilterId' ? teacherIds
        : filter === 'roomId' ? roomIds : filter === 'periodId' ? periodIds : null;
      if (!validIds || (id !== null && !validIds.includes(id))) return { changed: false, state };
      // Teacher and room filters are lenses: at most one of them pivots the matrix at a time.
      const otherLens = id === null ? null : filter === 'teacherFilterId' ? 'roomId' : filter === 'roomId' ? 'teacherFilterId' : null;
      return leaveOrEnterLens({ ...state, [filter]: id, ...(otherLens ? { [otherLens]: null } : {}) }, scrollContext);
    },
    resetFilters: () => leaveOrEnterLens({ ...state, searchQuery: '', cohortId: null, teacherFilterId: null, roomId: null,
      periodId: null, subjectOnly: false }),
    clearNarrowing: () => leaveOrEnterLens({ ...state, cohortId: null, teacherFilterId: null, roomId: null, periodId: null,
      subjectOnly: false }),
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
      state = { ...state, range: 'DAY', weekdayId, selectedLessonId: selectionCleared ? null : state.selectedLessonId,
        reviewTargetSide: selectionCleared ? null : state.reviewTargetSide };
      return { changed: true, selectionCleared, state };
    },
    persist: () => persistPreference(storage, schoolId, state),
    resetEphemeral: () => { state = { ...state, selectedLessonId: null, reviewTargetSide: null }; return state; }
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
