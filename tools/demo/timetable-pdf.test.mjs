import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { makeAcceptedModel } from '../../timetable-workspace/src/main/resources/static/workspace/accepted-model.js';
import { createTimetablePdf, selectPdfView } from '../../timetable-workspace/src/main/resources/static/workspace/timetable-pdf.js';
import { PDFDocument } from '../../timetable-workspace/src/main/resources/static/workspace/vendor/pdf/pdf-lib.js';

const source = new URL('../../timetable-workspace/src/main/resources/demo/timetables/', import.meta.url);
const definition = JSON.parse(await readFile(new URL('full-definition.json', source)));
const result = JSON.parse(await readFile(new URL('full-a-result.json', source)));
const model = makeAcceptedModel({ definition, result });
// Only school/schedule/class/teacher define export scope; unrelated filters are deliberately impossible.
const filters = { range: 'DAY', day: 'MONDAY', cohortId: '', teacherId: '', roomId: 'missing', subjectId: 'missing', periodId: 'missing', search: 'missing' };
const originalFetch = globalThis.fetch;
globalThis.fetch = async url => new Response(await readFile(url));
const options = { model, filters, schoolName: definition.displayName, timetableName: 'Timetable A' };
const output = new URL('../../output/pdf/', import.meta.url);
await mkdir(output, { recursive: true });

test('selected class wins over teacher; selected teacher wins over school; each scope contains its full week', () => {
  const item = model.assignments[0];
  const teacherId = definition.teachers[0].id;
  const cases = [
    [{}, 'School', model.assignments],
    [{ cohortId: item.cohortId }, 'Class', model.assignments.filter(a => a.cohortId === item.cohortId)],
    [{ teacherId }, 'Teacher', model.assignments.filter(a => a.teacherId === teacherId)],
    [{ cohortId: item.cohortId, teacherId }, 'Class', model.assignments.filter(a => a.cohortId === item.cohortId)]
  ];
  for (const [scope, kind, expected] of cases) {
    const selected = selectPdfView({ ...options, filters: { ...filters, ...scope } });
    assert.equal(selected.kind, kind);
    assert.deepEqual(selected.days, model.weekdays);
    assert.deepEqual(selected.items.map(a => a.lessonId), expected.map(a => a.lessonId));
  }
  const both = selectPdfView({ ...options, filters: { ...filters, cohortId: item.cohortId, teacherId } });
  assert.ok(both.items.some(a => a.teacherId !== teacherId), 'class PDF includes lessons with other teachers');
  assert.throws(() => selectPdfView({ ...options, filters: { ...filters, cohortId: 'missing' } }), /selected class is unavailable/);
  const baseline = structuredClone({ definition, result });
  baseline.definition.teachers.push({ id: 'unassigned', displayName: 'Unassigned' });
  assert.throws(() => selectPdfView({ ...options, model: makeAcceptedModel(baseline), filters: { ...filters, teacherId: 'unassigned' } }), /No lessons are scheduled/);
});

test('school, class and teacher PDF artifacts use embedded fonts and readable A4 landscape pages without changing source', async () => {
  const before = JSON.stringify({ definition, result, filters });
  for (const type of ['school', 'class', 'teacher']) {
    const entityId = type === 'class' ? model.assignments[0].cohortId : type === 'teacher' ? model.assignments[0].teacherId : '';
    const scopedFilters = { ...filters, cohortId: type === 'class' ? entityId : '', teacherId: type === 'teacher' ? entityId : '' };
    const bytes = await createTimetablePdf({ ...options, filters: scopedFilters });
    const pdf = await PDFDocument.load(bytes);
    assert.equal(pdf.getTitle(), definition.displayName + ' - Timetable A');
    assert.equal(pdf.getSubject(), `${type[0].toUpperCase() + type.slice(1)} weekly timetable`);
    assert.ok(pdf.getPageCount() >= (type === 'school' ? definition.cohorts.length : 1));
    for (const page of pdf.getPages()) { assert.ok(Math.abs(page.getWidth() - 841.89) < .01); assert.ok(Math.abs(page.getHeight() - 595.28) < .01); }
    await writeFile(new URL(`timetable-${type}.pdf`, output), bytes);
  }
  assert.equal(JSON.stringify({ definition, result, filters }), before);
});

