package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small version boundary shared by the native client screens. */
public abstract class CinematicScreen extends Screen {
    protected CinematicScreen(Component title){super(title);}
    protected void cinematicBlur(net.minecraft.client.gui.GuiGraphics graphics,float partialTick){renderBlurredBackground(partialTick);}
}
