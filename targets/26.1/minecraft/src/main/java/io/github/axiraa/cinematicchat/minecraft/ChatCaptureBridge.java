package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class ChatCaptureBridge {
    private record Context(ChatSource source,SpeakerIdentity speaker,Component body){}
    private final Map<Component,Context> contexts=new IdentityHashMap<>();private final AtomicLong ids=new AtomicLong();
    private final Deque<ChatType.Bound> scopedTypes=new ArrayDeque<>();
    public synchronized AutoCloseable scope(ChatType.Bound bound){scopedTypes.push(bound);return ()->{synchronized(this){scopedTypes.pop();}};}
    public synchronized void context(Component displayed,ChatType.Bound bound,UUID sender,PlayerChatMessage signed,boolean overlay){
        if(overlay)return;
        ChatSource source=classify(displayed);if(source==ChatSource.UNKNOWN)source=ChatSource.SYSTEM;SpeakerIdentity identity=SpeakerIdentity.unknown(source);Component body=null;
        if(bound!=null){ResourceKey<ChatType> key=bound.chatType().unwrapKey().orElse(null);
            source=ChatType.SAY_COMMAND.equals(key)?ChatSource.SAY:(ChatType.MSG_COMMAND_INCOMING.equals(key)||ChatType.MSG_COMMAND_OUTGOING.equals(key)?ChatSource.WHISPER:ChatSource.PLAYER_CHAT);
            var info=sender==null?null:Minecraft.getInstance().getConnection()==null?null:Minecraft.getInstance().getConnection().getPlayerInfo(sender);
            String profile=info==null?null:PlayerProfileBridge.name(info.getProfile());UUID uuid=sender==null||sender.equals(new UUID(0,0))?null:sender;
            identity=new SpeakerIdentity(source,uuid,profile,ComponentBridge.convert(bound.name()));
            // Only use a body when vanilla's filtering and decoration produce the exact captured display.
            if(signed!=null&&signed.filterMask().isEmpty()&&bound.decorate(signed.decoratedContent()).equals(displayed))body=signed.decoratedContent();
            if(body==null&&displayed.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents){var args=contents.getArgs();if(args.length>=2&&args[1] instanceof Component candidate&&bound.decorate(candidate).equals(displayed))body=candidate;}
        }
        if(contexts.size()>=200)contexts.clear();contexts.put(displayed,new Context(source,identity,body));
    }
    public synchronized void capture(Component component,MessageSignature signature,net.minecraft.client.multiplayer.chat.GuiMessageTag tag,net.minecraft.client.multiplayer.chat.GuiMessageSource messageSource){
        var client=CinematicChatClient.get();if(client==null)return;
        Context context=contexts.remove(component);
        if(!scopedTypes.isEmpty()&&(context==null||context.source==ChatSource.SYSTEM)){context(component,scopedTypes.peek(),null,null,false);context=contexts.remove(component);}
        ChatSource source=context==null?classify(component):context.source;
        if(context==null&&source==ChatSource.UNKNOWN)source=messageSource==net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER?ChatSource.PLAYER_CHAT:ChatSource.SYSTEM;
        SpeakerIdentity identity=context==null?SpeakerIdentity.unknown(source):context.speaker;
        Component body=context!=null&&context.body!=null?context.body:component;
        client.controller().capture(new ChatEnvelope(ids.incrementAndGet(),source,identity,ComponentBridge.convert(body),new ComponentBridge.Original(component,signature,tag),signature==null?null:signature.toString()));
    }
    private ChatSource classify(Component component){
        if(component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t){String key=t.getKey();
            if(key.startsWith("multiplayer.player.joined")||key.startsWith("multiplayer.player.left"))return ChatSource.JOIN_LEAVE;
            if(key.startsWith("death."))return ChatSource.DEATH;if(key.startsWith("chat.type.advancement"))return ChatSource.ADVANCEMENT;
        }return ChatSource.UNKNOWN;
    }
    public synchronized void tick(){contexts.clear();}
}
