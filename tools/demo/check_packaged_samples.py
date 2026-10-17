#!/usr/bin/env python3
"""Check fictional sample provenance and shipped JARs without emitting source identities."""
import argparse
import hashlib
import io
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', required=True, type=Path)
parser.add_argument('jars', nargs='+', type=Path)
args = parser.parse_args()
sys.dont_write_bytecode = True
import re
samples = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'
retired_hashes = set(json.loads(Path(__file__).with_name('retired-identity-hashes.json').read_text()))
for identifier, title in [('two-class', 'Ranniku õppehoone'), ('full', 'Kasevälja Põhikool'),
                           ('english', 'Meadowbrook Community School'), ('star-wars', 'Coruscant Imperial Academy')]:
    sample = json.loads((samples / f'{identifier}-definition.json').read_text())
    assert sample['displayName'] == title
    teachers = [t['displayName'] for t in sample['teachers']]
    assert len(teachers) == len(set(teachers))
    assert all(hashlib.sha256(name.encode()).hexdigest() not in retired_hashes for name in teachers)


def check_identity(value):
    assert hashlib.sha256(value.encode()).hexdigest() not in retired_hashes, 'Retired identity found in shipped artifact'

checked = []
def scan(data, path):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for entry in archive.infolist():
            if entry.is_dir(): continue
            assert not entry.filename.startswith('examples/'), 'Real examples packaged'
            assert '/demo/samples/' not in entry.filename and '/demo/timetables/small-' not in entry.filename, 'Retired sample packaged'
            body = archive.read(entry)
            if "/demo/timetables/" in entry.filename:
                resource = samples / Path(entry.filename).name
                assert resource.is_file() and body == resource.read_bytes(), "Packaged timetable resource differs from authoritative file"
            if entry.filename.endswith('.jar'):
                scan(body, path + '!' + entry.filename)
            else:
                # Check JSON/JS string literals and JVM UTF-8 constants against the
                # historical deny-list without retaining the retired source identities.
                for literal in re.findall(rb'"(?:[^"\\]|\\.)*"', body):
                    try: check_identity(json.loads(literal))
                    except (ValueError, UnicodeError): pass
                if entry.filename.endswith('.class'):
                    for offset, marker in enumerate(body[:-2]):
                        if marker != 1: continue
                        length = int.from_bytes(body[offset+1:offset+3], 'big')
                        try: check_identity(body[offset+3:offset+3+length].decode())
                        except UnicodeError: pass
                checked.append(path + '!' + entry.filename)

artifacts = {}
for jar in args.jars:
    data = jar.read_bytes(); scan(data, str(jar))
    artifacts[str(jar)] = hashlib.sha256(data).hexdigest()
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps({'verdict': 'PASS', 'artifacts': artifacts, 'scannedEntries': len(checked),
    'retiredIdentityHashCount': len(retired_hashes), 'schools': 4, 'variants': 8,
    'identityCheck': 'retired JSON literals and JVM UTF-8 constants checked by SHA-256', 'retiredIdentityMatches': 0, 'packagedExamples': 0}, indent=2) + '\n')
print('PASS: authoritative fictional samples, retired identity exclusion and', len(checked), 'packaged entries')
