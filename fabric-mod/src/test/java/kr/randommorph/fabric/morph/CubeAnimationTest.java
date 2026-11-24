package kr.randommorph.fabric.morph;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CubeAnimationTest {
    @Test void firstFrameDoesNotInventAJumpOrLanding(){
        for(boolean magma:new boolean[]{false,true})for(boolean ground:new boolean[]{false,true}){
            var animation=new CubeAnimation(magma);animation.update(100,ground);
            assertEquals(0,animation.previousSquish());assertEquals(0,animation.squish());assertEquals(0,animation.targetSquish());
        }
    }
    @Test void jumpAndLandingProduceOppositeDeformationAndThenSettle(){
        var animation=new CubeAnimation(false);animation.update(0,true);animation.update(1,false);animation.update(2,false);
        assertTrue(animation.squish()>0);assertEquals(0,animation.previousSquish());
        animation.update(3,true);animation.update(4,true);animation.update(5,true);
        assertTrue(animation.squish()<0);
        for(int tick=6;tick<100;tick++)animation.update(tick,true);
        assertEquals(0,animation.squish(),1e-10);
    }
    @Test void magmaSegmentsStayExpandedLongerThanSlime(){
        var slime=new CubeAnimation(false);var magma=new CubeAnimation(true);
        slime.update(0,true);magma.update(0,true);
        for(int tick=1;tick<=10;tick++){slime.update(tick,false);magma.update(tick,false);}
        assertTrue(magma.squish()>slime.squish()*5);
    }
    @Test void repeatedFramesAndPausedTicksCannotRetriggerContact(){
        var animation=new CubeAnimation(true);animation.update(10,true);animation.update(11,false);animation.update(12,false);
        float previous=animation.previousSquish(),current=animation.squish(),target=animation.targetSquish();
        for(int frame=0;frame<120;frame++)animation.update(12,true);
        assertEquals(previous,animation.previousSquish());assertEquals(current,animation.squish());assertEquals(target,animation.targetSquish());
    }
    @Test void replacementClockAndMissingTicksResetTheOldSpring(){
        var animation=new CubeAnimation(true);animation.update(100,true);animation.update(101,false);animation.update(102,false);
        assertTrue(animation.squish()>0);animation.update(0,true);assertEquals(0,animation.squish());
        animation.update(1,false);animation.update(2,false);animation.update(10,true);assertEquals(0,animation.squish());
        animation.update(11,true);assertEquals(0,animation.targetSquish());
    }
}
