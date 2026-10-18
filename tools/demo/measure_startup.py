#!/usr/bin/env python3
"""Compare packaged JVM/native demo readiness and verify their HTTP contract.

Uses disposable local processes and sessions. Run from any directory after building
both artifacts; results and process logs are retained in the requested directory.
"""
import argparse
import hashlib
import http.cookiejar
import io
import json
from pathlib import Path
import platform
import re
import shutil
import socket
import statistics
import subprocess
import time
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]


class Visitor:
    def __init__(self, base):
        self.base = base
        self.http = urllib.request.build_opener(
            urllib.request.ProxyHandler({}),
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def request(self, path, data=None, headers=None, status=200):
        request = urllib.request.Request(self.base + path, data=data, headers=headers or {})
        try:
            response = self.http.open(request, timeout=35)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read()
            assert response.status == status, (path, response.status, body[:1500])
            return body, response.headers

    def json(self, path, **kwargs):
        body, headers = self.request(path, **kwargs)
        return json.loads(body), headers


def smoke(base):
    owner, other = Visitor(base), Visitor(base)
    static, _ = owner.request('/workspace/')
    assert static == (ROOT / 'timetable-workspace/src/main/resources/static/workspace/index.html').read_bytes()
    catalog, _ = owner.json('/api/samples')
    config, _ = owner.json('/api/demo/config')
    assert config['demo'] is True
    source = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'
    bundle, _ = owner.json('/api/demo/timetables')
    assert [school['identifier'] for school in bundle['schools']] == [row['identifier'] for row in catalog]
    for school in bundle['schools']:
        identifier = school['identifier']
        assert school['definition'] == json.loads((source / f'{identifier}-definition.json').read_text())
        for variant in school['timetables']:
            assert variant['result'] == json.loads((source / f"{identifier}-{variant['id']}-result.json").read_text())
    empty, version = owner.json('/api/workspace')
    assert empty['state'] == 'EMPTY'
    csrf, _ = owner.json('/api/csrf')
    headers = {'Origin': base, csrf['headerName']: csrf['token'],
               'If-Match': version['ETag'], 'Content-Type': 'application/json'}
    sample, _ = owner.json('/api/samples/two-class')
    accepted, _ = owner.json('/api/samples/two-class/use', data=b'{}', headers=headers)
    assert accepted['state'] == 'ACCEPTED_BASELINE'
    baseline = accepted['workspace']['acceptedBaseline']
    assert baseline['definition'] == sample['definition']
    assert baseline['result'] == sample['timetables'][0]['result']
    archive, _ = owner.request('/api/accepted/export')
    with zipfile.ZipFile(io.BytesIO(archive)) as exported:
        assert set(exported.namelist()) == {'school-definition.json', 'timetable-result.json', 'workspace-manifest.json'}
        for name, field in [('school-definition.json', 'definition'),
                            ('timetable-result.json', 'result'), ('workspace-manifest.json', 'manifest')]:
            assert json.loads(exported.read(name)) == baseline[field]
    foreign, _ = other.json('/api/workspace')
    assert foreign['state'] == 'EMPTY'
    check_headers = {**headers, 'Content-Type': 'text/plain'}
    verified, _ = owner.json('/api/demo/definition/check',
                             data=json.dumps(sample['definition']).encode(), headers=check_headers)
    assert verified['definitionRevision'] == baseline['result']['inputRevision']
    assert verified['build'] == bundle['build']
    review, _ = owner.json('/api/demo/timetable/review',
                           data=json.dumps({'definition': sample['definition'], 'result': baseline['result']}).encode(),
                           headers=headers)
    assert review['status'] == 'REVIEWED' and review['valid'] is True
    assert review['definitionRevision'] == baseline['result']['inputRevision']
    owner.request('/api/demo/definition/check', data=b'{}', headers=check_headers, status=422)
    owner.request('/api/demo/definition/check', data=b'{}',
                  headers={**check_headers, 'Origin': 'https://foreign.example'}, status=403)
    after, _ = owner.json('/api/workspace')
    assert after == accepted
    return 'PASS: static HTML/config, four schools/eight exact timetables, sample use, exact export, session isolation, kernel verification/review, invalid input and foreign-origin refusal without mutation'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java', default=shutil.which('java'))
    parser.add_argument('--native', type=Path, default=ROOT / 'timetable-workspace/target/school-demo')
    parser.add_argument('--trials', type=int, default=3)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.trials < 3:
        parser.error('use at least three trials')
    args.output.mkdir(parents=True, exist_ok=False)
    jar = ROOT / 'timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar'
    native = args.native.resolve()
    commands = {'jvm': [args.java, '-jar', str(jar)], 'native': [str(native)]}
    artifact_hashes = {}
    for artifact in [jar, native]:
        with artifact.open('rb') as content:
            artifact_hashes[str(artifact)] = hashlib.file_digest(content, 'sha256').hexdigest()
    result = {'machine': platform.platform(), 'host_architecture': platform.machine(),
              'java_version': subprocess.run([args.java, '-version'], capture_output=True, text=True, check=True).stderr,
              'artifacts': artifact_hashes,
              'method': 'alternating sequential process launches; monotonic clock to HTTP 200 /healthz; 10ms polling; normal filesystem caches; no simultaneous builds',
              'trials': [], 'verdict': 'IN_PROGRESS'}
    report = args.output / 'results.json'
    try:
        for trial in range(args.trials):
            for label, command in commands.items():
                with socket.socket() as reserved:
                    reserved.bind(('127.0.0.1', 0))
                    port = reserved.getsockname()[1]
                base = f'http://127.0.0.1:{port}'
                flags = ['--spring.profiles.active=demo', f'--server.port={port}',
                         f'--demo.public-hosts=127.0.0.1:{port}',
                         f'--demo.kernel-artifact={ROOT / "kernel-cli/target/school-kernel.jar"}',
                         f'--workspace.kernel-executable={ROOT / "school-kernel"}']
                log = args.output / f'{label}-{trial + 1}.log'
                record = {'runtime': label, 'trial': trial + 1, 'command': command + flags, 'log': str(log)}
                with log.open('w') as output:
                    started = time.monotonic()
                    process = subprocess.Popen(command + flags, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
                    try:
                        health = Visitor(base)
                        while time.monotonic() - started < 40:
                            if process.poll() is not None:
                                raise RuntimeError(f'{label} exited; see {log}')
                            try:
                                body, _ = health.json('/healthz')
                                if body == {'status': 'UP'}:
                                    break
                            except (OSError, AssertionError):
                                time.sleep(.01)
                        else:
                            raise RuntimeError(f'{label} readiness timeout; see {log}')
                        record['http_seconds'] = round(time.monotonic() - started, 4)
                        # The startup log can follow the first health response by a few milliseconds.
                        deadline = time.monotonic() + 2
                        while True:
                            match = re.search(r'Started WorkspaceApplication in ([\d.]+) seconds', log.read_text())
                            if match or time.monotonic() > deadline:
                                break
                            time.sleep(.01)
                        record['boot_seconds'] = float(match[1]) if match else None
                        if trial == 0:
                            record['smoke'] = smoke(base)
                    finally:
                        process.terminate()
                        try:
                            process.wait(timeout=10)
                        except subprocess.TimeoutExpired:
                            process.kill()
                            process.wait()
                            raise RuntimeError(f'{label} did not shut down within 10 seconds')
                        record['exit_code'] = process.returncode
                        result['trials'].append(record)
                        report.write_text(json.dumps(result, indent=2) + '\n')
                print(json.dumps(record), flush=True)
        result['medians'] = {label: {
            metric: statistics.median(row[metric] for row in result['trials'] if row['runtime'] == label)
            for metric in ['http_seconds', 'boot_seconds']} for label in commands}
        assert result['medians']['native']['http_seconds'] < result['medians']['jvm']['http_seconds']
        result['verdict'] = 'PASS'
        print(json.dumps(result['medians'], indent=2))
    except BaseException as error:
        result['verdict'] = f'FAIL: {error}'
        raise
    finally:
        report.write_text(json.dumps(result, indent=2) + '\n')


if __name__ == '__main__':
    main()
