package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 1.21.6 submits GUI elements using a two dimensional transform stack. */
final class GuiPose {
    private GuiPose() {}
    static void push(GuiGraphicsExtractor graphics) { graphics.pose().pushMatrix(); }
    static void pop(GuiGraphicsExtractor graphics) { graphics.pose().popMatrix(); }
    static void translate(GuiGraphicsExtractor graphics, float x, float y) { graphics.pose().translate(x, y); }
    static void scale(GuiGraphicsExtractor graphics, float scale) { graphics.pose().scale(scale, scale); }
    static void outline(GuiGraphicsExtractor g,int x,int y,int width,int height,int color){g.outline(x,y,width,height,color);}
}
