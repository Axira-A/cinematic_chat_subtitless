# Cinematic Chat

[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja-JP.md) · [한국어](README.ko-KR.md)

Minecraft のチャットを映画風の字幕で表示するクライアント専用 Mod です。上限付きの CCML スクリプトエンジン、視覚的な HUD エディター、任意のローカル疑似音声を備えています。チャット画面を開くとバニラの履歴を確認できます。サーバーへの導入は不要です。

## 機能

- 文字装飾、Unicode 書記素・単語単位の表示、時間制御、部分的な文字エフェクト。
- 送信元フィルター、キューの優先度、アニメーション、閲覧時間、アクセシビリティ設定。
- 視覚的な配置、九つのアンカー、幅と倍率のハンドル、スナップ、六つの内蔵 HUD プリセット。
- ローカルの OGG Vorbis・WAV PCM を使う音声プロファイルと五つの再生モード。
- 英語、簡体字中国語、日本語、韓国語のインターフェース。

## 対象バージョン

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

すべて実験的なビルド対象です。Minecraft の正確なバージョンと Loader の両方に合う JAR を選んでください。認証済み Signed Chat の検証は未実施であり、オフラインの開発シナリオではその検証を代替できません。

## 導入

現在、このリポジトリに Release はありません。アクセス権がある場合は成功した [Actions ビルド](https://github.com/Axira-A/cinematic_chat_subtitless/actions/workflows/matrix.yml)から対応する JAR を取得するか、[ソースからビルド](BUILDING.md)してください。対応する Fabric Loader と Fabric API、または NeoForge を導入し、クライアントの `mods/` に JAR を置きます。共通モジュールは JAR に含まれます。

## はじめに

1. ワールドに入り、`/cchat config` で字幕を設定します。
2. `/cchat preview` で Steve と Alex の歓迎メッセージを表示します。
3. `/cchat hud` で字幕の位置を調整し、適用します。

`/cinematicchat` は `/cchat` の別名です。ほかに `clear`、`reload`、`debug`、`parser <text>`、`voice` が使えます。設定と HUD のキー割り当ては Minecraft の操作設定で変更できます。既定ではチャット画面で字幕を非表示・一時停止にします。F1 は字幕と疑似音声を止めます。

## CCML

CCML はローカルの字幕表示を制御します。元のチャットやバニラの履歴は変更しません。既定の信頼モードは SAFE です。

```text
[speed=30]こんにちは、[color=#67BDE5]Cinematic Chatへようこそ。[/color][hold=3]
```

タグ、時間制御、エスケープ、信頼モードは [CCML ガイド](docs/ja-JP/chat-code.md)を参照してください。

## HUD

字幕領域をドラッグして移動し、端で幅、角で倍率を調整します。Shift は軸固定、Alt は一時的なスナップ解除です。直接開いた場合は「適用」で確定します。設定画面から開いた場合は親の設定画面も保存してください。[HUD ガイド](docs/ja-JP/hud-editor.md)を参照してください。

## 疑似音声

既定では無効です。字幕の表示に合わせてローカルの短い音を鳴らす機能で、音声合成ではありません。プロファイルを作成して Steve または Alex に関連付け、`config/cinematic_chat/audio/` の音を選び、テストして保存します。[音声ガイド](docs/ja-JP/voice.md)を参照してください。

## 言語

Minecraft の言語メニューで選択します。新しいプレビューは現在の言語を使います。実際のチャット本文、独自の名前、プロファイル ID、ファイル名は元の値を保ちます。

## ビルド

1.21.x は JDK 21、26.1.x は JDK 25 を使います。共通 Java モジュールの対象は Java 21 です。[BUILDING.md](BUILDING.md) に Wrapper、個別・全対象ビルド、開発シナリオ、依存関係の検証をまとめています。

## ライセンス

[MIT](LICENSE)。同梱するオリジナルのテスト音と再生成方法は [SOURCES.txt](adapters/common/minecraft/src/main/resources/assets/cinematic_chat/audio/SOURCES.txt) に記載しています。
