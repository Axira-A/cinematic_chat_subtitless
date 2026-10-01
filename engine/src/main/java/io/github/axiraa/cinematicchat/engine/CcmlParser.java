package io.github.axiraa.cinematicchat.engine;

import io.github.axiraa.cinematicchat.api.StyledText;
import java.util.*;

/** Bounded scanner. Input produces data only; there are no external capabilities. */
public final class CcmlParser {
    private static final Set<String> STYLES = Set.of("color", "scale", "alpha", "shake", "wave", "bold", "italic", "underline");
    private static final Set<String> COMMANDS = Set.of("pause", "wait", "hold", "speed", "reveal", "clear", "replace", "erase", "fadein", "fadeout", "br");
    private static final class Frame {
        final String name; final int start; final Tag tag; final List<SubtitleNode> children = new ArrayList<>();
        Frame(String name, int start, Tag tag) { this.name = name; this.start = start; this.tag = tag; }
    }
    private record Tag(String name, double value, String literal, Map<String, Double> attributes) {}
    public SubtitleDocument parse(StyledText source, CcmlLimits requested, boolean enabled) {
        CcmlLimits limits = requested.bounded();
        String input = source.text();
        if (input.length() > limits.inputLength()) {
            int end = limits.inputLength();
            if (end > 0 && Character.isHighSurrogate(input.charAt(end - 1))) end--;
            input = input.substring(0, end);
        }
        StyledText bounded = new StyledText(input, source.runs());
        if (!enabled) return plain(bounded, limits, List.of());
        try{return scan(bounded,limits);}catch(RuntimeException error){return plain(bounded,limits,List.of("Parser fallback: "+error.getClass().getSimpleName()));}
    }
    private SubtitleDocument scan(StyledText bounded,CcmlLimits limits){
        String input=bounded.text();
        List<String> diagnostics = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>(); stack.push(new Frame("", 0, null));
        int count = 0;
        for (int i = 0; i < input.length();) {
            int start = i;
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && "n\\[]".indexOf(input.charAt(i + 1)) >= 0) {
                char escaped = input.charAt(i + 1); String value = escaped == 'n' ? "\n" : String.valueOf(escaped);
                stack.peek().children.add(text(value, i,i+2)); i += 2; count++;
            } else if (c == '[') {
                int close = input.indexOf(']', i + 1);
                if (close < 0 || close - i > 512) { stack.peek().children.add(text("[", i++)); count++; }
                else {
                    String token = input.substring(i + 1, close);
                    if (token.startsWith("/") && stack.size() > 1 && token.substring(1).equals(stack.peek().name)) {
                        Frame f = stack.pop();
                        stack.peek().children.add(new SubtitleNode.Span(f.name, f.tag.value, f.tag.literal, f.tag.attributes, f.children,new SubtitleNode.SourceRange(f.start,close+1)));
                        i = close + 1;
                    } else {
                        Tag tag = readTag(token, limits);
                        if (tag != null && STYLES.contains(tag.name) && stack.size() <= limits.depth()) {
                            stack.push(new Frame(tag.name, i, tag)); i = close + 1; count++;
                        } else if (tag != null && COMMANDS.contains(tag.name)) {
                            stack.peek().children.add(tag.name.equals("br") ? text("\n", i,close+1) : new SubtitleNode.Command(tag.name, tag.value, tag.literal,new SubtitleNode.SourceRange(i,close+1)));
                            i = close + 1; count++;
                        } else {
                            stack.peek().children.add(text(input.substring(i, close + 1), i)); i = close + 1; count++;
                            if (diagnostics.size() < 16) diagnostics.add("Literal markup at " + start);
                        }
                    }
                }
            } else {
                i++;
                while (i < input.length() && input.charAt(i) != '[' && input.charAt(i) != '\\') i++;
                stack.peek().children.add(text(input.substring(start, i), start)); count++;
            }
            if (count > limits.nodes()) return plain(bounded, limits, List.of("AST node limit: literal fallback"));
        }
        while (stack.size() > 1) {
            Frame frame = stack.pop();
            stack.peek().children.add(text(input.substring(frame.start), frame.start));
            // An unmatched outer span owns the rest of the source. Discard its partially parsed children.
        }
        return new SubtitleDocument(bounded, stack.pop().children, diagnostics, limits);
    }
    public SubtitleDocument parse(String source) { return parse(StyledText.plain(source), CcmlLimits.ABSOLUTE, true); }
    private static SubtitleDocument plain(StyledText text, CcmlLimits limits, List<String> diagnostics) {
        return new SubtitleDocument(text, List.of(text(text.text(), 0)), diagnostics, limits);
    }
    private static SubtitleNode.Text text(String value, int offset) {
        return text(value,offset,offset+value.length());
    }
    private static SubtitleNode.Text text(String value,int offset,int end){
        List<Integer> offsets = new ArrayList<>(value.length());
        for (int j = 0; j < value.length(); j++) offsets.add(offset + j);
        return new SubtitleNode.Text(value, offsets,new SubtitleNode.SourceRange(offset,end));
    }
    private static Tag readTag(String token, CcmlLimits limits) {
        try {
            String[] pieces = token.trim().split("\\s+"); String[] head = pieces[0].split("=", 2);
            String name = head[0]; String value = head.length == 2 ? head[1] : "";
            if (!STYLES.contains(name) && !COMMANDS.contains(name)) return null;
            Map<String, Double> attributes = new HashMap<>();
            for (int i = 1; i < pieces.length; i++) {
                String[] attr = pieces[i].split("=", 2);
                if (attr.length != 2 || attributes.containsKey(attr[0])) return null;
                if (!(name.equals("shake") && Set.of("strength", "frequency").contains(attr[0])) &&
                    !(name.equals("wave") && Set.of("amplitude", "frequency").contains(attr[0]))) return null;
                attributes.put(attr[0], number(attr[1]));
            }
            double number = 0;
            switch (name) {
                case "color" -> { if (!value.matches("#[0-9a-fA-F]{6}")) return null; }
                case "scale" -> number = CcmlLimits.clamp(number(value), limits.minScale(), limits.maxScale());
                case "alpha" -> number = CcmlLimits.clamp(number(value), 0, 1);
                case "pause" -> number = CcmlLimits.clamp(number(value), 0, limits.pauseSeconds());
                case "hold" -> number = CcmlLimits.clamp(number(value), 0, limits.holdSeconds());
                case "speed" -> number = value.equals("default") ? -1 : CcmlLimits.clamp(number(value), 1, 120);
                case "erase" -> { if (!value.matches("-?[0-9]+")) return null; number = CcmlLimits.clamp(number(value), 0, 256); }
                case "fadein", "fadeout" -> number = CcmlLimits.clamp(number(value), 0, 3);
                case "reveal" -> { if (!Set.of("instant", "typewriter", "smooth", "word").contains(value)) return null; }
                case "shake", "wave" -> {
                    if (!value.isEmpty()) return null;
                    String magnitude = name.equals("shake") ? "strength" : "amplitude";
                    number = CcmlLimits.clamp(attributes.getOrDefault(magnitude, name.equals("shake") ? .7 : 2), 0, name.equals("shake") ? limits.shakePixels() : limits.wavePixels());
                    attributes.put("frequency", CcmlLimits.clamp(attributes.getOrDefault("frequency", name.equals("shake") ? 12.0 : 2.0), 0, 30));
                }
                default -> { if (!value.isEmpty()) return null; }
            }
            return new Tag(name, number, value, attributes);
        } catch (IllegalArgumentException ex) { return null; }
    }
    private static double number(String value) {
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("Non-finite number");
        return parsed;
    }
}
