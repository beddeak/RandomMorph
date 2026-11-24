package kr.randommorph.fabric.morph;
import java.util.*;
import kr.randommorph.fabric.ai.SpeciesAI;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
/** Native goal class/selector contracts, separate from actual world navigation/combat. */
class GoalRelationAuditTest {
    @BeforeAll static void init(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private ClassNode type(String name)throws Exception{
        try(var input=getClass().getResourceAsStream("/net/minecraft/"+name+".class")){
            assertNotNull(input,name);var node=new ClassNode();new ClassReader(input).accept(node,0);return node;
        }
    }
    private Set<String> goalClasses(String name)throws Exception{
        var result=new HashSet<String>();
        for(var method:type(name).methods)if(method.name.equals("registerGoals"))for(var instruction:method.instructions){
            if(instruction instanceof LdcInsnNode literal&&literal.cst instanceof Type t)result.add(t.getInternalName());
            if(instruction instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.NEW)result.add(t.desc);
        }
        return result;
    }
    private LivingEntity body(String species,String className)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var entity=(LivingEntity)((sun.misc.Unsafe)field.get(null)).allocateInstance(Class.forName("net.minecraft.world.entity."+className));
        var type=Entity.class.getDeclaredField("type");type.setAccessible(true);type.set(entity,MorphRegistry.find(species).type());return entity;
    }
    @Test void skeletonVariantsMatchNativeWolfClassWithoutIncludingTheHorse()throws Exception{
        for(var entry:Map.of("skeleton","Skeleton","stray","Stray","bogged","Bogged","parched","Parched","wither_skeleton","WitherSkeleton").entrySet())
            assertTrue(SpeciesAI.matches(AbstractSkeleton.class,body(entry.getKey(),"monster.skeleton."+entry.getValue())),entry.getKey());
        assertFalse(SpeciesAI.matches(AbstractSkeleton.class,body("skeleton_horse","animal.equine.SkeletonHorse")));
    }
    @Test void nativeWolfPreySelectorHuntsSheepRabbitFoxAndLeavesCowAndPig()throws Exception{
        for(var entry:Map.of("sheep","animal.sheep.Sheep","rabbit","animal.rabbit.Rabbit","fox","animal.fox.Fox").entrySet())
            assertTrue(Wolf.PREY_SELECTOR.test(body(entry.getKey(),entry.getValue()),null),entry.getKey());
        for(var entry:Map.of("cow","animal.cow.Cow","pig","animal.pig.Pig").entrySet())
            assertFalse(Wolf.PREY_SELECTOR.test(body(entry.getKey(),entry.getValue()),null),entry.getKey());
    }
    @Test void skeletonAvoidsWolfButSheepDoesNotGainPredatorSpecificAvoidance()throws Exception{
        var skeleton=goalClasses("world/entity/monster/skeleton/AbstractSkeleton");
        assertTrue(skeleton.containsAll(Set.of("net/minecraft/world/entity/animal/wolf/Wolf","net/minecraft/world/entity/ai/goal/AvoidEntityGoal")));
        assertFalse(goalClasses("world/entity/animal/sheep/Sheep").contains("net/minecraft/world/entity/ai/goal/AvoidEntityGoal"));
    }
    @Test void creeperAndSpiderKeepTheirNativeTypedFearCandidates()throws Exception{
        assertTrue(goalClasses("world/entity/monster/Creeper").containsAll(Set.of("net/minecraft/world/entity/animal/feline/Cat","net/minecraft/world/entity/animal/feline/Ocelot")));
        assertTrue(goalClasses("world/entity/monster/spider/Spider").contains("net/minecraft/world/entity/animal/armadillo/Armadillo"));
    }
    @Test void nativePlayerSearchHasDistinctUnboxedBranch()throws Exception{
        var method=type("world/entity/ai/goal/target/NearestAttackableTargetGoal").methods.stream().filter(m->m.name.equals("findTarget")).findFirst().orElseThrow();
        var calls=new HashSet<String>();for(var i:method.instructions)if(i instanceof MethodInsnNode call)calls.add(call.name);
        assertTrue(calls.containsAll(Set.of("getNearestPlayer","getNearestEntity","getEyeY","getTargetSearchArea")));
    }
    @Test void nativeAvoidStopLeavesNavigationForTheBridgeToClean()throws Exception{
        var method=type("world/entity/ai/goal/AvoidEntityGoal").methods.stream().filter(m->m.name.equals("stop")).findFirst().orElseThrow();
        assertFalse(java.util.stream.StreamSupport.stream(method.instructions.spliterator(),false).anyMatch(i->i instanceof MethodInsnNode call&&call.name.equals("stop")));
        assertTrue(java.util.stream.StreamSupport.stream(method.instructions.spliterator(),false).anyMatch(i->i instanceof FieldInsnNode field&&field.name.equals("toAvoid")));
    }
}
