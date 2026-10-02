package io.github.axiraa.cinematicchat.smoke;

import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.engine.*;
import io.github.axiraa.cinematicchat.minecraft.*;
import io.github.axiraa.cinematicchat.minecraft.mixin.ChatHistoryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Native language reloads and UI feedback; production chat is never translated. */
public final class LocalizationScenario {
    private static final String[] LANGUAGES={"en_us","zh_cn","ja_jp","ko_kr"};
    private static final String[] WELCOME={"Hello, welcome to Cinematic Chat.","您好，欢迎使用Cinematic Chat。","こんにちは、Cinematic Chatへようこそ。","안녕하세요. Cinematic Chat에 오신 것을 환영합니다."};
    private static int language,stage;
    private static String originalLanguage;
    private static CompletableFuture<Void> reload;
    private LocalizationScenario(){}
    public static boolean tick(Minecraft mc,CinematicChatClient client,Map<String,Object> results){
        if(language==LANGUAGES.length){
            if(stage==0){select(mc,originalLanguage);stage++;return false;}
            if(!reload.isDone()||mc.getOverlay()!=null)return false;
            reload.join();mc.setScreen(null);client.controller().clear();return true;
        }
        String suffix=LANGUAGES[language];
        switch(stage){
            case 0->{if(originalLanguage==null)originalLanguage=mc.options.languageCode;select(mc,suffix);stage++;}
            case 1->{
                if(!reload.isDone()||mc.getOverlay()!=null)return false;reload.join();
                require(UiText.welcome().equals(WELCOME[language]),"welcome_"+suffix,results);
                mc.setScreen(new CinematicConfigScreen(null));widgets(mc.screen,suffix,results);
                sections(mc.screen,suffix,results);
                var preview=client.preview(client.configs().copy(client.config(),CinematicConfig.class));client.sample(preview);
                preview.tick(System.nanoTime(),new ClientState(true,false,false,false));
                require(preview.queue().visible().size()==2&&preview.queue().visible().stream().allMatch(e->WELCOME[language].equals(e.pending().envelope().body().text())&&java.util.Set.of("Steve","Alex").contains(e.pending().envelope().speaker().profileName())),"neutral_preview_"+suffix,results);
                preview.clear();stage++;
            }
            case 2->{ScenarioPlatform.screenshot(mc,"lang-"+suffix+"-config.png");mc.setScreen(new SubtitleHudEditorScreen(null));widgets(mc.screen,suffix,results);require(UiText.lines(Component.translatable("cinematic_chat.hud.help"),mc.font,mc.screen.width-16).size()>=2,"wrapped_hud_help_"+suffix,results);stage++;}
            case 3->{ScenarioPlatform.screenshot(mc,"lang-"+suffix+"-hud.png");mc.screen.onClose();var owner=new VoiceConfig();var profile=new VoiceProfile();profile.id="steve_preview";owner.profiles.add(profile);mc.setScreen(new VoiceProfileScreen(new VoiceProfilesScreen(null),owner,profile));widgets(mc.screen,suffix,results);((VoiceProfileScreen)mc.screen).test(true);stage++;}
            case 4->{ScenarioPlatform.screenshot(mc,"lang-"+suffix+"-voice.png");mc.screen.onClose();mc.setScreen(ScenarioPlatform.chat("/cchat parser [pause=abc]"));ScenarioPlatform.enter(mc);stage++;}
            case 5->{String expected=UiText.parser(new CcmlParser().parse("[pause=abc]")).getString();require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().anyMatch(m->m.content().getString().equals(expected)),"parser_feedback_"+suffix,results);mc.setScreen(null);client.controller().clear();ScenarioPlatform.addSystemMessage(mc,Component.literal(WELCOME[0]));stage++;}
            case 6->{require(((ChatHistoryAccessor)mc.gui.getChat()).cinematicChat$history().stream().anyMatch(m->m.content().getString().equals(WELCOME[0])),"original_chat_"+suffix,results);require(client.controller().queue().visible().stream().anyMatch(e->e.pending().envelope().body().text().equals(WELCOME[0])),"original_subtitle_"+suffix,results);language++;stage=0;}
            default->throw new IllegalStateException("Unknown localization stage "+stage);
        }
        return false;
    }
    private static void select(Minecraft mc,String language){mc.getLanguageManager().setSelected(language);mc.options.languageCode=language;reload=mc.reloadResourcePacks();}
    private static void sections(Screen screen,String suffix,Map<String,Object> results){
        String[] sections={"general","hud","typography","animation","messages","ccml","voice","accessibility","advanced"};
        for(String section:sections){
            String label=Component.translatable("cinematic_chat.section."+section).getString();
            Button selected=null;for(var child:screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(label)){selected=button;break;}
            if(selected==null)throw new AssertionError("Missing section "+section);
            double x=selected.getX()+selected.getWidth()/2.0,y=selected.getY()+selected.getHeight()/2.0;
            ScenarioPlatform.click(screen,x,y,0);ScenarioPlatform.release(screen,x,y,0);widgets(screen,suffix,results);
        }
    }
    private static void widgets(Screen screen,String suffix,Map<String,Object> results){
        require(!screen.getTitle().getString().startsWith("cinematic_chat."),"screen_title_"+screen.getClass().getSimpleName()+"_"+suffix,results);
        for(var child:screen.children())if(child instanceof AbstractWidget button){
            String text=button.getMessage().getString();require(!text.contains("cinematic_chat."),"button_labels_"+screen.getClass().getSimpleName()+"_"+suffix,results);
            if(Minecraft.getInstance().font.width(button.getMessage())>button.getWidth()-8)require(NativeSmokeProbes.hasTooltip(button),"long_label_tooltip_"+screen.getClass().getSimpleName()+"_"+suffix,results);
        }
    }
    private static void require(boolean condition,String key,Map<String,Object> results){results.put(key,condition);if(!condition)throw new AssertionError(key);}
}
