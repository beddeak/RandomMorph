package kr.randommorph.fabric.core;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimalStateTest {
    @Test void onlyOneStingPerBeeAndOtherFormsCannotSting(){
        var state=new AnimalState();assertFalse(state.sting());state.form("bee");
        assertTrue(state.sting());assertFalse(state.sting());
        state.form("cow");assertFalse(state.stung());assertFalse(state.sting());
        state.form("bee");assertTrue(state.sting());
    }
    @Test void nativeDeathChecksOccurEveryFiveTicksWithShrinkingBound(){
        var state=new AnimalState();state.form("bee");state.sting();var bounds=new ArrayList<Integer>();
        for(int i=0;i<14;i++)assertFalse(state.stingDeath(bound->{bounds.add(bound);return 1;}));
        assertEquals(List.of(1195,1190),bounds);
        assertTrue(state.stingDeath(bound->{assertEquals(1185,bound);return 0;}));
    }
    @Test void sixtySecondsReachesNativeBoundOneAndDeathResetStopsFurtherRolls(){
        var state=new AnimalState();state.form("bee");state.sting();
        for(int i=0;i<1199;i++)assertFalse(state.stingDeath(bound->bound-1));
        assertTrue(state.stingDeath(bound->{assertEquals(1,bound);return 0;}));
        state.resetSting();assertFalse(state.stingDeath(bound->{fail("Dead bee kept ticking");return 0;}));
    }
    @Test void creditsOrDimensionReapplicationKeepsStingButDeathReplacementResetsIt(){
        var old=new AnimalState();old.form("bee");old.sting();for(int i=0;i<12;i++)old.stingDeath(bound->1);
        var returned=new AnimalState();returned.copyFrom(old,true);returned.form("bee");
        assertTrue(returned.stung());assertEquals(12,returned.stingTicks());assertFalse(returned.sting());
        returned.stingDeath(bound->1);assertEquals(12,old.stingTicks());
        var respawn=new AnimalState();respawn.copyFrom(old,false);respawn.form("bee");assertTrue(respawn.sting());assertEquals(0,respawn.stingTicks());
    }
    @Test void stopOrDifferentSpeciesCancelsBeeInjury(){
        var state=new AnimalState();state.form("bee");state.sting();state.form("");
        for(int i=0;i<2000;i++)assertFalse(state.stingDeath(bound->{fail("Stopped form still ran fate");return 0;}));
        assertEquals(0,state.flags());
    }
    @Test void woolCannotBeFarmedByRemorphingOrRespawning(){
        var state=new AnimalState();state.form("sheep");assertTrue(state.shear());assertFalse(state.shear());
        state.form("cow");state.form("sheep");assertFalse(state.shear());assertEquals(MorphFlags.SHEARED,state.flags());
        var respawn=new AnimalState();respawn.copyFrom(state,false);respawn.form("sheep");assertFalse(respawn.shear());
        respawn.ate();assertTrue(respawn.shear());assertTrue(state.sheared());
    }
    @Test void productionSaveLoadPreservesWoolAndEggProgressWithoutBeeInjury(){
        var old=new AnimalState();old.form("chicken");for(int i=0;i<1200;i++)assertFalse(old.chickenTick(bound->0));old.shear();
        var restored=new AnimalState();restored.restoreProduction(old.sheared(),old.eggTicks());restored.form("chicken");
        assertTrue(restored.sheared());assertEquals(4800,restored.eggTicks());
        for(int i=0;i<4799;i++)assertFalse(restored.chickenTick(bound->0));assertTrue(restored.chickenTick(bound->0));assertEquals(6000,restored.eggTicks());
    }
    @Test void fiveOneMinuteChickenFormsProduceAnEggAndCowTimeDoesNotCount(){
        var state=new AnimalState();int eggs=0;
        for(int cycle=0;cycle<5;cycle++){
            state.form("chicken");for(int i=0;i<1200;i++)if(state.chickenTick(bound->0))eggs++;
            state.form("cow");int remaining=state.eggTicks();
            for(int i=0;i<1200;i++)assertFalse(state.chickenTick(bound->{fail("Cow advanced egg clock");return 0;}));
            assertEquals(remaining,state.eggTicks());
        }
        assertEquals(1,eggs);assertEquals(6000,state.eggTicks());
    }
    @Test void productionDataBoundsPreventInstantEggsFromMalformedSavedValues(){
        var state=new AnimalState();state.restoreProduction(false,Integer.MAX_VALUE);state.form("chicken");
        assertFalse(state.chickenTick(bound->5999));assertEquals(11998,state.eggTicks());
        state.restoreProduction(true,-8);assertEquals(-1,state.eggTicks());assertTrue(state.sheared());
    }
    @Test void eatingCountdownDoesNotAlterPhysicalOrResourceBits(){
        for(int ticks=0;ticks<=40;ticks++){
            int flags=MorphFlags.withGrazing(127,ticks);
            assertTrue(MorphFlags.valid(flags));assertEquals(ticks,MorphFlags.grazing(flags));assertEquals(127,flags&127);
        }
        assertFalse(MorphFlags.valid(41<<7));assertFalse(MorphFlags.valid(-1));
        assertEquals(MorphFlags.SHEARED,MorphFlags.withGrazing(MorphFlags.SHEARED,-1));
    }
    @Test void snowGolemPumpkinCanBeHarvestedOnlyOnceAndOnlyInItsOwnForm(){
        var state=new AnimalState();assertFalse(state.shearPumpkin());state.form("sheep");assertFalse(state.shearPumpkin());
        state.form("snow_golem");assertTrue(state.shearPumpkin());assertFalse(state.shearPumpkin());
        assertTrue(state.pumpkinRemoved());assertEquals(MorphFlags.SHEARED,state.flags());assertTrue(MorphFlags.valid(state.flags()));
        assertFalse(state.sheared());state.form("sheep");assertEquals(0,state.flags());
    }
    @Test void pumpkinCannotBeFarmedByRemorphingStoppingOrEitherRespawnCopy(){
        var state=new AnimalState();state.form("snow_golem");assertTrue(state.shearPumpkin());
        state.form("");assertEquals(0,state.flags());state.form("cow");state.form("snow_golem");assertFalse(state.shearPumpkin());
        for(boolean alive:List.of(false,true)){
            var next=new AnimalState();next.copyFrom(state,alive);next.form("snow_golem");
            assertTrue(next.pumpkinRemoved());assertFalse(next.shearPumpkin());assertEquals(MorphFlags.SHEARED,next.flags());
        }
    }
    @Test void savedPumpkinStateSurvivesReloadWithoutConflatingSheepWool(){
        var state=new AnimalState();state.restoreProduction(true,4800,14,true);state.form("snow_golem");
        assertFalse(state.shearPumpkin());assertEquals(MorphFlags.SHEARED,state.flags());
        state.form("sheep");assertEquals(14,MorphFlags.color(state.flags()));assertTrue(state.sheared());
        state.ate();assertFalse(state.sheared());assertTrue(state.pumpkinRemoved());assertEquals(4800,state.eggTicks());
        state.form("snow_golem");assertFalse(state.shearPumpkin());
    }
    @Test void olderSavesDefaultToPumpkinPresentRegardlessOfSheepResource(){
        var state=new AnimalState();state.restoreProduction(true,500,7);state.form("snow_golem");
        assertFalse(state.pumpkinRemoved());assertEquals(0,state.flags());assertTrue(state.shearPumpkin());
        assertTrue(state.sheared());assertEquals(7,state.woolColor());
    }
}
