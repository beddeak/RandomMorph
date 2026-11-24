package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=NearestAttackableTargetGoal.class,remap=false)
public abstract class NativeTargetMixin extends TargetGoal {
    @Shadow @Final protected Class<? extends LivingEntity> targetType;
    @Shadow @Final protected TargetingConditions targetConditions;
    @Shadow protected LivingEntity target;
    @Unique private String randommorph$acquiredForm;
    @Shadow protected abstract AABB getTargetSearchArea(double distance);
    protected NativeTargetMixin(Mob mob,boolean see){super(mob,see);}
    @Inject(method="findTarget",at=@At("HEAD"),cancellable=true)
    private void randommorph$speciesTargets(CallbackInfo ci){
        randommorph$acquiredForm=null;
        if(!(mob.level() instanceof ServerLevel level)||!SpeciesAI.enabled(level))return;
        var conditions=targetConditions.range(getFollowDistance());
        target=targetType==net.minecraft.world.entity.player.Player.class||targetType==net.minecraft.server.level.ServerPlayer.class
                ?SpeciesAI.nearestPlayer(level,mob,targetType,conditions)
                :SpeciesAI.nearest(level,mob,targetType,getTargetSearchArea(getFollowDistance()),conditions);
        randommorph$acquiredForm=target instanceof MorphAccess access?(access.randommorph$form()==null?"":access.randommorph$form().id()):null;
        ci.cancel();
    }
    // Added override: stop proactive species targeting after a form change. HurtByTargetGoal is untouched.
    @Override public boolean canContinueToUse(){
        var current=mob.getTarget();
        if(randommorph$acquiredForm!=null&&current instanceof MorphAccess access&&!randommorph$acquiredForm.equals(access.randommorph$form()==null?"":access.randommorph$form().id()))return false;
        if(current instanceof MorphAccess access && access.randommorph$form()!=null && mob.level() instanceof ServerLevel level && SpeciesAI.enabled(level)){
            if(!SpeciesAI.matches(targetType,current)||!SpeciesAI.test(targetConditions.copy().ignoreLineOfSight(),level,mob,current))return false;
        }
        return super.canContinueToUse();
    }
}
