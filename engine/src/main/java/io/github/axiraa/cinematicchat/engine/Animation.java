package io.github.axiraa.cinematicchat.engine;
public final class Animation {
    public enum Kind { NONE, FADE, SLIDE_UP, SLIDE_DOWN, SCALE_FADE }
    public enum Easing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    public static double ease(double value, Easing easing) {
        double t = CcmlLimits.clamp(value, 0, 1);
        return switch (easing) { case LINEAR -> t; case EASE_IN -> t*t; case EASE_OUT -> 1-(1-t)*(1-t); case EASE_IN_OUT -> t*t*(3-2*t); };
    }
    private Animation() {}
}
