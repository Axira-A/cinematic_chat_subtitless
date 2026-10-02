package io.github.axiraa.cinematicchat.smoke;

import io.github.axiraa.cinematicchat.api.PlatformAudioBackend.VoiceHandle;
import io.github.axiraa.cinematicchat.minecraft.MinecraftAudioBackend;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.blaze3d.audio.Channel;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.lwjgl.openal.AL10;

/** Native observations exist only in the development scenario, never in a release JAR. */
public final class NativeSmokeProbes {
    public record AudioState(float gain,float pitch,int state){}
    private NativeSmokeProbes(){}
    public static boolean hasTooltip(net.minecraft.client.gui.components.AbstractWidget widget){
        try {
            // Up to 1.21.5 the widget exposes a getter; newer versions keep a holder.
            try{return widget.getClass().getMethod("getTooltip").invoke(widget)!=null;}
            catch(NoSuchMethodException newer){var holder=field(widget,"tooltip");return holder.getClass().getMethod("get").invoke(holder)!=null;}
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot inspect native tooltip",error);}
    }
    private static Object field(Object owner,String name)throws ReflectiveOperationException{
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static net.minecraft.client.sounds.ChannelAccess.ChannelHandle channel(VoiceHandle handle)throws ReflectiveOperationException{
        var accessor=handle.getClass().getDeclaredMethod("channel");accessor.setAccessible(true);
        return (net.minecraft.client.sounds.ChannelAccess.ChannelHandle)accessor.invoke(handle);
    }
    public static CompletableFuture<AudioState> audio(VoiceHandle handle)throws ReflectiveOperationException{
        var result=new CompletableFuture<AudioState>();
        channel(handle).execute(value->{try{
            int source=(Integer)field(value,"source");
            result.complete(new AudioState(AL10.alGetSourcef(source,AL10.AL_GAIN),AL10.alGetSourcef(source,AL10.AL_PITCH),AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE)));
        }catch(ReflectiveOperationException error){result.completeExceptionally(error);}});
        return result;
    }
    public static int activeVoices(MinecraftAudioBackend backend)throws ReflectiveOperationException{
        int count=0;for(Object handle:(Set<?>)field(backend,"handles"))if(!channel((VoiceHandle)handle).isStopped())count++;return count;
    }
    public static String input(ChatScreen screen)throws ReflectiveOperationException{return ((net.minecraft.client.gui.components.EditBox)field(screen,"input")).getValue();}
    @SuppressWarnings("unchecked") public static CompletableFuture<Suggestions> suggestions(ChatScreen screen)throws ReflectiveOperationException{
        return (CompletableFuture<Suggestions>)field(field(screen,"commandSuggestions"),"pendingSuggestions");
    }
    public static boolean suggestClick(ChatScreen screen,Style style)throws ReflectiveOperationException{
        for(Class<?> type=screen.getClass();type!=null;type=type.getSuperclass())for(Method method:type.getDeclaredMethods()){
            if(!method.getName().equals("handleComponentClicked"))continue;
            Class<?>[] args=method.getParameterTypes();if(args.length<1||args[0]!=Style.class)continue;
            method.setAccessible(true);return (Boolean)(args.length==1?method.invoke(screen,style):method.invoke(screen,style,false));
        }
        throw new NoSuchMethodException("Vanilla component click handler");
    }
}
