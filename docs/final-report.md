# Cinematic Chat 开发交付报告

2026-10-02。本次交付为可构建、可启动并经过真实客户端自动场景的实验版本。15 个 Minecraft 版本、两种 Loader，共 30 个独立目标。完整人工验收与已认证 Signed Chat 尚未完成，未标记 Supported。

## 1. 项目架构

`minecraft-api` 提供纯 Java 契约；`engine` 实现 CCML、Unicode、时间线、队列、动画和布局；`client-common` 实现配置、编辑模型、预设、声线与异步文件缓存；版本 adapter 提供 Component、Screen、绘制和 native audio；Loader 只做客户端初始化、事件、命令、按键与 metadata。共享业务随每个 JAR 打包。

源码导航：[API](../minecraft-api/src/main/java/io/github/axiraa/cinematicchat/api/ChatEnvelope.java)、[parser](../engine/src/main/java/io/github/axiraa/cinematicchat/engine/CcmlParser.java)、[controller](../client-common/src/main/java/io/github/axiraa/cinematicchat/client/SubtitleController.java)、[参考客户端](../adapters/common/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/CinematicChatClient.java)。详见 [架构](architecture.md)。

## 2. 已完成功能

纯客户端字幕、四种 reveal、局部效果、有限演出上限、3 可见/50 待播队列、原消息保留、聊天/F1 生命周期、缓存与资源重载；原生配置界面、中英语言、working copy、HUD 拖动/宽度/整体缩放/九锚点/吸附/Reset、六预设和用户视觉预设；默认关闭的本地声线、五模式、身份匹配、OGG/WAV、随机化、节流与安全路径、后台缓存和 native buffer 释放。

实现与证据逐节对应 [116 节需求追踪](requirements/traceability.md)，不将源码存在等同于端到端验收通过。主要界面：[配置](../adapters/common/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/CinematicConfigScreen.java)、[HUD](../adapters/common/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/SubtitleHudEditorScreen.java)、[声线](../adapters/common/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/VoiceProfileScreen.java)。

## 3. CCML 语法

`pause/wait/hold/speed/reveal/clear/replace/erase/br/fadein/fadeout` 与嵌套 `color/scale/alpha/bold/italic/underline/shake/wave`。未知/非法/不匹配标签保留文本，数值 clamp，异常退回 styled subtitle。保留 AST 源码区间，使用 grapheme；SAFE 60 秒、ALL 120 秒演出上限。详见 [语法及示例](ccml.md)。

## 4. 聊天捕获

1.21.x 在最终三参数 `ChatComponent.addMessage` 的历史写入后捕获；26.1.x 在 `addMessageToQueue(GuiMessage)` 的历史写入后捕获。Loader 事件仅提供结构化上下文，不第二次入队。不改 Component、签名、发送参数或协议。过滤/撤回同步到本地字幕；不能可靠分离正文时保留完整 Component。

关键文件：[ChatCaptureBridge](../adapters/api/legacy/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/ChatCaptureBridge.java)、[参考 Mixin](../adapters/api/legacy/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/mixin/ChatComponentMixin.java)、[26.1 Mixin](../adapters/api/mc26.1/minecraft/src/main/java/io/github/axiraa/cinematicchat/minecraft/mixin/ChatComponentMixin.java)。

## 5. 原版 HUD 隐藏

NeoForge 取消聊天 GUI layer 的 gameplay 绘制；Fabric 精确 gate 原版 ChatComponent 的 render/extract 入口。ChatScreen 恢复原版历史与交互；26.1 DisplayMode 只 gate BACKGROUND，保留前景模式。F1 隐藏字幕并静音；ChatScreen 暂停字幕且停止声音，关闭后续播。世界离开清理队列、解码代次与声音。

## 6. Fabric / NeoForge 差异

共享 engine、client-common 与大部分 adapter 一致。Loader 分别使用客户端事件、命令、按键与资源重载 API；Fabric 依赖 Fabric API，NeoForge 没有额外配置或跨 Loader 运行时。required Mixin 的实际签名在各目标启动检查，`verifyModJar` 检查客户端 metadata、精确 MC 版本和共享核心。

## 7. Minecraft 版本差异

