package io.github.axiraa.cinematicchat.engine;

public record CcmlLimits(int inputLength, int nodes, int depth, double pauseSeconds, double holdSeconds,
                         double minScale, double maxScale, double shakePixels, double wavePixels, double scriptSeconds) {
    public static final CcmlLimits ABSOLUTE = new CcmlLimits(32768, 2048, 16, 10, 30, .5, 2.5, 4, 8, 120);
    public static final CcmlLimits SAFE = new CcmlLimits(32768, 2048, 16, 3, 10, .75, 1.5, 1, 2, 60);
    public CcmlLimits bounded() {
        return new CcmlLimits(Math.clamp(inputLength, 1, 32768), Math.clamp(nodes, 1, 2048), Math.clamp(depth, 1, 16),
                clamp(pauseSeconds, 0, 10), clamp(holdSeconds, 0, 30), clamp(minScale, .5, 2.5),
                clamp(maxScale, Math.max(.5, clamp(minScale, .5, 2.5)), 2.5), clamp(shakePixels, 0, 4),
                clamp(wavePixels, 0, 8), clamp(scriptSeconds, 1, 120));
    }
    public static double clamp(double v, double min, double max) { return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : min; }
}
