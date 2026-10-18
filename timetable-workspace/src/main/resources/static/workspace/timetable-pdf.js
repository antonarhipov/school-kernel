import { PDFDocument, rgb } from './vendor/pdf/pdf-lib.js';
import fontkit from './vendor/pdf/fontkit.js';
import { M } from './messages.js';
import { resolvePdfScope } from './pdf-scope.js';

const WIDTH = 841.89, HEIGHT = 595.28, MARGIN = 32, BOTTOM = 550;
const INK = rgb(.09, .17, .13), MUTED = rgb(.32, .39, .35), GREEN = rgb(.12, .32, .24);
const LINE = rgb(.81, .85, .82), WHITE = rgb(1, 1, 1);
const PALETTE = [[.91,.95,.92], [.92,.94,.98], [.98,.94,.87], [.96,.91,.94], [.90,.96,.96], [.95,.94,.89]];
const clean = value => String(value ?? '').normalize('NFC').replace(/[\u2010-\u2015]/g, '-').replace(/\s+/g, ' ').trim();
const name = (entity, fallback) => clean(entity?.displayName || fallback);
let fontResources;

async function loadFonts() {
  if (!fontResources) fontResources = Promise.all(['Regular', 'Bold'].map(async weight => {
    const response = await fetch(new URL(`./vendor/pdf/LiberationSans-${weight}.ttf`, import.meta.url));
    if (!response.ok) throw new Error('PDF fonts could not load. Check your connection and try again.');
    return new Uint8Array(await response.arrayBuffer());
  })).catch(() => { fontResources = null; throw new Error('PDF fonts could not load. Check your connection and try again.'); });
  return fontResources;
}

export function selectPdfView({ model, filters, authoring }) {
  const { type, entityId } = resolvePdfScope(filters);
  const kind = { class: 'Class', teacher: 'Teacher', school: 'School' }[type];
  const entities = type === 'school' ? model.definition.cohorts
    : [model.maps[type === 'class' ? 'cohorts' : 'teachers'].get(entityId)].filter(Boolean);
  if (type !== 'school' && !entities.length) throw new Error(`The selected ${kind.toLowerCase()} is unavailable. Change the timetable filters and try again.`);
  const items = model.assignments.filter(item => type === 'school' || item[type === 'class' ? 'cohortId' : 'teacherId'] === entityId);
  if (!items.length) throw new Error(`No lessons are scheduled for this ${kind.toLowerCase()}. Choose another timetable to export.`);
  const days = model.weekdays;
  const periods = days.map(day => {
    const teaching = model.definition.periods.filter(p => p.weekday === day).sort((a, b) => a.order - b.order);
    if (!authoring?.grid) return teaching;
    let last = 0, between = 0;
    const extra = [];
    for (const interval of authoring.grid.intervals.filter(i => i.day === day)) {
      const period = teaching.find(p => p.id === interval.periodId);
      if (period) { last = period.order; between = 0; }
      else if (['Break', 'Lunch'].includes(interval.kind)) extra.push({ id: `interval-${interval.id}`, weekday: day,
        order: last + (++between) / 1000, displayName: interval.label, startTime: interval.start, endTime: interval.end, nonTeaching: interval.kind });
    }
    return [...teaching, ...extra];
  });
  const orders = [...new Set(periods.flat().map(p => p.order))].sort((a, b) => a - b);
  return { days, items, kind, type, entities, periods, orders };
}

// Line breaking uses actual embedded-font metrics, including unbroken long names.
function wrap(value, font, size, width) {
  const lines = []; let line = '';
  for (const word of clean(value).split(' ')) {
    const candidate = line ? `${line} ${word}` : word;
    if (font.widthOfTextAtSize(candidate, size) <= width) { line = candidate; continue; }
    if (line) { lines.push(line); line = ''; }
    for (const letter of word) {
      if (line && font.widthOfTextAtSize(line + letter, size) > width) { lines.push(line); line = ''; }
      line += letter;
    }
  }
  if (line) lines.push(line);
  return lines;
}

