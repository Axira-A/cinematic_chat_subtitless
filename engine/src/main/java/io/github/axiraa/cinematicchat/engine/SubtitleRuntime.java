package io.github.axiraa.cinematicchat.engine;
import java.util.*;
import java.util.function.Consumer;
import static io.github.axiraa.cinematicchat.engine.SubtitleRuntimeEvent.Kind.*;

public final class SubtitleRuntime {
    public record VisibleGlyph(SubtitleTimeline.Glyph glyph, double alpha) {}
    public record Snapshot(long id, double time, List<VisibleGlyph> glyphs, List<SubtitleTimeline.Glyph> layoutGlyphs, int revision, double entryProgress, double exitProgress, boolean revealing) {
        public Snapshot { glyphs = List.copyOf(glyphs); }
        public String text() {StringBuilder text=new StringBuilder();for(var g:glyphs)text.append(g.glyph.text());return text.toString();}
    }
    private final long id; private final SubtitleTimeline.Plan plan;
    private final List<SubtitleTimeline.Glyph> visible = new ArrayList<>();
    private int next,revision; private double time = -1; private boolean started, completed;
    private List<SubtitleTimeline.Glyph> layoutGlyphs;
    public SubtitleRuntime(long id, SubtitleTimeline.Plan plan) { this.id = id; this.plan = plan; rebuildLayout(); }
    private void rebuildLayout() {
        List<SubtitleTimeline.Glyph> glyphs = new ArrayList<>(visible);
        for (int i = next; i < plan.actions().size(); i++) { var action = plan.actions().get(i);
            if (action.event() == SUBTITLE_CLEARED || action.event() == SUBTITLE_REPLACED) break;
            if (action.event() == REVEAL_UNIT_SHOWN) glyphs.add(action.glyph());
        }
        layoutGlyphs = List.copyOf(glyphs);
    }
    public void advance(double seconds, Consumer<SubtitleRuntimeEvent> sink) {
        if (!Double.isFinite(seconds) || seconds < time) return;
        time = seconds;
        if (!started) { started = true; sink.accept(new SubtitleRuntimeEvent(id, SUBTITLE_STARTED, 0, "", false)); }
        while (next < plan.actions().size() && plan.actions().get(next).time() <= seconds + 1e-9) {
            var action = plan.actions().get(next++);
            if (action.event() == REVEAL_UNIT_SHOWN) visible.add(action.glyph());
            else if (action.event() == SUBTITLE_CLEARED || action.event() == SUBTITLE_REPLACED) {
                if (action.erase() < 0) visible.clear();
                else for (int i = 0; i < action.erase() && !visible.isEmpty(); i++) visible.removeLast();
                revision++; rebuildLayout();
            }
            sink.accept(new SubtitleRuntimeEvent(id, action.event(), action.time(), action.glyph() == null ? "" : action.glyph().text(), action.instant()));
        }
        if (!completed && seconds >= plan.completeAt()) { completed = true; sink.accept(new SubtitleRuntimeEvent(id, SUBTITLE_COMPLETED, plan.completeAt(), "", false)); }
    }
    public Snapshot snapshot() {
        return snapshotAt(time);
    }
    /** Read-only interpolation; destructive timeline actions remain owned by advance(). */
    public Snapshot snapshotAt(double seconds) {
        double rendered = Double.isFinite(seconds) ? Math.max(time, seconds) : time;
        for (int i=next;i<plan.actions().size();i++) {
            var action=plan.actions().get(i);
            if (action.event()==SUBTITLE_CLEARED || action.event()==SUBTITLE_REPLACED) { rendered=Math.min(rendered,action.time());break; }
        }
        final double at=rendered;
        List<VisibleGlyph> glyphs = visible.stream().map(g -> new VisibleGlyph(g,
                g.fadeDuration() == 0 ? 1 : CcmlLimits.clamp((at - g.shownAt()) / g.fadeDuration(), 0, 1))).toList();
        double entry = plan.entryDuration() == 0 ? 1 : CcmlLimits.clamp(at / plan.entryDuration(), 0, 1);
        double exitStart = plan.completeAt() + plan.holdDuration();
        double exit = plan.exitDuration() == 0 ? (at >= exitStart ? 1 : 0) : CcmlLimits.clamp((at - exitStart) / plan.exitDuration(), 0, 1);
        return new Snapshot(id, Math.max(0, at), glyphs, layoutGlyphs, revision, entry, exit, at<plan.completeAt());
    }
    public boolean expired() { return time >= plan.completeAt() + plan.holdDuration() + plan.exitDuration(); }
    public double completeAt() { return plan.completeAt(); }
    public double endAt(){return plan.completeAt()+plan.holdDuration()+plan.exitDuration();}
}
