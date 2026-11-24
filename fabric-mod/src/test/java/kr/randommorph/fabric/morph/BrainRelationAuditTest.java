package kr.randommorph.fabric.morph;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.sensing.*;
import net.minecraft.world.entity.monster.hoglin.*;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.monster.skeleton.*;
import net.minecraft.world.entity.monster.zombie.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executes pinned vanilla predicates, without loading a world or starting AI. */
class BrainRelationAuditTest {
    @BeforeAll static void init(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private static <T>T allocate(Class<T> type)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    private static LivingEntity body(String id,Class<? extends LivingEntity> type,double x)throws Exception{
        var entity=allocate(type);
        var field=Entity.class.getDeclaredField("type");field.setAccessible(true);field.set(entity,MorphRegistry.find(id).type());
        field=Entity.class.getDeclaredField("position");field.setAccessible(true);field.set(entity,new Vec3(x,0,0));
        return entity;
    }
    private static boolean villagerMatches(LivingEntity villager,LivingEntity target)throws Exception{
        var method=VillagerHostilesSensor.class.getDeclaredMethod("isMatchingEntity",net.minecraft.server.level.ServerLevel.class,LivingEntity.class,LivingEntity.class);
        method.setAccessible(true);return (boolean)method.invoke(new VillagerHostilesSensor(),null,villager,target);
    }
    @Test void villagersUseElevenExactThreatTypesAndNativeDistances()throws Exception{
        var field=VillagerHostilesSensor.class.getDeclaredField("ACCEPTABLE_DISTANCE_FROM_HOSTILES");field.setAccessible(true);
        var actual=(Map<?,?>)field.get(null);var expected=Map.ofEntries(
                Map.entry("drowned",8f),Map.entry("evoker",12f),Map.entry("husk",8f),Map.entry("illusioner",12f),
                Map.entry("pillager",15f),Map.entry("ravager",12f),Map.entry("vex",8f),Map.entry("vindicator",10f),
                Map.entry("zoglin",10f),Map.entry("zombie",8f),Map.entry("zombie_villager",8f));
        assertEquals(expected.size(),actual.size());
        for(var entry:expected.entrySet())assertEquals(entry.getValue(),actual.get(MorphRegistry.find(entry.getKey()).type()),entry.getKey());
    }
    @Test void nativeThreatDistanceHasInclusiveBoundary()throws Exception{
        var witness=body("zombie",Zombie.class,0);
        assertTrue(villagerMatches(witness,body("zombie",Zombie.class,8)));
        assertFalse(villagerMatches(witness,body("zombie",Zombie.class,8.01)));
        assertTrue(villagerMatches(witness,body("pillager",net.minecraft.world.entity.monster.illager.Pillager.class,15)));
        assertFalse(villagerMatches(witness,body("pillager",net.minecraft.world.entity.monster.illager.Pillager.class,15.01)));
    }
    @Test void monsterCategoryDoesNotAddSkeletonWitchPiglinToVillagerFear()throws Exception{
        var witness=body("zombie",Zombie.class,0);
        assertFalse(villagerMatches(witness,body("skeleton",Skeleton.class,1)));
        assertFalse(villagerMatches(witness,body("witch",net.minecraft.world.entity.monster.Witch.class,1)));
        assertFalse(villagerMatches(witness,body("piglin",Piglin.class,1)));
    }
    @Test void piglinFearIsZombifiedPiglinAndZoglinRatherThanAllUndead()throws Exception{
        assertTrue(PiglinAi.isZombified(body("zombified_piglin",ZombifiedPiglin.class,0)));
        assertTrue(PiglinAi.isZombified(body("zoglin",Zoglin.class,0)));
        assertFalse(PiglinAi.isZombified(body("zombie",Zombie.class,0)));
        assertFalse(PiglinAi.isZombified(body("wither_skeleton",WitherSkeleton.class,0)));
        assertFalse(PiglinAi.isZombified(body("wither",WitherBoss.class,0)));
    }
    @Test void bruteDoesNotInheritPiglinHunting()throws Exception{
        var method=Piglin.class.getDeclaredMethod("canHunt");method.setAccessible(true);
        assertTrue((boolean)method.invoke(allocate(Piglin.class)));assertFalse(allocate(PiglinBrute.class).canHunt());
    }
    @Test void bruteNemesisSensorKeepsWitherClassPredicates()throws Exception{
        var method=PiglinBruteSpecificSensor.class.getDeclaredMethod("lambda$doTick$0",LivingEntity.class);method.setAccessible(true);
        assertTrue((boolean)method.invoke(null,body("wither_skeleton",WitherSkeleton.class,0)));
        assertTrue((boolean)method.invoke(null,body("wither",WitherBoss.class,0)));
        assertFalse((boolean)method.invoke(null,body("zombified_piglin",ZombifiedPiglin.class,0)));
        assertFalse((boolean)method.invoke(null,body("hoglin",Hoglin.class,0)));
    }
    @Test void dolphinsAvoidBothGuardianClassesWithoutInventingReciprocalHunting()throws Exception{
        var dolphin=net.minecraft.world.entity.animal.dolphin.Dolphin.class;
        try(var stream=dolphin.getResourceAsStream("Dolphin.class")){
            var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(stream).accept(node,0);
            var goals=node.methods.stream().filter(m->m.name.equals("registerGoals")).findFirst().orElseThrow();
            assertTrue(java.util.stream.StreamSupport.stream(goals.instructions.spliterator(),false).anyMatch(i->i instanceof org.objectweb.asm.tree.LdcInsnNode ldc&&ldc.cst instanceof org.objectweb.asm.Type t&&t.getClassName().equals("net.minecraft.world.entity.monster.Guardian")));
        }
        assertTrue(net.minecraft.world.entity.monster.Guardian.class.isAssignableFrom(net.minecraft.world.entity.monster.ElderGuardian.class));
    }
    @Test void nativeGuardianHuntsSquidAxolotlButNotDolphinAndKeepsMinimumDistance()throws Exception{
        var type=Class.forName("net.minecraft.world.entity.monster.Guardian$GuardianAttackSelector");
        var constructor=type.getDeclaredConstructor(net.minecraft.world.entity.monster.Guardian.class);constructor.setAccessible(true);
        var selector=constructor.newInstance(body("guardian",net.minecraft.world.entity.monster.Guardian.class,0));
        var method=type.getDeclaredMethod("test",LivingEntity.class,net.minecraft.server.level.ServerLevel.class);method.setAccessible(true);
        assertFalse((boolean)method.invoke(selector,body("dolphin",net.minecraft.world.entity.animal.dolphin.Dolphin.class,6),null));
        assertTrue((boolean)method.invoke(selector,body("squid",net.minecraft.world.entity.animal.squid.Squid.class,6),null));
        assertTrue((boolean)method.invoke(selector,body("axolotl",net.minecraft.world.entity.animal.axolotl.Axolotl.class,6),null));
        assertFalse((boolean)method.invoke(selector,body("squid",net.minecraft.world.entity.animal.squid.Squid.class,3),null));
    }
}
