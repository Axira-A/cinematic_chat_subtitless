# 五十一、可视化 HUD 编辑器

新增一个专门用于调整字幕显示区域的客户端可视化编辑模式。

客户端命令：

```text
/cchat hud
```

或：

```text
/cinematicchat hud
```

执行后打开：

```text
SubtitleHudEditorScreen
```

这不是普通配置表单，而是一个类似游戏 UI 编辑器的实时画布。

进入后必须直接显示字幕预览，例如：

```text
            Axira
      北边的门打不开。

            Honma
      那就从塔楼过去。
```

玩家可以直接使用鼠标调整 HUD。

---

# 五十二、字幕 HUD 可拖拽

字幕显示区域必须存在一个可视化 Bounding Box。

默认：

```text
x = 50%
y = 78%
anchor = BOTTOM_CENTER
```

玩家鼠标移动到 Bounding Box 后：

```text
左键按住
→ 拖动整个字幕 HUD
```

拖动必须实时更新预览。

不得要求：

```text
修改数字
保存
退出
重新进入游戏
```

才能看到结果。

所有改变均实时 Preview。

---

# 五十三、Shift 约束拖动

拖动字幕 HUD 时：

```text
普通拖动
→ X / Y 自由移动

Shift + Drag
→ 锁定单一轴移动
```

行为要求：

当玩家开始拖动后，计算：

```text
abs(deltaX)
abs(deltaY)
```

如果：

```text
abs(deltaX) > abs(deltaY)
```

则：

```text
锁定 Y
仅水平移动
```

否则：

```text
锁定 X
仅垂直移动
```

在 Shift 持续按住期间保持该轴锁定。

松开 Shift：

恢复自由拖动。

不要每一帧重新切换锁定方向导致抖动。

锁定方向应在当前 constrained-drag session 第一次超过最小移动阈值时确定。

例如：

```text
axisLockThreshold = 3 px
```

---

# 五十四、HUD Resize

字幕区域不能只有位置。

必须可以调整：

```text
宽度
整体缩放
```

Bounding Box 左右边缘提供 Resize Handle。

例如：

```text
┌────────────────────────────┐
│                            │
│          Subtitle          │
│                            │
└────────────────────────────┘
^                            ^
resize                       resize
```

拖动左右边缘：

```text
改变 maxWidth
```

而不是直接拉伸 glyph。

也就是说：

```text
Resize horizontal
→ 修改字幕换行宽度
```

文字字号不因此改变。

---

# 五十五、字幕整体缩放

另外提供 Scale Handle。

例如右下角：

```text
┌───────────────────────┐
│                       │
│       Subtitle        │
│                       │
└───────────────────────◢
```

拖动：

```text
fontScale
senderScale
lineSpacing
padding
```

按照统一比例改变。

这里修改的是 HUD scale。

不要通过纹理拉伸已经渲染完成的字幕。

必须重新按照新的 font scale/layout 渲染。

---

# 五十六、尺寸限制

必须存在安全范围。

例如：

```text
minWidth = 120 px
maxWidth = 95% screen width

minScale = 0.5
maxScale = 3.0
```

具体值允许配置。

不能把 Bounding Box 缩成：

```text
0 × 0
```

也不能拖到无穷大。

---

# 五十七、默认锚点

实现 Anchor System。

至少支持：

```text
TOP_LEFT
TOP_CENTER
TOP_RIGHT

CENTER_LEFT
CENTER
CENTER_RIGHT

BOTTOM_LEFT
BOTTOM_CENTER
BOTTOM_RIGHT
```

另外可以支持：

```text
CUSTOM
```

默认：

```text
BOTTOM_CENTER
```

屏幕中显示轻量级 Anchor Guides：

```text
+-----------+-----------+-----------+
|     ●     |     ●     |     ●     |
|           |           |           |
|     ●     |     ●     |     ●     |
|           |           |           |
|     ●     |     ●     |     ●     |
+-----------+-----------+-----------+
```

这些点正常游戏中不可见。

只有 HUD Editor 中显示。

---

# 五十八、Anchor Snap

当字幕 HUD 靠近默认锚点时：

自动吸附。

例如：

```text
snapDistance = 12 px
```

HUD 中心/对应 anchor 距离目标锚点小于阈值：

```text
→ 显示吸附提示
→ 松开鼠标后吸附
```

视觉上可以显示辅助线：

