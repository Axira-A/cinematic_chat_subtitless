package io.github.axiraa.cinematicchat.neoforge;
import io.github.axiraa.cinematicchat.minecraft.*;
import io.github.axiraa.cinematicchat.engine.CcmlParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
public final class ClientCommands {
    public static void register(RegisterClientCommandsEvent event){for(String alias:new String[]{"cchat","cinematicchat"})event.getDispatcher().register(Commands.literal(alias)
        .executes(ctx->openConfig())
        .then(Commands.literal("config").executes(ctx->openConfig()))
        .then(Commands.literal("hud").executes(ctx->openHud()))
        .then(Commands.literal("voice").executes(ctx->openVoices())
            .then(Commands.literal("list").executes(ctx->{for(var profile:CinematicChatClient.get().voices().profiles)ctx.getSource().sendSuccess(()->Component.translatable("cinematic_chat.command.voice_row",profile.id,Component.translatable("cinematic_chat.voice.mode."+profile.playMode.name().toLowerCase(java.util.Locale.ROOT)),profile.audio.toString()),false);return 1;}))
            .then(Commands.literal("reload").executes(ctx->{CinematicChatClient.get().reloadAudio();return 1;}))
            .then(Commands.literal("test").then(Commands.argument("voiceId",StringArgumentType.word()).executes(ctx->{String id=StringArgumentType.getString(ctx,"voiceId");var client=CinematicChatClient.get();var config=client.configs().copy(client.voices(),io.github.axiraa.cinematicchat.client.VoiceConfig.class);var profile=new io.github.axiraa.cinematicchat.client.VoiceProfileResolver(config).byId(id);if(profile==null){ctx.getSource().sendFailure(Component.translatable("cinematic_chat.command.unknown_voice",StringArgumentType.getString(ctx,"voiceId")));return 0;}ClientTasks.defer(()->{var screen=new VoiceProfileScreen(new VoiceProfilesScreen(null),config,profile);Minecraft.getInstance().setScreen(screen);screen.test(false);});return 1;})))
            .then(Commands.literal("assign").then(Commands.argument("speaker",StringArgumentType.string()).then(Commands.argument("voiceId",StringArgumentType.word()).executes(ctx->{var client=CinematicChatClient.get();var copy=client.configs().copy(client.voices(),io.github.axiraa.cinematicchat.client.VoiceConfig.class);var profile=new io.github.axiraa.cinematicchat.client.VoiceProfileResolver(copy).byId(StringArgumentType.getString(ctx,"voiceId"));if(profile==null){ctx.getSource().sendFailure(Component.translatable("cinematic_chat.command.unknown_voice",StringArgumentType.getString(ctx,"voiceId")));return 0;}String speaker=StringArgumentType.getString(ctx,"speaker");if(!profile.names.contains(speaker))profile.names.add(speaker);client.applyVoices(copy);ctx.getSource().sendSuccess(()->Component.translatable("cinematic_chat.command.voice_assigned",speaker,profile.id),false);return 1;})))))
        .then(Commands.literal("clear").executes(ctx->{CinematicChatClient.get().controller().clear();return 1;}))
        .then(Commands.literal("reload").executes(ctx->{CinematicChatClient.get().reload();return 1;}))
        .then(Commands.literal("preview").executes(ctx->{CinematicChatClient.get().sample(CinematicChatClient.get().controller());return 1;}))
        .then(Commands.literal("debug").executes(ctx->{var c=CinematicChatClient.get().config();c.debug=!c.debug;return 1;}))
        .then(Commands.literal("parser").then(Commands.argument("text",StringArgumentType.greedyString()).executes(ctx->{var doc=new CcmlParser().parse(StringArgumentType.getString(ctx,"text"));ctx.getSource().sendSuccess(()->UiText.parser(doc),false);return 1;}))));}
    private static int openConfig(){var mc=Minecraft.getInstance();ClientTasks.defer(()->mc.setScreen(new CinematicConfigScreen(null)));return 1;}
    private static int openHud(){var mc=Minecraft.getInstance();ClientTasks.defer(()->mc.setScreen(new SubtitleHudEditorScreen(null)));return 1;}
    private static int openVoices(){var mc=Minecraft.getInstance();ClientTasks.defer(()->mc.setScreen(new VoiceProfilesScreen(null)));return 1;}
    private ClientCommands(){}
}
