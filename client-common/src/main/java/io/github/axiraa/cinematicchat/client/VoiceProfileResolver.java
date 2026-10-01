package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.*;
import java.util.*;
public final class VoiceProfileResolver {
    private final VoiceConfig config;
    public VoiceProfileResolver(VoiceConfig config){this.config=config;}
    public String normalize(String name){if(name==null)return "";String result=name;if(config.stripFormatting)result=result.replaceAll("§[0-9a-fk-orA-FK-OR]","");if(config.trimWhitespace)result=result.trim();if(config.caseInsensitive)result=result.toLowerCase(Locale.ROOT);return result;}
    public VoiceProfile byId(String id){return config.profiles.stream().filter(p->p.enabled&&Objects.equals(p.id,id)).findFirst().orElse(null);}
    public VoiceProfile resolve(SpeakerIdentity speaker){
        for(String field:config.matchOrder)for(var p:config.profiles)if(p.enabled) {
            boolean matches=switch(field){
                case "UUID"->speaker.uuid()!=null&&p.uuids.stream().anyMatch(s->s.equalsIgnoreCase(speaker.uuid().toString()));
                case "PROFILE"->speaker.profileName()!=null&&contains(p.names,speaker.profileName());
                case "DISPLAY"->speaker.displayName()!=null&&contains(p.names,speaker.displayName().text());
                case "ALIAS"->contains(p.aliases,speaker.profileName())||(speaker.displayName()!=null&&contains(p.aliases,speaker.displayName().text()));default->false;};
            if(matches)return p;
        }
        return byId(switch(speaker.source()){case PLAYER_CHAT,WHISPER->config.defaultPlayer;case SAY->config.defaultNpc;default->config.defaultSystem;});
    }
    private boolean contains(List<String> names,String name){return name!=null&&names.stream().anyMatch(s->normalize(s).equals(normalize(name)));}
}
