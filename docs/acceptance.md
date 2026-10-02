# 验收清单与证据边界

自动测试通过与完整验收通过分别记录。纯 Java 模型断言不能代替实体 Shift/Alt 操作、显示器比例检查或人工听感。开发账号无法证明已认证 Signed Chat。每个目标暂保留 `supported=false`。

Core 证据在 [core-tests.json](validation/core-tests.json)，Native 证据在 [matrix.json](validation/matrix.json)。Native 测试进入真实临时世界，调用原生 Screen 输入、Minecraft 音频 Channel 与 OpenAL；测试专用反射探针只在 smoke source set 中，发布 JAR 排除它们。

## 原需求第 45 节：参考版本 20 项

| # | 项目 | 实际证据与状态 |
|---|---|---|
| 1 | NeoForge 1.21.1 build | 独立 `:neoforge:build check` Pass |
| 2 | 实际启动 | `:neoforge:runSmoke` Pass，Mixin 必需注入启用 |
| 3 | 进入单人世界 | `entered_world` |
| 4 | 正常聊天显示字幕 | `capture_exactly_once`、`player_chat_captured`、`say_captured_structured` 与截图 |
| 5 | 正常游戏不显示原版 chat HUD | `hide_gameplay_chat` 与字幕截图 |
| 6 | ChatScreen 显示完整历史 | `restore_chat_screen`、`original_history_keeps_markup` 与历史截图 |
| 7 | 发送聊天 | `vanilla_chat_input_sent` |
| 8 | 发送 command | `vanilla_command_input_sent`，真实 `/say` |
| 9 | command suggestions | `client_command_suggestions_work` |
| 10 | Component click/hover | click 实际 handler 与 suggest 输入 Pass；hover 数据保留 Pass；实体指针 hover 的可见弹窗待人工 |
| 11 | Signed Chat 未修改 | Component/MessageSignature 对象身份保留 Pass；网络参数未改的源码证据；已认证服务器端到端待测 |
| 12 | CCML 运行 | Engine fixtures Pass，真实带标签消息与字幕截图 |
| 13 | 非法 CCML 不 crash | Core 恶意输入、`malformed_ccml_scenario_executed` |
| 14 | F1 隐藏字幕 | F1 截图，`f1_stops_pseudo_voice` 与恢复断言 |
| 15 | GUI Scale | 实际 GUI Scale 3 重建与截图；布局模型 Core |
| 16 | 中文 | 中文字幕截图、`chinese_language_resource_loaded` |
| 17 | Emoji 无 surrogate split | grapheme/输入截断 Core，真实家庭 emoji 消息 |
| 18 | 刷屏有界 | 100 条实际消息 `burst_remains_bounded`；队列 Core |
| 19 | ChatScreen 暂停/隐藏 | timeline 时间不推进、声音停止 Native；字幕隐藏源码与截图 |
| 20 | 关闭后恢复 | `resume_timeline`、声音恢复 Native，无补播错过的声音 Core |

1.21.1 Fabric 执行相同场景验证共享业务。参考版本的自动部分先于移植完成；由于认证测试环境与人工验收未具备，不能声称原需求“20 项全部验收后才迁移”的阶段门槛已完整满足。

## 原需求第 114 节：HUD 17 项

| # | 项目 | 已执行 | 尚待实际操作 |
|---|---|---|---|
| 1 | `/cchat hud` 打开 | 真实 ChatScreen Enter + 延迟打开 Native | — |
| 2 | 自由拖动 | 原生 Screen click/drag/release Native | — |
| 3 | Shift 水平锁 | 模型 Core | 实体 Shift 拖动 |
| 4 | Shift 垂直锁 | 模型 Core | 实体 Shift 拖动 |
| 5 | 不抖动切轴 | 3 px 阈值、锁轴持续与状态切换 Core | 实体按键切换 |
| 6 | 左右 Resize | 左右模型 Core、两个原生手柄 Native | — |
| 7 | Scale Handle | 原生角落手柄 Native | — |
| 8 | 中心吸附 | 模型 Core、原生释放吸附 Native | — |
| 9 | 九 Anchor | 九种模型 Core、九种原生点击 Native | — |
| 10 | GUI Scale | 原生 Scale 切换与布局截图 | 更多 GUI Scale 组合 |
| 11 | 分辨率变化 | Core 与六种实际窗口重设后的归一化 Native | — |
| 12 | 21:9 | 比例 Core、2520×1080 实际窗口/截图 Native | — |
| 13 | 4:3 | 比例 Core、1600×1200 实际窗口/截图 Native | — |
| 14 | Cancel | 工作副本不污染 live config、Cancel Native | — |
| 15 | Apply | 原生点击与 live config 断言 Native，异步原子保存 Core | — |
| 16 | Reset | 三种独立重置 Core、三个原生按钮及视觉隔离 Native | — |
| 17 | 编辑器不发送 packet | 客户端命令消费和 editor 无网络调用源码审阅 | 外部 packet 观测未执行 |

