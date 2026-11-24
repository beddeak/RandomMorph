package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=TargetingConditions.class,remap=false)
public abstract class CombatIdentityMixin {
    @Shadow @Final private boolean isCombat;
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void randommorph$combatIdentity(ServerLevel level,LivingEntity attacker,LivingEntity target,CallbackInfoReturnable<Boolean> cir){
        if(isCombat && attacker instanceof Mob && target instanceof MorphAccess access && access.randommorph$form()!=null && SpeciesAI.enabled(level))
            cir.setReturnValue(SpeciesAI.test((TargetingConditions)(Object)this,level,attacker,target));
    }
}
