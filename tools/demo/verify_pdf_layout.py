#!/usr/bin/env python3
"""Check rendered PDF cell fills and bell placement against exact source assignments."""
import json, re, sys
from pathlib import Path
import pdfplumber

PALETTE = [( .91,.95,.92),(.92,.94,.98),(.98,.94,.87),(.96,.91,.94),(.90,.96,.96),(.95,.94,.89)]
root = Path(sys.argv[1] if len(sys.argv)>1 else 'output/pdf/layout-regression')
for path in sorted(root.glob('*.pdf')):
    source = json.loads(path.with_suffix('.json').read_text())
    definition, result = source['definition'], source['result']
    days = list(dict.fromkeys(p['weekday'] for p in definition['periods']))
    items = [a for a in result['timetable']['assignments'] if a['cohortId' if source['kind']=='class' else 'teacherId']==source['entityId']]
    checked = set()
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            assert 'Complete printable key' not in page.extract_text()
            assert not re.search(r'\b[LC]\d+\b|Input revision:|Consecutive policy:|Coordination:',page.extract_text())
            assert 'Slot' in page.extract_text()
            for char in page.chars:
                assert char['size'] >= 7.99 and 0 <= char['x0'] <= char['x1'] <= page.width, (path, char)
            rows = sorted([r for r in page.rects if abs(r['x0']-32)<.01 and abs(r['width']-100)<.01],key=lambda r:r['top'])
            for row in rows:
                label = page.crop((32,row['top'],132,row['bottom'])).extract_text()
                order = int(label.splitlines()[0])
                times = []
                for period in [p for p in definition['periods'] if p['order']==order]:
                    time = ' - '.join(period[k][:5] for k in ('startTime','endTime') if period.get(k))
                    if time and time not in times: times.append(time)
                for time in times: assert time in label, (path, label, time)
                if len(times)==1: assert label.count(times[0])==1, (path, label)
                for index, day in enumerate(days):
                    width=(page.width-64-100)/len(days);x=132+index*width
                    cell=page.crop((x,row['top'],x+width,row['bottom']))
                    assert not re.search(r'\d\d:\d\d',cell.extract_text() or ''), (path,cell.extract_text())
                    period=next((p for p in definition['periods'] if p['weekday']==day and p['order']==order),None)
                    lessons=[a for a in items if period and a['periodId']==period['id']]
                    if not lessons: assert not (cell.extract_text() or '').strip(), (path,cell.extract_text())
                    for lesson in lessons:
                        subject=next(s for s in definition['subjects'] if s['id']==lesson['subjectId'])
                        assert subject['displayName'] in cell.extract_text(), (path,lesson,cell.extract_text())
                        room=next(r for r in definition['rooms'] if r['id']==lesson['roomId'])['displayName']
                        assert room in cell.extract_text() and ('Room '+room) not in cell.extract_text(), (path,cell.extract_text())
                        expected=PALETTE[next(i for i,s in enumerate(definition['subjects']) if s['id']==lesson['subjectId'])%len(PALETTE)]
                        fills=[r.get('non_stroking_color') for r in page.rects if abs(r['x0']-x)<.01 and abs(r['top']-row['top'])<.01 and abs(r['width']-width)<.01]
                        assert any(isinstance(f,(list,tuple)) and len(f)==3 and all(abs(a-b)<.001 for a,b in zip(f,expected)) for f in fills),(path,lesson,fills,expected)
                        checked.add(lesson['lessonId'])
        assert checked=={a['lessonId'] for a in items},(path,len(checked),len(items))
    print(f'PASS {path}: {len(checked)} lessons with exact subject colors; bells only in left column')
