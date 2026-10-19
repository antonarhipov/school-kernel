#!/usr/bin/env python3
"""Check timetable-only PDFs: readable lessons, no technical appendix, colors and grid geometry."""
import json,re,sys,unicodedata
from pathlib import Path
import pdfplumber
root=Path(sys.argv[1] if len(sys.argv)>1 else Path('output/pdf/student-groups/latest-run.txt').read_text().strip())
def norm(s):return re.sub(r'\s+','',re.sub('[\u2010-\u2015]','-',unicodedata.normalize('NFC',s)))
for path in sorted(root.glob('*.pdf')):
    key=path.stem
    source='nested' if key=='teacher' else key
    d=json.loads((root/f'{source}-definition.json').read_text())
    result=json.loads((root/f'{source}-review.json').read_text())['result']
    items=result['timetable']['assignments']
    if key=='teacher':items=[a for a in items if a['teacherId']==result['timetable']['assignments'][0]['teacherId']]
    with pdfplumber.open(path) as pdf:
        pages=[p.extract_text() or '' for p in pdf.pages];text='\n'.join(pages)
        assert not re.search(r'Complete printable key|Session:|Allocation:|Definition:|Input revision:|Consecutive policy:|Coordination:|Matched context:|Control |\b[LC]\d+\b',text),path
        # Check full labels inside their own printed lane, so adjacent columns cannot hide omissions.
        lane_text=[]
        for number,p in enumerate(pdf.pages,1):
            assert abs(p.width-841.89)<.01 and abs(p.height-595.28)<.01
            assert f'{number} / {len(pdf.pages)}' in pages[number-1]
            assert 'Slot' in pages[number-1], 'Every page must contain a timetable grid'
            for c in p.chars:assert c['size']>=7.99 and 0<=c['x0']<=c['x1']<=p.width and 0<=c['top']<=c['bottom']<=p.height,(path,c)
            rows=[r for r in p.rects if abs(r['x0']-32)<.01 and abs(r['width']-100)<.01]
            assert rows
            for row in rows:
                days=p.crop((132,row['top'],p.width-32,row['bottom'])).extract_text() or ''
                assert not re.search(r'\d\d:\d\d',days),'Bells belong only in the left column'
                assert 'No lesson' not in days and 'No selected lesson' not in days
                lanes=[r for r in p.rects if r['x0']>=132 and abs(r['top']-row['top'])<.01]
                assert all(abs(r['height']-row['height'])<.01 for r in lanes),'Split lanes fill the row without an empty header'
                for r in lanes:
                    lane_text.append(((round(r['x0'],2),round(r['width'],2)),p.crop((r['x0'],r['top'],r['x1'],r['bottom'])).extract_text() or ''))
        # Join each column across continuation pages for very long wrapped labels.
        columns={}
        for geometry,value in lane_text:columns[geometry]=columns.get(geometry,'')+value
        compact=[norm(value) for value in columns.values()]
        for a in items:
            for field,collection in [('subjectId','subjects'),('roomId','rooms'),('teacherId' if key!='teacher' else 'cohortId','teachers' if key!='teacher' else 'cohorts')]:
                entity=next(e for e in d[collection] if e['id']==a[field]);assert any(norm(entity['displayName']) in value for value in compact),(path,a,field)
        if key=='nested':
            assert all(word in text for word in ['E1','E2','H1','H2','H3','H4'])
            assert any(abs(r['width']-(841.89-64-100)/5/2)<.1 for p in pdf.pages for r in p.rects),'Composite half-width span'
        print(f'PASS {path}: {len(items)} lessons in {len(pdf.pages)} timetable-only pages; bounded labels and full-height lanes')
