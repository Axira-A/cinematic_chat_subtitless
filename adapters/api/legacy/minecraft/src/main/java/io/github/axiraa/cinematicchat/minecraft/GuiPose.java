package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.gui.GuiGraphics;

/** Version boundary for the GUI transform stack. */
final class GuiPose {
    private GuiPose() {}
    static void push(GuiGraphics graphics) { graphics.pose().pushPose(); }
    static void pop(GuiGraphics graphics) { graphics.pose().popPose(); }
    static void translate(GuiGraphics graphics, float x, float y) { graphics.pose().translate(x, y, 0); }
    static void scale(GuiGraphics graphics, float scale) { graphics.pose().scale(scale, scale, 1); }
    static void outline(GuiGraphics g,int x,int y,int width,int height,int color){g.renderOutline(x,y,width,height,color);}
}
