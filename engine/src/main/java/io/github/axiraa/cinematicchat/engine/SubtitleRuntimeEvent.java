package io.github.axiraa.cinematicchat.engine;
public record SubtitleRuntimeEvent(long entryId, Kind kind, double time, String unit, boolean instant) {
    public enum Kind { SUBTITLE_STARTED, REVEAL_UNIT_SHOWN, WORD_SHOWN, PAUSE_STARTED, PAUSE_ENDED,
        SUBTITLE_COMPLETED, SUBTITLE_CLEARED, SUBTITLE_REPLACED, SUBTITLE_REMOVED }
}
