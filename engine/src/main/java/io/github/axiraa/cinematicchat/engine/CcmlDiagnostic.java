package io.github.axiraa.cinematicchat.engine;

/** Language-neutral parser information. Platform adapters format it for users. */
public record CcmlDiagnostic(Code code, int offset, String detail) {
    public enum Code { LITERAL_MARKUP, NODE_LIMIT, PARSER_FALLBACK }
}
