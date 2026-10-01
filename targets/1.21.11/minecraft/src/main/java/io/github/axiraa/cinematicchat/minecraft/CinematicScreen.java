package io.github.axiraa.cinematicchat.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/** Converts native input events at the version boundary; shared screens keep logical coordinates. */
public abstract class CinematicScreen extends Screen {
    private MouseButtonEvent currentMouse;
    private boolean currentDoubleClick;
    protected CinematicScreen(Component title){super(title);}
    protected void cinematicBlur(GuiGraphics graphics,float partialTick){renderBlurredBackground(graphics);}
    protected static boolean hasShiftDown(){return Minecraft.getInstance().hasShiftDown();}
    protected static boolean hasAltDown(){return Minecraft.getInstance().hasAltDown();}
    private MouseButtonEvent mouse(double x,double y,int button){
        return currentMouse!=null?currentMouse:new MouseButtonEvent(x,y,new MouseButtonInfo(button,0));
    }
    public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(mouse(x,y,button),currentDoubleClick);}
    public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(mouse(x,y,button));}
    public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(mouse(x,y,button),dx,dy);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        var previous=currentMouse;boolean previousDouble=currentDoubleClick;currentMouse=event;currentDoubleClick=doubleClick;
        try{return mouseClicked(event.x(),event.y(),event.button());}finally{currentMouse=previous;currentDoubleClick=previousDouble;}
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        var previous=currentMouse;currentMouse=event;
        try{return mouseReleased(event.x(),event.y(),event.button());}finally{currentMouse=previous;}
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        var previous=currentMouse;currentMouse=event;
        try{return mouseDragged(event.x(),event.y(),event.button(),dx,dy);}finally{currentMouse=previous;}
    }
}
