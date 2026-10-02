"""Check all four translations and the stable options shown by native screens."""
from pathlib import Path
from collections import Counter
import json
import re

ROOT = Path(__file__).resolve().parents[2]
DIRECTORY = ROOT / 'adapters/common/minecraft/src/main/resources/assets/cinematic_chat/lang'

def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, f'Duplicate translation key: {key}'
        result[key] = value
    return result

def placeholders(value):
    # Minecraft's translatable Component supports %s and indexed %1$s arguments.
    result = Counter()
    implicit = 0
    pattern = re.compile(r'%%|%(?:(\d+)\$)?s')
    assert '%' not in pattern.sub('', value), f'Invalid translation placeholder: {value}'
    for match in pattern.finditer(value):
        if match[0] == '%%':
            continue
        if match[1]:
            index = int(match[1])
        else:
            implicit += 1
            index = implicit
        result[index] += 1
    return result

def enum_values(path, name):
    source = (ROOT / path).read_text(encoding='utf8')
    body = re.search(r'enum\s+' + name + r'\s*\{([^;}]+)', source).group(1)
    return re.findall(r'(?:^|,)\s*([A-Z][A-Z_]+)\b', body)

def main():
    languages = {language: json.loads((DIRECTORY / (language+'.json')).read_text(encoding='utf8'),
                                      object_pairs_hook=unique_pairs)
                 for language in ('en_us', 'zh_cn', 'ja_jp', 'ko_kr')}
    baseline = languages['en_us']
    for language, values in languages.items():
        assert values.keys() == baseline.keys(), f'Key mismatch: {language}'
        for key, value in values.items():
            assert isinstance(value, str) and value.strip(), f'Empty translation: {language} {key}'
            assert placeholders(value) == placeholders(baseline[key]), f'Placeholder mismatch: {language} {key}'
    sources = {
        'anchor': ('client-common/src/main/java/io/github/axiraa/cinematicchat/client/HudSettings.java', 'Anchor'),
        'reveal': ('engine/src/main/java/io/github/axiraa/cinematicchat/engine/RevealMode.java', 'RevealMode'),
        'alignment': ('engine/src/main/java/io/github/axiraa/cinematicchat/engine/SubtitleLayoutEngine.java', 'Alignment'),
        'animation': ('engine/src/main/java/io/github/axiraa/cinematicchat/engine/Animation.java', 'Kind'),
        'easing': ('engine/src/main/java/io/github/axiraa/cinematicchat/engine/Animation.java', 'Easing'),
        'trust': ('client-common/src/main/java/io/github/axiraa/cinematicchat/client/CinematicConfig.java', 'TrustMode'),
        'priority': ('engine/src/main/java/io/github/axiraa/cinematicchat/engine/SubtitleQueue.java', 'Priority'),
    }
    for family, (path, name) in sources.items():
        values = enum_values(path, name)
        assert values, f'Missing dynamic option family: {family}'
        for value in values:
            assert f'cinematic_chat.option.{family}.{value.lower()}' in baseline, (family, value)
    for value in enum_values('minecraft-api/src/main/java/io/github/axiraa/cinematicchat/api/ChatSource.java', 'ChatSource'):
        for suffix in ('', '.priority'):
            assert 'cinematic_chat.source.'+value.lower()+suffix in baseline, value
    for value in enum_values('client-common/src/main/java/io/github/axiraa/cinematicchat/client/VoiceProfile.java', 'PlayMode'):
        assert 'cinematic_chat.voice.mode.'+value.lower() in baseline, value
    for value in ('uuid','profile','display','alias'):
        assert 'cinematic_chat.voice.match.'+value in baseline, value
    for value in ('cinematic','minimal','rpg','adventure','accessible','vanilla_like'):
        assert 'cinematic_chat.preset.'+value in baseline, value
    for directory in (ROOT/'adapters',):
        for path in directory.rglob('*.java'):
            if 'build' in path.parts or 'build-modern' in path.parts:
                continue
            for key in re.findall(r'Component\.translatable\("((?:cinematic_chat|key\.cinematic_chat|key\.categories\.cinematic_chat|key\.category\.cinematic_chat)[\w.]*)"\s*[,)]', path.read_text(encoding='utf8')):
                assert key in baseline, f'Untranslated key: {path.relative_to(ROOT)} {key}'
    print(f'Four languages verified: {len(baseline)} keys each; dynamic options and placeholders match')

if __name__ == '__main__':
    main()
