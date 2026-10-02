# 实际验证记录

2026-10-02，本机环境为 Windows 11，Temurin Java 21.0.9 / 25.0.1，RTX 5070 Ti Laptop，OpenGL 4.6，NVIDIA 616.56，Realtek OpenAL 音频设备。以下分别记录本机与 Ubuntu 24.04 GitHub Actions 的实际检查，不是完整支持声明。

## 构建与纯 Java

legacy Gradle 9.2.1、modern Gradle 9.4.0 均执行独立构建。共享核心以 `--release 21` 编译；26.1.x adapter/Loader 为 Java 25。Wrapper JAR 与 distribution pin 通过 `python tools/verify/verify-wrapper.py`。

`check` 覆盖 EngineTest、RequiredFixtureTest、ClientCommonTest、VoiceRuntimeTest，共 53 个不同测试；legacy、modern 入口分别执行且失败/错误/跳过为 0。原需求 A–H 的完整字符串各自作为 fixture 执行。测试方法、时间与数量见 [core-tests.json](validation/core-tests.json)。覆盖：

- 全部 CCML 指令、嵌套样式、转义、源码区间、非法/未知标签、2048 节点/16 深度/32768 输入上限、2000 个随机恶意输入、累计演出上限与 Unicode grapheme。
- 单条时间线与多条队列晋升在 30/60/144/240 FPS 的事件轨迹一致；渲染插值不发送额外声音事件或跨越破坏性指令。
- 像素换行与长词、连续堆叠动画、配置损坏/未来版本备份、保存与后台重载顺序、工作副本、独立 easing 与视觉预设隔离。
- HUD 水平/垂直锁轴及 Shift 状态切换、左右 resize 与 scale 分离、九锚点、安全区/Alt、16:9/16:10/21:9/32:9/4:3/小窗口的归一化模型。
- 说话者匹配、五种声音模式、随机 shuffle 跨轮不重复、CJK/标点与节流、INSTANT 合并、多 speaker 隔离、暂停/clear/replace、reload 竞态和迟到 native future 停止。
- 音频路径、失败缓存、PCM 8/16/24/32 的实际样本转换与后台解码。

所有最终构建开启 SHA-256 依赖校验，不使用 `--write-verification-metadata`。传递依赖锁定按共享模块和独立目标保存。

2026-10-02 的兼容层整改后，30 个目标重新构建并进入真实客户端场景，输出 510 张截图。最终仓库解析策略下又独立复查全部 30 个 build、smokeClasses 和 check，全部通过，JAR SHA-256 与各自场景测试产物完全一致。`matrix.json` 的 `finalBuildVerification` 保存此复查的命令、时间和日志 hash。本次构建没有写入或绕过校验基线。

## 真实客户端自动场景

30 个目标分别执行 `:<loader>:build check` 和 `:<loader>:runSmoke`，进入新建临时单人世界，不使用现有存档。每个目标独立输出 JSON、日志和截图。最新 1.21.x 场景含 98 个布尔断言/操作记录，26.1.x 额外验证权限准入过滤；确切数量以各目标 JSON 为准。

实际检查包括：唯一历史捕获、相同文本两次消息、原 Component 和 MessageSignature 身份、历史标签、click/hover 数据、实际 suggest-command click handler、命令补全、普通聊天与 `/say` 发送、结构化说话者、过滤和撤回、ChatScreen 暂停/恢复、F1、100 条消息、语言/资源与后台配置重载、HUD 拖动/左右宽度/缩放/Cancel/Apply、九锚点点击、中心吸附、三个 Reset 按钮、实际 16:9/16:10/21:9/32:9/4:3/小窗口与归一化位置，以及完整声线新建、EditBox 中文 alias、文件选择器添加三个 OGG、保存、重载、彩色中文姓名匹配播放和删除。

