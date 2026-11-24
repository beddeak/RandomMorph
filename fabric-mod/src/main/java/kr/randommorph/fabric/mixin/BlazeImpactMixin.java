package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.ability.AbilityEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=SmallFireball.class,remap=false)
public abstract class BlazeImpactMixin {
    @Inject(method="onHitBlock",at=@At("HEAD"),cancellable=true)
    private void randommorph$fireRule(BlockHitResult hit,CallbackInfo ci){
        var self=(SmallFireball)(Object)this;
        if(self.entityTags().contains(AbilityEngine.OWNED_TAG)&&self.getOwner() instanceof ServerPlayer player && !AbilityEngine.destruction(RandomMorphMod.manager(player.level().getServer()),player))ci.cancel();
    }
    @Inject(method="onHitBlock",at=@At("RETURN"))
    private void randommorph$optionalExplosion(BlockHitResult hit,CallbackInfo ci){
        var self=(SmallFireball)(Object)this;
        if(self.entityTags().contains(AbilityEngine.OWNED_TAG)&&self.getOwner() instanceof ServerPlayer player){
            var manager=RandomMorphMod.manager(player.level().getServer());
            if(manager.config().blazeImpactExplosion){var pos=hit.getLocation();
                self.level().explode(self,pos.x,pos.y,pos.z,(float)manager.config().blazeExplosionPower,AbilityEngine.destruction(manager,player)?(manager.config().respectMobGriefing?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.TNT):Level.ExplosionInteraction.NONE);
            }
        }
    }
}
