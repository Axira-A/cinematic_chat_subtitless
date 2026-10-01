package io.github.axiraa.cinematicchat.api;
import java.util.List;
import java.util.Objects;

public record StyledText(String text, List<Run> runs) {
    public record Run(int start, int end, TextStyle style) {}
    public StyledText { Objects.requireNonNull(text); runs = List.copyOf(runs); }
    public static StyledText plain(String text) { return new StyledText(text, List.of(new Run(0, text.length(), TextStyle.DEFAULT))); }
    public TextStyle styleAt(int index) {
        for (Run run : runs) if (index >= run.start && index < run.end) return run.style;
        return TextStyle.DEFAULT;
    }
}
