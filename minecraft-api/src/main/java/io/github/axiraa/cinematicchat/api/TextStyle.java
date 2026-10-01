package io.github.axiraa.cinematicchat.api;

/** Immutable style independent of Minecraft. Null font means use the user's selected font. */
public record TextStyle(int color, String font, boolean bold, boolean italic, boolean underline,
                        boolean strikethrough, boolean obfuscated, double scale, double alpha,
                        double shake, double shakeFrequency, double wave, double waveFrequency) {
    public static final TextStyle DEFAULT = new TextStyle(0xffffff, null, false, false, false, false, false, 1, 1, 0, 12, 0, 2);
    public TextStyle with(String name, double value, String text) {
        return new TextStyle(name.equals("color") ? Integer.parseInt(text.substring(1), 16) : color,
                font, name.equals("bold") || bold, name.equals("italic") || italic,
                name.equals("underline") || underline, strikethrough, obfuscated,
                name.equals("scale") ? scale * value : scale, name.equals("alpha") ? alpha * value : alpha,
                name.equals("shake") ? value : shake, name.equals("shakeFrequency") ? value : shakeFrequency,
                name.equals("wave") ? value : wave, name.equals("waveFrequency") ? value : waveFrequency);
    }
}
