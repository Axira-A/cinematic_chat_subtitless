package io.github.axiraa.cinematicchat.client;

import io.github.axiraa.cinematicchat.engine.*;
import java.util.*;

/** Visual-only presets. Applying one never changes trust, message filters, or voice profiles. */
public final class HudPresets {
    public int version = 1;
    public Map<String, Visual> presets = new LinkedHashMap<>();

    public record Visual(HudSettings hud, String fontId, boolean showSender, boolean shadow,
                         boolean backgroundEnabled, double backgroundOpacity, double senderScale,
                         double senderOpacity, double lineSpacing, EngineConfig animation,Animation.Easing entryEasing,Animation.Easing exitEasing) {
        public static Visual capture(CinematicConfig config) {
            return new Visual(ConfigManager.JSON.fromJson(ConfigManager.JSON.toJson(config.hud), HudSettings.class),
                    config.fontId, config.showSender, config.shadow, config.backgroundEnabled,
                    config.backgroundOpacity, config.senderScale, config.senderOpacity, config.lineSpacing, config.animation,config.entryEasing,config.exitEasing);
        }
        public void apply(CinematicConfig config) {
            config.hud = ConfigManager.JSON.fromJson(ConfigManager.JSON.toJson(hud), HudSettings.class);
            config.fontId = fontId; config.showSender = showSender; config.shadow = shadow;
            config.backgroundEnabled = backgroundEnabled; config.backgroundOpacity = backgroundOpacity;
            config.senderScale = senderScale; config.senderOpacity = senderOpacity;
            config.lineSpacing = lineSpacing; config.animation = animation;config.entryEasing=entryEasing;config.exitEasing=exitEasing; config.validate();
        }
    }

    public void validate() {
        version = 1;
        if (presets == null) presets = new LinkedHashMap<>();
        presets.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isBlank() || e.getKey().length() > 64 || e.getValue() == null);
        while (presets.size() > 64) presets.remove(presets.keySet().iterator().next());
    }

    public static Map<String, Visual> builtins() {
        Map<String, Visual> result = new LinkedHashMap<>();
        for (String name : List.of("Cinematic", "Minimal", "RPG", "Adventure", "Accessible", "Vanilla-like")) {
            CinematicConfig config = new CinematicConfig();
            switch (name) {
                case "Minimal" -> { config.showSender = false; config.hud.scale = .85; config.hud.widthRatio = .35; }
                case "RPG" -> { config.backgroundEnabled = true; config.hud.scale = 1.15; config.senderOpacity = 1; }
                case "Adventure" -> { config.hud.offsetY = -.15; config.hud.widthRatio = .55; }
                case "Accessible" -> { config.hud.scale = 1.5; config.backgroundEnabled = true; config.backgroundOpacity = .7; config.animation = instant(config.animation); }
                case "Vanilla-like" -> { config.hud.anchor = HudSettings.Anchor.BOTTOM_LEFT; config.hud.offsetX = .03; config.hud.offsetY = -.12; config.hud.alignment = SubtitleLayoutEngine.Alignment.LEFT; config.backgroundEnabled = true; config.animation = instant(config.animation); }
                default -> { }
            }
            result.put(name, Visual.capture(config));
        }
        return Collections.unmodifiableMap(result);
    }

    private static EngineConfig instant(EngineConfig a) {
        return new EngineConfig(RevealMode.INSTANT, a.speed(), a.baseDuration(), a.perCharacter(), a.minDuration(), a.maxDuration(),
                a.entryDuration(), a.exitDuration(), a.entry(), a.exit(), a.easing(), a.maxVisible(), a.maxQueued());
    }
}
