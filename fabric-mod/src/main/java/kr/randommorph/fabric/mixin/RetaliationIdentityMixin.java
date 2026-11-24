package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=HurtByTargetGoal.class,remap=false)
public abstract class RetaliationIdentityMixin {
    // Only the excluded damage class uses a species identity. Eligibility/home/reach checks keep the owner.
    @Redirect(method="canUse",at=@At(value="INVOKE",target="Ljava/lang/Object;getClass()Ljava/lang/Class;"))
    private Class<?> randommorph$damageSpecies(Object target){
        return target instanceof LivingEntity living&&living.level() instanceof ServerLevel level&&SpeciesAI.enabled(level)
                ?SpeciesAI.identity(living).getClass():target.getClass();
    }
}
