package io.github.axiraa.cinematicchat.engine;
import io.github.axiraa.cinematicchat.api.ChatEnvelope;
import java.util.*;
import java.util.function.Consumer;

public final class SubtitleQueue {
    public enum Priority { SYSTEM, NORMAL, IMPORTANT }
    public record Pending(ChatEnvelope envelope, SubtitleDocument document, Priority priority, String voiceId) {}
    public static final class Entry {
        private final Pending pending; private final SubtitleRuntime runtime; private final double started;
        Entry(Pending pending, SubtitleRuntime runtime, double started) { this.pending = pending; this.runtime = runtime; this.started = started; }
        public Pending pending() { return pending; } public SubtitleRuntime runtime() { return runtime; }
        public double startedAt() { return started; }
    }
    private record Queued(Pending pending,double arrivedAt) {}
    private record TimedEvent(double absoluteTime,int order,SubtitleRuntimeEvent event) {}
    private final Deque<Queued> queued = new ArrayDeque<>(); private final List<Entry> visible = new ArrayList<>();
    private double advanced;
    private EngineConfig config; private final SubtitleTimeline compiler = new SubtitleTimeline();
    public SubtitleQueue(EngineConfig config) { this.config = config.bounded(); }
    public void configure(EngineConfig config) { this.config = config.bounded(); trim();while(visible.size()>this.config.maxVisible())visible.removeFirst(); }
    public void enqueue(Pending pending) { enqueue(pending,advanced); }
    public void enqueue(Pending pending,double arrivedAt) {
        if(!Double.isFinite(arrivedAt))throw new IllegalArgumentException("Arrival must be finite");
        double arrival=Math.max(advanced,arrivedAt);
        if(!queued.isEmpty())arrival=Math.max(arrival,queued.getLast().arrivedAt);
        queued.addLast(new Queued(Objects.requireNonNull(pending),arrival));trim();
    }
    private void trim() {
        while (queued.size() > config.maxQueued()) {
            Queued victim = queued.stream().min(Comparator.comparing(q->q.pending.priority())).orElseThrow(); queued.removeFirstOccurrence(victim);
        }
    }
    public void advance(double now, Consumer<SubtitleRuntimeEvent> sink) {
        if(!Double.isFinite(now)||now<advanced)return;
        double cursor=advanced;
        while(true){
            var iterator=visible.iterator();
            while(iterator.hasNext()){
                Entry entry=iterator.next();
                if(entry.started+entry.runtime.endAt()<=cursor+1e-9){
                    entry.runtime.advance(entry.runtime.endAt(),sink);
                    sink.accept(new SubtitleRuntimeEvent(entry.pending.envelope.id(),SubtitleRuntimeEvent.Kind.SUBTITLE_REMOVED,entry.runtime.endAt(),"",false));
                    iterator.remove();
                }
            }
            while(visible.size()<config.maxVisible()&&!queued.isEmpty()&&queued.getFirst().arrivedAt<=cursor+1e-9){
                Queued waiting=queued.removeFirst();Pending pending=waiting.pending;
                Entry entry=new Entry(pending,new SubtitleRuntime(pending.envelope.id(),compiler.compile(pending.document,config)),Math.max(cursor,waiting.arrivedAt));
                visible.add(entry);entry.runtime.advance(0,sink);
            }
            if(cursor>=now)break;
            double next=now;
            for(Entry entry:visible)next=Math.min(next,entry.started+entry.runtime.endAt());
            if(visible.size()<config.maxVisible()&&!queued.isEmpty())next=Math.min(next,queued.getFirst().arrivedAt);
            List<TimedEvent> events=new ArrayList<>();
            for(int index=0;index<visible.size();index++){
                Entry entry=visible.get(index);int order=index;
                entry.runtime.advance(Math.max(0,next-entry.started),event->events.add(new TimedEvent(entry.started+event.time(),order,event)));
            }
            events.sort(Comparator.comparingDouble(TimedEvent::absoluteTime).thenComparingInt(TimedEvent::order));
            for(TimedEvent event:events)sink.accept(event.event);
            cursor=next;
        }
        advanced=now;
    }
    public void removeSignature(String signature, Consumer<SubtitleRuntimeEvent> sink) {
        if (signature == null) return;
        queued.removeIf(p -> signature.equals(p.pending.envelope.signatureKey()));
        visible.removeIf(e -> { if (!signature.equals(e.pending.envelope.signatureKey())) return false;
            sink.accept(new SubtitleRuntimeEvent(e.pending.envelope.id(), SubtitleRuntimeEvent.Kind.SUBTITLE_REMOVED, 0, "", false)); return true; });
    }
    public void clear() { queued.clear(); visible.clear(); }
    public List<Entry> visible() { return List.copyOf(visible); }
    public int queuedSize() { return queued.size(); }
}
