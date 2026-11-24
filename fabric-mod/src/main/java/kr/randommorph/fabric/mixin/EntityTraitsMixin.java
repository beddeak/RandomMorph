package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Entity.class,remap=false)
public abstract class EntityTraitsMixin {
    @Inject(method="getBoundingBox",at=@At("HEAD"),cancellable=true)
    private void randommorph$attachedBox(CallbackInfoReturnable<net.minecraft.world.phys.AABB> cir){
        var self=(Entity)(Object)this;
        if(ShulkerAttachment.shulker(self)&&self instanceof net.minecraft.world.entity.LivingEntity living&&self.isAlive()&&!self.isSpectator()){
            int flags=((kr.randommorph.fabric.morph.MorphAccess)self).randommorph$flags();
            cir.setReturnValue(net.minecraft.world.entity.monster.Shulker.getProgressAabb(living.getScale(),kr.randommorph.fabric.core.MorphFlags.face(flags).getOpposite(),(flags&1)==0?1:0,net.minecraft.world.phys.Vec3.atBottomCenterOf(self.blockPosition())));
        }
    }
    @Inject(method="isNoGravity",at=@At("HEAD"),cancellable=true)
    private void randommorph$attachedGravity(CallbackInfoReturnable<Boolean> cir){
        var self=(Entity)(Object)this;if(ShulkerAttachment.shulker(self)&&self.isAlive()&&!self.isSpectator())cir.setReturnValue(true);
    }
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void randommorph$attachedMovement(net.minecraft.world.entity.MoverType type,net.minecraft.world.phys.Vec3 motion,CallbackInfo ci){
        var self=(Entity)(Object)this;if(ShulkerAttachment.shulker(self)&&self.isAlive()&&!self.isSpectator()){
            self.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);self.fallDistance=0;ci.cancel();
        }
    }

    @Inject(method="deflection",at=@At("HEAD"),cancellable=true)
    private void randommorph$breeze(net.minecraft.world.entity.projectile.Projectile projectile,
            CallbackInfoReturnable<net.minecraft.world.entity.projectile.ProjectileDeflection> cir){
        var self=(Entity)(Object)this;var profile=AbilityHooks.profile(self);
        if(profile!=null&&profile.id().equals("breeze")&&self.isAlive()&&!self.isRemoved()&&!self.isSpectator()&&
                self.is(net.minecraft.tags.EntityTypeTags.DEFLECTS_PROJECTILES)){
            var type=projectile.getType();
            cir.setReturnValue(type==net.minecraft.world.entity.EntityTypes.WIND_CHARGE||type==net.minecraft.world.entity.EntityTypes.BREEZE_WIND_CHARGE
                    ?net.minecraft.world.entity.projectile.ProjectileDeflection.NONE:BreezeDeflectionAccess.randommorph$deflection());
        }
    }
    @Inject(method="interact",at=@At("HEAD"),cancellable=true)
    private void randommorph$harvest(net.minecraft.world.entity.player.Player actor,net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.Vec3 location,CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir){
        var result=AnimalActions.interact((Entity)(Object)this,actor,hand);
        if(result!=net.minecraft.world.InteractionResult.PASS)cir.setReturnValue(result);
    }
    @Inject(method="fireImmune",at=@At("HEAD"),cancellable=true)
    private void randommorph$fire(CallbackInfoReturnable<Boolean> cir){
        var profile=AbilityHooks.profile((Entity)(Object)this);
        if(profile!=null)cir.setReturnValue(profile.type().fireImmune()||profile.has(Trait.FIRE_IMMUNE));
    }
    @Inject(method="getMaxAirSupply",at=@At("HEAD"),cancellable=true)
    private void randommorph$air(CallbackInfoReturnable<Integer> cir){
        var profile=AbilityHooks.profile((Entity)(Object)this);
        if(profile!=null){if(profile.id().equals("dolphin"))cir.setReturnValue(4800);else if(profile.id().equals("axolotl"))cir.setReturnValue(6000);}
    }
}
