package kr.randommorph.fabric.mixin;
import java.util.*;
import java.util.function.Predicate;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Native typed sensors see native species; only untyped action targets are mapped back to players. */
@Mixin(value=NearestLivingEntitySensor.class,remap=false)
public abstract class BrainPerceptionMixin {
    @Redirect(method="doTick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<LivingEntity> randommorph$nativeCandidates(ServerLevel level,Class<LivingEntity> type,AABB area,Predicate<LivingEntity> filter,ServerLevel originalLevel,LivingEntity body){
        var originals=level.getEntitiesOfClass(type,area,filter);
        if(!SpeciesAI.enabled(level))return originals;
        var result=new ArrayList<LivingEntity>();
        for(var actual:originals){
            if(actual.getUUID().equals(body.getUUID())||actual.isSpectator()||actual instanceof net.minecraft.world.entity.player.Player p&&p.isCreative())continue;
            result.add(SpeciesAI.identity(actual));
        }
        return result;
    }
}
