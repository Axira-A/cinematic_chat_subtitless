package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.gui.GuiGraphics;

/** 1.21.6 submits GUI elements using a two dimensional transform stack. */
final class GuiPose {
    private GuiPose() {}
    static void push(GuiGraphics graphics) { graphics.pose().pushMatrix(); }
    static void pop(GuiGraphics graphics) { graphics.pose().popMatrix(); }
    static void translate(GuiGraphics graphics, float x, float y) { graphics.pose().translate(x, y); }
    static void scale(GuiGraphics graphics, float scale) { graphics.pose().scale(scale, scale); }
    static void outline(GuiGraphics g,int x,int y,int width,int height,int color){g.submitOutline(x,y,width,height,color);}
}
