package kr.randommorph.verify;

import java.util.*;
import kr.randommorph.fabric.ai.*;
import kr.randommorph.fabric.morph.MorphRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.phys.Vec3;
import static kr.randommorph.verify.SpeciesQueryProbe.*;

/** Actual merged Brain memories/visibility/trackers/native decisions, using worldless fixtures only. */
public final class BrainRelationProbe {
    private static int cases;
    private static void check(boolean ok,String message){cases++;if(!ok)throw new AssertionError(message);}
    static Brain<LivingEntity> brain()throws Exception{
        var result=new Brain<LivingEntity>();
        var register=Brain.class.getDeclaredMethod("registerMemory",MemoryModuleType.class);register.setAccessible(true);
        for(var memory:MemoryModuleType.class.getDeclaredFields())if(memory.getType()==MemoryModuleType.class)register.invoke(result,memory.get(null));
        return result;
    }
    private static <T extends LivingEntity>T body(Class<T> type,String id,Space level,Person owner)throws Exception{
        var entity=at(type,level,2,0,0);field(entity,Entity.class,"type",MorphRegistry.find(id).type());
        entity.setUUID(owner.getUUID());entity.setId(owner.getId());entity.entityTags().add(SpeciesAI.IDENTITY_TAG);return entity;
    }
    private static NearestVisibleLivingEntities visible(List<LivingEntity> entities)throws Exception{
        var result=NativeEntityFixtures.allocate(NearestVisibleLivingEntities.class);
        field(result,NearestVisibleLivingEntities.class,"nearbyEntities",entities);
        field(result,NearestVisibleLivingEntities.class,"lineOfSightTest",(java.util.function.Predicate<LivingEntity>)e->true);
        return result;
    }
    public static void run(String side)throws Exception{
        cases=0;
        var level=NativeEntityFixtures.allocate(Space.class);level.people=new ArrayList<>();level.candidates=new ArrayList<>();
        var server=NativeEntityFixtures.allocate(DedicatedServer.class);
        var people=NativeEntityFixtures.allocate(DedicatedPlayerList.class);
        field(server,MinecraftServer.class,"playerList",people);
        var online=new HashMap<UUID,net.minecraft.server.level.ServerPlayer>();
        field(people,PlayerList.class,"playersByUUID",online);level.server=server;
        var owner=at(Person.class,level,2,0,0);online.put(owner.getUUID(),owner);level.people.add(owner);
        owner.randommorph$form(MorphRegistry.find("zombified_piglin"));
        var zombie=body(ZombifiedPiglin.class,"zombified_piglin",level,owner);
        var memories=brain();
        memories.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,Optional.of(zombie));
        memories.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET,zombie,117);
        check(memories.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).orElseThrow()==zombie,"Typed threat snapshot was replaced");
        check(memories.getMemory(MemoryModuleType.AVOID_TARGET).orElseThrow()==owner,"Avoid target must be the actual player");
        check(memories.getTimeUntilExpiry(MemoryModuleType.AVOID_TARGET)==117,"Timed write changed expiry");
        check(memories.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,owner),"Fear equality regression remains");
        var piglin=at(Piglin.class,level,0,0,0);field(piglin,LivingEntity.class,"brain",memories);
        var stop=PiglinAi.class.getDeclaredMethod("wantsToStopFleeing",Piglin.class);stop.setAccessible(true);
        check(!(boolean)stop.invoke(null,piglin),"Native piglin immediately stopped fleeing a visible zombified player");
        memories.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED);
        check((boolean)stop.invoke(null,piglin),"Native piglin failed to stop after the threat disappeared");
        memories.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,zombie);
        memories.setMemory(MemoryModuleType.ATTACK_TARGET,Optional.of(zombie));
        check(memories.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow()==owner,"Optional attack target was not resolved");
        var nemesis=body(WitherSkeleton.class,"wither_skeleton",level,owner);
        memories.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS,nemesis);
        check(memories.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS).orElseThrow()==nemesis,"Typed Mob memory was replaced by Player");
        var hoglin=body(Hoglin.class,"hoglin",level,owner);
        memories.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,hoglin);
        check(memories.getMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN).orElseThrow()==hoglin,"Typed Hoglin memory was replaced by Player");
        memories.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED);
        var choose=PiglinAi.class.getDeclaredMethod("findNearestValidAttackTarget",net.minecraft.server.level.ServerLevel.class,Piglin.class);choose.setAccessible(true);
        check(((Optional<?>)choose.invoke(null,level,piglin)).orElseThrow()==owner,"Piglin nemesis decision targeted a detached body");
        var brute=at(PiglinBrute.class,level,0,0,0);field(brute,LivingEntity.class,"brain",memories);
        choose=PiglinBruteAi.class.getDeclaredMethod("findNearestValidAttackTarget",net.minecraft.server.level.ServerLevel.class,AbstractPiglin.class);choose.setAccessible(true);
        check(((Optional<?>)choose.invoke(null,level,brute)).orElseThrow()==owner,"Brute nemesis decision targeted a detached body");
        memories.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        check(((Optional<?>)choose.invoke(null,level,brute)).isEmpty(),"Brute inherited the Hoglin hunting candidate");
        memories.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,zombie);

        var cache=visible(List.of(zombie));
        check(cache.nearbyEntities().contains(owner),"Native membership must compare the shared entity ID");
        check(cache.contains(owner),"Visible action player failed typed-cache membership");
        memories.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,cache);
        check(new EntityTracker(owner,false).isVisibleBy(piglin),"Native action tracker cannot see its target");
        var tracker=new EntityTracker(zombie,false);
        check(tracker.getEntity()==owner,"Look tracker retained detached identity");
        field(owner,Entity.class,"position",new Vec3(8,0,0));
        check(tracker.currentPosition().x()==8,"Tracker follows stale proxy coordinates");
        field(owner,Entity.class,"position",new Vec3(2,0,0));
        owner.randommorph$form(MorphRegistry.find("cow"));
        check(cache.nearbyEntities().getFirst()==zombie&&cache.contains(owner),"Stale species cache regression not reproduced");
        owner.randommorph$form(MorphRegistry.find("zombified_piglin"));
        var npc=at(Hunter.class,level,4,0,0);
        check(new EntityTracker(npc,false).getEntity()==npc,"Ordinary NPC tracker was changed");
        check(visible(List.of(npc)).contains(npc),"Ordinary NPC visibility was changed");

        var hunter=at(Hunter.class,level,0,0,0);var clean=brain();field(hunter,LivingEntity.class,"brain",clean);
        clean.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES,List.of(zombie,npc));
        clean.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,visible(List.of(zombie,npc)));
        clean.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,zombie);
        clean.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS,nemesis);
        clean.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,hoglin);
        clean.setMemory(MemoryModuleType.ATTACK_TARGET,zombie);
        clean.setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(zombie,1,0));
        clean.setMemory(MemoryModuleType.LOOK_TARGET,new EntityTracker(zombie,true));
        clean.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT,owner.getUUID(),333);
        clean.setMemory(MemoryModuleType.HURT_BY_ENTITY,owner);
        clean.setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY,true,200);
        check(BrainRelations.clear(hunter,level,owner.getUUID()),"Old action path not canceled");
        check(clean.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElseThrow().equals(List.of(npc)),"Other candidate was erased");
        check(clean.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElseThrow().nearbyEntities().equals(List.of(npc)),"Visible cache retained old identity or lost NPC");
        check(!clean.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN)&&!clean.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_NEMESIS)&&!clean.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED),"Typed derived memories survived a form change");
        check(!clean.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)&&!clean.hasMemoryValue(MemoryModuleType.WALK_TARGET)&&!clean.hasMemoryValue(MemoryModuleType.LOOK_TARGET),"Action trackers survived cleanup");
        check(clean.getMemory(MemoryModuleType.ANGRY_AT).orElseThrow().equals(owner.getUUID())&&clean.getTimeUntilExpiry(MemoryModuleType.ANGRY_AT)==333,"Native anger erased/reset");
        check(clean.getMemory(MemoryModuleType.HURT_BY_ENTITY).orElseThrow()==owner,"Retaliation memory erased");
        check(clean.getMemory(MemoryModuleType.HUNTED_RECENTLY).orElseThrow()&&clean.getTimeUntilExpiry(MemoryModuleType.HUNTED_RECENTLY)==200,"Hunt cooldown reset");
        check(!BrainRelations.clear(hunter,level,owner.getUUID()),"Repeat cleanup is not idempotent");

        var list=brain();field(hunter,LivingEntity.class,"brain",list);
        var ally=body(Piglin.class,"piglin",level,owner);var other=at(Piglin.class,level,5,0,0);
        list.setMemoryWithExpiry(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,List.of(ally,other),67);
        list.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,visible(List.of(ally,other)));
        list.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,2);
        list.setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(new Vec3(10,0,0),1,0));
        check(!BrainRelations.clear(hunter,level,owner.getUUID()),"Perception-only invalidation canceled unrelated stroll");
        check(list.getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElseThrow().equals(List.of(other))&&list.getTimeUntilExpiry(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)==67,"Typed list cleanup lost NPC or expiry");
        check(list.getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElseThrow()==1,"Removed ally remains in count");
        check(list.hasMemoryValue(MemoryModuleType.WALK_TARGET),"Unrelated walk target erased");
        list.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,visible(List.of(hoglin)));
        list.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,1);BrainRelations.clear(hunter,level,owner.getUUID());
        check(list.getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElseThrow()==0,"Removed visible hoglin remains in count");
        var nav=NativeEntityFixtures.allocate(Nav.class);field(hunter,Mob.class,"navigation",nav);
        list.setMemory(MemoryModuleType.AVOID_TARGET,zombie);level.candidates=List.of(hunter);
        SpeciesAI.invalidate(owner,zombie);check(nav.stops==1&&!list.hasMemoryValue(MemoryModuleType.AVOID_TARGET),"Central form cleanup was not wired");
        SpeciesAI.invalidate(owner,zombie);check(nav.stops==1,"Central repeat cleanup stopped unrelated path");
        System.out.println("RANDOMMORPH_BRAIN_RELATION_OK side="+side+" cases="+cases+"; actual memories/native fear/visibility/trackers/cleanup, no world/AI tick started");
    }
}