test('combined school book includes every class and matches the sum of individual weekly pages', async () => {
  const directory = new URL('individual-classes/', output);
  await mkdir(directory, { recursive: true });
  let pages = 0;
  for (const cohort of definition.cohorts) {
    const bytes = await createTimetablePdf({ ...options, filters: { ...filters, cohortId: cohort.id } });
    pages += (await PDFDocument.load(bytes)).getPageCount();
    await writeFile(new URL(`${cohort.id}.pdf`, directory), bytes);
  }
  assert.equal((await PDFDocument.load(await createTimetablePdf(options))).getPageCount(), pages);

  // Authoring details use the same path in a book, including continuation pages and empty classes.
  const smallDefinition = JSON.parse(await readFile(new URL('two-class-definition.json', source)));
  const smallResult = JSON.parse(await readFile(new URL('two-class-a-result.json', source)));
  smallDefinition.cohorts.push({ id: 'empty-class', displayName: 'Empty class' });
  const smallModel = makeAcceptedModel({ definition: smallDefinition, result: smallResult });
  const period = smallModel.assignments[0].period;
  const authoring = { grid: { intervals: [
    { id: 'teaching', day: period.weekday, periodId: period.id },
    { id: 'break', day: period.weekday, kind: 'Break', label: 'Morning break', start: '09:00', end: '09:15' }
  ] }, classes: [{ id: smallDefinition.cohorts[0].id, lunchPeriodIds: [period.id] }] };
  const authored = { ...options, model: smallModel, authoring, draft: true };
  const authoredDirectory = new URL('authored-classes/', output);
  await mkdir(authoredDirectory, { recursive: true });
  for (const cohort of smallDefinition.cohorts.slice(0, -1)) {
    await writeFile(new URL(`${cohort.id}.pdf`, authoredDirectory), await createTimetablePdf({ ...authored, filters: { ...filters, cohortId: cohort.id } }));
  }
  const book = await createTimetablePdf(authored);
  assert.ok((await PDFDocument.load(book)).getPageCount() >= smallDefinition.cohorts.length);
  await writeFile(new URL('timetable-authored-school.pdf', output), book);
  await writeFile(new URL('authored-definition.json', output), JSON.stringify(smallDefinition));
  assert.throws(() => selectPdfView({ ...authored, filters: { ...filters, cohortId: 'empty-class' } }), /No lessons are scheduled/);
});

test('selected schedule supplies exact assignments instead of a different variant or school', async () => {
  const variantB = JSON.parse(await readFile(new URL('full-b-result.json', source)));
  const selectedModel = makeAcceptedModel({ definition, result: variantB });
  const cohortId = definition.cohorts[0].id;
  const selected = selectPdfView({ ...options, model: selectedModel, filters: { ...filters, cohortId } });
  assert.deepEqual(selected.items.map(a => [a.lessonId, a.periodId, a.roomId]), variantB.timetable.assignments.filter(a => a.cohortId === cohortId).map(a => [a.lessonId, a.periodId, a.roomId]));
  assert.notDeepEqual(selected.items.map(a => [a.lessonId, a.periodId, a.roomId]), model.assignments.filter(a => a.cohortId === cohortId).map(a => [a.lessonId, a.periodId, a.roomId]));
  await writeFile(new URL('timetable-class-b.pdf', output), await createTimetablePdf({ ...options, model: selectedModel, filters: { ...filters, cohortId }, timetableName: 'Variant B' }));
});

test('long labels paginate, authoring breaks and lunches remain representable, and unsupported glyphs refuse', async () => {
  const baseline = structuredClone({ definition, result });
  baseline.definition.subjects.forEach(subject => subject.displayName = 'LONGSUBJECT ' + 'VeryLongUnbrokenLabel'.repeat(20));
  const longModel = makeAcceptedModel(baseline);
  const period = model.assignments[0].period;
  const authoring = { grid: { intervals: [{ id: 'b', day: period.weekday, kind: 'Break', label: 'Morning break', start: '08:00', end: '08:15' }] }, classes: [{ id: model.assignments[0].cohortId, lunchPeriodIds: [period.id] }] };
  for (const type of ['class', 'school']) {
    const bytes = await createTimetablePdf({ ...options, model: longModel, filters: { ...filters, cohortId: type === 'class' ? model.assignments[0].cohortId : '' }, authoring, draft: true });
    assert.ok((await PDFDocument.load(bytes)).getPageCount() > 1);
    await writeFile(new URL(`timetable-long-${type}.pdf`, output), bytes);
  }
  await assert.rejects(createTimetablePdf({ ...options, schoolName: 'School 🏫' }), /characters the PDF font cannot display/);
});

test.after(() => { globalThis.fetch = originalFetch; });
