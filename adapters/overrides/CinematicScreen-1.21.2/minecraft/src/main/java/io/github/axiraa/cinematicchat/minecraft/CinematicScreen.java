package io.github.axiraa.cinematicchat.minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class CinematicScreen extends Screen {
    protected CinematicScreen(Component title){super(title);}
    protected void cinematicBlur(GuiCanvas graphics,float partialTick){renderBlurredBackground();}

    @Override public final void render(net.minecraft.client.gui.GuiGraphics graphics,int x,int y,float partialTick) {
        renderContent(GuiCanvas.wrap(graphics),x,y,partialTick);
    }
    protected void renderContent(GuiCanvas graphics,int x,int y,float partialTick) {
        super.render(graphics.nativeGraphics(),x,y,partialTick);
    }
    @Override public final void renderBackground(net.minecraft.client.gui.GuiGraphics graphics,int x,int y,float partialTick) {
        renderBackdrop(GuiCanvas.wrap(graphics),x,y,partialTick);
    }
    protected void renderBackdrop(GuiCanvas graphics,int x,int y,float partialTick) {}
}
