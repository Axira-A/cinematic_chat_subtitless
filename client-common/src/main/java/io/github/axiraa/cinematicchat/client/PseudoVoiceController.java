package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.engine.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PseudoVoiceController {
    private static final class State {
        final VoiceProfile profile;final VoiceShuffleBag bag;final Random intervalRandom,playRandom;double last=-100,nextInterval;int cjk;boolean instantPlayed;
        PlatformAudioBackend.VoiceHandle loop;boolean paused,completed,loopPending;
        final Set<PlatformAudioBackend.VoiceHandle> playing=new LinkedHashSet<>();long generation;
        State(VoiceProfile p,Random random){profile=p;intervalRandom=new Random(random.nextLong());playRandom=new Random(random.nextLong());bag=new VoiceShuffleBag(new Random(random.nextLong()));nextInterval=p.intervalMs/1000.0;}
    }
    private record Completion(long id,long generation,long stateGeneration,boolean loop,PlatformAudioBackend.VoiceHandle handle){}
    private final Map<Long,State> states=new HashMap<>();private final List<Long> requests=new ArrayList<>();
    private final ConcurrentLinkedQueue<Completion> completions=new ConcurrentLinkedQueue<>();
    private final PlatformAudioBackend backend;private final ExternalAudioManager audio;private final Random random;
    private VoiceConfig config;private long generation;private boolean wasMuted;
    public PseudoVoiceController(VoiceConfig config,PlatformAudioBackend backend,ExternalAudioManager audio,Random random){this.config=config;this.backend=backend;this.audio=audio;this.random=random;}
    public void bind(long id,String voiceId){var p=new VoiceProfileResolver(config).byId(voiceId);if(p!=null){states.put(id,new State(p,random));for(String clip:p.audio)audio.load(clip);}}
    public void event(SubtitleRuntimeEvent event){
        State state=states.get(event.entryId());if(state==null)return;
        var mode=state.profile.playMode;
        switch(event.kind()) {
            case SUBTITLE_STARTED->{if(mode==VoiceProfile.PlayMode.ON_SUBTITLE_START||mode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING)requests.add(event.entryId());}
            case REVEAL_UNIT_SHOWN,WORD_SHOWN->{
                if(mode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING){if(event.kind()==SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN&&!state.paused&&!state.completed&&!requests.contains(event.entryId()))requests.add(event.entryId());return;}
                if(mode!=VoiceProfile.PlayMode.PER_REVEAL_UNIT&&mode!=VoiceProfile.PlayMode.PER_WORD)return;
                if(event.kind()==SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN&&mode==VoiceProfile.PlayMode.PER_WORD)return;
                if(event.kind()==SubtitleRuntimeEvent.Kind.WORD_SHOWN&&mode==VoiceProfile.PlayMode.PER_REVEAL_UNIT)return;
                if(!UnicodeUnits.speakable(event.unit(),state.profile.voiceOnPunctuation))return;
                if(!event.instant()&&UnicodeUnits.isCjk(event.unit().codePointAt(0))&&++state.cjk%state.profile.cjkCharactersPerVoice!=0)return;
                if(event.instant()){if(state.instantPlayed)return;state.instantPlayed=true;}
                if(event.time()-state.last>=state.nextInterval){requests.add(event.entryId());state.last=event.time();state.nextInterval=Math.max(.03,(state.profile.intervalMs+(state.intervalRandom.nextDouble()*2-1)*state.profile.intervalRandomnessMs)/1000.0);}
            }
            case PAUSE_STARTED->{state.paused=true;stopLoop(state);if(mode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING)removeRequests(event.entryId());}
            case SUBTITLE_COMPLETED->{state.completed=true;stopLoop(state);if(mode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING)removeRequests(event.entryId());}
            case PAUSE_ENDED->{state.paused=false;if(mode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING&&!state.completed)requests.add(event.entryId());}
            case SUBTITLE_CLEARED,SUBTITLE_REPLACED->{stopEntry(state);removeRequests(event.entryId());state.instantPlayed=false;state.last=-100;state.cjk=0;}
            case SUBTITLE_REMOVED->{stopEntry(state);removeRequests(event.entryId());states.remove(event.entryId());}
        }
    }
    public void flush(boolean muted){
        Completion completed;while((completed=completions.poll())!=null){State s=states.get(completed.id);if(s==null||completed.generation!=generation||completed.stateGeneration!=s.generation||completed.loop&&(s.paused||s.completed)){if(completed.handle!=null)backend.stop(completed.handle);continue;}
            if(completed.loop)s.loopPending=false;
            if(completed.handle==null)continue;
            // A bounded set also lets clear/replace stop one-shot clips that are still playing.
            if(s.playing.size()>=32){var iterator=s.playing.iterator();var oldest=iterator.next();backend.stop(oldest);iterator.remove();}s.playing.add(completed.handle);if(completed.loop)s.loop=completed.handle;}
        if(muted||!config.enabled||config.masterVolume<=0){requests.clear();if(!wasMuted)stopSounds();wasMuted=true;return;}
        if(wasMuted)for(var entry:states.entrySet()){var s=entry.getValue();if(s.profile.playMode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING&&!s.paused&&!s.completed)requests.add(entry.getKey());}wasMuted=false;
        for(long id:requests){State s=states.get(id);if(s!=null)play(id,s);}requests.clear();
    }
    private void play(long id,State state){
        if(state.profile.playMode==VoiceProfile.PlayMode.OFF||state.profile.volume<=0)return;
        if(state.profile.playMode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING&&(state.loop!=null||state.loopPending||state.paused||state.completed))return;
        List<String> ready=state.profile.audio.stream().filter(s->audio.ready(s)!=null).toList();String clipId=state.bag.next(ready,state.profile.avoidImmediateRepeat);if(clipId==null)return;
        var clip=audio.ready(clipId);double pitch=CcmlLimits.clamp(state.profile.pitch+(state.playRandom.nextDouble()*2-1)*state.profile.pitchRandomness,.5,2);
        boolean loop=state.profile.playMode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING;state.loopPending=loop;long epoch=generation,stateEpoch=state.generation;
        double volume=CcmlLimits.clamp(state.profile.volume+(state.playRandom.nextDouble()*2-1)*state.profile.volumeRandomness,0,1);
        backend.play(clip,new VoicePlaybackParams(config.masterVolume*volume,pitch,loop)).whenComplete((handle,error)->completions.add(new Completion(id,epoch,stateEpoch,loop,handle)));
    }
    private void stopLoop(State state){if(state.loopPending){state.generation++;state.loopPending=false;}if(state.loop!=null){backend.stop(state.loop);state.playing.remove(state.loop);state.loop=null;}}
    private void removeRequests(long id){requests.removeIf(value->value==id);}
    public void audioReloaded(){stopSounds();requests.clear();for(var entry:states.entrySet()){var state=entry.getValue();for(String clip:state.profile.audio)audio.load(clip);if(state.profile.playMode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING&&!state.paused&&!state.completed)requests.add(entry.getKey());}}
    private void stopEntry(State state){state.generation++;for(var handle:state.playing)backend.stop(handle);state.playing.clear();stopLoop(state);}
    public void stopSounds(){generation++;for(State s:states.values()){stopLoop(s);s.playing.clear();}backend.stopAll();}
    public void clear(){stopSounds();states.clear();requests.clear();}
    public void configure(VoiceConfig config){clear();this.config=config;}
    public void preview(VoiceProfile profile){if(profile!=null){State previous=states.remove(Long.MIN_VALUE);if(previous!=null)stopEntry(previous);State state=new State(profile,random);states.put(Long.MIN_VALUE,state);for(String clip:profile.audio)audio.load(clip);play(Long.MIN_VALUE,state);}}
}
