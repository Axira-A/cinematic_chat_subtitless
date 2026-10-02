"""Reviewable content pins for Loom's generated, timestamped mapping ZIPs.

Run only during an intentional baseline update, after verifying official inputs.
Normal Gradle builds compare this baseline and never update it automatically.
"""
from pathlib import Path
import argparse, hashlib, json, zipfile

root=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser()
parser.add_argument('--loom-cache',type=Path,required=True)
parser.add_argument('--allow-incomplete',action='store_true',help='Preparation only; missing targets still fail Gradle verification')
args=parser.parse_args()
versions=sorted({t['minecraft'] for t in json.loads((root/'gradle/targets.json').read_text(encoding='utf-8'))['targets'] if t['java']==21})
records={};missing=[]
for version in versions:
    directory=args.loom_cache.resolve()/version
    generated=directory/'layered/loom.mappings-layered+hash.2198.jar'
    if not generated.exists():missing.append(version);continue
    manifest=json.loads((directory/'mojang_minecraft_info.json').read_text(encoding='utf-8'))
    assert manifest['id']==version
    sources={}
    for kind in ('client','server'):
        path=directory/'layered/working_dir/layered_hash_2198/mojang'/f'{kind}.txt'
        digest=hashlib.sha1(path.read_bytes()).hexdigest()
        assert digest==manifest['downloads'][kind+'_mappings']['sha1'],f'Mojang input mismatch: {version} {kind}'
        sources[kind+'Sha1']=digest
    with zipfile.ZipFile(generated) as jar:
        assert jar.testzip() is None
        assert [e.filename for e in jar.infolist() if not e.is_dir()]==['mappings/mappings.tiny']
        data=jar.read('mappings/mappings.tiny')
        assert data.startswith(b'tiny\t2\t0\tintermediary\tnamed')
    records[version]=dict(tinySha256=hashlib.sha256(data).hexdigest(),**sources)
assert not missing or args.allow_incomplete,f'Generate mappings before updating: {missing}'
(root/'gradle/generated-mappings-checksums.json').write_bytes((json.dumps(dict(schemaVersion=1,loom='1.15.5',mappingsCoordinate='loom:mappings:layered+hash.2198',targets=records),indent=2)+'\n').encode('utf-8'))
print(f'{len(records)}/12 mapping content pins; missing: {missing}')
