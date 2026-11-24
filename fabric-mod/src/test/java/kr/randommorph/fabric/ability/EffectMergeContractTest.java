package kr.randommorph.fabric.ability;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native merge contracts used by the transformed behavioral probes in verifyMixins. */
class EffectMergeContractTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private MobEffectInstance effect(int duration, int amplifier) {
        return new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier);
    }
    private void ticks(MobEffectInstance effect, int ticks) {
        for (int i = 0; i < ticks; i++) effect.tickClient();
    }
    @Test void durationExtensionMutatesTheSameInstance() {
        var current = effect(200, 0);
        var same = current;
        assertTrue(current.update(effect(400, 0)));
        assertSame(same, current);
        assertEquals(400, current.getDuration());
    }
    @Test void weakerLongerMergeReturnsFalseButSurvivesAsHidden() {
        var current = effect(200, 2);
        assertFalse(current.update(effect(400, 0)));
        ticks(current, 200);
        assertEquals(0, current.getAmplifier());
        assertEquals(200, current.getDuration());
    }
    @Test void hiddenDurationRefreshAlsoReturnsFalse() {
        var current = effect(200, 2);
        current.update(effect(400, 0));
        assertFalse(current.update(effect(500, 0)));
        ticks(current, 200);
        assertEquals(300, current.getDuration());
    }
    @Test void strongerShorterEffectKeepsPreviousLayerAndTicksBoth() {
        var current = effect(200, 0);
        assertTrue(current.update(effect(40, 1)));
        ticks(current, 40);
        assertEquals(0, current.getAmplifier());
        assertEquals(160, current.getDuration());
    }
    @Test void weakerShorterEffectDoesNotCreateHiddenLayer() {
        var current = effect(200, 2);
        assertFalse(current.update(effect(100, 0)));
        ticks(current, 200);
        assertEquals(2, current.getAmplifier());
        assertEquals(0, current.getDuration());
    }
    @Test void infiniteExtensionSurvivesTicking() {
        var current = effect(200, 0);
        assertTrue(current.update(effect(MobEffectInstance.INFINITE_DURATION, 0)));
        ticks(current, 300);
        assertTrue(current.isInfiniteDuration());
    }
    @Test void vanillaAliveRespawnCopyDropsTheExternalHiddenLayer() {
        var original=effect(200,0);original.update(effect(40,1));
        var copy=new MobEffectInstance(original);
        ticks(original,40);ticks(copy,40);
        assertEquals(0,original.getAmplifier());assertEquals(160,original.getDuration());
        assertEquals(1,copy.getAmplifier());assertEquals(0,copy.getDuration());
    }
}
