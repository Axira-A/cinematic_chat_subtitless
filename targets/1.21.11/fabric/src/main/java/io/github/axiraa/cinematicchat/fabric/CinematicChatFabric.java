package io.github.axiraa.cinematicchat.fabric;
import io.github.axiraa.cinematicchat.minecraft.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
public final class CinematicChatFabric implements ClientModInitializer {
    public void onInitializeClient(){
        new CinematicChatClient();
        var configKey=KeyBindingHelper.registerKeyBinding(ClientKeyBindings.create("key.cinematic_chat.config"));
        var hudKey=KeyBindingHelper.registerKeyBinding(ClientKeyBindings.create("key.cinematic_chat.hud"));
        ClientReceiveMessageEvents.CHAT.register((message,signed,sender,bound,time)->CinematicChatClient.get().capture().context(message,bound,sender==null?null:PlayerProfileBridge.id(sender),signed,false));
        ClientReceiveMessageEvents.GAME.register((message,overlay)->CinematicChatClient.get().capture().context(message,null,null,null,overlay));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{CinematicChatClient.get().tick();while(configKey.consumeClick())mc.setScreen(new CinematicConfigScreen(mc.screen));while(hudKey.consumeClick())mc.setScreen(new SubtitleHudEditorScreen(mc.screen));});
        HudRenderCallback.EVENT.register((graphics,delta)->CinematicChatClient.get().render(GuiCanvas.wrap(graphics)));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            public Identifier getFabricId(){return Identifier.fromNamespaceAndPath("cinematic_chat","fonts_and_audio");}
            public void onResourceManagerReload(ResourceManager manager){CinematicChatClient.get().resourcesReloaded();}
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->FabricCommands.register(dispatcher));
    }
}
