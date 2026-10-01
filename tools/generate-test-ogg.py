"""Encode original synthesized tones; this tool is optional and never a runtime dependency.

Install the encoder into tools/.cache/audio-encoder with soundfile==0.13.1.
No recordings, game assets or third-party samples are used.
"""
from pathlib import Path
import math
import sys

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / 'tools/.cache/audio-encoder'))
import soundfile as sf
import numpy as np

destination = root / 'targets/1.21.1/minecraft/src/main/resources/assets/cinematic_chat/audio'
destination.mkdir(parents=True, exist_ok=True)
rate, count = 22050, 2205
for index, frequency in enumerate((440, 660, 880), 1):
    samples = np.array([
        0.18 * math.sin(math.pi * i / count) * math.exp(-22 * i / rate)
        * (math.sin(2 * math.pi * frequency * i / rate)
           + 0.25 * math.sin(4 * math.pi * frequency * i / rate))
        for i in range(count)
    ])
    path = destination / f'test-blip-{index}.ogg'
    sf.write(path, samples, rate, format='OGG', subtype='VORBIS')
    info = sf.info(path)
    assert info.frames == count and info.channels == 1 and info.samplerate == rate
    assert path.read_bytes().startswith(b'OggS')
    print(f'{path.name}: {info.frames} frames, {path.stat().st_size} bytes')