```text
--------- screen center ---------

              |
              |
         Subtitle HUD
```

吸附时可以有轻微视觉反馈。

不要播放过于明显的声音。

---

# 五十九、辅助吸附线

除九个 Anchor 外，还支持：

```text
水平中心线
垂直中心线

safe area left
safe area right
safe area top
safe area bottom
```

当 HUD：

```text
中心
边缘
```

靠近这些辅助线时可以吸附。

这会让用户很容易做出：

```text
真正居中
左右对称
底部统一间距
```

---

# 六十、关闭吸附

HUD Editor 中提供：

```text
Snap to anchors
[x]
```

可关闭。

另外建议：

```text
Alt + Drag
```

临时关闭吸附。

如果 Alt 在不同平台/版本存在冲突，则允许省略，但配置开关必须存在。

---

# 六十一、Safe Area

HUD Editor 显示可选：

```text
Safe Area
```

例如默认：

```text
left   5%
right  5%
top    5%
bottom 8%
```

帮助用户避免字幕紧贴屏幕边缘。

Safe Area 只是编辑辅助。

不强制限制字幕必须位于其中。

---

# 六十二、GUI Scale 自适应

HUD 的保存方式不能只保存物理像素：

```text
x = 963 px
y = 712 px
```

否则切换分辨率后位置失效。

推荐保存：

```text
anchor
normalizedOffsetX
normalizedOffsetY
normalizedWidth
scale
```

例如：

```text
anchor = BOTTOM_CENTER

offsetX = 0.0
offsetY = -0.12

width = 0.42
```

使用屏幕逻辑 GUI 尺寸重新计算实际位置。

这样：

```text
1920×1080
2560×1440
3840×2160
窗口模式
全屏
GUI Scale 2/3/4
```

都能基本保持相同构图。

---

# 六十三、编辑器操作提示

HUD Editor 底部显示轻量提示：

```text
Drag                  Move
Shift + Drag          Horizontal / Vertical lock
Drag side handles     Width
Drag corner handle    Scale
R                     Reset position
Esc                   Cancel / Exit
```

不要做成大量遮挡画面的教程。

---

# 六十四、Reset

提供：

```text
Reset Position
Reset Size
Reset HUD
```

区别：

`Reset Position`

只恢复：

```text
anchor
x
y
```

`Reset Size`

只恢复：

```text
width
scale
```

`Reset HUD`

全部恢复默认。

---

# 六十五、Apply / Cancel

进入 HUD Editor 时创建：

```text
working copy
```

不要实时直接覆盖磁盘配置。

操作：

```text
Apply
→ 保存 working config
→ 写入 config

Cancel
→ 丢弃 working config
→ 恢复进入编辑器之前的设置
```

实时 Preview 可以使用 working config。

---

# 六十六、伪配音系统

新增：

**Pseudo Voice System**

目的：

给字幕中的不同说话者绑定一组短音频。

当该角色产生字幕时：

```text
随机播放指定音频
```

形成：

```text
Undertale
老式 RPG
像素 RPG
模拟语音
mumble voice
character blip
```

一类伪配音效果。

该功能同样必须：

**完全客户端。**

服务器不知道 Voice Profile 的存在。

---

# 六十七、Voice Profile

定义：

```text
VoiceProfile
```

每一个 Profile 至少拥有：

```text
id
matchNames
audioFiles

volume
pitch
pitchRandomness

playMode

interval
intervalRandomness

avoidImmediateRepeat

enabled
```

例如：

```json
{
  "id": "raan",
  "matchNames": [
    "拉恩",
    "Raan"
  ],
  "audioFiles": [
    "raan_01.ogg",
    "raan_02.ogg",
    "raan_03.ogg"
  ],
  "volume": 0.8,
  "pitch": 1.0,
  "pitchRandomness": 0.06,
  "playMode": "PER_REVEAL_UNIT",
  "interval": 0.065,
  "intervalRandomness": 0.015,
  "avoidImmediateRepeat": true,
  "enabled": true
}
```

---

# 六十八、Voice ID

用户可以通过配置 Screen 新建：

```text
Voice ID
```

例如：

```text
raan
elvis
npc_guard
narrator
```

Voice ID 只是内部唯一 ID。

不要强制等于玩家名。

---

# 六十九、说话者匹配

一个 Voice Profile 可以绑定多个：

```text
Speaker Match Name
```

例如：

