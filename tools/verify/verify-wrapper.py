"""Verify all three checked-in wrappers against the official pinned SHA-256 values."""
from pathlib import Path
import hashlib
import json

root = Path(__file__).resolve().parents[2]
expected = json.loads((root / 'gradle/wrapper-checksums.json').read_text(encoding='utf-8'))
for entry in ('', 'legacy', 'modern'):
    baseline = expected['modern' if entry == 'modern' else 'legacy']
    folder = root / entry / 'gradle/wrapper'
    actual = hashlib.sha256((folder / 'gradle-wrapper.jar').read_bytes()).hexdigest()
    assert actual == baseline['wrapperJarSha256'], f'{entry or "root"}: wrapper JAR checksum differs'
    properties = (folder / 'gradle-wrapper.properties').read_text(encoding='utf-8')
    assert f"gradle-{baseline['version']}-bin.zip" in properties
    assert f"distributionSha256Sum={baseline['distributionSha256']}" in properties
    print(f'{entry or "root"}: Gradle {baseline["version"]}, wrapper and distribution pins verified')
