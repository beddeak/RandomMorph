package kr.randommorph.fabric.ability;

import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MorphFlightTest {
    @Test void spectatorDoesNotReceiveSpeciesSpeedOrLoseNativeFlight(){
        var abilities=new Abilities();GameType.SPECTATOR.updatePlayerAbilities(abilities);abilities.setFlyingSpeed(.17f);
        assertFalse(MorphFlight.update(abilities,true,.024f,true));
        assertTrue(abilities.mayfly);assertTrue(abilities.flying);assertEquals(.17f,abilities.getFlyingSpeed());
    }
    @Test void returningFromSpectatorRestoresSpeciesSpeedAndFlightPermissionWithoutForcedTakeoff(){
        var abilities=new Abilities();GameType.SPECTATOR.updatePlayerAbilities(abilities);
        MorphFlight.update(abilities,true,.024f,true);
        GameType.SURVIVAL.updatePlayerAbilities(abilities);
        assertFalse(abilities.mayfly);assertFalse(abilities.flying);
        assertTrue(MorphFlight.update(abilities,false,.024f,false));
        assertTrue(abilities.mayfly);assertFalse(abilities.flying);assertEquals(.024f,abilities.getFlyingSpeed());
        assertFalse(MorphFlight.update(abilities,false,.024f,false));
    }
    @Test void initialFormTakesOffAndLaterTicksPreserveLanding(){
        var abilities=new Abilities();assertTrue(MorphFlight.update(abilities,false,.032f,true));
        assertTrue(abilities.flying);abilities.flying=false;
        assertFalse(MorphFlight.update(abilities,false,.032f,false));assertFalse(abilities.flying);
        abilities.flying=true;assertFalse(MorphFlight.update(abilities,false,.032f,false));assertTrue(abilities.flying);
    }
    @Test void vanillaCreativeSpeedResetIsRepairedEvenWhenMayflyAlreadyTrue(){
        var abilities=new Abilities();GameType.CREATIVE.updatePlayerAbilities(abilities);abilities.setFlyingSpeed(.05f);
        assertTrue(abilities.mayfly);assertTrue(MorphFlight.update(abilities,false,.024f,false));
        assertEquals(.024f,abilities.getFlyingSpeed());assertTrue(abilities.instabuild);assertTrue(abilities.invulnerable);
    }
}
