package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.engine.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** All mutable engine state is owned by the client thread. Incoming hooks can enqueue safely. */
public final class SubtitleController {
    private CinematicConfig config;private VoiceConfig voices;
    private final CcmlParser parser=new CcmlParser();private final SubtitleQueue queue;
    private final ConcurrentLinkedQueue<ChatEnvelope> incoming=new ConcurrentLinkedQueue<>();
    private final PseudoVoiceController voice;private final ExternalAudioManager audio;private long lastNanos;private double logicalTime;private boolean inWorld,paused;private String serverKey;
    public SubtitleController(CinematicConfig config,VoiceConfig voices,PlatformAudioBackend backend,ExternalAudioManager audio){
        this.config=config;this.voices=voices;this.audio=audio;queue=new SubtitleQueue(config.animation);voice=new PseudoVoiceController(voices,backend,audio,new Random());
    }
    public void capture(ChatEnvelope envelope){if(incoming.size()<200)incoming.add(envelope);}
    public void tick(long now,ClientState state){
        double delta=lastNanos==0?0:Math.max(0,(now-lastNanos)/1e9);lastNanos=now;
        if(!state.inWorld()){if(inWorld)clear();inWorld=false;return;}inWorld=true;
        if(!config.enabled){clear();return;}
        boolean pause=state.gamePaused()||(state.chatOpen()&&config.pauseWhenChatOpen);
        paused=pause;
        if(!pause)logicalTime+=delta;
        ChatEnvelope e;int budget=32;
        while(budget-->0&&(e=incoming.poll())!=null){
            if(!config.messageTypes.getOrDefault(e.source(),true))continue;
            var rule=config.speakerRule(serverKey);if(rule!=null&&rule.enabled&&!e.speaker().known())e=fallback(e,rule);
            var doc=parser.parse(e.body(),config.effectiveLimits(),config.parseMarkup(e.source()));
            VoiceProfile profile=new VoiceProfileResolver(voices).resolve(e.speaker());String id=profile==null?null:profile.id;
            if(profile!=null)for(String clip:profile.audio)audio.load(clip);
            queue.enqueue(new SubtitleQueue.Pending(e,doc,config.priorities.getOrDefault(e.source(),SubtitleQueue.Priority.SYSTEM),id),logicalTime);
        }
        if(!pause)queue.advance(logicalTime,event->{
            if(event.kind()==SubtitleRuntimeEvent.Kind.SUBTITLE_STARTED)for(var entry:queue.visible())if(entry.pending().envelope().id()==event.entryId())voice.bind(event.entryId(),entry.pending().voiceId());
            voice.event(event);
        });
        voice.flush(state.hideGui()||state.chatOpen()||pause);
    }
    private ChatEnvelope fallback(ChatEnvelope e,CinematicConfig.SpeakerRule rule){String text=e.body().text();if(!text.startsWith(rule.prefix))return e;int start=rule.prefix.length(),end=text.indexOf(rule.delimiter,start);if(end<=start||end-start>64)return e;
        String name=text.substring(start,end);var speaker=new SpeakerIdentity(e.source(),null,null,StyledText.plain(name));
        return new ChatEnvelope(e.id(),e.source(),speaker,e.body(),e.original(),e.signatureKey());}
    public void clear(){incoming.clear();queue.clear();voice.clear();}
    public void removeSignature(String signature){queue.removeSignature(signature,voice::event);incoming.removeIf(e->Objects.equals(signature,e.signatureKey()));}
    public SubtitleQueue queue(){return queue;}
    public CinematicConfig config(){return config;}
    public void serverKey(String key){serverKey=key;}
    public double renderTime(long now){return logicalTime+(paused||!inWorld||!config.enabled?0:Math.max(0,Math.min(.05,(now-lastNanos)/1e9)));}
    public SubtitleRuntime.Snapshot snapshot(SubtitleQueue.Entry entry,long now){return entry.runtime().snapshotAt(Math.max(0,renderTime(now)-entry.startedAt()));}
    public void configure(CinematicConfig config,VoiceConfig voices){config.validate();voices.validate();this.config=config;this.voices=voices;queue.configure(config.animation);voice.configure(voices);for(var entry:queue.visible())voice.bind(entry.pending().envelope().id(),entry.pending().voiceId());}
    public void voicePreview(String id){voice.preview(new VoiceProfileResolver(voices).byId(id));}
    public void audioReloaded(){voice.audioReloaded();}
}
