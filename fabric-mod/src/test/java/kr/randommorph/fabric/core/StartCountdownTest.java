package kr.randommorph.fabric.core;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StartCountdownTest {
    @Test void threeSecondCountdownStartsEveryoneOnTheSameTick(){
        var c=new StartCountdown();var a=UUID.randomUUID();var b=UUID.randomUUID();
        assertTrue(c.begin(List.of(a,b)));assertEquals(3,c.seconds());
        for(int tick=1;tick<60;tick++){assertTrue(c.tick().isEmpty());assertEquals((60-tick+19)/20,c.seconds());}
        assertEquals(Set.of(a,b),Set.copyOf(c.tick()));assertFalse(c.active());assertTrue(c.tick().isEmpty());
    }
    @Test void duplicateStartCannotResetCountdownOrReplacePlayers(){
        var c=new StartCountdown();var a=UUID.randomUUID();c.begin(List.of(a));for(int i=0;i<20;i++)c.tick();
        assertFalse(c.begin(List.of(UUID.randomUUID())));assertEquals(2,c.seconds());assertEquals(Set.of(a),c.players());
    }
    @Test void quitStopOrSetRemovesOnlyThatPlayer(){
        var c=new StartCountdown();var a=UUID.randomUUID();var b=UUID.randomUUID();c.begin(List.of(a,b));c.cancel(a);
        List<UUID> due=List.of();for(int i=0;i<60;i++)due=c.tick();assertEquals(List.of(b),due);
    }
    @Test void shutdownOrLastCancellationLeavesNoDelayedStart(){
        var c=new StartCountdown();var a=UUID.randomUUID();c.begin(List.of(a));c.cancel(a);assertFalse(c.active());
        assertTrue(c.begin(List.of(a)));c.clear();for(int i=0;i<80;i++)assertTrue(c.tick().isEmpty());
        assertFalse(c.begin(List.of()));assertTrue(c.begin(List.of(a)));assertEquals(3,c.seconds());
    }
    @Test void participantsCannotMutatePendingState(){
        var c=new StartCountdown();c.begin(List.of(UUID.randomUUID()));assertThrows(UnsupportedOperationException.class,()->c.players().clear());
    }
}
