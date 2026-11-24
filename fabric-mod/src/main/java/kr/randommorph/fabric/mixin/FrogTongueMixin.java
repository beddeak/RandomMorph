package kr.randommorph.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.frog.ShootTongue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep native tongue timing/damage while delivering its impulse to the real player's client. */
@Mixin(value=ShootTongue.class,remap=false)
public abstract class FrogTongueMixin {
    @Redirect(method="tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/frog/Frog;J)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void randommorph$tonguePull(LivingEntity target,Vec3 velocity){
        target.setDeltaMovement(velocity);
        // ServerEntity sends this velocity to tracking players AND the controlled player.
        if(target instanceof ServerPlayer)target.syncVelocity=true;
    }

    @Redirect(method="eatEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void randommorph$keepPlayerDeathLifecycle(Entity target,Entity.RemovalReason reason){
        // Native damage already decided death; respawn/disconnect owns removal of player entities.
        if(!(target instanceof Player))target.remove(reason);
    }
}
