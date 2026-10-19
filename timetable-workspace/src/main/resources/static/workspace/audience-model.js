import { resolveRelationships } from './setup-relationships.js';

// Derived indexes only: the exact accepted definition/result are never rewritten for presentation.
export function sessionDefinitions(definition) {
  const sessions = new Map((definition.lessons ?? []).map(l => [l.id, l]));
  for (const plan of definition.audiencePlans ?? []) for (const portion of plan.portions) {
    for (const id of portion.sessionIds) sessions.set(id, { ...plan, ...portion, id, displayName:portion.displayName ?? plan.displayName ?? id,
      cohortId: plan.classId, subjectId: plan.subjectId, planId: plan.id, portionId: portion.id,
      requiredRoomCapabilityIds: portion.requiredRoomCapabilityIds ?? plan.requiredRoomCapabilityIds ?? [],
      blockRules: portion.blockRules ?? plan.blockRules ?? [] });
  }
  for (const lock of definition.sessionLocks ?? []) {
    const lesson=sessions.get(lock.sessionId);
    if(lesson)sessions.set(lock.sessionId,{...lesson,...(lock.periodId!==undefined?{periodLock:lock.periodId}:{}),...(lock.roomId!==undefined?{roomLock:lock.roomId}:{})});
  }
  return sessions;
}
export const sessionCount = d => sessionDefinitions(d).size;
export function classAudiences(definition, c) {
  const plans = (definition.audiencePlans ?? []).filter(p => p.classId === c.id);
  const used = (c.splits ?? []).filter(s => plans.some(p => p.portions.some(q => q.sessionIds.length && s.groups.some(g => g.id === q.audienceId))));
  const { blocks, issues } = resolveRelationships(c, used.map(s => s.id));
  if (issues.length) throw new Error(issues.map(i => i.message).join(' '));
  const normalized = blocks.map(b => ({ ...b, id: b.groupIds.length === 0 ? `class:${c.id}` : b.groupIds.length === 1 ? b.groupIds[0] : JSON.stringify([...b.groupIds].sort()) }));
  const audiences = new Map([[`class:${c.id}`, { id: `class:${c.id}`, displayName: 'Whole class', size: c.size, blocks: normalized.map(b => b.id) }]]);
  for (const s of c.splits ?? []) for (const g of s.groups) audiences.set(g.id, { ...g, splitName: s.displayName, splitId: s.id, blocks: normalized.filter(b => b.groupIds.includes(g.id)).map(b => b.id) });
  const base = used.length === 1 ? used[0] : c.relationships?.mode === 'nested' ? used.find(s => s.id === c.relationships.constituentSplitId) : null;
  const lanes = base?.groups.map(g => audiences.get(g.id)) ?? (used.length === 0 ? [audiences.get(`class:${c.id}`)] : null);
  return { audiences, blocks: normalized, lanes };
}
export function audienceDescription(item) {
  const a = item.audience;
  return a ? `${a.splitName ? a.splitName + ' / ' : ''}${a.displayName} · ${a.size} students` : 'Whole class';
}
export function shortGroupName(audience) {
  return (audience?.displayName ?? '').replace(/^Group ([1-9]\d*)$/, 'G$1');
}
export function audienceCell(model, classId, periodId, visibleItems) {
  const context = model.audienceClasses.get(classId);
  const all = model.assignmentsByCell.get(`${classId}\u0000${periodId}`) ?? [];
  const occupied = new Set(all.flatMap(a => a.audience.blocks));
  const untaught = context.blocks.filter(b => !occupied.has(b.id));
  const filtered = all.some(a => !visibleItems.some(v => v.lessonId === a.lessonId));
  let spans = []; const occupiedLanes=new Set();
  if (context.lanes) {
    for (const item of all) {
      const indices = context.lanes.flatMap((lane, i) => lane.blocks.every(b => item.audience.blocks.includes(b)) ? [i] : []);
      const represented = indices.flatMap(i => context.lanes[i].blocks);
      if (!indices.length || represented.length !== item.audience.blocks.length || indices.at(-1) - indices[0] + 1 !== indices.length) return { all, untaught, filtered, spans: null, context };
      if(indices.some(i=>occupiedLanes.has(i)))return {all,untaught,filtered,spans:null,context};
      indices.forEach(i=>occupiedLanes.add(i));
      spans.push({ item, start: indices[0], length: indices.length });
    }
    for (let i = 0; i < context.lanes.length; i++) if (context.lanes[i].blocks.every(b => !occupied.has(b))) spans.push({ audience: context.lanes[i], start: i, length: 1 });
  } else spans = null;
  return { all, untaught, filtered, spans, context };
}
export function blockDescription(context, block) {
  return block.groupIds.map(id => context.audiences.get(id)?.displayName ?? id).join(' ∩ ') || 'Whole class';
}
