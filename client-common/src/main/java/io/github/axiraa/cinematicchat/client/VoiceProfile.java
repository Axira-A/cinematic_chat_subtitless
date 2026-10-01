package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.engine.CcmlLimits;
import java.util.*;
public final class VoiceProfile {
    public enum PlayMode { OFF,ON_SUBTITLE_START,PER_WORD,PER_REVEAL_UNIT,LOOP_WHILE_REVEALING }
    public String id="voice";
    public List<String> names=new ArrayList<>(),uuids=new ArrayList<>(),aliases=new ArrayList<>(),audio=new ArrayList<>();
    public double volume=.8,volumeRandomness=0,pitch=1,pitchRandomness=.05;
    public int intervalMs=65,intervalRandomnessMs=10,cjkCharactersPerVoice=2;
    public boolean avoidImmediateRepeat=true,enabled=true,voiceOnPunctuation=false;
    public PlayMode playMode=PlayMode.PER_REVEAL_UNIT;
    public void validate(){
        if(id==null||!id.matches("[a-z0-9_.-]{1,64}"))enabled=false;
        if(names==null)names=new ArrayList<>();if(uuids==null)uuids=new ArrayList<>();if(aliases==null)aliases=new ArrayList<>();if(audio==null)audio=new ArrayList<>();
        names.removeIf(Objects::isNull);uuids.removeIf(Objects::isNull);aliases.removeIf(Objects::isNull);audio.removeIf(Objects::isNull);
        volume=CcmlLimits.clamp(volume,0,1);volumeRandomness=CcmlLimits.clamp(volumeRandomness,0,.5);pitch=CcmlLimits.clamp(pitch,.5,2);pitchRandomness=CcmlLimits.clamp(pitchRandomness,0,.5);
        intervalMs=Math.clamp(intervalMs,30,2000);intervalRandomnessMs=Math.clamp(intervalRandomnessMs,0,intervalMs/2);cjkCharactersPerVoice=Math.clamp(cjkCharactersPerVoice,1,4);
        if(playMode==null)playMode=PlayMode.OFF;
    }
}
