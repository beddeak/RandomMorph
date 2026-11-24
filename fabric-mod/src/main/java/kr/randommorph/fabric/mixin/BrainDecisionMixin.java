package kr.randommorph.fabric.mixin;
import java.util.Optional;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Keep native decision equality consistent with the real-player ATTACK_TARGET memory. */
@Mixin(value={PiglinAi.class,PiglinBruteAi.class},remap=false)
public abstract class BrainDecisionMixin {
    @Inject(method="findNearestValidAttackTarget",at=@At("RETURN"),cancellable=true)
    private static void randommorph$resolveDecision(CallbackInfoReturnable<Optional<? extends LivingEntity>> cir){cir.setReturnValue(cir.getReturnValue().map(SpeciesAI::actual));}
}
