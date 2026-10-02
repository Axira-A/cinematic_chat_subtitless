# HUD editor

[English](../en/hud-editor.md) · [简体中文](../zh-CN/hud-editor.md) · [日本語](../ja-JP/hud-editor.md) · [한국어](../ko-KR/hud-editor.md)

Open `/cchat hud` (or `/cinematicchat hud`). The config page's HUD button opens the same editor with the parent's working copy. The preview uses the normal renderer and an independent queue, with Steve and Alex's localized welcome messages.

1. Drag inside the subtitle region to move it.
2. Hold Shift to lock movement to one axis after moving more than three logical pixels. Changing Shift resets the drag baseline to avoid a jump.
3. Drag the left or right edge to change wrapping width. Text scale stays unchanged.
4. Drag the lower-right corner to change overall scale (0.5–3.0).
5. Click one of nine anchor markers. Toggle snapping and safe-area guides; hold Alt when releasing a drag to bypass snapping.

Center lines and safe-area guides help placement. Position and width are normalized to the screen, so changes in resolution keep their proportions. Reset Position, Reset Size and Reset HUD provide separate recovery actions. Replay Preview restarts the welcome sample.

Cycle the six built-in presets (Cinematic, Minimal, RPG, Adventure, Accessible and Vanilla-like), or enter a name and save a personal preset. Presets store visual properties only; user names retain their original text. A preset enters the editor's working copy first.

When opened directly, **Apply** saves the HUD and preset working copies to `config/cinematic_chat/`. When opened from config, Apply returns the changes to the parent; **Save on the parent page is still required**. Cancel or Esc discards the current screen's working copy. Parent config Cancel also discards changes returned from its child editors.

[README](../../README.md)
