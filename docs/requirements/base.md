# 任务：开发 Cinematic Chat —— Minecraft 客户端沉浸式聊天字幕与字幕脚本引擎

你现在负责从零开始设计、实现、测试并整理一个完整 Minecraft 客户端模组，暂定名称：

**Cinematic Chat**

Mod ID：

`cinematic_chat`

这是一个纯客户端模组。

它的核心目标不是“美化原版聊天框”，而是：

**在正常游戏过程中隐藏 Minecraft 原版聊天 HUD，将原本出现在聊天栏中的玩家聊天、/say、系统消息等内容转换成类似现代 RPG / 开放世界游戏的沉浸式 HUD 字幕。**

玩家主动按下 `T`、`/` 或以其他正常方式打开聊天界面时，仍然必须能够看到完整原版聊天历史、发送消息、使用命令补全、点击 Component、查看 HoverEvent、处理签名消息等。

除了字幕化显示，本模组还需要实现一套安全的、纯客户端解释执行的字幕标记语言，用普通聊天文字即可控制：

- 换行
- 停顿
- 打字速度
- 淡入淡出
- 打字机效果
- Smooth Reveal
- 逐词出现
- 局部抖动
- 波浪
- 字号缩放
- 颜色
- 透明度
- 字幕保持时间
- 等待上一段动画完成
- 替换当前字幕
- 删除部分字符
- 清空字幕
- 其他合理的字幕演出

整个系统必须保持：

**100% client-side。**

不得要求服务器安装 Cinematic Chat。

不得修改 Minecraft 聊天协议。

不得实现自定义聊天网络协议。

不得绕过、破坏或重新实现 Mojang 的 Signed Chat。

不得删除原版聊天历史。

不得改变服务器收到的玩家消息。

不得让字幕脚本执行 Java、命令、文件、网络请求、系统调用或任何任意代码。

字幕代码仅仅是一套安全的显示标记语言。

---

# 一、必须支持的 Minecraft / Loader 范围

目标必须覆盖以下 Minecraft 正式版本：

```text
1.21
1.21.1
1.21.2
1.21.3
1.21.4
1.21.5
1.21.6
1.21.7
1.21.8
1.21.9
1.21.10
1.21.11
26.1
26.1.1
26.1.2
```

每一个版本均要求：

```text
Fabric
NeoForge
```

即目标矩阵理论上为：

```text
15 Minecraft versions × 2 loaders = 30 targets
```

不要因为相邻 Minecraft 版本“看起来兼容”就未经验证地跳过。

默认策略是：

**每一个 Minecraft 版本都必须作为独立测试目标。**

如果经过实际编译和运行验证，某个 JAR 确实可以安全兼容多个相邻 patch/minor 版本，可以合并发布 artifact，但 CI 仍然必须分别测试对应 Minecraft 版本。

绝对不要尝试制作一个从 Minecraft 1.21 一直运行到 26.1.2 的 universal jar。

Minecraft 26.1 必须视为明确的兼容边界。

技术要求：

```text
Minecraft 1.21.x
→ Java 21
→ Mojang Official Mappings，Fabric 侧也优先统一采用官方映射

Minecraft 26.1.x
→ Java 25
→ Mojang 官方名称 / unobfuscated environment
```

共享核心代码尽量以 Java 21 编译，从而同时被 Java 21 和 Java 25 target 使用。

26.1.x Minecraft adapter/platform 层使用 Java 25。

不要为了共享代码而强迫 Java 21 target 使用 Java 25。

---

# 二、开始开发前必须完成的研究

不要直接开始乱写 Mixin。

首先检查：

1. 当前工作区是否已有工程。
2. 如果已有工程，完整审计现有 Gradle、源码、资源、Mixin、LICENSE、Git 状态。
3. 如果为空，建立新的 monorepo。
4. 阅读每个关键 Minecraft 版本对应的：
   - NeoForge migration primer
   - NeoForge documentation
   - Fabric documentation
   - Fabric porting guide
   - Minecraft vanilla 源码
5. 特别研究这些版本边界：
   - 1.21 → 1.21.1
   - 1.21.1 → 1.21.2/3
   - 1.21.3 → 1.21.4
   - 1.21.4 → 1.21.5
   - 1.21.5 → 1.21.6
   - 1.21.6 → 1.21.7
   - 1.21.7 → 1.21.8
   - 1.21.8 → 1.21.9
   - 1.21.9 → 1.21.10
   - 1.21.10 → 1.21.11
   - 1.21.11 → 26.1
