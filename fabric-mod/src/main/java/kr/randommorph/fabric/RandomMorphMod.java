package kr.randommorph.fabric;
import java.util.IdentityHashMap;
import java.util.Map;
import kr.randommorph.fabric.morph.MorphManager;
import kr.randommorph.fabric.network.MorphPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.LoggerFactory;

public final class RandomMorphMod implements ModInitializer {
    private static final Map<MinecraftServer,MorphManager> MANAGERS=new IdentityHashMap<>();
    public static MorphManager manager(MinecraftServer server) { return MANAGERS.computeIfAbsent(server,MorphManager::new); }
    @Override public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(MorphPayload.TYPE,MorphPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(kr.randommorph.fabric.network.SensePayload.TYPE,kr.randommorph.fabric.network.SensePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(kr.randommorph.fabric.network.InstinctPayload.TYPE,kr.randommorph.fabric.network.InstinctPayload.CODEC);
        kr.randommorph.fabric.ability.MorphWeapons.registerInteractionGuards();
        CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment) -> MorphCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> manager(server).tick());
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {kr.randommorph.fabric.ability.MorphWeapons.clear(handler.player);manager(server).sendSnapshot(handler.player);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server) -> manager(server).disconnected(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive) -> manager(newPlayer.level().getServer()).respawn(oldPlayer,newPlayer));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source) -> {
            if(entity instanceof ServerPlayer player) manager(player.level().getServer()).died(player);
        });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player,origin,destination)->manager(destination.getServer()).changedWorld(player));
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(entity.entityTags().contains(kr.randommorph.fabric.ability.AbilityEngine.OWNED_TAG)&&!manager(level.getServer()).owns(entity))entity.discard();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            var manager=MANAGERS.get(server);
            try{if(manager!=null)manager.shutdown();}finally{MANAGERS.remove(server);}
        });
        LoggerFactory.getLogger("RandomMorph").info("RandomMorph Fabric 0.7.0: 90 explicit forms, native traits/abilities and species AI bridge.");
    }
}
