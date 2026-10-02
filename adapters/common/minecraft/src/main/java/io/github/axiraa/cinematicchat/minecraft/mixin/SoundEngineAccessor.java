package io.github.axiraa.cinematicchat.minecraft.mixin;
import net.minecraft.client.sounds.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(SoundEngine.class) public interface SoundEngineAccessor {
    @Accessor("channelAccess") ChannelAccess cinematicChat$channels();
    @Accessor("executor") SoundEngineExecutor cinematicChat$executor();
    @Accessor("loaded") boolean cinematicChat$loaded();
}
