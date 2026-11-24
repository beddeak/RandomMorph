package kr.randommorph.fabric.ability;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightSnapshotTest {
    @Test void creativeToSpectatorKeepsMandatorySpectatorFlight() {
        var before = new FlightSnapshot(true, false, .08f, true, false);
        var restored = before.restored(false, true);
        assertTrue(restored.mayfly());
        assertTrue(restored.flying());
        assertEquals(.05f, restored.speed());
    }

    @Test void spectatorToCreativeDoesNotRestoreSpectatorFlightOrSpeed() {
        var before = new FlightSnapshot(true, true, .15f, false, true);
        var restored = before.restored(true, false);
        assertTrue(restored.mayfly());
        assertFalse(restored.flying());
        assertEquals(.05f, restored.speed());
    }

    @Test void unchangedCreativeRestoresOriginalFlightAndCustomSpeed() {
        var before = new FlightSnapshot(true, false, .08f, true, false);
        assertEquals(before, before.restored(true, false));
    }

    @Test void unchangedSpectatorRestoresOriginalSpeed() {
        var before = new FlightSnapshot(true, true, .15f, false, true);
        assertEquals(before, before.restored(false, true));
    }

    @Test void unchangedSurvivalPreservesFlightGrantedBeforeMorph() {
        var before = new FlightSnapshot(true, true, .08f, false, false);
        assertEquals(before, before.restored(false, false));
    }

    @Test void leavingCreativeOrSpectatorRevokesOwnedFlight() {
        for (var before : new FlightSnapshot[]{
                new FlightSnapshot(true, false, .08f, true, false),
                new FlightSnapshot(true, true, .15f, false, true)}) {
            var restored = before.restored(false, false);
            assertFalse(restored.mayfly());
            assertFalse(restored.flying());
            assertEquals(.05f, restored.speed());
        }
    }

    @Test void enteringCreativeOrSpectatorUsesCurrentModeDefaults() {
        var before = new FlightSnapshot(false, false, .05f, false, false);
        var creative = before.restored(true, false);
        assertTrue(creative.mayfly());
        assertFalse(creative.flying());
        var spectator = before.restored(false, true);
        assertTrue(spectator.mayfly());
        assertTrue(spectator.flying());
    }
}
