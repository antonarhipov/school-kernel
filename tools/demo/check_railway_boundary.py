#!/usr/bin/env python3
"""Read-only preflight for the explicit isolated Railway HTTP ingress trust option."""
import argparse
import datetime
import json
import subprocess
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--railway', default='railway')
parser.add_argument('--project', required=True)
parser.add_argument('--environment', required=True)
parser.add_argument('--service', required=True)
parser.add_argument('--domain', required=True)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()

def query(command):
    result = subprocess.run([args.railway, *command], capture_output=True, text=True, check=True, timeout=45)
    return json.loads(result.stdout)

project = query(['status', '--project', args.project, '--environment', args.environment, '--json'])
assert project['id'] == args.project
assert [s['node']['id'] for s in project['services']['edges']] == [args.service], 'A second project service invalidates isolated ingress'
environment = next(e['node'] for e in project['environments']['edges'] if e['node']['id'] == args.environment)
instances = [e['node'] for e in environment['serviceInstances']['edges']]
assert len(instances) == 1 and instances[0]['serviceId'] == args.service, 'Environment must contain only the demo service'
instance = instances[0]
assert instance['numReplicas'] == 1, 'In-memory demo requires one replica'
assert instance['domains']['customDomains'] == [], 'Requalify additional public domains'
assert [d['domain'] for d in instance['domains']['serviceDomains']] == [args.domain]
assert args.domain.endswith('.up.railway.app')
tcp = query(['tcp-proxy', 'list', '--project', args.project, '--environment', args.environment, '--service', args.service, '--json'])
assert tcp['proxies'] == [], 'Raw TCP ingress bypasses edge sanitization'
deployment = instance['latestDeployment']
evidence = {'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'project': args.project,
            'environment': args.environment, 'service': args.service, 'domain': args.domain,
            'projectServiceCount': 1, 'environmentServiceCount': 1, 'replicas': 1, 'tcpProxies': [],
            'deployment': deployment['id'], 'status': deployment['status'],
            'imageDigest': deployment.get('meta', {}).get('imageDigest'),
            'verdict': 'PASS: isolated project/environment, one service/replica, generated domain, no TCP proxy'}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(evidence, indent=2) + '\n')
print(evidence['verdict'])
