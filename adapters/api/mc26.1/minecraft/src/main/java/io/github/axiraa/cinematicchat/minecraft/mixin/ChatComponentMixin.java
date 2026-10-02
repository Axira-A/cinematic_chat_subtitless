package io.github.axiraa.cinematicchat.minecraft.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    // addMessage can reject through visibleMessageFilter; only admitted history entries reach here.
    @Inject(method="addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",at=@At("TAIL"))
    private void cinematicChat$capture(GuiMessage message,CallbackInfo ci){
        var client=CinematicChatClient.get();
        if(client!=null)client.capture().capture(message.content(),message.signature(),message.tag(),message.source());
    }
    @Inject(method="deleteMessage",at=@At("HEAD"))
    private void cinematicChat$delete(MessageSignature signature,CallbackInfo ci){
        var client=CinematicChatClient.get();if(client!=null)client.controller().removeSignature(signature.toString());
    }
    @Inject(method="setVisibleMessageFilter",at=@At("TAIL"))
    private void cinematicChat$permissionsChanged(CallbackInfo ci){
        var client=CinematicChatClient.get();if(client!=null)client.controller().clear();
    }
}
