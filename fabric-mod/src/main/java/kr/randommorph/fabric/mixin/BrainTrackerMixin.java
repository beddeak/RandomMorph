package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Typed perception stays native, but look/walk trackers follow the real moving player. */
@Mixin(value=EntityTracker.class,remap=false)
public abstract class BrainTrackerMixin {
    @ModifyVariable(method="<init>(Lnet/minecraft/world/entity/Entity;ZZ)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static Entity randommorph$trackedOwner(Entity target){return target instanceof LivingEntity living?SpeciesAI.actual(living):target;}
}
