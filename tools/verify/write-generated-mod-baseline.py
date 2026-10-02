"""Review Loom-remapped Fabric API jars by content, ignoring ZIP timestamps.

Original inputs must match the existing official-download verification pins.
Run deliberately after rebuilding and reviewing dependencies; never in CI.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import os
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--gradle-home', type=Path, default=Path(os.environ.get('GRADLE_USER_HOME',Path.home()/'.gradle')))
args = parser.parse_args()
cache = args.gradle_home.resolve()/'caches/modules-2/files-2.1'
ns = {'v': 'https://schema.gradle.org/dependency-verification'}
components = {tuple(c.get(k) for k in ('group', 'name', 'version')): c
              for c in ET.parse(root / 'gradle/verification-metadata.xml').findall('v:components/v:component', ns)}

def content_pin(path):
    result = hashlib.sha256()
    with zipfile.ZipFile(path) as jar:
        entries = sorted((e for e in jar.infolist() if not e.is_dir()), key=lambda e: e.filename)
        assert len({e.filename for e in entries}) == len(entries), path
        for entry in entries:
            result.update(entry.filename.encode('utf-8') + b'\0')
            result.update(hashlib.sha256(jar.read(entry)).digest())
    return dict(contentSha256=result.hexdigest(), entryCount=len(entries))

records = {}
for target in json.loads((root / 'gradle/targets.json').read_text(encoding='utf-8'))['targets']:
    if target['loader'] != 'fabric' or target['apiFamily'] != 'legacy':
        continue
    version = target['minecraft']
    artifacts = []
    lock = root / f'gradle/dependency-locks/{version}-fabric-fabric.lockfile'
    for line in lock.read_text(encoding='utf-8').splitlines():
        if not line.startswith('remapped.net.fabricmc.fabric-api:'):
            continue
        group, name, revision = line.split('=', 1)[0].split(':')
        original_name = re.sub(r'-[0-9a-f]{8}$', '', name)
        assert original_name != name, name
        original_group = group.removeprefix('remapped.')
        original_file = f'{original_name}-{revision}.jar'
        candidates=list((cache/original_group/original_name/revision).glob('*/'+original_file))
        assert len(candidates)==1, f'Expected one reviewed Gradle artifact: {original_file}'
        original=candidates[0]
        original_sha = hashlib.sha256(original.read_bytes()).hexdigest()
        component = components[(original_group, original_name, revision)]
        checksum = component.find(f'v:artifact[@name="{original_file}"]/v:sha256', ns)
        assert checksum is not None
        allowed = {checksum.get('value'), *(x.get('value') for x in checksum)}
        assert original_sha in allowed, f'Unreviewed original input: {original}'
        relative = Path(group.replace('.', '/')) / name / revision / f'{name}-{revision}.jar'
        generated = root / 'legacy/.gradle/loom-cache/remapped_mods' / relative
        pin = content_pin(generated)
        shortcut = root / '.gradle/loom-cache/remapped_mods' / relative
        if shortcut.exists():
            assert content_pin(shortcut) == pin, f'Entry point content differs: {name}'
        artifacts.append(dict(group=group, name=name, version=revision, file=generated.name,
                              path=relative.as_posix(), inputCoordinate=f'{original_group}:{original_name}:{revision}',
                              inputFile=original_file, inputSha256=original_sha, **pin))
    assert artifacts, f'No remapped Fabric API artifacts for {version}'
    records[version] = dict(fabricApi=target['fabricApi'], artifacts=artifacts)

destination = root / 'gradle/generated-mod-checksums.json'
destination.write_text(json.dumps(dict(schemaVersion=1, loom='1.15.5',
    algorithm='SHA256 of sorted UTF8(name) + NUL + SHA256(entry bytes), excluding directory entries',
    targets=records), indent=2) + '\n', encoding='utf-8')
print(f'{len(records)} targets; {sum(len(r["artifacts"]) for r in records.values())} reviewed remapped mod content pins')
