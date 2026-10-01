package io.github.axiraa.cinematicchat.minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class CinematicScreen extends Screen {
    protected CinematicScreen(Component title){super(title);}
    protected void cinematicBlur(net.minecraft.client.gui.GuiGraphics graphics,float partialTick){renderBlurredBackground(graphics);}
}
