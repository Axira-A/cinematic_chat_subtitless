package io.github.axiraa.cinematicchat.api;
public record ChatEnvelope(long id, ChatSource source, SpeakerIdentity speaker, StyledText body,
                           OriginalMessageRef original, String signatureKey) {}
