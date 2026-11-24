package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import static kr.randommorph.fabric.ability.Trait.*;

@Mixin(value=LivingEntity.class,remap=false)
public abstract class LivingTraitsMixin {
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void randommorph$noBorrowedDrop(net.minecraft.world.item.ItemStack item,boolean scatter,net.minecraft.util.Prediction prediction,CallbackInfoReturnable<net.minecraft.world.entity.item.ItemEntity> cir){if(MorphWeapons.borrowed(item))cir.setReturnValue(null);}

    @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
    private void randommorph$nativeEffectRules(net.minecraft.world.effect.MobEffectInstance effect,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof net.minecraft.server.level.ServerPlayer player && AbilityHooks.profile(player)!=null)
            cir.setReturnValue(kr.randommorph.fabric.RandomMorphMod.manager(player.level().getServer()).identity(player).canBeAffected(effect));
    }
    @Inject(method="canAttack",at=@At("HEAD"),cancellable=true)
    private void randommorph$ownedSummon(LivingEntity target,CallbackInfoReturnable<Boolean> cir){
        var entity=(LivingEntity)(Object)this;
        if(entity.entityTags().contains(AbilityEngine.OWNED_TAG)&&entity.level() instanceof ServerLevel level){
            var owner=kr.randommorph.fabric.RandomMorphMod.manager(level.getServer()).owner(entity);
            if(owner!=null&&(owner.getUUID().equals(target.getUUID())||owner.isAlliedTo(target)||target instanceof net.minecraft.world.entity.player.Player player&&!owner.canHarmPlayer(player)))cir.setReturnValue(false);
        }
    }
    @Inject(method="canBreatheUnderwater",at=@At("HEAD"),cancellable=true)
    private void randommorph$gills(CallbackInfoReturnable<Boolean> cir){if(AbilityHooks.has((Entity)(Object)this,WATER_BREATHING))cir.setReturnValue(true);}
    @Inject(method="isSensitiveToWater",at=@At("HEAD"),cancellable=true)
    private void randommorph$waterWeak(CallbackInfoReturnable<Boolean> cir){if(AbilityHooks.has((Entity)(Object)this,WATER_WEAK))cir.setReturnValue(true);}
    @Inject(method="onClimbable",at=@At("HEAD"),cancellable=true)
    private void randommorph$climb(CallbackInfoReturnable<Boolean> cir){if(AbilityHooks.has((Entity)(Object)this,WALL_CLIMB)&&((LivingEntity)(Object)this).horizontalCollision)cir.setReturnValue(true);}
    @Inject(method="canStandOnFluid",at=@At("HEAD"),cancellable=true)
    private void randommorph$lava(FluidState fluid,CallbackInfoReturnable<Boolean> cir){if(AbilityHooks.has((Entity)(Object)this,LAVA_WALK)&&fluid.is(FluidTags.LAVA))cir.setReturnValue(true);}
    @Inject(method="travelInWater",at=@At("HEAD"),cancellable=true)
    private void randommorph$swim(Vec3 input,double gravity,boolean falling,double oldY,CallbackInfo ci){
        var entity=(LivingEntity)(Object)this;var profile=AbilityHooks.profile(entity);
        if(profile==null||!profile.has(AQUATIC)||entity.isPassenger())return;
        float speed=kr.randommorph.fabric.morph.NativeMovement.swimAcceleration(profile);
        entity.moveRelative(speed,input);entity.move(MoverType.SELF,entity.getDeltaMovement());entity.setDeltaMovement(entity.getDeltaMovement().scale(.9));ci.cancel();
    }
    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void randommorph$noWatchedJump(CallbackInfo ci){if(AbilityHooks.immobile((Entity)(Object)this))ci.cancel();}
    @Inject(method="hurtServer",at=@At("RETURN"))
    private void randommorph$nativeMelee(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue()){
            if((Object)this instanceof net.minecraft.server.level.ServerPlayer player){WitherTraits.damaged(player);ShulkerAttachment.damaged(player);}
            AbilityHooks.melee((LivingEntity)(Object)this,source);
            if((Object)this instanceof net.minecraft.server.level.ServerPlayer player&&AbilityHooks.profile(player)!=null&&source.getEntity() instanceof LivingEntity attacker)
                kr.randommorph.fabric.RandomMorphMod.manager(level.getServer()).identity(player).setLastHurtByMob(attacker);
        }
    }
}
