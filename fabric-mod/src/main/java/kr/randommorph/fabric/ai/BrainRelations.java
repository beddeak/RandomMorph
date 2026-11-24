package kr.randommorph.fabric.ai;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

/** Selective form-dependent memory cleanup. Anger, hurt, home, work and cooldowns stay native. */
public final class BrainRelations {
    private BrainRelations(){}
    private static final List<MemoryModuleType<?>> SPECIES_MEMORIES=List.of(
            MemoryModuleType.NEAREST_LIVING_ENTITIES,MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS,MemoryModuleType.VISIBLE_VILLAGER_BABIES,
            MemoryModuleType.NEAREST_HOSTILE,MemoryModuleType.NEAREST_ATTACKABLE,
            MemoryModuleType.NEAREST_VISIBLE_ADULT,MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
            MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
            MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN,
            MemoryModuleType.NEARBY_ADULT_PIGLINS,MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
            MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS,
            MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,
            MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM,
            MemoryModuleType.INTERACTION_TARGET,MemoryModuleType.BREED_TARGET,MemoryModuleType.RIDE_TARGET);

    private static boolean belongs(Object value,UUID owner){return value instanceof Entity e&&e.getUUID().equals(owner);}
    private static boolean tracks(Object value,UUID owner){return value instanceof EntityTracker tracker&&belongs(tracker.getEntity(),owner);}
    private static <T>T value(Brain<?> brain,MemoryModuleType<T> type){
        var memory=brain.getMemoryInternal(type);return memory==null?null:memory.orElse(null);
    }
    private static <T>void replace(Brain<?> brain,MemoryModuleType<T> type,T replacement){
        if(replacement==null){brain.eraseMemory(type);return;}
        long expiry=brain.getTimeUntilExpiry(type);
        if(expiry==Long.MAX_VALUE)brain.setMemory(type,replacement);else brain.setMemoryWithExpiry(type,replacement,expiry);
    }
    @SuppressWarnings("unchecked") // Removing elements preserves the native list's element type.
    private static <T>boolean forget(Brain<?> brain,MemoryModuleType<T> type,UUID owner){
        T old=value(brain,type);
        if(belongs(old,owner)){brain.eraseMemory(type);return true;}
        if(old instanceof List<?> list){
            var remaining=list.stream().filter(e->!belongs(e,owner)).toList();
            if(remaining.size()!=list.size()){replace(brain,type,(T)remaining);return true;}
        }
        return false;
    }
    private static void reduceCount(Brain<?> brain,MemoryModuleType<Integer> type,int removed){
        Integer count=value(brain,type);
        if(count!=null&&removed>0)replace(brain,type,Math.max(0,count-removed));
    }

    /** Returns whether navigation derived from the old species must be stopped. */
    public static boolean clear(Mob mob,ServerLevel level,UUID owner){
        var brain=mob.getBrain();
        boolean action=forget(brain,MemoryModuleType.ATTACK_TARGET,owner)|forget(brain,MemoryModuleType.AVOID_TARGET,owner);
        action|=forget(brain,MemoryModuleType.ROAR_TARGET,owner);
        action|=belongs(value(brain,MemoryModuleType.NEAREST_HOSTILE),owner);
        var walk=value(brain,MemoryModuleType.WALK_TARGET);
        boolean walking=walk!=null&&tracks(walk.getTarget(),owner);
        var look=value(brain,MemoryModuleType.LOOK_TARGET);
        if(tracks(look,owner))brain.eraseMemory(MemoryModuleType.LOOK_TARGET);

        var visible=value(brain,MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if(visible!=null){
            var removed=visible.nearbyEntities().stream().filter(e->belongs(e,owner)).toList();
            if(!removed.isEmpty()){
                // The sensor already cached visibility when calculating these counts. Keep other NPCs.
                int piglins=0,hoglins=0;
                for(var old:removed)if(visible.contains(old)){
                    if(old instanceof AbstractPiglin p&&p.isAdult())piglins++;
                    if(old instanceof Hoglin h&&h.isAdult())hoglins++;
                }
                reduceCount(brain,MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,piglins);
                reduceCount(brain,MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,hoglins);
                var remaining=visible.nearbyEntities().stream().filter(e->!belongs(e,owner)).toList();
                replace(brain,MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,new NearestVisibleLivingEntities(level,mob,remaining));
            }
        }
        for(var type:SPECIES_MEMORIES)forget(brain,type,owner);
        if(action||walking){
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.PATH);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
        return action||walking;
    }
}
