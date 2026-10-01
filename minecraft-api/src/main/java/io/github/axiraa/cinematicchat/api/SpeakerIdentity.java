package io.github.axiraa.cinematicchat.api;
import java.util.UUID;
public record SpeakerIdentity(ChatSource source, UUID uuid, String profileName, StyledText displayName) {
    public static SpeakerIdentity unknown(ChatSource source) { return new SpeakerIdentity(source, null, null, null); }
    public boolean known() { return uuid != null || profileName != null || displayName != null; }
}
