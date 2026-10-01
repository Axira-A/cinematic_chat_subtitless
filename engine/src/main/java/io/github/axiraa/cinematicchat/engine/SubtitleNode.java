package io.github.axiraa.cinematicchat.engine;
import java.util.List;
import java.util.Map;
public sealed interface SubtitleNode {
    record SourceRange(int start,int end){public SourceRange{if(start<0||end<start)throw new IllegalArgumentException("Invalid source range");}}
    SourceRange range();
    record Text(String text, List<Integer> sourceOffsets,SourceRange range) implements SubtitleNode {
        public Text(String text,List<Integer> offsets){this(text,offsets,new SourceRange(offsets.isEmpty()?0:offsets.getFirst(),offsets.isEmpty()?0:offsets.getLast()+1));}
        public Text { sourceOffsets = List.copyOf(sourceOffsets); }
    }
    record Span(String name, double value, String literalValue, Map<String, Double> attributes,
                List<SubtitleNode> children,SourceRange range) implements SubtitleNode {
        public Span(String name,double value,String literalValue,Map<String,Double> attributes,List<SubtitleNode> children){this(name,value,literalValue,attributes,children,new SourceRange(0,0));}
        public Span { attributes = Map.copyOf(attributes); children = List.copyOf(children); }
    }
    record Command(String name, double value, String literalValue,SourceRange range) implements SubtitleNode {
        public Command(String name,double value,String literalValue){this(name,value,literalValue,new SourceRange(0,0));}
    }
}
