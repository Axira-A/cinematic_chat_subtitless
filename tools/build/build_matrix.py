"""Build the tracked target matrix. Results are per-run ignored outputs."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]

def run(command, log):
    with log.open('w', encoding='utf-8') as output:
        with subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding='utf-8', errors='replace') as process:
            for line in process.stdout:
                output.write(line)
                print(line, end='', flush=True)
            return process.wait()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', nargs='+')
    parser.add_argument('--loader', nargs='+', choices=('fabric', 'neoforge'), default=['neoforge', 'fabric'])
    parser.add_argument('--smoke', action='store_true', help='Run the temporary-world client scenarios')
    parser.add_argument('--list', action='store_true', help='List selected targets without building')
    args = parser.parse_args()
    targets = json.loads((ROOT / 'gradle/targets.json').read_text(encoding='utf-8'))['targets']
    if args.minecraft and set(args.minecraft) - {t['minecraft'] for t in targets}:
        parser.error('Unknown Minecraft version; see gradle/targets.json')
    targets = [t for t in targets if t['loader'] in args.loader and (not args.minecraft or t['minecraft'] in args.minecraft)]
    if not targets:
        parser.error('No build targets selected')
    if args.list:
        for target in targets:
            print(target['minecraft'], target['loader'], 'Java', target['java'])
        return 0
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex[:6]
    destination = ROOT / 'dist/matrix' / stamp
    destination.mkdir(parents=True)
    for check in ('verify-wrapper.py','verify-source-layout.py','verify-localization.py','verify-docs.py'):
        if run([sys.executable,str(ROOT/'tools/verify'/check)],destination/(check+'.log')) != 0:
            return 1
    rows = []
    for target in targets:
        entry = 'modern' if target['apiFamily'] == 'mc26.1' else 'legacy'
        loader = target['loader']
        name = f"mc{target['minecraft']}-{loader}"
        command = [str(Path(entry) / ('gradlew.bat' if os.name == 'nt' else 'gradlew'))]
        if os.name != 'nt':
            command.insert(0, 'bash')
        command += ['-p', entry, f"-PmcVersion={target['minecraft']}", f'-Ploader={loader}', '--no-parallel']
        row = dict(minecraft=target['minecraft'], loader=loader, startedAt=datetime.now(timezone.utc).isoformat(),
                   build='Fail', launch='Not run')
        print(f'Building {name}', flush=True)
        build_command = command + [f':{loader}:build', f':{loader}:smokeClasses', 'check']
        row['buildCommand'] = build_command
        artifact = ROOT / 'targets' / target['minecraft'] / loader / 'build/libs' / target['artifact']
        artifact.unlink(missing_ok=True)
        row['buildExitCode'] = run(build_command, destination / f'{name}-build.log')
        if row['buildExitCode'] == 0 and artifact.is_file():
            row['build'] = 'Pass'
            shutil.copy2(artifact, destination)
            row['artifactSha256'] = hashlib.sha256(artifact.read_bytes()).hexdigest()
            if args.smoke:
                client_command = command + [f':{loader}:runSmoke']
                row['clientCommand'] = client_command
                row['clientExitCode'] = run(client_command, destination / f'{name}-client.log')
                report = ROOT / 'targets' / target['minecraft'] / loader / 'run-smoke/smoke-report.json'
                row['launch'] = 'Fail'
                if row['clientExitCode'] == 0 and report.is_file():
                    scenario = json.loads(report.read_text(encoding='utf-8'))
                    if scenario.get('passed') is True:
                        row['launch'] = 'Pass'
                    shutil.copy2(report, destination / f'{name}-scenario.json')
        row['finishedAt'] = datetime.now(timezone.utc).isoformat()
        rows.append(row)
        (destination / 'results.json').write_text(json.dumps(rows, indent=2) + '\n', encoding='utf-8')
        print(f"{name}: build={row['build']} launch={row['launch']}", flush=True)
    passed = sum(r['build'] == 'Pass' and (not args.smoke or r['launch'] == 'Pass') for r in rows)
    print(f'{passed}/{len(targets)} targets passed. Outputs: {destination.relative_to(ROOT)}')
    return 0 if passed == len(targets) else 1

if __name__ == '__main__':
    raise SystemExit(main())