| 边界 | 本地适配 |
|---|---|
| 1.21 / 1.21.1 | 参考聊天入口、GuiGraphics、tell 延后任务 |
| 1.21.2 | Screen blur 无参数、GameRules FeatureFlagSet、schedule 延后任务 |
| 1.21.4 / 1.21.5 | NeoForge reload 事件、FML 初始化时序 |
| 1.21.6 | 2D pose/ARGB、blur 绘制入口、Screenshot multiplier |
| 1.21.9 | 输入事件对象、KeyMapping category、profile id/name、FontDescription/Sprite/Atlas、ChatScreen ctor |
| 1.21.11 | Identifier、outline、Fabric render 的 Font/boolean 签名、GameRules 包 |
| 26.1.x | 历史准入入口、权限 filter、DisplayMode、GuiGraphicsExtractor、未混淆 Loom、DifficultySettings、resizeGui |

每个版本生成源码和实际 hook 记录在 [porting](porting/26.1.md) 目录的各版本文件。patch 版本分别编译/运行；无运行时版本分支塞入核心。

## 8. Java 与构建

legacy：Java 21、Gradle 9.2.1、Mojang Official Mappings；modern：Java 25、Gradle 9.4.0，共享模块仍 release 21。NeoForge ModDevGradle 2.0.148，Fabric Loom 1.15.5 / Loader 0.19.5。依赖表、锁文件、Wrapper 和 SHA-256 清单已保存；最终构建启用正常严格校验。官方仓库本机可选缓存不进入 Git。

关键文件：[targets.json](../gradle/targets.json)、[root build](../build.gradle)、[build-logic](../build-logic/target-properties.gradle)、[Windows 矩阵脚本](../tools/build/build-matrix.ps1)、[Unix 脚本](../tools/build/build-matrix.sh)。详见 [构建说明](building.md)。

## 9. 测试结果

53 个纯 Java 测试通过；原需求 A–H fixture、SAFE/ALL 累计上限、SYSTEM_ONLY 来源、fade 范围、长文本视口、30/60/144/240 FPS 模拟、编辑模型、声线随机化/节流和 reload 竞态有专门断言。每目标独立执行 Build、Launch、临时世界场景与原生音频检查，详情在 [实际验证](validation.md)。原生声音参数读取真实 AL_GAIN/AL_PITCH，测试代码不进入发布 JAR。

关键测试：[EngineTest](../engine/src/test/java/io/github/axiraa/cinematicchat/engine/EngineTest.java)、[ClientCommonTest](../client-common/src/test/java/io/github/axiraa/cinematicchat/client/ClientCommonTest.java)、[VoiceRuntimeTest](../client-common/src/test/java/io/github/axiraa/cinematicchat/client/VoiceRuntimeTest.java)、[ClientScenario](../adapters/common/minecraft/src/smoke/java/io/github/axiraa/cinematicchat/smoke/ClientScenario.java)。

## 10. 30 target compatibility matrix

每行状态、Loader 版本和完整 JAR SHA-256 见 [matrix.md](validation/matrix.md)；命令、时间、断言与证据 hash 见 [matrix.json](validation/matrix.json)。本机产物位于 `dist/`，命名 `cinematic-chat-0.1.0+mc<版本>-<loader>.jar`，共享核心已内置。`dist/SHA256SUMS` 可核对全部产物。

## 11. 当前限制与已知问题

没有已认证客户端/签名验证服务器的端到端证据；离线开发 profile 的对象身份与发送测试不能代替该项。人工听感、实体键鼠、更多 GUI Scale 组合、全 GUI/CLI 路径、字体资源包、其他聊天模组和长时性能仍待检查。六种指定窗口比例及声线新建/选择/保存/重载/删除已自动执行。完整 30 行 CI 已提供；首次远端运行构建失败，修复和复测状态见 [CI 记录](ci-status.md)。详见 [验收清单](acceptance.md)。没有将这些项目误标为完成或 Supported。

历史 native buffer 删除顺序、Fabric HUD 命令关闭时序、26.1 准入捕获位置、Windows NeoForm 内存和映射多 hash 校验问题已处理，修复证据及保留的开发环境警告记录在验证文档。

## 12. 下一阶段

优先在已认证测试服完成 Signed Chat、过滤、撤回与举报检查；按验收表补齐实体 HUD 操作、实体声线输入和人工试听；执行远端 Linux CI 与 macOS 脚本，并进行资源包/聊天模组组合和长时性能测试。之后按真实验收结果逐行更新 Supported，再准备正式发布。
