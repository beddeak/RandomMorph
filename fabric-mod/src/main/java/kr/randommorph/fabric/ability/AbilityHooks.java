package kr.randommorph.fabric.ability;
import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.morph.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.DamageTypeTags;
import java.util.*;
import static kr.randommorph.fabric.ability.Trait.*;

public final class AbilityHooks {
    private AbilityHooks(){}
    public static MorphProfile profile(Entity entity){return entity instanceof MorphAccess access?access.randommorph$form():null;}
    public static boolean has(Entity entity,Trait trait){var profile=profile(entity);return profile!=null&&profile.has(trait);}
    public static boolean needsWater(Entity entity){
        var p=profile(entity);return p!=null&&p.has(DRY_LAND)&&p.has(WATER_BREATHING)&&!WaterPhysiology.dries(p.id());
    }
    public static boolean immobile(Entity entity){
        var profile=profile(entity);if(profile==null||!(entity instanceof LivingEntity living))return false;
        if(profile.id().equals("shulker")&&!living.isSpectator()&&living.isAlive())return true;
        int flags=((MorphAccess)entity).randommorph$flags();
        if((flags&4)!=0 || profile.id().equals("armadillo")&&(flags&1)!=0)return true;
        if(!profile.has(CREAKING_WATCH))return false;
        for(var viewer:living.level().players()){
            if(viewer==living||viewer.isSpectator()||viewer.isCreative()||!viewer.isAlive()||viewer.distanceToSqr(living)>1024)continue;
            if(living.isLookingAtMe(viewer,.5,false,true,living.getEyeY(),living.getY()+.5*living.getScale(),(living.getEyeY()+living.getY())/2))return true;
        }
        return false;
    }
    public static boolean immune(ServerPlayer player,DamageSource source){
        var manager=RandomMorphMod.manager(player.level().getServer());var summonedOwner=manager.owner(source.getEntity());
        if(summonedOwner!=null&&(!player.canHarmPlayer(summonedOwner)||summonedOwner==player))return true;
        var profile=profile(player);if(profile==null)return false;
        var state=manager.abilityState(player);
        if(profile.id().equals("breeze")){
            var attacker=source.getEntity();var attackerForm=profile(attacker);
            if(attacker instanceof net.minecraft.world.entity.monster.breeze.Breeze||attackerForm!=null&&attackerForm.id().equals("breeze"))return true;
        }
        if(profile.has(FIRE_IMMUNE)&&source.is(DamageTypeTags.IS_FIRE))return true;
        if(profile.has(FALL_IMMUNE)&&source.is(DamageTypeTags.IS_FALL))return true;
        if(profile.phases()&&source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL))return true;
        if(profile.id().equals("enderman")&&source.is(DamageTypeTags.IS_PROJECTILE)&&!player.isPassenger()){
            AbilityEngine.dodge(manager,player,profile);return true;
        }
        if(profile.has(PROJECTILE_IMMUNE)&&source.is(DamageTypeTags.IS_PROJECTILE))return true;
        if(profile.has(WITHER_ARMOR)){
            if(source.is(DamageTypeTags.WITHER_IMMUNE_TO)||source.getEntity()!=null&&source.getEntity().is(net.minecraft.tags.EntityTypeTags.WITHER_FRIENDS))return true;
            if(player.getHealth()<=player.getMaxHealth()/2&&(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow||source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge))return true;
        }
        if(profile.id().equals("shulker")&&state!=null&&state.shell&&source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow)return true;
        return false;
    }
    public static float damage(ServerPlayer player,DamageSource source,float amount){
        var profile=profile(player);if(profile==null)return amount;
        var state=RandomMorphMod.manager(player.level().getServer()).abilityState(player);
        if(profile.id().equals("armadillo")&&state!=null&&state.shell)amount=Math.max(0,(amount-1)/2);
        if(profile.has(MAGIC_RESISTANCE)&&source.is(DamageTypeTags.WITCH_RESISTANT_TO))amount*=.15f;
        return amount;
    }
    public static void thorns(ServerPlayer player,DamageSource source){
        if(has(player,THORNS)&&player.getDeltaMovement().horizontalDistanceSqr()<.001&&!source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)&&!source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)&&source.getDirectEntity() instanceof LivingEntity attacker)
            attacker.hurtServer(player.level(),player.damageSources().thorns(player),2);
    }
    public static void melee(LivingEntity target,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer player)||source.getDirectEntity()!=player)return;
        var profile=profile(player);if(profile==null||!MorphManager.present(player))return;
        if(profile.id().equals("bee"))return; // Successful player hit owns sting consumption, including multipart targets.
        int difficulty=player.level().getDifficulty().getId();
        if(!target.isAlive())return;
        if(profile.has(POISON_ATTACK)&&difficulty>=2)target.addEffect(new MobEffectInstance(MobEffects.POISON,(difficulty==3?15:7)*20),player);
        if(profile.has(WITHER_ATTACK))target.addEffect(new MobEffectInstance(MobEffects.WITHER,200),player);
        if(profile.has(HUNGER_ATTACK))target.addEffect(new MobEffectInstance(MobEffects.HUNGER,140*difficulty),player);
        if(profile.has(LAUNCH_ATTACK)){target.push(0,.45*(1-target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)),0);target.needsSync=true;}
    }
    public static void sting(ServerPlayer player,Entity target){
        var profile=profile(player);if(profile==null||!profile.id().equals("bee")||!MorphManager.present(player))return;
        var manager=RandomMorphMod.manager(player.level().getServer());
        if(manager.abilityState(player)==null||!((MorphAccess)player).randommorph$animals().sting())return;
        if(target instanceof LivingEntity living){
            living.setStingerCount(living.getStingerCount()+1);int difficulty=player.level().getDifficulty().getId();
            if(living.isAlive()&&difficulty>=2)living.addEffect(new MobEffectInstance(MobEffects.POISON,(difficulty==3?18:10)*20),player);
        }
        player.level().playSound(null,player,net.minecraft.sounds.SoundEvents.BEE_STING,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        manager.syncFlags(player);
    }
}
