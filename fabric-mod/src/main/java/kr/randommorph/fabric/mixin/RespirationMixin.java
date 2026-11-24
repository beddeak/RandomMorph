package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.AbilityHooks;
import net.minecraft.tags.*;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LivingEntity.class,remap=false)
public abstract class RespirationMixin {
    @Inject(method="baseTick",at=@At("HEAD"))
    private void randommorph$hydrateBeforeBreathing(CallbackInfo ci){kr.randommorph.fabric.ability.WaterPhysiology.beforeBreathing((LivingEntity)(Object)this);}
    @Redirect(method="baseTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean randommorph$reverseMedium(LivingEntity entity,TagKey<Fluid> fluid){
        return fluid==FluidTags.WATER&&AbilityHooks.needsWater(entity)?!entity.isInWaterOrRain():entity.isEyeInFluid(fluid);
    }
    @Redirect(method="baseTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;canBreatheUnderwater()Z"))
    private boolean randommorph$gillsNeedWater(LivingEntity entity){return !AbilityHooks.needsWater(entity)&&entity.canBreatheUnderwater();}
    @Redirect(method="baseTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffectUtil;hasWaterBreathing(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean randommorph$noLandPotionOverride(LivingEntity entity){return !AbilityHooks.needsWater(entity)&&MobEffectUtil.hasWaterBreathing(entity);}
    @Inject(method="baseTick",at=@At("TAIL"))
    private void randommorph$wetRefill(CallbackInfo ci){
        var entity=(LivingEntity)(Object)this;
        if(!entity.level().isClientSide()&&entity.isAlive()&&AbilityHooks.needsWater(entity)&&entity.isInWaterOrRain())entity.setAirSupply(entity.getMaxAirSupply());
    }
}
