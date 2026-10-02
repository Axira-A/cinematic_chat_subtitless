package io.github.axiraa.cinematicchat.smoke;

import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Full local GUI flows and real window ratios, only in development builds. */
public final class NativeAcceptanceScenario {
    private static final String PROFILE="smoke_gui_voice";
    private static int step,windowWidth,windowHeight,guiScale;
    private static long plays;
    private static VoiceConfig originalVoices;
    private static CompletableFuture<Void> reload;
    private NativeAcceptanceScenario(){}
    public static boolean tick(Minecraft mc,CinematicChatClient client,Map<String,Object> results){
        switch(step++){
            case 0->{originalVoices=client.configs().copy(client.voices(),VoiceConfig.class);windowWidth=mc.getWindow().getWidth();windowHeight=mc.getWindow().getHeight();guiScale=mc.options.guiScale().get();mc.options.guiScale().set(2);resize(mc,1920,1080);}
            case 1->{require(Math.abs(ratio(mc)-16.0/9)<.01,"native_window_16_9",results);mc.setScreen(new SubtitleHudEditorScreen(null));ScenarioPlatform.screenshot(mc,"10-hud-16-9.png");}
            case 2->{var screen=(SubtitleHudEditorScreen)mc.screen;
                for(var anchor:HudSettings.Anchor.values()){
                    ScenarioPlatform.click(screen,Math.clamp(anchor.x*screen.width,4,screen.width-4),Math.clamp(anchor.y*screen.height,4,screen.height-4),0);
                    require(screen.workingCopy().hud.anchor==anchor,"native_anchor_"+anchor.name().toLowerCase(java.util.Locale.ROOT),results);
                }
                clickKey(screen,"cinematic_chat.hud.reset_position");require(screen.workingCopy().hud.anchor==HudSettings.Anchor.BOTTOM_CENTER&&screen.workingCopy().hud.offsetY==-.22,"native_reset_position",results);
                screen.workingCopy().hud.scale=1.7;screen.workingCopy().hud.widthRatio=.2;clickKey(screen,"cinematic_chat.hud.reset_size");require(screen.workingCopy().hud.scale==1&&screen.workingCopy().hud.widthRatio==.45,"native_reset_size",results);
                var trust=screen.workingCopy().ccmlTrustMode;screen.workingCopy().shadow=false;clickKey(screen,"cinematic_chat.hud.reset");require(screen.workingCopy().shadow&&screen.workingCopy().ccmlTrustMode==trust,"native_reset_hud_visual_only",results);
            }
            case 3->{var screen=(SubtitleHudEditorScreen)mc.screen;screen.workingCopy().hud.snap=false;var box=screen.bounds();double width=screen.workingCopy().hud.width(screen.width),scale=screen.workingCopy().hud.scale;
                ScenarioPlatform.click(screen,box.left(),box.top()+box.height()/2,0);ScenarioPlatform.drag(screen,box.left()+20,box.top()+box.height()/2,0,20,0);ScenarioPlatform.release(screen,box.left()+20,box.top()+box.height()/2,0);
                require(screen.workingCopy().hud.width(screen.width)<width&&screen.workingCopy().hud.scale==scale,"native_left_resize_changes_width_only",results);
                screen.workingCopy().hud.snap=true;screen.workingCopy().hud.position(screen.width/2.0+3,screen.height*.78,screen.width,screen.height);box=screen.bounds();double x=box.left()+box.width()/2,y=box.top()+box.height()/2;ScenarioPlatform.click(screen,x,y,0);ScenarioPlatform.release(screen,x,y,0);
                require(Math.abs(screen.workingCopy().hud.x(screen.width)-screen.width/2.0)<.01,"native_center_snap",results);resize(mc,2520,1080);
            }
            case 4->{require(Math.abs(ratio(mc)-21.0/9)<.01,"native_window_21_9",results);normalized((SubtitleHudEditorScreen)mc.screen,results,"21_9");ScenarioPlatform.screenshot(mc,"11-hud-21-9.png");resize(mc,1600,1200);}
            case 5->{require(Math.abs(ratio(mc)-4.0/3)<.01,"native_window_4_3",results);normalized((SubtitleHudEditorScreen)mc.screen,results,"4_3");ScenarioPlatform.screenshot(mc,"12-hud-4-3.png");resize(mc,1920,1200);step=22;}
            case 6->{((SubtitleHudEditorScreen)mc.screen).onClose();mc.setScreen(new VoiceProfilesScreen(null,new VoiceConfig(),client::applyVoices));clickKey(mc.screen,"cinematic_chat.voice.enabled");clickKey(mc.screen,"cinematic_chat.voice.new");}
            case 7->{require(mc.screen instanceof VoiceProfileScreen,"native_voice_new_profile",results);field(mc.screen,"id",PROFILE);field(mc.screen,"aliases","Steve, Alex");clickKey(mc.screen,"cinematic_chat.voice.audio");clickKey(mc.screen,"cinematic_chat.voice.add_audio");}
            case 8->{if(!(mc.screen instanceof LocalChoiceScreen)){step--;return false;}clickText(mc.screen,"cinematic-test-blip-1.ogg");}
            case 9->{clickKey(mc.screen,"cinematic_chat.voice.add_audio");}
            case 10->{if(!(mc.screen instanceof LocalChoiceScreen)){step--;return false;}clickText(mc.screen,"cinematic-test-blip-2.ogg");}
            case 11->{clickKey(mc.screen,"cinematic_chat.voice.add_audio");}
            case 12->{if(!(mc.screen instanceof LocalChoiceScreen)){step--;return false;}clickText(mc.screen,"cinematic-test-blip-3.ogg");}
            case 13->{var profile=((VoiceProfileScreen)mc.screen).workingCopy();require(profile.audio.size()==3&&profile.aliases.contains("Steve"),"native_voice_alias_and_three_ogg_selected",results);ScenarioPlatform.screenshot(mc,"13-voice-selected-ogg.png");clickKey(mc.screen,"cinematic_chat.save");require(mc.screen instanceof VoiceProfilesScreen,"native_profile_saved_into_working_copy",results);clickKey(mc.screen,"cinematic_chat.save");require(client.voices().enabled&&client.voices().profiles.stream().anyMatch(p->p.id.equals(PROFILE)),"native_voice_config_saved",results);client.controller().clear();plays=client.audioBackend().plays();}
            case 14->{var profile=mc.player.getGameProfile();mc.getChatListener().handlePlayerChatMessage(PlayerChatMessage.unsigned(PlayerProfileBridge.id(profile),"[speed=12]您好，欢迎使用Cinematic Chat。"),profile,ChatType.bind(ChatType.CHAT,mc.level.registryAccess(),Component.literal("Steve").withStyle(net.minecraft.ChatFormatting.GOLD)));}
            case 15->{require(client.controller().queue().visible().stream().anyMatch(e->PROFILE.equals(e.pending().voiceId())),"native_colored_alias_matches_entry",results);require(client.audioBackend().plays()>plays,"native_alias_profile_plays",results);}
            case 16->{require(client.audioBackend().bufferCount()>=3,"native_multiple_ogg_buffers_used",results);reload=client.reload();}
            case 17->{if(!reload.isDone()){step--;return false;}reload.join();require(client.voices().profiles.stream().anyMatch(p->p.id.equals(PROFILE)&&p.aliases.contains("Steve")&&p.audio.size()==3),"native_voice_save_reload_round_trip",results);mc.setScreen(new VoiceProfilesScreen(null));clickTextPrefix(mc.screen,PROFILE+"  ");}
            case 18->{clickKey(mc.screen,"cinematic_chat.delete");require(((VoiceProfilesScreen)mc.screen).workingCopy().profiles.stream().noneMatch(p->p.id.equals(PROFILE)),"native_voice_profile_deleted",results);clickKey(mc.screen,"cinematic_chat.save");client.applyVoices(originalVoices);client.controller().clear();mc.options.guiScale().set(guiScale);resize(mc,windowWidth,windowHeight);}
            case 19->{ScenarioPlatform.addSystemMessage(mc,Component.literal("[speed=120]长文本测试"+"中文世界".repeat(250)));}
            case 20->{var bounds=client.previewBounds(client.controller(),mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());require(bounds.top()>=0&&bounds.bottom()<=mc.getWindow().getGuiScaledHeight()&&bounds.height()<=mc.getWindow().getGuiScaledHeight()*.8,"native_long_subtitle_bounds_fit_screen",results);ScenarioPlatform.screenshot(mc,"14-long-subtitle.png");}
            case 21->{client.controller().clear();return true;}
            case 22->{require(Math.abs(ratio(mc)-16.0/10)<.01,"native_window_16_10",results);normalized((SubtitleHudEditorScreen)mc.screen,results,"16_10");ScenarioPlatform.screenshot(mc,"12a-hud-16-10.png");resize(mc,2560,720);}
            case 23->{require(Math.abs(ratio(mc)-32.0/9)<.01,"native_window_32_9",results);normalized((SubtitleHudEditorScreen)mc.screen,results,"32_9");ScenarioPlatform.screenshot(mc,"12b-hud-32-9.png");resize(mc,854,480);}
            case 24->{require(mc.getWindow().getWidth()==854&&mc.getWindow().getHeight()==480,"native_small_window",results);normalized((SubtitleHudEditorScreen)mc.screen,results,"small");ScenarioPlatform.screenshot(mc,"12c-hud-small.png");resize(mc,1920,1080);step=6;}
            default->throw new IllegalStateException("Unknown native acceptance step "+step);
        }
        return false;
    }
    private static void normalized(SubtitleHudEditorScreen screen,Map<String,Object> results,String suffix){require(Math.abs(screen.workingCopy().hud.x(screen.width)/screen.width-.5)<.01&&Math.abs(screen.workingCopy().hud.y(screen.height)/screen.height-.78)<.01,"native_normalized_hud_"+suffix,results);}
    private static double ratio(Minecraft mc){return mc.getWindow().getWidth()/(double)mc.getWindow().getHeight();}
    private static void resize(Minecraft mc,int width,int height){mc.getWindow().setWindowed(width,height);ScenarioPlatform.resizeGui(mc);}
    private static void field(Screen screen,String key,String value){String label=Component.translatable("cinematic_chat.voice."+key).getString();for(var child:screen.children())if(child instanceof EditBox field&&field.getMessage().getString().equals(label)){field.setValue(value);return;}throw new IllegalStateException("Missing field "+key);}
    private static void clickKey(Screen screen,String key){clickTextPrefix(screen,Component.translatable(key).getString());}
    private static void clickText(Screen screen,String text){for(var child:screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(text)){click(screen,button);return;}throw new IllegalStateException("Missing button "+text);}
    private static void clickTextPrefix(Screen screen,String text){for(var child:screen.children())if(child instanceof Button button&&button.getMessage().getString().startsWith(text)){click(screen,button);return;}throw new IllegalStateException("Missing button "+text);}
    private static void click(Screen screen,Button button){double x=button.getX()+button.getWidth()/2.0,y=button.getY()+button.getHeight()/2.0;ScenarioPlatform.click(screen,x,y,0);ScenarioPlatform.release(screen,x,y,0);}
    private static void require(boolean condition,String key,Map<String,Object> results){results.put(key,condition);if(!condition)throw new IllegalStateException("Native acceptance failed: "+key);}
}
