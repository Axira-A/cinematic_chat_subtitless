package io.github.axiraa.cinematicchat.minecraft;

import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.engine.CcmlLimits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Every field remains reachable at small GUI sizes through the same scrollable form. */
public final class VoiceProfileScreen extends CinematicScreen implements PreviewScreen {
    private final VoiceProfilesScreen parent;
    private final VoiceConfig owner, previewVoices = new VoiceConfig();
    private final VoiceProfile original, working;
    private final SubtitleController preview;
    private int page,scroll;
    private Component error=Component.empty();
    private final List<Row> rows=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();
    private record Row(int height,BiConsumer<Integer,Integer> draw){}
    private record Label(int x,int y,Component text){}
    public VoiceProfileScreen(VoiceProfilesScreen parent,VoiceConfig owner,VoiceProfile profile){
        super(label("profile"));this.parent=parent;this.owner=owner;original=profile;
        var client=CinematicChatClient.get();working=client.configs().copy(profile,VoiceProfile.class);
        previewVoices.enabled=true;previewVoices.masterVolume=owner.masterVolume;
        preview=client.preview(client.configs().copy(client.config(),CinematicConfig.class),previewVoices);
    }
    private static Component label(String key){return Component.translatable("cinematic_chat.voice."+key);}
    private static Component state(boolean on){return Component.translatable(on?"options.on":"options.off");}
    public VoiceProfile workingCopy(){return working;}
    @Override protected void init(){
        clearWidgets();rows.clear();labels.clear();int x=Math.max(4,width/2-155);
        var hud=preview.config().hud;hud.anchor=HudSettings.Anchor.BOTTOM_CENTER;hud.widthRatio=.95;hud.absoluteMaxWidth=310;hud.scale=1;hud.position(width/2.0,height-110,width,height);preview.config().senderScale=.8;
        for(int i=0;i<3;i++){int tab=i;String key=i==0?"matching":i==1?"sound":"audio";button(x+i*104,30,102,label(key),()->{page=tab;scroll=0;init();});}
        if(page==0){
            text("id",working.id,v->working.id=v,64);
            text("names",String.join(", ",working.names),v->working.names=split(v),2048);
            text("uuids",String.join(", ",working.uuids),v->working.uuids=split(v),2048);
            text("aliases",String.join(", ",working.aliases),v->working.aliases=split(v),2048);
            toggle("profile_enabled",()->working.enabled,v->working.enabled=v);
        }else if(page==1){
            rows.add(new Row(24,(left,y)->button(left,y,310,label("mode").copy().append(": ").append(Component.translatable("cinematic_chat.voice.mode."+working.playMode.name().toLowerCase(Locale.ROOT))),()->{var modes=VoiceProfile.PlayMode.values();working.playMode=modes[(working.playMode.ordinal()+1)%modes.length];init();})));
            slider("volume",0,1,()->working.volume,v->working.volume=v);
            slider("volume_randomness",0,.5,()->working.volumeRandomness,v->working.volumeRandomness=v);
            slider("pitch",.5,2,()->working.pitch,v->working.pitch=v);
            slider("pitch_randomness",0,.5,()->working.pitchRandomness,v->working.pitchRandomness=v);
            slider("interval",30,2000,()->working.intervalMs,v->working.intervalMs=(int)Math.round(v));
            slider("interval_randomness",0,1000,()->working.intervalRandomnessMs,v->working.intervalRandomnessMs=(int)Math.round(v));
            slider("cjk_group",1,4,()->working.cjkCharactersPerVoice,v->working.cjkCharactersPerVoice=(int)Math.round(v));
            toggle("avoid_repeat",()->working.avoidImmediateRepeat,v->working.avoidImmediateRepeat=v);
            toggle("punctuation",()->working.voiceOnPunctuation,v->working.voiceOnPunctuation=v);
        }else{
            rows.add(new Row(24,(left,y)->button(left,y,310,label("add_audio"),this::chooseAudio)));
            rows.add(new Row(24,(left,y)->button(left,y,152,label("reload"),()->{CinematicChatClient.get().reloadAudio();preview.audioReloaded();})));
            rows.add(new Row(24,(left,y)->button(left,y,310,label("install_test"),()->CinematicChatClient.get().installTestAudio())));
            for(String id:List.copyOf(working.audio))rows.add(new Row(24,(left,y)->button(left,y,310,label("remove_audio").copy().append(": "+id),()->{working.audio.remove(id);init();})));
        }
        scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-1));int y=62,index=scroll;
        while(index<rows.size()&&y+rows.get(index).height<=height-138){var row=rows.get(index++);row.draw.accept(x,y);y+=row.height;}
        if(scroll>0)button(x,height-104,40,Component.literal("▲"),()->{scroll--;init();});
        if(index<rows.size())button(x+270,height-104,40,Component.literal("▼"),()->{scroll++;init();});
        button(x,height-78,152,label("preview"),()->test(false));button(x+158,height-78,152,label("talking"),()->test(true));
        button(x,height-52,100,Component.translatable("cinematic_chat.save"),()->{
            if(working.id==null||!working.id.matches("[a-z0-9_.-]{1,64}")){error=label("invalid_id");return;}
            if(owner.profiles.stream().anyMatch(p->p!=original&&p.id.equals(working.id))){error=label("duplicate_id");return;}
            working.validate();int indexOf=owner.profiles.indexOf(original);if(indexOf<0)owner.profiles.add(working);else owner.profiles.set(indexOf,working);onClose();
        });
        button(x+105,height-52,100,Component.translatable("gui.cancel"),this::onClose);
        button(x+210,height-52,100,Component.translatable("cinematic_chat.delete"),()->{owner.profiles.remove(original);onClose();});
    }
    private static List<String> split(String s){return new ArrayList<>(Arrays.stream(s.split(",")).map(String::strip).filter(v->!v.isEmpty()).limit(128).toList());}
    private void chooseAudio(){CinematicChatClient.get().audio().list().thenAccept(files->minecraft.execute(()->{
        if(minecraft.screen==this)minecraft.setScreen(new LocalChoiceScreen(this,label("audio"),files,id->{if(!working.audio.contains(id))working.audio.add(id);}));
    }));}
    private void text(String key,String value,Consumer<String> changed,int limit){rows.add(new Row(40,(x,y)->{var field=new EditBox(font,x,y+13,310,20,label(key));field.setMaxLength(limit);field.setValue(value==null?"":value);field.setResponder(changed);addRenderableWidget(field);labels.add(new Label(x,y,label(key)));}));}
    private void slider(String key,double min,double max,DoubleSupplier value,DoubleConsumer changed){rows.add(new Row(24,(x,y)->addRenderableWidget(new AbstractSliderButton(x,y,310,20,Component.empty(),CcmlLimits.clamp((value.getAsDouble()-min)/(max-min),0,1)){
        {updateMessage();}protected void updateMessage(){setMessage(label(key).copy().append(": "+String.format(Locale.ROOT,"%.2f",min+this.value*(max-min))));}protected void applyValue(){changed.accept(min+this.value*(max-min));}
    })));}
    private void toggle(String key,BooleanSupplier value,Consumer<Boolean> changed){rows.add(new Row(24,(x,y)->button(x,y,310,label(key).copy().append(": ").append(state(value.getAsBoolean())),()->{changed.accept(!value.getAsBoolean());init();})));}
    private void button(int x,int y,int width,Component text,Runnable action){addRenderableWidget(Button.builder(text,b->action.run()).bounds(x,y,width,20).build());}
    public void test(boolean talking){
        var p=CinematicChatClient.get().configs().copy(working,VoiceProfile.class);p.validate();p.enabled=true;if(!talking)p.playMode=VoiceProfile.PlayMode.ON_SUBTITLE_START;
        previewVoices.profiles=new ArrayList<>(List.of(p));previewVoices.defaultPlayer=p.id;previewVoices.masterVolume=owner.masterVolume;preview.configure(preview.config(),previewVoices);preview.clear();
        var speaker=new SpeakerIdentity(ChatSource.PLAYER_CHAT,null,p.id,StyledText.plain(p.id));preview.capture(new ChatEnvelope(CinematicChatClient.get().nextPreviewId(),ChatSource.PLAYER_CHAT,speaker,StyledText.plain(talking?"你好，旅行者。Welcome to Cinematic Chat.":"Voice preview"),null,null));
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll-=Integer.signum((int)vertical);init();return true;}
    @Override public void tick(){preview.tick(System.nanoTime(),new ClientState(true,false,Minecraft.getInstance().options.hideGui,false));}
    @Override public void render(GuiGraphics g,int x,int y,float partialTick){g.fill(0,0,width,height,0xe0101822);CinematicChatClient.get().drawPreview(g,preview);g.drawCenteredString(font,title,width/2,10,0xffffffff);for(var label:labels)g.drawString(font,label.text,label.x,label.y,0xffaaaaaa,false);g.drawCenteredString(font,error,width/2,height-20,0xffff8888);super.render(g,x,y,partialTick);}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partialTick){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){preview.clear();minecraft.setScreen(parent);}
}
