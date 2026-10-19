#!/usr/bin/env python3
"""Record hashes only after BuildTimetableVariants evaluates every complete candidate with the current kernel."""
import hashlib
import json
import zipfile
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SAMPLES = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()


def main():
    with zipfile.ZipFile(ROOT/'kernel-cli/target/school-kernel.jar') as archive:
        digest = hashlib.sha256()
        for name in sorted(n for n in archive.namelist() if n.startswith('org/schoolkernel/') and n.endswith('.class')
                           or n.startswith('schema/') and n.endswith('.json')):
            digest.update(name.encode()+b'\0'+archive.read(name))
    checked = json.loads(subprocess.check_output(['java', '--class-path', str(ROOT/'kernel-cli/target/school-kernel.jar'),
        str(ROOT/'tools/demo/VerifySampleScores.java'), str(SAMPLES)], text=True))
    evidence = {'kernelBuild': digest.hexdigest(), 'catalogVersion': checked['catalogVersion'], 'samples': {}}
    for identifier in ['two-class', 'full', 'star-wars', 'english']:
        definition = json.loads((SAMPLES/f'{identifier}-definition.json').read_text())
        for variant in ['a', 'b']:
            result = json.loads((SAMPLES/f'{identifier}-{variant}-result.json').read_text())
            assert result['status'] == 'FEASIBLE' and result['score'] == checked['samples'][f'{identifier}/{variant}']
            evidence['samples'][f'{identifier}/{variant}'] = {
                'definitionSha256': hashlib.sha256(canonical(definition)).hexdigest(),
                'resultSha256': hashlib.sha256(canonical(result)).hexdigest(), 'score': result['score']}
    (SAMPLES/'scoring-evidence.json').write_text(json.dumps(evidence, indent=2)+'\n')


if __name__ == '__main__': main()
