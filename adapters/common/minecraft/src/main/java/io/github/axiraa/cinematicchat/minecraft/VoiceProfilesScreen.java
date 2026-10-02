package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.engine.CcmlLimits;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class VoiceProfilesScreen extends CinematicScreen implements PreviewScreen {
    private final Screen parent;private final VoiceConfig working;private int page;private boolean matching;
    private final java.util.function.Consumer<VoiceConfig> saved;
    public VoiceProfilesScreen(Screen parent){this(parent,CinematicChatClient.get().voices(),CinematicChatClient.get()::applyVoices);}
    public VoiceProfilesScreen(Screen parent,VoiceConfig initial,java.util.function.Consumer<VoiceConfig> saved){super(label("title"));this.parent=parent;this.saved=saved;working=CinematicChatClient.get().configs().copy(initial,VoiceConfig.class);}
    public VoiceConfig workingCopy(){return working;}
    private static Component label(String key){return Component.translatable("cinematic_chat.voice."+key);}
    @Override protected void init(){
        clearWidgets();int x=width/2-155;
        button(x,30,152,label("enabled").copy().append(": ").append(Component.translatable(working.enabled?"options.on":"options.off")),()->{working.enabled=!working.enabled;init();});
        button(x+158,30,152,label("new"),()->{var p=new VoiceProfile();int i=1;Set<String> ids=new HashSet<>();for(var v:working.profiles)ids.add(v.id);while(ids.contains("voice_"+i))i++;p.id="voice_"+i;minecraft.setScreen(new VoiceProfileScreen(this,working,p));});
        addRenderableWidget(new AbstractSliderButton(x,54,152,20,Component.empty(),working.masterVolume){
            {updateMessage();}protected void updateMessage(){setMessage(label("master_volume").copy().append(": "+String.format(Locale.ROOT,"%.2f",value)));}protected void applyValue(){working.masterVolume=CcmlLimits.clamp(value,0,1);}
        });
        button(x+158,54,152,label("reload"),()->CinematicChatClient.get().reloadAudio());
        if(!matching){
            for(int n=0;n<3;n++){int index=n;String key=n==0?"player_fallback":n==1?"npc_fallback":"system_fallback",value=n==0?working.defaultPlayer:n==1?working.defaultNpc:working.defaultSystem;
                button(x+n*104,78,102,label(key).copy().append(": ").append(value==null?Component.translatable("cinematic_chat.voice.none"):Component.literal(value)),()->{var options=new ArrayList<String>();options.add("None");for(var p:working.profiles)options.add(p.id);minecraft.setScreen(new LocalChoiceScreen(this,label(key),options,id->{String v=id.equals("None")?null:id;if(index==0)working.defaultPlayer=v;else if(index==1)working.defaultNpc=v;else working.defaultSystem=v;}));});}
            int rows=Math.max(1,(height-166)/24),start=page*rows;page=Math.min(page,Math.max(0,(working.profiles.size()-1)/rows));start=page*rows;
            for(int i=0;i<rows&&start+i<working.profiles.size();i++){var p=working.profiles.get(start+i);button(x,104+i*24,310,Component.literal(p.id).append("  ").append(Component.translatable("cinematic_chat.voice.mode."+p.playMode.name().toLowerCase(Locale.ROOT))),()->minecraft.setScreen(new VoiceProfileScreen(this,working,p)));}
            button(x,height-54,40,Component.literal("<"),()->{page=Math.max(0,page-1);init();});button(x+270,height-54,40,Component.literal(">"),()->{if((page+1)*rows<working.profiles.size())page++;init();});
        }else{
            button(x,80,310,label("strip_formatting").copy().append(": "+working.stripFormatting),()->{working.stripFormatting=!working.stripFormatting;init();});
            button(x,104,310,label("case_insensitive").copy().append(": "+working.caseInsensitive),()->{working.caseInsensitive=!working.caseInsensitive;init();});
            button(x,128,310,label("trim_whitespace").copy().append(": "+working.trimWhitespace),()->{working.trimWhitespace=!working.trimWhitespace;init();});
            int rows=Math.max(1,(height-216)/24),start=page*rows;
            for(int i=0;i<rows&&start+i<working.matchOrder.size();i++){int index=start+i;String value=working.matchOrder.get(index);button(x,156+i*24,310,label("order").copy().append(": "+(index+1)+" "+value+" ↑"),()->{if(index>0)Collections.swap(working.matchOrder,index,index-1);else Collections.rotate(working.matchOrder,1);init();});}
            button(x,height-54,40,Component.literal("<"),()->{page=Math.max(0,page-1);init();});button(x+270,height-54,40,Component.literal(">"),()->{if((page+1)*rows<working.matchOrder.size())page++;init();});
        }
        button(x+46,height-54,218,label(matching?"title":"matching"),()->{matching=!matching;page=0;init();});
        button(width/2-104,height-28,100,Component.translatable("cinematic_chat.save"),()->{working.validate();saved.accept(working);onClose();});button(width/2+4,height-28,100,Component.translatable("gui.cancel"),this::onClose);
    }
    private void button(int x,int y,int w,Component text,Runnable action){addRenderableWidget(Button.builder(text,b->action.run()).bounds(x,y,w,20).build());}
    @Override public void renderContent(GuiCanvas g,int x,int y,float partialTick){g.fill(0,0,width,height,0xe0101822);g.drawCenteredString(font,title,width/2,10,0xffffffff);super.renderContent(g,x,y,partialTick);}
    @Override public void renderBackdrop(GuiCanvas g,int x,int y,float partialTick){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}