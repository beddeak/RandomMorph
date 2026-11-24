package kr.randommorph.fabric.mixin;
import com.llamalad7.mixinextras.expression.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Change only native human-priority booleans; native anger thresholds, sounds and storage stay intact. */
@Mixin(value=Warden.class,remap=false)
public abstract class WardenPriorityMixin {
    private static boolean randommorph$human(Entity entity,boolean original){
        return entity instanceof MorphAccess access&&access.randommorph$form()!=null&&entity.level() instanceof ServerLevel level&&SpeciesAI.enabled(level)?false:original;
    }
    @Definition(id="Human",type=Player.class)
    @Expression("? instanceof Human")
    @ModifyExpressionValue(method="increaseAngerAt(Lnet/minecraft/world/entity/Entity;IZ)V",at=@At(value="MIXINEXTRAS:EXPRESSION",ordinal=0))
    private boolean randommorph$currentHuman(boolean original){return randommorph$human(((Warden)(Object)this).getTarget(),original);}
    @Definition(id="Human",type=Player.class)
    @Expression("? instanceof Human")
    @ModifyExpressionValue(method="increaseAngerAt(Lnet/minecraft/world/entity/Entity;IZ)V",at=@At(value="MIXINEXTRAS:EXPRESSION",ordinal=1))
    private boolean randommorph$sourceHuman(boolean original,Entity source,int amount,boolean sound){return randommorph$human(source,original);}
}
