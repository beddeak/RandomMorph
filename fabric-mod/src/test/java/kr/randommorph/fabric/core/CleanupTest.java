package kr.randommorph.fabric.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CleanupTest {
    @Test void brokenListenerDoesNotBlockEntitiesEffectsOrFlight() {
        var failures=new ArrayList<Throwable>();var released=new ArrayList<String>();
        var cleanup=new Cleanup(failures::add);
        var failure=new IllegalStateException("listener removal");
        cleanup.attempt(()->{released.add("listener");throw failure;});
        for(var resource:List.of("projectile","effect","flight","body"))cleanup.attempt(()->released.add(resource));
        assertEquals(List.of("listener","projectile","effect","flight","body"),released);
        assertEquals(List.of(failure),failures);
    }
    @Test void linkageFailureInOnePlayerDoesNotBlockAnotherPlayer() {
        var failures=new ArrayList<Throwable>();var released=new ArrayList<String>();
        var cleanup=new Cleanup(failures::add);
        cleanup.attempt(()->{throw new NoClassDefFoundError("other mod callback");});
        cleanup.attempt(()->released.add("second player"));
        assertInstanceOf(LinkageError.class,failures.getFirst());assertEquals(List.of("second player"),released);
    }
    @Test void fatalJvmErrorsAreNotSwallowed() {
        var failures=new ArrayList<Throwable>();var cleanup=new Cleanup(failures::add);
        assertThrows(OutOfMemoryError.class,()->cleanup.attempt(()->{throw new OutOfMemoryError("probe");}));
        assertTrue(failures.isEmpty());
    }
}
