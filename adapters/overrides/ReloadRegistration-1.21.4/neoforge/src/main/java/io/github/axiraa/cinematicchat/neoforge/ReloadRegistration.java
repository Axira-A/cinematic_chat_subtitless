package io.github.axiraa.cinematicchat.neoforge;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
public final class ReloadRegistration {
    private ReloadRegistration(){}
    public static void register(IEventBus bus){bus.addListener((AddClientReloadListenersEvent event)->event.addListener(ResourceLocation.fromNamespaceAndPath("cinematic_chat","resources"),(ResourceManagerReloadListener)manager->CinematicChatClient.get().resourcesReloaded()));}
}
