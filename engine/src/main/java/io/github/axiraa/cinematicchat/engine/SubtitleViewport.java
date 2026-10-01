package io.github.axiraa.cinematicchat.engine;

/** A bounded view into a stable full layout, following the latest revealed row. */
public final class SubtitleViewport {
    public record Window(double height,double scroll,int first,int end) {}
    private SubtitleViewport(){}
    public static Window at(SubtitleLayoutEngine.Layout layout,double time,double lineHeight,double maximumHeight){
        var glyphs=layout.glyphs();double height=Math.min(layout.height(),Math.max(lineHeight,maximumHeight));
        int low=0,high=glyphs.size();
        while(low<high){int mid=(low+high)>>>1;if(glyphs.get(mid).glyph().glyph().shownAt()<=time)low=mid+1;else high=mid;}
        int revealed=low;if(revealed==0)return new Window(height,0,0,0);
        double row=glyphs.get(revealed-1).y(),bottom=row;
        for(int i=revealed-1;i>=0&&glyphs.get(i).y()==row;i--)bottom=Math.max(bottom,row+lineHeight*glyphs.get(i).glyph().glyph().style().scale());
        double scroll=Math.max(0,Math.min(layout.height()-height,bottom-height));
        low=0;high=revealed;
        while(low<high){int mid=(low+high)>>>1;if(glyphs.get(mid).y()+1e-7<scroll)low=mid+1;else high=mid;}
        return new Window(height,scroll,low,revealed);
    }
}
