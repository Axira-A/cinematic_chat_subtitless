package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Typed draw boundary. The renderer and editor share one implementation. */
public final class GuiCanvas {
    private final GuiGraphicsExtractor graphics;
    private GuiCanvas(GuiGraphicsExtractor graphics) { this.graphics=graphics; }
    public static GuiCanvas wrap(GuiGraphicsExtractor graphics) { return new GuiCanvas(graphics); }
    GuiGraphicsExtractor nativeGraphics() { return graphics; }
    public int guiWidth() { return graphics.guiWidth(); }
    public int guiHeight() { return graphics.guiHeight(); }
    public void fill(int left,int top,int right,int bottom,int color) { graphics.fill(left,top,right,bottom,color); }
    public void drawString(Font font,String text,int x,int y,int color,boolean shadow) { graphics.text(font,text,x,y,color,shadow); }
    public void drawString(Font font,Component text,int x,int y,int color,boolean shadow) { graphics.text(font,text,x,y,color,shadow); }
    public void drawCenteredString(Font font,String text,int x,int y,int color) { graphics.centeredText(font,text,x,y,color); }
    public void drawCenteredString(Font font,Component text,int x,int y,int color) { graphics.centeredText(font,text,x,y,color); }
    public void push() { GuiPose.push(graphics); }
    public void pop() { GuiPose.pop(graphics); }
    public void translate(float x,float y) { GuiPose.translate(graphics,x,y); }
    public void scale(float scale) { GuiPose.scale(graphics,scale); }
    public void outline(int x,int y,int width,int height,int color) { GuiPose.outline(graphics,x,y,width,height,color); }
}
