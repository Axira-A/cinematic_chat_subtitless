package io.github.axiraa.cinematicchat.engine;
import java.util.*;
import java.util.regex.Pattern;
public final class UnicodeUnits {
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    public record Unit(String text, int start, int end, boolean wordEnd) {}
    public static List<Unit> split(String text) {
        List<Unit> units = new ArrayList<>(); var matcher = GRAPHEME.matcher(text);
        while (matcher.find()) units.add(new Unit(matcher.group(), matcher.start(), matcher.end(), false));
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i); boolean cjk = isCjk(unit.text.codePointAt(0));
            boolean boundary = cjk || i == units.size() - 1 || units.get(i + 1).text.isBlank() || isCjk(units.get(i + 1).text.codePointAt(0));
            units.set(i, new Unit(unit.text, unit.start, unit.end, boundary && !unit.text.isBlank()));
        }
        return List.copyOf(units);
    }
    public static boolean isCjk(int cp) {
        return switch (Character.UnicodeScript.of(cp)) { case HAN, HIRAGANA, KATAKANA, HANGUL -> true; default -> false; };
    }
    public static boolean speakable(String unit, boolean punctuation) {
        return unit.codePoints().anyMatch(cp -> Character.isLetterOrDigit(cp) || (!Character.isWhitespace(cp) && punctuation));
    }
    private UnicodeUnits() {}
}
