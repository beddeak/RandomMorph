package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Entity.class,remap=false)
public abstract class IdentityTeamMixin {
    @Inject(method="getScoreboardName",at=@At("HEAD"),cancellable=true)
    private void randommorph$actualTeam(CallbackInfoReturnable<String> cir){
        if((Object)this instanceof LivingEntity e&&e.entityTags().contains(SpeciesAI.IDENTITY_TAG)){var actual=SpeciesAI.actual(e);if(actual!=e)cir.setReturnValue(actual.getScoreboardName());}
    }
}
