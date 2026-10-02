# CCML 聊天代码

[English](../en/chat-code.md) · [简体中文](../zh-CN/chat-code.md) · [日本語](../ja-JP/chat-code.md) · [한국어](../ko-KR/chat-code.md)

CCML 来自原版已接受的玩家或系统聊天，包括 `/say`；消息来源开关与信任模式决定是否解析。`/cchat parser <text>` 在本地检查脚本，`/cchat preview` 生成当前语言的欢迎语。这些客户端命令不会向其他玩家发送脚本。

```text
[speed=30]您好，[color=#67BDE5][bold]欢迎使用Cinematic Chat。[/bold][/color][hold=3]
```

| 标签 | 含义 |
|---|---|
| `[pause=0.5]` | 等待前文显现完成，再停顿指定秒数。 |
| `[wait]` | 等待此前有限时长的入场与显现动画完成；持续 shake/wave 不阻塞。 |
| `[hold=3]` | 全文显示后的最短保留秒数；多个 hold 取最大值。 |
| `[speed=30]`、`[speed=default]` | 每秒显现单位数，或恢复配置速度。 |
| `[reveal=instant]` | 可选 `instant`、`typewriter`、`smooth`、`word`。 |
| `[clear]`、`[replace]` | 清空当前 entry 的文字，后续文字替换它。 |
| `[erase=2]` | 删除当前 entry 末尾最多两个字素。 |
| `[fadein=0.2]`、`[fadeout=0.3]` | 入场、退场淡化秒数，限制在 0–3。 |
| `[br]` | 换行；`\n` 也会换行。 |
| `[color=#67BDE5]…[/color]` | RGB 颜色。 |
| `[scale=1.2]…[/scale]`、`[alpha=0.8]…[/alpha]` | 局部缩放与透明度。 |
| `[bold]…[/bold]`、`[italic]…[/italic]`、`[underline]…[/underline]` | 粗体、斜体、下划线。 |
| `[shake strength=0.7 frequency=12]…[/shake]` | 抖动强度与频率。 |
| `[wave amplitude=2 frequency=2]…[/wave]` | 波动幅度与频率。 |

样式可嵌套，按相反顺序关闭。`\[`、`\]` 输出普通方括号，`\\` 输出反斜杠，`\n` 换行。未知标签、非法参数及不匹配标签回退为普通文字；合法数值越界时 clamp。显现和删除按 Unicode 字素计数，连续 CJK 在逐词模式下回退为字素。

SAFE 默认 pause ≤3 秒、hold ≤10 秒、scale 0.75–1.5、shake ≤1 像素、wave ≤2 像素，累计脚本时间 ≤60 秒。ALL 使用绝对上限，累计时间 ≤120 秒。SYSTEM_ONLY 只解析结构化分类为系统来源的消息，UNKNOWN 不视为系统。OFF 按普通文字显示标签。所有模式均限制输入长度、节点、深度及效果；累计时间达到上限后，剩余脚本按普通文字呈现。标签不读取文件、执行命令或发送数据包。原始聊天 Component 与历史保持原样。

[README](../../README.zh-CN.md)
