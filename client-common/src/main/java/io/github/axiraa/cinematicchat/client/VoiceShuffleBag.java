package io.github.axiraa.cinematicchat.client;
import java.util.*;
public final class VoiceShuffleBag {
    private final Random random;private final ArrayDeque<String> bag=new ArrayDeque<>();private String last;
    public VoiceShuffleBag(Random random){this.random=random;}
    public String next(List<String> clips,boolean avoidRepeat){
        if(clips.isEmpty())return null;
        if(bag.isEmpty()){var shuffled=new ArrayList<>(new LinkedHashSet<>(clips));Collections.shuffle(shuffled,random);
            if(avoidRepeat&&shuffled.size()>1&&Objects.equals(shuffled.getFirst(),last))Collections.swap(shuffled,0,1);bag.addAll(shuffled);}
        last=bag.removeFirst();return last;
    }
}
