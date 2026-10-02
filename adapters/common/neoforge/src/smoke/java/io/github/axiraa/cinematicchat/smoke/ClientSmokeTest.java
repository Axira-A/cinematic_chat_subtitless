package io.github.axiraa.cinematicchat.smoke;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="cinematic_chat",value=Dist.CLIENT)
public final class ClientSmokeTest {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){ClientScenario.tick();}
}
