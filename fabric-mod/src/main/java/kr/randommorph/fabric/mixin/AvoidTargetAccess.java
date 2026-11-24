package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=AvoidEntityGoal.class,remap=false)
public interface AvoidTargetAccess {
    @Accessor("toAvoid") LivingEntity randommorph$avoided();
}
