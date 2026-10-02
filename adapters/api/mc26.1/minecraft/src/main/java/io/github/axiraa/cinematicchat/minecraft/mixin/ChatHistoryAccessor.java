package io.github.axiraa.cinematicchat.minecraft.mixin;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
/** Read-only diagnostics and regression checks. Vanilla remains the history owner. */
@Mixin(ChatComponent.class) public interface ChatHistoryAccessor { @Accessor("allMessages") List<GuiMessage> cinematicChat$history(); }
