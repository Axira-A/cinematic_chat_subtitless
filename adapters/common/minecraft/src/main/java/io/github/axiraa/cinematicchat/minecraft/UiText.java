package io.github.axiraa.cinematicchat.minecraft;

import io.github.axiraa.cinematicchat.client.*;
import io.github.axiraa.cinematicchat.engine.*;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Localized presentation of stable config values and generated previews. */
public final class UiText {
    public static Component state(boolean enabled){return Component.translatable(enabled?"options.on":"options.off");}
    public static Component value(Component label,Component value){return Component.translatable("cinematic_chat.value",label,value);}
    public static Component option(Enum<?> value){
        String family;
        if(value instanceof HudSettings.Anchor)family="anchor";
        else if(value instanceof RevealMode)family="reveal";
        else if(value instanceof SubtitleLayoutEngine.Alignment)family="alignment";
        else if(value instanceof Animation.Kind)family="animation";
        else if(value instanceof Animation.Easing)family="easing";
        else if(value instanceof CinematicConfig.TrustMode)family="trust";
        else if(value instanceof SubtitleQueue.Priority)family="priority";
        else throw new IllegalArgumentException("Unmapped UI option: "+value.getClass());
        return Component.translatable("cinematic_chat.option."+family+"."+value.name().toLowerCase(Locale.ROOT));
    }
    public static Component preset(String id,boolean userDefined){
        if(userDefined)return Component.literal(id);
        return switch(id){
            case "Cinematic"->Component.translatable("cinematic_chat.preset.cinematic");
            case "Minimal"->Component.translatable("cinematic_chat.preset.minimal");
            case "RPG"->Component.translatable("cinematic_chat.preset.rpg");
            case "Adventure"->Component.translatable("cinematic_chat.preset.adventure");
            case "Accessible"->Component.translatable("cinematic_chat.preset.accessible");
            case "Vanilla-like"->Component.translatable("cinematic_chat.preset.vanilla_like");
            default->Component.literal(id);
        };
    }
    public static Component diagnostic(CcmlDiagnostic diagnostic){return switch(diagnostic.code()){
        case LITERAL_MARKUP->Component.translatable("cinematic_chat.diagnostic.literal_markup",diagnostic.offset());
        case NODE_LIMIT->Component.translatable("cinematic_chat.diagnostic.node_limit");
        case PARSER_FALLBACK->Component.translatable("cinematic_chat.diagnostic.parser_fallback",diagnostic.detail());
    };}
    public static Component parser(SubtitleDocument document){
        var result=Component.translatable("cinematic_chat.command.parser",document.nodes().size(),document.diagnostics().size());
        for(var diagnostic:document.diagnostics())result.append("\n").append(diagnostic(diagnostic));
        return result;
    }
    public static String welcome(){return Component.translatable("cinematic_chat.preview.welcome").getString();}
    public static List<Component> lines(Component text,Font font,int width){
        List<Component> result=new ArrayList<>();
        for(var line:font.split(text,Math.max(80,width))){var plain=new StringBuilder();line.accept((i,style,codePoint)->{plain.appendCodePoint(codePoint);return true;});result.add(Component.literal(plain.toString()));}
        return result;
    }
    private UiText(){}
}
