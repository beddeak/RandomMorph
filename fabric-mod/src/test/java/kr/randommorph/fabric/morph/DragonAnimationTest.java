package kr.randommorph.fabric.morph;

import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DragonAnimationTest {
    @Test void firstFrameSeedsAllNativeSamplesAtThePlayersPose(){
        var history=new DragonFlightHistory();var animation=new DragonAnimation(history);
        animation.update(100,new Vec3(1200,180,-300),45);
        for(int i=0;i<DragonFlightHistory.LENGTH;i++){
            assertEquals(180,history.get(i).y());assertEquals(-135,history.get(i).yRot());
        }
    }
    @Test void bodyForwardMatchesPlayerLookInEveryCardinalDirection(){
        for(float yaw:new float[]{0,90,180,270}){
            var history=new DragonFlightHistory();new DragonAnimation(history).update(0,Vec3.ZERO,yaw);
            double dragonYaw=Math.toRadians(history.get(0).yRot()),playerYaw=Math.toRadians(yaw);
            assertEquals(-Math.sin(playerYaw),Math.sin(dragonYaw),1e-6);
            assertEquals(Math.cos(playerYaw),-Math.cos(dragonYaw),1e-6);
        }
    }
    @Test void repeatedFramesAndPausedTicksDoNotAdvanceOrConsumeTurnHistory(){
        var history=new DragonFlightHistory();var animation=new DragonAnimation(history);
        animation.update(10,new Vec3(0,80,0),0);animation.update(11,new Vec3(.2,80,0),5);
        var sample=history.get(0);float previous=animation.previousFlapTime(),current=animation.flapTime();
        for(int frame=0;frame<120;frame++)animation.update(11,new Vec3(.2,80,0),5);
        assertSame(sample,history.get(0));assertEquals(previous,animation.previousFlapTime());assertEquals(current,animation.flapTime());
    }
    @Test void turnsAndClimbsReachNativeDelayedBodyNeckAndTailSamples(){
        var history=new DragonFlightHistory();var animation=new DragonAnimation(history);
        for(int i=0;i<=30;i++)animation.update(100+i,new Vec3(i*.2,120+i*.1,0),i*3);
        assertEquals(123,history.get(0).y());assertEquals(122.3,history.get(7).y(),1e-9);
        assertEquals(-111,history.get(7).yRot());
        assertNotEquals(history.get(0).yRot(),history.get(16).yRot());
        assertEquals(.5,history.get(5).y()-history.get(10).y(),1e-9);
    }
    @Test void nativeYawInterpolationTakesTheShortPathAcrossTheAngleSeam(){
        var history=new DragonFlightHistory();var animation=new DragonAnimation(history);
        animation.update(0,Vec3.ZERO,-1);animation.update(1,Vec3.ZERO,1);
        assertEquals(180,Math.abs(history.get(0,.5f).yRot()),1e-6);
    }
    @Test void flapRateUsesObservedRemoteMotionAndNativeVerticalGain(){
        var still=new DragonAnimation(new DragonFlightHistory());still.update(0,Vec3.ZERO,0);still.update(1,Vec3.ZERO,0);
        var flying=new DragonAnimation(new DragonFlightHistory());flying.update(0,Vec3.ZERO,0);flying.update(1,new Vec3(.4,0,0),0);
        var climbing=new DragonAnimation(new DragonFlightHistory());climbing.update(0,Vec3.ZERO,0);climbing.update(1,new Vec3(.4,.5,0),0);
        assertEquals(.2,still.flapTime(),1e-6);assertEquals(.04,flying.flapTime(),1e-6);
        assertEquals(.04*Math.sqrt(2),climbing.flapTime(),1e-6);
    }
    @Test void teleportsAndEntityClockReplacementDoNotCarryOldBodyPitch(){
        var history=new DragonFlightHistory();var animation=new DragonAnimation(history);
        animation.update(100,new Vec3(0,80,0),0);animation.update(101,new Vec3(0,81,0),10);
        animation.update(102,new Vec3(1000,250,1000),90);
        for(int i=0;i<DragonFlightHistory.LENGTH;i++)assertEquals(250,history.get(i).y());
        animation.update(0,new Vec3(20,40,20),180);
        for(int i=0;i<DragonFlightHistory.LENGTH;i++){assertEquals(40,history.get(i).y());assertEquals(0,history.get(i).yRot());}
    }
    @Test void longAnimationsKeepFlapPrecisionAndContinuousPartialTickIntervals(){
        var animation=new DragonAnimation(new DragonFlightHistory());
        for(int tick=0;tick<100_000;tick++){
            animation.update(tick,Vec3.ZERO,0);
            assertTrue(animation.previousFlapTime()>=0&&animation.previousFlapTime()<1);
            assertTrue(animation.flapTime()>=animation.previousFlapTime());
            if(tick>0)assertEquals(.2,animation.flapTime()-animation.previousFlapTime(),1e-6);
        }
    }
}
