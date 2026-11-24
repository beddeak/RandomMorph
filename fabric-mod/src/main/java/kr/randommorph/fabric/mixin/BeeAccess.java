package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=Bee.class,remap=false)
public interface BeeAccess {
    @Invoker("setHasStung") void randommorph$stung(boolean value);
}