6. 找到各版本 vanilla 中负责：
   - 接收 player chat
   - 接收 system chat
   - ChatComponent / equivalent
   - addMessage
   - ChatScreen
   - HUD render
   - Font
   - GUI scale
   - Component / Style / FormattedCharSequence
   的实际实现。

不要猜 API 名称。

不要使用自己记忆中的旧 API 硬套新版本。

如果某个函数或类发生变化，以对应目标 Minecraft 版本源码为准。

所有 loader、Fabric API、NeoForge、Loom、ModDevGradle/NeoGradle 版本必须从官方 metadata、模板或文档确认。

不要凭空猜版本号。

---

# 三、工程架构

这个工程最重要的原则：

**字幕业务核心与 Minecraft/Loader API 完全解耦。**

推荐结构：

```text
CinematicChat/
│
├─ engine/
│  ├─ parser/
│  ├─ ast/
│  ├─ timeline/
│  ├─ animation/
│  ├─ layout-model/
│  ├─ queue/
│  ├─ config-model/
│  └─ tests/
│
├─ minecraft-api/
│
├─ targets/
│  ├─ 1.21/
│  │  ├─ minecraft/
│  │  ├─ fabric/
│  │  └─ neoforge/
│  ├─ 1.21.1/
│  │  ├─ minecraft/
│  │  ├─ fabric/
│  │  └─ neoforge/
│  ├─ ...
│  ├─ 1.21.11/
│  ├─ 26.1/
│  ├─ 26.1.1/
│  └─ 26.1.2/
│
├─ build-logic/
├─ docs/
├─ tools/
└─ .github/workflows/
```

具体目录允许根据 Gradle 实际需求调整，但架构边界不能破坏。

## engine

`engine` 禁止引用：

```text
net.minecraft.*
Fabric API
Fabric Loader API
NeoForge
Mixin
Blaze3D
```

这里只允许实现纯 Java 字幕系统。

例如：

```text
SubtitleDocument
SubtitleNode
TextNode
PauseNode
WaitNode
HoldNode
ClearNode
ReplaceNode
EraseNode

SubtitleTimeline
TimelineCursor
RevealController

EffectSpan
ColorSpan
ScaleSpan
ShakeSpan
WaveSpan

SubtitleQueue
SubtitleState
SubtitleRuntime
```

engine 使用 Java 21 language level。

必须有完整 JUnit 测试。

## minecraft-api

这里定义稳定抽象，例如：

```text
ChatEnvelope
ChatSource
StyledText
StyledSpan
RenderContext
FontBridge
Clock
SubtitleSink
ClientState
```

同样尽量禁止直接依赖 Minecraft 类。

Minecraft Component 到这些 DTO 的转换放进版本 adapter。

## targets/<version>/minecraft

这里只负责：

- 对应 Minecraft 版本的 Component 转换
- Chat hook
- GUI hook
- Font bridge
- HUD render bridge
- ChatScreen 检测
- GUI Scale
- KeyMapping
- ResourceLocation/font handling
- 版本特有兼容

## fabric / neoforge

Loader 层尽量薄。

只处理：

- 初始化
- loader event
- config screen 注册
- client command
- key binding
- loader metadata
- Mixin bootstrap
- loader 特有 HUD/event bridge

禁止分别写两套 SubtitleEngine。

---

# 四、不要加入 Architectury 等强制运行时依赖

项目的目标是：

Fabric：

```text
Fabric Loader
Fabric API（在实际需要的版本使用）
Cinematic Chat
```

NeoForge：

```text
NeoForge
Cinematic Chat
```

不要让用户额外安装：

```text
Architectury API
Cloth Config
YACL
owo-lib
Kotlin
```

除非未来明确决定，否则 V1 不应依赖它们。

配置 Screen 自己实现。

ModMenu 可以做 optional integration，但不能成为必需依赖。

---

# 五、聊天处理基本原则

绝对不要在 packet protocol 层重新实现聊天。

正确逻辑：

```text
Server
 ↓
Vanilla networking
 ↓
Vanilla signed/system chat processing
 ↓
Vanilla Chat Component/history
 ↓
Cinematic Chat capture
 ↓
Subtitle Engine
 ↓
HUD
```

我们改变的是：

**显示方式。**

不是：

**聊天协议。**

必须保证：

- Signed Chat 正常工作
- 举报/签名相关 vanilla 数据不丢失
- 原始 Component 保留
- 原版 Chat History 保留
- HoverEvent 保留
- ClickEvent 保留
- Translation Component 保留
- Style 保留
- Narrator 行为不被破坏
- command suggestion 不被破坏

