package kr.randommorph.verify;
import java.lang.reflect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.syncher.SynchedEntityData;
/** Worldless verification only. Uses each real entity's native data definitions. */
public final class NativeEntityFixtures {
    private NativeEntityFixtures(){}
    public static void initializeData(Entity entity)throws Exception{
        var builder=new SynchedEntityData.Builder(entity);
        // Entity's eight base entries are defined in its constructor, not in defineSynchedData.
        var entries=java.util.Map.of("DATA_SHARED_FLAGS_ID",(Object)(byte)0,"DATA_AIR_SUPPLY_ID",300,
                "DATA_CUSTOM_NAME_VISIBLE",false,"DATA_CUSTOM_NAME",java.util.Optional.empty(),"DATA_SILENT",false,
                "DATA_NO_GRAVITY",false,"DATA_POSE",net.minecraft.world.entity.Pose.STANDING,"DATA_TICKS_FROZEN",0);
        for(var entry:entries.entrySet()){
            var f=Entity.class.getDeclaredField(entry.getKey());f.setAccessible(true);
            define(builder,f.get(null),entry.getValue());
        }
        var define=Entity.class.getDeclaredMethod("defineSynchedData",SynchedEntityData.Builder.class);define.setAccessible(true);define.invoke(entity,builder);
        var data=Entity.class.getDeclaredField("entityData");data.setAccessible(true);data.set(entity,builder.build());
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void define(SynchedEntityData.Builder builder,Object key,Object value){builder.define((net.minecraft.network.syncher.EntityDataAccessor)key,value);}
    @SuppressWarnings("unchecked") public static <T>T allocate(Class<T> type)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);return (T)((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
    }
}
