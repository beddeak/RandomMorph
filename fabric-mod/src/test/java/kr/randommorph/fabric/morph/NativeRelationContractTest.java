package kr.randommorph.fabric.morph;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the native class/tag distinctions the bridge relies on. Does not simulate a world or AI tick. */
class NativeRelationContractTest {
    private Set<String> values(String kind,String name)throws Exception{
        try(var input=getClass().getResourceAsStream("/data/minecraft/tags/"+kind+"/"+name+".json")){
            assertNotNull(input,name);var out=new HashSet<String>();
            for(var value:JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("values"))out.add(value.getAsString());
            return out;
        }
    }
    @Test void wolfUsesAbstractSkeletonRatherThanBroadSkeletonTag()throws Exception{
        assertTrue(values("entity_type","skeletons").contains("minecraft:skeleton_horse"));
        try(var input=getClass().getResourceAsStream("/net/minecraft/world/entity/animal/wolf/Wolf.class")){
            assertNotNull(input);var node=new ClassNode();new ClassReader(input).accept(node,0);boolean skeletonClass=false;
            for(var method:node.methods)if(method.name.equals("registerGoals"))for(var instruction:method.instructions)
                if(instruction instanceof LdcInsnNode ldc&&ldc.cst instanceof Type type&&type.getInternalName().equals("net/minecraft/world/entity/monster/skeleton/AbstractSkeleton"))skeletonClass=true;
            assertTrue(skeletonClass,"Native wolf skeleton candidate must remain a class selector");
        }
    }
    @Test void frogFoodDoesNotIncludeSulfurCube()throws Exception{assertEquals(Set.of("minecraft:slime","minecraft:magma_cube"),values("entity_type","frog_food"));}
    @Test void aquaticClassificationDoesNotErasePufferfishException()throws Exception{
        var list=values("entity_type","not_scary_for_pufferfish");assertTrue(list.containsAll(Set.of("minecraft:nautilus","minecraft:zombie_nautilus","minecraft:sulfur_cube")));assertFalse(list.contains("minecraft:axolotl"));
    }
    @Test void parchedDoesNotInheritWindChargeNoAngerTag()throws Exception{var list=values("entity_type","no_anger_from_wind_charge");assertTrue(list.contains("minecraft:skeleton"));assertFalse(list.contains("minecraft:parched"));}
    @Test void illagerFriendshipAndWolfRetaliationKeepNativeExceptions()throws Exception{
        assertEquals(Set.of("#minecraft:illager"),values("entity_type","illager_friends"));
        assertEquals(Set.of("minecraft:pillager","minecraft:vindicator","minecraft:evoker","minecraft:illusioner"),values("entity_type","illager"));
        assertEquals(Set.of("minecraft:sulfur_cube_hot"),values("damage_type","no_wolf_retaliation"));
    }
}
