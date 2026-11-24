package kr.randommorph.fabric;

import java.nio.file.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import kr.randommorph.fabric.morph.MorphRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Config files in a JUnit temporary directory; never reads or changes an installed game config. */
class MorphConfigIoTest {
    @TempDir Path directory;
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void existingJsonWithoutCooldownFieldsKeepsDefaults()throws Exception{
        var path=directory.resolve("randommorph.json");Files.writeString(path,"{\"mobs\":{\"ender_dragon\":{\"weight\":0.2}}}");
        var c=MorphConfig.load(path);assertEquals(100,c.cooldownTicks(MorphRegistry.find("ender_dragon"),false));
        assertEquals(.2,c.mobs.get("ender_dragon").weight);assertEquals(90,c.mobs.size());
    }
    @Test void savedOverridesSurviveFreshLoadAndKeepOtherSettings()throws Exception{
        var path=directory.resolve("randommorph.json");var c=new MorphConfig();
        c.morphIntervalSeconds=30;c.blockDestruction=false;c.mobs.get("wither").primaryCooldownSeconds=0;c.mobs.get("wither").secondaryCooldownSeconds=8;
        c.save(path);var loaded=MorphConfig.load(path);
        assertEquals(0,loaded.cooldownTicks(MorphRegistry.find("wither"),false));assertEquals(160,loaded.cooldownTicks(MorphRegistry.find("wither"),true));
        assertEquals(30,loaded.morphIntervalSeconds);assertFalse(loaded.blockDestruction);
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void resetOmitsTheOverrideAndRestoresDefaultAfterReload()throws Exception{
        var path=directory.resolve("randommorph.json");var c=new MorphConfig();c.mobs.get("wither").secondaryCooldownSeconds=8;c.save(path);
        var loaded=MorphConfig.load(path);loaded.mobs.get("wither").secondaryCooldownSeconds=null;loaded.save(path);
        assertFalse(Files.readString(path).contains("secondaryCooldownSeconds"));assertEquals(200,MorphConfig.load(path).cooldownTicks(MorphRegistry.find("wither"),true));
    }
    @Test void invalidSaveAndFailedReplacementPreserveLiveRulesAndPreviousFile()throws Exception{
        var path=directory.resolve("randommorph.json");var live=new MorphConfig();live.mobs.get("ender_dragon").primaryCooldownSeconds=3;live.save(path);
        String previous=Files.readString(path);var invalid=live.copy();invalid.mobs.get("ender_dragon").primaryCooldownSeconds=-1;
        assertThrows(IllegalArgumentException.class,()->invalid.save(path));assertEquals(previous,Files.readString(path));
        var blocked=directory.resolve("cannot-replace");Files.createDirectory(blocked);Files.writeString(blocked.resolve("keep"),"preserve");
        var staged=live.copy();staged.mobs.get("ender_dragon").primaryCooldownSeconds=7;
        assertThrows(java.io.IOException.class,()->staged.save(blocked));
        assertEquals(3,live.mobs.get("ender_dragon").primaryCooldownSeconds);assertEquals(previous,Files.readString(path));
        try(var files=Files.list(directory)){assertFalse(files.anyMatch(p->p.toString().endsWith(".tmp")));}
    }
}
