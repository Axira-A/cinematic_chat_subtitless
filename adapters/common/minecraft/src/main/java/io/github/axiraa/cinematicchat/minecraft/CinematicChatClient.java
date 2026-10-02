package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.api.*;
import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.engine.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.sounds.JOrbisAudioStream;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.nio.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

public final class CinematicChatClient {
    private static CinematicChatClient instance;private static final Logger LOGGER=LogUtils.getLogger();
    private final ConfigManager configs;private CinematicConfig config;private VoiceConfig voices;private HudPresets presets;
    private final MinecraftAudioBackend backend=new MinecraftAudioBackend();private final ExternalAudioManager audio;
    private final SubtitleController controller;private final ChatCaptureBridge capture=new ChatCaptureBridge();private final SubtitleRenderer renderer=new SubtitleRenderer();
    private final AtomicLong previewIds=new AtomicLong(-1);private boolean wasInWorld,closed;private long configGeneration;
    public CinematicChatClient(){
        configs=new ConfigManager(Minecraft.getInstance().gameDirectory.toPath().resolve("config/cinematic_chat"),LOGGER::warn);
        config=new CinematicConfig();voices=new VoiceConfig();presets=new HudPresets();
        audio=new ExternalAudioManager(configs.directory().resolve("audio"),CinematicChatClient::decodeOgg,LOGGER::warn);
        controller=new SubtitleController(config,voices,backend,audio);instance=this;reload();
    }
    public static CinematicChatClient get(){return instance;}
    public SubtitleController controller(){return controller;}public ChatCaptureBridge capture(){return capture;}public ConfigManager configs(){return configs;}
    public CinematicConfig config(){return config;}public VoiceConfig voices(){return voices;}public ExternalAudioManager audio(){return audio;}
    public HudPresets presets(){return presets;}
    public long nextPreviewId(){return previewIds.getAndDecrement();}
    public MinecraftAudioBackend audioBackend(){return backend;}
    public String serverKey(){var server=Minecraft.getInstance().getCurrentServer();return server==null?"local":server.ip.toLowerCase(java.util.Locale.ROOT);}
    public java.util.concurrent.CompletableFuture<Void> installTestAudio(){return java.util.concurrent.CompletableFuture.runAsync(()->{
        try{
            var root=configs.directory().resolve("audio");java.nio.file.Files.createDirectories(root);
            if(java.nio.file.Files.isSymbolicLink(root))throw new IOException("Audio directory is a symbolic link");
            for(String name:java.util.List.of("test-blip.wav","test-blip-1.ogg","test-blip-2.ogg","test-blip-3.ogg")){
                try(var input=CinematicChatClient.class.getResourceAsStream("/assets/cinematic_chat/audio/"+name)){
                    if(input==null)throw new IOException("Bundled test audio missing: "+name);
                    var target=root.resolve("cinematic-"+name);if(!java.nio.file.Files.exists(target))java.nio.file.Files.copy(input,target);
                }
            }
        }catch(IOException error){throw new java.util.concurrent.CompletionException(error);}
    }).thenRunAsync(this::reloadAudio,Minecraft.getInstance()::execute);}
    public void applyPresets(HudPresets next){configGeneration++;next.validate();presets=next;configs.save("hud_presets.json",presets);}
    public void tick(){var mc=Minecraft.getInstance();backend.tick();boolean editor=mc.screen instanceof PreviewScreen;
        controller.serverKey(serverKey());
        controller.tick(System.nanoTime(),new ClientState(mc.level!=null,mc.screen instanceof ChatScreen||editor,mc.options.hideGui,mc.isPaused()));capture.tick();
        if(mc.level==null&&wasInWorld){backend.reload();audio.reload();}wasInWorld=mc.level!=null;}
    public void render(GuiCanvas graphics){var mc=Minecraft.getInstance();if(!config.enabled||mc.options.hideGui||mc.level==null||(config.hideWhenChatOpen&&mc.screen instanceof ChatScreen)||mc.screen instanceof PreviewScreen)return;renderer.render(graphics,controller);}
    public boolean hideVanilla(){var mc=Minecraft.getInstance();return config.enabled&&mc.level!=null&&!(mc.screen instanceof ChatScreen);}
    public void apply(CinematicConfig next){configGeneration++;next.validate();config=next;controller.configure(config,voices);configs.save("cinematic_chat.json",config);renderer.invalidate();}
    public void applyVoices(VoiceConfig next){configGeneration++;next.validate();voices=next;controller.configure(config,voices);configs.save("voices.json",voices);}
    public java.util.concurrent.CompletableFuture<Void> reload(){
        long epoch=++configGeneration;
        var settings=configs.loadAsync();var profiles=configs.readAsync("voices.json",VoiceConfig.class,VoiceConfig::new);var hud=configs.readAsync("hud_presets.json",HudPresets.class,HudPresets::new);
        return java.util.concurrent.CompletableFuture.allOf(settings,profiles,hud).thenRunAsync(()->{
            if(closed||epoch!=configGeneration)return;
            config=settings.join();voices=profiles.join();voices.validate();presets=hud.join();presets.validate();controller.configure(config,voices);reloadAudio();renderer.invalidate();
        },Minecraft.getInstance()::execute).exceptionally(error->{LOGGER.warn("Configuration reload failed",error);return null;});
    }
    public void reloadAudio(){backend.reload();audio.reload();controller.audioReloaded();}
    public void resourcesReloaded(){ComponentBridge.invalidateFonts();renderer.invalidate();reloadAudio();}
    public void beforeSoundDeviceClose(){backend.beforeDeviceClose();}
    public SubtitleController preview(CinematicConfig settings){return new SubtitleController(settings,voices,backend,audio);}
    public SubtitleController preview(CinematicConfig settings,VoiceConfig voiceSettings){return new SubtitleController(settings,voiceSettings,backend,audio);}
    public void sample(SubtitleController target){target.clear();for(String[] entry:new String[][]{{"Axira","北边的门打不开。"},{"Honma","那就从塔楼过去。"}}){long id=previewIds.getAndDecrement();var speaker=new SpeakerIdentity(ChatSource.PLAYER_CHAT,null,entry[0],StyledText.plain(entry[0]));target.capture(new ChatEnvelope(id,ChatSource.PLAYER_CHAT,speaker,StyledText.plain(entry[1]),null,null));}}
    public void drawPreview(GuiCanvas graphics,SubtitleController target){if(!Minecraft.getInstance().options.hideGui)renderer.render(graphics,target);}
    public SubtitleRenderer.Bounds previewBounds(SubtitleController target,int width,int height){return renderer.bounds(target,width,height);}
    public void shutdown(){closed=true;configGeneration++;controller.clear();backend.reload();audio.close();configs.close();}
    private static DecodedVoiceClip decodeOgg(String id,InputStream input)throws IOException{
        try(var stream=new JOrbisAudioStream(input)){var format=stream.getFormat();ByteArrayOutputStream pcm=new ByteArrayOutputStream();
            while(true){ByteBuffer part=stream.read(16384);if(!part.hasRemaining())break;if(pcm.size()+part.remaining()>16*1024*1024)throw new IOException("Decoded clip exceeds 16 MiB");byte[] block=new byte[part.remaining()];part.get(block);pcm.write(block);}
            byte[] data=pcm.toByteArray();ByteBuffer buffer=ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());buffer.put(data).flip();return new DecodedVoiceClip(id,buffer,(int)format.getSampleRate(),format.getChannels(),format.getSampleSizeInBits());}
    }
}
