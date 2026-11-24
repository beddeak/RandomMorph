package kr.randommorph.fabric.mixin;

import kr.randommorph.fabric.RandomMorphMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerList.class, remap = false)
public abstract class PlayerRespawnMixin {
    // Before both spawn-space selection and replacement; AFTER_RESPAWN is too late for cleanup.
    @Inject(method = "respawn", at = @At("HEAD"))
    private void randommorph$beforeRespawn(ServerPlayer oldPlayer, boolean alive, Entity.RemovalReason reason,
                                         CallbackInfoReturnable<ServerPlayer> ci) {
        RandomMorphMod.manager(oldPlayer.level().getServer()).suspend(oldPlayer);
    }
}
