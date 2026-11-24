package kr.randommorph.fabric.morph;
import java.util.*;
import kr.randommorph.fabric.ability.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class CatalogueAuditTest {
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void everyNativeLivingMobIsExplicitlyRegistered(){
        var missing=new TreeSet<String>();var ids=new HashSet<String>();MorphRegistry.profiles().forEach(p->ids.add(p.id()));
        for(var type:BuiltInRegistries.ENTITY_TYPE)if(DefaultAttributes.hasSupplier(type)){
            String id=BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            if(!Set.of("player","armor_stand","mannequin").contains(id)&&!ids.contains(id))missing.add(id);
        }
        assertEquals(Set.of(),missing,"Native 26.3 catalogue omissions");
    }
    @Test void phaseAndFlightAreAvailableWithoutBodyScaleCaps(){assertTrue(MorphRegistry.find("vex").phases());assertTrue(MorphRegistry.find("ender_dragon").phases());assertTrue(MorphRegistry.find("ender_dragon").dimensions(0).width()>10);}
    @Test void newestSpeciesAndBossesAreIncluded(){for(var id:List.of("sulfur_cube","nautilus","zombie_nautilus","camel_husk","parched","copper_golem","happy_ghast","warden","wither","ender_dragon"))assertNotNull(MorphRegistry.find(id),id);}
    @Test void noInventedUtilityActives(){assertEquals(Active.NONE,MorphRegistry.find("villager").primary());assertEquals(Active.NONE,MorphRegistry.find("sniffer").primary());assertEquals(Active.NONE,MorphRegistry.find("cow").primary());}
    @Test void nativeSkeletonVariantsHaveTheirOwnArrows(){assertEquals(Active.POISON_ARROW,MorphRegistry.find("bogged").primary());assertEquals(Active.WEAK_ARROW,MorphRegistry.find("parched").primary());assertEquals(Active.SLOW_ARROW,MorphRegistry.find("stray").primary());}
    @Test void mediumCubeAndPufferfishGeometryMatchesModels(){assertEquals(MorphRegistry.find("slime").type().getDimensions().width()*2,MorphRegistry.find("slime").dimensions(0).width());var fish=MorphRegistry.find("pufferfish");assertEquals(fish.dimensions(0).width()*2,fish.dimensions(2).width());}
}
