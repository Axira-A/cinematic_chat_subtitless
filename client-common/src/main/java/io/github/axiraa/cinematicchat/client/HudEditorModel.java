package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.engine.CcmlLimits;
public final class HudEditorModel {
    public enum Operation { NONE, MOVE, WIDTH_LEFT, WIDTH_RIGHT, SCALE }
    public enum Axis { NONE, HORIZONTAL, VERTICAL }
    private final HudSettings hud; private Operation operation=Operation.NONE; private Axis axis=Axis.NONE;
    private double mouseX,mouseY,positionX,positionY,startWidth,startScale; private boolean constrained;
    private double extentWidth,extentHeight;
    public HudEditorModel(HudSettings hud) { this.hud=hud; }
    public void begin(Operation op,double x,double y,double w,double h) {
        operation=op; mouseX=x;mouseY=y;positionX=hud.x(w);positionY=hud.y(h);startWidth=hud.width(w);startScale=hud.scale;axis=Axis.NONE;constrained=false;
    }
    public void drag(double x,double y,double w,double h,boolean shift) {
        if(operation==Operation.NONE)return;
        if(operation==Operation.MOVE) {
            if(shift!=constrained) { positionX=hud.x(w);positionY=hud.y(h);mouseX=x;mouseY=y;axis=Axis.NONE;constrained=shift; }
            double dx=x-mouseX,dy=y-mouseY;
            if(shift && axis==Axis.NONE && Math.max(Math.abs(dx),Math.abs(dy))>3)axis=Math.abs(dx)>Math.abs(dy)?Axis.HORIZONTAL:Axis.VERTICAL;
            if(shift) { if(axis==Axis.HORIZONTAL)dy=0;else if(axis==Axis.VERTICAL)dx=0;else {dx=0;dy=0;} }
            hud.position(CcmlLimits.clamp(positionX+dx,0,w),CcmlLimits.clamp(positionY+dy,0,h),w,h);
        } else if(operation==Operation.SCALE) hud.scale=CcmlLimits.clamp(startScale+(x-mouseX+y-mouseY)/150,.5,3);
        else { double width=startWidth+(operation==Operation.WIDTH_RIGHT?x-mouseX:mouseX-x)*2;
            width=CcmlLimits.clamp(width,Math.min(120,w*.95),w*.95);hud.widthRatio=width/w;hud.absoluteMaxWidth=Math.max(120,width); }
    }
    public void end(double w,double h,boolean alt) {
        if(operation==Operation.MOVE && hud.snap && !alt) {
            double x=hud.x(w),y=hud.y(h); HudSettings.Anchor candidate=null;double closest=12;
            for(var anchor:HudSettings.Anchor.values()) { double d=Math.hypot(x-anchor.x*w,y-anchor.y*h); if(d<closest){closest=d;candidate=anchor;} }
            if(candidate!=null){hud.anchor=candidate;hud.offsetX=0;hud.offsetY=0;}
            else { if(Math.abs(x-w*.5)<12)x=w*.5;if(Math.abs(y-h*.5)<12)y=h*.5;
                double centerX=x+extentWidth*(.5-hud.anchor.x),centerY=y+extentHeight*(.5-hud.anchor.y);
                if(extentWidth>0&&Math.abs(centerX-w*.5)<12)x+=w*.5-centerX;
                if(extentHeight>0&&Math.abs(centerY-h*.5)<12)y+=h*.5-centerY;
                double left=x-extentWidth*hud.anchor.x,right=left+extentWidth,top=y-extentHeight*hud.anchor.y,bottom=top+extentHeight;
                if(extentWidth>0&&Math.abs(left-w*.05)<12)x+=w*.05-left;
                else if(extentWidth>0&&Math.abs(right-w*.95)<12)x+=w*.95-right;
                if(extentHeight>0&&Math.abs(top-h*.05)<12)y+=h*.05-top;
                else if(extentHeight>0&&Math.abs(bottom-h*.92)<12)y+=h*.92-bottom;
                if(Math.abs(x-w*.05)<12)x=w*.05;if(Math.abs(x-w*.95)<12)x=w*.95;
                if(Math.abs(y-h*.05)<12)y=h*.05;if(Math.abs(y-h*.92)<12)y=h*.92;hud.position(x,y,w,h); }
        }
        operation=Operation.NONE;axis=Axis.NONE;
    }
    public Axis axis(){return axis;}
    public void extent(double width,double height){extentWidth=Math.max(0,width);extentHeight=Math.max(0,height);}
    public Operation operation(){return operation;}
}
