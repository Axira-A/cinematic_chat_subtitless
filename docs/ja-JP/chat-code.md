# CCML チャットコード

[English](../en/chat-code.md) · [简体中文](../zh-CN/chat-code.md) · [日本語](../ja-JP/chat-code.md) · [한국어](../ko-KR/chat-code.md)

CCML はバニラが受け入れたプレイヤー・システムのチャット（`/say` を含む）から読み取ります。送信元設定と信頼モードが解析を制御します。`/cchat parser <text>` はローカルでスクリプトを調べ、`/cchat preview` は現在の言語の歓迎文を生成します。他のプレイヤーには送信しません。

```text
[speed=30]こんにちは、[color=#67BDE5][bold]Cinematic Chatへようこそ。[/bold][/color][hold=3]
```

| タグ | 動作 |
|---|---|
| `[pause=0.5]` | 前の文字を表示してから指定秒数停止。 |
| `[wait]` | 前の有限の入場・表示アニメーションを待つ。継続する shake/wave は待たない。 |
| `[hold=3]` | 全文表示後の最短保持時間。複数指定時は最大値。 |
| `[speed=30]`、`[speed=default]` | 秒あたりの表示単位数、または設定速度。 |
| `[reveal=instant]` | `instant`、`typewriter`、`smooth`、`word` が選択可能。 |
| `[clear]`、`[replace]` | 現在の entry の文字を消し、後続の文字に置き換える。 |
| `[erase=2]` | 現在の entry の末尾から最大二つの書記素を削除。 |
| `[fadein=0.2]`、`[fadeout=0.3]` | 入場・退場のフェード秒数。0～3 に制限。 |
| `[br]` | 改行。`\n` も使用可能。 |
| `[color=#67BDE5]…[/color]` | RGB 色。 |
| `[scale=1.2]…[/scale]`、`[alpha=0.8]…[/alpha]` | 部分倍率と不透明度。 |
| `[bold]…[/bold]`、`[italic]…[/italic]`、`[underline]…[/underline]` | 太字、斜体、下線。 |
| `[shake strength=0.7 frequency=12]…[/shake]` | 揺れの強度と周波数。 |
| `[wave amplitude=2 frequency=2]…[/wave]` | 波の振幅と周波数。 |

装飾は入れ子にでき、開いた順と逆順に閉じます。`\[` と `\]` は角括弧、`\\` はバックスラッシュ、`\n` は改行です。不明なタグ、不正な引数、対応しないタグは文字として表示します。有効な数値が上限を超える場合は制限内に丸めます。表示と削除は Unicode 書記素単位です。連続した CJK は単語モードでも書記素単位になります。

SAFE の既定値は pause ≤3 秒、hold ≤10 秒、scale 0.75～1.5、shake ≤1 ピクセル、wave ≤2 ピクセル、合計 ≤60 秒です。ALL も絶対上限を守り、合計は ≤120 秒です。SYSTEM_ONLY は構造的にシステムと分類された送信元だけを解析し、UNKNOWN は対象外です。OFF はタグをそのまま表示します。すべてのモードで入力長、ノード数、深さ、効果を制限します。合計時間の上限後は残りを通常の文字として表示します。タグはファイル、コマンド、パケットにアクセスしません。元の Component と履歴は保持します。

[README](../../README.ja-JP.md)
