package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.engine.CcmlLimits;
import java.util.*;
public final class VoiceConfig {
    public int version=1;public boolean enabled=false,stripFormatting=true,caseInsensitive=true,trimWhitespace=true;
    public double masterVolume=1; public String defaultPlayer,defaultNpc,defaultSystem;
    public List<String> matchOrder=new ArrayList<>(List.of("UUID","PROFILE","DISPLAY","ALIAS"));
    public List<VoiceProfile> profiles=new ArrayList<>();
    public void validate(){version=1;masterVolume=CcmlLimits.clamp(masterVolume,0,1);if(profiles==null)profiles=new ArrayList<>();profiles.removeIf(Objects::isNull);
        Set<String> ids=new HashSet<>();for(var p:profiles){p.validate();if(!ids.add(p.id))p.enabled=false;}
        if(matchOrder==null)matchOrder=new ArrayList<>();matchOrder.removeIf(s->s==null||!Set.of("UUID","PROFILE","DISPLAY","ALIAS").contains(s));
        matchOrder=new ArrayList<>(new LinkedHashSet<>(matchOrder));for(String s:List.of("UUID","PROFILE","DISPLAY","ALIAS"))if(!matchOrder.contains(s))matchOrder.add(s);
    }
}
