# CCML

CCML 是只控制字幕演出的数据语言，没有命令执行、网络、文件、反射、表达式或服务器能力。

| 标签 | 作用 |
| --- | --- |
| `[pause=1.2]` | 前文 reveal 完成后停顿 |
| `[wait]` | 等待此前有限入场/reveal，不等待持续 shake/wave |
| `[hold=7]` | 全文完成后的最短保留时间，多次取最大 |
| `[speed=15]`、`[speed=default]` | 后续显现速度 |
| `[reveal=instant/typewriter/smooth/word]` | 后续显现模式 |
| `[clear]`、`[replace]` | 只清空当前 entry 的已显现内容 |
| `[erase=2]` | 删除当前 entry 最后两个 grapheme |
| `[fadein=0.2]`、`[fadeout=0.3]` | 当前 entry 入场/退场淡化，0–3 秒 |
| `[br]`、`\n` | 换行 |
| `[color=#B02020]…[/color]` | 局部颜色 |
| `[scale=1.2]…[/scale]` | 局部缩放 |
| `[alpha=0.5]…[/alpha]` | 局部透明度 |
| `[bold]`、`[italic]`、`[underline]` | 嵌套样式，使用匹配结束标签 |
| `[shake strength=0.7 frequency=12]…[/shake]` | 局部抖动 |
| `[wave amplitude=2 frequency=2]…[/wave]` | 局部波浪 |

使用 `\[`、`\]`、`\\` 转义。未知标签、非法数值、非有限数和不匹配标签显示为普通文字；合法越界数值 clamp。解析达到节点上限或发生异常时退回 styled 纯文本。AST 的文本、样式和指令节点保留源码区间 `[start,endExclusive)`；转义后的字符仍映射到原源码范围。Component 原有颜色、字体和样式与局部 CCML 样式叠加，不修改原 Component。

默认 SAFE：pause ≤3 秒、hold ≤10 秒、最终局部 scale 0.75–1.5、shake ≤1 像素、wave ≤2 像素。ALL 绝对上限：输入 32768 UTF-16 单位、2048 节点、16 层嵌套、pause 10 秒、hold 30 秒、scale 0.5–2.5、shake 4 像素、wave 8 像素。SAFE 演出时间上限 60 秒，ALL 120 秒；达到上限时余文立即显示，不继续累积停顿。

SYSTEM_ONLY 仅根据结构化来源解析 SYSTEM、SAY、JOIN_LEAVE、DEATH、ADVANCEMENT。UNKNOWN 不获得系统待遇。OFF 原样显示标签。

```text
你终于来了。\n[pause=1.2]不过……已经太迟了。
[speed=15]潮水正在上涨……[wait][pause=0.5]\n快走。
不要碰那个[color=#B02020][shake]东西[/shake][/color]。
欢迎回来，勇者。[pause=1][replace]欢迎回来，罪人。
勇者[wait][erase=2]罪人。
这是\[pause=5\]普通文字。
```
