"""Intentional review of Loom-generated game jars, independent of ZIP dates.

Checks official client/server downloads and every generated entry's bytes. This
does not download files or change them; normal builds never update these pins.
"""
from pathlib import Path
import argparse, hashlib, json, zipfile, xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser()
parser.add_argument('--loom-cache',required=True,type=Path)
args=parser.parse_args()
cache=args.loom_cache.resolve()
targets=json.loads((root/'gradle/targets.json').read_text(encoding='utf-8'))['targets']
versions={t['minecraft']:t for t in targets if t['loader']=='fabric'}
ns={'g':'https://schema.gradle.org/dependency-verification'}
records={}
for version,target in versions.items():
    directory=cache/version
    manifest=json.loads((directory/'mojang_minecraft_info.json').read_text(encoding='utf-8'))
    assert manifest['id']==version
    inputs={}
    for side in ('client','server'):
        digest=hashlib.sha1((directory/f'minecraft-{side}.jar').read_bytes()).hexdigest()
        assert digest==manifest['downloads'][side]['sha1'],f'Mojang game input mismatch: {version} {side}'
        inputs[side+'Sha1']=digest
    records[version]=dict(**inputs,artifacts=[])

def content_pin(path):
    result=hashlib.sha256()
    with zipfile.ZipFile(path) as jar:
        entries=sorted((e for e in jar.infolist() if not e.is_dir()),key=lambda e:e.filename)
        assert len({e.filename for e in entries})==len(entries),'Duplicate ZIP entry'
        for entry in entries:
            result.update(entry.filename.encode('utf-8')+b'\0')
            result.update(hashlib.sha256(jar.read(entry)).digest())
    return dict(contentSha256=result.hexdigest(),entryCount=len(entries))

for component in ET.parse(root/'gradle/verification-metadata.xml').findall('g:components/g:component',ns):
    if component.get('group')!='net.minecraft':continue
    name,coordinate_version=component.get('name'),component.get('version')
    version=coordinate_version.split('-loom.')[0]
    assert version in versions and (name in ('minecraft-merged','minecraft-merged-deobf') or name.startswith('minecraft-merged-'))
    relative=Path('net/minecraft')/name/coordinate_version/f'{name}-{coordinate_version}.jar'
    scope='global' if name in ('minecraft-merged','minecraft-merged-deobf') else 'project'
    entry='modern' if versions[version]['java']==25 else 'legacy'
    directory=cache/'minecraftMaven' if scope=='global' else root/entry/'.gradle/loom-cache/minecraftMaven'
    path=directory/relative
    assert path.exists(),f'Generate reviewed game artifact first: {path}'
    pin=content_pin(path)
    # The root shortcut and independent legacy entry must yield identical bytes.
    shortcut=root/'.gradle/loom-cache/minecraftMaven'/relative
    if scope=='project' and shortcut.exists():assert content_pin(shortcut)==pin,f'Entry point content differs: {version}'
    records[version]['artifacts'].append(dict(group='net.minecraft',name=name,version=coordinate_version,
        file=path.name,scope=scope,path=relative.as_posix(),**pin))
assert all(len(r['artifacts'])==2 for r in records.values()),'Expected base + processed jar for every target'
(root/'gradle/generated-game-checksums.json').write_bytes((json.dumps(dict(schemaVersion=1,loom='1.15.5',
    algorithm='SHA256 of sorted UTF8(name) + NUL + SHA256(entry bytes), excluding directory entries',targets=records),indent=2)+'\n').encode('utf-8'))
print(f'{len(records)}/15 game input pins; {sum(len(r["artifacts"]) for r in records.values())} generated content pins')
