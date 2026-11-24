package kr.randommorph.fabric.ability;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class HarvestTest {
    @BeforeAll static void init()throws Exception{
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();NativeItemFixtures.bind();
    }
    @Test void allNativeDyesCarryTheMatchingColorComponentAndOnlyDyeSheep(){
        for(int color=0;color<16;color++){
            var stack=Items.DYE.pick(DyeColor.byId(color)).getDefaultInstance();assertEquals(color,stack.get(DataComponents.DYE).getId());
            assertEquals(AnimalActions.Harvest.DYE,AnimalActions.harvest("sheep",stack));
            assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest("mooshroom",stack));
        }
    }
    @Test void bucketBowlAndShearsUseSeparateNativeContainerActions(){
        for(var form:List.of("cow","mooshroom","goat"))assertEquals(AnimalActions.Harvest.MILK,AnimalActions.harvest(form,Items.BUCKET.getDefaultInstance()));
        assertEquals(AnimalActions.Harvest.STEW,AnimalActions.harvest("mooshroom",Items.BOWL.getDefaultInstance()));
        assertEquals(AnimalActions.Harvest.SHEEP_SHEAR,AnimalActions.harvest("sheep",Items.SHEARS.getDefaultInstance()));
        assertEquals(AnimalActions.Harvest.MOOSHROOM_SHEAR,AnimalActions.harvest("mooshroom",Items.SHEARS.getDefaultInstance()));
        assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest("cow",Items.BOWL.getDefaultInstance()));
        assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest("cow",Items.SHEARS.getDefaultInstance()));
    }
    @Test void golemInteractionItemsCannotRepairOrShearOtherSpecies(){
        assertEquals(AnimalActions.Harvest.IRON_GOLEM_REPAIR,AnimalActions.harvest("iron_golem",Items.IRON_INGOT.getDefaultInstance()));
        assertEquals(AnimalActions.Harvest.SNOW_GOLEM_SHEAR,AnimalActions.harvest("snow_golem",Items.SHEARS.getDefaultInstance()));
        for(var form:List.of("cow","goat","snow_golem","copper_golem","player"))
            assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest(form,Items.IRON_INGOT.getDefaultInstance()));
        for(var form:List.of("iron_golem","copper_golem","goat"))
            assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest(form,Items.SHEARS.getDefaultInstance()));
    }
    @Test void snowPumpkinRestorationIsNotInventedAndEmptyStacksFallThrough(){
        assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest("snow_golem",Items.CARVED_PUMPKIN.getDefaultInstance()));
        for(var form:List.of("goat","iron_golem","snow_golem","cow","sheep"))
            assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest(form,ItemStack.EMPTY));
    }
    @Test void filledContainersWeaponsAndUnrelatedFormsFallThroughToNormalUse(){
        for(var form:List.of("cow","mooshroom","sheep","goat","iron_golem","snow_golem","villager","player"))
            for(var item:List.of(Items.MILK_BUCKET,Items.MUSHROOM_STEW,Items.DIAMOND_SWORD,Items.BOW))
                assertEquals(AnimalActions.Harvest.NONE,AnimalActions.harvest(form,item.getDefaultInstance()));
    }
}
