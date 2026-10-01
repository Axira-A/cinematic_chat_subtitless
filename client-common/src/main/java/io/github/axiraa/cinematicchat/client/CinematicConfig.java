package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.ChatSource;
import io.github.axiraa.cinematicchat.engine.*;
import java.util.*;

public final class CinematicConfig {
    public int configVersion = 2;
    public boolean enabled = true, showSender = true, shadow = true, backgroundEnabled = false;
    public boolean pauseWhenChatOpen = true, hideWhenChatOpen = true, reducedMotion = false;
    public boolean disableShake = false, disableWave = false, ccmlEnabled = true, debug = false;
    public String fontId = "minecraft:default";
    public double senderScale = .8, senderOpacity = .8, backgroundOpacity = .35, lineSpacing = 2;
    public TrustMode ccmlTrustMode = TrustMode.SAFE;
    public HudSettings hud = new HudSettings();
    public EngineConfig animation = EngineConfig.DEFAULT;
    public Animation.Easing entryEasing,exitEasing;
    public CcmlLimits ccmlLimits = CcmlLimits.ABSOLUTE;
    public Map<ChatSource, Boolean> messageTypes = new EnumMap<>(ChatSource.class);
    public boolean speakerFallbackEnabled = false;
    public String speakerDelimiter = ": ";
    public Map<String, SpeakerRule> serverSpeakerRules = new LinkedHashMap<>();
    public Map<ChatSource, SubtitleQueue.Priority> priorities = new EnumMap<>(ChatSource.class);
    public static final class SpeakerRule {
        public boolean enabled;
        public String prefix="", delimiter=": ";
        public void validate(){if(prefix==null||prefix.length()>64)prefix="";if(delimiter==null||delimiter.isEmpty()||delimiter.length()>16)delimiter=": ";}
    }
    public enum TrustMode { ALL, SAFE, SYSTEM_ONLY, OFF }
    public CinematicConfig() { for (ChatSource source : ChatSource.values()) {messageTypes.put(source, true);priorities.put(source,source==ChatSource.PLAYER_CHAT?SubtitleQueue.Priority.NORMAL:SubtitleQueue.Priority.SYSTEM);} }
    public void validate() {
        // Version 1's global switch becomes an editor default; it never grants all servers permission.
        configVersion = 2; if (hud == null) hud = new HudSettings(); hud.validate();
        animation = animation == null ? EngineConfig.DEFAULT : animation.bounded();
        ccmlLimits = ccmlLimits == null ? CcmlLimits.ABSOLUTE : ccmlLimits.bounded();
        ccmlTrustMode = ccmlTrustMode == null ? TrustMode.SAFE : ccmlTrustMode;
        if (messageTypes == null) messageTypes = new EnumMap<>(ChatSource.class);
        for (ChatSource source : ChatSource.values()) messageTypes.putIfAbsent(source, true);
        messageTypes.replaceAll((source,value)->value==null?true:value);
        if (fontId == null || !fontId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) fontId = "minecraft:default";
        senderScale = CcmlLimits.clamp(senderScale, .5, 2); senderOpacity = CcmlLimits.clamp(senderOpacity, 0, 1);
        backgroundOpacity = CcmlLimits.clamp(backgroundOpacity, 0, 1); lineSpacing = CcmlLimits.clamp(lineSpacing, 0, 20);
        if (speakerDelimiter == null || speakerDelimiter.isEmpty() || speakerDelimiter.length() > 16) speakerDelimiter = ": ";
        if(serverSpeakerRules==null)serverSpeakerRules=new LinkedHashMap<>();
        serverSpeakerRules.entrySet().removeIf(e->e.getKey()==null||e.getKey().isBlank()||e.getKey().length()>255||e.getValue()==null);
        while(serverSpeakerRules.size()>256)serverSpeakerRules.remove(serverSpeakerRules.keySet().iterator().next());
        serverSpeakerRules.values().forEach(SpeakerRule::validate);
        if(priorities==null)priorities=new EnumMap<>(ChatSource.class);
        for(ChatSource source:ChatSource.values())if(priorities.get(source)==null)priorities.put(source,source==ChatSource.PLAYER_CHAT?SubtitleQueue.Priority.NORMAL:SubtitleQueue.Priority.SYSTEM);
    }
    public SpeakerRule speakerRule(String server){return server==null?null:serverSpeakerRules.get(server);}
    public Animation.Easing effectiveEntryEasing(){return entryEasing==null?animation.easing():entryEasing;}
    public Animation.Easing effectiveExitEasing(){return exitEasing==null?animation.easing():exitEasing;}
    public SpeakerRule editSpeakerRule(String server){return serverSpeakerRules.computeIfAbsent(server,key->{var rule=new SpeakerRule();rule.delimiter=speakerDelimiter;return rule;});}
    public CcmlLimits effectiveLimits() {
        CcmlLimits a = ccmlLimits.bounded();
        if (ccmlTrustMode != TrustMode.SAFE) return a;
        return new CcmlLimits(a.inputLength(), a.nodes(), a.depth(), Math.min(3,a.pauseSeconds()), Math.min(10,a.holdSeconds()),
                Math.min(1.5,Math.max(.75,a.minScale())), Math.min(1.5,Math.max(.75,a.maxScale())), Math.min(1,a.shakePixels()), Math.min(2,a.wavePixels()), Math.min(60,a.scriptSeconds())).bounded();
    }
    public boolean parseMarkup(ChatSource source) {
        if (!ccmlEnabled || ccmlTrustMode == TrustMode.OFF) return false;
        return ccmlTrustMode != TrustMode.SYSTEM_ONLY || switch(source) { case SYSTEM,SAY,JOIN_LEAVE,DEATH,ADVANCEMENT -> true; default -> false; };
    }
}
