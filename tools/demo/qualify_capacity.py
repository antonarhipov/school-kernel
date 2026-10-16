#!/usr/bin/env python3
"""Prepare fictional target schools; optionally measure real cold solves through an isolated demo API.

This never deploys, changes provider settings, or qualifies memory/spend from solve results alone.
Run only on a disposable validation service configured with visitor reuse disabled.
"""
import argparse
import hashlib
import http.cookiejar
import json
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SAMPLES = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'


def representative(cohorts, lessons_each):
    subject_ids = [f'qualification-subject-{i:02}' for i in range(12)]
    teachers = [{'id': f'qualification-teacher-{i:03}', 'displayName': f'Fictional teacher {i+1:03}',
                 'qualifiedSubjectIds': subject_ids} for i in range(100)]
    periods = [{'id': f'qualification-period-{day}-{order:02}', 'weekday': day, 'order': order,
                'displayName': f'{day.title()} {order}'}
               for day in ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'] for order in range(1, 11)]
    lessons = [{'id': f'qualification-lesson-{cohort:02}-{occurrence:02}',
                'displayName': f'Lesson {cohort+1} / {occurrence+1}',
                'subjectId': subject_ids[occurrence % 12], 'cohortId': f'qualification-cohort-{cohort:02}',
                'teacherId': teachers[(cohort * 7 + occurrence * 3) % 100]['id'],
                'seriesId': f'qualification-series-{cohort:02}-{occurrence % 12:02}',
                'preferredRoomIds': [f'qualification-room-{cohort:02}']}
               for cohort in range(cohorts) for occurrence in range(lessons_each)]
    return {'schemaVersion': 1, 'catalogVersion': json.loads((SAMPLES / 'two-class-definition.json').read_text())['catalogVersion'],
            'schoolId': f'fictional-qualification-{cohorts}', 'displayName': f'Fictional qualification school ({cohorts} classes)',
            'subjects': [{'id': sid, 'displayName': f'Subject {i+1}'} for i, sid in enumerate(subject_ids)],
            'teachers': teachers, 'cohorts': [{'id': f'qualification-cohort-{i:02}', 'displayName': f'Class {i+1}', 'size': 24} for i in range(cohorts)],
            'rooms': [{'id': f'qualification-room-{i:02}', 'displayName': f'Room {i+1}', 'capacity': 30, 'capabilityIds': []} for i in range(80)],
            'periods': periods, 'lessons': lessons}


def dimensions(definition):
    result = {field: len(definition[field]) for field in ['lessons', 'cohorts', 'teachers', 'rooms', 'periods']}
    result['placementWork'] = result['lessons'] * result['periods'] * result['rooms']
    return result


class Demo:
    def __init__(self, base, local_insecure):
        url = urllib.parse.urlsplit(base)
        if url.scheme != 'https' or not url.netloc or url.query or url.fragment or url.path not in ['', '/']:
            raise ValueError('Use the exact HTTPS origin without a path, query or fragment')
        if local_insecure and url.hostname not in ['localhost', '127.0.0.1']:
            raise ValueError('Disposable TLS override is restricted to loopback')
        self.base = base.rstrip('/')
        context = ssl._create_unverified_context() if local_insecure else ssl.create_default_context()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()), urllib.request.HTTPSHandler(context=context))
        self.etag = None
        self.request('/api/workspace')
        self.csrf = self.request('/api/csrf')

    def request(self, path, method='GET', body=None, content='application/json', extra=None):
        headers = {'Accept': 'application/json', **(extra or {})}
        if method != 'GET':
            headers.update({'Origin': self.base, self.csrf['headerName']: self.csrf['token'], 'If-Match': self.etag, 'Content-Type': content})
        payload = body.encode() if isinstance(body, str) else json.dumps(body).encode() if body is not None else None
        try:
            response = self.opener.open(urllib.request.Request(self.base + path, data=payload, method=method, headers=headers), timeout=45)
        except urllib.error.HTTPError as error:
            raise RuntimeError(f'{method} {path}: {error.code}: {error.read().decode()}') from error
        with response:
            if response.headers.get('ETag') and not path.startswith('/api/samples'):
                self.etag = response.headers['ETag']
            return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--base', help='Omit to prepare fictional fixtures without any network access')
    parser.add_argument('--local-insecure', action='store_true', help='Allow disposable loopback TLS only')
    parser.add_argument('--runs', type=int, default=5)
    args = parser.parse_args()
    if args.runs < 5:
        parser.error('Qualification records require at least five consecutive cold runs')
    args.output.mkdir(parents=True, exist_ok=False)
    targets = {f'target-{cohorts}': representative(cohorts, lessons) for cohorts, lessons in [(60, 36), (72, 30)]}
    cases = {key: (json.loads((SAMPLES / f'{key}-definition.json').read_text()), effort) for key, effort in [('two-class', 'QUICK'), ('full', 'THOROUGH'), ('star-wars', 'THOROUGH'), ('english', 'THOROUGH')]}
    cases.update({key: (definition, 'THOROUGH') for key, definition in targets.items()})
    manifest = {'capacityQualified': False, 'deployedQualification': False,
                'note': 'Generated fixtures and cold solve evidence do not establish combined memory, platform or financial qualification.', 'inputs': {}}
    for name, (definition, effort) in cases.items():
        raw = json.dumps(definition, ensure_ascii=False, indent=2).encode() + b'\n'
        (args.output / f'{name}.json').write_bytes(raw)
        manifest['inputs'][name] = {**dimensions(definition), 'sha256': hashlib.sha256(raw).hexdigest(), 'effort': effort}
    (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    if not args.base:
        print(json.dumps(manifest, indent=2))
        return
    demo = Demo(args.base, args.local_insecure)
    config = demo.request('/api/demo/config')
    if config.get('reuseEnabled') is not False:
        raise RuntimeError('Refusing benchmarks: the running service must explicitly report reuse disabled')
    (args.output / 'effective-api-config.json').write_text(json.dumps(config, indent=2) + '\n')
    previous = set()
    rows = []
    try:
        for name, (definition, effort) in cases.items():
            for number in range(1, args.runs + 1):
                started = time.monotonic()
                demo.request('/api/demo/definition', 'POST', json.dumps(definition), 'text/plain;charset=UTF-8', {'X-Demo-Confirmed': 'true', 'X-Demo-Source': 'file'})
                run = demo.request('/api/runs', 'POST', {'effort': effort})['workspace']['run']['id']
                while True:
                    state = demo.request(f'/api/runs/{run}')
                    if state['status'] not in ['QUEUED', 'RUNNING', 'CHECKING_RESULT']:
                        break
                    if time.monotonic() - started > 240:
                        raise RuntimeError('Overall qualification request exceeded 240 seconds')
                    time.sleep(1)
                snapshot = demo.request('/api/workspace')
                (args.output / f'{name}-{number}-snapshot.json').write_text(json.dumps(snapshot, indent=2) + '\n')
                if snapshot['state'] != 'INITIAL_PROPOSAL':
                    raise RuntimeError(f'{name} cold run {number} did not produce a verified feasible proposal: {state}')
                proposal = snapshot['workspace']['proposal']
                computation = proposal['solveProvenance']['computationId']
                if proposal['reused'] or computation in previous:
                    raise RuntimeError('Reuse/coalescing cannot be counted as cold measurement')
                previous.add(computation)
                row = {'case': name, 'run': number, 'wallMs': round((time.monotonic()-started)*1000),
                       'solveProcessMs': proposal['originalSolveProcessMs'], 'verificationProcessMs': proposal['originalVerificationProcessMs'],
                       'solveProvenance': proposal['solveProvenance'], 'outcome': state['status'], 'dimensions': dimensions(definition)}
                rows.append(row)
                (args.output / 'cold-runs.json').write_text(json.dumps(rows, indent=2) + '\n')
        (args.output / 'verdict.txt').write_text('PASS: cold solve dimension only. Combined memory, spending/platform, lifecycle and launch qualification remain separate.\n')
    except Exception as error:
        (args.output / 'verdict.txt').write_text(f'FAIL: {error}\n')
        raise


if __name__ == '__main__':
    main()
