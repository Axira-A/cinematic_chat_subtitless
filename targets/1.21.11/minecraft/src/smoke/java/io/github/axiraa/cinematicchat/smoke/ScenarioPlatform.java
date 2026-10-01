package io.github.axiraa.cinematicchat.smoke;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
public final class ScenarioPlatform {
    private ScenarioPlatform(){}
    private static net.minecraft.client.input.MouseButtonEvent mouse(double x,double y,int button){return new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));}
    public static void click(net.minecraft.client.gui.screens.Screen screen,double x,double y,int button){screen.mouseClicked(mouse(x,y,button),false);}
    public static void drag(net.minecraft.client.gui.screens.Screen screen,double x,double y,int button,double dx,double dy){screen.mouseDragged(mouse(x,y,button),dx,dy);}
    public static void release(net.minecraft.client.gui.screens.Screen screen,double x,double y,int button){screen.mouseReleased(mouse(x,y,button));}
    public static net.minecraft.client.gui.screens.ChatScreen chat(String input){return new net.minecraft.client.gui.screens.ChatScreen(input,false);}
    public static void createWorld(Minecraft mc,String name){mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings("Cinematic Chat test",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new net.minecraft.world.level.gamerules.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),WorldDataConfiguration.DEFAULT),new WorldOptions(4242,false,false),WorldPresets::createNormalWorldDimensions,null);}
    public static Component rich(){return Component.literal("Rich original").withStyle(Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand("/say hello")).withHoverEvent(new HoverEvent.ShowText(Component.literal("Hover retained"))));}
    public static void enter(Minecraft mc){mc.screen.keyPressed(new net.minecraft.client.input.KeyEvent(257,0,0));}
    static void screenshot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,c->{});}
}
