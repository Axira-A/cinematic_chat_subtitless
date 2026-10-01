# HUD 与本地伪配音

HUD 编辑器与正常字幕共用布局和 renderer。默认底部中心锚点、屏高 78%，宽度为屏宽 45% 与 900 逻辑像素的较小值。坐标和宽度以归一化值保存，整体 scale 0.5–3.0。

拖动字幕框移动；Shift 移动超过 3 逻辑像素后锁定当前主轴，松开 Shift 恢复自由拖动。左右手柄改变换行宽度，右下角手柄调整整个 HUD 的字体、姓名、间距和留白比例。九个锚点、中心辅助线和安全区只在编辑模式显示。吸附可以关闭，Alt 临时关闭。

六个内置视觉预设为 Cinematic、Minimal、RPG、Adventure、Accessible、Vanilla-like。用户 HUD 预设只保存显示参数，不包含声线、来源过滤或 CCML 信任。Save Preset 先写入编辑副本，Apply 后持久化；从配置页打开时，继续保留在配置页工作副本中，直到配置页最终 Save。Reset Position、Reset Size、Reset HUD 分别恢复位置、宽度/缩放和整套视觉参数。入场与退场 easing 可分别设置。

## 声线

声线默认关闭，Player/NPC/System fallback 均为 None。创建 Profile、填写本地 ID 与说话者名、从 audio 目录选择声音，保存后再启用。UUID 优先于 profile name、display name、alias、fallback；匹配键可去格式、去两端空白和忽略大小写，原消息不变。

播放模式：OFF、ON_SUBTITLE_START、PER_WORD、PER_REVEAL_UNIT、LOOP_WHILE_REVEALING。默认逐揭示单位、65 ms 最小间隔、跳过空白和标点；逐字/逐词声音模式均默认每两个 CJK grapheme 分组。INSTANT 按字/按词合并成至多一次。shuffle bag 可避免跨轮紧邻重复，声线固定在入队 entry 上。

音量乘以伪配音总音量及 Minecraft VOICE 音量，正在播放的声道也随 VOICE 滑块更新。pitch 范围 0.5–2.0；Profile 可设置音量、pitch 和 interval 随机变化。声音只接受 `audio/` 中相对路径的 OGG Vorbis 与 WAV 8/16/24/32-bit 整数 PCM 单/双声道；WAV 转换为 16-bit PCM。路径穿越、绝对路径和可检测的符号链接逃逸被拒绝。压缩文件上限 8 MiB、单个解码上限 16 MiB、总 PCM 缓存上限 64 MiB、缓存条目上限 512。

音频页可安装原创 `cinematic-test-blip.wav` 和 `cinematic-test-blip-1/2/3.ogg` 四个测试音。它们是 100 ms 合成正弦波，MIT 授权，生成脚本和来源说明随项目保留。OGG 生成时使用的可选编码工具不属于模组运行依赖。音频目录为空时字幕照常工作。

Preview Sound 与 Test Talking 使用独立测试会话；聊天框、禁用、clear/replace、世界切换和断开连接停止对应声音。reload 会清除失败缓存并丢弃旧异步结果。
