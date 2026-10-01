package io.github.axiraa.cinematicchat.engine;
import io.github.axiraa.cinematicchat.api.StyledText;
import java.util.List;
public record SubtitleDocument(StyledText source, List<SubtitleNode> nodes, List<String> diagnostics, CcmlLimits limits) {
    public SubtitleDocument { nodes = List.copyOf(nodes); diagnostics = List.copyOf(diagnostics); }
}
