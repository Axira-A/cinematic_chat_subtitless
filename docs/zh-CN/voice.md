# 本地伪配音

[English](../en/voice.md) · [简体中文](../zh-CN/voice.md) · [日本語](../ja-JP/voice.md) · [한국어](../ko-KR/voice.md)

此功能播放短音片段，不合成语音。默认关闭，玩家、系统及 NPC fallback 均为 None（无）。

1. 执行 `/cchat voice`，启用伪配音，创建 Profile。ID 使用 `a-z`、`0-9`、`_`、`-`、`.`，长度 1–64，且不能重复。
2. 在说话者匹配页将 `Steve` 或 `Alex` 加入名称或 alias，也可绑定玩家 UUID。默认顺序为 UUID、profile name、display name、alias、fallback。可调整去格式、大小写及首尾空白；原始名称不变。
3. 将短 OGG Vorbis 或 WAV PCM 放入 `config/cinematic_chat/audio/`，通过音频文件 → 添加音频选择。列表仅限目录内部，拒绝绝对路径、路径穿越及可检测的符号链接逃逸。“安装原创测试音”提供随模组附带的 WAV 与 OGG。
4. 调整播放模式、音量、pitch、随机幅度、最小间隔及 CJK 分组。Preview 测试开始音，Test Talking 使用 Steve 和当前语言欢迎语测试显现事件。
5. 保存 Profile，再保存父声线页；若从配置页进入，还需保存父配置页。取消或 Esc 丢弃当前页的 working copy。

五种模式：关闭、字幕开始时、逐词、逐显现单位、显现期间循环。新 Profile 默认逐显现单位、65ms 最小间隔、两个 CJK 字素一组，默认跳过空白和标点。Shuffle bag 在可行时避免立即重复；瞬间显现时，逐字或逐词触发合并为最多一次。

命令包括 `/cchat voice list`、`/cchat voice test <voiceId>`、`/cchat voice assign Steve <voiceId>`、`/cchat voice reload`。assign 保存绑定；替换缺失或损坏文件后，重载音频可重试。`/cchat reload` 还会重新读取配置。Profile 保存于 `config/cinematic_chat/voices.json`，HUD 预设与主配置分文件保存。加载和解码在后台进行，资源未就绪时跳过声音，不阻塞字幕。清空、替换、聊天画面、禁用及世界切换会停止相关播放。

[README](../../README.zh-CN.md)
