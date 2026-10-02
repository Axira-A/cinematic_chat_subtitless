# Cinematic Chat

[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja-JP.md) · [한국어](README.ko-KR.md)

为 Minecraft 聊天提供电影式字幕的纯客户端模组，包含有界 CCML 脚本引擎、可视化 HUD 编辑器和可选的本地伪配音。打开聊天界面即可查看原版历史，服务器无需安装此模组。

## 功能

- 保留文字样式，支持按 Unicode 字素或单词显现、时间控制及局部文字效果。
- 可配置消息来源、队列优先级、动画、阅读时间和无障碍选项。
- 可视化定位、九锚点、宽度及缩放手柄、吸附和六个内置 HUD 预设。
- 可选声线 Profile，使用本地 OGG Vorbis 或 WAV PCM，支持五种播放模式。
- 英文、简体中文、日文和韩文界面资源。

## 版本范围

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

以上均为实验性构建目标。请同时匹配 Minecraft 的精确版本和 Loader。已认证的 Signed Chat 测试仍待执行，离线开发场景不代表该项已验证。

## 安装

仓库目前没有 Release。有仓库访问权限时，可从成功的 [Actions 构建](https://github.com/Axira-A/cinematic_chat_subtitless/actions/workflows/matrix.yml)下载对应 JAR，或[自行构建](BUILDING.md)。安装对应的 Fabric Loader 与 Fabric API，或 NeoForge，再将模组 JAR 放入客户端的 `mods/` 目录。共享模块已随 JAR 打包。

## 快速开始

1. 进入世界，执行 `/cchat config` 调整字幕。
2. 执行 `/cchat preview` 查看 Steve 和 Alex 的欢迎语。
3. 执行 `/cchat hud` 调整字幕区域并应用。

`/cinematicchat` 是 `/cchat` 的别名。其他客户端命令包括 `clear`、`reload`、`debug`、`parser <text>` 和 `voice`。配置及 HUD 快捷键可在 Minecraft 控制设置中修改。默认打开聊天时隐藏并暂停字幕；F1 隐藏字幕并静音伪配音。

## CCML

CCML 只控制本地字幕呈现，保留原始聊天消息与原版历史。默认信任模式为 SAFE。

```text
[speed=30]您好，[color=#67BDE5]欢迎使用Cinematic Chat。[/color][hold=3]
```

标签、时间语义、转义和信任模式见 [CCML 教程](docs/zh-CN/chat-code.md)。

## HUD

拖动字幕区域移动位置，拖动边缘调整宽度，拖动角落调整缩放。Shift 锁定移动轴，Alt 临时取消吸附。直接打开编辑器时，Apply（应用）提交修改；从配置页进入时，还需保存父配置页。参见 [HUD 教程](docs/zh-CN/hud-editor.md)。

## 伪配音

伪配音默认关闭，通过字幕显现触发本地短音，并非语音合成。建立 Profile，绑定 Steve 或 Alex，选择 `config/cinematic_chat/audio/` 内的文件，测试并保存。参见[声线教程](docs/zh-CN/voice.md)。

## 语言

在 Minecraft 语言菜单选择语言。新生成的预览使用当前语言。真实聊天正文、自定义名称、Profile ID 和文件名保留原值。

## 构建

1.21.x 使用 JDK 21，26.1.x 使用 JDK 25；共享 Java 模块以 Java 21 为目标。[BUILDING.md](BUILDING.md) 介绍 Wrapper、单目标与全矩阵构建、客户端场景及依赖校验。

## 许可证

[MIT](LICENSE)。附带的原创测试音及重生成说明见 [SOURCES.txt](adapters/common/minecraft/src/main/resources/assets/cinematic_chat/audio/SOURCES.txt)。
