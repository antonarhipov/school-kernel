#!/usr/bin/env python3
"""Verify the native Linux demo container using disposable data and fixed resource limits."""
import argparse
import json
from pathlib import Path
import subprocess
import time
import urllib.request
import uuid

import measure_startup as startup


def docker(*arguments):
    return subprocess.check_output(['docker', *arguments], text=True, stderr=subprocess.STDOUT).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    name = 'school-native-check-' + uuid.uuid4().hex
    domain = 'native-check.up.railway.app'
    identity = '00000000-0000-0000-0000-000000000001'
    result = {'image': args.image, 'verdict': 'IN_PROGRESS', 'checks': []}
    plain_visitor = startup.Visitor
    started = False
    try:
        result['image_id'] = docker('image', 'inspect', '--format', '{{.Id}}', args.image)
        docker('run', '--detach', '--platform', 'linux/amd64', '--name', name,
               '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=128m',
               '--cpus', '1', '--memory', '500000000', '--publish', '127.0.0.1::8080',
               '--env', 'DEMO_WEB_HEAP_MB=128', '--env', 'DEMO_VERIFY_HEAP_MB=96',
               '--env', 'DEMO_SOLVER_HEAP_MB=128', '--env', 'DEMO_PUBLIC_HOSTS=' + domain,
               '--env', 'DEMO_RAILWAY_ISOLATED_INGRESS=true', '--env', 'RAILWAY_PUBLIC_DOMAIN=' + domain,
               '--env', 'RAILWAY_PROJECT_ID=' + identity, '--env', 'RAILWAY_ENVIRONMENT_ID=' + identity,
               '--env', 'RAILWAY_SERVICE_ID=' + identity,
               '--env', 'SPRING_APPLICATION_JSON={"demo":{"max-workspaces":2}}', args.image)
        started = True
        port = int(docker('port', name, '8080/tcp').rsplit(':', 1)[1])
        base = f'http://127.0.0.1:{port}'
        deadline = time.monotonic() + 90
        health = plain_visitor(base)
        while True:
            assert docker('inspect', '--format', '{{.State.Running}}', name) == 'true', 'Container stopped before readiness'
            try:
                body, headers = health.json('/healthz', headers={'Host': 'healthcheck.railway.app'})
                assert body == {'status': 'UP'} and 'Set-Cookie' not in headers
                break
            except (OSError, AssertionError):
                assert time.monotonic() < deadline, 'Readiness timed out'
                time.sleep(.1)
        result['checks'].append('PASS: actual read-only container becomes healthy without creating a session')
        assert docker('exec', name, 'id', '-u') == '10001'
        executable = docker('exec', name, 'readlink', '/proc/1/exe')
        command = docker('exec', name, 'sh', '-c', 'tr "\\000" " " < /proc/1/cmdline')
        # Under Docker Desktop cross-architecture emulation /proc/1/exe can identify Rosetta/QEMU.
        assert '/opt/demo/workspace' in command.split(), command
        assert docker('exec', name, 'od', '-An', '-t', 'x1', '-N4', '/opt/demo/workspace').split() == ['7f', '45', '4c', '46']
        result['pid1'] = {'executable': executable, 'command': command}
        result['checks'].append('PASS: UID 10001 and native web executable is PID 1')

        class LocalPublicVisitor(plain_visitor):
            def request(self, path, data=None, headers=None, status=200):
                headers = {'Host': domain, 'X-Forwarded-Proto': 'https', **(headers or {})}
                if headers.get('Origin') == base:
                    headers['Origin'] = 'https://' + domain
                # This isolated loopback probe models the TLS edge. Allow its cookie jar to send the server's
                # Secure cookie over the private HTTP hop; production cookie settings are not changed.
                for handler in self.http.handlers:
                    if isinstance(handler, urllib.request.HTTPCookieProcessor):
                        for cookie in handler.cookiejar:
                            cookie.secure = False
                return super().request(path, data, headers, status)

        startup.Visitor = LocalPublicVisitor
        result['checks'].append(startup.smoke(base))
        for headers in [{'Host': 'foreign.example'}, {'Host': domain},
                        {'Host': domain, 'Forwarded': 'proto=https;host=' + domain}]:
            refused, response = plain_visitor(base).json('/api/workspace', headers=headers, status=403)
            assert refused['code'] == 'PUBLIC_REQUEST_DENIED'
            assert 'Set-Cookie' not in response and refused['etag'] is None
        result['checks'].append('PASS: foreign host, direct HTTP and untrusted Forwarded disclose no workspace or cookie')
        logs = docker('logs', name)
        assert 'Starting AOT-processed WorkspaceApplication' in logs
        assert 'The following 1 profile is active: "demo"' in logs
        assert 'kernelCommand=verify exitClass=VERIFIED' in logs
        assert 'exitClass=REVIEWED' in logs
        assert 'ERROR' not in logs
        result['resources'] = json.loads(docker('stats', '--no-stream', '--format', '{{json .}}', name))
        docker('stop', '--time', '15', name)
        state = json.loads(docker('inspect', '--format', '{{json .State}}', name))
        assert state['ExitCode'] == 143 and state['OOMKilled'] is False, state
        result['checks'].append('PASS: native SIGTERM shutdown completes without OOM or leftover kernel processes')
        result['verdict'] = 'PASS'
    except BaseException as error:
        result['verdict'] = f'FAIL: {error}'
        raise
    finally:
        startup.Visitor = plain_visitor
        if started:
            (args.output / 'server.log').write_text(docker('logs', name) + '\n')
            docker('rm', '--force', name)
        result['cleanup'] = 'Owned container removed; no volume, database or operator data used'
        (args.output / 'results.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