export async function createTimetablePdf(options) {
  const { model, schoolName, timetableName, draft = false, authoring } = options;
  const selected = selectPdfView(options);
  const { type } = selected;
  const [regularBytes, boldBytes] = await loadFonts();
  const pdf = await PDFDocument.create(); pdf.registerFontkit(fontkit);
  const regular = await pdf.embedFont(regularBytes, { subset: true });
  const bold = await pdf.embedFont(boldBytes, { subset: true });
  const charset = new Set(regular.getCharacterSet());
  const check = value => {
    const text = clean(value);
    if ([...text].some(char => !charset.has(char.codePointAt(0))))
      throw new Error('This timetable contains characters the PDF font cannot display. Please use a supported name before exporting.');
    return text;
  };
  const drawText = (page, value, x, top, size = 9, font = regular, color = INK) =>
    page.drawText(check(value), { x, y: HEIGHT - top - size, size, font, color });
  const box = (page, x, top, width, height, color, border = LINE) =>
    page.drawRectangle({ x, y: HEIGHT - top - height, width, height, color, borderColor: border, borderWidth: .4 });
  const linesFor = (value, size, font, width, color = INK) => wrap(check(value), font, size, width).map(text => ({ text, size, font, color }));
  const contentWidth = WIDTH - MARGIN * 2;
  const date = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date());
  pdf.setTitle(`${clean(schoolName)} - ${clean(timetableName)}`); pdf.setAuthor('School Kernel');
  pdf.setSubject(`${selected.kind} weekly timetable`); pdf.setCreator('School Kernel timetable export'); pdf.setLanguage('en');
  const sheets = [];
  const times = period => [period.startTime, period.endTime].filter(Boolean).map(t => t.slice(0,5)).join(' - ');
  const cellFor = (period, lessons, width, cohortId, includeTimes) => {
    const lines = [];
    if (period && includeTimes && times(period)) lines.push(...linesFor(times(period), 8, regular, width, MUTED));
    if (period?.nonTeaching) lines.push(...linesFor(`${period.nonTeaching}: ${period.displayName}`, 8, regular, width, MUTED));
    const lunch = authoring?.classes?.find(c => c.id === cohortId)?.lunchPeriodIds?.includes(period?.id);
    if (lunch) lines.push(...linesFor('Class lunch', 9, bold, width, MUTED));
    lessons.forEach((item, index) => {
      if (index) lines.push({ text: '', size: 8, font: regular, color: MUTED });
      lines.push(...linesFor(name(item.subject, item.subjectId), 9, bold, width));
      lines.push(...linesFor(name(item.teacher, item.teacherId), 8, regular, width));
      lines.push(...linesFor(`Room ${name(item.room, item.roomId)}`, 8, regular, width, MUTED));
      if (type === 'teacher') lines.push(...linesFor(`Class ${name(item.cohort, item.cohortId)}`, 8, regular, width, MUTED));
    });
    if (period && !lessons.length && !period.nonTeaching && !lunch) lines.push(...linesFor('No lesson', 8, regular, width, MUTED));
    const subjectIndex = model.definition.subjects.findIndex(s => s.id === lessons[0]?.subjectId);
    // Unoccupied slots keep only their grid border, leaving the paper unprinted.
    return { lines, color: lessons.length ? rgb(...PALETTE[Math.max(0, subjectIndex) % PALETTE.length]) : undefined };
  };
  const plans = [];
  // School export reuses the individual weekly layout for every class, in definition order.
  for (const entity of selected.entities) {
    const classView = type !== 'teacher';
    const items = type === 'school' ? selected.items.filter(item => item.cohortId === entity.id) : selected.items;
    const labelWidth = 38;
    const cellWidth = (contentWidth - labelWidth) / selected.days.length, width = cellWidth - 16;
    const occupied = items.map(item => item.period.order);
    const orders = occupied.length
      ? selected.orders.filter(order => order >= Math.min(...occupied) && order <= Math.max(...occupied))
      : selected.orders;
    const rows = orders.map(order => ({
      label: linesFor(Number.isInteger(order) ? String(order) : '', 10, bold, labelWidth - 12, MUTED),
      cells: selected.days.map((day, index) => {
        const period = selected.periods[index].find(p => p.order === order);
        return cellFor(period, items.filter(i => i.periodId === period?.id), width, classView ? entity.id : null, true);
      })
    }));
    plans.push({ heading: `${classView ? 'Class' : 'Teacher'} ${name(entity, entity.id)}`, scope: `Whole week / Slots ${orders[0]}-${orders.at(-1)}`, label: 'Slot', labelWidth, cellWidth,
      columns: selected.days.map(day => linesFor(M.days[day] || day, 10, bold, width, WHITE)), rows });
  }
  for (const plan of plans) {
    let page, y, part = 0;
    const newPage = () => {
      page = pdf.addPage([WIDTH, HEIGHT]); part++;
      box(page, 0, 0, WIDTH, 6, GREEN, GREEN);
      drawText(page, 'S C H O O L   K E R N E L', MARGIN, 22, 8, bold, GREEN);
      drawText(page, draft ? 'DRAFT - Unpublished changes' : 'TIMETABLE', WIDTH - MARGIN - 210, 22, 9, bold, draft ? rgb(.60,.25,.14) : MUTED);
      let top = 41;
      for (const line of linesFor(schoolName, 17, bold, contentWidth)) { drawText(page, line.text, MARGIN, top, 17, bold); top += 20; }
      for (const line of linesFor(`${timetableName} / ${plan.heading}${part > 1 ? ' / continued' : ''}`, 11, bold, contentWidth)) {
        drawText(page, line.text, MARGIN, top, 11, bold); top += 14;
      }
      for (const line of linesFor(plan.scope, 8, regular, contentWidth, MUTED)) {
        drawText(page, line.text, MARGIN, top + 3, 8, regular, MUTED); top += 10;
      }
      y = top + 15;
      const headerHeight = Math.max(25, Math.max(...plan.columns.map(c => c.length)) * 11 + 12);
      if (y + headerHeight > BOTTOM - 65) throw new Error('The page headings are too long for the PDF page. Shorten the titles and try again.');
      box(page, MARGIN, y, contentWidth, headerHeight, GREEN, GREEN);
      drawText(page, plan.label, MARGIN + 6, y + 7, 8, bold, WHITE);
      plan.columns.forEach((lines, index) => lines.forEach((line, row) => drawText(page, line.text, MARGIN + plan.labelWidth + index * plan.cellWidth + 8, y + 6 + row * 11, line.size, line.font, WHITE)));
      y += headerHeight; sheets.push(page);
    };
    newPage();
    for (const row of plan.rows) {
      let offset = 0;
      const totalLines = Math.max(2, row.label.length, ...row.cells.map(cell => cell.lines.length));
      while (offset < totalLines) {
        const desiredHeight = (totalLines - offset) * 10 + 10;
        if (y + Math.min(desiredHeight, 65) > BOTTOM || (desiredHeight <= BOTTOM - 160 && y + desiredHeight > BOTTOM)) newPage();
        const count = Math.min(totalLines - offset, Math.floor((BOTTOM - y - 10) / 10));
        if (count < 1) throw new Error('The page headings leave insufficient space for lessons. Shorten the titles and try again.');
        const height = count * 10 + 10;
        box(page, MARGIN, y, plan.labelWidth, height);
        const labelOffset = offset < row.label.length ? offset : 0;
        row.label.slice(labelOffset, labelOffset + count).forEach((line, index) => drawText(page, line.text, MARGIN + 6, y + 7 + index * 10, line.size, line.font, line.color));
        row.cells.forEach((cell, index) => {
          const x = MARGIN + plan.labelWidth + index * plan.cellWidth;
          box(page, x, y, plan.cellWidth, height, cell.color);
          cell.lines.slice(offset, offset + count).forEach((line, index) => drawText(page, line.text, x + 8, y + 7 + index * 10, line.size, line.font, line.color));
        });
        y += height; offset += count;
        if (offset < totalLines) newPage();
      }
    }
  }
  sheets.forEach((page, index) => {
    page.drawLine({ start: { x: MARGIN, y: 31 }, end: { x: WIDTH - MARGIN, y: 31 }, color: LINE, thickness: .5 });
    drawText(page, `School Kernel / ${date}${draft ? ' / DRAFT' : ''}`, MARGIN, 571, 8, regular, MUTED);
    const text = `${index + 1} / ${sheets.length}`;
    drawText(page, text, WIDTH - MARGIN - regular.widthOfTextAtSize(text, 8), 571, 8, regular, MUTED);
  });
  return pdf.save();
}

export async function downloadTimetablePdf(options) {
  const bytes = await createTimetablePdf(options);
  const url = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
  const link = document.createElement('a');
  const { type, entityId } = resolvePdfScope(options.filters);
  const entity = options.model.maps[type === 'teacher' ? 'teachers' : 'cohorts'].get(entityId);
  const filename = [options.schoolName, options.timetableName, type, entity?.displayName, 'week', options.draft ? 'draft' : '']
    .filter(Boolean).join('-').normalize('NFC').replace(/[^\p{L}\p{N}._-]+/gu, '-').replace(/-+/g, '-').slice(0, 160);
  link.href = url; link.download = `${filename || 'timetable'}.pdf`;
  document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}
