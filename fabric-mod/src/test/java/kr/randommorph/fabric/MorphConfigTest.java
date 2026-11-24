package kr.randommorph.fabric;
import kr.randommorph.fabric.morph.MorphRegistry;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class MorphConfigTest {
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void defaultsContainEverySpeciesAndRareBosses(){var c=new MorphConfig();c.validate();assertEquals(90,c.mobs.size());assertEquals(.1,c.mobs.get("ender_dragon").weight);assertEquals(.25,c.mobs.get("warden").weight);assertEquals(1,c.mobs.get("chicken").weight);}
    @Test void invalidIntervalsAreRejected(){var c=new MorphConfig();for(int interval:new int[]{0,4,3601,Integer.MAX_VALUE}){c.morphIntervalSeconds=interval;assertThrows(IllegalArgumentException.class,c::validate);}}
    @Test void nonFiniteNegativeAndNullWeightsRejected(){for(double weight:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1}){var c=new MorphConfig();c.mobs.get("cow").weight=weight;assertThrows(IllegalArgumentException.class,c::validate);}var c=new MorphConfig();c.mobs.put("cow",null);assertThrows(IllegalArgumentException.class,c::validate);}
    @Test void unknownOrDuplicatedIdsRejected(){var c=new MorphConfig();c.mobs.put("ARMOR_STAND",new MorphConfig.Mob());assertThrows(IllegalArgumentException.class,c::validate);var duplicate=new MorphConfig();duplicate.mobs.put("COW",new MorphConfig.Mob());assertThrows(IllegalArgumentException.class,duplicate::validate);}
    @Test void allDisabledIsRejected(){var c=new MorphConfig();c.mobs.values().forEach(m->m.enabled=false);assertThrows(IllegalArgumentException.class,c::validate);}
    @Test void omittedSpeciesGetDefaults(){var c=new MorphConfig();c.mobs.clear();c.mobs.put("COW",new MorphConfig.Mob());c.validate();assertEquals(90,c.mobs.size());assertTrue(c.mobs.containsKey("cow"));}
    @Test void onlyOneEnabledFormWorks(){var c=new MorphConfig();c.mobs.values().forEach(m->m.enabled=false);c.mobs.get("vex").enabled=true;c.validate();assertEquals("vex",c.random(new Random(),"vex").id());}
    @Test void constrainedRollTriesAllEnabledPositiveFormsBeforeFailing(){
        var c=new MorphConfig();c.mobs.values().forEach(m->m.enabled=false);
        c.mobs.get("ender_dragon").enabled=true;c.mobs.get("ender_dragon").weight=Double.MAX_VALUE;
        c.mobs.get("chicken").enabled=true;c.mobs.get("chicken").weight=Double.MIN_VALUE;
        c.mobs.get("cow").enabled=true;c.mobs.get("cow").weight=0;c.validate();
        var checked=new ArrayList<String>();var random=new Random(){@Override public double nextDouble(){return 0;}};
        assertEquals("chicken",c.random(random,null,profile->{checked.add(profile.id());return profile.id().equals("chicken");}).id());
        assertEquals(List.of("ender_dragon","chicken"),checked);
    }
    @Test void blockedPoolReturnsNoFormWithoutEnablingDisabledAlternatives(){
        var c=new MorphConfig();c.mobs.values().forEach(m->m.enabled=false);c.mobs.get("cow").enabled=true;c.validate();
        var checked=new ArrayList<String>();assertNull(c.random(new Random(1),"cow",profile->{checked.add(profile.id());return false;}));
        assertEquals(List.of("cow"),checked);
    }
    @Test void optionalBlazeExplosionHasFiniteBounds(){var c=new MorphConfig();assertFalse(c.blazeImpactExplosion);c.blazeExplosionPower=Double.NaN;assertThrows(IllegalArgumentException.class,c::validate);}
    @Test void omittedCooldownsUseEveryNativeAbilityDefault(){
        var c=new MorphConfig();c.validate();for(var profile:MorphRegistry.profiles()){
            assertEquals(profile.primary().cooldownTicks(),c.cooldownTicks(profile,false),profile.id());
            assertEquals(profile.secondary().cooldownTicks(),c.cooldownTicks(profile,true),profile.id());
        }
    }
    @Test void cooldownOverridesAreIndependentForSpeciesAndSlots(){
        var c=new MorphConfig();c.mobs.get("pillager").primaryCooldownSeconds=0;
        c.mobs.get("wither").secondaryCooldownSeconds=3600;c.validate();
        assertEquals(0,c.cooldownTicks(MorphRegistry.find("pillager"),false));
        assertEquals(40,c.cooldownTicks(MorphRegistry.find("skeleton"),false));
        assertEquals(40,c.cooldownTicks(MorphRegistry.find("wither"),false));
        assertEquals(72_000,c.cooldownTicks(MorphRegistry.find("wither"),true));
    }
    @Test void invalidBoundsAndAbilitiesThatDoNotExistAreRejected(){
        for(int seconds:new int[]{-1,3601,Integer.MAX_VALUE}){
            var c=new MorphConfig();c.mobs.get("wither").secondaryCooldownSeconds=seconds;assertThrows(IllegalArgumentException.class,c::validate);
        }
        var primary=new MorphConfig();primary.mobs.get("vex").primaryCooldownSeconds=0;assertThrows(IllegalArgumentException.class,primary::validate);
        var secondary=new MorphConfig();secondary.mobs.get("ender_dragon").secondaryCooldownSeconds=3;assertThrows(IllegalArgumentException.class,secondary::validate);
    }
    @Test void stagedCommandEditsDoNotAliasTheLiveMobRules(){
        var c=new MorphConfig();c.mobs.get("wither").primaryCooldownSeconds=3;var next=c.copy();
        next.mobs.get("wither").primaryCooldownSeconds=7;next.mobs.get("wither").weight=.8;
        assertEquals(3,c.mobs.get("wither").primaryCooldownSeconds);assertEquals(.1,c.mobs.get("wither").weight);
        assertEquals(7,next.mobs.get("wither").primaryCooldownSeconds);
    }
}
