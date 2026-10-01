package io.github.axiraa.cinematicchat.engine;
import io.github.axiraa.cinematicchat.api.FontBridge;
import java.util.*;
public final class SubtitleLayoutEngine {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public record PositionedGlyph(SubtitleRuntime.VisibleGlyph glyph, double x, double y, int index) {}
    public record Layout(List<PositionedGlyph> glyphs, double width, double height) { public Layout { glyphs = List.copyOf(glyphs); } }
    public Layout layout(SubtitleRuntime.Snapshot snapshot, FontBridge font, double maxWidth, Alignment alignment, double spacing) {
        List<List<SubtitleRuntime.VisibleGlyph>> lines = new ArrayList<>(); List<SubtitleRuntime.VisibleGlyph> line = new ArrayList<>();
        double width = 0;
        var allGlyphs = snapshot.layoutGlyphs().stream().map(g -> new SubtitleRuntime.VisibleGlyph(g,1)).toList();
        for (int i=0; i<allGlyphs.size(); i++) {
            var glyph = allGlyphs.get(i);
            String text = glyph.glyph().text();
            if (!text.isBlank() && !UnicodeUnits.isCjk(text.codePointAt(0)) && (i==0 || allGlyphs.get(i-1).glyph().text().isBlank() || UnicodeUnits.isCjk(allGlyphs.get(i-1).glyph().text().codePointAt(0)))) {
                double wordWidth=0;
                for(int j=i;j<allGlyphs.size();j++){var candidate=allGlyphs.get(j);String t=candidate.glyph().text();if(t.isBlank()||UnicodeUnits.isCjk(t.codePointAt(0)))break;wordWidth+=font.width(t,candidate.glyph().style());}
                if (!line.isEmpty() && wordWidth<=maxWidth && width+wordWidth>maxWidth) {lines.add(line);line=new ArrayList<>();width=0;}
            }
            double advance = font.width(glyph.glyph().text(), glyph.glyph().style());
            if (glyph.glyph().text().equals("\n")) { lines.add(line); line = new ArrayList<>(); width = 0; continue; }
            if (!line.isEmpty() && width + advance > maxWidth) { lines.add(line); line = new ArrayList<>(); width = 0; }
            line.add(glyph); width += advance;
        }
        lines.add(line); List<PositionedGlyph> output = new ArrayList<>(); double y = 0, widest = 0; int index = 0;
        for (var row : lines) {
            double rowWidth = row.stream().mapToDouble(g -> font.width(g.glyph().text(), g.glyph().style())).sum();
            double height = row.stream().mapToDouble(g -> font.lineHeight(g.glyph().style())).max().orElse(9);
            double x = switch (alignment) { case LEFT -> 0; case CENTER -> (maxWidth - rowWidth) / 2; case RIGHT -> maxWidth - rowWidth; };
            for (var glyph : row) { output.add(new PositionedGlyph(glyph, x, y, index++)); x += font.width(glyph.glyph().text(), glyph.glyph().style()); }
            widest = Math.max(widest, rowWidth); y += height + spacing;
        }
        return new Layout(output, widest, Math.max(0, y - spacing));
    }
}
