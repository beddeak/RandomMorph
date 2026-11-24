package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AvoidEntityGoal.class,remap=false)
public abstract class NativeAvoidMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow @Final protected Class<? extends LivingEntity> avoidClass;
    @Shadow @Final protected float maxDist;
    @Shadow @Final protected PathNavigation pathNav;
    @Shadow @Final private TargetingConditions avoidEntityTargeting;
    @Shadow protected LivingEntity toAvoid;
    @Shadow protected Path path;
    @Unique private String randommorph$avoidedForm;
    @Unique private boolean randommorph$cancelFearPath;
    @Inject(method="canUse",at=@At("HEAD"),cancellable=true)
    private void randommorph$speciesFear(CallbackInfoReturnable<Boolean> cir){
        randommorph$avoidedForm=null;
        randommorph$cancelFearPath=false;
        // A native proxy preserves typed T fields in Spider/Armadillo and other derived avoid goals.
        if(!(mob.level() instanceof ServerLevel level)||!SpeciesAI.enabled(level))return;
        var candidate=SpeciesAI.nearest(level,mob,avoidClass,mob.getBoundingBox().inflate(maxDist,3,maxDist),avoidEntityTargeting);
        randommorph$avoidedForm=candidate instanceof MorphAccess access?(access.randommorph$form()==null?"":access.randommorph$form().id()):null;
        toAvoid=candidate==null?null:SpeciesAI.identity(candidate);
        if(toAvoid==null){cir.setReturnValue(false);return;}
        var away=DefaultRandomPos.getPosAway(mob,16,7,toAvoid.position());
        if(away==null||toAvoid.distanceToSqr(away)<toAvoid.distanceToSqr(mob)){cir.setReturnValue(false);return;}
        path=pathNav.createPath(away.x,away.y,away.z,0);cir.setReturnValue(path!=null);
    }
    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void randommorph$stopFear(CallbackInfoReturnable<Boolean> cir){
        if(randommorph$avoidedForm==null)return;
        if(toAvoid==null||!(mob.level() instanceof ServerLevel level)){randommorph$cancelFearPath=true;cir.setReturnValue(false);return;}
        if(!SpeciesAI.enabled(level)){
            if(randommorph$avoidedForm.isEmpty()){randommorph$avoidedForm=null;return;}
            randommorph$cancelFearPath=true;cir.setReturnValue(false);return;
        }
        var current=SpeciesAI.refreshAvoided(level,toAvoid,randommorph$avoidedForm,avoidClass);
        if(current==null){randommorph$cancelFearPath=true;cir.setReturnValue(false);return;}
        toAvoid=current;
    }
    @Inject(method="stop",at=@At("HEAD"))
    private void randommorph$clearFearPath(CallbackInfo ci){
        // Native stop only clears toAvoid and leaves navigation running towards the old threat.
        boolean stop=randommorph$avoidedForm!=null&&(!randommorph$avoidedForm.isEmpty()||randommorph$cancelFearPath);
        randommorph$avoidedForm=null;randommorph$cancelFearPath=false;
        if(stop)pathNav.stop();
    }
}
