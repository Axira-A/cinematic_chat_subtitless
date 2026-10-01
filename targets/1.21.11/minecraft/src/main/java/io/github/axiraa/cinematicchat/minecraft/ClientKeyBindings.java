package io.github.axiraa.cinematicchat.minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
public final class ClientKeyBindings {
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("cinematic_chat","controls"));
    private ClientKeyBindings(){}
    public static KeyMapping create(String translationKey){return new KeyMapping(translationKey,-1,CATEGORY);}
}