不要使用：

```java
component.getString()
```

然后丢弃 Component 信息作为唯一数据来源。

可以生成用于字幕的 styled representation，但必须永远保留原始 Component。

---

# 六、原版聊天 HUD 行为

正常游戏：

```text
Vanilla Chat HUD
→ 隐藏

Cinematic Subtitle HUD
→ 显示
```

玩家打开 ChatScreen：

```text
Vanilla Chat History
→ 显示

Vanilla Chat input
→ 正常

Cinematic subtitles
→ 默认隐藏并暂停计时
```

关闭 ChatScreen：

```text
恢复 Cinematic Subtitle HUD
```

必须支持：

```text
T
/
以及其他打开 ChatScreen 的方式
```

不要依赖检测某个按键来判断聊天是否打开。

应该直接检查当前 Screen 类型/状态。

F1 Hide GUI 时：

```text
Cinematic subtitles
→ 同样隐藏
```

不要独立于 Minecraft Hide GUI 设置继续显示字幕。

---

# 七、消息捕获

优先寻找尽量稳定、侵入最小的 hook。

需要捕获至少：

```text
PLAYER_CHAT
SYSTEM
SAY
WHISPER
JOIN_LEAVE
DEATH
ADVANCEMENT
UNKNOWN
```

如果某版本 vanilla 可以可靠提供 source metadata，则使用真实 metadata。

不要通过纯字符串正则猜测：

```text
<Steve> hello
```

来判断 sender，除非该版本实在无法取得结构化上下文，并且必须有 fallback。

`/say` 必须能够进入字幕系统。

普通玩家聊天必须进入字幕系统。

服务器 system message 必须可以配置是否进入字幕系统。

Action Bar 不默认接管。

Boss Bar 不接管。

Title/SubTitle 不接管。

以后可以扩展，但不要混为一谈。

---

# 八、Mixin 策略

原则：

**Inject > Redirect/Wrap > Overwrite**

尽量避免 `@Overwrite`。

禁止复制整段 vanilla ChatComponent render 实现然后修改。

我们需要达到：

```text
消息依然进入 Vanilla ChatComponent/history
但 ChatComponent 在正常 gameplay 中不绘制
```

优先寻找：

- loader 官方 HUD render event
- cancellable GUI layer event
- targeted Mixin

如果事件 API 无法可靠阻止原版 chat render，则对对应 Minecraft 版本进行最小 Mixin。

Mixin 只负责：

```text
capture
hide
bridge
```

不要把 parser、animation、queue 逻辑写进 Mixin。

Mixin 调用：

```text
CinematicChatClient.get().onChat(...)
```

即可。

每个版本都检查实际调用链，防止：

```text
addMessage(A)
→ addMessage(B)
```

导致消息重复捕获。

必须写 duplicate capture regression test 或 debug assertion。

---

# 九、字幕默认视觉设计

默认字幕位置：

```text
horizontal = center
vertical = 78% screen height
```

默认：

```text
Max visible subtitles = 3
Background = disabled
Text shadow = enabled
Alignment = center
```

玩家聊天推荐布局：

```text
                PlayerName
        我们从北边的入口进去。
```

Sender name：

- 比正文稍小
- 可以单独调节 opacity
- 可隐藏

如果无法可靠取得 sender：

直接渲染完整 Component。

字幕不应该表现成矩形聊天气泡。

默认没有聊天框背景。

默认没有头像。

默认没有 Discord 风格卡片。

这是 Cinematic Chat 最重要的视觉方向。

---

# 十、字幕队列

实现：

```text
SubtitleQueue
```

默认：

```text
maxVisible = 3
maxQueued = 50
```

新字幕从底部进入。

旧字幕向上平滑移动。

消息越旧：

- opacity 可以逐渐降低
- priority 可以降低

禁止无限堆积。

刷屏时不能占满整个 HUD。

需要有：

```text
NORMAL
IMPORTANT
SYSTEM
```

等基础优先级概念。

但不要根据聊天内容擅自判断重要性。

---

# 十一、显示时间

不要所有消息固定显示 3 秒。

实现：

```text
duration =
baseDuration
+ readableCharacterCount * durationPerCharacter
```

配置：

```text
baseDuration
durationPerCharacter
minDuration
maxDuration
```

字幕脚本 `[hold]` 可以覆盖/延长它。

动画时间不要依赖 FPS。

使用稳定 monotonic clock。

