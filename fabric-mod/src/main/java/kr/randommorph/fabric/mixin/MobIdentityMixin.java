package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Mob.class,remap=false)
public abstract class MobIdentityMixin {
    @ModifyVariable(method="setTarget",at=@At("HEAD"),argsOnly=true)
    private LivingEntity randommorph$realTarget(LivingEntity target){return target==null?null:SpeciesAI.actual(target);}
}
