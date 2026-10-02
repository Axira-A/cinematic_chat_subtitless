# Cinematic Chat

[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja-JP.md) · [한국어](README.ko-KR.md)

Client-side cinematic subtitles for Minecraft chat, with a bounded CCML script engine, a visual HUD editor, and optional local pseudo voice. Vanilla chat history remains available when you open chat. The server does not need this mod.

## Features

- Styled subtitles, grapheme-aware reveal, word reveal, timing controls, and local text effects.
- Configurable source filters, queue priorities, animations, reading time, and accessibility options.
- Visual placement, nine anchors, width and scale handles, snapping, and six built-in HUD presets.
- Optional voice profiles with local OGG Vorbis or WAV PCM clips and five playback modes.
- English, Simplified Chinese, Japanese, and Korean interface resources.

## Versions

| Minecraft | Fabric | NeoForge |
|---|---|---|
| 1.21 | Experimental | Experimental |
| 1.21.1 | Experimental | Experimental |
| 1.21.2 | Experimental | Experimental |
| 1.21.3 | Experimental | Experimental |
| 1.21.4 | Experimental | Experimental |
| 1.21.5 | Experimental | Experimental |
| 1.21.6 | Experimental | Experimental |
| 1.21.7 | Experimental | Experimental |
| 1.21.8 | Experimental | Experimental |
| 1.21.9 | Experimental | Experimental |
| 1.21.10 | Experimental | Experimental |
| 1.21.11 | Experimental | Experimental |
| 26.1 | Experimental | Experimental |
| 26.1.1 | Experimental | Experimental |
| 26.1.2 | Experimental | Experimental |

These are experimental build targets. Choose the JAR matching both your exact Minecraft version and loader. Authenticated Signed Chat testing is still pending; offline development scenarios do not establish that result.

## Installation

This repository currently has no Release. If you have repository access, download the matching JAR from a successful [Actions run](https://github.com/Axira-A/cinematic_chat_subtitless/actions/workflows/matrix.yml), or [build from source](BUILDING.md). Install the corresponding Fabric Loader and Fabric API, or NeoForge, and place the mod JAR in your client's `mods/` directory. Shared modules are bundled in the JAR.

## Quick start

1. Enter a world and run `/cchat config` to adjust subtitles.
2. Run `/cchat preview` to display Steve and Alex's welcome messages.
3. Run `/cchat hud` to position the subtitles; apply the result.

`/cinematicchat` is an alias for `/cchat`. Other client commands are `clear`, `reload`, `debug`, `parser <text>`, and `voice`. The config and HUD key bindings can be changed in Minecraft Controls. Opening chat pauses/hides subtitles by default; F1 hides them and mutes pseudo voice.

## CCML

CCML controls local subtitle presentation. It does not edit the original chat message or vanilla history. SAFE is the default trust mode.

```text
[speed=30]Hello, [color=#67BDE5]welcome to Cinematic Chat.[/color][hold=3]
```

Learn the tags, timing, escaping, and trust modes in the [CCML guide](docs/en/chat-code.md).

## HUD

Drag the subtitle region, resize its width at the edges, and adjust scale at the corner. Shift locks movement to an axis; Alt temporarily bypasses snapping. Apply commits a directly opened editor; when opened from settings, save the parent settings page too. See the [HUD guide](docs/en/hud-editor.md).

## Pseudo voice

Pseudo voice is disabled by default. It plays short local sounds as text appears; it is not speech synthesis. Create a profile, bind Steve or Alex, choose files under `config/cinematic_chat/audio/`, test, and save. See the [voice guide](docs/en/voice.md).

## Languages

Select a language in Minecraft's language menu. New previews use the current language. Real chat text, custom names, profile IDs, and filenames retain their original values.

## Build

Use JDK 21 for 1.21.x and JDK 25 for 26.1.x; shared Java modules target Java 21. [BUILDING.md](BUILDING.md) describes Wrappers, individual targets, the full matrix, client scenarios, and dependency verification.

## License

[MIT](LICENSE). The bundled original test sounds and their regeneration instructions are documented in [SOURCES.txt](adapters/common/minecraft/src/main/resources/assets/cinematic_chat/audio/SOURCES.txt).
