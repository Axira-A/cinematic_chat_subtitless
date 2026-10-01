package io.github.axiraa.cinematicchat.minecraft;
import io.github.axiraa.cinematicchat.api.StyledText;
import net.minecraft.network.chat.Component;

final class StyledComponentBridge {
    private StyledComponentBridge(){}
    static Component component(StyledText text,String fallbackFont){
        var result=Component.empty();
        int start=0;
        while(start<text.text().length()){
            var style=text.styleAt(start);int end=start+1;
            while(end<text.text().length()&&text.styleAt(end).equals(style))end++;
            result.append(Component.literal(text.text().substring(start,end)).setStyle(ComponentBridge.style(style,fallbackFont)));
            start=end;
        }
        return result;
    }
}
