package io.github.axiraa.cinematicchat.fabric.mixin;
import io.github.axiraa.cinematicchat.minecraft.CinematicChatClient;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.BooleanSupplier;
/** Fabric's GAME event has no bound ChatType. Scope metadata around vanilla's delayed handler. */
@Mixin(ChatListener.class)
public abstract class ProfilelessChatContextMixin {
    @WrapOperation(method="handleDisguisedChatMessage",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/chat/ChatListener;handleMessage(Lnet/minecraft/network/chat/MessageSignature;Ljava/util/function/BooleanSupplier;)V"))
    private void cinematicChat$context(ChatListener listener,MessageSignature signature,BooleanSupplier handler,Operation<Void> original,@Local(argsOnly=true) ChatType.Bound bound){
        original.call(listener,signature,(BooleanSupplier)()->{var client=CinematicChatClient.get();if(client==null)return handler.getAsBoolean();
            try(var scope=client.capture().scope(bound)){return handler.getAsBoolean();}catch(RuntimeException error){throw error;}catch(Exception error){throw new IllegalStateException(error);}
        });
    }
}
