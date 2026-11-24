package kr.randommorph.fabric.mixin;

import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.frog.Frog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The native food tag alone is insufficient: cube prey must also have native size one. */
@Mixin(value=Frog.class,remap=false)
public abstract class FrogFoodMixin {
    @ModifyVariable(method="canEat",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static LivingEntity randommorph$foodIdentity(LivingEntity target){
        return target instanceof MorphAccess access&&access.randommorph$form()!=null
                &&target.level() instanceof ServerLevel level&&SpeciesAI.enabled(level)
                ?SpeciesAI.identity(target):target;
    }
}
