package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.monster.Blaze;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=Blaze.class,remap=false)
public interface BlazeAccess {
    @Invoker("setCharged") void randommorph$charged(boolean value);
}
