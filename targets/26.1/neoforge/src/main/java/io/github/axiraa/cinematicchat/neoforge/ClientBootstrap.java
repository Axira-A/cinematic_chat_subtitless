package io.github.axiraa.cinematicchat.neoforge;

import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** FML constructs mods before the Minecraft singleton is available in 1.21.5. */
final class ClientBootstrap {
    private ClientBootstrap() {}
    static void register(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(CinematicChatClient::new));
    }
}
