# CCML chat code

[English](../en/chat-code.md) · [简体中文](../zh-CN/chat-code.md) · [日本語](../ja-JP/chat-code.md) · [한국어](../ko-KR/chat-code.md)

CCML is read from accepted player/system chat, including `/say`; source switches and the trust mode decide whether it is interpreted. `/cchat parser <text>` inspects a script locally; `/cchat preview` generates the current language's welcome message. These client commands do not send scripts to other players.

```text
[speed=30]Hello, [color=#67BDE5][bold]welcome to Cinematic Chat.[/bold][/color][hold=3]
```

| Tag | Meaning |
|---|---|
| `[pause=0.5]` | Wait until earlier text is revealed, then pause for seconds. |
| `[wait]` | Wait for earlier finite entry and reveal animations; ongoing shake/wave does not block. |
| `[hold=3]` | Minimum retention after the final text appears; multiple holds use the largest value. |
| `[speed=30]`, `[speed=default]` | Reveal units per second, or configured speed. |
| `[reveal=instant]` | Modes: `instant`, `typewriter`, `smooth`, `word`. |
| `[clear]`, `[replace]` | Clear this entry's displayed text; following text replaces it. |
| `[erase=2]` | Delete up to two trailing graphemes of this entry. |
| `[fadein=0.2]`, `[fadeout=0.3]` | Entry/exit fade duration, clamped to 0–3 seconds. |
| `[br]` | Line break; `\n` also produces a line break. |
| `[color=#67BDE5]…[/color]` | RGB color. |
| `[scale=1.2]…[/scale]`, `[alpha=0.8]…[/alpha]` | Local scale and opacity. |
| `[bold]…[/bold]`, `[italic]…[/italic]`, `[underline]…[/underline]` | Text styles. |
| `[shake strength=0.7 frequency=12]…[/shake]` | Shake magnitude and frequency. |
| `[wave amplitude=2 frequency=2]…[/wave]` | Wave magnitude and frequency. |

Style spans can nest and must close in reverse order. Use `\[` and `\]` for literal brackets, `\\` for a literal backslash, and `\n` for a newline. Unknown tags, invalid parameters, or mismatched tags fall back to literal text. Valid numeric values outside the limits are clamped. Reveal and erase count Unicode grapheme clusters; word mode falls back to graphemes for continuous CJK text.

SAFE defaults to pause ≤3 seconds, hold ≤10 seconds, scale 0.75–1.5, shake ≤1 pixel, wave ≤2 pixels and a cumulative script time ≤60 seconds. ALL permits the absolute bounds, including ≤120 seconds total. SYSTEM_ONLY interprets only structurally classified system sources; UNKNOWN is not trusted as system. OFF displays markup literally. All modes enforce input, node, depth and effect limits. At the cumulative time limit the remaining script is presented as plain text. No tag opens files, runs commands or sends packets. Original chat Components and history remain intact.

[README](../../README.md)
