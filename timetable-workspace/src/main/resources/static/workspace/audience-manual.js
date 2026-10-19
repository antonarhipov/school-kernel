/** Explicit proposals only. Callers must show every patch and obtain the chosen scope before applying it. */
export function audienceEditProposal(model, lessonId, field, value) {
  const selected = model.assignmentMap.get(lessonId);
  if (!selected) throw new Error('The selected lesson is unavailable. Reopen the exact timetable.');
  const single = [{ lessonId, [field]: value }];
  if (model.definition.schemaVersion !== 2 || value === selected[field]) return { single, combined: single };
  if (field === 'teacherId') {
    if (selected.lesson.teacherPolicy?.mode !== 'automatic') throw new Error('This teacher is fixed by the definition. Explicitly edit its teaching policy in School setup; the timetable is unchanged.');
    const teacher = model.maps.teachers.get(value);
    const restrictions = [selected.lesson.candidateTeacherIds,selected.lesson.teacherPolicy?.candidateTeacherIds];
    if (!teacher?.qualifiedSubjectIds?.includes(selected.subjectId) || !teacher.eligibleClassIdsBySubject?.[selected.subjectId]?.includes(selected.cohortId) || restrictions.some(candidates=>candidates && !candidates.includes(value)))
      throw new Error('The selected teacher is not qualified and eligible for this audience/subject allocation.');
    const combined = model.assignments.filter(a => a.allocationId === selected.allocationId).map(a => ({ lessonId:a.lessonId, teacherId:value }));
    return { single, combined, allocation:true, reason:'Weekly teacher continuity requires an explicit change to this whole automatic allocation.' };
  }
  if (!['periodId','roomId'].includes(field)) return {single,combined:single};
  const ids = new Set([lessonId]), reasons = new Set(), certifiedPeriods = new Map();
  // Retain whole certified rounds when a counted hard relation links their placement.
  if (field === 'periodId') for (const match of model.result.coordinationMatches ?? []) {
    if (!['START_TOGETHER','SAME_DAY','COVERAGE'].includes(match.type)) continue;
    for (const round of match.rounds ?? []) {
      const linked = round.occurrences.flatMap(o => o.sessionIds);
      if (linked.includes(lessonId)) { linked.forEach(id => ids.add(id)); for(const o of round.occurrences) o.sessionIds.forEach((id,i)=>{const period=model.maps.periods.get(o.periodIds?.[i]);if(period)certifiedPeriods.set(id,period);}); reasons.add(match.type + ' / ' + match.ruleId); }
    }
  }
  // A required consecutive run retains its same teacher and room, and moves as a listed proposal.
  if (selected.lesson.blockRules?.length) {
    const run = model.assignments.filter(a => a.cohortId === selected.cohortId && a.subjectId === selected.subjectId && a.audienceId === selected.audienceId && a.teacherId === selected.teacherId && a.roomId === selected.roomId && a.period.weekday === selected.period.weekday).sort((a,b)=>a.period.order-b.period.order);
    let start = run.findIndex(a=>a.lessonId===lessonId), end=start;
    while(start>0 && run[start-1].period.adjacentToNext && run[start-1].period.order+1===run[start].period.order)start--;
    while(end+1<run.length && run[end].period.adjacentToNext && run[end].period.order+1===run[end+1].period.order)end++;
    if(end>start){run.slice(start,end+1).forEach(a=>ids.add(a.lessonId));reasons.add('Required consecutive run');}
  }
  const destination = field === 'periodId' ? model.maps.periods.get(value) : null;
  const combined = [...ids].map(id => {
    const a=model.assignmentMap.get(id);
    if (field==='roomId') return {lessonId:id,roomId:value};
    const period=model.definition.periods.find(p=>p.weekday===destination?.weekday && p.order===destination.order+(certifiedPeriods.get(id) ?? a.period).order-(certifiedPeriods.get(lessonId) ?? selected.period).order);
    if(!period)return null;
    return {lessonId:id,periodId:period.id};
  });
  if(combined.some(p=>!p))return {single,combined:[],unavailable:true,reason:'The combined change does not fit the selected day. Keep only the selected provisional edit, cancel, or choose another slot.'};
  return {single,combined,reason:[...reasons].join('; ')};
}
export function describeAudiencePatches(model, patches) {
  return patches.map(p => {
    const a=model.assignmentMap.get(p.lessonId);
    return `${a.cohort.displayName} / ${a.subject.displayName} / ${a.audience.displayName} (${a.audience.size} students) / ${p.lessonId}: ${Object.entries(p).filter(([k])=>k!=='lessonId').map(([k,v])=>`${k==='teacherId'?'Teacher':k==='roomId'?'Room':'Period'} ${model.maps[k==='teacherId'?'teachers':k==='roomId'?'rooms':'periods'].get(v)?.displayName ?? v}`).join(', ')}`;
  });
}
