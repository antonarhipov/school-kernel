export function createProposalComparison(accepted, proposed, review) {
  const changes = new Map(review.changedLessons.map(change => [change.lessonId, change]));
  const entries = new Map();
  const assignmentsByCell = new Map();
  const assignmentsById = new Map();
  const assignments = [];
  const place = (item, side, change) => {
    if (!item) return;
    const representation = { ...item, comparisonSide: side, change };
    assignments.push(representation);
    const byId = assignmentsById.get(item.lessonId) || [];
    byId.push(representation);
    assignmentsById.set(item.lessonId, byId);
    const key = `${item.cohortId}\u0000${item.periodId}`;
    const cell = assignmentsByCell.get(key) || [];
    cell.push(representation);
    assignmentsByCell.set(key, cell);
  };
  for (const id of new Set([...accepted.assignmentMap.keys(), ...proposed.assignmentMap.keys(), ...changes.keys()])) {
    const old = accepted.assignmentMap.get(id);
    const next = proposed.assignmentMap.get(id);
    const change = changes.get(id);
    entries.set(id, { old, proposed: next, change });
    if (!change) { place(old, 'unchanged', null); continue; }
    if (old && next && old.cohortId === next.cohortId && old.periodId === next.periodId) {
      place(next, 'combined', change);
    } else {
      place(old, 'accepted', change);
      place(next, 'proposed', change);
    }
  }
  return { entries, assignments, assignmentsByCell, assignmentsById };
}