package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.api.*;
import net.minecraft.network.chat.*;
import java.util.*;
public final class ComponentBridge {
    private static final Map<String,net.minecraft.resources.ResourceLocation> FONTS=new HashMap<>();
    public static void invalidateFonts(){FONTS.clear();}
    private static net.minecraft.resources.ResourceLocation font(String id){if(FONTS.size()>128)FONTS.clear();return FONTS.computeIfAbsent(id,key->{var parsed=net.minecraft.resources.ResourceLocation.tryParse(key);if(parsed==null)return Style.DEFAULT_FONT;var definition=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parsed.getNamespace(),"font/"+parsed.getPath()+".json");return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(definition).isPresent()?parsed:Style.DEFAULT_FONT;});}
    public record Original(Component component, MessageSignature signature, net.minecraft.client.GuiMessageTag tag) implements OriginalMessageRef {}
    public static StyledText convert(Component component) {
        StringBuilder text=new StringBuilder();List<StyledText.Run> runs=new ArrayList<>();
        component.visit((style,part)->{int start=text.length();text.append(part);runs.add(new StyledText.Run(start,text.length(),convert(style)));return Optional.empty();},Style.EMPTY);
        return new StyledText(text.toString(),runs);
    }
    public static TextStyle convert(Style style){return new TextStyle(style.getColor()==null?0xffffff:style.getColor().getValue(),style.getFont().equals(Style.DEFAULT_FONT)?null:style.getFont().toString(),style.isBold(),style.isItalic(),style.isUnderlined(),style.isStrikethrough(),style.isObfuscated(),1,1,0,12,0,2);}
    public static Style style(TextStyle s,String fallback){return Style.EMPTY.withColor(s.color()).withBold(s.bold()).withItalic(s.italic()).withUnderlined(s.underline()).withStrikethrough(s.strikethrough()).withObfuscated(s.obfuscated()).withFont(font(s.font()==null?fallback:s.font()));}
    private ComponentBridge(){}
}
