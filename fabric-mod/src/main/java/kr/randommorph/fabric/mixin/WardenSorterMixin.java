package kr.randommorph.fabric.mixin;
import com.llamalad7.mixinextras.expression.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="net.minecraft.world.entity.monster.warden.AngerManagement$Sorter",remap=false)
public abstract class WardenSorterMixin {
    private static boolean randommorph$human(Entity entity,boolean original){
        return entity instanceof MorphAccess access&&access.randommorph$form()!=null&&entity.level() instanceof ServerLevel level&&SpeciesAI.enabled(level)?false:original;
    }
    @Definition(id="Human",type=Player.class)
    @Expression("? instanceof Human")
    @ModifyExpressionValue(method="compare(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)I",at=@At(value="MIXINEXTRAS:EXPRESSION",ordinal=0))
    private boolean randommorph$firstHuman(boolean original,Entity first,Entity second){return randommorph$human(first,original);}
    @Definition(id="Human",type=Player.class)
    @Expression("? instanceof Human")
    @ModifyExpressionValue(method="compare(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)I",at=@At(value="MIXINEXTRAS:EXPRESSION",ordinal=1))
    private boolean randommorph$secondHuman(boolean original,Entity first,Entity second){return randommorph$human(second,original);}
}
