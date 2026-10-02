"""Report recorded builds and scenarios; dependency pins never imply support.

--archive writes evidence summaries into docs/validation. Logs, jars, screenshots
and temporary worlds remain ignored under dist and run-smoke.
"""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import hashlib
import json
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--archive', action='store_true')
parser.add_argument('--record-target', nargs=2, metavar=('MINECRAFT','LOADER'))
parser.add_argument('--started-at')
parser.add_argument('--finished-at')
parser.add_argument('--build-command')
parser.add_argument('--client-command')
parser.add_argument('--build-status', choices=('Pass','Fail'))
parser.add_argument('--launch-status', choices=('Pass','Fail or incomplete','Not run'))
args = parser.parse_args()
destination = root / 'dist'
destination.mkdir(exist_ok=True)
targets = json.loads((root / 'gradle/targets.json').read_text(encoding='utf-8'))['targets']
old_path = destination / 'matrix-report.json'
old_rows = json.loads(old_path.read_text(encoding='utf-8-sig')) if old_path.exists() else []
old = {(r['minecraft'], r['loader']): r for r in old_rows}
if args.record_target:
    key=tuple(args.record_target)
    assert any((t['minecraft'],t['loader'])==key for t in targets), f'Unknown target: {key}'
    assert all((args.started_at,args.finished_at,args.build_command,args.build_status,args.launch_status))
    old[key]=dict(startedAt=args.started_at,finishedAt=args.finished_at,
                  buildCommand=args.build_command,build=args.build_status,launch=args.launch_status)
    if args.client_command: old[key]['clientCommand']=args.client_command

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def relative(path):
    return path.relative_to(root).as_posix()

def successful_log(path):
    return path.exists() and 'BUILD SUCCESSFUL' in path.read_text(encoding='utf-8-sig', errors='replace')

rows = []
for target in targets:
    name = f"mc{target['minecraft']}-{target['loader']}"
    artifact = destination / target['artifact']
    build_log = destination / f'{name}-build.log'
    report = destination / f'{name}-scenario.json'
    client_log = destination / f'{name}-client.log'
    scenario = json.loads(report.read_text(encoding='utf-8-sig')) if report.exists() else {}
    earlier = old.get((target['minecraft'], target['loader']), {})
    built = successful_log(build_log) and artifact.exists() and earlier.get('build')!='Fail'
    passed = built and successful_log(client_log) and scenario.get('passed') is True and earlier.get('launch') not in ('Not run','Fail or incomplete')
    row = dict(minecraft=target['minecraft'], loader=target['loader'], java=target['java'],
               loaderVersion=target['neoForge'] if target['loader']=='neoforge' else '0.19.5',
               fabricApi=target['fabricApi'] if target['loader']=='fabric' else None,
               build='Pass' if built else ('Fail' if earlier.get('build')=='Fail' else 'Not verified'),
               launch='Pass' if passed else 'Not verified',
               gameplay='Pass (automated scenario)' if passed else 'Not verified', supported=False,
               signedChat='Pending: authenticated client and server required',
               fullAcceptance='Pending: see docs/acceptance.md')
    for field in ('startedAt', 'finishedAt', 'buildCommand', 'clientCommand'):
        if field in earlier: row[field] = earlier[field]
    entry = 'modern' if target['java']==25 else 'legacy'
    prefix = f'{entry}/gradlew -p {entry} -PmcVersion={target["minecraft"]} -Ploader={target["loader"]} --no-parallel'
    row.setdefault('buildCommand', prefix+f' :{target["loader"]}:build check')
    row.setdefault('clientCommand', prefix+f' :{target["loader"]}:runSmoke')
    for field, path in [('artifact', artifact), ('buildLog', build_log), ('clientLog', client_log), ('scenarioReport', report)]:
        if path.exists():
            row[field] = relative(path)
            row[field+'Sha256'] = digest(path)
    if artifact.exists(): row['sha256'] = digest(artifact)
    if report.exists():
        row['testedAt'] = datetime.fromtimestamp(report.stat().st_mtime, timezone.utc).isoformat()
        row['assertions'] = {k:v for k,v in scenario.items() if isinstance(v, bool) and k!='passed'}
        row['assertionCount'] = len(row['assertions'])
        row['scenarioNotes'] = {k:v for k,v in scenario.items() if not isinstance(v, bool)}
        screenshots = root / 'targets' / target['minecraft'] / target['loader'] / 'run-smoke/screenshots'
        row['screenshots'] = [dict(path=relative(p), sha256=digest(p)) for p in sorted(screenshots.glob('*.png'))]
    rows.append(row)
encoded = json.dumps(rows, ensure_ascii=False, indent=2)
(destination / 'matrix-report.json').write_text(encoded, encoding='utf-8')
(destination / 'SHA256SUMS').write_text(''.join(f'{row["sha256"]}  {Path(row["artifact"]).name}\n' for row in rows if 'sha256' in row), encoding='utf-8')

if args.archive:
    archive = root / 'docs/validation'
    archive.mkdir(exist_ok=True)
    (archive / 'matrix.json').write_text(encoded, encoding='utf-8')
    lines = ['# 30 目标实际执行结果', '', '由 `python tools/reports/write-matrix-report.py --archive` 从日志、JAR 与场景报告生成。Supported 全部为 false；验收限制见 [验收清单](../acceptance.md)。', '',
             '| Minecraft | Loader | Java | Build | Launch | Gameplay | 断言/记录 | JAR SHA-256 |',
             '|---|---|---|---|---|---|---|---|']
    for r in rows:
        lines.append(f'| {r["minecraft"]} | {r["loader"]} {r["loaderVersion"]} | {r["java"]} | {r["build"]} | {r["launch"]} | {r["gameplay"]} | {r.get("assertionCount",0)} | `{r.get("sha256","—")}` |')
    lines += ['', '每行实际命令、UTC 时间、断言名称、日志/截图校验值见 [matrix.json](matrix.json)。相同测试场景由两个加载器调用，测试专用源码不进入发布 JAR。', '']
    (archive / 'matrix.md').write_text('\n'.join(lines), encoding='utf-8')
    suites=[]
    for entry, build_directory in (('legacy','build'),('modern','build-modern')):
        for module in ('engine', 'client-common'):
            for result in sorted((root / module / build_directory / 'test-results/test').glob('TEST-*.xml')):
                suite=ET.parse(result).getroot()
                suites.append(dict(entry=entry,module=module, suite=suite.get('name'), tests=int(suite.get('tests')),
                                   failures=int(suite.get('failures')), errors=int(suite.get('errors')), skipped=int(suite.get('skipped')),
                                   timestamp=suite.get('timestamp'), seconds=float(suite.get('time')),
                                   reportSha256=digest(result),cases=[case.get('name') for case in suite.findall('testcase')]))
    (archive / 'core-tests.json').write_text(json.dumps(suites,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'{sum(r["build"]=="Pass" for r in rows)}/30 builds; {sum(r["launch"]=="Pass" for r in rows)}/30 client scenarios; support not inferred')
