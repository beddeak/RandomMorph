package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.ability.AbilityEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Preserve native power/resistance/damage calculations while applying the filming server's grief setting. */
@Mixin(value={LargeFireball.class,WitherSkull.class},remap=false)
public abstract class ProjectileExplosionMixin {
    @Redirect(method="onHit",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void randommorph$explosion(Level level,Entity cause,double x,double y,double z,float power,boolean fire,Level.ExplosionInteraction interaction){
        var self=(Projectile)(Object)this;
        if(self.entityTags().contains(AbilityEngine.OWNED_TAG)&&self.getOwner() instanceof ServerPlayer player){
            var manager=RandomMorphMod.manager(player.level().getServer());
            if(!AbilityEngine.destruction(manager,player)){interaction=Level.ExplosionInteraction.NONE;fire=false;}
            else if(!manager.config().respectMobGriefing)interaction=Level.ExplosionInteraction.TNT;
        }
        level.explode(cause,x,y,z,power,fire,interaction);
    }
}
