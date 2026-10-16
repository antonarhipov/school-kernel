#!/usr/bin/env python3
"""Disposable local packaging/health check. Never claims Railway/capacity qualification."""
import argparse
import json
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image', default='school-kernel-demo:local')
parser.add_argument('--memory-bytes', type=int, default=3000000000)
parser.add_argument('--cpus', type=float, default=2)
parser.add_argument('--web-heap-mb', type=int, default=512)
parser.add_argument('--verify-heap-mb', type=int, default=256)
parser.add_argument('--output', type=Path, default=Path('output/execution/demo-deployment/UC-12') / str(time.time_ns()))
args = parser.parse_args()
if min(args.memory_bytes, args.cpus, args.web_heap_mb, args.verify_heap_mb) <= 0:
    parser.error('Resource limits must be positive')
args.output.mkdir(parents=True, exist_ok=False)
container = 'school-demo-smoke-' + uuid.uuid4().hex


def docker(*arguments):
    return subprocess.check_output(['docker', *arguments], text=True).strip()


def request(port, path, host, headers=None):
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    message = urllib.request.Request(f'http://127.0.0.1:{port}{path}', headers={'Host': host, **(headers or {})})
    try:
        response = opener.open(message, timeout=3)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read()


started = False
checks = []
try:
    identity = docker('image', 'inspect', '--format', '{{.Id}}', args.image)
    docker('run', '--detach', '--name', container, '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=128m',
           '--cpus', str(args.cpus), '--memory', str(args.memory_bytes), '--publish', '127.0.0.1::8080',
           '--env', 'DEMO_PUBLIC_HOSTS=demo.example',
           '--env', 'DEMO_WEB_HEAP_MB=' + str(args.web_heap_mb),
           '--env', 'DEMO_VERIFY_HEAP_MB=' + str(args.verify_heap_mb), args.image)
    started = True
    port = int(docker('port', container, '8080/tcp').rsplit(':', 1)[1])
    deadline = time.monotonic() + 60
    while True:
        assert docker('inspect', '--format', '{{.State.Running}}', container) == 'true', 'Container stopped before readiness'
        try:
            status, headers, body = request(port, '/healthz', 'healthcheck.railway.app')
            if status == 200:
                assert json.loads(body) == {'status': 'UP'}
                assert 'Set-Cookie' not in headers
                break
        except (OSError, urllib.error.URLError):
            pass
        assert time.monotonic() < deadline, 'Readiness timed out'
        time.sleep(0.25)
    checks.append('PASS: read-only non-root image becomes ready after three real packaged-sample verifications; health creates no session')
    assert docker('exec', container, 'id', '-u') == '10001'
    runtime_files = set(docker('exec', container, 'sh', '-c',
        'find /opt/demo -maxdepth 1 -type f -printf "%f\\n"').splitlines())
    assert runtime_files == {'kernel', 'start', 'kernel.jar', 'workspace.jar'}
    for host, path, forwarding in [
        ('healthcheck.railway.app', '/api/workspace', {}),
        ('evil.example', '/api/workspace', {}),
        ('demo.example', '/api/workspace', {}),
        ('demo.example', '/api/workspace', {'X-Forwarded-Proto': 'https'}),
        ('demo.example', '/api/workspace', {'Forwarded': 'proto=https;host=demo.example'}),
    ]:
        status, headers, body = request(port, path, host, forwarding)
        assert status == 403 and json.loads(body)['code'] == 'PUBLIC_REQUEST_DENIED'
        assert 'Set-Cookie' not in headers
        assert json.loads(body)['etag'] is None
    checks.append('PASS: HTTP and untrusted scheme/host/forwarded requests disclose no workspace and create no session')
    logs = docker('logs', container)
    assert logs.count('kernelCommand=verify exitClass=VERIFIED') == 3
    checks.append('PASS: all three startup verifications ran under the container verification heap ceiling')
    (args.output / 'resources.json').write_text(json.dumps({
        'memoryBytes': args.memory_bytes, 'cpus': args.cpus,
        'webHeapMiB': args.web_heap_mb, 'verifyHeapMiB': args.verify_heap_mb,
        'observed': json.loads(docker('stats', '--no-stream', '--format', '{{json .}}', container)),
    }, indent=2) + '\n')
    docker('stop', '--time', '30', container)
    assert docker('inspect', '--format', '{{.State.ExitCode}}', container) == '143'
    checks.append('PASS: direct SIGTERM stops the disposable web process cleanly')
    (args.output / 'ledger.md').write_text('# UC-12 local container smoke check\n\nImage: `' + identity + '`\n\n'
        + '\n\n'.join(checks) + '\n\nLocal packaging PASS only. No public Railway, capacity, spend, queue or reuse qualification.\n'
        + 'Container removed; no volume or database was used. Local image retained for review.\n')
    print('Local container smoke PASS:', args.output)
finally:
    if started:
        (args.output / 'server.log').write_text(docker('logs', container))
        docker('rm', '--force', container)
    if not (args.output / 'ledger.md').exists():
        (args.output / 'failed-attempt.md').write_text('# Container smoke check failed\n\n' + '\n\n'.join(checks)
            + '\n\nSee server.log and the invoking command output. Owned container removed.\n')
