#!/usr/bin/env python3
"""Check fictional sample provenance and shipped JARs without emitting source identities."""
import argparse
import hashlib
import io
import json
import subprocess
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', required=True, type=Path)
parser.add_argument('jars', nargs='+', type=Path)
args = parser.parse_args()
sys.dont_write_bytecode = True
from generate_samples import COLLECTIONS, FIELDS, strings
samples = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'
source_names = set()
for filename in ['mv5', 'mvk', 'initial-school']:
    source = json.loads((ROOT / f'examples/{filename}.json').read_text())
    source_names.add(source['displayName'])
    source_names.update(t['displayName'] for t in source['teachers'])
for identifier, filename, title in [('two-class','mv5','Ranniku õppehoone'), ('full','mvk','Kasevälja Põhikool')]:
    source = json.loads((ROOT / f'examples/{filename}.json').read_text())
    sample = json.loads((samples / f'{identifier}-definition.json').read_text())
    assert sample['displayName'] == title
    teachers = [t['displayName'] for t in sample['teachers']]
    assert len(teachers) == len(set(teachers)) and set(teachers).isdisjoint(source_names)
    for collection in COLLECTIONS:
        assert {row['id'] for row in source.get(collection, [])}.isdisjoint(row['id'] for row in sample.get(collection, [])), identifier
    for field in FIELDS:
        assert set(strings(source, field)).isdisjoint(strings(sample, field)), identifier

# Both full-size fictional curricula below are independently constructed, with no source mapping.
for identifier, title, prefix in [('english', 'Meadowbrook Community School', 'meadowbrook-'),
                                   ('star-wars', 'Coruscant Imperial Academy', 'imperial-')]:
    sample = json.loads((samples / f'{identifier}-definition.json').read_text())
    assert sample['displayName'] == title
    teachers = [t['displayName'] for t in sample['teachers']]
    assert len(teachers) == len(set(teachers)) and set(teachers).isdisjoint(source_names)
    for collection in COLLECTIONS:
        assert all(row['id'].startswith(prefix) for row in sample.get(collection, []))
    for filename in ['mv5', 'mvk', 'initial-school']:
        source = json.loads((ROOT / f'examples/{filename}.json').read_text())
        for field in FIELDS:
            assert set(strings(source, field)).isdisjoint(strings(sample, field)), identifier

checked = []
def scan(data, path):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for entry in archive.infolist():
            if entry.is_dir(): continue
            assert not entry.filename.startswith('examples/'), 'Real examples packaged'
            assert '/demo/samples/' not in entry.filename and '/demo/timetables/small-' not in entry.filename, 'Retired sample packaged'
            body = archive.read(entry)
            if entry.filename.endswith('.jar'):
                scan(body, path + '!' + entry.filename)
            else:
                # Exact string literals cover every source name; full multiword names are also scanned as raw bytes.
                # A generic single-word substring in third-party author metadata is not a source identity.
                for name in source_names:
                    for literal in ({json.dumps(name, ensure_ascii=False), json.dumps(name, ensure_ascii=True)} | ({name} if ' ' in name else set())):
                        assert literal.encode() not in body, 'Source identity found in shipped artifact'
                    encoded = name.encode()
                    if entry.filename.endswith('.class'):
                        assert b'\x01' + struct.pack('>H', len(encoded)) + encoded not in body, 'Source identity in JVM string constant'
                checked.append(path + '!' + entry.filename)

artifacts = {}
for jar in args.jars:
    data = jar.read_bytes(); scan(data, str(jar))
    artifacts[str(jar)] = hashlib.sha256(data).hexdigest()
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps({'verdict': 'PASS', 'artifacts': artifacts, 'scannedEntries': len(checked),
    'sourceIdentityCount': len(source_names), 'schools': 4, 'variants': 8,
    'identityMapping': 'two synthetic mappings; English and Imperial schools independently constructed', 'sourceIdentityMatches': 0,
    'sourceReferenceOverlap': 0, 'packagedExamples': 0}, indent=2) + '\n')
print('PASS: fictional sample provenance, source reference exclusion and', len(checked), 'packaged entries')
