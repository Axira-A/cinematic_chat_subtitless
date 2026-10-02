package io.github.axiraa.cinematicchat.smoke;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
public final class ScenarioPlatform {
    private ScenarioPlatform(){}
    public static void click(net.minecraft.client.gui.screens.Screen screen,double x,double y,int button){screen.mouseClicked(x,y,button);}
    public static void drag(net.minecraft.client.gui.screens.Screen screen,double x,double y,int button,double dx,double dy){screen.mouseDragged(x,y,button,dx,dy);}
    public static void release(net.minecraft.client.gui.screens.Screen screen,double x,double y,int button){screen.mouseReleased(x,y,button);}
    public static net.minecraft.client.gui.screens.ChatScreen chat(String input){return new net.minecraft.client.gui.screens.ChatScreen(input);}
    public static void createWorld(Minecraft mc,String name){mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings("Cinematic Chat test",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),WorldDataConfiguration.DEFAULT),new WorldOptions(4242,false,false),WorldPresets::createNormalWorldDimensions,null);}
    public static Component rich(){return Component.literal("Rich original").withStyle(Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand("/say hello")).withHoverEvent(new HoverEvent.ShowText(Component.literal("Hover retained"))));}
    public static void enter(Minecraft mc){mc.screen.keyPressed(257,0,0);}
    static void screenshot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}

    public static void resizeGui(Minecraft mc){mc.resizeDisplay();}
    public static void addSystemMessage(Minecraft mc,Component message){mc.gui.getChat().addMessage(message);}
    public static void addPlayerMessage(Minecraft mc,Component message,MessageSignature signature,net.minecraft.client.GuiMessageTag tag){mc.gui.getChat().addMessage(message,signature,tag);}
}
