package io.github.axiraa.cinematicchat.api;
import java.util.concurrent.CompletableFuture;
public interface PlatformAudioBackend {
    interface VoiceHandle {}
    CompletableFuture<VoiceHandle> play(DecodedVoiceClip clip, VoicePlaybackParams params);
    void stop(VoiceHandle handle);
    void stopAll();
    void reload();
}
