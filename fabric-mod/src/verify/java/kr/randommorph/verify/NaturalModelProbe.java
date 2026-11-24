package kr.randommorph.verify;
import kr.randommorph.fabric.core.MorphFlags;
import kr.randommorph.fabric.morph.MorphBodies;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
/** Executes native colour/sheared/charged data writes after real Mixin transformation. */
public final class NaturalModelProbe {
    public static class Wool extends Sheep {
        private Wool(EntityType<? extends Sheep> type,Level level){super(type,level);}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data){}
    }
    public static class Fire extends Blaze {
        private Fire(EntityType<? extends Blaze> type,Level level){super(type,level);}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data){}
    }
    public static void run(String side)throws Exception{
        var sheep=NativeEntityFixtures.allocate(Wool.class);NativeEntityFixtures.initializeData(sheep);
        for(int color=0;color<16;color++)for(boolean sheared:new boolean[]{false,true}){
            MorphBodies.flags(sheep,MorphFlags.withColor(sheared?MorphFlags.SHEARED:0,color));
            if(sheep.getColor().getId()!=color||sheep.isSheared()!=sheared)throw new AssertionError("Native wool state not copied");
        }
        var blaze=NativeEntityFixtures.allocate(Fire.class);NativeEntityFixtures.initializeData(blaze);
        for(int flags:new int[]{16,0}){MorphBodies.flags(blaze,flags);if(blaze.isOnFire()!=((flags&16)!=0))throw new AssertionError("Native charge state not copied");}
        System.out.println("RANDOMMORPH_NATURAL_MODEL_OK side="+side+" cases=34; native synced data, no world/game started");
    }
}
