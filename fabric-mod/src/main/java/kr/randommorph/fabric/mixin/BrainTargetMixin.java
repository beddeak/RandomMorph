package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Brain.class,remap=false)
public abstract class BrainTargetMixin {
    private static Object randommorph$resolve(Object value,MemoryModuleType<?> type){
        if(value instanceof LivingEntity entity&&(type==MemoryModuleType.ATTACK_TARGET||type==MemoryModuleType.AVOID_TARGET||type==MemoryModuleType.NEAREST_HOSTILE||type==MemoryModuleType.ROAR_TARGET))return SpeciesAI.actual(entity);
        return value;
    }
    @ModifyVariable(method="setMemoryInternal(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Object randommorph$target(Object value,MemoryModuleType<?> type,Object original){return randommorph$resolve(value,type);}
    @ModifyVariable(method="setMemoryInternal(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;J)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Object randommorph$timedTarget(Object value,MemoryModuleType<?> type,Object original,long expiry){return randommorph$resolve(value,type);}
}
