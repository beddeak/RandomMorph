package kr.randommorph.fabric.morph;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import kr.randommorph.fabric.ability.Active;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Vex;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Zero-initialized ID fixtures only; no entity constructor, level, Minecraft or renderer is started. */
class DetachedBodyTest {
    @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private static Vex unassigned(){
        try{
            var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
            var body=(Vex)((sun.misc.Unsafe)field.get(null)).allocateInstance(Vex.class);
            var builder=new net.minecraft.network.syncher.SynchedEntityData.Builder(body);
            for(var entry:java.util.Map.of("DATA_SHARED_FLAGS_ID",(Object)(byte)0,"DATA_AIR_SUPPLY_ID",300,
                    "DATA_CUSTOM_NAME_VISIBLE",false,"DATA_CUSTOM_NAME",java.util.Optional.empty(),"DATA_SILENT",false,
                    "DATA_NO_GRAVITY",false,"DATA_POSE",Pose.STANDING,"DATA_TICKS_FROZEN",0).entrySet()){
                var base=Entity.class.getDeclaredField(entry.getKey());base.setAccessible(true);define(builder,base.get(null),entry.getValue());
            }
            var define=Entity.class.getDeclaredMethod("defineSynchedData",net.minecraft.network.syncher.SynchedEntityData.Builder.class);define.setAccessible(true);define.invoke(body,builder);
            var data=Entity.class.getDeclaredField("entityData");data.setAccessible(true);data.set(body,builder.build());return body;
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void define(net.minecraft.network.syncher.SynchedEntityData.Builder b,Object key,Object value){b.define((net.minecraft.network.syncher.EntityDataAccessor)key,value);}
    private static Vex owner(int id){var owner=unassigned();owner.setId(id);return owner;}
    @SuppressWarnings("unchecked")
    private static MorphProfile profile(EntityType.EntityFactory<Vex> factory){
        try{
            // An isolated type fixture avoids adding entries to the frozen native registry.
            var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
            var type=(EntityType<Vex>)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(EntityType.class);
            var field=EntityType.class.getDeclaredField("factory");field.setAccessible(true);field.set(type,factory);
            return new MorphProfile("test_body","test",type,Set.of(),Active.NONE,Active.NONE);
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    @Test void unassignedNativeEntityReproducesTheReportedError(){
        var error=assertThrows(IllegalStateException.class,()->unassigned().getId());
        assertEquals("Tried to access entity ID before ID assignment",error.getMessage());
    }
    @Test void factoryReturnsAnAssignedDetachedBodyWithoutAccessingAWorld(){
        var body=unassigned();var actor=owner(41);var created=new AtomicInteger();
        var form=profile((type,level)->{assertNull(level);created.incrementAndGet();return body;});
        assertSame(body,MorphBodies.create(form,actor));
        assertEquals(41,body.getId());assertEquals(41,body.hashCode());assertEquals(1,created.get());
        assertNull(body.level());
    }
    @Test void differentPlayersAndReplacementEntitiesKeepIndependentIds(){
        var form=profile((type,level)->unassigned());
        var first=MorphBodies.create(form,owner(41));var second=MorphBodies.create(form,owner(72));
        var replacement=MorphBodies.create(form,owner(93));
        assertEquals(41,first.getId());assertEquals(72,second.getId());assertEquals(93,replacement.getId());
        assertNotEquals(first,second);assertNotEquals(first,replacement);
    }
    @Test void missingOwnerIdFailsBeforeCallingTheEntityFactory(){
        var created=new AtomicInteger();var form=profile((type,level)->{created.incrementAndGet();return unassigned();});
        assertThrows(IllegalStateException.class,()->MorphBodies.create(form,unassigned()));
        assertEquals(0,created.get());
    }
    @Test void nullFactoryResultFailsWithoutPublishingAnInvalidBody(){
        var form=profile((type,level)->null);
        var error=assertThrows(IllegalStateException.class,()->MorphBodies.create(form,owner(41)));
        assertEquals("No native model: test_body",error.getMessage());
    }
}
