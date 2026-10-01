package io.github.axiraa.cinematicchat.minecraft;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
public final class PlayerProfileBridge {
    private PlayerProfileBridge(){}
    public static UUID id(GameProfile profile){return profile.getId();}
    public static String name(GameProfile profile){return profile.getName();}
}
