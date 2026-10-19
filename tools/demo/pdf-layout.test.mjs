import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { makeAcceptedModel } from '../../timetable-workspace/src/main/resources/static/workspace/accepted-model.js';
import { createTimetablePdf, selectPdfView } from '../../timetable-workspace/src/main/resources/static/workspace/timetable-pdf.js';
import { slotLabels } from '../../timetable-workspace/src/main/resources/static/workspace/pdf-style.js';

const source = 'timetable-workspace/src/main/resources/demo/timetables/';
const definition = JSON.parse(await readFile(source + 'full-definition.json'));
const result = JSON.parse(await readFile(source + 'full-a-result.json'));
const directory = 'output/pdf/layout-regression';
await mkdir(directory, { recursive: true });
const originalFetch = globalThis.fetch;
globalThis.fetch = async url => new Response(await readFile(url));

test('common and weekday-specific bells are represented once in the slot column', () => {
  const selected = selectPdfView({ model: makeAcceptedModel({ definition, result }), filters: {} });
  const period = selected.periods[0][0];
  assert.deepEqual(slotLabels(selected, period.order), [String(period.order), `${period.startTime.slice(0,5)} - ${period.endTime.slice(0,5)}`]);
  const varied = structuredClone(selected);
  varied.periods[1].find(p => p.order === period.order).startTime = '07:55';
  const labels = slotLabels(varied, period.order);
  assert.equal(labels.length, selected.days.length + 1);
  assert(labels.some(label => label.startsWith('Tue 07:55')));
});

test('current and legacy class/teacher exports retain colored lessons and left-side bells', async () => {
  const legacy = {
    definition: { schemaVersion: 1, schoolId: 'legacy', displayName: 'Legacy school',
      cohorts: [{ id: 'c', displayName: '1A', size: 20 }], subjects: [{ id: 's', displayName: 'Math' }],
      teachers: [{ id: 't', displayName: 'Teacher' }], rooms: [{ id: 'r', displayName: '101' }],
      periods: ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'].map((weekday, i) => ({ id: 'p'+i, weekday, order: 1, displayName: 'First', startTime: i===1?'09:00':'08:00', endTime: i===1?'09:45':'08:45' })),
      lessons: [{ id: 'l', cohortId: 'c', subjectId: 's' }] },
    result: { timetable: { assignments: [{ lessonId: 'l', cohortId: 'c', subjectId: 's', teacherId: 't', roomId: 'r', periodId: 'p0' }] } }
  };
  const varied = structuredClone({ definition, result });
  varied.definition.periods.filter(p => p.weekday === 'TUESDAY').forEach(p => { p.startTime = '07:55'; p.endTime = '08:40'; });
  for (const [version, pair] of [['current', { definition, result }], ['current-varied', varied], ['legacy', legacy]]) {
    const model = makeAcceptedModel(pair);
    for (const kind of ['class', 'teacher']) {
      const entityId = kind === 'class' ? pair.definition.cohorts[0].id : pair.definition.teachers.find(t => t.displayName === 'Mari Tammela')?.id ?? pair.definition.teachers[0].id;
      const filters = { [kind === 'class' ? 'cohortId' : 'teacherId']: entityId };
      const before = JSON.stringify(pair);
      await writeFile(`${directory}/${version}-${kind}.pdf`, await createTimetablePdf({ model, filters, schoolName: pair.definition.displayName, timetableName: 'Variant A' }));
      await writeFile(`${directory}/${version}-${kind}.json`, JSON.stringify({ ...pair, kind, entityId }));
      assert.equal(JSON.stringify(pair), before);
    }
  }
});
test.after(() => { globalThis.fetch = originalFetch; });
