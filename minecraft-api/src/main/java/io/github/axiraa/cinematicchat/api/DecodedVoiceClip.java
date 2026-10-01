package io.github.axiraa.cinematicchat.api;
import java.nio.ByteBuffer;
public record DecodedVoiceClip(String id, ByteBuffer pcm, int sampleRate, int channels, int bitsPerSample) {
    public DecodedVoiceClip { pcm = pcm.asReadOnlyBuffer(); }
    @Override public ByteBuffer pcm() { return pcm.asReadOnlyBuffer(); }
}