渲染插值必须支持：

- 30 FPS
- 60 FPS
- 144 FPS
- 240 FPS

获得视觉一致结果。

---

# 十二、文字 Reveal 模式

实现：

```text
INSTANT
TYPEWRITER
SMOOTH
WORD_BY_WORD
```

## INSTANT

立即显示完整文字。

## TYPEWRITER

逐字符出现。

## SMOOTH

字符不是突然出现。

类似：

```text
previous glyph    alpha 1.00
current glyph     alpha 0.70
next glyph        alpha 0.30
next+1            alpha 0.10
```

形成柔和 reveal。

## WORD_BY_WORD

英文以单词为单位。

CJK 没有空格时必须合理 fallback，不允许整句话一次出现。

Reveal 不得拆开 UTF-16 surrogate pair。

尽量按 Unicode grapheme cluster 工作。

至少必须安全处理：

- 中文
- 日文
- 韩文
- Emoji
- combining characters

---

# 十三、字幕脚本语言

给这套语言暂定名称：

**CCML — Cinematic Chat Markup Language**

CCML 是安全的 presentation language。

它绝不是真正脚本执行器。

示例：

```text
你终于来了。\n[pause=1.2]不过……已经太迟了。
```

客户端表现：

```text
你终于来了。

（等待 1.2 秒）

不过……已经太迟了。
```

---

# 十四、转义

必须支持：

```text
\n      newline
\\      literal backslash
\[      literal [
\]      literal ]
```

也可支持：

```text
[br]
```

作为 `\n` 的可读别名。

---

# 十五、Timeline 指令

至少实现：

```text
[pause=1.0]

[wait]

[hold=2.0]

[speed=30]

[speed=default]

[reveal=instant]

[reveal=typewriter]

[reveal=smooth]

[reveal=word]

[clear]

[replace]

[erase=4]
```

语义必须明确。

## pause

```text
[pause=1.25]
```

暂停 Timeline 1.25 秒，然后继续。

## wait

```text
[wait]
```

等待当前已经排入 reveal timeline 的文字完全显示，然后继续。

不需要作者计算当前字符需要多少秒。

## hold

```text
[hold=3]
```

当前字幕内容全部显示之后，至少保留三秒。

## speed

```text
[speed=20]
```

后续 reveal speed = 20 visible units/sec。

```text
[speed=default]
```

恢复用户默认值。

## clear

清除当前 subtitle slot 的显示内容。

不得删除 Vanilla Chat History。

## replace

将当前字幕 slot 当前显示内容替换为后续内容。

例如：

```text
欢迎回来，勇者。[pause=1][replace]欢迎回来，罪人。
```

表现为同一个字幕位置发生替换，而不是创建新聊天记录。

## erase

```text
[erase=4]
```

从当前字幕可见文本末尾删除 4 个 grapheme/codepoint units。

不要破坏 Unicode surrogate pair。

---

# 十六、局部 Style / Effect 标签

必须实现成可嵌套 AST，而不是字符串 replace。

例如：

```text
[color=#C8A46B]黄金[/color]

[scale=1.2]危险[/scale]

[alpha=0.6]低语[/alpha]

[shake]快跑！[/shake]

[wave]潮水正在上涨……[/wave]

[bold]文字[/bold]

[italic]文字[/italic]

[underline]文字[/underline]
```

支持嵌套：

```text
不要碰那个[color=#B02020][shake]东西[/shake][/color]。
```

预期 AST：

```text
Document
├─ Text
├─ ColorSpan
│  └─ ShakeSpan
│     └─ Text
└─ Text
```

不要把标签作用强制扩散到整条消息。

---

# 十七、效果参数

建议允许：

```text
[shake strength=0.7 frequency=12]
...
[/shake]

[wave amplitude=2 frequency=2]
...
[/wave]
```

如果第一阶段 parser 实现成本过高，可以先支持：

```text
[shake]
[wave]
```

再逐步添加 attributes。

但 Parser 架构从一开始就必须支持 attribute map，避免以后重写 grammar。

---

# 十八、Parser 安全规则

任何通过聊天收到的内容都必须视为：

**untrusted input**

严格禁止：

```text
reflection
eval
JavaScript engine
Groovy
shell
Runtime.exec
ProcessBuilder
filesystem access
HTTP
socket
Minecraft command execution
client command execution
packet sending
class loading
```

CCML 只能返回 AST。

建议硬限制：