```text
Voice ID:
raan

Speaker Names:
拉恩
Raan
帝国元帅拉恩
```

当字幕系统解析出的：

```text
speakerId
speakerDisplayName
```

匹配其中任意一项时：

使用：

```text
raan
```

Voice Profile。

---

# 七十、玩家名与 /say 说话者

必须尽可能通过 Minecraft 的结构化 Chat 信息取得说话者。

优先级：

```text
ChatType / bound chat context
Player profile UUID/name
sender Component
structured message metadata
```

不要优先通过：

```text
<Steve>
Steve:
[Steve]
```

字符串解析取得玩家名。

如果某些 Minecraft 版本或服务器插件只留下格式化文本，才允许使用 fallback speaker resolver。

Fallback 必须：

```text
version isolated
configurable
safe
```

---

# 七十一、/say

Vanilla `/say` 产生的聊天也要尝试解析其实际：

```text
sender
```

如果发送者是：

```text
player
command block
server
entity
```

都应尽可能形成：

```text
SpeakerIdentity
```

例如：

```text
SpeakerIdentity {
    sourceType
    uuid
    profileName
    displayName
}
```

然后通过：

```text
VoiceProfileResolver
```

匹配 Voice ID。

---

# 七十二、不要只按 UUID 配音

VoiceProfile 不应只支持玩家 UUID。

因为我们需要支持：

```text
玩家
/say NPC
命令方块模拟角色
服务器剧情消息
地图制作人自定义角色
```

所以匹配层至少允许：

```text
display name
profile name
UUID
alias
```

未来可扩展：

```text
regex
```

但 V1 不建议默认开放任意复杂 regex 编辑器。

---

# 七十三、Voice Profile 编辑界面

Config Screen 增加：

```text
Pseudo Voice
```

页面。

显示：

```text
Voice Profiles

+ New Voice Profile

[raan]
[elvis]
[narrator]
```

进入 Profile：

```text
ID

Speaker Names

Audio Files

Play Mode

Volume
Pitch
Pitch Randomness

Interval
Interval Randomness

Avoid Immediate Repeat

Preview
Delete
```

---

# 七十四、添加音频

配置页面允许：

```text
Add Audio
```

列出：

```text
config/cinematic_chat/audio/
```

中的可用音频。

不要要求用户手写完整绝对路径。

用户只选择：

```text
raan_01.ogg
```

内部保存相对路径。

---

# 七十五、配置目录

使用：

```text
config/
└─ cinematic_chat/
   ├─ cinematic_chat.json
   │
   ├─ voices.json
   │
   └─ audio/
      ├─ raan_01.ogg
      ├─ raan_02.ogg
      ├─ elvis_01.ogg
      └─ narrator/
         ├─ 01.ogg
         └─ 02.ogg
```

也允许子目录。

例如：

```text
raan/voice01.ogg
```

配置中使用：

```text
raan/voice01.ogg
```

禁止：

```text
C:\Users\...
/home/user/...
```

这种绝对路径。

---

# 七十六、路径安全

音频加载器必须限制在：

```text
config/cinematic_chat/audio/
```

之内。

禁止：

```text
../
..\

absolute path
symlink escape where detectable
```

不得允许聊天文本或者服务器指定本地文件路径。

这是纯用户本地配置。

服务器消息只能决定：

```text
谁在说话
```

绝不能决定：

```text
播放客户端哪个任意文件
```

VoiceProfileResolver 只能访问用户自己已经配置好的 ID。

---

# 七十七、音频格式

第一阶段必须至少支持：

```text
.ogg
```

推荐额外支持：

```text
.wav
```

不要为了 MP3 强制加入大型第三方音频库。

如果当前 Minecraft/OpenAL/decoder 架构能干净、跨版本地支持 MP3，可以后续增加。

V1 优先保证：

```text
OGG Vorbis
WAV PCM
```

稳定。

README 必须明确支持格式。

---

# 七十八、不要把配置音频注册成资源包 SoundEvent

这些音频来自：

```text
config/
```

运行时。

不要要求用户：

```text
制作 resource pack
写 sounds.json
重启游戏
```

实现独立的：

```text
ExternalAudioManager
```

用于：

```text
load
decode
cache
play
stop
reload
```

避免污染 Minecraft resource namespace。

---

# 七十九、跨版本音频抽象

`engine` 不负责真正播放声音。

定义抽象：

