package io.github.axiraa.cinematicchat.engine;
public record EngineConfig(RevealMode reveal, double speed, double baseDuration, double perCharacter,
                           double minDuration, double maxDuration, double entryDuration, double exitDuration,
                           Animation.Kind entry, Animation.Kind exit, Animation.Easing easing, int maxVisible, int maxQueued) {
    public static final EngineConfig DEFAULT = new EngineConfig(RevealMode.SMOOTH, 30, 2, .055, 3, 12, .2, .35,
            Animation.Kind.FADE, Animation.Kind.FADE, Animation.Easing.EASE_OUT, 3, 50);
    public EngineConfig bounded() {
        double minimum = CcmlLimits.clamp(minDuration, 0, 30);
        return new EngineConfig(reveal == null ? RevealMode.SMOOTH : reveal, CcmlLimits.clamp(speed, 1, 120),
                CcmlLimits.clamp(baseDuration, 0, 30), CcmlLimits.clamp(perCharacter, 0, 1),
                minimum, CcmlLimits.clamp(maxDuration, minimum, 60),
                CcmlLimits.clamp(entryDuration, 0, 3), CcmlLimits.clamp(exitDuration, 0, 3),
                entry == null ? Animation.Kind.FADE : entry, exit == null ? Animation.Kind.FADE : exit,
                easing == null ? Animation.Easing.EASE_OUT : easing, Math.clamp(maxVisible, 1, 8), Math.clamp(maxQueued, 0, 200));
    }
}
