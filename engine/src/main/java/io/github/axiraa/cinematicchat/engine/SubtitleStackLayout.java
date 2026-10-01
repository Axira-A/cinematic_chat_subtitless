package io.github.axiraa.cinematicchat.engine;

import java.util.*;

/** A time-based allocation grows on entry and shrinks on exit, moving older entries smoothly. */
public final class SubtitleStackLayout {
    public record Item(long id,double height,double entryProgress,double exitProgress){}
    public record Placement(long id,double top,double height){}
    private SubtitleStackLayout(){}
    public static List<Placement> place(List<Item> items,double anchorY,double anchorFraction,double gap,Animation.Easing easing,boolean reducedMotion){
        return place(items,anchorY,anchorFraction,gap,easing,easing,reducedMotion);
    }
    public static List<Placement> place(List<Item> items,double anchorY,double anchorFraction,double gap,Animation.Easing entryEasing,Animation.Easing exitEasing,boolean reducedMotion){
        double[] allocated=new double[items.size()];double total=0;
        for(int i=0;i<items.size();i++){
            Item item=items.get(i);double weight=reducedMotion?1:Animation.ease(item.entryProgress,entryEasing)*(1-Animation.ease(item.exitProgress,exitEasing));
            allocated[i]=(Math.max(0,item.height)+(i==0?0:Math.max(0,gap)))*weight;total+=allocated[i];
        }
        double bottom=anchorY+total*(1-anchorFraction);var result=new ArrayList<Placement>(items.size());
        for(int i=items.size()-1;i>=0;i--){Item item=items.get(i);result.add(new Placement(item.id,bottom-item.height,item.height));bottom-=allocated[i];}
        return List.copyOf(result);
    }
}
