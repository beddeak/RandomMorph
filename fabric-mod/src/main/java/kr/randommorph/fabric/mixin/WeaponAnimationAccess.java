package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=LivingEntity.class,remap=false)
public interface WeaponAnimationAccess {
    @Accessor("useItemRemaining") void randommorph$useRemaining(int value);
}
