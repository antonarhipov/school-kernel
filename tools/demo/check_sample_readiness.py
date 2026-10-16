#!/usr/bin/env python3
"""Corrupt disposable packaged copies and require startup refusal; never edits operator or source artifacts."""
import argparse
import hashlib
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
    original = {name: archive.read(name) for name in archive.namelist() if name.startswith(prefix) and not name.endswith('/')}
checks = []
with tempfile.TemporaryDirectory(prefix='demo-readiness-') as directory:
    for case in ['old-build', 'stale-score', 'invalid-pair', 'english-invalid-pair', 'imperial-invalid-pair']:
        evidence = json.loads(original[prefix + 'scoring-evidence.json'])
        identifier = {'english-invalid-pair': 'english', 'imperial-invalid-pair': 'star-wars'}.get(case, 'full')
        result = json.loads(original[prefix + identifier + '-a-result.json'])
        updates = {}
        if case == 'old-build': evidence['kernelBuild'] = 'stale-build'
        if case == 'stale-score': result['score']['ordinaryPreferencePenalty'] += 1
        if case in ['invalid-pair', 'english-invalid-pair', 'imperial-invalid-pair']:
            result['timetable']['assignments'][0]['periodId'] = 'nonexistent-period'
            data = json.dumps(result, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()
            evidence['samples'][identifier + '/a']['resultSha256'] = hashlib.sha256(data).hexdigest()
        if case != 'old-build': updates[prefix + identifier + '-a-result.json'] = json.dumps(result).encode()
        updates[prefix + 'scoring-evidence.json'] = json.dumps(evidence).encode()
        jar = Path(directory) / f'{case}.jar'
        with zipfile.ZipFile(source) as archive, zipfile.ZipFile(jar, 'w') as target:
            for entry in archive.infolist(): target.writestr(entry, updates.get(entry.filename, archive.read(entry)))
        log = args.output / f'{case}.log'
        with log.open('w') as output:
            result = subprocess.run(['java', '-Xmx192m', '-jar', str(jar), '--spring.profiles.active=demo',
                '--server.address=127.0.0.1', '--server.port=0', '--demo.public-hosts=localhost:8080',
                '--workspace.kernel-executable=' + str(ROOT / 'school-kernel'),
                '--demo.kernel-artifact=' + str(ROOT / 'kernel-cli/target/school-kernel.jar')],
                stdout=output, stderr=subprocess.STDOUT, timeout=45)
        content = log.read_text()
        assert result.returncode != 0 and ('APPLICATION FAILED TO START' in content or 'Application run failed' in content), case
        assert 'Started WorkspaceApplication in' not in content, case
        checks.append({'case': case, 'exit': result.returncode, 'ready': False, 'log': log.name, 'verdict': 'PASS'})
    # Evidence maintenance itself must refuse stale scores even if hashes could be recomputed.
    samples = Path(directory) / 'samples'; samples.mkdir()
    for name, body in original.items(): (samples / name.removeprefix(prefix)).write_bytes(body)
    path = samples / 'two-class-a-result.json'; document = json.loads(path.read_text()); document['score']['ordinaryPreferencePenalty'] += 1
    path.write_text(json.dumps(document))
    with (args.output / 'stale-score-maintenance.log').open('w') as output:
        result = subprocess.run(['java', '--class-path', str(ROOT / 'kernel-cli/target/school-kernel.jar'),
            str(ROOT / 'tools/demo/VerifySampleScores.java'), str(samples)], stdout=output, stderr=subprocess.STDOUT, timeout=30)
    assert result.returncode != 0
    checks.append({'case': 'stale-score-maintenance', 'exit': result.returncode, 'verdict': 'PASS'})
(args.output / 'checks.json').write_text(json.dumps(checks, indent=2) + '\n')
print('PASS: real packaged readiness refuses stale build, stale score and invalid pair; maintenance cannot bless stale scores')
