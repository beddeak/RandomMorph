package kr.randommorph.fabric.morph;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeFoundationTest {
    @BeforeAll static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
    }
    @Test void catalogueHasAllExplicitSpecies() {
        assertEquals(90,MorphRegistry.profiles().size());
        assertNull(MorphRegistry.find("player"));assertNull(MorphRegistry.find("armor_stand"));
        assertNotNull(MorphRegistry.find("vex"));assertNull(MorphRegistry.find("mannequin"));assertNotNull(MorphRegistry.find("ENDERMAN"));
    }
    @Test void tallAndSmallDimensionsAreNative() {
        var small=MorphRegistry.find("endermite").type().getDimensions();
        var tall=MorphRegistry.find("enderman").type().getDimensions();
        assertTrue(small.height()<.5);assertTrue(tall.height()>2.5);
        assertTrue(small.eyeHeight()>0 && small.eyeHeight()<small.height());
        assertTrue(tall.eyeHeight()>2 && tall.eyeHeight()<tall.height());
    }
    @Test void nativeDefaultsExistForEveryForm() {
        for(var profile:MorphRegistry.profiles()) {
            var defaults=DefaultAttributes.getSupplier(profile.type());
            assertNotNull(defaults,profile.id());
            assertTrue(defaults.getBaseValue(Attributes.MAX_HEALTH)>0,profile.id());
        }
        assertEquals(40,DefaultAttributes.getSupplier(MorphRegistry.find("enderman").type()).getBaseValue(Attributes.MAX_HEALTH));
    }
    @Test void rerollNeverRepeatsCurrentForm() {
        var random=new Random(123);
        for(var profile:MorphRegistry.profiles())for(int i=0;i<100;i++)assertNotEquals(profile.id(),MorphRegistry.random(random,profile.id()).id());
    }
    @Test void dedicatedServerClassesHaveNoClientReferences() throws Exception {
        try(var files=Files.walk(Path.of("build/classes/java/main"))) {
            for(var path:files.filter(p->p.toString().endsWith(".class")).toList()) {
                var constants=new String(Files.readAllBytes(path),StandardCharsets.ISO_8859_1);
                assertFalse(constants.contains("net/minecraft/client/"),path.toString());
                assertFalse(constants.contains("net/fabricmc/fabric/api/client/"),path.toString());
                assertFalse(constants.contains("kr/randommorph/fabric/client/"),path.toString());
            }
        }
    }
}
