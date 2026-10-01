package io.github.axiraa.cinematicchat.minecraft.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SoundEngine.class)
public abstract class SoundEngineLifecycleMixin {
    @Inject(method="destroy",at=@At("HEAD"))
    private void cinematicChat$beforeDeviceClose(CallbackInfo ci){var client=CinematicChatClient.get();if(client!=null)client.beforeSoundDeviceClose();}
}
