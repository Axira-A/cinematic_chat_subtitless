package io.github.axiraa.cinematicchat.fabric;
import io.github.axiraa.cinematicchat.minecraft.*;
import io.github.axiraa.cinematicchat.engine.CcmlParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
public final class FabricCommands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher){for(String alias:new String[]{"cchat","cinematicchat"})dispatcher.register(ClientCommands.literal(alias)
        .executes(ctx->openConfig())
        .then(ClientCommands.literal("config").executes(ctx->openConfig()))
        .then(ClientCommands.literal("hud").executes(ctx->openHud()))
        .then(ClientCommands.literal("voice").executes(ctx->openVoices())
            .then(ClientCommands.literal("list").executes(ctx->{for(var profile:CinematicChatClient.get().voices().profiles)ctx.getSource().sendFeedback(Component.literal(profile.id+" "+profile.playMode+" "+profile.audio));return 1;}))
            .then(ClientCommands.literal("reload").executes(ctx->{CinematicChatClient.get().reloadAudio();return 1;}))
            .then(ClientCommands.literal("test").then(ClientCommands.argument("voiceId",StringArgumentType.word()).executes(ctx->{String id=StringArgumentType.getString(ctx,"voiceId");var client=CinematicChatClient.get();var config=client.configs().copy(client.voices(),io.github.axiraa.cinematicchat.client.VoiceConfig.class);var profile=new io.github.axiraa.cinematicchat.client.VoiceProfileResolver(config).byId(id);if(profile==null)return 0;ClientTasks.defer(()->{var screen=new VoiceProfileScreen(new VoiceProfilesScreen(null),config,profile);Minecraft.getInstance().setScreen(screen);screen.test(false);});return 1;})))
            .then(ClientCommands.literal("assign").then(ClientCommands.argument("speaker",StringArgumentType.string()).then(ClientCommands.argument("voiceId",StringArgumentType.word()).executes(ctx->{var client=CinematicChatClient.get();var copy=client.configs().copy(client.voices(),io.github.axiraa.cinematicchat.client.VoiceConfig.class);var profile=new io.github.axiraa.cinematicchat.client.VoiceProfileResolver(copy).byId(StringArgumentType.getString(ctx,"voiceId"));if(profile==null)return 0;String speaker=StringArgumentType.getString(ctx,"speaker");if(!profile.names.contains(speaker))profile.names.add(speaker);client.applyVoices(copy);return 1;})))))
        .then(ClientCommands.literal("clear").executes(ctx->{CinematicChatClient.get().controller().clear();return 1;}))
        .then(ClientCommands.literal("reload").executes(ctx->{CinematicChatClient.get().reload();return 1;}))
        .then(ClientCommands.literal("preview").executes(ctx->{CinematicChatClient.get().sample(CinematicChatClient.get().controller());return 1;}))
        .then(ClientCommands.literal("debug").executes(ctx->{var c=CinematicChatClient.get().config();c.debug=!c.debug;return 1;}))
        .then(ClientCommands.literal("parser").then(ClientCommands.argument("text",StringArgumentType.greedyString()).executes(ctx->{var doc=new CcmlParser().parse(StringArgumentType.getString(ctx,"text"));ctx.getSource().sendFeedback(Component.literal("CCML nodes="+doc.nodes().size()+" diagnostics="+doc.diagnostics()));return 1;}))));}
    private static int openConfig(){var mc=Minecraft.getInstance();ClientTasks.defer(()->mc.setScreen(new CinematicConfigScreen(null)));return 1;}
    private static int openHud(){var mc=Minecraft.getInstance();ClientTasks.defer(()->mc.setScreen(new SubtitleHudEditorScreen(null)));return 1;}
    private static int openVoices(){var mc=Minecraft.getInstance();ClientTasks.defer(()->mc.setScreen(new VoiceProfilesScreen(null)));return 1;}
    private FabricCommands(){}
}
