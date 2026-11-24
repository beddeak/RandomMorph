package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Persistent anger retains a real owner, never a body discarded at the next form change. */
@Mixin(value=AngerManagement.class,remap=false)
public abstract class WardenAngerIdentityMixin {
    @ModifyVariable(method="increaseAnger",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Entity randommorph$angerOwner(Entity target){return target instanceof LivingEntity living?SpeciesAI.actual(living):target;}
}
