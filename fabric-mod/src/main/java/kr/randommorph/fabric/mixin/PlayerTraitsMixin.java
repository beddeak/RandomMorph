package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.*;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import static kr.randommorph.fabric.ability.Trait.*;
@Mixin(value=Player.class,remap=false)
public abstract class PlayerTraitsMixin {
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void randommorph$oneSting(Entity target,CallbackInfo ci){
        var player=(Player)(Object)this;var profile=AbilityHooks.profile(player);
        if(profile!=null&&profile.id().equals("bee")&&
                (((MorphAccess)player).randommorph$animals().stung()||(((MorphAccess)player).randommorph$flags()&kr.randommorph.fabric.core.MorphFlags.STUNG)!=0))ci.cancel();
    }
    @Inject(method="doSweepAttack",at=@At("HEAD"),cancellable=true)
    private void randommorph$noBeeSweep(CallbackInfo ci){var profile=AbilityHooks.profile((Entity)(Object)this);if(profile!=null&&profile.id().equals("bee"))ci.cancel();}
    @Inject(method="stabAttack",at=@At("HEAD"),cancellable=true)
    private void randommorph$oneStab(CallbackInfoReturnable<Boolean> cir){
        var player=(Player)(Object)this;var profile=AbilityHooks.profile(player);
        if(profile!=null&&profile.id().equals("bee")&&
                (((MorphAccess)player).randommorph$animals().stung()||(((MorphAccess)player).randommorph$flags()&kr.randommorph.fabric.core.MorphFlags.STUNG)!=0))cir.setReturnValue(false);
    }
    @Inject(method="createAttackSource",at=@At("HEAD"),cancellable=true)
    private void randommorph$stingDamage(net.minecraft.world.item.ItemStack weapon,CallbackInfoReturnable<DamageSource> cir){
        if((Object)this instanceof ServerPlayer player){var profile=AbilityHooks.profile(player);if(profile!=null&&profile.id().equals("bee"))cir.setReturnValue(player.damageSources().sting(player));}
    }
    @WrapOperation(method={"attack","stabAttack"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),require=2)
    private boolean randommorph$stingHit(Entity target,DamageSource source,float amount,Operation<Boolean> original){
        boolean accepted=original.call(target,source,amount);
        if(accepted&&(Object)this instanceof ServerPlayer player)AbilityHooks.sting(player,target);
        return accepted;
    }
    @Redirect(method="tick",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/player/Player;noPhysics:Z",opcode=Opcodes.PUTFIELD))
    private void randommorph$phase(Player player,boolean spectator){var profile=AbilityHooks.profile(player);player.noPhysics=spectator||profile!=null&&profile.phases();}
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void randommorph$immunities(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof ServerPlayer player){if(AbilityHooks.immune(player,source))cir.setReturnValue(false);else AbilityHooks.thorns(player,source);}
    }
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float randommorph$resistance(float amount,ServerLevel level,DamageSource source,float original){
        return (Object)this instanceof ServerPlayer player?AbilityHooks.damage(player,source,amount):amount;
    }
    @Inject(method="causeFallDamage",at=@At("HEAD"),cancellable=true)
    private void randommorph$fall(double distance,float modifier,DamageSource source,CallbackInfoReturnable<Boolean> cir){if(AbilityHooks.has((Entity)(Object)this,FALL_IMMUNE)||AbilityHooks.has((Entity)(Object)this,SLOW_FALL))cir.setReturnValue(false);}
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void randommorph$still(Vec3 input,CallbackInfo ci){
        var player=(Player)(Object)this;
        if(AbilityHooks.immobile(player)){player.setDeltaMovement(new Vec3(0,player.getDeltaMovement().y,0));player.move(MoverType.SELF,player.getDeltaMovement());ci.cancel();}
    }
    @Inject(method="aiStep",at=@At("HEAD"))
    private void randommorph$movement(CallbackInfo ci){
        var player=(Player)(Object)this;var profile=AbilityHooks.profile(player);if(profile==null||!player.isAlive()||player.isSpectator())return;
        if(profile.has(FLIGHT)&&!player.isPassenger())player.getAbilities().flying=true;
        if(profile.has(SLOW_FALL)&&!player.onGround()&&player.getDeltaMovement().y<-.06)player.setDeltaMovement(player.getDeltaMovement().multiply(1,0,1).add(0,-.06,0));
        if(profile.has(LAVA_WALK)&&player.isInLava()){
            var context=CollisionContext.of(player);
            if(!context.isAbove(player.getLiquidCollisionShape(),player.blockPosition(),true)||player.level().getFluidState(player.blockPosition().above()).is(net.minecraft.tags.FluidTags.LAVA))player.setDeltaMovement(player.getDeltaMovement().scale(.5).add(0,.05,0));
            else player.setOnGround(true);
        }
    }
}