```text
PseudoVoicePlayer
```

例如：

```java
interface PseudoVoicePlayer {
    VoiceHandle play(DecodedVoiceClip clip, VoicePlaybackParams params);
    void stop(VoiceHandle handle);
    void stopAll();
}
```

Minecraft adapter / platform 层负责：

```text
OpenAL / Minecraft audio bridge
```

不要让 CCML Parser 直接访问音频 API。

---

# 八十、音频缓存

不要每出现一个字符就重新：

```text
打开文件
解码 OGG
创建 buffer
```

实现：

```text
VoiceClipCache
```

生命周期：

```text
第一次使用
→ load/decode

之后
→ reuse decoded/buffered data
```

配置 reload：

```text
释放旧 buffer
重新加载
```

退出世界/客户端：

正确释放 native audio resource。

---

# 八十一、多音频随机播放

如果 Profile 配置：

```text
voice01.ogg
voice02.ogg
voice03.ogg
voice04.ogg
```

每次需要播放时随机选择。

但默认：

```text
avoidImmediateRepeat = true
```

例如刚刚播放：

```text
voice03
```

下一次尽量不要继续：

```text
voice03
```

推荐实现：

```text
shuffle bag
```

而不是每次完全随机。

例如：

```text
[1, 3, 4, 2]
```

全部播放一轮之后重新 shuffle。

这样听起来更自然。

---

# 八十二、Pseudo Voice Play Modes

至少支持：

```text
OFF

ON_SUBTITLE_START

PER_WORD

PER_REVEAL_UNIT

LOOP_WHILE_REVEALING
```

## ON_SUBTITLE_START

每条字幕开始时播放一次随机音频。

适合：

```text
较长语音片段
角色语气声
```

## PER_WORD

每出现一个单词触发一次。

英文效果较好。

CJK 需要 fallback grouping。

## PER_REVEAL_UNIT

随着打字机文字出现周期性播放短 blip。

最适合像素 RPG 伪配音。

不要真的每个字符都无条件播放。

使用最小 interval。

## LOOP_WHILE_REVEALING

播放 loop sound。

字幕 reveal 完成：

```text
stop/fade out
```

---

# 八十三、推荐默认伪配音算法

默认使用：

```text
PER_REVEAL_UNIT
```

但按时间节流。

例如：

```text
minimumInterval = 65ms
```

逻辑：

```text
字符继续 reveal
        ↓
距离上一 voice blip >= interval
        ↓
当前字符不是纯空格
        ↓
播放随机 clip
```

不要：

```text
60 CPS
→ 每秒播放 60 个声音
```

导致刺耳噪声。

---

# 八十四、标点与空白

默认这些字符：

```text
空格
\n
\t
```

不触发 Voice。

标点：

```text
,
.
!
?
，
。
！
？
……
```

默认：

```text
不触发
```

或者允许配置：

```text
voiceOnPunctuation
```

默认关闭。

---

# 八十五、CJK 伪配音

中文、日文等不能依赖空格 word split。

PER_WORD 模式需要：

```text
Unicode-aware fallback
```

例如可以按：

```text
2～4 grapheme cluster
```

形成一个 pseudo-word unit。

配置：

```text
cjkCharactersPerVoice = 2
```

---

# 八十六、声音 Pitch

允许每次播放轻微随机 Pitch：

```text
basePitch = 1.0
pitchRandomness = ±0.05
```

例如：

```text
实际 pitch：
0.95～1.05
```

能大幅降低：

```text
同一个 blip 连续播放
```

产生的机械感。

必须 clamp 到安全范围。

例如：

```text
0.5～2.0
```

---

# 八十七、Volume

Voice Profile 拥有：

```text
volume
```

全局还有：

```text
Pseudo Voice Master Volume
```

最终：

```text
finalVolume =
masterVolume × profileVolume
```

必须支持：

```text
0
```

完全静音。

---

# 八十八、Minecraft 音量类别

如果能够合理集成 Minecraft SoundSource：

推荐：

```text
VOICE
```

若当前版本不存在合适 category，则使用：

```text
MASTER
```

或新增仅客户端内部控制。

不要错误绑定：

```text
MUSIC
BLOCKS
HOSTILE
```

建议 Cinematic Chat 自己拥有：

```text
Pseudo Voice Volume
```

滑块作为主要控制。

---

# 八十九、字幕停止时的声音处理

如果：

