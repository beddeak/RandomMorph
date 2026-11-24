package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Mob.class,remap=false)
public interface MobGoalsAccess {
    @Accessor("targetSelector") GoalSelector randommorph$targets();
}
