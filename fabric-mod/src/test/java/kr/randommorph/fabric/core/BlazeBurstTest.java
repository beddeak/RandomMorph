package kr.randommorph.fabric.core;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BlazeBurstTest {
    @Test void threeShotsFollowNativeChargeAndSpacing(){
        var burst=new BlazeBurst();assertTrue(burst.begin());var shots=new ArrayList<Integer>();
        for(int tick=1;tick<=100;tick++){if(burst.tick())shots.add(tick);if(tick<78)assertTrue(burst.active());}
        assertEquals(List.of(60,66,72),shots);assertFalse(burst.active());assertFalse(burst.tick());
    }
    @Test void repeatedKeyCannotResetChargeOrCreateOverlappingBursts(){
        var burst=new BlazeBurst();burst.begin();for(int tick=1;tick<60;tick++){assertFalse(burst.tick());assertFalse(burst.begin());}assertTrue(burst.tick());
    }
    @Test void deathStopOrRemorphCancelsRemainingShots(){
        var burst=new BlazeBurst();burst.begin();for(int i=0;i<60;i++)burst.tick();burst.clear();for(int i=0;i<120;i++)assertFalse(burst.tick());assertTrue(burst.begin());
    }
    @Test void playersHaveIndependentChargeAndNewFormsStartEmpty(){
        var a=new BlazeBurst();var b=new BlazeBurst();a.begin();for(int i=0;i<59;i++)a.tick();b.begin();assertTrue(a.tick());assertFalse(b.tick());
        a.clear();assertTrue(b.active());assertFalse(new BlazeBurst().active());
    }
}
