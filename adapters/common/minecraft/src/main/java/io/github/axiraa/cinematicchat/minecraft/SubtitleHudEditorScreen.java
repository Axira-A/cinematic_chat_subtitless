package io.github.axiraa.cinematicchat.minecraft;

import io.github.axiraa.cinematicchat.api.ClientState;
import io.github.axiraa.cinematicchat.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SubtitleHudEditorScreen extends CinematicScreen implements PreviewScreen {
    private final Screen parent;
    private final CinematicConfig working;
    private final HudPresets presets;
    private HudEditorModel editor;
    private final SubtitleController preview;
    private final java.util.function.Consumer<CinematicConfig> applied;
    private final java.util.function.Consumer<HudPresets> presetsApplied;
    private EditBox presetName;
    private int presetIndex;
    private String selectedPreset = "Cinematic";

    public SubtitleHudEditorScreen(Screen parent) {
        this(parent,CinematicChatClient.get().config(),CinematicChatClient.get()::apply);
    }
    public SubtitleHudEditorScreen(Screen parent,CinematicConfig initial,java.util.function.Consumer<CinematicConfig> applied) {
        this(parent,initial,CinematicChatClient.get().presets(),applied,CinematicChatClient.get()::applyPresets);
    }
    public SubtitleHudEditorScreen(Screen parent,CinematicConfig initial,HudPresets initialPresets,java.util.function.Consumer<CinematicConfig> applied,java.util.function.Consumer<HudPresets> presetsApplied) {
        super(Component.translatable("cinematic_chat.hud.title")); this.parent = parent;
        this.applied=applied;
        this.presetsApplied=presetsApplied;
        var client = CinematicChatClient.get();
        working = client.configs().copy(initial, CinematicConfig.class);
        presets = client.configs().copy(initialPresets, HudPresets.class);
        editor = new HudEditorModel(working.hud); preview = client.preview(working); client.sample(preview);
    }

    @Override protected void init() {
        clearWidgets();
        int cell=Math.min(92,(width-28)/4);
        button(8, 8, cell, "cinematic_chat.replay", () -> CinematicChatClient.get().sample(preview));
        button(12+cell,8,cell,"cinematic_chat.hud.reset_position",()->{working.hud.resetPosition();editor=new HudEditorModel(working.hud);});
        button(16+cell*2,8,cell,"cinematic_chat.hud.reset_size",()->{working.hud.resetSize();editor=new HudEditorModel(working.hud);});
        button(20+cell*3,8,cell,"cinematic_chat.hud.reset",()->{HudPresets.builtins().get("Cinematic").apply(working);editor=new HudEditorModel(working.hud);preview.configure(working,CinematicChatClient.get().voices());});
        button(8, 32, 76, "cinematic_chat.hud.snap", () -> {working.hud.snap = !working.hud.snap;});
        button(88, 32, 90, "cinematic_chat.hud.safe", () -> {working.hud.showSafeArea = !working.hud.showSafeArea;});
        int rowY = 56;
        presetName = new EditBox(font, 8, rowY, 100, 20, Component.translatable("cinematic_chat.preset.name"));
        presetName.setMaxLength(64); presetName.setValue("My Cinematic"); addRenderableWidget(presetName);
        button(112, rowY, 82, "cinematic_chat.preset.save", () -> {String name = presetName.getValue().strip(); if (!name.isEmpty()) presets.presets.put(name, HudPresets.Visual.capture(working));});
        addRenderableWidget(Button.builder(Component.literal(selectedPreset), b -> {
            var values = new ArrayList<>(allPresets().entrySet()); if (values.isEmpty()) return;
            var next = values.get(presetIndex++ % values.size()); selectedPreset = next.getKey(); next.getValue().apply(working);
            editor = new HudEditorModel(working.hud); preview.configure(working, CinematicChatClient.get().voices()); CinematicChatClient.get().sample(preview); b.setMessage(Component.literal(selectedPreset));
        }).bounds(184, 32, Math.min(170,width-192), 20).build());
        button(width / 2 - 104, height - 26, 100, "cinematic_chat.apply", () -> {applied.accept(working); presetsApplied.accept(presets); onClose();});
        button(width / 2 + 4, height - 26, 100, "gui.cancel", this::onClose);
    }

    private Map<String, HudPresets.Visual> allPresets() {var result = new LinkedHashMap<>(HudPresets.builtins()); result.putAll(presets.presets); return result;}
    private void button(int x, int y, int w, String key, Runnable action) {addRenderableWidget(Button.builder(Component.translatable(key), b -> action.run()).bounds(x, y, w, 20).build());}
    public CinematicConfig workingCopy() {return working;}
    public SubtitleRenderer.Bounds bounds() {return CinematicChatClient.get().previewBounds(preview, width, height);}

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (button != 0) return false;
        // Anchor targets are a local editor operation and never send a command or packet.
        for (var anchor : HudSettings.Anchor.values()) if (Math.hypot(x - anchor.x * width, y - anchor.y * height) < 9) {
            working.hud.anchor = anchor; working.hud.offsetX = 0; working.hud.offsetY = 0; return true;
        }
        var box = bounds(); var operation = HudEditorModel.Operation.NONE;
        if (Math.hypot(x - box.right(), y - box.bottom()) < 10) operation = HudEditorModel.Operation.SCALE;
        else if (Math.abs(x - box.left()) < 7 && y >= box.top() - 7 && y <= box.bottom() + 7) operation = HudEditorModel.Operation.WIDTH_LEFT;
        else if (Math.abs(x - box.right()) < 7 && y >= box.top() - 7 && y <= box.bottom() + 7) operation = HudEditorModel.Operation.WIDTH_RIGHT;
        else if (box.contains(x, y)) operation = HudEditorModel.Operation.MOVE;
        if (operation == HudEditorModel.Operation.NONE) return false;
        editor.extent(box.width(),box.height());editor.begin(operation, x, y, width, height); return true;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && editor.operation() != HudEditorModel.Operation.NONE) {editor.drag(x, y, width, height, hasShiftDown()); return true;}
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (editor.operation() != HudEditorModel.Operation.NONE) {editor.end(width, height, hasAltDown()); return true;}
        return super.mouseReleased(x, y, button);
    }
    @Override public void tick() {preview.tick(System.nanoTime(), new ClientState(true, false, Minecraft.getInstance().options.hideGui, false)); if (preview.queue().visible().isEmpty()) CinematicChatClient.get().sample(preview);}
    @Override public void renderContent(GuiCanvas g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x50081018);
        line(g, width / 2, 0, width / 2, height, 0x6067bde5); line(g, 0, height / 2, width, height / 2, 0x6067bde5);
        if (working.hud.showSafeArea) g.outline((int)(width*.05), (int)(height*.05), (int)(width*.9), (int)(height*.87), 0x806ea07f);
        for (var anchor : HudSettings.Anchor.values()) {int x = Math.clamp((int)(anchor.x*width), 4, width-4), y = Math.clamp((int)(anchor.y*height), 4, height-4); g.fill(x-3, y-3, x+3, y+3, anchor == working.hud.anchor ? 0xfff4cc6a : 0xff67bde5);}
        CinematicChatClient.get().drawPreview(g, preview);
        var b = bounds(); int left=(int)b.left(), top=(int)b.top(), right=(int)b.right(), bottom=(int)b.bottom();
        g.outline(left-4, top-4, (int)b.width()+8, (int)b.height()+8, 0xd067bde5);
        g.fill(left-4, (top+bottom)/2-7, left+4, (top+bottom)/2+7, 0xff67bde5);
        g.fill(right-4, (top+bottom)/2-7, right+4, (top+bottom)/2+7, 0xff67bde5);
        g.fill(right-5, bottom-5, right+5, bottom+5, 0xfff4cc6a);
        g.drawCenteredString(font, Component.translatable("cinematic_chat.hud.help"), width/2, height-42, 0xc0ffffff);
        g.drawString(font, String.format(Locale.ROOT, "X %.1f%%  Y %.1f%%  W %.0f  %.2fx",working.hud.x(width)/width*100,working.hud.y(height)/height*100,working.hud.width(width),working.hud.scale),8,82,0xffcccccc,true);
        super.renderContent(g, mouseX, mouseY, partialTick);
    }
    private static void line(GuiCanvas g,int x1,int y1,int x2,int y2,int color) {g.fill(x1,y1,x2==x1?x2+1:x2,y2==y1?y2+1:y2,color);}
    @Override public void renderBackdrop(GuiCanvas g,int x,int y,float partialTick) {}
    @Override public boolean isPauseScreen() {return false;}
    @Override public void onClose() {preview.clear(); minecraft.setScreen(parent);}
}