```text
maxInputLength        = 32768
maxAstNodes           = 2048
maxNestingDepth       = 16

maxPauseSeconds       = 10
maxHoldSeconds        = 30

minSpeed              = 1
maxSpeed              = 120

minScale              = 0.5
maxScale              = 2.5

maxShakePixels        = 4
maxWaveAmplitude      = 8

maxEraseUnits         = 256
```

这些值做成 config，但必须存在 absolute safety ceiling。

即使 config 文件被修改，也不能允许：

```text
[pause=999999999]
```

永久堵塞 queue。

---

# 十九、Parser 错误处理

聊天绝不能因为 malformed CCML 崩溃。

例如：

```text
hello [pause=abc] world
```

或者：

```text
[color=#GGHHZZ]
```

或者：

```text
[shake][wave][/color]
```

处理原则：

```text
Parse success
→ AST

Parse partially invalid
→ invalid markup 按普通文字显示或安全忽略

Unexpected exception
→ fallback plain subtitle
```

永远不能：

```text
crash client
disconnect
drop original chat message
```

未知标签默认作为 literal text 显示。

这能让：

```text
[test]
```

这种正常聊天不莫名消失。

---

# 二十、Vanilla Component Style

Minecraft Component 自己已有：

- color
- bold
- italic
- underline
- strikethrough
- obfuscated
- translation
- siblings

字幕转换器必须尽量保留。

CCML Style 与 Vanilla Style 叠加。

规则：

```text
Vanilla base style
        ↓
CCML local overrides
        ↓
Subtitle rendering style
```

原始 Component 永远不修改。

---

# 二十一、字体

必须支持 Minecraft resource font。

默认：

```text
minecraft:default
```

允许配置类似：

```text
minecraft:uniform
namespace:custom_font
```

如果 Resource Pack 添加字体，本模组应允许使用对应 ResourceLocation。

字体不存在：

```text
fallback minecraft:default
```

禁止因为无效 font ID 崩溃。

不要内置未经授权的字体文件。

---

# 二十二、自动换行

禁止根据：

```text
string.length()
```

进行换行。

必须根据 Font 实际像素宽度。

配置：

```text
maxWidthPixels
maxWidthScreenRatio
```

支持：

```text
center
left
right
```

三种 alignment。

长单词必须安全 break。

中文必须正确按字符换行。

---

# 二十三、字幕动画

Entry：

```text
NONE
FADE
SLIDE_UP
SLIDE_DOWN
SCALE_FADE
```

Reveal：

```text
INSTANT
TYPEWRITER
SMOOTH
WORD_BY_WORD
```

Exit：

```text
NONE
FADE
SLIDE_UP
SLIDE_DOWN
```

实现通用 easing：

```text
LINEAR
EASE_IN
EASE_OUT
EASE_IN_OUT
```

不要把 easing 写死在 renderer 内。

---

# 二十四、配置系统

不要依赖第三方配置库。

保存到例如：

```text
config/cinematic_chat.json
```

配置必须有：

```text
configVersion
```

未来升级配置时执行 migration。

损坏配置：

```text
backup corrupted file
load defaults
log warning
```

不要崩溃。

主要设置：

```text
enabled

positionX
positionY

alignment

fontId
fontScale

maxWidth
maxVisible

showSender
senderScale
senderOpacity

backgroundEnabled
backgroundOpacity

shadow

baseDuration
durationPerCharacter
minDuration
maxDuration

entryAnimation
entryDuration
entryEasing

revealMode
charactersPerSecond

exitAnimation
exitDuration
exitEasing

pauseWhenChatOpen
hideWhenChatOpen

messageType settings

ccmlEnabled
ccmlTrustMode
ccml limits
```

---

# 二十五、CCML Trust Mode

实现：

```text
ALL
SAFE
SYSTEM_ONLY
OFF
```

## ALL

所有进入字幕系统的消息解析 CCML。

## SAFE

所有消息解析，但 disruptive effects 使用更严格限制。

## SYSTEM_ONLY

仅系统消息、/say 等可信来源解析 CCML。

普通 PLAYER_CHAT 将代码按普通文字显示。

## OFF

完全关闭 CCML。

默认可以使用：

```text
SAFE
```

用户可以主动改成 ALL。

---

# 二十六、Config Screen

自己实现原生 Minecraft Screen。

必须支持实时 Preview。

Screen 至少包括：

```text
General
Position
Typography
Animation
Message Types
CCML
Accessibility
Advanced
```

Preview 示例可以持续模拟：

```text
Axira
北边的门打不开。

Honma
那就从塔楼过去。
```

改变：

