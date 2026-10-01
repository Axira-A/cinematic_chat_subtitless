package io.github.axiraa.cinematicchat.fabric;
import io.github.axiraa.cinematicchat.minecraft.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
public final class CinematicChatFabric implements ClientModInitializer {
    public void onInitializeClient(){
        new CinematicChatClient();
        var configKey=KeyMappingHelper.registerKeyMapping(ClientKeyBindings.create("key.cinematic_chat.config"));
        var hudKey=KeyMappingHelper.registerKeyMapping(ClientKeyBindings.create("key.cinematic_chat.hud"));
        ClientReceiveMessageEvents.CHAT.register((message,signed,sender,bound,time)->CinematicChatClient.get().capture().context(message,bound,sender==null?null:PlayerProfileBridge.id(sender),signed,false));
        ClientReceiveMessageEvents.GAME.register((message,overlay)->CinematicChatClient.get().capture().context(message,null,null,null,overlay));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{CinematicChatClient.get().tick();while(configKey.consumeClick())mc.setScreen(new CinematicConfigScreen(mc.screen));while(hudKey.consumeClick())mc.setScreen(new SubtitleHudEditorScreen(mc.screen));});
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("cinematic_chat","subtitles"),(graphics,delta)->CinematicChatClient.get().render(graphics));
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath("cinematic_chat","fonts_and_audio"),(ResourceManagerReloadListener)manager->CinematicChatClient.get().resourcesReloaded());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->FabricCommands.register(dispatcher));
    }
}
