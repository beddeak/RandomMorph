package kr.randommorph.fabric.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MorphIntervalTest {
    @Test void shorterIntervalPreservesElapsed(){var s=new MorphSession("cow",60);for(int i=0;i<200;i++)s.tick();s.interval(30);assertEquals(20,s.remainingSeconds());assertEquals(30,s.intervalSeconds());}
    @Test void elapsedPastNewIntervalRerollsOnNextTick(){var s=new MorphSession("cow",60);for(int i=0;i<1000;i++)s.tick();s.interval(30);assertEquals(1,s.remainingTicks());assertTrue(s.tick());assertEquals(600,s.remainingTicks());}
    @Test void longerIntervalDoesNotResetElapsed(){var s=new MorphSession("cow",60);s.tick();s.interval(120);assertEquals(2399,s.remainingTicks());}
    @Test void pausedClockStaysPausedAfterChange(){var s=new MorphSession("cow",60);s.paused(true);s.interval(5);assertFalse(s.tick());assertEquals(100,s.remainingTicks());}
    @Test void invalidIntervalKeepsClock(){var s=new MorphSession("cow",60);assertThrows(IllegalArgumentException.class,()->s.interval(4));assertThrows(IllegalArgumentException.class,()->s.interval(3601));assertEquals(60,s.intervalSeconds());}
}
