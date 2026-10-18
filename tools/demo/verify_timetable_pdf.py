#!/usr/bin/env python3
"""Independently verify exported lesson cells and combined/individual class layout parity.
Usage: python verify_timetable_pdf.py PDF DEFINITION RESULT [school|class|teacher] [ENTITY_ID]
       python verify_timetable_pdf.py --compare-classes PDF DEFINITION INDIVIDUAL_PDF_DIRECTORY [RESULT]
"""
import json
import re
import sys
import unicodedata
from pathlib import Path
import pdfplumber


def normalized(value):
    return re.sub(r'\s+', '', re.sub('[\u2010-\u2015]', '-', unicodedata.normalize('NFC', value or '')))


def page_class(page, cohorts):
    heading = normalized(page.extract_text().split('Whole week')[0])
    if heading.endswith('/continued'):
        heading = heading[:-len('/continued')]
    matches = [c for c in cohorts if heading.endswith('/Class' + normalized(c.get('displayName') or c['id']))]
    assert len(matches) == 1, (heading, matches)
    return matches[0]


def bounds(page):
    assert abs(page.width - 841.89) < .01 and abs(page.height - 595.28) < .01
    for char in page.chars:
        assert char['size'] >= 7.99 and 0 <= char['x0'] <= char['x1'] <= page.width and 0 <= char['top'] <= char['bottom'] <= page.height, char


def verify(pdf_path, definition_path, result_path, kind='school', entity_id=None):
    definition = json.loads(Path(definition_path).read_text())
    assignments = json.loads(Path(result_path).read_text())['timetable']['assignments']
    maps = {key: {row['id']: row for row in definition[key]} for key in ['subjects', 'teachers', 'cohorts', 'rooms', 'periods']}
    days = list(dict.fromkeys(p['weekday'] for p in definition['periods']))
    expected_items = [a for a in assignments if kind == 'school' or a['cohortId' if kind == 'class' else 'teacherId'] == entity_id]
    seen, class_sections = set(), []
    with pdfplumber.open(pdf_path) as pdf:
        for number, page in enumerate(pdf.pages, 1):
            bounds(page)
            page_items = expected_items
            if kind == 'school':
                cohort = page_class(page, definition['cohorts'])
                page_items = [a for a in expected_items if a['cohortId'] == cohort['id']]
                if not class_sections or class_sections[-1] != cohort['id']:
                    assert '/ continued' not in page.extract_text(), 'A class must begin on its own fresh page'
                    class_sections.append(cohort['id'])
            assert re.search(rf'\b{number} / {len(pdf.pages)}\b', page.extract_text()), 'Continuous book page numbers'
            label_width = 38
            rows = sorted([r for r in page.rects if abs(r['x0'] - 32) < .01 and abs(r['width'] - label_width) < .01], key=lambda r: r['top'])
            cell_width = (page.width - 64 - label_width) / len(days)
            for row in rows:
                label = normalized(page.crop((32, row['top'], 32 + label_width, row['bottom'])).extract_text())
                order = int(label)
                for index, day in enumerate(days):
                    period = next((p for p in definition['periods'] if p['weekday'] == day and p['order'] == order), None)
                    items = [a for a in page_items if period and a['periodId'] == period['id']]
                    x = 32 + label_width + index * cell_width
                    actual = normalized(page.crop((x, row['top'], x + cell_width, row['bottom'])).extract_text())
                    expected = ''
                    if period:
                        expected = ' - '.join(period[key][:5] for key in ['startTime', 'endTime'] if period.get(key))
                    for item in items:
                        expected += maps['subjects'][item['subjectId']]['displayName'] + maps['teachers'][item['teacherId']]['displayName']
                        expected += ' Room ' + maps['rooms'][item['roomId']]['displayName']
                        if kind == 'teacher':
                            expected += ' Class ' + maps['cohorts'][item['cohortId']]['displayName']
                        assert item['lessonId'] not in seen, item['lessonId']
                        seen.add(item['lessonId'])
                    if period and not items:
                        expected += ' No lesson'
                    assert actual == normalized(expected), (label, period, actual, normalized(expected))
        assert seen == {a['lessonId'] for a in expected_items}, f'{len(seen)}/{len(expected_items)} lessons'
        if kind == 'school':
            assert class_sections == [c['id'] for c in definition['cohorts']], class_sections
        print(f'PASS {kind}: {len(seen)} exact lessons across {len(pdf.pages)} A4 landscape pages; text >=8pt and within bounds.')


def compare_classes(pdf_path, definition_path, directory, result_path=None):
    """Compare text, font metrics, positions and table geometry, excluding only global footers."""
    cohorts = json.loads(Path(definition_path).read_text())['cohorts']
    def layout(page):
        bounds(page)
        chars = [(c['text'], round(c['size'], 3), *[round(c[k], 3) for k in ('x0', 'top', 'x1', 'bottom')]) for c in page.chars if c['top'] < 550]
        rects = [tuple(round(r[k], 3) for k in ('x0', 'top', 'width', 'height')) for r in page.rects]
        return chars, rects
    with pdfplumber.open(pdf_path) as book:
        sections, order = {}, []
        for page in book.pages:
            cohort = page_class(page, cohorts)
            if not order or order[-1] != cohort['id']:
                order.append(cohort['id'])
            sections.setdefault(cohort['id'], []).append(page)
        assert order == [c['id'] for c in cohorts], order
        matched = 0
        for cohort in cohorts:
            path = Path(directory) / (cohort['id'] + '.pdf')
            if not path.exists():
                # Empty classes cannot be exported individually, but still belong in the collection.
                assert result_path, f'Missing individual PDF: {path}'
                assignments = json.loads(Path(result_path).read_text())['timetable']['assignments']
                assert not any(a['cohortId'] == cohort['id'] for a in assignments), cohort['id']
                assert all('No lesson' in p.extract_text() for p in sections[cohort['id']])
                continue
            with pdfplumber.open(path) as individual:
                pages = sections[cohort['id']]
                assert len(pages) == len(individual.pages), cohort['id']
                for combined, single in zip(pages, individual.pages):
                    assert layout(combined) == layout(single), (cohort['id'], 'layout differs')
                matched += 1
        print(f'PASS class collection: {matched} class sections match individual PDFs exactly in text, font sizes, positions and grid geometry; {len(cohorts)} classes in order.')


if __name__ == '__main__':
    if sys.argv[1] == '--compare-classes':
        compare_classes(*sys.argv[2:])
    else:
        verify(*sys.argv[1:])