- Font Scale
- Position
- Opacity
- Animation
- Width

时实时变化。

提供：

```text
Reset section
Reset all
Save
Cancel
```

Cancel 必须恢复进入 Screen 之前的配置。

---

# 二十七、Presets

提供几个自己的通用名称，不使用商业游戏名称：

```text
Cinematic
Minimal
RPG
Adventure
Accessible
Vanilla-like
```

不要直接把预设命名成：

```text
Elden Ring
RDR2
Assassin's Creed
Skyrim
```

---

# 二十八、客户端命令

实现：

```text
/cinematicchat
```

或简写：

```text
/cchat
```

这些必须是纯客户端命令。

推荐：

```text
/cchat config
/cchat reload
/cchat preview
/cchat clear
/cchat debug
/cchat parser <text>
```

`parser` 用于开发调试 CCML。

绝不能发送到服务器。

Fabric 与 NeoForge 分别使用正确的客户端 command API。

---

# 二十九、Debug Mode

实现可切换 debug。

Debug 信息可以显示：

```text
Source
Queue size
Visible count
Message ID
Age
Timeline position
AST nodes
Reveal progress
Current animation state
Minecraft version
Loader
```

正式默认关闭。

Logger 不得每帧刷日志。

---

# 三十、兼容其他聊天模组

我们的目标不是保证与所有聊天大修模组完美兼容，但必须采取低侵入策略。

不要：

```text
Overwrite ChatComponent
替换 Minecraft.chat 对象
重写整个 ChatScreen
```

尽量只：

```text
capture message
cancel normal-world rendering
draw custom subtitle
```

如果检测到其他模组改变 chat render：

记录 debug 信息，不要直接崩溃。

README 中维护 Compatibility 表。

---

# 三十一、Accessibility

必须考虑：

```text
字幕字号放大
高对比度
Background 可开启
Background opacity
Reduced Motion
Disable shake
Disable wave
Instant reveal
Longer reading time
```

增加：

```text
Reduce Motion
```

启用后：

- 禁用 shake
- 禁用 wave
- 禁用大位移 slide
- 可以保留简单 fade

---

# 三十二、性能要求

字幕系统不应该对普通玩家造成明显性能影响。

禁止每帧：

- 重新 parse CCML
- 重新创建完整 AST
- 重新计算不变文本 layout
- 重新构建所有 glyph metrics
- 大量临时对象

流程应该是：

```text
Message arrives
 ↓
parse once
 ↓
build AST once
 ↓
layout/cache
 ↓
runtime only advances state
 ↓
renderer draws current state
```

只有：

- GUI scale 改变
- font 改变
- max width 改变
- resource reload

才触发必要 re-layout。

---

# 三十三、Resource Reload

资源包切换后字体可能改变。

监听对应版本的 resource reload。

完成：

```text
clear font/layout cache
re-layout visible subtitles
```

不能继续保存失效 Font handle。

---

# 三十四、CCML 示例测试

以下必须成为测试 fixture。

### 示例 A

```text
你终于来了。\n[pause=1.2]不过……已经太迟了。
```

### 示例 B

```text
[speed=15]潮水正在上涨……[wait][pause=0.5]\n快走。
```

### 示例 C

```text
不要碰那个[color=#B02020][shake]东西[/shake][/color]。
```

### 示例 D

```text
欢迎回来，勇者。[pause=1][replace]欢迎回来，罪人。
```

### 示例 E

```text
勇者[wait][erase=2]罪人。
```

### 示例 F

```text
这是\[pause=5\]普通文字。
```

必须显示 literal：

```text
[pause=5]
```

而不是执行。

### 示例 G

非法：

```text
[pause=abc]
```

不能 crash。

### 示例 H

恶意：

```text
[pause=999999999]
```

必须被 clamp。

---

# 三十五、Core Unit Tests

至少测试：

```text
escape parser
nested tags
invalid tags
unknown tags
Unicode
Chinese
Japanese
Emoji
nested styles
timeline pause
wait
hold
replace
erase
speed
clamping
deep nesting attack
very long message
queue overflow
animation timing
config migration
```

Parser 应尽量达到高测试覆盖率。

---

# 三十六、Minecraft Integration Tests

每个目标版本至少执行：

```text
launch client
enter singleplayer
/send or /say test
normal player chat simulation where possible
open T chat
close T chat
F1
change GUI scale
change language
resource reload
long text
CCML
malformed CCML
```

确保：

