"""Validate all targets' source ownership and synchronized dependency trust pins."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]

for entry in ('tools/build/build-matrix.ps1', 'tools/build/build-matrix.sh'):
    assert (root / entry).is_file(), f'Missing matrix build entry point: {entry}'
targets = json.loads((root / 'gradle/targets.json').read_text(encoding='utf-8'))['targets']
layout = json.loads((root / 'gradle/source-layout.json').read_text(encoding='utf-8'))
assert layout['schemaVersion'] == 1
versions = {target['minecraft'] for target in targets}
assert len({(target['minecraft'], target['loader']) for target in targets}) == len(targets)
override_owners = {}
for group in layout['overrides']:
    assert group['component'] in {'minecraft', 'fabric', 'neoforge'}
    assert set(group['minecraft']) <= versions
    path = (root / group['directory']).resolve()
    assert path.is_relative_to(root / 'adapters') and path.is_dir(), path
    assert list(path.rglob('*.java')), f'Empty override group: {path}'
    for version in group['minecraft']:
        for kind in ('main', 'smoke'):
            source = path / 'src' / kind / 'java'
            for file in source.rglob('*.java'):
                key = (group['component'], version, kind, file.relative_to(source))
                assert key not in override_owners, f'Conflicting shared overrides: {file}, {override_owners.get(key)}'
                override_owners[key] = file

for target in targets:
    family = target['apiFamily']
    assert family in {'legacy', 'mc26.1'}
    assert target['java'] == {'legacy': 21, 'mc26.1': 25}[family]
    for component in ('minecraft', target['loader']):
        directories = [root / 'adapters/common' / component, root / 'adapters/api/legacy' / component]
        if family == 'mc26.1':
            directories.append(root / 'adapters/api/mc26.1' / component)
        directories.extend(root / group['directory'] for group in layout['overrides']
                           if group['component'] == component and target['minecraft'] in group['minecraft'])
        directories.append(root / 'targets' / target['minecraft'] / component)
        for kind in ('main', 'smoke'):
            effective = {}
            for directory in directories:
                source = directory / 'src' / kind / 'java'
                for file in source.rglob('*.java'):
                    effective[file.relative_to(source)] = file
            classes = {}
            for relative, file in effective.items():
                text = file.read_text(encoding='utf-8')
                package = re.search(r'^package\s+([\w.]+);', text, re.MULTILINE)
                assert package, file
                name = package[1] + '.' + file.stem
                assert name not in classes, f'Duplicate effective class {name}: {file}, {classes.get(name)}'
                assert relative.as_posix() == package[1].replace('.', '/') + '/' + file.name, file
                classes[name] = file
            if component == 'minecraft':
                prefix = 'io.github.axiraa.cinematicchat.'
                required = ('minecraft.GuiCanvas', 'minecraft.CinematicScreen', 'minecraft.mixin.ChatComponentMixin') if kind == 'main' else ('smoke.ClientScenario', 'smoke.ScenarioPlatform')
                assert all(prefix + name in classes for name in required), (target, kind)
                if kind == 'main' and family == 'mc26.1':
                    for name in required:
                        assert classes[prefix + name].is_relative_to(root / 'adapters/api/mc26.1'), classes[prefix + name]

baselines = [root / entry / 'gradle/verification-metadata.xml' for entry in ('', 'legacy', 'modern')]
assert len({path.read_bytes() for path in baselines}) == 1, 'Dependency verification baselines have diverged'
namespace = {'v': 'https://schema.gradle.org/dependency-verification'}
tree = ET.parse(baselines[0])
reviewed = json.loads((root / 'docs/validation/dependency-metadata.json').read_text(encoding='utf-8'))
for artifact in reviewed['artifacts']:
    group, name, version = artifact['coordinate'].split(':')
    component = tree.find(f'v:components/v:component[@group="{group}"][@name="{name}"][@version="{version}"]', namespace)
    assert component is not None
    filename = artifact.get('file', f'{name}-{version}.module')
    checksum = component.find(f'v:artifact[@name="{filename}"]/v:sha256', namespace)
    assert checksum is not None and checksum.get('value') == artifact['sha256'], artifact['coordinate']
expected_trusts = {('loom', 'mappings', 'layered+hash.2198', 'mappings-layered+hash.2198.jar')}
for baseline in ('generated-game-checksums.json', 'generated-mod-checksums.json'):
    generated = json.loads((root / 'gradle' / baseline).read_text(encoding='utf-8'))
    assert generated['schemaVersion'] == 1 and generated['loom'] == '1.15.5'
    expected_versions = {t['minecraft'] for t in targets if t['loader'] == 'fabric'
                         and (baseline == 'generated-game-checksums.json' or t['apiFamily'] == 'legacy')}
    assert set(generated['targets']) == expected_versions, baseline
    for version, target in generated['targets'].items():
        assert target['artifacts'], version
        for artifact in target['artifacts']:
            coordinate = tuple(artifact[k] for k in ('group', 'name', 'version', 'file'))
            assert coordinate not in expected_trusts, coordinate
            expected_trusts.add(coordinate)
            assert artifact['entryCount'] > 0 and re.fullmatch('[0-9a-f]{64}', artifact['contentSha256'])
            expected_path = '/'.join((artifact['group'].replace('.', '/'), artifact['name'], artifact['version'], artifact['file']))
            assert artifact['path'] == expected_path
            if baseline == 'generated-mod-checksums.json':
                assert artifact['group'] == 'remapped.net.fabricmc.fabric-api'
                group, name, revision = artifact['inputCoordinate'].split(':')
                component = tree.find(f'v:components/v:component[@group="{group}"][@name="{name}"][@version="{revision}"]', namespace)
                assert component is not None
                checksum = component.find(f'v:artifact[@name="{artifact["inputFile"]}"]/v:sha256', namespace)
                assert checksum is not None
                assert artifact['inputSha256'] in {checksum.get('value'), *(x.get('value') for x in checksum)}
trusts = tree.findall('v:configuration/v:trusted-artifacts/v:trust', namespace)
assert all(t.get('regex', 'false') == 'false' for t in trusts), 'Generated-artifact trust must be exact'
assert {tuple(t.get(k) for k in ('group', 'name', 'version', 'file')) for t in trusts} == expected_trusts
assert len(trusts) == len(expected_trusts), 'Duplicate generated-artifact trust'
print(f'{len(targets)} targets: explicit source ownership and synchronized metadata pins verified')
