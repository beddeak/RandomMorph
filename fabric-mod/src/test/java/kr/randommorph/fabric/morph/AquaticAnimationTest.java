package kr.randommorph.fabric.morph;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AquaticAnimationTest {
    @Test void duplicateExtractionCannotAdvanceAnimation(){
        var a=new AquaticAnimation();a.update(10,true,true,0,.1);float angle=a.tentacle(),tail=a.tail();
        for(int i=0;i<120;i++)assertFalse(a.update(10,true,true,0,.1));assertEquals(angle,a.tentacle());assertEquals(tail,a.tail());
    }
    @Test void swimRetractsGuardianSpikesWhileDryPoseExtendsThem(){
        var a=new AquaticAnimation();for(int t=0;t<40;t++)a.update(t,true,true,0,.1);assertTrue(a.spikes()<.1);
        for(int t=40;t<80;t++)a.update(t,false,false,0,0);assertTrue(a.spikes()>.9);assertTrue(a.pitch()<-45);
    }
    @Test void squidTentaclesChangeAndSeparatePlayersStayIndependent(){
        var a=new AquaticAnimation();var b=new AquaticAnimation();a.update(0,true,false,0,0);b.update(0,true,false,0,0);
        float first=a.tentacle();for(int t=1;t<12;t++)a.update(t,true,true,.03,.1);
        assertNotEquals(first,a.tentacle());assertEquals(first,b.tentacle());assertTrue(a.pitch()<0);
    }
    @Test void longGapOrNewOwnerTickReseedsPreviousInterpolation(){
        var a=new AquaticAnimation();for(int t=0;t<20;t++)a.update(t,true,true,0,.1);a.update(500,false,false,0,0);
        assertEquals(0,a.tailOld());assertEquals(-90,a.pitchOld());a.update(0,true,false,0,0);assertEquals(0,a.pitchOld());
    }
}
