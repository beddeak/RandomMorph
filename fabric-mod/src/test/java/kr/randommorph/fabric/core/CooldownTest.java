package kr.randommorph.fabric.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CooldownTest {
    @Test void waitsExactlyTheConfiguredTicksIncludingZero(){
        var timer=new Cooldown();timer.start(60);
        for(int tick=0;tick<59;tick++)timer.tick();assertEquals(1,timer.remaining());
        timer.tick();assertEquals(0,timer.remaining());timer.tick();assertEquals(0,timer.remaining());
        timer.start(0);assertEquals(0,timer.remaining());
    }
    @Test void shorteningAndRepeatedEditsKeepTimeAlreadyWaited(){
        var timer=new Cooldown();timer.start(200);
        for(int tick=0;tick<40;tick++)timer.tick();
        timer.duration(100);assertEquals(60,timer.remaining());
        timer.duration(160);assertEquals(120,timer.remaining());
        timer.tick();timer.duration(100);assertEquals(59,timer.remaining());
    }
    @Test void shorteningPastElapsedMakesTheAbilityReadyImmediately(){
        var timer=new Cooldown();timer.start(200);
        for(int tick=0;tick<100;tick++)timer.tick();timer.duration(60);assertEquals(0,timer.remaining());
    }
    @Test void increasingWaitUsesTheOriginalElapsedTime(){
        var timer=new Cooldown();timer.start(100);
        for(int tick=0;tick<20;tick++)timer.tick();timer.duration(200);assertEquals(180,timer.remaining());
    }
    @Test void readyDisabledAndExpiredAbilitiesStayReadyUntilUsedAgain(){
        var timer=new Cooldown();timer.duration(200);assertEquals(0,timer.remaining());
        timer.start(10);timer.duration(0);timer.duration(200);assertEquals(0,timer.remaining());
        timer.start(2);timer.tick();timer.tick();timer.duration(200);assertEquals(0,timer.remaining());
        timer.start(200);assertEquals(200,timer.remaining());
    }
    @Test void differentPlayersAndSlotsHaveIndependentWaits(){
        var first=new Cooldown();var second=new Cooldown();first.start(100);second.start(200);
        first.tick();first.duration(50);assertEquals(49,first.remaining());assertEquals(200,second.remaining());
    }
    @Test void invalidDurationsCannotCorruptTheRunningWait(){
        var timer=new Cooldown();timer.start(100);timer.tick();
        for(int ticks:new int[]{-1,72_001,Integer.MAX_VALUE}){
            assertThrows(IllegalArgumentException.class,()->timer.start(ticks));
            assertThrows(IllegalArgumentException.class,()->timer.duration(ticks));assertEquals(99,timer.remaining());
        }
    }
}
