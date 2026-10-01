package io.github.axiraa.cinematicchat.neoforge;

import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.neoforged.bus.api.IEventBus;

final class ClientBootstrap {
    private ClientBootstrap() {}
    static void register(IEventBus modBus) { new CinematicChatClient(); }
}
