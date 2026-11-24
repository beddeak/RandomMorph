package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Wolf.class,remap=false)
public abstract class WolfOwnerIdentityMixin {
    @Inject(method="wantsToAttack",at=@At("HEAD"),cancellable=true)
    private void randommorph$ownerPvp(LivingEntity target,LivingEntity owner,CallbackInfoReturnable<Boolean> cir){
        var actual=SpeciesAI.actual(target);if(actual instanceof Player p&&owner instanceof Player o&&!o.canHarmPlayer(p))cir.setReturnValue(false);
    }
    @ModifyVariable(method="wantsToAttack",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private LivingEntity randommorph$ownerTarget(LivingEntity target){return target.level() instanceof ServerLevel level&&SpeciesAI.enabled(level)?SpeciesAI.identity(target):target;}
}
