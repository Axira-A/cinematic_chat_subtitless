package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.engine.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class SubtitleRenderer {
    private final SubtitleLayoutEngine layouts=new SubtitleLayoutEngine();
    private final Map<String,Double> widths=new HashMap<>();
    private record ComponentKey(String text,TextStyle style,String font){}
    private final Map<ComponentKey,Component> components=new HashMap<>();
    private record NameKey(StyledText text,String font){}
    private final Map<NameKey,Component> names=new HashMap<>();
    private record LayoutKey(long id,int revision,double width,double spacing,String font){}
    private final Map<LayoutKey,SubtitleLayoutEngine.Layout> cache=new HashMap<>();
    public record Bounds(double left,double top,double width,double height) {
        public double right(){return left+width;} public double bottom(){return top+height;}
        public boolean contains(double x,double y){return x>=left&&x<=right()&&y>=top&&y<=bottom();}
    }
    private FontBridge fontBridge(CinematicConfig config){var font=Minecraft.getInstance().font;
        if(widths.size()>8192)widths.clear();
        return new FontBridge(){public double width(String s,TextStyle st){String key=s+"|"+st+"|"+config.fontId;return widths.computeIfAbsent(key,k->font.width(component(s,st,config.fontId))*st.scale());}public double lineHeight(TextStyle st){return font.lineHeight*st.scale();}};
    }
    private Component component(String text,TextStyle style,String font){if(components.size()>8192)components.clear();return components.computeIfAbsent(new ComponentKey(text,style,font),key->Component.literal(text).setStyle(ComponentBridge.style(style,font)));}
    private SubtitleLayoutEngine.Layout layout(SubtitleQueue.Entry entry,CinematicConfig config,int width){var snapshot=entry.runtime().snapshot();double maxWidth=config.hud.width(width)/config.hud.scale;
        var key=new LayoutKey(snapshot.id(),snapshot.revision(),maxWidth,config.lineSpacing,config.fontId+config.hud.alignment);
        return cache.computeIfAbsent(key,k->layouts.layout(snapshot,fontBridge(config),maxWidth,config.hud.alignment,config.lineSpacing));
    }
    private double senderHeight(SubtitleQueue.Entry entry,CinematicConfig config){return config.showSender&&entry.pending().envelope().speaker().known()?Minecraft.getInstance().font.lineHeight*config.senderScale+4:0;}
    private double entryHeight(SubtitleQueue.Entry entry,SubtitleLayoutEngine.Layout layout,CinematicConfig config,int screenHeight,int count){
        double scale=config.hud.scale,minimum=Minecraft.getInstance().font.lineHeight*scale,available=screenHeight*.75,y=config.hud.y(screenHeight),anchor=config.hud.anchor.y;
        if(anchor>0)available=Math.min(available,Math.max(minimum,y-screenHeight*.05)/anchor);
        if(anchor<1)available=Math.min(available,Math.max(minimum,screenHeight*.95-y)/(1-anchor));
        double limit=Math.max(minimum,(available-8*scale*Math.max(0,count-1))/Math.max(1,count));
        return Math.min((layout.height()+senderHeight(entry,config))*scale,limit);
    }
    public Bounds bounds(SubtitleController controller,int width,int height){var c=controller.config();double total=0;var entries=controller.queue().visible();
        for(var entry:entries)total+=entryHeight(entry,layout(entry,c,width),c,height,entries.size())+8*c.hud.scale;
        total=entries.isEmpty()?45*c.hud.scale:total-8*c.hud.scale;
        double w=c.hud.width(width);return new Bounds(c.hud.x(width)-w*c.hud.anchor.x,c.hud.y(height)-total*c.hud.anchor.y,w,total);
    }
    public void invalidate(){widths.clear();cache.clear();components.clear();names.clear();}
    public void render(GuiGraphics graphics,SubtitleController controller){
        var config=controller.config();var mc=Minecraft.getInstance();var font=mc.font;double scale=config.hud.scale;
        double maxWidth=config.hud.width(graphics.guiWidth())/scale;
        var bounds=bounds(controller,graphics.guiWidth(),graphics.guiHeight());var entries=controller.queue().visible();
        Set<Long> active=new HashSet<>();for(var entry:entries)active.add(entry.pending().envelope().id());cache.keySet().removeIf(k->!active.contains(k.id));
        long now=System.nanoTime();
        var stackItems=new ArrayList<SubtitleStackLayout.Item>();
        for(var entry:entries){var state=controller.snapshot(entry,now);stackItems.add(new SubtitleStackLayout.Item(state.id(),entryHeight(entry,layout(entry,config,graphics.guiWidth()),config,graphics.guiHeight(),entries.size()),state.entryProgress(),state.exitProgress()));}
        var placements=SubtitleStackLayout.place(stackItems,config.hud.y(graphics.guiHeight()),config.hud.anchor.y,8*scale,config.effectiveEntryEasing(),config.effectiveExitEasing(),config.reducedMotion);
        if(names.size()>128)names.clear();
        for(int n=entries.size()-1;n>=0;n--){var entry=entries.get(n);var snapshot=controller.snapshot(entry,now);var layout=layout(entry,config,graphics.guiWidth());
            double enter=Animation.ease(snapshot.entryProgress(),config.effectiveEntryEasing()),exit=Animation.ease(snapshot.exitProgress(),config.effectiveExitEasing());
            double alpha=enter*(1-exit)*Math.max(.35,1-(entries.size()-1-n)*.18);
            double height=entryHeight(entry,layout,config,graphics.guiHeight(),entries.size());
            double top=placements.get(entries.size()-1-n).top();double left=bounds.left();
            double movement=0,animScale=1;if(!config.reducedMotion){switch(config.animation.entry()){case SLIDE_UP->movement=(1-enter)*12;case SLIDE_DOWN->movement=-(1-enter)*12;case SCALE_FADE->animScale=.85+.15*enter;default->{}}switch(config.animation.exit()){case SLIDE_UP->movement-=exit*12;case SLIDE_DOWN->movement+=exit*12;default->{}}}
            GuiPose.push(graphics);GuiPose.translate(graphics,(float)left,(float)(top+movement));GuiPose.scale(graphics,(float)(scale*animScale));
            if(config.backgroundEnabled)graphics.fill(-4,-3,(int)maxWidth+4,(int)(height/scale)+3,(int)(config.backgroundOpacity*alpha*255)<<24);
            double offset=0;var speaker=entry.pending().envelope().speaker();
            if(config.showSender&&speaker.known()){StyledText name=speaker.displayName()!=null?speaker.displayName():StyledText.plain(speaker.profileName()==null?"":speaker.profileName());
                GuiPose.push(graphics);GuiPose.scale(graphics,(float)config.senderScale);var component=names.computeIfAbsent(new NameKey(name,config.fontId),key->StyledComponentBridge.component(name,config.fontId));
                int x=(int)((maxWidth/config.senderScale-font.width(component))/2);graphics.drawString(font,component,x,0,color(0xffffff,alpha*config.senderOpacity),config.shadow);GuiPose.pop(graphics);offset=font.lineHeight*config.senderScale+4;}
            var viewport=SubtitleViewport.at(layout,snapshot.time(),font.lineHeight,Math.max(font.lineHeight,height/scale-offset));
            for(int i=viewport.first();i<viewport.end();i++){var positioned=layout.glyphs().get(i);var g=positioned.glyph();var st=g.glyph().style();double reveal=snapshot.time()<g.glyph().shownAt()?0:g.glyph().fadeDuration()==0?1:CcmlLimits.clamp((snapshot.time()-g.glyph().shownAt())/g.glyph().fadeDuration(),0,1);double a=alpha*reveal*st.alpha();if(a<.02)continue;
                double x=positioned.x(),localY=positioned.y()-viewport.scroll(),y=localY+offset;double t=snapshot.time();
                if(localY<-.01||localY+font.lineHeight*st.scale()>viewport.height()+.01)continue;
                if(top+movement+y*scale>graphics.guiHeight()||top+movement+(y+font.lineHeight*st.scale())*scale<0)continue;
                if(!config.reducedMotion&&!config.disableShake&&st.shake()>0){x+=Math.sin(t*st.shakeFrequency()*6.283+positioned.index()*7.13)*st.shake();y+=Math.cos(t*st.shakeFrequency()*5.71+positioned.index()*5.17)*st.shake();}
                if(!config.reducedMotion&&!config.disableWave)y+=Math.sin(t*st.waveFrequency()*6.283+positioned.index()*.65)*st.wave();
                GuiPose.push(graphics);GuiPose.translate(graphics,(float)x,(float)y);GuiPose.scale(graphics,(float)st.scale());
                graphics.drawString(font,component(g.glyph().text(),st,config.fontId),0,0,color(st.color(),a),config.shadow);GuiPose.pop(graphics);}
            GuiPose.pop(graphics);if(top<graphics.guiHeight()*.05)break;
        }
        if(config.debug)graphics.drawString(font,"Cinematic Chat | visible="+entries.size()+" queued="+controller.queue().queuedSize(),4,4,0xffffffff,true);
    }
    private int color(int rgb,double alpha){return ((int)CcmlLimits.clamp(alpha*255,4,255)<<24)|rgb;}
}
