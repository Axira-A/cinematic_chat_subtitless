package io.github.axiraa.cinematicchat.client;
import io.github.axiraa.cinematicchat.api.DecodedVoiceClip;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.sound.sampled.*;

public final class ExternalAudioManager implements AutoCloseable {
    @FunctionalInterface public interface OggDecoder { DecodedVoiceClip decode(String id,InputStream stream)throws IOException; }
    private final Path root;private final OggDecoder decoder;private final Consumer<String> warning;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Cinematic Chat audio decode");t.setDaemon(true);return t;});
    private final Map<String,CompletableFuture<DecodedVoiceClip>> cache=new HashMap<>();private long bytes,generation;
    public ExternalAudioManager(Path root,OggDecoder decoder,Consumer<String> warning){this.root=root.toAbsolutePath().normalize();this.decoder=decoder;this.warning=warning;}
    public Path resolve(String relative)throws IOException {
        if(Files.isSymbolicLink(root))throw new IOException("Audio directory cannot be a symbolic link");
        if(relative==null||relative.isBlank()||relative.contains("\\")||relative.contains(":"))throw new IOException("Only relative audio paths are allowed");
        try{if(Path.of(relative).isAbsolute())throw new IOException("Only relative audio paths are allowed");}catch(InvalidPathException error){throw new IOException("Invalid audio path",error);}
        Path path=root.resolve(relative).normalize();if(!path.startsWith(root)||Arrays.asList(relative.split("/")).contains(".."))throw new IOException("Audio path escapes directory");
        if(!Files.isRegularFile(path)||!path.toRealPath().startsWith(root.toRealPath()))throw new IOException("Missing or escaped audio file");
        if(Files.size(path)>8*1024*1024)throw new IOException("Audio file exceeds 8 MiB");return path;
    }
    public synchronized CompletableFuture<DecodedVoiceClip> load(String id) {
        if(!cache.containsKey(id)&&cache.size()>=512)return CompletableFuture.completedFuture(null);
        return cache.computeIfAbsent(id,key->{long epoch=generation;return CompletableFuture.supplyAsync(()->{
            try {Path path=resolve(key);DecodedVoiceClip clip;
                try(InputStream stream=Files.newInputStream(path)){if(key.toLowerCase(Locale.ROOT).endsWith(".ogg"))clip=decoder.decode(key,stream);else if(key.toLowerCase(Locale.ROOT).endsWith(".wav"))clip=wav(key,stream);else throw new IOException("Unsupported format");}
                int size=clip.pcm().remaining();if(size>16*1024*1024||clip.sampleRate()<=0||clip.channels()<1||clip.channels()>2||clip.bitsPerSample()!=16)throw new IOException("Unsupported or oversized decoded audio");
                synchronized(this){if(epoch!=generation)return null;if(bytes+size>64*1024*1024)throw new IOException("Audio cache exceeds 64 MiB");bytes+=size;}return clip;
            }catch(IOException|UnsupportedAudioFileException|RuntimeException ex){warning.accept("Voice clip "+key+": "+ex.getMessage());return null;}
        },worker);});
    }
    public DecodedVoiceClip ready(String id){var future=load(id);return future.isDone()?future.getNow(null):null;}
    public CompletableFuture<List<String>> list(){return CompletableFuture.supplyAsync(()->{try{Files.createDirectories(root);try(var paths=Files.walk(root)){return paths.filter(Files::isRegularFile).filter(p->{String n=p.toString().toLowerCase(Locale.ROOT);return n.endsWith(".ogg")||n.endsWith(".wav");}).filter(p->{try{return p.toRealPath().startsWith(root.toRealPath());}catch(IOException ex){return false;}}).map(p->root.relativize(p).toString().replace('\\','/')).sorted().toList();}}catch(IOException ex){warning.accept("Cannot scan audio: "+ex.getMessage());return List.of();}},worker);}
    public synchronized void reload(){generation++;cache.clear();bytes=0;}
    public synchronized long generation(){return generation;}
    private static DecodedVoiceClip wav(String id,InputStream input)throws IOException,UnsupportedAudioFileException {
        try(var stream=AudioSystem.getAudioInputStream(new BufferedInputStream(input))){var f=stream.getFormat();
            int bits=f.getSampleSizeInBits(),channels=f.getChannels();boolean signed=f.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
            if((!signed&&!f.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED))||!Set.of(8,16,24,32).contains(bits)||channels<1||channels>2||!Float.isFinite(f.getSampleRate())||f.getSampleRate()<1||f.getSampleRate()>384000)throw new IOException("WAV must be 8/16/24/32-bit integer PCM mono/stereo");
            int sampleBytes=bits/8,frameBytes=sampleBytes*channels;
            if(f.getFrameSize()!=frameBytes)throw new IOException("Unsupported WAV frame padding");
            int inputLimit=(16*1024*1024/2)*sampleBytes;
            byte[] data=stream.readNBytes(inputLimit+frameBytes);
            if(data.length>inputLimit||data.length%frameBytes!=0)throw new IOException("Oversized or truncated WAV data");
            ByteBuffer buffer=ByteBuffer.allocateDirect(data.length/sampleBytes*2).order(ByteOrder.LITTLE_ENDIAN);
            for(int offset=0;offset<data.length;offset+=sampleBytes){
                long sample=0;
                for(int part=0;part<sampleBytes;part++){int position=f.isBigEndian()?offset+sampleBytes-1-part:offset+part;sample|=(long)(data[position]&255)<<(8*part);}
                long midpoint=1L<<(bits-1);if(signed){if((sample&midpoint)!=0)sample-=1L<<bits;}else sample-=midpoint;
                buffer.putShort((short)(bits>=16?sample>>(bits-16):sample<<(16-bits)));
            }
            buffer.flip();return new DecodedVoiceClip(id,buffer,(int)f.getSampleRate(),channels,16);}
    }
    public void close(){reload();worker.shutdownNow();}
}
