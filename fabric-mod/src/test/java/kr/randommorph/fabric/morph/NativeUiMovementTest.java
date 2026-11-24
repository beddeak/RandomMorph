package kr.randommorph.fabric.morph;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class NativeUiMovementTest {
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void maxHealthUsesSyncedNativeHeartAttribute(){assertTrue(Attributes.MAX_HEALTH.value().isClientSyncable());assertEquals(500,DefaultAttributes.getSupplier(MorphRegistry.find("warden").type()).getBaseValue(Attributes.MAX_HEALTH));assertEquals(40,DefaultAttributes.getSupplier(MorphRegistry.find("enderman").type()).getBaseValue(Attributes.MAX_HEALTH));}
    @Test void turtleLandAccountsForNativeGroundControl(){double turtle=NativeMovement.landSpeed("turtle",.25);assertEquals(Math.pow(.875*.06+.125*.25,2),turtle,1e-12);assertTrue(turtle<.01);assertTrue(turtle<NativeMovement.landSpeed("cow",.2));}
    @Test void movementUsesNativeMobForwardGain(){assertEquals(.23*.23,NativeMovement.landSpeed("zombie",.23),1e-12);}
    @Test void fishAndDolphinHaveDifferentTravelMechanisms(){var fish=MorphRegistry.find("cod");var dolphin=MorphRegistry.find("dolphin");assertEquals(.01*NativeMovement.rawSpeed(fish),NativeMovement.swimAcceleration(fish),1e-6);assertEquals(.02*Math.pow(NativeMovement.rawSpeed(dolphin),2),NativeMovement.swimAcceleration(dolphin),1e-6);assertTrue(NativeMovement.swimAcceleration(fish)<NativeMovement.swimAcceleration(dolphin));}
    @Test void nautilusDoesNotWalkLikeAPlayer(){assertEquals(0,NativeMovement.landSpeed("nautilus",1));assertEquals(.011,NativeMovement.swimAcceleration(MorphRegistry.find("nautilus")),1e-6);}
    @Test void villagersAndFrogsDoNotUseUnscaledHighBaseSpeeds(){
        double villager=NativeMovement.rawSpeed(MorphRegistry.find("villager")),frog=NativeMovement.rawSpeed(MorphRegistry.find("frog"));
        assertEquals(.5,villager);assertEquals(1,frog);
        assertEquals(.0625,NativeMovement.landSpeed("villager",villager),1e-12);
        assertEquals(.01,NativeMovement.landSpeed("frog",frog),1e-12);
        assertTrue(NativeMovement.landSpeed("villager",villager)<.1);
        assertTrue(NativeMovement.landSpeed("frog",frog)<NativeMovement.landSpeed("cow",.2));
    }
    @Test void traderUsesStrollingAndAxolotlCrawlsSlowly(){
        assertEquals(.030625,NativeMovement.landSpeed("wandering_trader",.5),1e-12);
        assertEquals(.0135,NativeMovement.landSpeed("axolotl",1),1e-12);
        assertEquals(.012,NativeMovement.swimAcceleration(MorphRegistry.find("axolotl")),1e-6);
        assertTrue(NativeMovement.landSpeed("axolotl",1)<NativeMovement.landSpeed("cow",.2));
    }
    @Test void vexIsSlightlySlowerThanPreviousSeventyPercent(){
        var p=MorphRegistry.find("vex");assertEquals(NativeMovement.rawSpeed(p)*.1*.6,NativeMovement.flightSpeed(p),1e-6);
    }
    @Test void speciesMovementIsFinite(){for(var p:MorphRegistry.profiles()){assertTrue(Double.isFinite(NativeMovement.landSpeed(p.id(),NativeMovement.rawSpeed(p))));assertTrue(Float.isFinite(NativeMovement.swimAcceleration(p)));assertTrue(Float.isFinite(NativeMovement.flightSpeed(p)));}}
}
