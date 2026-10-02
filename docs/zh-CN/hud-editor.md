# HUD 编辑器

[English](../en/hud-editor.md) · [简体中文](../zh-CN/hud-editor.md) · [日本語](../ja-JP/hud-editor.md) · [한국어](../ko-KR/hud-editor.md)

执行 `/cchat hud` 或 `/cinematicchat hud` 打开。配置页的 HUD 按钮打开相同编辑器，使用父页面的 working copy。预览采用正常字幕 renderer 和独立队列，显示 Steve 与 Alex 的本地化欢迎语。

1. 在字幕区域内拖动来移动位置。
2. 按住 Shift，移动超过三个逻辑像素后锁定一条轴；Shift 状态变化会重设拖动基准，避免跳动。
3. 拖动左、右边缘调整换行宽度，文字缩放保持原值。
4. 拖动右下角调整整体缩放，范围 0.5–3.0。
5. 点击九个锚点之一；切换吸附和安全区。释放拖动时按住 Alt 临时取消吸附。

中心线与安全区帮助定位，位置和宽度以归一化坐标保存，随分辨率保持比例。Reset Position、Reset Size、Reset HUD 分别恢复位置、尺寸或 HUD 视觉参数。Replay Preview 重播欢迎语。

可切换六个内置预设（电影、极简、角色扮演、冒险、无障碍、原版风格），或输入名称保存个人预设。预设只记录视觉参数，自定义名称保持原文。保存预设先修改编辑器的 working copy。

**直接打开时，Apply（应用）保存 HUD 与预设到 `config/cinematic_chat/`。从配置页进入时，Apply 将修改返回父页面，仍需在父配置页点击 Save（保存）**。Cancel（取消）或 Esc 丢弃当前页面的 working copy；父配置页的取消也会丢弃子编辑器返回的修改。

[README](../../README.zh-CN.md)