Alt 临时取消吸附、安全区边缘吸附、六类比例、预设与 CCML/voice 隔离均有 Core 测试；六类比例另有实际窗口与截图证据。当前原生输入场景调用实际 Screen API，不把模型测试描述为实体键鼠测试。

## 原需求第 115 节：Voice 25 项

| # | 项目 | 实际证据与状态 |
|---|---|---|
| 1 | 新建 Profile | 原生 New→Profile Save→Voice Save→reload→Delete 完整 Native 流程 |
| 2 | 输入 speaker alias | 原生 EditBox responder 填写中文/英文 alias Native；实体键盘输入未测 |
| 3 | 添加 OGG | 原生文件列表按钮依次选择三个 OGG，Native 解码和播放 |
| 4 | speaker 匹配播放 | Core；彩色中文 ChatType 姓名匹配固定 entry profile 并真正播放 Native |
| 5 | 多 OGG 随机 | shuffle Core；原生声线使用三个 OGG buffer Native；人工随机试听待测 |
| 6 | 不连续重复 | 100 次跨轮 shuffle Core |
| 7 | volume | OpenAL AL_GAIN=.4 Native |
| 8 | pitch | OpenAL AL_PITCH=1.2 Native |
| 9 | pitch randomness | fake backend 参数范围、随机事件跨 flush 频率 Core；人工试听待测 |
| 10 | interval | 逻辑时间节流、参数随机化、30/60/144/240 FPS Core |
| 11 | pause 不播放 | Runtime 事件与 LOOP 暂停 Core |
| 12 | wait | 有限 reveal/entry 语义 Core；持续效果不阻塞 |
| 13 | clear 停止 | fake backend + 迟到 native future 停止 Core；native buffer reload 释放 |
| 14 | replace | entry 局部替换和 voice clear 事件 Core |
| 15 | ChatScreen 声音 | Native 打开停止、关闭恢复；无补播 Core |
| 16 | 文件不存在 | `missing_audio_fails_without_crash` Native、失败缓存 Core |
| 17 | 文件损坏 | 真正的 JOrbis 损坏 OGG Native、失败缓存 Core |
| 18 | reload 新文件 | 异步代次/失败重试 Core；原生缓冲清理和重建，GUI 新配置保存/重载 Native |
| 19 | 未绑定不播放 | 默认 None 与 resolver/runtime Core |
| 20 | 连续多玩家不串 | 固定 entry voiceProfileId、speaker/随机参数隔离 Core |
| 21 | 中文 speaker | 中文 display/name/alias Core，实际中文 alias 匹配与播放 Native |
| 22 | Component 彩色名字 | 匹配键与原消息分离 Core；金色 Component 姓名与中文 alias 匹配 Native；人工试听待测 |
| 23 | `/say` sender | ChatType 结构化来源 Native，按服务器启用文本规则 Core |
| 24 | 两 Loader 一致 | 同一 Core、同一 Native 场景与逐目标断言 |
| 25 | 所有目标安全启动 | 30 行独立实际启动与临时世界场景；不是全面兼容声明 |

声音参数直接读取实际声道验证，不能据此宣称完成听感验收。背景解码、缺失文件和损坏文件不会停止字幕时间线；没有音频资源时仍可正常启动和显示字幕。

## 仍需外部环境或人工的检查

1. 在已认证客户端与开启签名验证的服务器验证聊天、举报、撤回、屏蔽和权限变化，并保留服务端证据。当前场景使用离线开发 profile。
2. 按上表补齐实体 Shift/Alt、hover 弹窗、全控件及全 CLI 路径和人工听感。六种实际窗口比例和声线完整新建/选择/保存/重载/删除已自动执行，但没有实体键盘输入或人工听感证据。
3. 安装代表性聊天模组与自定义字体资源包进行组合测试，完成长时刷屏/内存/帧耗时测量。
4. GitHub Actions 的首次 Linux 运行已失败，修复与复测状态见 [CI 记录](ci-status.md)；macOS 仍待实际验证。

上述项目未获得结果前，不将任何目标标记为 Supported。
