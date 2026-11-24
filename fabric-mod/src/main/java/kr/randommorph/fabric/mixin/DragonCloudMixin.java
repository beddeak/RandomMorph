package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=DragonFireball.class,remap=false)
public abstract class DragonCloudMixin {
    @Redirect(method="onHit",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean randommorph$cloud(Level level,Entity cloud){
        var self=(DragonFireball)(Object)this;
        if(self.getOwner() instanceof ServerPlayer player && ((MorphAccess)player).randommorph$form()!=null)RandomMorphMod.manager(player.level().getServer()).track(player,cloud,600);
        return level.addFreshEntity(cloud);
    }
}
