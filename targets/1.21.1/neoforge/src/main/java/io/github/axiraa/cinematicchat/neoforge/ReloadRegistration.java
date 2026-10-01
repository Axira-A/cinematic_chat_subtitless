package io.github.axiraa.cinematicchat.neoforge;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
public final class ReloadRegistration {
    private ReloadRegistration(){}
    public static void register(IEventBus bus){bus.addListener((RegisterClientReloadListenersEvent event)->event.registerReloadListener((ResourceManagerReloadListener)manager->CinematicChatClient.get().resourcesReloaded()));}
}
