#!/usr/bin/env python3
"""Check packaged samples offline; never start the web app or modify operator artifacts."""
import argparse
import json
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
source = ROOT / 'timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar'
prefix = 'BOOT-INF/classes/demo/timetables/'
with zipfile.ZipFile(source) as archive:
    original = {name.removeprefix(prefix): archive.read(name) for name in archive.namelist()
                if name.startswith(prefix) and not name.endswith('/')}
checks = []
with tempfile.TemporaryDirectory(prefix='demo-maintenance-') as directory:
    for case in ['packaged-baseline', 'stale-score', 'invalid-pair', 'english-invalid-pair', 'imperial-invalid-pair']:
        samples = Path(directory) / case
        samples.mkdir()
        for name, body in original.items(): (samples / name).write_bytes(body)
        identifier = {'english-invalid-pair': 'english', 'imperial-invalid-pair': 'star-wars'}.get(case, 'full')
        path = samples / f'{identifier}-a-result.json'
        result = json.loads(path.read_text())
        if case == 'stale-score': result['score']['ordinaryPreferencePenalty'] += 1
        if 'invalid-pair' in case: result['timetable']['assignments'][0]['periodId'] = 'nonexistent-period'
        if case != 'packaged-baseline': path.write_text(json.dumps(result))
        log = args.output / f'{case}.log'
        with log.open('w') as output:
            process = subprocess.run(['java', '--class-path', str(ROOT / 'kernel-cli/target/school-kernel.jar'),
                str(ROOT / 'tools/demo/VerifySampleScores.java'), str(samples)],
                stdout=output, stderr=subprocess.STDOUT, timeout=45)
        assert (process.returncode == 0) == (case == 'packaged-baseline'), case
        checks.append({'case': case, 'exit': process.returncode, 'webApplicationStarted': False,
                       'log': log.name, 'verdict': 'PASS'})
(args.output / 'checks.json').write_text(json.dumps(checks, indent=2) + '\n')
print('PASS: offline maintenance accepts packaged samples and refuses stale scores and invalid pairs; no web startup')