```text
字幕被 clear
字幕被 replace
进入 ChatScreen
关闭 cinematic chat
切换世界
断开服务器
```

正在运行的 pseudo voice 必须按对应模式：

```text
stop
```

或短暂 fade。

不能让字幕消失后还继续无限播放。

---

# 九十、多个说话者连续发言

每个 subtitle entry 保留：

```text
voiceProfileId
```

不能使用一个全局：

```text
currentVoice
```

否则多人快速聊天会出现角色声音串台。

例如：

```text
Raan subtitle
→ raan voice

Elvis subtitle
→ elvis voice

Raan older subtitle still visible
→ 不得突然切成 Elvis
```

---

# 九十一、同名问题

可能存在：

```text
玩家名字 = NPC 显示名
```

Resolver 应支持优先级。

建议：

```text
UUID exact match
↓
profile name exact
↓
display name exact
↓
alias exact
↓
fallback
```

用户可以手动调整优先级。

---

# 九十二、Voice Profile Fallback

允许配置：

```text
Default Player Voice
Default NPC Voice
Default System Voice
```

例如任何未单独绑定的玩家：

```text
player_default
```

也可以设置：

```text
None
```

默认建议：

```text
None
```

避免安装模组后所有服务器玩家突然都有声音。

---

# 九十三、说话者名称标准化

匹配前提供可选：

```text
stripFormatting
caseInsensitive
trimWhitespace
```

例如：

```text
§6拉恩
```

或者 Component style 中显示为金色：

仍然可以匹配：

```text
拉恩
```

不要通过直接破坏原始 Component 完成。

仅创建：

```text
normalizedSpeakerKey
```

用于 resolver。

---

# 九十四、Voice Preview

配置 Profile 时：

```text
Preview
```

按钮。

点击：

随机播放一次该 Profile 的声音。

如果有多个文件：

每次 Preview 随机/Shuffle Bag。

还应提供：

```text
Test Talking
```

模拟：

```text
拉恩
北边的门打不开。
```

同时预览：

```text
字幕 reveal
+
pseudo voice
```

---

# 九十五、缺失音频

如果配置：

```text
raan_04.ogg
```

但文件不存在：

```text
skip file
log warning once
```

不要：

```text
crash
```

如果整个 profile 没有有效音频：

```text
disable playback for this profile
```

字幕依旧正常。

---

# 九十六、损坏音频

Decode failure：

```text
mark clip invalid
log warning
continue
```

同一损坏文件不要每个字符重新尝试 decode 并刷日志。

可以缓存：

```text
FAILED
```

状态，直到：

```text
audio reload
```

---

# 九十七、音频热重载

客户端命令：

```text
/cchat voice reload
```

重新扫描：

```text
config/cinematic_chat/audio/
```

释放旧 cache。

重新加载配置。

这样用户可以：

```text
游戏运行中
放入新 OGG
执行 reload
立即测试
```

尽量不要求重启 Minecraft。

---

# 九十八、Voice CLI

客户端命令建议：

```text
/cchat voice list

/cchat voice reload

/cchat voice test <voiceId>

cchat voice assign <speaker> <voiceId>
```

但复杂配置仍然以 GUI 为主。

所有这些命令：

**仅客户端执行。**

绝对不能发送服务器。

---

# 九十九、Voice Profile GUI 新建流程

点击：

```text
New Voice Profile
```

输入：

```text
Voice ID
```

例如：

```text
raan
```

然后：

```text
Speaker Names
[拉恩]
[Raan]
[+]

Audio Files
[raan_01.ogg]
[raan_02.ogg]
[+ Add]

Playback
PER_REVEAL_UNIT

Volume
0.80

Pitch
1.00

Pitch Randomness
0.05

Interval
65 ms
```

保存。

之后只要字幕解析出的 speaker：

```text
拉恩
```

就自动使用这个 Voice Profile。

---

# 一百、Voice 与 CCML

CCML 可以影响 reveal：

```text
[speed]
[pause]
[wait]
```

Pseudo Voice 必须跟随实际 Timeline。

例如：

```text
[speed=5]
你……[pause=2]来了。
```

声音应该：

```text
“你”
voice
↓
文字逐渐显示
↓
pause 两秒期间没有新的 voice blip
↓
“来了”
恢复 voice
```

绝对不能根据：

```text
完整字符串长度
```

预先播放一串声音。

Voice 必须由真实 Reveal Events 驱动。

