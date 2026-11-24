package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Sheep.class,remap=false)
public interface SheepEatAccess {
    @Accessor("eatAnimationTick") void randommorph$eating(int ticks);
}
