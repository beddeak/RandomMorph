package kr.randommorph.fabric.client;
import kr.randommorph.fabric.network.MorphPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class RandomMorphClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(kr.randommorph.fabric.network.SensePayload.TYPE,(p,c)->c.client().execute(()->ClientSenses.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(kr.randommorph.fabric.network.InstinctPayload.TYPE,(p,c)->c.client().execute(()->ClientInstincts.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(MorphPayload.TYPE,(payload,context) -> context.client().execute(() -> ClientMorphs.receive(payload)));
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client) -> ClientMorphs.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> ClientMorphs.reset());
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientMorphs.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientMorphs.tickAnimations());
    }
}
