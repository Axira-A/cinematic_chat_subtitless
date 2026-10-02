package io.github.axiraa.cinematicchat.minecraft;
import net.minecraft.client.KeyMapping;
public final class ClientKeyBindings {
    private ClientKeyBindings(){}
    public static KeyMapping create(String translationKey){return new KeyMapping(translationKey,-1,"key.categories.cinematic_chat");}
}