音频场景真正解码原创 WAV 和三个 OGG，调用 Minecraft Channel/SoundBuffer，并在音频线程观察 OpenAL：gain=.4、pitch=1.2、loop 正在播放；将 VOICE 音量改为 .25 后活动声道 gain=.1。验证 F1/ChatScreen 停止与恢复、reload 后缓冲释放/重建、缺失与损坏 OGG 不使字幕或客户端崩溃。这些参数和生命周期检查不替代人工听感。

## 30 目标结果与产物

完整逐行 Build、Launch、Gameplay、断言数量、加载器版本和 JAR SHA-256 见 [matrix.md](validation/matrix.md)。实际命令、UTC 时间、全部断言名称、日志和截图 hash 见 [matrix.json](validation/matrix.json)。没有根据一个 patch 版本的结果推断另一个版本。

本机缓存构建命令形式为：

```powershell
.\tools\build\build-matrix.ps1 -Smoke -DependencyCache 'tools/.cache/maven' -NeoFormCache 'tools/.cache/neoformruntime' -Offline
```

统一命名的 30 个 JAR、日志、场景报告和 `SHA256SUMS` 位于被 Git 忽略的 `dist/`。原始截图位于各目标 `run-smoke/screenshots/`。源码、锁文件、校验清单、文档及 CI 纳入版本管理。

## Linux CI

提交 `09d6e966afe1ab5d30ea5132312a062102c21d08` 的 [完整 CI](https://github.com/Axira-A/cinematic_chat_subtitless/actions/runs/36959424117) 已完成：核心测试与 30/30 个目标的独立构建、启动和临时世界自动场景全部通过。每个任务的状态、时间、JAR SHA-256、日志 hash 和上传证据标识保存于 [ci-matrix.json](validation/ci-matrix.json)，历史失败和修复保留在 [CI 记录](ci-status.md)。Linux 使用 Xvfb、Mesa 与 OpenAL null backend，未据此宣称完成实体输入或人工听感验证。

## 已修复的问题与运行警告

早期 native reload 曾在未释放停止声道时删除 buffer，出现 OpenAL Invalid operation；改为停止、调度 Minecraft 清理、再删除缓冲后，重新运行原生场景验证。Fabric 客户端命令曾在发送关闭 ChatScreen 时覆盖新 HUD 界面；`ClientTasks` 延后打开，实际 `/cchat hud` Enter 场景验证修复。

初次 26.1.2 解编译遇到本机 Windows 虚拟内存不足；串行处理和复用已完成的中间结果后独立构建成功。严格矩阵发现 Loom 多 SHA-256 合并遗漏，以及本地生成映射/游戏 ZIP 的时间戳变化；按每个版本 Mojang 输入 hash 核对，并改为逐个列明的派生 ZIP 例外加完整内容/输入校验任务。错误内容 pin、错误输入 pin 均实际触发失败，恢复后通过；证据在 [映射探针](validation/generated-mappings.json)、[游戏探针](validation/generated-game.json) 和 [15 个 Fabric 严格复查](validation/generated-game-builds.json)。受影响目标重新执行，失败运行不计为通过。

开发日志中的离线 Realms 认证、部分原版 shader/texture 警告、可选 Mixin decompiler 缺失，以及测试故意制造的缺失/损坏 OGG WARN 均保留；不把它们当作认证服务器或第三方模组兼容证明。

整改回归期间，NeoForm 缓存写满 C 盘，导致六个构建无法写入中间结果。将中间缓存完整保留到 E 盘的忽略目录，恢复 C 盘空间，并使用 `-PneoFormCache=tools/.cache/neoformruntime` 后逐目标补测通过；没有将磁盘失败计为成功。

## 未完成的外部及人工验收

已认证 Signed Chat、实体 Shift/Alt、hover 弹窗、全部配置/命令路径、人工听感、自定义字体资源包、其他聊天模组组合、长时性能及 macOS 验证仍待执行；Linux CI 的实际结果见 [CI 记录](ci-status.md)。逐项状态见 [acceptance.md](acceptance.md)；116 节定位见 [traceability.md](requirements/traceability.md)。所有目标仍为 `supported=false`。
