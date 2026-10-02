package io.github.axiraa.cinematicchat.minecraft;
import net.minecraft.client.Minecraft;
/** Queue after ChatScreen has completed its own input/close lifecycle. */
public final class ClientTasks {
    private ClientTasks(){}
    public static void defer(Runnable action){Minecraft.getInstance().tell(action);}
}
