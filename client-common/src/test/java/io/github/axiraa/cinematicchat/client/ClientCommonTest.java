package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ClientCommonTest {
    @TempDir Path directory;
    @Test void configSaveCancelAndCorruption()throws Exception{
        var warnings=new ArrayList<String>();try(var manager=new ConfigManager(directory,warnings::add)){
            var config=manager.load();var copy=manager.copy(config,CinematicConfig.class);copy.hud.scale=2;assertEquals(1,config.hud.scale);
            manager.save("cinematic_chat.json",copy).join();assertEquals(2,manager.load().hud.scale);
            Files.writeString(directory.resolve("cinematic_chat.json"),"broken");assertEquals(1,manager.load().hud.scale);assertFalse(warnings.isEmpty());
            try(var paths=Files.list(directory)){assertTrue(paths.anyMatch(p->p.getFileName().toString().contains("corrupt")));}
        }
    }
    @Test void backgroundReloadObservesEarlierAtomicSaves(){try(var manager=new ConfigManager(directory,s->{})){
        var config=new CinematicConfig();config.hud.scale=1.75;var saved=manager.save("cinematic_chat.json",config);var reload=manager.loadAsync();
        assertEquals(1.75,reload.join().hud.scale);assertTrue(saved.isDone());
        var voices=new VoiceConfig();voices.enabled=true;manager.save("voices.json",voices);assertTrue(manager.readAsync("voices.json",VoiceConfig.class,VoiceConfig::new).join().enabled);
    }}
    @Test void hudLockNeverSwitchesUntilShiftReleased(){var settings=new HudSettings();var editor=new HudEditorModel(settings);double x=settings.x(1000),y=settings.y(600);
        editor.begin(HudEditorModel.Operation.MOVE,x,y,1000,600);editor.drag(x,y,1000,600,true);editor.drag(x+10,y+1,1000,600,true);
        assertEquals(HudEditorModel.Axis.HORIZONTAL,editor.axis());editor.drag(x+11,y+100,1000,600,true);assertEquals(y,settings.y(600));
        editor.drag(x+11,y+100,1000,600,false);editor.drag(x+11,y+110,1000,600,false);assertEquals(y+10,settings.y(600),.01);
    }
    @Test void verticalLockThresholdAndShiftToggleDoNotJump(){var hud=new HudSettings();var e=new HudEditorModel(hud);double x=hud.x(1000),y=hud.y(600);
        e.begin(HudEditorModel.Operation.MOVE,x,y,1000,600);e.drag(x,y,1000,600,true);e.drag(x+2,y+3,1000,600,true);assertEquals(HudEditorModel.Axis.NONE,e.axis());assertEquals(x,hud.x(1000));assertEquals(y,hud.y(600));
        e.drag(x+1,y+8,1000,600,true);assertEquals(HudEditorModel.Axis.VERTICAL,e.axis());e.drag(x+100,y+9,1000,600,true);assertEquals(x,hud.x(1000));assertEquals(y+9,hud.y(600));
        e.drag(x+100,y+9,1000,600,false);assertEquals(x,hud.x(1000));assertEquals(y+9,hud.y(600));e.drag(x+110,y+20,1000,600,false);assertEquals(x+10,hud.x(1000));assertEquals(y+20,hud.y(600));
    }
    @Test void allNineAnchorsSnapAndResizeLeftKeepsScale(){for(var anchor:HudSettings.Anchor.values()){
        var hud=new HudSettings();hud.position(anchor.x*1000+2,anchor.y*600+2,1000,600);var editor=new HudEditorModel(hud);editor.begin(HudEditorModel.Operation.MOVE,hud.x(1000),hud.y(600),1000,600);editor.end(1000,600,false);assertEquals(anchor,hud.anchor);assertEquals(0,hud.offsetX);assertEquals(0,hud.offsetY);
    }
        var hud=new HudSettings();var editor=new HudEditorModel(hud);editor.begin(HudEditorModel.Operation.WIDTH_LEFT,300,200,1000,600);editor.drag(275,200,1000,600,false);assertEquals(.5,hud.widthRatio);assertEquals(1,hud.scale);
    }
    @Test void resizeAndScaleAreIndependentAndNormalized(){var h=new HudSettings();var e=new HudEditorModel(h);e.begin(HudEditorModel.Operation.WIDTH_RIGHT,500,400,1000,600);e.drag(550,400,1000,600,false);assertEquals(1,h.scale);assertEquals(.55,h.widthRatio,.001);
        assertEquals(.78,h.y(900)/900,.001);assertEquals(.5,h.x(2000)/2000,.001);}
    @Test void independentResetsAndEasingSurviveVisualPresetRoundTrip(){
        var c=new CinematicConfig();c.hud.position(100,100,1000,600);c.hud.scale=2;c.hud.widthRatio=.8;c.hud.resetPosition();assertEquals(.5,c.hud.x(1000)/1000);assertEquals(.78,c.hud.y(600)/600);assertEquals(2,c.hud.scale);assertEquals(.8,c.hud.widthRatio);
        c.hud.position(100,100,1000,600);c.hud.resetSize();assertEquals(100,c.hud.x(1000),1e-9);assertEquals(100,c.hud.y(600),1e-9);assertEquals(1,c.hud.scale);assertEquals(.45,c.hud.widthRatio);
        c.entryEasing=io.github.axiraa.cinematicchat.engine.Animation.Easing.EASE_IN;c.exitEasing=io.github.axiraa.cinematicchat.engine.Animation.Easing.LINEAR;var captured=HudPresets.Visual.capture(c);var restored=ConfigManager.JSON.fromJson(ConfigManager.JSON.toJson(captured),HudPresets.Visual.class);var next=new CinematicConfig();restored.apply(next);assertEquals(c.entryEasing,next.effectiveEntryEasing());assertEquals(c.exitEasing,next.effectiveExitEasing());
    }
    @Test void safeAreaEdgesSnapOnReleaseAndAltBypassesThem(){var h=new HudSettings();h.position(278,468,1000,600);var editor=new HudEditorModel(h);editor.extent(450,40);editor.begin(HudEditorModel.Operation.MOVE,278,468,1000,600);editor.end(1000,600,false);assertEquals(50,h.x(1000)-225);h.position(278,468,1000,600);editor.begin(HudEditorModel.Operation.MOVE,278,468,1000,600);editor.end(1000,600,true);assertEquals(53,h.x(1000)-225);}
    @Test void everyAspectRatioRetainsNormalizedCoordinatesAndAbsoluteWidth(){for(double[] size:List.of(new double[]{1920,1080},new double[]{1920,1200},new double[]{2560,1080},new double[]{3840,1080},new double[]{1600,1200},new double[]{853,479})){var h=new HudSettings();assertEquals(.5,h.x(size[0])/size[0],1e-9);assertEquals(.78,h.y(size[1])/size[1],1e-9);assertTrue(h.width(size[0])<=900);assertTrue(h.width(size[0])<=size[0]*.95);}}
    @Test void presetsOnlyChangeVisualSettingsAndDoNotShareMutableHud(){var config=new CinematicConfig();config.ccmlTrustMode=CinematicConfig.TrustMode.OFF;config.messageTypes.put(ChatSource.SYSTEM,false);var p=HudPresets.builtins().get("Accessible");p.apply(config);assertEquals(CinematicConfig.TrustMode.OFF,config.ccmlTrustMode);assertFalse(config.messageTypes.get(ChatSource.SYSTEM));config.hud.scale=3;var second=new CinematicConfig();p.apply(second);assertEquals(1.5,second.hud.scale);assertEquals(6,HudPresets.builtins().size());}
    @Test void profilesUseStructuredIdentityAndUuidPriority(){var cfg=new VoiceConfig();var byName=new VoiceProfile();byName.id="name";byName.names.add("Steve");var byUuid=new VoiceProfile();byUuid.id="uuid";UUID id=UUID.randomUUID();byUuid.uuids.add(id.toString());cfg.profiles.addAll(List.of(byName,byUuid));cfg.validate();
        var resolver=new VoiceProfileResolver(cfg);assertEquals("uuid",resolver.resolve(new SpeakerIdentity(ChatSource.PLAYER_CHAT,id,"Steve",StyledText.plain("§6Steve"))).id);
        assertEquals("name",resolver.resolve(new SpeakerIdentity(ChatSource.SAY,null,null,StyledText.plain(" §6Steve "))).id);assertNull(resolver.resolve(SpeakerIdentity.unknown(ChatSource.SYSTEM)));}
    @Test void shuffleNeverImmediatelyRepeatsAcrossBags(){var bag=new VoiceShuffleBag(new Random(2));String previous=null;Set<String> round=new HashSet<>();for(int i=0;i<100;i++){String next=bag.next(List.of("a","b","c"),true);assertNotEquals(previous,next);previous=next;round.add(next);if(i%3==2){assertEquals(3,round.size());round.clear();}}}
    @Test void audioPathsAreConfinedAndFailureIsCached()throws Exception {var warnings=new ArrayList<String>();Path audio=directory.resolve("audio");Files.createDirectories(audio);Files.writeString(audio.resolve("bad.ogg"),"invalid");
        try(var manager=new ExternalAudioManager(audio,(id,in)->{throw new java.io.IOException("invalid");},warnings::add)){
            for(String path:List.of("../escape.ogg","..\\escape.ogg","C:/test.ogg","/tmp/a.ogg","bad\u0000.ogg"))assertThrows(java.io.IOException.class,()->manager.resolve(path));
            assertNull(manager.load("bad.ogg").join());assertNull(manager.load("bad.ogg").join());assertEquals(1,warnings.size());manager.reload();assertNull(manager.load("bad.ogg").join());assertEquals(2,warnings.size());
        }
    }
    @Test void legacyGlobalFallbackCannotGrantOtherServersPermission(){var config=ConfigManager.JSON.fromJson("{\"configVersion\":1,\"speakerFallbackEnabled\":true,\"speakerDelimiter\":\"> \"}",CinematicConfig.class);config.validate();assertEquals(2,config.configVersion);assertNull(config.speakerRule("untrusted.example"));var rule=config.editSpeakerRule("trusted.example");assertFalse(rule.enabled);rule.enabled=true;rule.prefix="[NPC] ";assertNull(config.speakerRule("another.example"));assertEquals("> ",rule.delimiter);}
    @Test void futureSchemaIsBackedUpBeforeDefaultsAreUsed()throws Exception{Files.writeString(directory.resolve("cinematic_chat.json"),"{\"configVersion\":999,\"enabled\":false}");try(var manager=new ConfigManager(directory,s->{})){assertTrue(manager.load().enabled);assertFalse(Files.exists(directory.resolve("cinematic_chat.json")));try(var files=Files.list(directory)){assertEquals(1,files.count());}}}
    @Test void trustModesUseStructuredSourcesAndSafeCumulativeLimits(){
        var config=new CinematicConfig();config.validate();var limits=config.effectiveLimits();
        assertEquals(3,limits.pauseSeconds());assertEquals(10,limits.holdSeconds());assertEquals(.75,limits.minScale());assertEquals(1.5,limits.maxScale());
        assertEquals(1,limits.shakePixels());assertEquals(2,limits.wavePixels());assertEquals(60,limits.scriptSeconds());
        var parser=new io.github.axiraa.cinematicchat.engine.CcmlParser();
        var plan=new io.github.axiraa.cinematicchat.engine.SubtitleTimeline().compile(parser.parse(io.github.axiraa.cinematicchat.api.StyledText.plain("[pause=10]".repeat(100)+"余下纯文本"),limits,true),config.animation);
        assertEquals(60,plan.completeAt());var runtime=new io.github.axiraa.cinematicchat.engine.SubtitleRuntime(1,plan);runtime.advance(61,e->{});assertEquals("余下纯文本",runtime.snapshot().text());
        config.ccmlTrustMode=CinematicConfig.TrustMode.SYSTEM_ONLY;
        var system=java.util.EnumSet.of(ChatSource.SYSTEM,ChatSource.SAY,ChatSource.JOIN_LEAVE,ChatSource.DEATH,ChatSource.ADVANCEMENT);
        for(var source:ChatSource.values())assertEquals(system.contains(source),config.parseMarkup(source),source.name());
        assertFalse(config.parseMarkup(ChatSource.UNKNOWN));
        config.ccmlTrustMode=CinematicConfig.TrustMode.ALL;assertEquals(120,config.effectiveLimits().scriptSeconds());
        for(var source:ChatSource.values())assertTrue(config.parseMarkup(source));
        config.ccmlTrustMode=CinematicConfig.TrustMode.OFF;for(var source:ChatSource.values())assertFalse(config.parseMarkup(source));
    }
}
