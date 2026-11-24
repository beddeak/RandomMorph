package kr.randommorph.fabric.ai;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pinned vanilla predicates and bytecode/data contracts; no world, AI tick or navigation runs. */
class ProtectionRelationAuditTest {
    @BeforeAll static void init(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}

    private ClassNode nativeClass(String name)throws Exception{
        try(var input=getClass().getResourceAsStream("/net/minecraft/"+name+".class")){
            assertNotNull(input,name);var node=new ClassNode();new ClassReader(input).accept(node,0);return node;
        }
    }
    private MethodNode method(String type,String name)throws Exception{
        return nativeClass(type).methods.stream().filter(m->m.name.equals(name)&&!((m.access&Opcodes.ACC_BRIDGE)!=0)).findFirst().orElseThrow();
    }
    private static boolean calls(MethodNode method,String owner,String name){
        for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/"+owner)&&call.name.equals(name))return true;
        return false;
    }
    private static Set<String> testedClasses(MethodNode method){
        var result=new HashSet<String>();
        for(var instruction:method.instructions)if(instruction instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.INSTANCEOF)result.add(type.desc);
        return result;
    }
    private static boolean field(MethodNode method,String owner,String name){
        for(var instruction:method.instructions)if(instruction instanceof FieldInsnNode ref&&ref.owner.equals("net/minecraft/"+owner)&&ref.name.equals(name))return true;
        return false;
    }
    private static boolean guarded(MethodNode method,String name){
        for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.name.equals(name)){
            var next=instruction.getNext();while(next!=null&&next.getOpcode()<0)next=next.getNext();
            if(next instanceof JumpInsnNode)return true;
        }
        return false;
    }
    private Set<String> tag(String kind,String name)throws Exception{
        try(var input=getClass().getResourceAsStream("/data/minecraft/tags/"+kind+"/"+name+".json")){
            assertNotNull(input,name);var result=new HashSet<String>();
            for(var value:JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("values"))result.add(value.getAsString());
            return result;
        }
    }
    private static LivingEntity body(String className)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return (LivingEntity)((sun.misc.Unsafe)field.get(null)).allocateInstance(Class.forName("net.minecraft.world.entity."+className));
    }

    @Test void wolfNativeOwnerAttackExcludesCreeperGhastArmorStandButAllowsOrdinaryPrey()throws Exception{
        var wolf=(Wolf)body("animal.wolf.Wolf");
        for(var type:List.of("monster.Creeper","monster.Ghast","decoration.ArmorStand"))assertFalse(wolf.wantsToAttack(body(type),null),type);
        for(var type:List.of("animal.cow.Cow","monster.cubemob.Slime"))assertTrue(wolf.wantsToAttack(body(type),null),type);
    }
    @Test void wolfOwnerAttackRetainsTameOwnerPvpAndHorseExceptions()throws Exception{
        var target=method("world/entity/animal/wolf/Wolf","wantsToAttack");
        assertTrue(testedClasses(target).containsAll(Set.of(
                "net/minecraft/world/entity/animal/wolf/Wolf","net/minecraft/world/entity/player/Player",
                "net/minecraft/world/entity/animal/equine/AbstractHorse","net/minecraft/world/entity/TamableAnimal")));
        assertTrue(calls(target,"world/entity/animal/wolf/Wolf","getOwner"));
        assertTrue(guarded(target,"isTame"));assertTrue(guarded(target,"isTamed"));
        assertTrue(calls(target,"world/entity/player/Player","canHarmPlayer"));assertTrue(guarded(target,"canHarmPlayer"));
    }
    @Test void bothOwnerDefenseGoalsRequireTameStandingOwnerNewAttackAndNativeEligibility()throws Exception{
        for(var type:List.of("OwnerHurtByTargetGoal","OwnerHurtTargetGoal")){
            var use=method("world/entity/ai/goal/target/"+type,"canUse");
            assertTrue(guarded(use,"isTame"),type);assertTrue(guarded(use,"isOrderedToSit"),type);
            assertTrue(calls(use,"world/entity/TamableAnimal","getOwner"),type);
            assertTrue(guarded(use,"canAttack"),type);assertTrue(guarded(use,"wantsToAttack"),type);
            assertTrue(calls(use,"world/entity/LivingEntity",type.equals("OwnerHurtByTargetGoal")?"getLastHurtByMobTimestamp":"getLastHurtMobTimestamp"),type);
        }
    }
    @Test void ownerHurtRetaliationRequiresRecentDamageAndUsesExactNoWolfRetaliationTag()throws Exception{
        var use=method("world/entity/ai/goal/target/OwnerHurtByTargetGoal","canUse");
        assertTrue(calls(use,"world/entity/LivingEntity","getLastDamageSource"));
        assertTrue(field(use,"tags/DamageTypeTags","NO_WOLF_RETALIATION"));
        assertTrue(calls(use,"world/damagesource/DamageSource","is"));
        assertEquals(Set.of("minecraft:sulfur_cube_hot"),tag("damage_type","no_wolf_retaliation"));
        assertFalse(field(method("world/entity/ai/goal/target/OwnerHurtTargetGoal","canUse"),"tags/DamageTypeTags","NO_WOLF_RETALIATION"));
    }
    @Test void traderLlamaDefendsOnlyItsLeashedTraderAndChecksFreshAttack()throws Exception{
        var use=method("world/entity/animal/equine/TraderLlama$TraderLlamaDefendWanderingTraderGoal","canUse");
        assertTrue(guarded(use,"isLeashed"));assertTrue(calls(use,"world/entity/animal/equine/Llama","getLeashHolder"));
        assertEquals(Set.of("net/minecraft/world/entity/npc/wanderingtrader/WanderingTrader"),testedClasses(use));
        assertTrue(calls(use,"world/entity/npc/wanderingtrader/WanderingTrader","getLastHurtByMob"));
        assertTrue(calls(use,"world/entity/npc/wanderingtrader/WanderingTrader","getLastHurtByMobTimestamp"));
        assertTrue(guarded(use,"canAttack"));
        for(var instruction:use.instructions)if(instruction instanceof MethodInsnNode call)assertFalse(call.name.startsWith("getNearest")||call.name.equals("getEntitiesOfClass"),"Protection must not find an unrelated nearby trader");
    }
    @Test void bothGolemSelectorsUseEnemyIncludingSlimeWhileOnlyIronExcludesCreeper()throws Exception{
        var slime=body("monster.cubemob.Slime");
        assertTrue(slime instanceof net.minecraft.world.entity.monster.Enemy);
        assertFalse(slime instanceof net.minecraft.world.entity.monster.Monster);
        for(var type:List.of("IronGolem","SnowGolem")){
            var selector=Class.forName("net.minecraft.world.entity.animal.golem."+type).getDeclaredMethod("lambda$registerGoals$0",LivingEntity.class,net.minecraft.server.level.ServerLevel.class);selector.setAccessible(true);
            assertTrue((boolean)selector.invoke(null,slime,null),type);
            assertTrue((boolean)selector.invoke(null,body("monster.skeleton.Skeleton"),null),type);
            assertFalse((boolean)selector.invoke(null,body("animal.cow.Cow"),null),type);
            assertEquals(type.equals("SnowGolem"),(boolean)selector.invoke(null,body("monster.Creeper"),null),type);
        }
    }
    @Test void witherNaturalSelectorUsesWitherFriendsUndeadTagAndAttackability()throws Exception{
        var select=method("world/entity/boss/wither/WitherBoss","lambda$static$0");
        assertTrue(field(select,"tags/EntityTypeTags","WITHER_FRIENDS"));
        assertTrue(calls(select,"world/entity/LivingEntity","is"));assertTrue(guarded(select,"attackable"));
        assertEquals(Set.of("#minecraft:undead"),tag("entity_type","wither_friends"));
        assertEquals(Set.of("#minecraft:skeletons","#minecraft:zombies","minecraft:wither","minecraft:phantom"),tag("entity_type","undead"));
    }
    @Test void ordinaryMobGhastExceptionDoesNotApplyToWardensSeparateTargetRules()throws Exception{
        var normal=method("world/entity/Mob","canAttack");assertTrue(field(normal,"world/entity/EntityTypes","GHAST"));
        assertTrue(calls(normal,"world/entity/LivingEntity","canAttack"));
        var warden=method("world/entity/monster/warden/Warden","canAttack");
        assertTrue(calls(warden,"world/entity/monster/warden/Warden","canTargetEntity"));
        assertFalse(calls(warden,"world/entity/Mob","canAttack"));
        var target=method("world/entity/monster/warden/Warden","canTargetEntity");
        assertFalse(field(target,"world/entity/EntityTypes","GHAST"));
        assertTrue(field(target,"world/entity/EntityTypes","WARDEN"));assertTrue(field(target,"world/entity/EntityTypes","ARMOR_STAND"));
        assertTrue(guarded(target,"isAlliedTo"));assertTrue(guarded(target,"isInvulnerable"));
    }
    @Test void johnnyBroadTargetsStillUseNativeEligibilityAllianceAndBabyVillagerRules()throws Exception{
        var johnny=method("world/entity/monster/illager/Vindicator$VindicatorJohnnyAttackGoal","canUse");
        assertTrue(field(johnny,"world/entity/monster/illager/Vindicator","isJohnny"));
        assertTrue(guarded(johnny,"canUse"));
        assertTrue(calls(johnny,"world/entity/ai/goal/target/NearestAttackableTargetGoal","canUse"));
        assertTrue(calls(method("world/entity/monster/illager/Vindicator$VindicatorJohnnyAttackGoal","lambda$new$0"),"world/entity/LivingEntity","attackable"));
        var illager=method("world/entity/monster/illager/AbstractIllager","canAttack");
        assertTrue(testedClasses(illager).contains("net/minecraft/world/entity/npc/villager/AbstractVillager"));
        assertTrue(guarded(illager,"isBaby"));assertTrue(calls(illager,"world/entity/raid/Raider","canAttack"));
        var conditions=method("world/entity/ai/targeting/TargetingConditions","test");
        assertTrue(guarded(conditions,"canAttack"));assertTrue(guarded(conditions,"isAlliedTo"));
    }
}
