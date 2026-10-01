package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
class VoiceRuntimeTest {
    @TempDir Path directory;
    static final class Backend implements PlatformAudioBackend {
        record Handle(int id) implements VoiceHandle{}
        final List<VoicePlaybackParams> plays=new ArrayList<>();int stops;
        public CompletableFuture<VoiceHandle> play(DecodedVoiceClip clip,VoicePlaybackParams params){plays.add(params);return CompletableFuture.completedFuture(new Handle(plays.size()));}
        public void stop(VoiceHandle h){stops++;}public void stopAll(){stops++;}public void reload(){}
    }
    private ExternalAudioManager audio()throws Exception{
        byte[] pcm=new byte[800];try(var input=new AudioInputStream(new ByteArrayInputStream(pcm),new AudioFormat(8000,16,1,true,false),400)){AudioSystem.write(input,AudioFileFormat.Type.WAVE,directory.resolve("clip.wav").toFile());}
        var manager=new ExternalAudioManager(directory,(id,in)->{throw new IOException();},s->{});assertNotNull(manager.load("clip.wav").join());return manager;
    }
    private VoiceConfig config(){var config=new VoiceConfig();config.enabled=true;var p=new VoiceProfile();p.id="test";p.audio.add("clip.wav");p.intervalRandomnessMs=0;p.pitchRandomness=0;config.profiles.add(p);config.validate();return config;}
    @Test void integerPcmWavConvertsSampleValuesToSigned16Bit()throws Exception{
        try(var manager=new ExternalAudioManager(directory,(id,in)->{throw new IOException();},s->{})){
            for(int bits:List.of(8,16,24,32)){
                int sampleBytes=bits/8;long midpoint=1L<<(bits-1);
                long[] samples=bits==8?new long[]{0,128,255}:new long[]{-midpoint,0,midpoint-1};
                var data=java.nio.ByteBuffer.allocate(samples.length*sampleBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                for(long sample:samples)for(int part=0;part<sampleBytes;part++)data.put((byte)(sample>>(8*part)));
                var format=new AudioFormat(22050,bits,1,bits!=8,false);String id="pcm-"+bits+".wav";
                try(var stream=new AudioInputStream(new ByteArrayInputStream(data.array()),format,samples.length)){AudioSystem.write(stream,AudioFileFormat.Type.WAVE,directory.resolve(id).toFile());}
                var clip=manager.load(id).join();assertNotNull(clip,id);assertEquals(16,clip.bitsPerSample());assertEquals(22050,clip.sampleRate());
                var decoded=clip.pcm().order(java.nio.ByteOrder.LITTLE_ENDIAN);assertEquals(-32768,decoded.getShort());assertEquals(0,decoded.getShort());assertEquals(bits==8?32512:32767,decoded.getShort());assertFalse(decoded.hasRemaining());
            }
        }
    }
    @Test void audioReloadDiscardsAnInFlightDecodeAndRetries()throws Exception{
        Files.writeString(directory.resolve("race.ogg"),"Ogg test fixture");var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var attempts=new java.util.concurrent.atomic.AtomicInteger();
        try(var manager=new ExternalAudioManager(directory,(id,in)->{
            if(attempts.incrementAndGet()==1){entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IOException("Test timeout");}catch(InterruptedException e){throw new IOException(e);}}
            return new DecodedVoiceClip(id,java.nio.ByteBuffer.allocateDirect(4),8000,1,16);
        },s->{})){
            var stale=manager.load("race.ogg");assertTrue(entered.await(5,TimeUnit.SECONDS));manager.reload();var current=manager.load("race.ogg");release.countDown();
            assertNull(stale.join());assertNotNull(current.join());assertEquals(2,attempts.get());assertSame(current,manager.load("race.ogg"));
        }finally{release.countDown();}
    }
    @Test void revealThrottleInstantMuteAndClear()throws Exception{
        try(var audio=audio()){var backend=new Backend();var cfg=config();cfg.profiles.getFirst().cjkCharactersPerVoice=1;var voice=new PseudoVoiceController(cfg,backend,audio,new Random(1));voice.bind(1,"test");
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,0,"你",false));voice.flush(false);assertEquals(1,backend.plays.size());
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,.03,"好",false));voice.flush(false);assertEquals(1,backend.plays.size());
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,.07,"！",false));voice.flush(false);assertEquals(1,backend.plays.size());
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,.08,"来",true));voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,.09,"了",true));voice.flush(false);assertEquals(2,backend.plays.size());
            voice.flush(false);voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.SUBTITLE_CLEARED,.1,"",false));assertTrue(backend.stops>=2);
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,.2,"字",false));voice.flush(true);assertEquals(2,backend.plays.size());
        }
    }
    @Test void voiceTriggersAndRandomParamsSurviveDifferentFlushRates()throws Exception{
        try(var audio=audio()){
            List<VoicePlaybackParams> expected=null;
            for(int fps:List.of(30,60,144,240)){
                var cfg=config();var profile=cfg.profiles.getFirst();profile.intervalMs=30;profile.intervalRandomnessMs=12;profile.pitchRandomness=.15;profile.volumeRandomness=.1;
                var backend=new Backend();var voice=new PseudoVoiceController(cfg,backend,audio,new Random(88));voice.bind(1,"test");
                var runtime=new SubtitleRuntime(1,new SubtitleTimeline().compile(new CcmlParser().parse("[speed=200]"+"abcdefghij".repeat(8)),EngineConfig.DEFAULT));
                for(int frame=0;frame<=fps;frame++){runtime.advance(frame/(double)fps,voice::event);voice.flush(false);}
                assertTrue(backend.plays.size()>10);
                if(expected==null)expected=List.copyOf(backend.plays);else assertEquals(expected,backend.plays,"Voice at "+fps+" FPS");
            }
            var cfg=config();var backend=new Backend();var voice=new PseudoVoiceController(cfg,backend,audio,new Random(5));voice.bind(1,"test");
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,0,"你",false));voice.flush(false);assertTrue(backend.plays.isEmpty());
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,.07,"好",false));voice.flush(false);assertEquals(1,backend.plays.size());
        }
    }
    @Test void profilesRemainPerEntryAndAudioFailureDoesNotBlock()throws Exception{
        try(var audio=audio()){var cfg=config();var p=new VoiceProfile();p.id="second";p.audio.add("clip.wav");p.volume=.3;p.pitch=1.5;p.pitchRandomness=0;cfg.profiles.add(p);cfg.validate();var backend=new Backend();var voice=new PseudoVoiceController(cfg,backend,audio,new Random(1));voice.bind(1,"test");voice.bind(2,"second");
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,0,"A",false));voice.event(new SubtitleRuntimeEvent(2,SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN,0,"B",false));voice.flush(false);assertEquals(2,backend.plays.size());assertEquals(.8,backend.plays.get(0).volume());assertEquals(.3,backend.plays.get(1).volume());assertEquals(1.5,backend.plays.get(1).pitch());}
    }
    @Test void allModesAndInstantCompletionUseActualRuntimeEvents()throws Exception{
        try(var audio=audio()){for(var mode:VoiceProfile.PlayMode.values()){
            var cfg=config();cfg.profiles.getFirst().playMode=mode;var backend=new Backend();var voice=new PseudoVoiceController(cfg,backend,audio,new Random(3));voice.bind(1,"test");
            var parser=new CcmlParser();var runtime=new SubtitleRuntime(1,new SubtitleTimeline().compile(parser.parse("[reveal=instant]你好 hello!"),EngineConfig.DEFAULT));runtime.advance(0,voice::event);voice.flush(false);
            assertEquals(mode==VoiceProfile.PlayMode.OFF||mode==VoiceProfile.PlayMode.LOOP_WHILE_REVEALING?0:1,backend.plays.size(),mode.name());
        }}
    }
    @Test void loopStopsDuringPauseAndResumesOnlyWhenRevealing()throws Exception{
        try(var audio=audio()){var cfg=config();cfg.profiles.getFirst().playMode=VoiceProfile.PlayMode.LOOP_WHILE_REVEALING;var backend=new Backend();var voice=new PseudoVoiceController(cfg,backend,audio,new Random(1));voice.bind(1,"test");
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.SUBTITLE_STARTED,0,"",false));voice.flush(false);voice.flush(false);assertEquals(1,backend.plays.size());assertTrue(backend.plays.getFirst().loop());
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.PAUSE_STARTED,.1,"",false));voice.flush(true);voice.flush(false);assertEquals(1,backend.plays.size());
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.PAUSE_ENDED,.3,"",false));voice.flush(false);assertEquals(2,backend.plays.size());voice.flush(false);
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.SUBTITLE_COMPLETED,.5,"",false));voice.flush(true);voice.flush(false);assertEquals(2,backend.plays.size());
        }
    }
    @Test void lateNativeCompletionIsStoppedAfterPauseAndClear()throws Exception{
        try(var audio=audio()){var cfg=config();cfg.profiles.getFirst().playMode=VoiceProfile.PlayMode.LOOP_WHILE_REVEALING;
            class Delayed implements PlatformAudioBackend {int stops;CompletableFuture<VoiceHandle> pending=new CompletableFuture<>();public CompletableFuture<VoiceHandle> play(DecodedVoiceClip c,VoicePlaybackParams p){return pending;}public void stop(VoiceHandle h){stops++;}public void stopAll(){}public void reload(){}}
            var backend=new Delayed();var voice=new PseudoVoiceController(cfg,backend,audio,new Random(1));voice.bind(1,"test");voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.SUBTITLE_STARTED,0,"",false));voice.flush(false);
            voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.PAUSE_STARTED,.1,"",false));backend.pending.complete(new Backend.Handle(1));voice.flush(false);assertEquals(1,backend.stops);
            backend.pending=new CompletableFuture<>();voice.event(new SubtitleRuntimeEvent(1,SubtitleRuntimeEvent.Kind.PAUSE_ENDED,.2,"",false));voice.flush(false);voice.clear();backend.pending.complete(new Backend.Handle(2));voice.flush(false);assertEquals(2,backend.stops);
        }
    }
    @Test void fallbackInferenceIsOptInPerServerAndOriginalEnvelopeRemainsIntact()throws Exception{
        try(var audio=audio()){var cfg=new CinematicConfig();cfg.speakerFallbackEnabled=true;var rule=cfg.editSpeakerRule("trusted");rule.enabled=true;rule.prefix="[NPC] ";cfg.validate();var controller=new SubtitleController(cfg,new VoiceConfig(),new Backend(),audio);OriginalMessageRef original=new OriginalMessageRef(){};
            var envelope=new ChatEnvelope(1,ChatSource.UNKNOWN,SpeakerIdentity.unknown(ChatSource.UNKNOWN),StyledText.plain("[NPC] Raan: hello"),original,null);controller.serverKey("other");controller.capture(envelope);controller.tick(1,new ClientState(true,false,false,false));assertFalse(controller.queue().visible().getFirst().pending().envelope().speaker().known());
            controller.clear();controller.serverKey("trusted");controller.capture(envelope);controller.tick(2,new ClientState(true,false,false,false));var captured=controller.queue().visible().getFirst().pending().envelope();assertEquals("Raan",captured.speaker().displayName().text());assertSame(original,captured.original());assertEquals(envelope.body(),captured.body());
        }
    }
}
