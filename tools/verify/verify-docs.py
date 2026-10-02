"""Validate tracked Markdown's relative links, anchors and language navigation."""
from pathlib import Path
import json
import re
import subprocess
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[2]

def anchors(path):
    result=set()
    for heading in re.findall(r'^#{1,6}\s+(.+)$', path.read_text(encoding='utf8'), re.M):
        result.add(re.sub(r'[^\w\- ]','',heading.lower()).replace(' ','-'))
    return result

def main():
    tracked=subprocess.check_output(['git','ls-files','-z','--','*.md'],cwd=ROOT).decode('utf8').split('\0')
    # Include newly created documentation before the candidate commit as well.
    paths={ROOT/p for p in tracked if p and (ROOT/p).exists()}
    paths.update(ROOT.glob('README*.md'));paths.add(ROOT/'BUILDING.md');paths.update((ROOT/'docs').rglob('*.md'))
    checked=0
    for path in sorted(paths):
        source=path.read_text(encoding='utf8')
        for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)',source):
            target=target.split(' "',1)[0].strip('<>')
            if re.match(r'[a-z]+:',target) or target.startswith('//'):
                continue
            relative,_,fragment=target.partition('#')
            resolved=(path.parent/unquote(relative)).resolve() if relative else path
            assert resolved.is_relative_to(ROOT), f'Link escapes repository: {path} {target}'
            assert resolved.exists(), f'Broken link: {path.relative_to(ROOT)} -> {target}'
            if fragment and resolved.suffix=='.md':
                assert unquote(fragment) in anchors(resolved), f'Broken anchor: {path.name} {target}'
            checked+=1
    readmes=['README.md','README.zh-CN.md','README.ja-JP.md','README.ko-KR.md']
    versions=list(dict.fromkeys(t['minecraft'] for t in json.loads((ROOT/'gradle/targets.json').read_text(encoding='utf8'))['targets']))
    for name in readmes:
        source=(ROOT/name).read_text(encoding='utf8')
        assert all(other in source for other in readmes), f'Missing language navigation: {name}'
        rows=re.findall(r'^\|\s*(\d+\.\d+(?:\.\d+)?)\s*\|\s*([^|]+)\|\s*([^|]+)\|\s*$',source,re.M)
        assert [row[0] for row in rows]==versions, f'README version table differs from matrix: {name}'
    for language in ('en','zh-CN','ja-JP','ko-KR'):
        for name in ('chat-code','hud-editor','voice'):
            path=ROOT/f'docs/{language}/{name}.md'
            assert path.is_file(), (language,name)
            source=path.read_text(encoding='utf8')
            assert all(f'../{other}/{name}.md' in source for other in ('en','zh-CN','ja-JP','ko-KR')), f'Missing tutorial language navigation: {path}'
    print(f'Documentation verified: {len(paths)} files, {checked} local links')

if __name__=='__main__':
    main()
