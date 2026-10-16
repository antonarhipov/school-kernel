#!/usr/bin/env python3
"""Read Linux cgroup v2 usage (directly or through docker exec). Never starts or changes a workload.

Run concurrently with explicit qualification traffic. Provider service graphs/spend remain separate evidence.
"""
import argparse
import json
import subprocess
import time
from pathlib import Path


def read_values(read):
    result = {}
    for name in ['memory.current', 'memory.peak', 'memory.max', 'memory.events', 'cpu.stat', 'cpu.max', 'pids.current']:
        value = read(name).strip()
        result[name] = {line.split()[0]: int(line.split()[1]) for line in value.splitlines()} if name in ['memory.events', 'cpu.stat'] else int(value) if value.isdigit() else value
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--container', help='Existing disposable local Docker container; performs read-only exec cat')
    parser.add_argument('--cgroup', type=Path, default=Path('/sys/fs/cgroup'))
    parser.add_argument('--seconds', type=float, default=300)
    parser.add_argument('--interval', type=float, default=1)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.seconds <= 0 or args.interval < 0.1:
        parser.error('Positive duration and interval >= 0.1 seconds required')
    if args.container and (args.container.startswith('-') or not all(c.isalnum() or c in '_.-' for c in args.container)):
        parser.error('Use an exact local container name or ID')
    args.output.mkdir(parents=True, exist_ok=False)
    def read(name):
        path = args.cgroup / name
        return subprocess.check_output(['docker', 'exec', args.container, 'cat', str(path)], text=True) if args.container else path.read_text()
    deadline = time.monotonic() + args.seconds
    first, last, maximum, samples = None, None, 0, 0
    try:
        with (args.output / 'cgroup.jsonl').open('w') as log:
            while True:
                last = read_values(read)
                if first is None:
                    first = last
                samples += 1
                maximum = max(maximum, last['memory.current'])
                log.write(json.dumps({'utcEpochSeconds': time.time(), **last}) + '\n')
                log.flush()
                if time.monotonic() >= deadline:
                    break
                time.sleep(min(args.interval, max(0, deadline-time.monotonic())))
    finally:
        if last:
            summary = {'samples': samples, 'sampledMaximumBytes': maximum, 'cgroupPeakBytes': last['memory.peak'],
                       'memoryLimitBytes': last['memory.max'],
                       'memoryEventDeltas': {key: value-first['memory.events'].get(key, 0) for key, value in last['memory.events'].items()},
                       'capacityQualified': False,
                       'note': 'Peak includes cgroup lifetime; sampled maximum may miss transient peaks. Attach exact workload/cache/workspace/process evidence and provider metrics; this reader does not generate load or qualify launch.'}
            (args.output / 'summary.json').write_text(json.dumps(summary, indent=2)+'\n')


if __name__ == '__main__':
    main()
