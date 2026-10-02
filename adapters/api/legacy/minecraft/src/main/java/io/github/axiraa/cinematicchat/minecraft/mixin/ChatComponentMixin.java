package io.github.axiraa.cinematicchat.minecraft.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",at=@At("TAIL"))
    private void cinematicChat$capture(Component component,MessageSignature signature,GuiMessageTag tag,CallbackInfo ci){var client=CinematicChatClient.get();if(client!=null)client.capture().capture(component,signature,tag);}
    @Inject(method="deleteMessage",at=@At("HEAD"))
    private void cinematicChat$delete(MessageSignature signature,CallbackInfo ci){var client=CinematicChatClient.get();if(client!=null)client.controller().removeSignature(signature.toString());}
}
