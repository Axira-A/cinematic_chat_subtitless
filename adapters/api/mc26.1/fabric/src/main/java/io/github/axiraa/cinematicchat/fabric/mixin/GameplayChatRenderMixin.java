package io.github.axiraa.cinematicchat.fabric.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChatComponent.class)
public abstract class GameplayChatRenderMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",at=@At("HEAD"),cancellable=true)
    private void cinematicChat$gameplayGate(GuiGraphicsExtractor graphics,net.minecraft.client.gui.Font font,int tick,int x,int y,ChatComponent.DisplayMode displayMode,boolean changeCursor,CallbackInfo ci){
        var client=CinematicChatClient.get();if(displayMode==ChatComponent.DisplayMode.BACKGROUND&&client!=null&&client.hideVanilla())ci.cancel();
    }
}
