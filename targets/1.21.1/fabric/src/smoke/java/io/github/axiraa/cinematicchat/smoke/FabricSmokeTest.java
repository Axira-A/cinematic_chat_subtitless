package io.github.axiraa.cinematicchat.smoke;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public final class FabricSmokeTest implements ClientModInitializer {
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->ClientScenario.tick());}
}
