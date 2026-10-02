package io.github.axiraa.cinematicchat.fabric.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChatComponent.class)
public abstract class GameplayChatRenderMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void cinematicChat$gameplayGate(GuiGraphics graphics,int tick,int x,int y,boolean focused,CallbackInfo ci){
        var client=CinematicChatClient.get();if(!focused&&client!=null&&client.hideVanilla())ci.cancel();
    }
}
