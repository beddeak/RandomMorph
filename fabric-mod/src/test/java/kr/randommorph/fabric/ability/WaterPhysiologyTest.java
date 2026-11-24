package kr.randommorph.fabric.ability;
import kr.randommorph.fabric.MorphConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class WaterPhysiologyTest {
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void dolphinAndAxolotlHaveFifteenSecondsAndSuicideDefault(){
        var c=new MorphConfig();c.validate();assertEquals(300,WaterPhysiology.graceTicks("dolphin",c));assertEquals(300,WaterPhysiology.graceTicks("axolotl",c));assertTrue(c.creeperSuicide);
    }
    @Test void nativeBubbleScaleDrainsContinuouslyToEmptyAtFifteenSeconds(){
        for(int maximum:new int[]{4800,6000}){
            int previous=maximum;for(int elapsed=0;elapsed<=320;elapsed++){
                int air=WaterPhysiology.displayAir(elapsed,300,maximum);assertTrue(air>=0&&air<=previous);previous=air;
                if(elapsed==150)assertEquals(maximum/2,air);if(elapsed>=300)assertEquals(0,air);
            }
        }
    }
    @Test void dolphinStillShowsLungAirUnderwaterAndMoistureOnLand(){
        assertTrue(WaterPhysiology.showAir("dolphin",false,false,false));assertTrue(WaterPhysiology.showAir("dolphin",false,true,true));
        assertFalse(WaterPhysiology.showAir("dolphin",false,true,false));
        assertTrue(WaterPhysiology.showAir("axolotl",false,false,false));assertFalse(WaterPhysiology.showAir("axolotl",false,true,true));
        assertTrue(WaterPhysiology.showAir("cod",true,false,false));assertFalse(WaterPhysiology.showAir("cod",true,true,true));
        assertFalse(WaterPhysiology.showAir("",false,false,false));assertTrue(WaterPhysiology.showAir("",false,true,true));
    }
    @Test void dryConfigSurvivesCopyAndHasBounds(){
        var c=new MorphConfig();c.dolphinDryOutSeconds=120;c.axolotlDryOutSeconds=300;var copy=c.copy();copy.validate();assertEquals(120,copy.dolphinDryOutSeconds);assertEquals(300,copy.axolotlDryOutSeconds);
        c.dolphinDryOutSeconds=0;assertThrows(IllegalArgumentException.class,c::validate);c.dolphinDryOutSeconds=601;assertThrows(IllegalArgumentException.class,c::validate);
        c.dolphinDryOutSeconds=15;c.axolotlDryOutSeconds=-1;assertThrows(IllegalArgumentException.class,c::validate);
    }
}
