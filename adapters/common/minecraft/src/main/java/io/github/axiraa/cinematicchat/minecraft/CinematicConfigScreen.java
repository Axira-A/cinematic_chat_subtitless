package io.github.axiraa.cinematicchat.minecraft;

import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.engine.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

public final class CinematicConfigScreen extends CinematicScreen implements PreviewScreen {
    private final Screen parent;
    private CinematicConfig working;
    private VoiceConfig workingVoices;
    private HudPresets workingPresets;
    private final SubtitleController preview;
    private int section, scroll;
    private final List<BiConsumer<Integer,Integer>> rows=new ArrayList<>();
    private int contentX, contentWidth, visibleRows;
    private static final String[] SECTIONS={"general","hud","typography","animation","messages","ccml","voice","accessibility","advanced"};
    public CinematicConfigScreen(Screen parent){
        super(Component.literal("Cinematic Chat"));this.parent=parent;var client=CinematicChatClient.get();
        working=client.configs().copy(client.config(),CinematicConfig.class);workingVoices=client.configs().copy(client.voices(),VoiceConfig.class);workingPresets=client.configs().copy(client.presets(),HudPresets.class);preview=client.preview(working,workingVoices);client.sample(preview);
    }
    public CinematicConfig workingCopy(){return working;}
    @Override protected void init(){
        clearWidgets();rows.clear();contentX=136;contentWidth=Math.max(100,Math.min(310,width-contentX-12));
        visibleRows=Math.max(2,(height-106)/25);
        if(height>=310){for(int i=0;i<SECTIONS.length;i++){int index=i;button(12,32+i*23,112,sectionLabel(i),()->{section=index;scroll=0;init();});}}
        else {button(12,32,112,sectionLabel(section),()->{section=(section+1)%SECTIONS.length;scroll=0;init();});button(12,56,54,Component.literal("<"),()->{section=(section+SECTIONS.length-1)%SECTIONS.length;scroll=0;init();});button(70,56,54,Component.literal(">"),()->{section=(section+1)%SECTIONS.length;scroll=0;init();});}
        switch(section){
            case 0->{toggle("enabled",()->working.enabled,v->working.enabled=v);toggle("show_sender",()->working.showSender,v->working.showSender=v);toggle("pause_chat",()->working.pauseWhenChatOpen,v->working.pauseWhenChatOpen=v);toggle("hide_chat",()->working.hideWhenChatOpen,v->working.hideWhenChatOpen=v);}
            case 1->{action("hud.open",()->minecraft.setScreen(new SubtitleHudEditorScreen(this,working,workingPresets,next->{working=next;preview.configure(working,workingVoices);},next->workingPresets=next)));slider("position_x",0,1,()->working.hud.x(width)/width,v->working.hud.position(v*width,working.hud.y(height),width,height));slider("position_y",0,1,()->working.hud.y(height)/height,v->working.hud.position(working.hud.x(width),v*height,width,height));cycle("anchor",()->working.hud.anchor,HudSettings.Anchor.values(),v->working.hud.changeAnchor(v,width,height));slider("scale",.5,3,()->working.hud.scale,v->working.hud.scale=v);slider("width_ratio",.05,.95,()->working.hud.widthRatio,v->working.hud.widthRatio=v);slider("width_pixels",120,2000,()->working.hud.absoluteMaxWidth,v->working.hud.absoluteMaxWidth=v);cycle("alignment",()->working.hud.alignment,SubtitleLayoutEngine.Alignment.values(),v->working.hud.alignment=v);toggle("background",()->working.backgroundEnabled,v->working.backgroundEnabled=v);slider("background_opacity",0,1,()->working.backgroundOpacity,v->working.backgroundOpacity=v);action("preset.load",()->minecraft.setScreen(new LocalChoiceScreen(this,label("preset.load"),new ArrayList<>(allPresets().keySet()),name->UiText.preset(name,workingPresets.presets.containsKey(name)),name->{allPresets().get(name).apply(working);refresh();})));}
            case 2->{text("font",()->working.fontId,v->working.fontId=v);toggle("shadow",()->working.shadow,v->working.shadow=v);slider("sender_scale",.5,2,()->working.senderScale,v->working.senderScale=v);slider("sender_opacity",0,1,()->working.senderOpacity,v->working.senderOpacity=v);slider("line_spacing",0,20,()->working.lineSpacing,v->working.lineSpacing=v);}
            case 3->{cycle("reveal",()->working.animation.reveal(),RevealMode.values(),v->setAnimation("reveal",v,0));engineSlider("speed",1,120,()->working.animation.speed());engineSlider("base_duration",0,30,()->working.animation.baseDuration());engineSlider("per_character",0,.5,()->working.animation.perCharacter());engineSlider("min_duration",0,30,()->working.animation.minDuration());engineSlider("max_duration",0,60,()->working.animation.maxDuration());cycle("entry",()->working.animation.entry(),Animation.Kind.values(),v->setAnimation("entry",v,0));engineSlider("entry_duration",0,3,()->working.animation.entryDuration());cycle("exit",()->working.animation.exit(),new Animation.Kind[]{Animation.Kind.NONE,Animation.Kind.FADE,Animation.Kind.SLIDE_UP,Animation.Kind.SLIDE_DOWN},v->setAnimation("exit",v,0));engineSlider("exit_duration",0,3,()->working.animation.exitDuration());cycle("entry_easing",working::effectiveEntryEasing,Animation.Easing.values(),v->working.entryEasing=v);cycle("exit_easing",working::effectiveExitEasing,Animation.Easing.values(),v->working.exitEasing=v);engineSlider("max_visible",1,10,()->working.animation.maxVisible());engineSlider("max_queued",1,200,()->working.animation.maxQueued());}
            case 4->{for(var source:ChatSource.values()){String key="source."+source.name().toLowerCase(Locale.ROOT);toggle(key,()->working.messageTypes.getOrDefault(source,true),v->working.messageTypes.put(source,v));cycle(key+".priority",()->working.priorities.get(source),SubtitleQueue.Priority.values(),v->working.priorities.put(source,v));}}
            case 5->{toggle("ccml.enabled",()->working.ccmlEnabled,v->working.ccmlEnabled=v);cycle("ccml.trust",()->working.ccmlTrustMode,CinematicConfig.TrustMode.values(),v->working.ccmlTrustMode=v);limitSlider("pause",0,10,()->working.ccmlLimits.pauseSeconds());limitSlider("hold",0,30,()->working.ccmlLimits.holdSeconds());limitSlider("min_scale",.5,2.5,()->working.ccmlLimits.minScale());limitSlider("max_scale",.5,2.5,()->working.ccmlLimits.maxScale());limitSlider("shake",0,4,()->working.ccmlLimits.shakePixels());limitSlider("wave",0,8,()->working.ccmlLimits.wavePixels());limitSlider("script",1,120,()->working.ccmlLimits.scriptSeconds());limitSlider("input",256,32768,()->working.ccmlLimits.inputLength());limitSlider("nodes",64,2048,()->working.ccmlLimits.nodes());limitSlider("depth",1,16,()->working.ccmlLimits.depth());}
            case 6->{action("voice.title",()->minecraft.setScreen(new VoiceProfilesScreen(this,workingVoices,next->{workingVoices=next;preview.configure(working,workingVoices);})));action("voice.reload",()->{CinematicChatClient.get().reloadAudio();preview.audioReloaded();});}
            case 7->{toggle("reduced_motion",()->working.reducedMotion,v->working.reducedMotion=v);toggle("disable_shake",()->working.disableShake,v->working.disableShake=v);toggle("disable_wave",()->working.disableWave,v->working.disableWave=v);action("instant",()->setAnimation("reveal",RevealMode.INSTANT,0));}
            case 8->{toggle("debug",()->working.debug,v->working.debug=v);var rule=working.editSpeakerRule(CinematicChatClient.get().serverKey());toggle("speaker_fallback",()->rule.enabled,v->rule.enabled=v);text("speaker_prefix",()->rule.prefix,v->rule.prefix=v);text("speaker_delimiter",()->rule.delimiter,v->rule.delimiter=v);}
        }
        scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-visibleRows));
        for(int i=0;i<visibleRows&&scroll+i<rows.size();i++)rows.get(scroll+i).accept(contentX,36+i*25);
        if(rows.size()>visibleRows){button(contentX+contentWidth-42,height-68,20,Component.literal("▲"),()->{scroll--;init();});button(contentX+contentWidth-20,height-68,20,Component.literal("▼"),()->{scroll++;init();});}
        button(12,height-76,112,label("reset_section"),()->{resetSection();refresh();});button(12,height-52,112,label("reset_all"),()->{working=new CinematicConfig();workingVoices=new VoiceConfig();refresh();});
        button(136,height-44,110,label("replay"),()->CinematicChatClient.get().sample(preview));
        button(width-218,height-24,100,label("save"),()->{CinematicChatClient.get().apply(working);CinematicChatClient.get().applyVoices(workingVoices);CinematicChatClient.get().applyPresets(workingPresets);onClose();});button(width-110,height-24,100,Component.translatable("gui.cancel"),this::onClose);
    }
    private Map<String,HudPresets.Visual> allPresets(){var all=new LinkedHashMap<>(HudPresets.builtins());all.putAll(workingPresets.presets);return all;}
    private static Component label(String key){return Component.translatable("cinematic_chat."+key);}
    private Component sectionLabel(int index){return label("section."+SECTIONS[index]);}
    private void button(int x,int y,int w,Component text,Runnable action){var button=Button.builder(text,b->action.run()).bounds(x,y,w,20).build();button.setTooltip(Tooltip.create(text));addRenderableWidget(button);}
    private void toggle(String key,BooleanSupplier value,Consumer<Boolean> setter){rows.add((x,y)->button(x,y,contentWidth,UiText.value(label(key),UiText.state(value.getAsBoolean())),()->{setter.accept(!value.getAsBoolean());refresh();}));}
    private void action(String key,Runnable action){rows.add((x,y)->button(x,y,contentWidth,label(key),action));}
    private <T>void cycle(String key,Supplier<T> value,T[] values,Consumer<T> setter){rows.add((x,y)->button(x,y,contentWidth,UiText.value(label(key),UiText.option((Enum<?>)value.get())),()->{int i=Arrays.asList(values).indexOf(value.get());setter.accept(values[(i+1)%values.length]);refresh();}));}
    private void slider(String key,double min,double max,DoubleSupplier value,DoubleConsumer setter){rows.add((x,y)->addRenderableWidget(new AbstractSliderButton(x,y,contentWidth,20,Component.empty(),CcmlLimits.clamp((value.getAsDouble()-min)/(max-min),0,1)){
        {updateMessage();}protected void updateMessage(){setMessage(label(key).copy().append(": "+String.format(Locale.ROOT,"%.2f",min+this.value*(max-min))));setTooltip(Tooltip.create(getMessage()));}protected void applyValue(){setter.accept(min+this.value*(max-min));}
    }));}
    private void text(String key,Supplier<String> value,Consumer<String> setter){rows.add((x,y)->{EditBox field=new EditBox(font,x,y,contentWidth,20,label(key));field.setMaxLength(128);field.setValue(value.get());field.setResponder(setter);field.setTooltip(Tooltip.create(label(key)));addRenderableWidget(field);});}
    private void engineSlider(String key,double min,double max,DoubleSupplier value){slider(key,min,max,value,v->setAnimation(key,null,v));}
    private void setAnimation(String key,Object object,double v){var a=working.animation;working.animation=new EngineConfig(key.equals("reveal")?(RevealMode)object:a.reveal(),key.equals("speed")?v:a.speed(),key.equals("base_duration")?v:a.baseDuration(),key.equals("per_character")?v:a.perCharacter(),key.equals("min_duration")?v:a.minDuration(),key.equals("max_duration")?v:a.maxDuration(),key.equals("entry_duration")?v:a.entryDuration(),key.equals("exit_duration")?v:a.exitDuration(),key.equals("entry")?(Animation.Kind)object:a.entry(),key.equals("exit")?(Animation.Kind)object:a.exit(),key.equals("easing")?(Animation.Easing)object:a.easing(),key.equals("max_visible")?(int)v:a.maxVisible(),key.equals("max_queued")?(int)v:a.maxQueued());}
    private void limitSlider(String key,double min,double max,DoubleSupplier value){slider("limit."+key,min,max,value,v->{var a=working.ccmlLimits;working.ccmlLimits=new CcmlLimits(key.equals("input")?(int)v:a.inputLength(),key.equals("nodes")?(int)v:a.nodes(),key.equals("depth")?(int)v:a.depth(),key.equals("pause")?v:a.pauseSeconds(),key.equals("hold")?v:a.holdSeconds(),key.equals("min_scale")?v:a.minScale(),key.equals("max_scale")?v:a.maxScale(),key.equals("shake")?v:a.shakePixels(),key.equals("wave")?v:a.wavePixels(),key.equals("script")?v:a.scriptSeconds());});}
    private void resetSection(){var d=new CinematicConfig();switch(section){case 0->{working.enabled=d.enabled;working.showSender=d.showSender;working.pauseWhenChatOpen=d.pauseWhenChatOpen;working.hideWhenChatOpen=d.hideWhenChatOpen;}case 1->{working.hud=d.hud;working.backgroundEnabled=d.backgroundEnabled;working.backgroundOpacity=d.backgroundOpacity;}case 2->{working.fontId=d.fontId;working.shadow=d.shadow;working.senderScale=d.senderScale;working.senderOpacity=d.senderOpacity;working.lineSpacing=d.lineSpacing;}case 3->{working.animation=d.animation;working.entryEasing=d.entryEasing;working.exitEasing=d.exitEasing;}case 4->{working.messageTypes=d.messageTypes;working.priorities=d.priorities;}case 5->{working.ccmlEnabled=d.ccmlEnabled;working.ccmlTrustMode=d.ccmlTrustMode;working.ccmlLimits=d.ccmlLimits;}case 6->workingVoices=new VoiceConfig();case 7->{working.reducedMotion=d.reducedMotion;working.disableShake=d.disableShake;working.disableWave=d.disableWave;}case 8->{working.debug=d.debug;working.serverSpeakerRules.remove(CinematicChatClient.get().serverKey());}default->{}}}
    private void refresh(){working.validate();preview.configure(working,workingVoices);CinematicChatClient.get().sample(preview);init();}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x>=contentX){scroll-=Integer.signum((int)vertical);init();return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean mouseReleased(double x,double y,int button){boolean used=super.mouseReleased(x,y,button);working.validate();preview.configure(working,workingVoices);return used;}
    @Override public void tick(){preview.tick(System.nanoTime(),new ClientState(true,false,Minecraft.getInstance().options.hideGui,false));if(preview.queue().visible().isEmpty())CinematicChatClient.get().sample(preview);}
    @Override public void renderContent(GuiCanvas g,int x,int y,float partialTick){cinematicBlur(g,partialTick);g.fill(0,0,width,height,0xb0101018);CinematicChatClient.get().drawPreview(g,preview);g.drawCenteredString(font,title,width/2,12,0xffffffff);super.renderContent(g,x,y,partialTick);}
    @Override public void renderBackdrop(GuiCanvas g,int x,int y,float partialTick){}
    @Override public void onClose(){preview.clear();minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
