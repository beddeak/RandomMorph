package kr.randommorph.fabric.ability;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Runs native LivingEntity.heal and ItemStack.consume with scalar health, without a world or player constructor. */
class GolemRepairTest {
    @BeforeAll static void init()throws Exception{
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();NativeItemFixtures.bind();
    }
    static final class Fixture extends Player {
        float health,maximum;boolean creative,healingBlocked;
        Fixture(){super(null,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"repair_fixture"));}
        @Override public GameType gameMode(){return creative?GameType.CREATIVE:GameType.SURVIVAL;}
        @Override public float getHealth(){return health;}
        @Override public void setHealth(float value){if(!healingBlocked)health=Math.clamp(value,0,maximum);}
        @Override public boolean hasInfiniteMaterials(){return creative;}
    }
    private static Fixture fixture(float health,float maximum,boolean creative)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var body=(Fixture)((sun.misc.Unsafe)field.get(null)).allocateInstance(Fixture.class);
        body.health=health;body.maximum=maximum;body.creative=creative;return body;
    }
    @Test void survivalRepairHealsTwentyFiveAndConsumesExactlyOneIngot()throws Exception{
        var target=fixture(40,100,false);var user=fixture(20,20,false);var iron=new ItemStack(Items.IRON_INGOT,3);
        assertTrue(AnimalActions.repair(target,user,iron));assertEquals(65,target.health);assertEquals(2,iron.getCount());
        assertEquals(20,user.health);
    }
    @Test void partialRepairRespectsActualMaximumAndFullHealthConsumesNothing()throws Exception{
        var target=fixture(18,20,false);var user=fixture(20,20,false);var iron=new ItemStack(Items.IRON_INGOT,2);
        assertTrue(AnimalActions.repair(target,user,iron));assertEquals(20,target.health);assertEquals(1,iron.getCount());
        assertFalse(AnimalActions.repair(target,user,iron));assertEquals(1,iron.getCount());
    }
    @Test void nativeCreativeConsumptionRetainsTheIngot()throws Exception{
        var target=fixture(50,100,false);var user=fixture(20,20,true);var iron=new ItemStack(Items.IRON_INGOT);
        assertTrue(AnimalActions.repair(target,user,iron));assertEquals(75,target.health);assertEquals(1,iron.getCount());
    }
    @Test void deadOrBlockedHealingCannotSpendIronAndEmptyOrWrongItemsCannotHeal()throws Exception{
        var target=fixture(0,100,false);var user=fixture(20,20,false);var iron=new ItemStack(Items.IRON_INGOT,2);
        assertFalse(AnimalActions.repair(target,user,iron));assertEquals(0,target.health);assertEquals(2,iron.getCount());
        target.health=30;target.healingBlocked=true;
        assertFalse(AnimalActions.repair(target,user,iron));assertEquals(30,target.health);assertEquals(2,iron.getCount());
        target.healingBlocked=false;
        assertFalse(AnimalActions.repair(target,user,ItemStack.EMPTY));
        assertFalse(AnimalActions.repair(target,user,new ItemStack(Items.BUCKET)));assertEquals(30,target.health);
    }
}
