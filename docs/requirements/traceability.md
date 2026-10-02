# 116 节需求追踪

编号与标题来自两份原始需求。这是实现定位和证据索引，不代表每节已通过完整人工验收。Core = 纯 Java 自动测试；Native = 真实 Minecraft 临时世界场景；源码 = 已实现且可审阅，未声称执行该项端到端操作。

统一测试源码：[EngineTest](../../engine/src/test/java/io/github/axiraa/cinematicchat/engine/EngineTest.java)、[RequiredFixtureTest](../../engine/src/test/java/io/github/axiraa/cinematicchat/engine/RequiredFixtureTest.java)、[ClientCommonTest](../../client-common/src/test/java/io/github/axiraa/cinematicchat/client/ClientCommonTest.java)、[VoiceRuntimeTest](../../client-common/src/test/java/io/github/axiraa/cinematicchat/client/VoiceRuntimeTest.java)、[ClientScenario](../../adapters/common/minecraft/src/smoke/java/io/github/axiraa/cinematicchat/smoke/ClientScenario.java)。

实际结果：[30 目标矩阵](../validation/matrix.md)、[验收条目](../acceptance.md)、[交付报告](../final-report.md)。

| 编号 | 原需求标题 | 实现定位 | 自动证据 / 验证限制 |
|---|---|---|---|
| 1 | 一、必须支持的 Minecraft / Loader 范围 | targets.json / 版本 adapter | 30 行 Build/Launch/自动世界场景；Supported 保持 false |
| 2 | 二、开始开发前必须完成的研究 | docs/porting / API audit | 逐目标源码签名及官方 primer；研究记录在移植文档 |
| 3 | 三、工程架构 | minecraft-api / engine / client-common | verifyIsolation、verifyModJar、独立 Loader 编译 |
| 4 | 四、不要加入 Architectury 等强制运行时依赖 | Loader metadata / fat JAR | verifyModJar；共享核心内置，无 Architectury 运行时 |
| 5 | 五、聊天处理基本原则 | ChatCaptureBridge / ChatComponentMixin | 原 Component/签名引用、过滤、撤回、普通发送的 Native 断言；认证 Signed Chat 待测 |
| 6 | 六、原版聊天 HUD 行为 | NeoForge GUI layer / Fabric gate | gameplay 隐藏、ChatScreen 历史恢复、F1 Native 场景 |
| 7 | 七、消息捕获 | ChatCaptureBridge / SubtitleController | 唯一捕获点、相同文本两次真实消息、结构化 /say 与玩家聊天 Native 断言 |
| 8 | 八、Mixin 策略 | 版本 Mixin 与 mixins.json | 每目标实际启动；required/defaultRequire=1，精确签名 |
| 9 | 九、字幕默认视觉设计 | CinematicConfig / SubtitleRenderer | 默认视觉源码、字幕/HUD 截图；全部主观视觉评价待人工 |
| 10 | 十、字幕队列 | SubtitleQueue | 队列上限、优先级淘汰、消息 ID Core；100 条刷屏 Native |
| 11 | 十一、显示时间 | EngineConfig / SubtitleRuntime | 阅读时间与 hold Core；逻辑时钟不依赖帧数 |
| 12 | 十二、文字 Reveal 模式 | UnicodeUnits / SubtitleTimeline | 四种 Reveal、英文单词、CJK、Emoji/组合字符 Core |
| 13 | 十三、字幕脚本语言 | CcmlParser / SubtitleNode | 有界扫描器与嵌套 AST；合法/非法标签 Core |
| 14 | 十四、转义 | CcmlParser | 反斜杠、括号、换行和源码区间 Core |
| 15 | 十五、Timeline 指令 | SubtitleTimeline / SubtitleRuntime | pause/wait/hold/speed/reveal/clear/replace/erase/fade Core |
| 16 | 十六、局部 Style / Effect 标签 | CcmlParser / TextStyle | 嵌套局部样式、基础 Component 样式叠加 Core |
| 17 | 十七、效果参数 | CcmlParser / CcmlLimits | shake/wave 属性和 clamp Core |
| 18 | 十八、Parser 安全规则 | CcmlLimits / verifyIsolation | 绝对限制、深度、超长输入、2000 个随机输入、累计时限 Core |
| 19 | 十九、Parser 错误处理 | CcmlParser.parse | 未知/错误标签保留，异常回退；非法消息 Native 场景 |
| 20 | 二十、Vanilla Component Style | ComponentBridge / StyledComponentBridge | 样式边界 Core；原 Component/click/hover 数据 Native；复杂资源字体视觉待测 |
| 21 | 二十一、字体 | 版本 StyledComponentBridge / FontBridge | 字体 ID 与 Sprite/Atlas 桥接源码；实际自定义资源包字体待测 |
| 22 | 二十二、自动换行 | SubtitleLayoutEngine / SubtitleViewport / renderer | 像素换行、CJK、长词、长文本有界视口 Core/Native |
| 23 | 二十三、字幕动画 | Animation / SubtitleStackLayout | 入场/退场、独立 easing、堆叠平移 Core；实际字幕截图 |
| 24 | 二十四、配置系统 | ConfigManager / CinematicConfig | 异步原子保存、工作副本、损坏/未来版本备份、重载顺序 Core |
| 25 | 二十五、CCML Trust Mode | CinematicConfig.parseMarkup / effectiveLimits | OFF/SAFE/SYSTEM_ONLY/ALL 与 UNKNOWN 边界源码及 Core |
| 26 | 二十六、Config Screen | CinematicConfigScreen | 原生分类与中文 Native 打开/截图；全部控件人工遍历待测 |
| 27 | 二十七、Presets | HudPresets | 六个内置预设与视觉隔离 Core |
| 28 | 二十八、客户端命令 | FabricCommands / ClientCommands | 客户端消费；HUD 命令、补全、原版输入 Native；所有 CLI 路径源码 |
| 29 | 二十九、Debug Mode | CinematicChatClient debug / parser commands | 队列、来源、位置与解析诊断源码；debug UI 人工检查待测 |
| 30 | 三十、兼容其他聊天模组 | 最终历史 hook / 不修改原消息 | 最小接入源码；其他聊天模组组合未安装实测 |
| 31 | 三十一、Accessibility | Accessibility / Animation | Reduced Motion、高对比、字号、INSTANT 源码；辅助技术人工体验待测 |
| 32 | 三十二、性能要求 | 解析/分段/度量/布局缓存 | 有界队列与 Core；真实游戏长时性能基准未测 |
| 33 | 三十三、Resource Reload | Loader reload listener / invalidateRenderer | 语言切换与资源重载 Native；音频代次 Core/Native |
| 34 | 三十四、CCML 示例测试 | RequiredFixtureTest / EngineTest | 原需求完整 A–H fixture、fade 范围与演出语义 Core |
| 35 | 三十五、Core Unit Tests | EngineTest / ClientCommonTest / VoiceRuntimeTest | 纯 Java 测试结果汇总 core-tests.json |
| 36 | 三十六、Minecraft Integration Tests | ClientScenario / NativeSmokeProbes | 每目标真实临时世界、日志、JSON 与 17 张截图 |
| 37 | 三十七、版本兼容 CI | .github/workflows/matrix.yml | 完整 30 行 CI；首次远端运行依赖校验失败，当前状态见 docs/ci-status.md |
| 38 | 三十八、Gradle 策略 | legacy / modern / convention scripts | Gradle 9.2.1/9.4.0，Java 21/25，锁文件与 SHA-256 严格构建 |
| 39 | 三十九、Artifact 命名 | target-properties.gradle / verifyModJar | 每 MC/Loader 独立命名；dist JAR hash |
| 40 | 四十、不要提交构建产物 | .gitignore / .gitattributes | 构建/运行/缓存不入 Git，脚本行尾规则；暂存清单检查 |
| 41 | 四十一、文档 | README / docs | 架构、构建、使用、移植、验收与交付报告 |
| 42 | 四十二、CCML 文档 | docs/ccml.md | 语法、错误处理、安全边界和例子 |
| 43 | 四十三、兼容矩阵 | docs/validation/matrix.json | 实际命令、时间、断言、hash；未推断 Supported |
| 44 | 四十四、开发顺序 | 参考版本与矩阵执行记录 | 参考版本先实现再迁移；认证与人工验收未完成，不能宣称阶段门槛全部满足 |
| 45 | 四十五、第一参考版本验收标准 | docs/acceptance.md 参考 20 项 | 逐项区分自动证据与待测 Signed Chat/hover 视觉 |
| 46 | 四十六、代码质量 | 职责分离 / 版本 override | 源码审阅、verifyIsolation；Mixin 仅桥接，业务复用 |
| 47 | 四十七、线程安全 | incoming queue / IO executor / audio executor | 线程所有权源码；保存/重载顺序与迟到音频完成 Core |
| 48 | 四十八、Render State | GuiPose / SubtitleRenderer | 官方 GUI API 与 pose push/pop；实际所有目标渲染 |
| 49 | 四十九、版本差异必须局部化 | targets/<版本> / modern source transform | 编译期版本差异，不在 engine 使用版本判断 |
| 50 | 五十、最终交付 | docs/final-report.md | 12 项交付总结、关键文件、实际结果与限制 |
| 51 | 五十一、可视化 HUD 编辑器 | SubtitleHudEditorScreen | 原生 Screen、/cchat hud Native；独立预览队列 |
| 52 | 五十二、字幕 HUD 可拖拽 | HudEditorModel / SubtitleHudEditorScreen | 自由拖动与工作副本 Native，归一化坐标 Core |
| 53 | 五十三、Shift 约束拖动 | HudEditorModel | 水平/垂直锁、3 px 阈值、Shift 切换无跳变 Core；实体按键待测 |
| 54 | 五十四、HUD Resize | HudEditorModel WIDTH_LEFT/RIGHT | 左右宽度 Core、两个原生手柄 Native；不改变 scale |
| 55 | 五十五、字幕整体缩放 | HudEditorModel SCALE | 整体缩放 Core 与原生角落手柄 Native |
| 56 | 五十六、尺寸限制 | HudSettings.validate | 宽度/scale 限制及非法值 Core |
| 57 | 五十七、默认锚点 | HudSettings.Anchor | 默认底部中心 78%、九个锚点源码/Core |
| 58 | 五十八、Anchor Snap | HudEditorModel.end / anchor hit targets | 九锚点、中心吸附 Core；九锚点点击与中心释放 Native |
| 59 | 五十九、辅助吸附线 | SubtitleHudEditorScreen.render | 中心线、安全区、锚点/手柄 Native 截图 |
| 60 | 六十、关闭吸附 | HudSettings.snap / hasAltDown | 开关与 Alt 绕过 Core；实体 Alt 操作待测 |
| 61 | 六十一、Safe Area | HudEditorModel safe edges | safe area 边缘吸附 Core、辅助线 Native 截图 |
| 62 | 六十二、GUI Scale 自适应 | HudSettings 归一化坐标 / renderer | GUI Scale Native；六类比例 Core |
| 63 | 六十三、编辑器操作提示 | SubtitleHudEditorScreen help text | 中英提示与位置/宽度/scale 数据 Native 截图 |
| 64 | 六十四、Reset | resetPosition / resetSize / Reset HUD | 独立重置与预设恢复 Core；三个原生按钮与视觉隔离 Native |
| 65 | 六十五、Apply / Cancel | Screen working copy / ConfigManager | HUD Cancel/Apply Native；配置原子保存 Core |
| 66 | 六十六、伪配音系统 | PseudoVoiceController / PlatformAudioBackend | 默认可选、Reveal→声音 Core；native WAV/OGG 播放 |
| 67 | 六十七、Voice Profile | VoiceProfile / VoiceProfilesScreen | 参数模型和 GUI；原生编辑打开/Test Talking/Cancel |
| 68 | 六十八、Voice ID | VoiceProfile.validate / editor ID check | 本地 ID 校验、重复 ID 拒绝源码 |
| 69 | 六十九、说话者匹配 | VoiceProfileResolver | UUID/profile/display/alias/fallback 优先级 Core |
| 70 | 七十、玩家名与 /say 说话者 | ChatCaptureBridge | 结构化玩家与 /say sender Native；可靠分离失败保留完整 Component |
| 71 | 七十一、/say | ChatType.SAY_COMMAND / per-server rule | 结构化 /say Native，按服务器规则 Core；不默认猜文本 |
| 72 | 七十二、不要只按 UUID 配音 | VoiceProfileResolver | UUID、中文名、alias、display name Core |
| 73 | 七十三、Voice Profile 编辑界面 | VoiceProfileScreen | 三页参数，原生打开/滚动/取消/新建/保存/删除 Native |
| 74 | 七十四、添加音频 | LocalChoiceScreen / ExternalAudioManager.list | 只列 audio 内文件；原生选择三个 OGG 按钮 Native |
| 75 | 七十五、配置目录 | ConfigManager / installTestAudio | config/cinematic_chat 三文件与 audio；原生安装原创测试音 |
| 76 | 七十六、路径安全 | ExternalAudioManager.resolve | 绝对路径、穿越、NUL、目录链接/逃逸拒绝 Core |
| 77 | 七十七、音频格式 | WAV converter / JOrbis adapter | 整数 PCM 8/16/24/32 样本 Core；真实 WAV 与三 OGG 解码 Native |
| 78 | 七十八、不要把配置音频注册成资源包 SoundEvent | MinecraftAudioBackend | 直接 Minecraft Channel/SoundBuffer 桥接；无外置 SoundEvent 注册 |
| 79 | 七十九、跨版本音频抽象 | PlatformAudioBackend / 版本 accessor | 所有目标 native 播放、音量/pitch、释放断言 |
| 80 | 八十、音频缓存 | ExternalAudioManager | 异步、容量/解码/条目限制、失败缓存与代次 Core |
| 81 | 八十一、多音频随机播放 | VoiceShuffleBag / PseudoVoiceController | shuffle bag、跨轮不重复与随机参数 Core |
| 82 | 八十二、Pseudo Voice Play Modes | PseudoVoiceController | 五模式、INSTANT 合并、暂停/恢复 Core；LOOP Native |
| 83 | 八十三、推荐默认伪配音算法 | VoiceProfile 默认 / Reveal events | 65 ms、跳标点、CJK 分组默认及事件节流 Core |
| 84 | 八十四、标点与空白 | PseudoVoiceController.event | 空白/标点跳过与可配置开关 Core |
| 85 | 八十五、CJK 伪配音 | UnicodeUnits / cjkCharactersPerVoice | grapheme、CJK 每两字，逐字/逐词声音 Core |
| 86 | 八十六、声音 Pitch | VoicePlaybackParams / backend | pitch clamp/randomness Core，实际 AL_PITCH Native |
| 87 | 八十七、Volume | VoiceProfile / masterVolume / backend | profile×master×VOICE，实际 AL_GAIN Native |
| 88 | 八十八、Minecraft 音量类别 | MinecraftAudioBackend.tick | VOICE 音量更新活动声道 Native |
| 89 | 八十九、字幕停止时的声音处理 | PseudoVoiceController.clear / entry events | clear/replace/聊天/F1/断线生命周期 Core/Native |
| 90 | 九十、多个说话者连续发言 | 固定 entry voiceProfileId | 多个 speaker 隔离、参数及顺序 Core |
| 91 | 九十一、同名问题 | 匹配顺序 / UUID first | 同名 UUID 优先 Core；不根据消息措辞猜身份 |
| 92 | 九十二、Voice Profile Fallback | VoiceConfig fallback None | 默认无 fallback 源码/Core；未绑定不播放 Core |
| 93 | 九十三、说话者名称标准化 | VoiceProfileResolver.normalize | 格式、空白、大小写匹配键 Core，原 Component 保留 Native |
| 94 | 九十四、Voice Preview | VoiceProfileScreen.test / independent preview | Native Test Talking；Preview 与临时队列源码 |
| 95 | 九十五、缺失音频 | ExternalAudioManager failure cache | 缺失文件 Core 与真实 missing OGG Native，不阻塞字幕 |
| 96 | 九十六、损坏音频 | ExternalAudioManager decoder guard | 失败缓存 Core；真实损坏 OGG Native |
| 97 | 九十七、音频热重载 | reload generation / reloadAudio | 迟到结果丢弃 Core；native 释放/重建 buffer |
| 98 | 九十八、Voice CLI | 客户端 voice 子命令 | list/reload/test/assign 源码；完整子命令人工遍历待测 |
| 99 | 九十九、Voice Profile GUI 新建流程 | VoiceProfilesScreen → VoiceProfileScreen | New/alias/三 OGG 选择/两层 Save/reload/Delete 完整 Native |
| 100 | 一百、Voice 与 CCML | entry voice binding / timeline events | CCML 演出触发 Core；不允许脚本访问文件或发包 |
| 101 | 一百零一、Reveal Event Bus | SubtitleRuntimeEvent | 逻辑时间事件仅一次；30/60/144/240 FPS 与不同 flush 频率 Core |
| 102 | 一百零二、声音不得影响字幕性能 | IO decode worker / ready clip skip | 后台读取与失败跳过 Core/Native；长时性能测量待测 |
| 103 | 一百零三、字幕 HUD Preset 保存 | HudPresets.Visual.capture | 用户 preset 仅视觉、工作副本、无音频/信任污染 Core |
| 104 | 一百零四、多屏幕比例 | HudSettings / layout model | 16:9/16:10/21:9/32:9/4:3/小窗口 Core 与实际窗口/截图 Native |
| 105 | 一百零五、HUD Editor 代码结构 | HudEditorModel / Screen / shared renderer | 职责分离源码，正常 HUD 与预览复用渲染 |
| 106 | 一百零六、Pseudo Voice 代码结构 | resolver / event controller / cache / backend | 四层职责分离，只有 backend 依赖 MC 音频 |
| 107 | 一百零七、Voice JSON | VoiceConfig / VoiceProfile JSON | voices.json schemaVersion 1、匹配顺序与参数源码/Core |
| 108 | 一百零八、隐私原则 | 本地配置与音频路径 | 无上传、遥测或声线网络协议；源码审查 |
| 109 | 一百零九、声音资源版权 | audio/SOURCES.txt / generate-test-* | 仅原创短合成音、MIT 来源说明 |
| 110 | 一百一十、无音频资源也必须正常工作 | 默认关闭 / None / unready skip | 默认空音频启动与字幕 Native；缺失资源 Core/Native |
| 111 | 一百一十一、新增设置页面布局 | CinematicConfigScreen 分类 / voice GUI | General/HUD/Typography/Animation/Message/CCML/Voice/Accessibility/Advanced 源码 |
| 112 | 一百一十二、首次打开 HUD Editor | HUD bounds/anchors/lines/help | 首次打开真实编辑器截图；提示不遮挡主要字幕 |
| 113 | 一百一十三、编辑期间模拟动画 | preview controller / Replay Preview | 独立动画预览与可选声音；Native 编辑预览截图 |
| 114 | 一百一十四、实际验收测试：HUD Editor | docs/acceptance.md HUD 17 项 | 模型/原生六种窗口及操作/待实体按键分别列出 |
| 115 | 一百一十五、实际验收测试：Pseudo Voice | docs/acceptance.md Voice 25 项 | fake backend/原生新建选择保存重载删除/待实体输入及听感分别列出 |
| 116 | 一百一十六、最终设计原则补充 | 共享 renderer + 可视编辑 + 本地 voice | 实现设计原则；未将部分自动回归宣称全面验收 |
