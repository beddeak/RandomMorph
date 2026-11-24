package kr.randommorph.verify;

import java.util.*;
import kr.randommorph.fabric.*;
import kr.randommorph.fabric.ai.*;
import kr.randommorph.fabric.morph.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.warden.*;
import static kr.randommorph.verify.SpeciesQueryProbe.*;

/** Executes merged native owner/anger/priority methods with fixed candidates; never runs a world or Mob AI. */
public final class ProtectionAngerProbe {
    private static int cases;
    private static void check(boolean ok,String message){cases++;if(!ok)throw new AssertionError(message);}
    // Wolf inherits these owner/reference checks from TamableAnimal; omit Wolf's datapack variant registries.
    public static class Companion extends TamableAnimal {
        Person owner;
        private Companion(){super(null,null);}
        @Override public LivingEntity getOwner(){return owner;}
        @Override public boolean isTame(){return true;}
        @Override public net.minecraft.world.scores.PlayerTeam getTeam(){return null;}
        @Override public boolean isFood(net.minecraft.world.item.ItemStack item){return false;}
        @Override public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level,AgeableMob mate){return null;}
    }
    public static class Sentinel extends Warden {
        private Sentinel(){super(null,null);}
        // This probe isolates anger priority, not native sensory eligibility or line of sight.
        @Override public boolean canTargetEntity(Entity entity){return true;}
    }
    @SuppressWarnings("unchecked") public static void run(String side)throws Exception{
        cases=0;
        var level=NativeEntityFixtures.allocate(Space.class);level.people=new ArrayList<>();level.candidates=new ArrayList<>();
        var server=NativeEntityFixtures.allocate(DedicatedServer.class);var people=NativeEntityFixtures.allocate(DedicatedPlayerList.class);
        field(server,MinecraftServer.class,"playerList",people);
        var online=new HashMap<UUID,net.minecraft.server.level.ServerPlayer>();field(people,PlayerList.class,"playersByUUID",online);level.server=server;
        var config=new MorphConfig();var manager=NativeEntityFixtures.allocate(MorphManager.class);
        field(manager,MorphManager.class,"config",config);
        var managersField=RandomMorphMod.class.getDeclaredField("MANAGERS");managersField.setAccessible(true);
        var managers=(Map<MinecraftServer,MorphManager>)managersField.get(null);managers.put(server,manager);
        try{
            var owner=at(Person.class,level,2,0,0);online.put(owner.getUUID(),owner);level.people.add(owner);
            owner.randommorph$form(MorphRegistry.find("sheep"));
            var proxy=at(Hunter.class,level,2,0,0);field(proxy,Entity.class,"type",MorphRegistry.find("sheep").type());
            proxy.setUUID(owner.getUUID());proxy.setId(owner.getId());proxy.entityTags().add(SpeciesAI.IDENTITY_TAG);
            var wolf=at(Companion.class,level,0,0,0);wolf.owner=owner;field(wolf,Entity.class,"type",EntityTypes.WOLF);
            check(wolf.isOwnedBy(owner)&&wolf.isAlliedTo(owner),"Native tamed owner fixture invalid");
            check(!wolf.isOwnedBy(proxy)&&!wolf.isAlliedTo(proxy),"Shared ID accidentally hid the native owner reference bug");
            var combat=TargetingConditions.forCombat().ignoreLineOfSight();
            check(combat.test(level,wolf,proxy),"Old detached-attacker eligibility regression not reproduced");
            wolf.tickCount=1;wolf.setLastHurtByMob(owner);
            var retaliate=NativeEntityFixtures.allocate(HurtByTargetGoal.class);
            field(retaliate,TargetGoal.class,"mob",wolf);field(retaliate,HurtByTargetGoal.class,"toIgnoreDamage",new Class<?>[0]);
            check(!retaliate.canUse(),"Tamed wolf retaliated against its morphed owner");
            check(wolf.getLastHurtByMob()==owner,"Retaliation history was replaced by a detached body");
            var stranger=at(Person.class,level,3,0,0);stranger.randommorph$form(MorphRegistry.find("cow"));
            level.people.add(stranger);wolf.setLastHurtByMob(stranger);
            stranger.creative=true;check(!retaliate.canUse(),"Creative morphed attacker accepted");stranger.creative=false;
            stranger.spectator=true;check(!retaliate.canUse(),"Spectator morphed attacker accepted");stranger.spectator=false;
            stranger.alive=false;check(!retaliate.canUse(),"Dead morphed attacker accepted");stranger.alive=true;
            field(stranger,Entity.class,"removalReason",Entity.RemovalReason.DISCARDED);
            check(!retaliate.canUse(),"Removed morphed attacker accepted");field(stranger,Entity.class,"removalReason",null);
            wolf.setLastHurtByMob(stranger);
            field(stranger,Entity.class,"level",NativeEntityFixtures.allocate(Space.class));
            check(!retaliate.canUse(),"Cross-world morphed attacker accepted");field(stranger,Entity.class,"level",level);
            var raider=at(net.minecraft.world.entity.monster.illager.Pillager.class,level,3,0,0);
            field(raider,Entity.class,"type",EntityTypes.PILLAGER);
            wolf.setLastHurtByMob(raider);
            field(retaliate,HurtByTargetGoal.class,"toIgnoreDamage",new Class<?>[]{net.minecraft.world.entity.raid.Raider.class});
            check(!retaliate.canUse(),"Native excluded damage class lost its early rejection");

            var anger=new AngerManagement(e->true,List.of());
            check(anger.increaseAnger(proxy,35)==35,"Native anger amount changed");
            check(anger.getActiveEntity().orElseThrow()==owner,"Warden stored a detached suspect instead of the player");
            field(proxy,Entity.class,"removalReason",Entity.RemovalReason.DISCARDED);
            owner.randommorph$form(MorphRegistry.find("cow"));field(anger,AngerManagement.class,"conversionDelay",10);
            anger.tick(level,e->true);
            check(anger.getActiveEntity().orElseThrow()==owner&&anger.getActiveAnger(owner)==34,"Discarding the old morph erased native anger");
            check(anger.increaseAnger(owner,200)==150,"Native anger cap changed");
            var pairs=AngerManagement.class.getDeclaredMethod("createUuidAngerPairs");pairs.setAccessible(true);
            var saved=(List<com.mojang.datafixers.util.Pair<UUID,Integer>>)pairs.invoke(anger);
            check(saved.size()==1&&saved.getFirst().getFirst().equals(owner.getUUID())&&saved.getFirst().getSecond()==150,"Native UUID persistence changed");
            anger.clearAnger(owner);check(anger.getActiveEntity().isEmpty(),"Native explicit anger clear failed");

            var npc=at(Hunter.class,level,5,0,0);var human=at(Person.class,level,6,0,0);
            var rank=new AngerManagement(e->true,List.of());rank.increaseAnger(owner,100);rank.increaseAnger(npc,140);
            check(rank.getActiveEntity().orElseThrow()==npc,"Morphed player retained native human priority");
            rank.increaseAnger(human,100);check(rank.getActiveEntity().orElseThrow()==human,"Ordinary human priority was removed");
            rank.clearAnger(human);config.speciesAi=false;rank.increaseAnger(owner,0);
            check(rank.getActiveEntity().orElseThrow()==owner,"speciesAi=false did not restore human priority");
            config.speciesAi=true;rank.increaseAnger(owner,0);check(rank.getActiveEntity().orElseThrow()==npc,"Species priority did not recover after enabling");
            var calm=new AngerManagement(e->true,List.of());calm.increaseAnger(human,79);calm.increaseAnger(npc,80);
            check(calm.getActiveEntity().orElseThrow()==npc,"Native angry threshold must precede human priority");

            var warden=at(Sentinel.class,level,0,0,0);var memories=BrainRelationProbe.brain();
            field(warden,Entity.class,"type",EntityTypes.WARDEN);
            field(warden,LivingEntity.class,"brain",memories);var currentAnger=new AngerManagement(e->true,List.of());
            field(warden,Warden.class,"angerManagement",currentAnger);
            memories.setMemory(MemoryModuleType.ATTACK_TARGET,npc);warden.increaseAngerAt(owner,80,false);
            check(memories.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow()==npc,"Morphed source stole attack target as a human");
            check(currentAnger.getActiveAnger(owner)==80,"Native Warden anger write changed");
            check(!memories.hasMemoryValue(MemoryModuleType.DIG_COOLDOWN),"Native anger must not create an absent dig cooldown");
            memories.setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN,net.minecraft.util.Unit.INSTANCE,120);
            warden.increaseAngerAt(owner,0,false);
            check(memories.getTimeUntilExpiry(MemoryModuleType.DIG_COOLDOWN)==1200,"Native existing dig cooldown was not refreshed");
            warden.increaseAngerAt(human,80,false);check(!memories.hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Unmorphed angry human no longer replaces non-human target");
            memories.setMemory(MemoryModuleType.ATTACK_TARGET,owner);warden.increaseAngerAt(human,0,false);
            check(!memories.hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Morphed current target was classified as human");
            memories.setMemory(MemoryModuleType.ATTACK_TARGET,npc);config.speciesAi=false;warden.increaseAngerAt(owner,0,false);
            check(!memories.hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Disabled species AI altered native Warden source priority");config.speciesAi=true;

            memories.setMemory(MemoryModuleType.ATTACK_TARGET,owner);
            var isTarget=WardenAi.class.getDeclaredMethod("isTarget",Warden.class,LivingEntity.class);isTarget.setAccessible(true);
            check((boolean)isTarget.invoke(null,warden,proxy),"Warden native target reference comparison rejected its morphed player");
            check(!(boolean)isTarget.invoke(null,warden,npc),"Unrelated NPC accepted as native current target");
            memories.setMemoryWithExpiry(MemoryModuleType.ROAR_TARGET,proxy,117);
            check(memories.getMemory(MemoryModuleType.ROAR_TARGET).orElseThrow()==owner&&memories.getTimeUntilExpiry(MemoryModuleType.ROAR_TARGET)==117,"Roar target did not retain the real owner and expiry");
            check(BrainRelations.clear(warden,level,owner.getUUID())&&!memories.hasMemoryValue(MemoryModuleType.ROAR_TARGET),"Form cleanup retained old roar/action target");
            check(currentAnger.getActiveAnger(owner)==80,"Form cleanup reset persistent anger");
            memories.setMemory(MemoryModuleType.ROAR_TARGET,npc);
            check(!BrainRelations.clear(warden,level,owner.getUUID())&&memories.getMemory(MemoryModuleType.ROAR_TARGET).orElseThrow()==npc,"Unrelated NPC roar target was erased");
            System.out.println("RANDOMMORPH_PROTECTION_ANGER_OK side="+side+" cases="+cases+"; actual native retaliation/anger maintenance/priority/reference checks, fixed candidates, no world or Mob/Brain AI loop");
        }finally{managers.remove(server);}
    }
}
