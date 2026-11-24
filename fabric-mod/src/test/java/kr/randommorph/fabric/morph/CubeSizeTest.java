package kr.randommorph.fabric.morph;

import java.util.*;
import kr.randommorph.fabric.core.MorphFlags;
import net.minecraft.core.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.cubemob.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Execute native size/attribute methods on detached fixtures; no constructor, world or AI runs. */
class CubeSizeTest {
    private static final Map<AbstractCubeMob,Map<net.minecraft.core.Holder<Attribute>,AttributeInstance>> ATTRIBUTES=new IdentityHashMap<>();
    @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @AfterEach void clearFixtures(){ATTRIBUTES.clear();}
    private static AttributeInstance attribute(AbstractCubeMob cube,net.minecraft.core.Holder<Attribute> type){
        return ATTRIBUTES.get(cube).computeIfAbsent(type,key->new AttributeInstance(key,ignored->{}));
    }
    public static class SlimeFixture extends Slime {
        public SlimeFixture(){super(EntityTypes.SLIME,null);}
        @Override protected void reapplyPosition(){}
        @Override public void refreshDimensions(){}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){}
        @Override public AttributeInstance getAttribute(net.minecraft.core.Holder<Attribute> type){return attribute(this,type);}
        @Override public void setHealth(float value){fail("Flag synchronization must not refill health");}
    }
    public static class MagmaFixture extends MagmaCube {
        double jumpY;
        public MagmaFixture(){super(EntityTypes.MAGMA_CUBE,null);}
        @Override protected void reapplyPosition(){}
        @Override public void refreshDimensions(){}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){}
        @Override public AttributeInstance getAttribute(net.minecraft.core.Holder<Attribute> type){return attribute(this,type);}
        @Override public void setHealth(float value){fail("Flag synchronization must not refill health");}
        @Override protected float getJumpPower(){return (float)getAttribute(Attributes.JUMP_STRENGTH).getBaseValue();}
        @Override public net.minecraft.world.phys.Vec3 getDeltaMovement(){return net.minecraft.world.phys.Vec3.ZERO;}
        @Override public void setDeltaMovement(double x,double y,double z){jumpY=y;}
    }
    public static class SulfurFixture extends SulfurCube {
        public SulfurFixture(){super(EntityTypes.SULFUR_CUBE,null);}
        @Override protected void reapplyPosition(){}
        @Override public void refreshDimensions(){}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){}
        @Override public AttributeInstance getAttribute(net.minecraft.core.Holder<Attribute> type){return attribute(this,type);}
        @Override public void setHealth(float value){fail("Flag synchronization must not refill health");}
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void define(SynchedEntityData.Builder builder,Object key,Object value){builder.define((EntityDataAccessor)key,value);}
    private static AbstractCubeMob fixture(String species) throws ReflectiveOperationException {
        var raw=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");raw.setAccessible(true);
        Class<? extends AbstractCubeMob> type=switch(species){case "slime"->SlimeFixture.class;case "magma_cube"->MagmaFixture.class;default->SulfurFixture.class;};
        var cube=(AbstractCubeMob)((sun.misc.Unsafe)raw.get(null)).allocateInstance(type);
        ATTRIBUTES.put(cube,new HashMap<>());
        var entityType=Entity.class.getDeclaredField("type");entityType.setAccessible(true);entityType.set(cube,MorphRegistry.find(species).type());
        var builder=new SynchedEntityData.Builder(cube);
        for(var entry:Map.of("DATA_SHARED_FLAGS_ID",(Object)(byte)0,"DATA_AIR_SUPPLY_ID",300,
                "DATA_CUSTOM_NAME_VISIBLE",false,"DATA_CUSTOM_NAME",Optional.empty(),"DATA_SILENT",false,
                "DATA_NO_GRAVITY",false,"DATA_POSE",Pose.STANDING,"DATA_TICKS_FROZEN",0).entrySet()){
            var key=Entity.class.getDeclaredField(entry.getKey());key.setAccessible(true);define(builder,key.get(null),entry.getValue());
        }
        var define=Entity.class.getDeclaredMethod("defineSynchedData",SynchedEntityData.Builder.class);define.setAccessible(true);define.invoke(cube,builder);
        var data=Entity.class.getDeclaredField("entityData");data.setAccessible(true);data.set(cube,builder.build());
        return cube;
    }
    @Test void sizesRoundtripWithoutTouchingFaceColorGrazingOrAbilityFlags(){
        for(int size:new int[]{1,2,4})for(var face:Direction.values())for(int color=0;color<16;color++){
            int original=MorphFlags.withColor(MorphFlags.withFace(MorphFlags.withGrazing(127,40),face),color);
            int flags=MorphFlags.withCubeSize(original,size);
            assertTrue(MorphFlags.valid(flags));assertEquals(size,MorphFlags.cubeSize(flags));
            assertEquals(original,MorphFlags.withCubeSize(flags,2));assertEquals(face,MorphFlags.face(flags));
            assertEquals(color,MorphFlags.color(flags));assertEquals(40,MorphFlags.grazing(flags));assertEquals(127,flags&127);
        }
        assertEquals(2,MorphFlags.cubeSize(0));
    }
    @Test void invalidSizesAndReservedCodesAreRejected(){
        for(int size:new int[]{-1,0,3,5,127,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->MorphFlags.withCubeSize(0,size));
        assertFalse(MorphFlags.valid(3<<20));assertFalse(MorphFlags.valid(1<<22));
        assertThrows(IllegalArgumentException.class,()->MorphFlags.cubeSize(3<<20));
    }
    @Test void allThreeSpeciesUseNativeSizeHealthMovementAndDimensions() throws ReflectiveOperationException {
        for(String species:new String[]{"slime","magma_cube","sulfur_cube"}){
            var cube=fixture(species);var profile=MorphRegistry.find(species);assertTrue(profile.cube());
            for(int size:new int[]{2,1,4,2}){
                int flags=MorphFlags.withCubeSize(0,size);MorphBodies.flags(cube,flags);
                assertEquals(size,cube.getSize());assertNull(cube.level());
                assertEquals(species.equals("sulfur_cube")?4*size:size*size,cube.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
                assertEquals(NativeMovement.cubeSpeed(size),cube.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue());
                var nativeDimensions=cube.getDefaultDimensions(Pose.STANDING);var dimensions=profile.dimensions(flags);
                assertEquals(nativeDimensions.width(),dimensions.width());assertEquals(nativeDimensions.height(),dimensions.height());
                if(!species.equals("sulfur_cube"))assertEquals(size,cube.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
                if(species.equals("magma_cube"))assertEquals(size*3,cube.getAttribute(Attributes.ARMOR).getBaseValue());
            }
            assertEquals(cube.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),NativeMovement.rawSpeed(profile));
        }
    }
    @Test void repeatedFlagPacketsDoNotReapplySizeOrRefillNativeAttributes() throws ReflectiveOperationException {
        var cube=fixture("slime");int flags=MorphFlags.withCubeSize(0,4);MorphBodies.flags(cube,flags);
        cube.getAttribute(Attributes.MAX_HEALTH).setBaseValue(13);
        cube.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.75);
        for(int i=0;i<40;i++)MorphBodies.flags(cube,flags);
        assertEquals(13,cube.getAttribute(Attributes.MAX_HEALTH).getBaseValue());assertEquals(.75,cube.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue());
    }
    @Test void cubeBitsDoNotResizeOtherSpecies(){
        for(String species:new String[]{"cow","pufferfish","ender_dragon"}){
            var profile=MorphRegistry.find(species);assertFalse(profile.cube());
            var normal=profile.dimensions(0);var flagged=profile.dimensions(MorphFlags.withCubeSize(0,4));
            assertEquals(normal.width(),flagged.width());assertEquals(normal.height(),flagged.height());
            assertEquals(normal.eyeHeight(),flagged.eyeHeight());
            assertEquals(normal.makeBoundingBox(net.minecraft.world.phys.Vec3.ZERO),flagged.makeBoundingBox(net.minecraft.world.phys.Vec3.ZERO));
        }
    }
    @Test void cubeJumpMatchesNativeMagmaSizeGainWithoutSlimeHighJumpFloor() throws ReflectiveOperationException {
        for(String species:new String[]{"slime","magma_cube","sulfur_cube"}){
            var profile=MorphRegistry.find(species);var cube=fixture(species);
            for(int size:new int[]{1,2,4})for(double base:new double[]{Attributes.JUMP_STRENGTH.value().getDefaultValue(),.8}){
                MorphBodies.flags(cube,MorphFlags.withCubeSize(0,size));cube.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(base);
                double actual=MorphAttributes.jumpStrength(profile,cube,base);
                if(cube instanceof MagmaFixture magma){magma.jumpFromGround();assertEquals(magma.jumpY,actual);}
                else assertEquals(base,actual);
            }
        }
        assertEquals(.9,MorphAttributes.jumpStrength(MorphRegistry.find("breeze"),null,.42));
    }
}