---

# 一百零一、Reveal Event Bus

在 engine 内建立：

```text
SubtitleRuntimeEvent
```

至少：

```text
SubtitleStarted
RevealUnitShown
WordShown
PauseStarted
PauseEnded
SubtitleCompleted
SubtitleCleared
SubtitleReplaced
```

Pseudo Voice 系统订阅这些事件。

例如：

```text
RevealUnitShown
→ PseudoVoiceController
→ 判断 interval
→ play clip
```

这样不要把：

```text
audio logic
```

硬写进 `SubtitleTimeline`。

---

# 一百零二、声音不得影响字幕性能

Pseudo Voice 失败：

```text
不得阻塞 reveal
```

字幕永远是主系统。

声音只是 optional presentation。

任何：

```text
IO
decode
audio error
```

都不得：

```text
卡住 render thread
暂停字幕
造成客户端断线
```

如果需要 decode：

可以在安全 worker 上预加载。

真正播放调用遵守对应版本 Minecraft/OpenAL 的线程要求。

---

# 一百零三、字幕 HUD Preset 保存

HUD Editor 增加：

```text
Save Preset
Load Preset
```

用户可以保存：

```text
My Cinematic
Bottom Minimal
Ultrawide
Large Subtitle
```

Preset 只保存 HUD 视觉参数。

不要自动包含：

```text
Voice Profile
```

除非未来明确增加完整 Profile Export。

---

# 一百零四、多屏幕比例

重点测试：

```text
16:9
16:10
21:9
32:9
4:3
窗口模式
```

Anchor + normalized layout 必须正确。

特别是：

```text
21:9 / 32:9
```

不能让字幕因为 screen width 太大而变成一整行超长文字。

字幕 max width 应同时支持：

```text
normalized width
+
absolute maximum
```

例如：

```text
widthRatio = 0.45
absoluteMaxWidth = 900 logical pixels
```

取较小值。

---

# 一百零五、HUD Editor 代码结构

推荐：

```text
SubtitleHudEditorScreen
HudEditorModel
HudBoundingBox
HudAnchor
HudSnapManager
HudDragController
HudResizeController
HudScaleController
HudEditorPreview
```

不要把所有鼠标逻辑塞进一个 Screen 类。

---

# 一百零六、Pseudo Voice 代码结构

推荐：

```text
VoiceProfile
VoiceProfileManager
VoiceProfileResolver
SpeakerIdentity
SpeakerNormalizer

ExternalAudioManager
VoiceClip
VoiceClipCache
VoicePlaybackParams

PseudoVoiceController
VoiceShuffleBag
VoiceRuntimeState
```

Loader/version 层：

```text
PlatformAudioBackend
```

---

# 一百零七、Voice JSON

建议单独：

```text
config/cinematic_chat/voices.json
```

不要把所有 Profile 全塞进主：

```text
cinematic_chat.json
```

示例：

```json
{
  "version": 1,
  "masterVolume": 1.0,
  "profiles": [
    {
      "id": "raan",
      "match": {
        "names": [
          "拉恩",
          "Raan"
        ],
        "uuids": []
      },
      "audio": [
        "raan/01.ogg",
        "raan/02.ogg",
        "raan/03.ogg"
      ],
      "playMode": "PER_REVEAL_UNIT",
      "volume": 0.8,
      "pitch": 1.0,
      "pitchRandomness": 0.05,
      "intervalMs": 65,
      "intervalRandomnessMs": 10,
      "avoidImmediateRepeat": true,
      "enabled": true
    }
  ]
}
```

真实 schema 可以调整。

必须提供 migration version。

---

# 一百零八、隐私原则

Pseudo Voice Profile：

```text
完全保存在客户端
```

不得：

```text
自动上传
发送服务器
发送其他玩家
遥测
```

服务器也不应该得知：

```text
哪个玩家绑定了哪个 voice
```

---

# 一百零九、声音资源版权

项目仓库默认不要附带：

```text
商业游戏提取语音
版权不明素材
第三方角色语音
```

只放：

```text
原创测试音
CC0
或明确允许再分发的测试素材
```

最好甚至只提供极短的自制占位测试音。

README 明确：

用户自己放入：

```text
config/cinematic_chat/audio/
```

的声音版权由用户负责。

---

# 一百一十、无音频资源也必须正常工作

Pseudo Voice 是 optional。

如果：

