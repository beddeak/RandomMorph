package kr.randommorph.verify;

import java.lang.reflect.*;
import java.util.*;
import kr.randommorph.fabric.ability.Active;
import kr.randommorph.fabric.morph.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.*;

/** Native ID/item extraction fixtures, with no entity constructor, level, GPU or Minecraft started. */
public final class DetachedBodyProbe {
    private static LivingEntity unassigned(Class<? extends LivingEntity> type)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return (LivingEntity)((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
    }
    @SuppressWarnings("unchecked")
    private static EntityType<LivingEntity> fixtureType(LivingEntity body)throws Exception{
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var type=(EntityType<LivingEntity>)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(EntityType.class);
        var field=EntityType.class.getDeclaredField("factory");field.setAccessible(true);
        field.set(type,(EntityType.EntityFactory<LivingEntity>)(t,level)->{
            if(level!=null)throw new AssertionError("No level is allowed in this fixture");return body;
        });
        return type;
    }
    public static void run(String side)throws Exception{
        Object resolver=null,renderState=null;Method update=null;
        if(side.equals("CLIENT")){
            // Reflection keeps client classes out of the SERVER probe's linkage.
            var resolverType=Class.forName("net.minecraft.client.renderer.item.ItemModelResolver");
            var stateType=Class.forName("net.minecraft.client.renderer.item.ItemStackRenderState");
            resolver=resolverType.getConstructor(Class.forName("net.minecraft.client.resources.model.ModelManager")).newInstance(new Object[]{null});
            renderState=stateType.getConstructor().newInstance();
            update=resolverType.getMethod("updateForLiving",stateType,ItemStack.class,ItemDisplayContext.class,LivingEntity.class);
        }
        int fixtures=0,consumers=0;
        for(var type:List.of(Vex.class,CopperGolem.class)){
            var body=unassigned(type);NativeEntityFixtures.initializeData(body);var owner=unassigned(Vex.class);owner.setId(41+fixtures);
            try{body.getId();throw new AssertionError("An unassigned native body must fail");}
            catch(IllegalStateException expected){
                if(!expected.getMessage().equals("Tried to access entity ID before ID assignment"))throw expected;
            }
            if(update!=null){
                try{update.invoke(resolver,renderState,ItemStack.EMPTY,ItemDisplayContext.HEAD,body);throw new AssertionError("Unassigned item extraction must fail");}
                catch(InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalStateException))throw expected;}
            }
            var form=new MorphProfile("probe_body","fixture",fixtureType(body),Set.of(),Active.NONE,Active.NONE);
            if(MorphBodies.create(form,owner)!=body||body.getId()!=owner.getId()||body.hashCode()!=owner.getId())throw new AssertionError("Detached ID was not assigned by the real body factory");
            if(update!=null){update.invoke(resolver,renderState,ItemStack.EMPTY,ItemDisplayContext.HEAD,body);consumers++;}
            fixtures++;
        }
        System.out.println("RANDOMMORPH_DETACHED_IDS_OK side="+side+" fixtures="+fixtures+" item_consumers="+consumers+"; no entity constructor/world/GPU/game was started");
    }
}
