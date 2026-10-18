// Class takes precedence over teacher; the school and schedule come from the captured model.
export function resolvePdfScope(filters = {}) {
  if (filters.cohortId) return { type: 'class', entityId: filters.cohortId };
  if (filters.teacherId) return { type: 'teacher', entityId: filters.teacherId };
  return { type: 'school', entityId: '' };
}
