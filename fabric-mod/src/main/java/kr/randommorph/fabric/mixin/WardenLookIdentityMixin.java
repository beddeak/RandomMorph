package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=WardenAi.class,remap=false)
public abstract class WardenLookIdentityMixin {
    @ModifyVariable(method="isTarget",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static LivingEntity randommorph$lookOwner(LivingEntity target){return SpeciesAI.actual(target);}
}
