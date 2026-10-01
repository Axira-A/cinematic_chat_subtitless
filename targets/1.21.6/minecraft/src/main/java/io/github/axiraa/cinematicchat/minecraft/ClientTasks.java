package io.github.axiraa.cinematicchat.minecraft;
import net.minecraft.client.Minecraft;
public final class ClientTasks {
    private ClientTasks(){}
    public static void defer(Runnable action){Minecraft.getInstance().schedule(action);}
}
