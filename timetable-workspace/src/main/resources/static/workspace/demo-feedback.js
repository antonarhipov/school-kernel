const rules = {
  'hard.teacher-period': ['Teacher double-booked', 'These lessons use the same teacher at the same time. Choose another qualified teacher or move one lesson to a free time slot.'],
  'hard.cohort-period': ['Class double-booked', 'This class has more than one lesson at the same time. Move one of the lessons to a free time slot.'],
  'hard.room-period': ['Room double-booked', 'These lessons use the same room at the same time. Choose another suitable room or move one lesson.'],
  'hard.teacher-availability': ['Teacher unavailable', 'The teacher is unavailable at this time. Choose a time in their availability or another qualified teacher.'],
  'hard.cohort-availability': ['Class unavailable', 'The class is unavailable at this time. Choose a time in the class availability.'],
  'hard.room-availability': ['Room unavailable', 'The room is unavailable at this time. Choose another suitable room or time slot.'],
  'hard.room-capacity': ['Room too small', 'The room cannot hold this class. Choose a room with enough seats.'],
  'hard.room-capability': ['Room lacks required equipment', 'This lesson needs a specialist room. Choose a room with the required capabilities.'],
  'hard.period-lock': ['Required time slot', 'This lesson is fixed to a time slot in the school definition. Restore its required time.'],
  'hard.room-lock': ['Required room', 'This lesson is fixed to a room in the school definition. Restore its required room.'],
  'hard.room-assignment': ['Room assignment rule', 'The room violates a school room-assignment policy. Choose a room allowed by that policy.'],
  'hard.cohort-home-room': ['Home room required', 'This class lesson must use its home room. Restore the class home room.'],
  'hard.reserved-period': ['Reserved time slot', 'This time is reserved for other lessons. Move this lesson to an ordinary or permitted reserved slot.'],
  'hard.cohort-gap': ['Gap in the class day', 'This class has too many empty periods between lessons. Move a lesson into a gap or move an edge lesson to shorten the day.'],
  'hard.cohort-late-start': ['Class starts too late', 'The first lesson starts after the latest permitted start. Move an appropriate lesson earlier in this class day.'],
  'hard.subject-day-edge': ['Subject at a forbidden day edge', 'This subject cannot be first or last in the class day. Move it inside the day or move another lesson to the edge.'],
  'hard.subject-daily-limit': ['Too many lessons of one subject today', 'This class exceeds the daily subject limit. Move one of these subject lessons to another weekday.'],
  'hard.subject-reserved-limit': ['Too many reserved slots for this subject', 'This class exceeds the weekly reserved-slot allowance for the subject. Move a lesson to an ordinary slot.'],
  'hard.cohort-daily-spread': ['Class days are too uneven', 'The difference between this class’s longest and shortest days is too large. Move an appropriate lesson from a longer day to a shorter day.']
};
const soft = {
  'soft.teacher-gap': ['Teacher gaps', 'Teachers have empty periods between lessons.'],
  'soft.cohort-gap': ['Class gaps', 'Classes have empty periods between lessons.'],
  'soft.cohort-late-start': ['Later class starts', 'Some class days start after the preferred first period.'],
  'soft.cohort-week-balance': ['Uneven class days', 'Weekly lesson counts differ between weekdays.'],
  'soft.series-same-day': ['Repeated subject on one day', 'Lessons in the same series are concentrated on one weekday.'],
  'soft.undesirable-period': ['Less preferred times', 'Some lessons use a time that a teacher, class or lesson would prefer to avoid.'],
  'soft.non-preferred-room': ['Less preferred rooms', 'Some lessons use a suitable room outside their preferred choices.']
};
export function conflictItems(evidence, model) {
  if (!evidence) return [];
  if (evidence.status === 'INVALID_INPUT') return (evidence.validationReport?.errors || []).map(error => ({
    rule: 'definition', title: 'Definition rule', message: `${error.location || '/'}: ${error.message}. Choose an eligible teacher or restore the original value.`,
    lessonIds: model.assignments.filter(a => error.entityIds?.some(id => model.assignmentMap.has(id))
      ? error.entityIds.includes(a.lessonId) : error.entityIds?.includes(a.teacherId) || error.entityIds?.includes(a.cohortId)).map(a => a.lessonId)
  }));
  return (evidence.conflicts || []).map(c => {
    let message = rules[c.constraintId]?.[1] || 'Restore the original value or choose a value allowed by this school’s rules.';
    if (c.constraintId === 'hard.cohort-gap' && model?.maps) {
      const periods = [...new Set((c.examples || []).flat())].map(id => model.maps.periods.get(id)).filter(Boolean);
      if (periods.length) message += ' Empty periods: ' + periods.map(p => `${p.weekday[0] + p.weekday.slice(1).toLowerCase()} · ${p.order}${p.startTime ? ' (' + p.startTime.slice(0,5) + '–' + p.endTime.slice(0,5) + ')' : ''}`).join(', ') + '.';
    }
    if (c.constraintId === 'hard.room-capacity' && model) {
      message += ' ' + [...new Set(c.lessonIds.map(id => model.assignmentMap.get(id)).filter(Boolean)
        .map(a => `${a.cohort.displayName}: ${a.cohort.size} pupils; ${a.room.displayName}: ${a.room.capacity} seats.`))].join(' ');
    }
    return { ...c, rule: c.constraintId, title: rules[c.constraintId]?.[0] || c.constraintId, message };
  });
}
export function warningItems(evidence, baseline) {
  const before = new Map((baseline?.score?.constraintBreakdown || []).map(c => [c.constraintId, c.matchCount]));
  return Object.entries(evidence?.softMatchCounts || {}).filter(([,count]) => count > 0).map(([id,count]) => ({
    title: soft[id]?.[0] || id, message: soft[id]?.[1] || 'Preference cost.', count, delta: count - (before.get(id) || 0)
  }));
}
