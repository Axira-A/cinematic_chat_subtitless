package io.github.axiraa.cinematicchat.engine;

import io.github.axiraa.cinematicchat.api.TextStyle;
import java.util.*;
import static io.github.axiraa.cinematicchat.engine.SubtitleRuntimeEvent.Kind.*;

/** Compiles finite presentation actions once. Timings are seconds on a monotonic logical clock. */
public final class SubtitleTimeline {
    public record Glyph(String text, TextStyle style, double shownAt, double fadeDuration) {}
    public record Action(double time, SubtitleRuntimeEvent.Kind event, Glyph glyph, int erase, boolean instant) {}
    public record Plan(List<Action> actions, double completeAt, double holdDuration, double entryDuration, double exitDuration, int readableUnits) {
        public Plan { actions = List.copyOf(actions); }
    }
    private record Piece(SubtitleNode node, List<SubtitleNode.Span> spans) {}
    public Plan compile(SubtitleDocument document, EngineConfig requested) {
        EngineConfig config = requested.bounded();
        List<Piece> pieces = new ArrayList<>(); flatten(document.nodes(), List.of(), pieces);
        List<Action> actions = new ArrayList<>(); double cursor = 0, completion = 0, hold = 0;
        double speed = config.speed(), entry = config.entry()==Animation.Kind.NONE?0:config.entryDuration(), exit = config.exit()==Animation.Kind.NONE?0:config.exitDuration();
        RevealMode reveal = config.reveal(); int readable = 0;
        StringBuilder word = new StringBuilder();
        for (int pieceIndex=0;pieceIndex<pieces.size();pieceIndex++) {
            Piece piece=pieces.get(pieceIndex);
            if (piece.node instanceof SubtitleNode.Text text) {
                // Style tag boundaries cannot split a grapheme or an English word.
                StringBuilder joined=new StringBuilder(text.text());List<Piece> chunks=new ArrayList<>();chunks.add(piece);
                while(pieceIndex+1<pieces.size()&&pieces.get(pieceIndex+1).node instanceof SubtitleNode.Text nextText){chunks.add(pieces.get(++pieceIndex));joined.append(nextText.text());}
                List<UnicodeUnits.Unit> units = UnicodeUnits.split(joined.toString());int chunkIndex=0,chunkStart=0;
                double wordTime = cursor;
                for (int i = 0; i < units.size(); i++) {
                    UnicodeUnits.Unit unit = units.get(i);
                    while(chunkIndex+1<chunks.size()&&unit.start()>=chunkStart+((SubtitleNode.Text)chunks.get(chunkIndex).node).text().length()){chunkStart+=((SubtitleNode.Text)chunks.get(chunkIndex++).node).text().length();}
                    var styledPiece=chunks.get(chunkIndex);var styledText=(SubtitleNode.Text)styledPiece.node;
                    TextStyle style = document.source().styleAt(styledText.sourceOffsets().get(unit.start()-chunkStart));
                    for (SubtitleNode.Span span : styledPiece.spans) {
                        style = style.with(span.name(), span.value(), span.literalValue());
                        if (span.attributes().containsKey("frequency")) style = style.with(span.name() + "Frequency", span.attributes().get("frequency"), "");
                    }
                    style = new TextStyle(style.color(), style.font(), style.bold(), style.italic(), style.underline(), style.strikethrough(), style.obfuscated(),
                            CcmlLimits.clamp(style.scale(), document.limits().minScale(), document.limits().maxScale()), CcmlLimits.clamp(style.alpha(), 0, 1), style.shake(), style.shakeFrequency(), style.wave(), style.waveFrequency());
                    double at = reveal == RevealMode.WORD_BY_WORD ? wordTime : cursor;
                    boolean budget = at >= document.limits().scriptSeconds();
                    double fade = reveal == RevealMode.SMOOTH && !budget ? .1 : 0;
                    at = Math.min(at, document.limits().scriptSeconds());
                    actions.add(new Action(at, REVEAL_UNIT_SHOWN, new Glyph(unit.text(), style, at, fade), 0, reveal == RevealMode.INSTANT || budget));
                    if (!unit.text().isBlank()) readable++;
                    completion = Math.max(completion, at + fade);
                    if(unit.text().isBlank())word.setLength(0);else word.append(unit.text());
                    if (unit.wordEnd()) {actions.add(new Action(at, WORD_SHOWN, new Glyph(word.toString(), style, at, fade), 0, reveal == RevealMode.INSTANT || budget));word.setLength(0);}
                    if (reveal == RevealMode.WORD_BY_WORD) {
                        if (unit.wordEnd()) { wordTime += 1 / speed; cursor = wordTime; }
                    } else if (reveal != RevealMode.INSTANT && !budget) cursor += 1 / speed;
                }
            } else if (piece.node instanceof SubtitleNode.Command command) {
                cursor = Math.min(cursor, document.limits().scriptSeconds());
                switch (command.name()) {
                    case "speed" -> speed = command.value() < 0 ? config.speed() : command.value();
                    case "reveal" -> reveal = switch (command.literalValue()) { case "instant" -> RevealMode.INSTANT; case "typewriter" -> RevealMode.TYPEWRITER; case "word" -> RevealMode.WORD_BY_WORD; default -> RevealMode.SMOOTH; };
                    case "wait" -> cursor = Math.max(cursor, Math.max(completion, entry));
                    case "pause" -> {
                        cursor = Math.max(cursor, completion);
                        if (cursor < document.limits().scriptSeconds()) {
                            actions.add(new Action(cursor, PAUSE_STARTED, null, 0, false));
                            cursor = Math.min(document.limits().scriptSeconds(), cursor + command.value());
                            actions.add(new Action(cursor, PAUSE_ENDED, null, 0, false));
                        }
                    }
                    case "hold" -> hold = Math.max(hold, command.value());
                    case "clear", "replace", "erase" -> {
                        cursor = Math.max(cursor, completion);
                        if (cursor < document.limits().scriptSeconds()) actions.add(new Action(cursor,
                                command.name().equals("replace") ? SUBTITLE_REPLACED : SUBTITLE_CLEARED, null,
                                command.name().equals("erase") ? (int)command.value() : -1, false));
                    }
                    case "fadein" -> entry = command.value();
                    case "fadeout" -> exit = command.value();
                    default -> throw new IllegalStateException("Uncompiled command " + command.name());
                }
            }
        }
        actions.sort(Comparator.comparingDouble(Action::time));
        double complete = Math.min(document.limits().scriptSeconds(), Math.max(completion, cursor));
        double read = CcmlLimits.clamp(config.baseDuration() + readable * config.perCharacter(), config.minDuration(), config.maxDuration());
        return new Plan(actions, complete, Math.max(hold, read), entry, exit, readable);
    }
    private static void flatten(List<SubtitleNode> nodes, List<SubtitleNode.Span> spans, List<Piece> output) {
        for (SubtitleNode node : nodes) {
            if (node instanceof SubtitleNode.Span span) { List<SubtitleNode.Span> next = new ArrayList<>(spans); next.add(span); flatten(span.children(), List.copyOf(next), output); }
            else output.add(new Piece(node, spans));
        }
    }
}
