package io.github.axiraa.cinematicchat.minecraft;

import io.github.axiraa.cinematicchat.api.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import java.lang.ref.WeakReference;
import java.util.*;

public final class ComponentBridge {
    private static final Map<String,FontDescription> FONTS=new HashMap<>();
    private static final Map<FontDescription,String> OPAQUE_KEYS=new WeakHashMap<>();
    private static final LinkedHashMap<String,WeakReference<FontDescription>> OPAQUE_FONTS=new LinkedHashMap<>();
    private static long nextFont;
    public static void invalidateFonts(){FONTS.clear();}
    private static String fontKey(FontDescription description){
        if(description.equals(FontDescription.DEFAULT))return null;
        if(description instanceof FontDescription.Resource resource)return resource.id().toString();
        String key=OPAQUE_KEYS.get(description);
        if(key==null||!OPAQUE_FONTS.containsKey(key)){
            key="cinematic_chat:component_font_"+(++nextFont);OPAQUE_KEYS.put(description,key);
            OPAQUE_FONTS.put(key,new WeakReference<>(description));
            if(OPAQUE_FONTS.size()>8192)OPAQUE_FONTS.remove(OPAQUE_FONTS.keySet().iterator().next());
        }
        return key;
    }
    private static FontDescription font(String id){
        var opaque=OPAQUE_FONTS.get(id);if(opaque!=null){var value=opaque.get();return value==null?FontDescription.DEFAULT:value;}
        if(FONTS.size()>128)FONTS.clear();
        return FONTS.computeIfAbsent(id,key->{
            var parsed=ResourceLocation.tryParse(key);if(parsed==null)return FontDescription.DEFAULT;
            var definition=ResourceLocation.fromNamespaceAndPath(parsed.getNamespace(),"font/"+parsed.getPath()+".json");
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(definition).isPresent()?new FontDescription.Resource(parsed):FontDescription.DEFAULT;
        });
    }
    public record Original(Component component,MessageSignature signature,net.minecraft.client.GuiMessageTag tag) implements OriginalMessageRef {}
    public static StyledText convert(Component component){
        StringBuilder text=new StringBuilder();List<StyledText.Run> runs=new ArrayList<>();
        component.visit((style,part)->{int start=text.length();text.append(part);runs.add(new StyledText.Run(start,text.length(),convert(style)));return Optional.empty();},Style.EMPTY);
        return new StyledText(text.toString(),runs);
    }
    public static TextStyle convert(Style style){return new TextStyle(style.getColor()==null?0xffffff:style.getColor().getValue(),fontKey(style.getFont()),style.isBold(),style.isItalic(),style.isUnderlined(),style.isStrikethrough(),style.isObfuscated(),1,1,0,12,0,2);}
    public static Style style(TextStyle style,String fallback){return Style.EMPTY.withColor(style.color()).withBold(style.bold()).withItalic(style.italic()).withUnderlined(style.underline()).withStrikethrough(style.strikethrough()).withObfuscated(style.obfuscated()).withFont(font(style.font()==null?fallback:style.font()));}
    private ComponentBridge(){}
}
