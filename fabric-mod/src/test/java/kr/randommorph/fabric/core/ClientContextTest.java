package kr.randommorph.fabric.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientContextTest {
    private record Player(String uuid) {}
    @Test void sameUuidRespawnInSameWorldInvalidatesOldEntityIds() {
        var context=new ClientContext();var level=new Object();var old=new Player("same-uuid");
        assertTrue(context.update(level,old));assertFalse(context.update(level,old));
        var replacement=new Player("same-uuid");assertEquals(old,replacement);
        assertTrue(context.update(level,replacement));
    }
    @Test void worldChangeInvalidatesEvenWhenPlayerInstanceIsRetained() {
        var context=new ClientContext();var player=new Player("uuid");
        context.update(new Object(),player);assertTrue(context.update(new Object(),player));
    }
    @Test void firstPacketAfterRespawnAndNextTickShareTheSameContext() {
        var context=new ClientContext();var level=new Object();var old=new Player("uuid");
        context.update(level,old);var replacement=new Player("uuid");
        assertTrue(context.update(level,replacement)); // Clear stale state BEFORE storing this packet.
        assertFalse(context.update(level,replacement)); // Keep the fresh packet on the next tick.
    }
    @Test void creditsNullWorldAndReconnectDoNotRetainPreviousContext() {
        var context=new ClientContext();var level=new Object();var player=new Player("uuid");
        context.update(level,player);assertTrue(context.update(null,null));assertFalse(context.update(null,null));
        assertTrue(context.update(level,player));context.reset();assertTrue(context.update(level,player));
    }
}
