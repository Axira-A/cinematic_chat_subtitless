package io.github.axiraa.cinematicchat.minecraft.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class MinecraftLifecycleMixin {
    @Inject(method="close",at=@At("HEAD"))
    private void cinematicChat$shutdown(CallbackInfo ci){var client=CinematicChatClient.get();if(client!=null)client.shutdown();}
}
