package kr.randommorph.fabric.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MorphSessionTest {
    @Test void expiresAtExactly60Seconds(){var s=new MorphSession("cow",60);for(int i=0;i<1199;i++)assertFalse(s.tick());assertTrue(s.tick());assertEquals(1200,s.remainingTicks());}
    @Test void pauseFreezesClock(){var s=new MorphSession("cow",60);s.tick();s.paused(true);for(int i=0;i<2000;i++)assertFalse(s.tick());assertEquals(1199,s.remainingTicks());s.paused(false);s.tick();assertEquals(1198,s.remainingTicks());}
    @Test void pendingFormDoesNotResetDeathClock(){var s=new MorphSession("cow",60);for(int i=0;i<1205;i++)s.tick();s.setForm("chicken",false);assertEquals(1195,s.remainingTicks());assertEquals("chicken",s.form());}
    @Test void playersHaveIndependentClocks(){var a=new MorphSession("cow",60);var b=new MorphSession("chicken",60);a.tick();b.paused(true);assertEquals(1199,a.remainingTicks());assertEquals(1200,b.remainingTicks());}
    @Test void manualSetRestartsClock(){var s=new MorphSession("cow",60);s.tick();s.setForm("zombie",true);assertEquals(1200,s.remainingTicks());}
    @Test void healthRatioSurvivesChange(){assertEquals(.5f,MorphSession.healthRatio(10,20));assertEquals(20,.5f*40);}
    @Test void deadRatioIsZero(){assertEquals(0,MorphSession.healthRatio(0,20));}
    @Test void invalidRatioDoesNotProduceNaN(){assertEquals(0,MorphSession.healthRatio(Float.NaN,20));assertEquals(0,MorphSession.healthRatio(5,0));assertEquals(1,MorphSession.healthRatio(50,20));}
    @Test void blockedRollRetriesAfterFiveSecondsWithoutChangingNormalInterval(){
        var s=new MorphSession("cow",60);for(int i=0;i<1200;i++)s.tick();s.retryAfter(5);
        assertEquals(100,s.remainingTicks());assertEquals(60,s.intervalSeconds());
        for(int i=0;i<99;i++)assertFalse(s.tick());assertTrue(s.tick());assertEquals(1200,s.remainingTicks());
    }
    @Test void repeatedSpaceChecksDoNotExtendDeadlineOrUnpauseGame(){
        var s=new MorphSession("cow",60);s.retryAfter(5);s.tick();s.retryAfter(5);assertEquals(99,s.remainingTicks());
        s.paused(true);s.retryAfter(5);assertFalse(s.tick());assertEquals(99,s.remainingTicks());
    }
    @Test void cubeVariantPreservesClockPauseAndPlayerIndependence(){
        var a=new MorphSession("slime",60);var b=new MorphSession("slime",60);a.tick();a.paused(true);a.cubeSize(4);
        assertEquals(4,a.cubeSize());assertEquals(2,b.cubeSize());assertEquals(1199,a.remainingTicks());assertTrue(a.paused());
        assertFalse(a.tick());a.retryAfter(5);assertEquals(4,a.cubeSize());
        a.setForm("slime",false);assertEquals(2,a.cubeSize());assertEquals(100,a.remainingTicks());assertTrue(a.paused());
        a.cubeSize(1);a.setForm("magma_cube",true);assertEquals(2,a.cubeSize());
        assertThrows(IllegalArgumentException.class,()->a.cubeSize(3));assertEquals(2,a.cubeSize());
    }
    @Test void boundsRejected(){assertThrows(IllegalArgumentException.class,()->new MorphSession("cow",0));assertThrows(IllegalArgumentException.class,()->new MorphSession("",60));}
}
