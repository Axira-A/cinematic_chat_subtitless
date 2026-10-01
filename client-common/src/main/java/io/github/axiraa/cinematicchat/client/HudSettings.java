package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.engine.CcmlLimits;
import io.github.axiraa.cinematicchat.engine.SubtitleLayoutEngine;
public final class HudSettings {
    public enum Anchor {
        TOP_LEFT(0,0),TOP_CENTER(.5,0),TOP_RIGHT(1,0),CENTER_LEFT(0,.5),CENTER(.5,.5),CENTER_RIGHT(1,.5),BOTTOM_LEFT(0,1),BOTTOM_CENTER(.5,1),BOTTOM_RIGHT(1,1);
        public final double x,y; Anchor(double x,double y) { this.x=x;this.y=y; }
    }
    public Anchor anchor = Anchor.BOTTOM_CENTER;
    public double offsetX = 0, offsetY = -.22, widthRatio = .45, absoluteMaxWidth = 900, scale = 1;
    public SubtitleLayoutEngine.Alignment alignment = SubtitleLayoutEngine.Alignment.CENTER;
    public boolean snap = true, showSafeArea = true;
    public void validate() {
        if(anchor==null)anchor=Anchor.BOTTOM_CENTER;if(alignment==null)alignment=SubtitleLayoutEngine.Alignment.CENTER;
        offsetX=CcmlLimits.clamp(offsetX,-1,1);offsetY=CcmlLimits.clamp(offsetY,-1,1);
        widthRatio=CcmlLimits.clamp(widthRatio,.05,.95);absoluteMaxWidth=CcmlLimits.clamp(absoluteMaxWidth,120,2000);scale=CcmlLimits.clamp(scale,.5,3);
    }
    public double x(double w) { return (anchor.x+offsetX)*w; }
    public double y(double h) { return (anchor.y+offsetY)*h; }
    public double width(double w) { return Math.min(w*.95,Math.max(Math.min(120,w*.95),Math.min(w*widthRatio,absoluteMaxWidth))); }
    public void position(double x,double y,double w,double h) { offsetX=x/w-anchor.x;offsetY=y/h-anchor.y; }
    public void changeAnchor(Anchor next,double w,double h) { double px=x(w),py=y(h);anchor=next;position(px,py,w,h); }
    public void resetPosition(){var defaults=new HudSettings();anchor=defaults.anchor;offsetX=defaults.offsetX;offsetY=defaults.offsetY;}
    public void resetSize(){var defaults=new HudSettings();widthRatio=defaults.widthRatio;absoluteMaxWidth=defaults.absoluteMaxWidth;scale=defaults.scale;}
}
