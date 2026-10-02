package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.components.Tooltip;

/** Paged local choices, used instead of accepting arbitrary external file paths. */
public final class LocalChoiceScreen extends CinematicScreen implements PreviewScreen {
    private final Screen parent; private final List<String> choices; private final Consumer<String> selected;
    private final Function<String,Component> display;
    private int page;
    public LocalChoiceScreen(Screen parent, Component title, List<String> choices, Consumer<String> selected) {
        this(parent,title,choices,Component::literal,selected);
    }
    public LocalChoiceScreen(Screen parent,Component title,List<String> choices,Function<String,Component> display,Consumer<String> selected){
        super(title); this.parent=parent; this.choices=List.copyOf(choices); this.selected=selected;this.display=display;
    }
    @Override protected void init() {
        clearWidgets(); int rows=Math.max(1,(height-80)/24), start=page*rows;
        for(int i=0;i<rows && start+i<choices.size();i++) {String value=choices.get(start+i);
            var text=display.apply(value);var button=Button.builder(text,b->{selected.accept(value);onClose();}).bounds(width/2-150,32+i*24,300,20).build();button.setTooltip(Tooltip.create(text));addRenderableWidget(button);}
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);init();}).bounds(width/2-150,height-30,40,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-100,height-30,200,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->{if((page+1)*rows<choices.size())page++;init();}).bounds(width/2+110,height-30,40,20).build());
    }
    @Override public void tick(){parent.tick();}
    @Override public void renderContent(GuiCanvas g,int x,int y,float partialTick){g.fill(0,0,width,height,0xe0101822);g.drawCenteredString(font,title,width/2,12,0xffffffff);if(choices.isEmpty())g.drawCenteredString(font,Component.translatable("cinematic_chat.empty"),width/2,height/2,0xffaaaaaa);super.renderContent(g,x,y,partialTick);}
    @Override public void renderBackdrop(GuiCanvas g,int x,int y,float partialTick){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