```text
正常 gameplay 无原版 Chat HUD
字幕存在
ChatScreen 内聊天历史存在
消息没有 duplicate
没有 signature/chat protocol breakage
```

---

# 三十七、版本兼容 CI

建立 GitHub Actions build matrix。

所有目标必须进入矩阵：

```text
Minecraft × Loader
```

即至少验证那 30 个目标。

CI 至少执行：

```text
core unit tests
compile
processResources
jar
Mixin validation where available
```

不能出现：

```text
1.21.1 build green
然后假定所有 1.21.x 都 green
```

每个 target 都要实际编译。

26.1.x 使用 Java 25 runner/toolchain。

1.21.x 使用 Java 21 target。

---

# 三十八、Gradle 策略

优先尝试共享 convention plugin。

但不同年代 Loom/NeoForge Gradle plugin 如果存在 Gradle wrapper 冲突：

**不要为了“单 Gradle wrapper”牺牲稳定性。**

允许采用 Gradle composite build：

```text
builds/legacy/
builds/modern/
```

甚至进一步按必要版本族拆分。

重要的是：

```text
one repository
shared engine
reproducible builds
all targets green
```

而不是强迫所有目标使用同一 plugin 版本。

必须提供统一入口，例如：

Windows：

```text
tools/build-all.ps1
```

Linux/macOS：

```text
tools/build-all.sh
```

最终执行后构建全部 artifacts。

---

# 三十九、Artifact 命名

推荐：

```text
cinematic-chat-<modVersion>+mc1.21.1-fabric.jar

cinematic-chat-<modVersion>+mc1.21.1-neoforge.jar

cinematic-chat-<modVersion>+mc26.1.2-fabric.jar

cinematic-chat-<modVersion>+mc26.1.2-neoforge.jar
```

如果多个 Minecraft 版本经过实际验证共用一个 binary，可调整名称，但 metadata 必须准确。

禁止 metadata 写：

```text
*
```

来假装兼容所有版本。

---

# 四十、不要提交构建产物

Git 仓库不得提交：

```text
build/
.gradle/
run/
logs/
crash-reports/
generated temporary files
IDE cache
release jars
```

正确维护 `.gitignore`。

---

# 四十一、文档

建立：

```text
README.md
docs/ARCHITECTURE.md
docs/CCML.md
docs/COMPATIBILITY.md
docs/PORTING.md
docs/TESTING.md
```

README 要明确写：

Cinematic Chat 是纯客户端模组。

服务器无需安装。

没有安装 Cinematic Chat 的玩家会看到 CCML 原始代码。

例如服务器：

```text
/say [speed=18]不要回头。[pause=1]\n继续向前。
```

普通客户端看到：

```text
[speed=18]不要回头。[pause=1]\n继续向前。
```

Cinematic Chat 客户端看到对应字幕演出。

---

# 四十二、CCML 文档

`docs/CCML.md` 必须完整记录：

```text
grammar
escaping
timeline tags
style tags
effects
nesting
limits
fallback
security
examples
```

不要让 CCML 行为只存在于源码里。

---

# 四十三、兼容矩阵

`docs/COMPATIBILITY.md` 必须生成实际表格：

```text
Minecraft | Fabric | NeoForge | Java | Build | Launch test
```

所有目标均需要状态。

不要写未经测试的：

```text
Supported
```

区分：

```text
Build verified
Launch verified
Gameplay verified
```

---

# 四十四、开发顺序

严格按阶段推进，不要一次写 30 套未经验证的代码。

### Phase 1 — Pure Java Engine

完成：

```text
AST
CCML Parser
Timeline
Animation model
Queue
Config model
JUnit
```

先做到无 Minecraft 依赖。

### Phase 2 — Reference Target

以：

```text
Minecraft 1.21.1 NeoForge
```

作为第一参考实现。

完成完整闭环：

```text
capture
history preserved
vanilla chat hidden
subtitle render
ChatScreen
CCML
config
```

这是当前最重要的 reference implementation。

### Phase 3 — 1.21.1 Fabric

复用 engine。

只实现 Fabric adapter。

此时验证 cross-loader architecture 是否真的成立。

### Phase 4 — 1.21.x Ports

按 Minecraft 迁移顺序：

```text
1.21
1.21.2
1.21.3
...
1.21.11
```

不要通过 copy-paste 整个项目完成。

每次只调整 version adapter。

### Phase 5 — 26.1 boundary

单独处理：

```text
Java 25
unobfuscated Minecraft
mapping changes
Fabric API changes
render changes
build changes
```

不要把这个阶段当作普通 patch update。

