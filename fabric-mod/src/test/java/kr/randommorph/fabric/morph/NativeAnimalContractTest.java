package kr.randommorph.fabric.morph;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pinned native action contracts; no world, entity or player is constructed. */
class NativeAnimalContractTest {
    private ClassNode type(String name)throws IOException{
        try(var input=getClass().getResourceAsStream("/net/minecraft/"+name+".class")){
            assertNotNull(input);var node=new ClassNode();new ClassReader(input).accept(node,0);return node;
        }
    }
    private MethodNode method(String type,String name)throws IOException{
        var methods=type(type).methods.stream().filter(m->m.name.equals(name)).toList();assertEquals(1,methods.size());return methods.getFirst();
    }
    private Set<String> fields(MethodNode method){
        var fields=new HashSet<String>();for(var instruction:method.instructions)if(instruction instanceof FieldInsnNode field)fields.add(field.name);return fields;
    }
    private Set<String> calls(MethodNode method){
        var calls=new HashSet<String>();for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call)calls.add(call.name);return calls;
    }
    private Set<Integer> ints(MethodNode method){
        var values=new HashSet<Integer>();
        for(var instruction:method.instructions){
            if(instruction instanceof IntInsnNode integer)values.add(integer.operand);
            if(instruction instanceof LdcInsnNode literal&&literal.cst instanceof Integer integer)values.add(integer);
            if(instruction.getOpcode()>=Opcodes.ICONST_M1&&instruction.getOpcode()<=Opcodes.ICONST_5)values.add(instruction.getOpcode()-Opcodes.ICONST_0);
        }
        return values;
    }
    @Test void shulkerRawPeekHasAudioSideEffectsAndClosedShellRejectsArrowsOnly()throws Exception{
        assertTrue(calls(method("world/entity/monster/Shulker","setRawPeekAmount")).containsAll(Set.of("playSound","gameEvent")));
        var damage=method("world/entity/monster/Shulker","hurtServer");
        assertTrue(calls(damage).containsAll(Set.of("getDirectEntity","isClosed","nextInt","teleportSomewhere")));
        assertTrue(ints(damage).contains(4));
        assertTrue(java.util.stream.StreamSupport.stream(damage.instructions.spliterator(),false)
                .anyMatch(i->i instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.INSTANCEOF&&t.desc.equals("net/minecraft/world/entity/projectile/arrow/AbstractArrow")));
    }
    @Test void elderUsesNativeMiningFatigueThreeFiveMinutesAndOneMinutePulse()throws Exception{
        var step=method("world/entity/monster/ElderGuardian","customServerAiStep");
        assertTrue(ints(step).containsAll(Set.of(1200,6000)));
        assertTrue(fields(step).contains("MINING_FATIGUE"));
        assertTrue(type("world/entity/monster/ElderGuardian").methods.stream().anyMatch(m->m.name.startsWith("lambda$customServerAiStep$")&&fields(m).contains("GUARDIAN_ELDER_EFFECT")));
        assertTrue(calls(step).contains("addEffectToPlayersAround"));
    }
    @Test void blazeNativeRangedCadenceHasChargeThreeShotsAndRecovery()throws Exception{
        var step=method("world/entity/monster/Blaze$BlazeAttackGoal","tick");
        assertTrue(ints(step).containsAll(Set.of(60,6,100)));
        assertTrue(calls(step).containsAll(Set.of("setCharged","levelEvent","addFreshEntity")));
        assertTrue(fields(step).contains("attackStep"));
    }
    @Test void mooshroomShearsUseNativeLootAndDyesUseNativeComponent()throws Exception{
        assertTrue(fields(method("world/entity/animal/cow/MushroomCow","lambda$shear$0")).contains("SHEAR_MOOSHROOM"));
        assertTrue(calls(method("world/entity/animal/cow/MushroomCow","shear")).contains("convertTo"));
        assertTrue(fields(method("world/item/DyeItem","interactLivingEntity")).contains("DYE"));
        assertTrue(calls(method("world/item/DyeItem","interactLivingEntity")).containsAll(Set.of("setColor","shrink")));
    }
    @Test void breezeUsesBothWindExceptionsAndNativeDeflectionStrategy()throws Exception{
        assertTrue(fields(method("world/entity/monster/breeze/Breeze","deflection")).containsAll(Set.of("BREEZE_WIND_CHARGE","WIND_CHARGE","NONE","PROJECTILE_DEFLECTION","DEFLECTS_PROJECTILES")));
        assertFalse(MorphRegistry.find("breeze").has(kr.randommorph.fabric.ability.Trait.PROJECTILE_IMMUNE));
    }
    @Test void nativeCollisionDeflectionRetainsOriginalProjectileOwner()throws Exception{
        var method=method("world/entity/projectile/Projectile","hitTargetOrDeflectSelf");
        assertTrue(fields(method).contains("owner"));assertTrue(calls(method).containsAll(Set.of("deflection","deflect","onHit")));
    }
    @Test void beeStingAndFatalityUseNativeCounterPoisonAndGenericDamage()throws Exception{
        assertTrue(calls(method("world/entity/animal/bee/Bee","doHurtTarget")).containsAll(Set.of("sting","hurtServer","setStingerCount","setHasStung","addEffect")));
        var fate=method("world/entity/animal/bee/Bee","customServerAiStep");
        assertTrue(ints(fate).contains(1200));assertTrue(calls(fate).containsAll(Set.of("hasStung","nextInt","generic","getHealth","hurtServer")));
    }
    @Test void sheepUsesShearingLootToolWearAndEdibleTag()throws Exception{
        assertTrue(fields(method("world/entity/animal/sheep/Sheep","shear")).contains("SHEAR_SHEEP"));
        assertTrue(calls(method("world/entity/animal/sheep/Sheep","mobInteract")).contains("hurtAndBreak"));
        var goal=type("world/entity/ai/goal/EatBlockGoal");
        assertTrue(goal.methods.stream().anyMatch(m->fields(m).contains("EDIBLE_FOR_SHEEP")));
        assertTrue(fields(method("world/entity/ai/goal/EatBlockGoal","tick")).containsAll(Set.of("MOB_GRIEFING","GRASS_BLOCK","DIRT")));
    }
    @Test void chickenUsesNativeGiftTableAndFiveToTenMinuteInterval()throws Exception{
        var step=method("world/entity/animal/chicken/Chicken","aiStep");
        assertTrue(fields(step).containsAll(Set.of("CHICKEN_LAY","eggTime")));
        assertTrue(calls(step).containsAll(Set.of("dropFromGiftLootTable","nextInt","isChickenJockey","isBaby")));
        assertTrue(ints(step).contains(6000));
    }
    @Test void milkUsesNativeFilledContainerAndPlayerSaveHooksArePinned()throws Exception{
        assertTrue(fields(method("world/entity/animal/cow/AbstractCow","mobInteract")).containsAll(Set.of("BUCKET","MILK_BUCKET")));
        assertTrue(calls(method("world/entity/animal/cow/AbstractCow","mobInteract")).contains("createFilledResult"));
        var player=type("world/entity/player/Player");
        for(var name:List.of("createAttackSource","doSweepAttack","stabAttack","readAdditionalSaveData","addAdditionalSaveData"))assertTrue(player.methods.stream().anyMatch(m->m.name.equals(name)),name);
    }
    @Test void normalAndSpearAttacksBothConsumeStingOnlyAfterTheAcceptedPrimaryHit()throws Exception{
        for(var name:List.of("attack","stabAttack")){
            int primaryHits=0;
            for(var instruction:method("world/entity/player/Player",name).instructions)
                if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/world/entity/Entity")&&
                        call.name.equals("hurtOrSimulate")&&call.desc.equals("(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))primaryHits++;
            assertEquals(1,primaryHits,name);
        }
    }
}
