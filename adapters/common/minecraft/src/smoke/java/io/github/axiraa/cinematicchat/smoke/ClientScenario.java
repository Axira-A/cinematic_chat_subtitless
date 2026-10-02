package io.github.axiraa.cinematicchat.smoke;
import io.github.axiraa.cinematicchat.minecraft.*;
import io.github.axiraa.cinematicchat.client.ConfigManager;
import io.github.axiraa.cinematicchat.engine.*;
import io.github.axiraa.cinematicchat.minecraft.mixin.ChatHistoryAccessor;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.*;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;




import java.nio.file.*;
import java.util.*;

/** Development-only scenario, excluded from release jars. It never runs without the smoke property. */

public final class ClientScenario {
    private static final Map<String,Object> results=new LinkedHashMap<>();private static int phase,ticks;private static boolean creating,nativeDone;
    private static final long began=System.nanoTime();private static io.github.axiraa.cinematicchat.client.CinematicConfig originalConfig;private static java.util.concurrent.CompletableFuture<Void> testAudio;private static long audioPlays;private static VoiceProfilesScreen voiceParent;
    private static java.util.concurrent.CompletableFuture<io.github.axiraa.cinematicchat.api.PlatformAudioBackend.VoiceHandle> nativeHandle;
    private static java.util.concurrent.CompletableFuture<NativeSmokeProbes.AudioState> nativeState;private static double originalVoiceGain;
    private static MessageSignature withdrawn;private static boolean admissionFilter;
    private static java.util.concurrent.CompletableFuture<Void> configReload;
    public static void tick(){
        if(!Boolean.getBoolean("cinematic_chat.smoke"))return;var mc=Minecraft.getInstance();
        try {
            if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.pauseOnLostFocus=false;mc.setScreen(new TitleScreen());}
            if((System.nanoTime()-began)/1e9>360)throw new IllegalStateException("Smoke timeout phase="+phase+" screen="+mc.screen);
            if(!creating&&mc.screen instanceof TitleScreen){creating=true;mc.options.pauseOnLostFocus=false;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getTutorial().stop();mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);
                String name="cchat-smoke-"+System.currentTimeMillis();
                ScenarioPlatform.createWorld(mc,name);return;}
            if(mc.level==null||mc.player==null)return;
            if(++ticks%20!=0)return;var client=CinematicChatClient.get();
            switch(phase++){
                case 0->{results.put("entered_world",true);mc.setScreen(null);client.controller().clear();ScenarioPlatform.addSystemMessage(mc,Component.literal("您好，\\n[pause=1.2]欢迎使用Cinematic Chat。"));}
                case 1->{require(client.controller().queue().visible().size()==1,"capture_exactly_once");require(client.hideVanilla(),"hide_gameplay_chat");ScenarioPlatform.screenshot(mc,"01-subtitles.png");
                    mc.setScreen(ScenarioPlatform.chat(""));}
                case 2->{require(!client.hideVanilla(),"restore_chat_screen");results.put("chat_screen_opens",true);require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().anyMatch(m->m.content().getString().contains("[pause=1.2]")),"original_history_keeps_markup");double time=client.controller().queue().visible().getFirst().runtime().snapshot().time();results.put("pause_started_time",time);ScenarioPlatform.screenshot(mc,"02-chat-history.png");}
                case 3->{double before=((Number)results.get("pause_started_time")).doubleValue();double after=client.controller().queue().visible().getFirst().runtime().snapshot().time();require(Math.abs(before-after)<.001,"chat_screen_pauses_timeline");mc.setScreen(null);}
                case 4->{double before=((Number)results.get("pause_started_time")).doubleValue();require(client.controller().queue().visible().getFirst().runtime().snapshot().time()>before,"resume_timeline");mc.options.hideGui=true;}
                case 5->{ScenarioPlatform.screenshot(mc,"03-f1.png");results.put("f1_scenario_executed",true);mc.options.hideGui=false;
                    client.controller().clear();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(),"say [speed=15]服务器字幕测试"));}
                case 6->{require(client.controller().queue().visible().stream().anyMatch(e->e.pending().envelope().source()==io.github.axiraa.cinematicchat.api.ChatSource.SAY),"say_captured_structured");
                    var profile=mc.player.getGameProfile();var bound=ChatType.bind(ChatType.CHAT,mc.level.registryAccess(),mc.player.getDisplayName());mc.getChatListener().handlePlayerChatMessage(PlayerChatMessage.unsigned(PlayerProfileBridge.id(profile),"玩家字幕测试"),profile,bound);}
                case 7->{require(client.controller().queue().visible().stream().anyMatch(e->e.pending().envelope().source()==io.github.axiraa.cinematicchat.api.ChatSource.PLAYER_CHAT),"player_chat_captured");
                    ScenarioPlatform.addSystemMessage(mc,Component.literal("[pause=abc] 👨‍👩‍👧‍👦 é 中文"));results.put("malformed_ccml_scenario_executed",true);}
                case 8->{mc.setScreen(new CinematicConfigScreen(null));}
                case 9->{ScenarioPlatform.screenshot(mc,"04-config.png");results.put("config_screen_opens",true);mc.setScreen(null);mc.options.guiScale().set(3);ScenarioPlatform.resizeGui(mc);}
                case 10->{ScenarioPlatform.screenshot(mc,"05-gui-scale.png");results.put("gui_scale_scenario_executed",true);
                    Component rich=ScenarioPlatform.rich();
                    MessageSignature sig=new MessageSignature(new byte[256]);ScenarioPlatform.addPlayerMessage(mc,rich,sig,null);
                    var original=((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().getFirst();require(original.content()==rich&&original.signature()==sig,"component_and_signature_identity_preserved");require(original.content().getStyle().getClickEvent()!=null&&original.content().getStyle().getHoverEvent()!=null,"click_hover_retained");
                    mc.setScreen(ScenarioPlatform.chat("/say command-input-test"));ScenarioPlatform.enter(mc);}
                case 11->{require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().anyMatch(m->m.content().getString().contains("command-input-test")),"vanilla_command_input_sent");mc.getConnection().sendChat("normal-player-input-test");}
                case 12->{require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().anyMatch(m->m.content().getString().contains("normal-player-input-test")),"vanilla_chat_input_sent");
                    for(int i=0;i<100;i++)ScenarioPlatform.addSystemMessage(mc,Component.literal("burst "+i));}
                case 13->{require(client.controller().queue().queuedSize()<=50&&client.controller().queue().visible().size()<=3,"burst_remains_bounded");configReload=client.reload();mc.getLanguageManager().setSelected("zh_cn");mc.options.languageCode="zh_cn";mc.reloadResourcePacks();}
                case 14->{if(mc.getOverlay()!=null||!configReload.isDone()){phase--;return;}configReload.join();results.put("background_config_reload_completed",true);results.put("resource_reload_completed",true);require(Component.translatable("cinematic_chat.save").getString().equals("保存"),"chinese_language_resource_loaded");originalConfig=client.configs().copy(client.config(),io.github.axiraa.cinematicchat.client.CinematicConfig.class);mc.setScreen(new SubtitleHudEditorScreen(null));}
                case 15->{require(mc.screen instanceof SubtitleHudEditorScreen,"hud_editor_opens");var editor=(SubtitleHudEditorScreen)mc.screen;ScenarioPlatform.screenshot(mc,"06-hud-editor.png");var box=editor.bounds();double x=box.left()+box.width()/2,y=box.top()+box.height()/2;double before=editor.workingCopy().hud.x(editor.width);ScenarioPlatform.click(editor,x,y,0);ScenarioPlatform.drag(editor,x+40,y+8,0,40,8);ScenarioPlatform.release(editor,x+40,y+8,0);require(editor.workingCopy().hud.x(editor.width)>before+20,"hud_drag_updates_working_copy");require(client.config().hud.offsetX==originalConfig.hud.offsetX,"hud_edit_does_not_mutate_live_config");}
                case 16->{var editor=(SubtitleHudEditorScreen)mc.screen;var box=editor.bounds();double scale=editor.workingCopy().hud.scale,w=editor.workingCopy().hud.width(editor.width);ScenarioPlatform.click(editor,box.right(),box.top()+box.height()/2,0);ScenarioPlatform.drag(editor,box.right()-30,box.top()+box.height()/2,0,-30,0);ScenarioPlatform.release(editor,box.right()-30,box.top()+box.height()/2,0);require(editor.workingCopy().hud.width(editor.width)<w&&editor.workingCopy().hud.scale==scale,"hud_resize_changes_width_only");box=editor.bounds();ScenarioPlatform.click(editor,box.right(),box.bottom(),0);ScenarioPlatform.drag(editor,box.right()+15,box.bottom()+15,0,15,15);ScenarioPlatform.release(editor,box.right()+15,box.bottom()+15,0);require(editor.workingCopy().hud.scale>scale,"hud_corner_changes_scale");editor.onClose();require(client.config().hud.scale==originalConfig.hud.scale,"hud_cancel_restores_config");mc.setScreen(new SubtitleHudEditorScreen(null));}
                case 17->{var editor=(SubtitleHudEditorScreen)mc.screen;editor.workingCopy().hud.scale=1.25;ScenarioPlatform.click(editor,editor.width/2-50,editor.height-16,0);require(client.config().hud.scale==1.25,"hud_apply_saves_config");client.apply(originalConfig);mc.setScreen(new VoiceProfilesScreen(null));testAudio=client.installTestAudio();}
                case 18->{require(mc.screen instanceof VoiceProfilesScreen,"voice_profiles_screen_opens");ScenarioPlatform.screenshot(mc,"07-voice-profiles.png");if(!testAudio.isDone()){phase--;return;}testAudio.join();client.audio().load("cinematic-test-blip.wav");mc.setScreen(null);}
                case 19->{var clip=client.audio().ready("cinematic-test-blip.wav");if(clip==null){phase--;return;}require(clip.sampleRate()==22050&&clip.channels()==1,"external_wav_decoded");audioPlays=client.audioBackend().plays();client.audioBackend().play(clip,new io.github.axiraa.cinematicchat.api.VoicePlaybackParams(.4,1.2,true));}
                case 20->{require(client.audioBackend().plays()>audioPlays,"native_audio_played");require(client.audioBackend().bufferCount()>0,"native_audio_buffer_allocated");client.audioBackend().reload();}
                case 21->{require(client.audioBackend().bufferCount()==0,"native_audio_buffers_released");voiceParent=new VoiceProfilesScreen(null);mc.setScreen(voiceParent);var profile=new io.github.axiraa.cinematicchat.client.VoiceProfile();profile.id="smoke_voice";profile.audio.add("cinematic-test-blip.wav");profile.pitchRandomness=0;var screen=new VoiceProfileScreen(voiceParent,voiceParent.workingCopy(),profile);mc.setScreen(screen);audioPlays=client.audioBackend().plays();screen.test(true);}
                case 22->{require(mc.screen instanceof VoiceProfileScreen,"voice_profile_editor_opens");require(client.audioBackend().plays()>audioPlays,"pseudo_voice_reveal_played");ScenarioPlatform.screenshot(mc,"08-voice-profile.png");var screen=(VoiceProfileScreen)mc.screen;ScenarioPlatform.click(screen,screen.width/2,40,0);screen.mouseScrolled(200,100,0,-4);results.put("voice_form_scroll_executed",true);}
                case 23->{ScenarioPlatform.screenshot(mc,"09-voice-sound-settings.png");((VoiceProfileScreen)mc.screen).onClose();require(voiceParent.workingCopy().profiles.stream().noneMatch(p->p.id.equals("smoke_voice")),"voice_profile_cancel_isolated");voiceParent.onClose();
                    client.reloadAudio();for(int i=1;i<=3;i++)client.audio().load("cinematic-test-blip-"+i+".ogg");
                    testAudio=java.util.concurrent.CompletableFuture.runAsync(()->{try{Files.write(client.configs().directory().resolve("audio/smoke-invalid.ogg"),new byte[32]);}catch(java.io.IOException error){throw new java.util.concurrent.CompletionException(error);}}).thenCompose(ignored->java.util.concurrent.CompletableFuture.allOf(client.audio().load("smoke-invalid.ogg"),client.audio().load("smoke-missing.ogg")));}
                case 24->{if(!testAudio.isDone()){phase--;return;}testAudio.join();require(client.audio().load("smoke-invalid.ogg").join()==null,"corrupt_ogg_fails_without_blocking_subtitles");require(client.audio().load("smoke-missing.ogg").join()==null,"missing_audio_fails_without_crash");for(int i=1;i<=3;i++){var clip=client.audio().ready("cinematic-test-blip-"+i+".ogg");if(clip==null){phase--;return;}require(clip.sampleRate()==22050&&clip.channels()==1&&clip.pcm().remaining()==4410,"external_ogg_"+i+"_decoded");}
                    originalVoiceGain=mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.VOICE).get();mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.VOICE).set(1.0);client.audioBackend().tick();audioPlays=client.audioBackend().plays();
                    nativeHandle=client.audioBackend().play(client.audio().ready("cinematic-test-blip-1.ogg"),new io.github.axiraa.cinematicchat.api.VoicePlaybackParams(.4,1.2,true));}
                case 25->{require(client.audioBackend().plays()>audioPlays,"native_ogg_played");nativeState=NativeSmokeProbes.audio(nativeHandle.join());}
                case 26->{if(!nativeState.isDone()){phase--;return;}var state=nativeState.join();require(Math.abs(state.gain()-.4)<.001,"native_volume_applied");require(Math.abs(state.pitch()-1.2)<.001,"native_pitch_applied");require(state.state()==org.lwjgl.openal.AL10.AL_PLAYING,"native_loop_playing");mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.VOICE).set(.25);}
                case 27->{nativeState=NativeSmokeProbes.audio(nativeHandle.join());}
                case 28->{if(!nativeState.isDone()){phase--;return;}require(Math.abs(nativeState.join().gain()-.1)<.001,"native_voice_category_updates_live_channel");mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.VOICE).set(originalVoiceGain);client.reloadAudio();}
                case 29->{require(client.audioBackend().bufferCount()==0&&NativeSmokeProbes.activeVoices(client.audioBackend())==0,"audio_reload_releases_native_state");client.audio().load("cinematic-test-blip-1.ogg");}
                case 30->{var clip=client.audio().ready("cinematic-test-blip-1.ogg");if(clip==null){phase--;return;}audioPlays=client.audioBackend().plays();client.audioBackend().play(clip,new io.github.axiraa.cinematicchat.api.VoicePlaybackParams(.4,1,false));}
                case 31->{require(client.audioBackend().plays()>audioPlays&&client.audioBackend().bufferCount()==1,"audio_reload_recreates_native_buffer");client.controller().clear();
                    var voices=new io.github.axiraa.cinematicchat.client.VoiceConfig();voices.enabled=true;var profile=new io.github.axiraa.cinematicchat.client.VoiceProfile();profile.id="native_loop";profile.audio.add("cinematic-test-blip-1.ogg");profile.playMode=io.github.axiraa.cinematicchat.client.VoiceProfile.PlayMode.LOOP_WHILE_REVEALING;voices.profiles.add(profile);voices.defaultSystem=profile.id;client.controller().configure(client.config(),voices);
                    ScenarioPlatform.addSystemMessage(mc,Component.literal("[speed=1]abcdefghijklmnopqrstuvwxyz"));}
                case 32->{require(NativeSmokeProbes.activeVoices(client.audioBackend())>0,"pseudo_voice_loop_active");mc.options.hideGui=true;}
                case 33->{require(NativeSmokeProbes.activeVoices(client.audioBackend())==0,"f1_stops_pseudo_voice");mc.options.hideGui=false;}
                case 34->{require(NativeSmokeProbes.activeVoices(client.audioBackend())>0,"pseudo_voice_resumes_after_f1");mc.setScreen(ScenarioPlatform.chat(""));}
                case 35->{require(NativeSmokeProbes.activeVoices(client.audioBackend())==0,"chat_screen_stops_pseudo_voice");mc.setScreen(null);}
                case 36->{require(NativeSmokeProbes.activeVoices(client.audioBackend())>0,"pseudo_voice_resumes_after_chat_screen");client.controller().clear();client.controller().configure(client.config(),client.voices());
                    ScenarioPlatform.addSystemMessage(mc,Component.literal("identical-real-message"));ScenarioPlatform.addSystemMessage(mc,Component.literal("identical-real-message"));}
                case 37->{require(client.controller().queue().visible().stream().filter(e->e.pending().envelope().body().text().equals("identical-real-message")).count()==2,"identical_real_messages_both_captured");client.controller().clear();
                    withdrawn=new MessageSignature(new byte[256]);ScenarioPlatform.addPlayerMessage(mc,Component.literal("withdrawn-message"),withdrawn,null);mc.gui.getChat().deleteMessage(withdrawn);
                    var profile=mc.player.getGameProfile();mc.getChatListener().handlePlayerChatMessage(PlayerChatMessage.unsigned(PlayerProfileBridge.id(profile),"fully-filtered-message").filter(FilterMask.FULLY_FILTERED),profile,ChatType.bind(ChatType.CHAT,mc.level.registryAccess(),mc.player.getDisplayName()));}
                case 38->{require(client.controller().queue().visible().stream().noneMatch(e->Objects.equals(e.pending().envelope().signatureKey(),withdrawn.toString())),"withdrawn_message_removed_from_subtitles");require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().noneMatch(m->m.content().getString().contains("fully-filtered-message")),"vanilla_rejected_filtered_message");require(client.controller().queue().visible().stream().noneMatch(e->e.pending().envelope().body().text().contains("fully-filtered-message")),"filtered_message_not_recaptured");
                    mc.setScreen(ScenarioPlatform.chat("/cchat h"));}
                case 39->{var suggestions=NativeSmokeProbes.suggestions((ChatScreen)mc.screen);require(suggestions!=null&&suggestions.isDone()&&suggestions.join().getList().stream().anyMatch(s->s.getText().equals("hud")),"client_command_suggestions_work");
                    require(NativeSmokeProbes.suggestClick((ChatScreen)mc.screen,ScenarioPlatform.rich().getStyle()),"vanilla_component_click_handled");require(NativeSmokeProbes.input((ChatScreen)mc.screen).equals("/say hello"),"suggest_command_click_updates_input");mc.setScreen(ScenarioPlatform.chat("/cchat hud"));ScenarioPlatform.enter(mc);}
                case 40->{require(mc.screen instanceof SubtitleHudEditorScreen,"hud_client_command_opens_editor");((SubtitleHudEditorScreen)mc.screen).onClose();
                    try{var filter=mc.gui.getChat().getClass().getMethod("setVisibleMessageFilter",java.util.function.Predicate.class);filter.invoke(mc.gui.getChat(),(java.util.function.Predicate<Object>)message->false);admissionFilter=true;ScenarioPlatform.addSystemMessage(mc,Component.literal("admission-rejected-message"));}catch(NoSuchMethodException olderApi){results.put("admission_filter","not applicable to this Minecraft API");}}
                case 41->{if(admissionFilter){require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().noneMatch(m->m.content().getString().contains("admission-rejected-message")),"permission_filter_rejects_history");require(client.controller().queue().visible().isEmpty(),"permission_filter_rejects_subtitle_capture");mc.gui.getChat().getClass().getMethod("setVisibleMessageFilter",java.util.function.Predicate.class).invoke(mc.gui.getChat(),(java.util.function.Predicate<Object>)message->true);}}
                default->{if(!nativeDone)nativeDone=NativeAcceptanceScenario.tick(mc,client,results);else if(LocalizationScenario.tick(mc,client,results))finish(null);}
            }
        }catch(Exception|AssertionError error){finish(error);}
    }
    private static void require(boolean value,String test){results.put(test,value);if(!value)throw new AssertionError(test);}
    private static void finish(Throwable failure){var mc=Minecraft.getInstance();results.put("passed",failure==null);results.put("signed_chat_authenticated_test","pending: development profile is offline");if(failure!=null)results.put("failure",failure.toString());
        try{Files.writeString(mc.gameDirectory.toPath().resolve("smoke-report.json"),ConfigManager.JSON.toJson(results));}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}mc.stop();}
}
