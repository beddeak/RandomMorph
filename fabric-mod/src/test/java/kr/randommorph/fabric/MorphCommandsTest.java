package kr.randommorph.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.phys.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Brigadier parsing/suggestions with a server-free command source; mutation callbacks are not run. */
class MorphCommandsTest {
    @BeforeAll static void init(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private CommandDispatcher<CommandSourceStack> dispatcher(){var d=new CommandDispatcher<CommandSourceStack>();MorphCommands.register(d);return d;}
    private CommandSourceStack source(PermissionSet permissions){
        return new CommandSourceStack(CommandSource.NULL,Vec3.ZERO,Vec2.ZERO,null,permissions,Component.literal("test"),null);
    }
    @Test void cubeSizeOffersOnlySupportedSizesAndOptionalTargets()throws Exception{
        var d=dispatcher();var source=source(PermissionSet.ALL_PERMISSIONS);
        for(var command:java.util.List.of("morph size 1","morph size 2","morph size 4 @a")){
            var parsed=d.parse(command,source);assertFalse(parsed.getReader().canRead(),command);assertNotNull(parsed.getContext().getCommand(),command);
        }
        for(var command:java.util.List.of("morph size 0","morph size 3","morph size 5","morph size 1.5"))
            assertTrue(d.parse(command,source).getReader().canRead(),command);
        var suggestions=d.getCompletionSuggestions(d.parse("morph size ",source)).get().getList().stream().map(x->x.getText()).toList();
        assertEquals(java.util.List.of("1","2","4"),suggestions);
        assertTrue(d.getCompletionSuggestions(d.parse("morph si",source(PermissionSet.NO_PERMISSIONS))).get().isEmpty());
    }
    @Test void shorthandExplicitSlotsQueryAndResetParseCompletely(){
        var d=dispatcher();var source=source(PermissionSet.ALL_PERMISSIONS);
        for(var command:java.util.List.of("morph cooldown","morph cooldown reset","morph cooldown ender_dragon",
                "morph cooldown ender_dragon 3","morph cooldown ender_dragon reset","morph cooldown wither primary 0",
                "morph cooldown wither secondary 3600","morph cooldown wither secondary","morph cooldown wither secondary reset")){
            var parsed=d.parse(command,source);assertFalse(parsed.getReader().canRead(),command);assertNotNull(parsed.getContext().getCommand(),command);
        }
    }
    @Test void malformedDurationAndExtraArgumentsCannotExecute(){
        var d=dispatcher();var source=source(PermissionSet.ALL_PERMISSIONS);
        for(var command:java.util.List.of("morph cooldown wither -1","morph cooldown wither 3601","morph cooldown wither 1.5",
                "morph cooldown wither secondary nope","morph cooldown wither primary 3 extra"))assertTrue(d.parse(command,source).getReader().canRead(),command);
    }
    @Test void permissionGateHidesAndRejectsCooldownMutation()throws Exception{
        var d=dispatcher();var source=source(PermissionSet.NO_PERMISSIONS);
        assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class,()->d.execute("morph cooldown wither 0",source));
        assertTrue(d.getCompletionSuggestions(d.parse("morph co",source)).get().isEmpty());
    }
    @Test void tabCompletionOffersAbilityFormsAndBothSlots()throws Exception{
        var d=dispatcher();var source=source(PermissionSet.ALL_PERMISSIONS);
        var forms=d.getCompletionSuggestions(d.parse("morph cooldown ",source)).get().getList().stream().map(s->s.getText()).toList();
        assertTrue(forms.contains("ender_dragon"));assertTrue(forms.contains("wither"));assertFalse(forms.contains("vex"));
        var slots=d.getCompletionSuggestions(d.parse("morph cooldown wither ",source)).get().getList().stream().map(s->s.getText()).toList();
        assertTrue(slots.containsAll(java.util.List.of("primary","secondary","reset")));
    }
    @Test void unknownMobAndMissingAbilityAreRejectedBeforeAccessingAServer()throws Exception{
        var d=dispatcher();var source=source(PermissionSet.ALL_PERMISSIONS);
        assertEquals(0,d.execute("morph cooldown no_such_mob 3",source));
        assertEquals(0,d.execute("morph cooldown vex 3",source));assertEquals(0,d.execute("morph cooldown ender_dragon secondary 3",source));
    }
}
