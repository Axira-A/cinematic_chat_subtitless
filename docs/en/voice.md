# Local pseudo voice

[English](../en/voice.md) · [简体中文](../zh-CN/voice.md) · [日本語](../ja-JP/voice.md) · [한국어](../ko-KR/voice.md)

This feature plays short clips, not generated speech. It is off by default, with player, system and NPC fallbacks set to None.

1. Run `/cchat voice` and enable voice. Use New Profile and choose a unique ID using `a-z`, `0-9`, `_`, `-`, or `.` (1–64 characters).
2. In Speaker Matching, add `Steve` or `Alex` under names or aliases, or bind a player's UUID. Matching defaults to UUID, profile name, display name, alias, then fallback. Formatting removal, case and surrounding-space handling are adjustable; the original name is preserved.
3. Put short OGG Vorbis or WAV PCM clips under `config/cinematic_chat/audio/`. Use Audio Files → Add Audio. The picker lists files within that directory; absolute paths, traversal and detectable symlink escapes are rejected. Install Original Test Tone adds the bundled original WAV and OGG files.
4. Adjust playback mode, volume, pitch, randomness, minimum interval and CJK grouping. Preview Sound tests a start sound; Test Talking tests reveal events using Steve and the localized welcome text.
5. Save the profile, then save its parent voice page. When entered from config, also save the parent config page. Cancel/Esc discards that page's working copy.

Modes are Off, On Subtitle Start, Per Word, Per Reveal Unit and Loop While Revealing. A new profile uses Per Reveal Unit, a 65 ms minimum interval and two CJK graphemes per sound; whitespace and punctuation are skipped by default. A shuffle bag avoids immediate repetition where possible. Instant reveal coalesces per-unit/per-word triggering to at most one sound.

Use `/cchat voice list`, `/cchat voice test <voiceId>`, `/cchat voice assign Steve <voiceId>` and `/cchat voice reload`. Assignment saves the binding; Reload Audio retries missing or damaged files after replacement. `/cchat reload` reloads settings as well. Profiles are in `config/cinematic_chat/voices.json`; HUD presets and general settings are separate files. Loading and decoding happen asynchronously: a clip that is not ready is skipped without delaying the subtitle. Clear, replacement, chat screen, disable and world changes stop relevant playback.

[README](../../README.md)
