package kr.randommorph.fabric.ability;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class ElderShulkerTest {
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void auraDoesNotSpamExistingLongStrongEffect(){
        assertFalse(ElderAura.refresh(new MobEffectInstance(MobEffects.MINING_FATIGUE,6000,2)));
        assertFalse(ElderAura.refresh(new MobEffectInstance(MobEffects.MINING_FATIGUE,1200,2)));
        assertFalse(ElderAura.refresh(new MobEffectInstance(MobEffects.MINING_FATIGUE,-1,3)));
    }
    @Test void auraRefreshesOnlyMissingWeakerOrShortEffect(){
        assertTrue(ElderAura.refresh(null));assertTrue(ElderAura.refresh(new MobEffectInstance(MobEffects.MINING_FATIGUE,6000,1)));
        assertTrue(ElderAura.refresh(new MobEffectInstance(MobEffects.MINING_FATIGUE,1199,2)));
    }
    @Test void shulkerEscapesOnlyBelowHalfAfterOneInFourRoll(){
        assertFalse(ShulkerAttachment.escapeRoll(15,30,0));assertFalse(ShulkerAttachment.escapeRoll(0,30,0));
        assertTrue(ShulkerAttachment.escapeRoll(14.99f,30,0));for(int roll=1;roll<4;roll++)assertFalse(ShulkerAttachment.escapeRoll(5,30,roll));
    }
}