### Phase 6

完成：

```text
26.1
26.1.1
26.1.2
Fabric
NeoForge
```

### Phase 7

CI、文档、全矩阵测试、清理。

---

# 四十五、第一参考版本验收标准

Minecraft 1.21.1 NeoForge 首先必须达到：

1. 启动不崩溃。
2. 进入世界后普通 vanilla chat 不显示。
3. 玩家消息变成 HUD subtitle。
4. `/say` 变成 HUD subtitle。
5. 原版 ChatComponent 依然保存消息。
6. 按 T 打开完整聊天历史。
7. 可以正常发送聊天。
8. 可以正常发送 command。
9. command suggestions 正常。
10. Component click/hover 在 ChatScreen 中正常。
11. Signed Chat 没有被修改。
12. CCML 可以运行。
13. 非法 CCML 不 crash。
14. F1 隐藏字幕。
15. GUI Scale 改变后正常布局。
16. 中文正确。
17. Emoji 不出现 surrogate split。
18. 刷屏不会无限增长。
19. 打开 ChatScreen 时字幕暂停/隐藏。
20. 关闭 ChatScreen 后正常恢复。

在这 20 项没完成之前，不要开始大规模移植其他版本。

---

# 四十六、代码质量

禁止：

```text
God class
5000 行 renderer
大量 static mutable global state
Mixin 内业务逻辑
重复 parser
Fabric/NeoForge 各写一套 engine
字符串魔法常量散落
空 catch
catch Throwable 然后无日志
```

推荐：

```text
CinematicChatClient
SubtitleManager
SubtitleEngine
CcmlParser
SubtitleTimeline
SubtitleLayoutEngine
SubtitleRenderer
ConfigManager
ChatCaptureBridge
VersionBridge
```

保持明确职责。

---

# 四十七、线程安全

绝大部分 Minecraft UI 工作只在 client thread。

如果 message hook 所在线程不保证 client/render thread：

不要直接修改 render collection。

使用安全 enqueue：

```text
incomingQueue
```

然后在 client thread 消费。

不要在 render thread 上进行：

- 文件 IO
- config save
- 网络访问
- 大型 parser 工作

---

# 四十八、Render State

渲染时必须正确保存/恢复状态。

尤其老版本 Blaze3D/OpenGL 和新版本 rendering backend 变化很大。

不要依赖 raw OpenGL 调用。

优先使用对应 Minecraft 版本官方 GUI rendering API。

目标是未来 Vulkan 等后端变化时尽量少受影响。

---

# 四十九、版本差异必须局部化

业务代码禁止出现：

```java
if (minecraftVersion.equals("1.21.1")) ...
else if ...
else if ...
```

这种大量运行时版本判断。

Minecraft 版本在构建时已经确定。

差异应通过：

```text
version adapter
source set
interface implementation
```

解决。

---

# 五十、最终交付

完成后必须给我一个总结，内容包括：

```text
1. 项目架构
2. 已完成功能
3. CCML 语法
4. 聊天捕获方式
5. 原版 Chat HUD 隐藏方式
6. Fabric/NeoForge 差异
7. Minecraft 各版本差异
8. Java 21/25 构建处理
9. 测试结果
10. 30 target compatibility matrix
11. 当前已知兼容问题
12. 下一阶段建议
```

并列出所有关键文件。

不要只说“完成”。

提供实际 Gradle build/test 结果。

---

# 最终原则

这个模组不是：

```text
Pretty Chat
Chat Bubble
Discord Chat
Better Chat Box
```

它的定位是：

```text
Minecraft chat
       ↓
client-side presentation transformation
       ↓
cinematic HUD subtitle
       +
safe subtitle timeline language
```

正常游戏中：

**聊天框应该像不存在一样。**

但 Minecraft 原来的：

```text
chat history
signed chat
components
commands
suggestions
links
hover
reportability
```

仍然存在。

Cinematic Chat 只是重新定义：

**玩家“看到”聊天的方式。**

不要破坏 Minecraft 的聊天系统。

不要建立新的聊天系统。

只建立一个非常优秀的字幕表现层。

从 Phase 1 开始工作。

先审计/初始化仓库，然后实现 pure Java engine 和 Minecraft 1.21.1 NeoForge reference target。完成并实际通过测试后，再继续 Fabric 1.21.1 与完整版本矩阵。

不要因为工作量较大而停在规划阶段。

直接创建、修改、构建、测试代码，并持续修复编译错误和运行问题，直到当前能够验证的目标全部通过。