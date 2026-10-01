package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.minecraft.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import com.mojang.blaze3d.audio.*;
import javax.sound.sampled.AudioFormat;
import java.util.*;
import java.util.concurrent.*;
public final class MinecraftAudioBackend implements PlatformAudioBackend {
    private record Handle(net.minecraft.client.sounds.ChannelAccess.ChannelHandle channel,double volume) implements VoiceHandle{}
    private final Map<String,SoundBuffer> buffers=new HashMap<>();private final Set<Handle> handles=ConcurrentHashMap.newKeySet();private volatile long generation,plays;private volatile int bufferCount;
    private volatile double voiceGain=1;private double previousGain=Double.NaN;
    private SoundEngineAccessor engine(){return (SoundEngineAccessor)((SoundManagerAccessor)Minecraft.getInstance().getSoundManager()).cinematicChat$engine();}
    public CompletableFuture<VoiceHandle> play(DecodedVoiceClip clip,VoicePlaybackParams params){
        if(!engine().cinematicChat$loaded())return CompletableFuture.completedFuture(null);long epoch=generation;
        return engine().cinematicChat$channels().createHandle(Library.Pool.STATIC).thenApply(channel->{
            if(channel==null)return null;Handle handle=new Handle(channel,params.volume());handles.add(handle);
            channel.execute(ch->{if(epoch!=generation){ch.stop();return;}
                handles.removeIf(h->h.channel.isStopped());
                SoundBuffer buffer=buffers.computeIfAbsent(clip.id(),key->new SoundBuffer(clip.pcm(),new AudioFormat(clip.sampleRate(),clip.bitsPerSample(),clip.channels(),true,false)));
                ch.attachStaticBuffer(buffer);ch.setRelative(true);ch.disableAttenuation();ch.setPitch((float)params.pitch());
                ch.setVolume((float)(params.volume()*voiceGain));ch.setLooping(params.loop());ch.play();plays++;bufferCount=buffers.size();
            });return handle;
        });
    }
    public void tick(){
        double gain=Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.VOICE);voiceGain=gain;
        handles.removeIf(handle->handle.channel.isStopped());
        if(gain!=previousGain){previousGain=gain;for(Handle handle:handles)handle.channel.execute(channel->channel.setVolume((float)(handle.volume*voiceGain)));}
    }
    public void stop(VoiceHandle h){if(h instanceof Handle handle){handle.channel.execute(Channel::stop);handles.remove(handle);}}
    public void stopAll(){generation++;for(Handle h:handles)h.channel.execute(Channel::stop);handles.clear();}
    public void reload(){stopAll();engine().cinematicChat$channels().scheduleTick();engine().cinematicChat$executor().execute(()->{buffers.values().forEach(SoundBuffer::discardAlBuffer);buffers.clear();bufferCount=0;});}
    public void beforeDeviceClose(){stopAll();engine().cinematicChat$channels().scheduleTick();engine().cinematicChat$executor().executeBlocking(()->{buffers.values().forEach(SoundBuffer::discardAlBuffer);buffers.clear();bufferCount=0;});}
    public long plays(){return plays;} public int bufferCount(){return bufferCount;}
}
