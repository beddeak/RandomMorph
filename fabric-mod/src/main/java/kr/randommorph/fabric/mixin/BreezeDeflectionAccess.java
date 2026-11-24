package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Breeze.class,remap=false)
public interface BreezeDeflectionAccess {
    @Accessor("PROJECTILE_DEFLECTION") static ProjectileDeflection randommorph$deflection(){throw new AssertionError("Mixin missing");}
}
