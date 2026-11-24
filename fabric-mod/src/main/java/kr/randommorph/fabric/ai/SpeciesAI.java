package kr.randommorph.fabric.ai;

import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.morph.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import org.slf4j.LoggerFactory;
import java.util.*;

/** A detached identity is used only to evaluate native typed selectors. Targets remain real players. */
public final class SpeciesAI {
    public static final String IDENTITY_TAG="randommorph_identity";
    private static final Set<String> WARNED=new HashSet<>();
    public static LivingEntity actual(LivingEntity entity){
        if(entity.entityTags().contains(IDENTITY_TAG)&&entity.level() instanceof ServerLevel level){var p=level.getServer().getPlayerList().getPlayer(entity.getUUID());if(p!=null)return p;}
        return entity;
    }
    /** Drop species-dependent targets on a form transition; native anger/retaliation memory is retained. */
    public static void invalidate(ServerPlayer player,LivingEntity previous){
        if(previous==null||!(previous.level() instanceof ServerLevel level))return;
        for(var entity:level.getAllEntities())if(entity instanceof Mob mob){
            boolean changed=false;
            if(mob.getTarget()!=null&&mob.getTarget().getUUID().equals(player.getUUID())){mob.setTarget(null);changed=true;}
            changed|=BrainRelations.clear(mob,level,player.getUUID());
            if(changed){
                mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
                mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH);
                mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                mob.getNavigation().stop();
            }
        }
    }
    private SpeciesAI(){}
    public static boolean enabled(ServerLevel level){return RandomMorphMod.manager(level.getServer()).config().speciesAi;}
    public static LivingEntity identity(LivingEntity entity){
        if(entity instanceof ServerPlayer player && ((MorphAccess)player).randommorph$form()!=null)
            return RandomMorphMod.manager(player.level().getServer()).identity(player);
        return entity;
    }
    public static boolean matches(Class<?> type,LivingEntity actual){return type.isInstance(identity(actual));}
    public static boolean test(TargetingConditions conditions,ServerLevel level,LivingEntity mob,LivingEntity actual){
        if(actual.isRemoved() || actual.level()!=level || !actual.isAlive() || actual.isSpectator() || actual instanceof ServerPlayer p && p.isCreative())return false;
        if(mob!=null && mob.isAlliedTo(actual))return false;
        var species=identity(actual);
        if(species==actual&&actual instanceof MorphAccess access&&access.randommorph$form()!=null)return false;
        try {return conditions.test(level,mob,species);}
        catch(ClassCastException incompatible){
            String key=conditions.getClass().getName()+":"+species.getType();
            if(WARNED.add(key))LoggerFactory.getLogger("RandomMorph").warn("Typed vanilla selector cannot accept {}. This AI target is skipped safely.",species.getType());
            return false;
        }
    }
    /** Native player goals have no four-block vertical box; TargetingConditions owns the range/visibility. */
    public static LivingEntity nearestPlayer(ServerLevel level,Mob mob,Class<? extends LivingEntity> type,TargetingConditions conditions){
        LivingEntity best=null;double distance=Double.MAX_VALUE;
        for(var player:level.players()){
            if(!matches(type,player)||!test(conditions,level,mob,player))continue;
            double d=player.distanceToSqr(mob.getX(),mob.getEyeY(),mob.getZ());
            if(d<distance){distance=d;best=player;}
        }
        return best;
    }
    /** A held typed fear identity must still belong to a present player in the acquired form/world. */
    public static LivingEntity refreshAvoided(ServerLevel level,LivingEntity previous,String form,Class<?> type){
        var entity=actual(previous);
        if(!(entity instanceof ServerPlayer player)||player.isRemoved()||!player.isAlive()||
                player.isCreative()||player.isSpectator()||player.level()!=level||!MorphManager.present(player))return null;
        var profile=((MorphAccess)player).randommorph$form();
        if(profile==null)return form.isEmpty()&&type.isInstance(player)?player:null;
        if(!profile.id().equals(form))return null;
        var current=identity(player);return type.isInstance(current)?current:null;
    }
    public static <T extends LivingEntity> LivingEntity nearest(ServerLevel level,Mob mob,Class<T> type,AABB area,TargetingConditions conditions){
        LivingEntity best=null;double distance=Double.MAX_VALUE;
        // Vanilla mobs are left in the candidate list; no global faction/scoreboard changes.
        for(var entity:level.getEntitiesOfClass(type,area,e->true)) {
            if(!matches(type,entity)||!test(conditions,level,mob,entity))continue;
            double d=entity.distanceToSqr(mob.getX(),mob.getEyeY(),mob.getZ());if(d<distance){distance=d;best=entity;}
        }
        for(var player:level.players()) {
            if(((MorphAccess)player).randommorph$form()==null || !area.intersects(player.getBoundingBox()) || !matches(type,player) || !test(conditions,level,mob,player))continue;
            double d=player.distanceToSqr(mob.getX(),mob.getEyeY(),mob.getZ());if(d<distance){distance=d;best=player;}
        }
        return best;
    }
}
