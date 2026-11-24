package kr.randommorph.fabric.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SheepColorTest {
    @Test void sixteenColorsSurviveOtherFlagsAndFaceUpdates(){
        for(int color=0;color<16;color++){
            var flags=MorphFlags.withColor(MorphFlags.withGrazing(127,40),color);
            assertTrue(MorphFlags.valid(flags));assertEquals(color,MorphFlags.color(flags));assertEquals(40,MorphFlags.grazing(flags));
            assertEquals(color,MorphFlags.color(MorphFlags.withGrazing(flags,0)));assertEquals(127,flags&127);
        }
        assertFalse(MorphFlags.valid(1<<22));
    }
    @Test void shearedOrAlreadyMatchingSheepCannotConsumeAnotherDye(){
        var state=new AnimalState();state.form("sheep");assertFalse(state.dye(0));assertTrue(state.dye(14));assertFalse(state.dye(14));
        state.shear();assertFalse(state.dye(11));state.ate();assertTrue(state.dye(11));assertFalse(state.dye(-1));assertFalse(state.dye(16));
    }
    @Test void deathRemorphAndRespawnCopyKeepWoolResourceAndDye(){
        var original=new AnimalState();original.form("sheep");original.dye(11);original.shear();original.form("cow");assertEquals(0,original.flags());
        original.form("sheep");assertEquals(11,MorphFlags.color(original.flags()));
        var copy=new AnimalState();copy.copyFrom(original,false);copy.form("sheep");assertEquals(11,copy.woolColor());assertTrue(copy.sheared());
        copy.ate();assertEquals(11,copy.woolColor());assertFalse(copy.sheared());
    }
    @Test void legacyAndMalformedSavedColorsRecoverWhite(){
        var s=new AnimalState();s.restoreProduction(false,55,99);assertEquals(0,s.woolColor());s.restoreProduction(true,77,9);assertEquals(9,s.woolColor());
        s.restoreProduction(false,44);assertEquals(0,s.woolColor());assertEquals(44,s.eggTicks());
    }
}