```text
audio directory empty
```

整个模组仍应：

```text
正常启动
正常显示字幕
正常执行 CCML
```

不能让 audio subsystem 成为硬依赖。

---

# 一百一十一、新增设置页面布局

最终配置结构建议：

```text
General

HUD
 ├─ Position
 ├─ Width
 ├─ Scale
 ├─ Alignment
 ├─ Anchor
 └─ Open HUD Editor

Typography

Animation

Message Types

CCML

Pseudo Voice
 ├─ Enabled
 ├─ Master Volume
 ├─ Default Voice
 ├─ Profiles
 └─ Reload Audio

Accessibility

Advanced
```

---

# 一百一十二、首次打开 HUD Editor

第一次使用：

默认显示：

```text
透明字幕边界框
九宫格 anchor
屏幕中心辅助线
safe area
```

并显示：

```text
Drag to move
Shift to constrain movement
Drag edges to resize
```

用户进行第一次操作后，这些提示可以降低透明度。

不要长期挡住 UI。

---

# 一百一十三、编辑期间模拟动画

HUD Editor 不能只展示静态字幕。

提供：

```text
Replay Preview
```

按钮。

可以模拟：

```text
fade in
typewriter
smooth reveal
pseudo voice
fade out
```

用户调整动画参数以后直接再次播放。

---

# 一百一十四、实际验收测试：HUD Editor

必须验证：

1. `/cchat hud` 能打开编辑器。
2. 可以自由拖动。
3. Shift 水平锁定正确。
4. Shift 垂直锁定正确。
5. 拖动过程中不抖动切轴。
6. 左右 Resize 正确修改换行宽度。
7. Scale Handle 正确调整字号体系。
8. 可以吸附屏幕中心。
9. 可以吸附九个默认 Anchor。
10. GUI Scale 改变后位置合理。
11. 分辨率改变后位置合理。
12. 21:9 正常。
13. 4:3 正常。
14. Cancel 不保存。
15. Apply 保存。
16. Reset 正常。
17. HUD Editor 不向服务器发送任何 packet。

---

# 一百一十五、实际验收测试：Pseudo Voice

必须验证：

1. 新建 Voice Profile。
2. 输入 speaker alias。
3. 添加一个 OGG。
4. 字幕 speaker 匹配后播放。
5. 多 OGG 可以随机播放。
6. 不连续重复同一声音。
7. volume 生效。
8. pitch 生效。
9. pitch randomness 生效。
10. interval 生效。
11. pause 期间不播放。
12. wait 正确。
13. clear 后停止。
14. replace 正常。
15. ChatScreen 打开后的声音行为正确。
16. 音频不存在不崩溃。
17. 音频损坏不崩溃。
18. reload 后新文件可使用。
19. 未绑定 speaker 不播放。
20. 多玩家连续讲话不会串 Voice Profile。
21. 中文 speaker 正确匹配。
22. Component 彩色名字仍能正确 normalize。
23. `/say` sender 能尽可能正确解析。
24. Fabric 与 NeoForge 行为一致。
25. 所有支持 Minecraft 版本均能安全启动。

---

# 一百一十六、最终设计原则补充

Cinematic Chat 的 HUD 不应该只是：

```text
config screen 中输入 X/Y 数字
```

用户应该能够：

```text
看到字幕
抓住字幕
拖动字幕
拉宽字幕
缩小字幕
吸附字幕
直接获得自己想要的布局
```

Pseudo Voice 也不应该只是：

```text
playerName -> sound
```

它必须是：

```text
Minecraft structured speaker
        ↓
SpeakerIdentity
        ↓
VoiceProfileResolver
        ↓
VoiceProfile
        ↓
Subtitle Reveal Events
        ↓
PseudoVoiceController
        ↓
ExternalAudioManager
```

最终体验应允许用户做到：

```text
创建 Voice ID：raan

匹配说话者：
拉恩
Raan

绑定：
raan01.ogg
raan02.ogg
raan03.ogg
```

之后无论该说话者来自：

```text
玩家聊天
/say
地图剧情
服务器产生的兼容聊天消息
```

只要客户端能够可靠识别：

```text
speaker = 拉恩
```

该客户端就使用：

```text
raan
```

这一套伪配音。

所有 Voice Profile、声音文件、匹配规则都完全存在于：

```text
用户客户端
```

之中。

服务器无需安装、无需知道、无需配合。