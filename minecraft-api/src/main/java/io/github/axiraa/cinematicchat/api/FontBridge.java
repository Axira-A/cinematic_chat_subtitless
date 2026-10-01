package io.github.axiraa.cinematicchat.api;
public interface FontBridge {
    double width(String grapheme, TextStyle style);
    double lineHeight(TextStyle style);
}
