package io.github.axiraa.cinematicchat.neoforge;
import io.github.axiraa.cinematicchat.minecraft.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
@Mod(value="cinematic_chat",dist=Dist.CLIENT)
public final class CinematicChatNeoForge {
    private static final KeyMapping CONFIG_KEY=ClientKeyBindings.create("key.cinematic_chat.config");
    private static final KeyMapping HUD_KEY=ClientKeyBindings.create("key.cinematic_chat.hud");
    public CinematicChatNeoForge(IEventBus modBus,ModContainer container){
        ClientBootstrap.register(modBus);
        container.registerExtensionPoint(IConfigScreenFactory.class,(IConfigScreenFactory)(ignored,parent)->new CinematicConfigScreen(parent));
        modBus.addListener((RegisterKeyMappingsEvent event)->{event.register(CONFIG_KEY);event.register(HUD_KEY);});
        ReloadRegistration.register(modBus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,ClientChatReceivedEvent.class,event->{
            if(event instanceof ClientChatReceivedEvent.System system&&system.isOverlay())return;
            var signed=event instanceof ClientChatReceivedEvent.Player p?p.getPlayerChatMessage():null;
            CinematicChatClient.get().capture().context(event.getMessage(),event.getBoundChatType(),event.getSender(),signed,false);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->{CinematicChatClient.get().tick();while(CONFIG_KEY.consumeClick())Minecraft.getInstance().setScreen(new CinematicConfigScreen(Minecraft.getInstance().screen));while(HUD_KEY.consumeClick())Minecraft.getInstance().setScreen(new SubtitleHudEditorScreen(Minecraft.getInstance().screen));});
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre event)->{if(event.getName().equals(VanillaGuiLayers.CHAT)&&CinematicChatClient.get().hideVanilla())event.setCanceled(true);});
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event)->CinematicChatClient.get().render(GuiCanvas.wrap(event.getGuiGraphics())));
        NeoForge.EVENT_BUS.addListener(ClientCommands::register);
    }
}
