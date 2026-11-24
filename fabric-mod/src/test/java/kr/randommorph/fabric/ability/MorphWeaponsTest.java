package kr.randommorph.fabric.ability;
import java.util.*;
import kr.randommorph.fabric.morph.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class MorphWeaponsTest {
    @BeforeAll static void init() throws Exception {SharedConstants.tryDetectVersion();Bootstrap.bootStrap();NativeItemFixtures.bind();}
    static final class Fixture extends Player implements MorphAccess {
        MorphProfile form;Inventory inventory;ItemCooldowns cooldowns;
        Fixture(){super(null,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"fixture"));}
        @Override public GameType gameMode(){return GameType.SURVIVAL;}
        @Override public Inventory getInventory(){return inventory;}
        @Override public ItemCooldowns getCooldowns(){return cooldowns;}
        @Override public ItemStack getUseItem(){return ItemStack.EMPTY;}
        public MorphProfile randommorph$form(){return form;}
        public void randommorph$form(MorphProfile value){form=value;}
        public int randommorph$flags(){return 0;}
        public void randommorph$flags(int value){}
        public kr.randommorph.fabric.core.AnimalState randommorph$animals(){return null;}
    }
    private static Fixture actor(UUID uuid,String form)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var actor=(Fixture)((sun.misc.Unsafe)field.get(null)).allocateInstance(Fixture.class);
        actor.setUUID(uuid);actor.form=MorphRegistry.find(form);
        actor.inventory=new Inventory(actor,new EntityEquipment());actor.cooldowns=new ItemCooldowns();
        actor.containerMenu=new Menu(actor.inventory);return actor;
    }
    static final class Menu extends AbstractContainerMenu {
        Menu(Inventory inventory){super(null,0);addSlot(new Slot(inventory,0,0,0));addSlot(new Slot(inventory,1,0,0));addSlot(new Slot(new SimpleContainer(1),0,0,0));}
        public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        public boolean stillValid(Player player){return true;}
    }
    private static ItemStack bundle(ItemStack item){
        var stack=new ItemStack(Items.BUNDLE);var contents=BundleContents.EMPTY.asMutable();
        assertEquals(item.getCount(),contents.tryInsert(item.copy()));stack.set(DataComponents.BUNDLE_CONTENTS,contents.toImmutable());return stack;
    }
    @Test void onlyArcherFormsReceiveBorrowedWeapons(){
        for(var id:List.of("pillager","skeleton","stray","bogged","parched","illusioner"))assertTrue(MorphWeapons.ranged(MorphRegistry.find(id)));
        for(var id:List.of("villager","frog","shulker","wither_skeleton","piglin"))assertFalse(MorphWeapons.ranged(MorphRegistry.find(id)));
        assertThrows(IllegalArgumentException.class,()->MorphWeapons.create(MorphRegistry.find("cow"),UUID.randomUUID()));
    }
    @Test void actualCrossbowAndBowAreMarkedWithoutGivingArrowItems(){
        var owner=UUID.randomUUID();var crossbow=MorphWeapons.create(MorphRegistry.find("pillager"),owner);var bow=MorphWeapons.create(MorphRegistry.find("skeleton"),owner);
        assertTrue(crossbow.is(Items.CROSSBOW));assertTrue(bow.is(Items.BOW));assertEquals(1,crossbow.getCount());
        assertTrue(MorphWeapons.borrowed(crossbow));assertTrue(crossbow.has(DataComponents.UNBREAKABLE));
        assertTrue(crossbow.get(DataComponents.CHARGED_PROJECTILES).isEmpty());
        assertEquals(Optional.of(MorphWeapons.COOLDOWN),crossbow.get(DataComponents.USE_COOLDOWN).cooldownGroup());
    }
    @Test void personalWeaponsAndOrdinaryItemsAreNotOwned(){
        for(var item:List.of(Items.BOW,Items.CROSSBOW,Items.ARROW,Items.DIAMOND_SWORD))assertFalse(MorphWeapons.borrowed(new ItemStack(item)));
        assertFalse(MorphWeapons.borrowed(ItemStack.EMPTY));assertFalse(MorphWeapons.borrowed(null));
    }
    @Test void weaponIsBoundToBothPlayerAndCurrentSpecies()throws Exception{
        var owner=UUID.randomUUID();var weapon=MorphWeapons.create(MorphRegistry.find("pillager"),owner);
        var actor=actor(owner,"pillager");assertTrue(MorphWeapons.matches(actor,weapon));
        assertFalse(MorphWeapons.matches(actor(UUID.randomUUID(),"pillager"),weapon));
        actor.form=MorphRegistry.find("skeleton");assertFalse(MorphWeapons.matches(actor,weapon));
        actor.form=null;assertFalse(MorphWeapons.matches(actor,weapon));
    }
    @Test void virtualAmmoIsNotAnInventoryWeaponAndEachCallHasItsOwnStack(){
        var profile=MorphRegistry.find("pillager");var first=MorphWeapons.ammunition(profile);var second=MorphWeapons.ammunition(profile);
        assertTrue(first.is(Items.ARROW));assertTrue(first.has(DataComponents.INTANGIBLE_PROJECTILE));assertFalse(MorphWeapons.borrowed(first));
        first.shrink(1);assertTrue(first.isEmpty());assertEquals(1,second.getCount());
    }
    @Test void variantAmmoKeepsNativeCustomDurationsAndEffects(){
        var cases=Map.of("bogged",net.minecraft.world.effect.MobEffects.POISON,"stray",net.minecraft.world.effect.MobEffects.SLOWNESS,"parched",net.minecraft.world.effect.MobEffects.WEAKNESS);
        for(var entry:cases.entrySet()){
            var ammo=MorphWeapons.ammunition(MorphRegistry.find(entry.getKey()));assertTrue(ammo.is(Items.TIPPED_ARROW));
            var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>();ammo.get(DataComponents.POTION_CONTENTS).getAllEffects().forEach(effects::add);
            assertEquals(1,effects.size());assertEquals(entry.getValue(),effects.getFirst().getEffect());assertEquals(entry.getKey().equals("bogged")?100:600,effects.getFirst().getDuration());
            assertTrue(ammo.has(DataComponents.INTANGIBLE_PROJECTILE));
        }
    }
    @Test void nativeBundleInsertionPreviouslyHidBoundWeaponsAndCleanupRepairsIt(){
        var weapon=MorphWeapons.create(MorphRegistry.find("pillager"),UUID.randomUUID());
        var stack=bundle(weapon);var saved=stack.get(DataComponents.BUNDLE_CONTENTS);
        assertTrue(MorphWeapons.borrowed(saved.itemCopies().findFirst().orElseThrow()));
        assertFalse(MorphWeapons.borrowed(stack)); // The former outer-stack audit missed this.
        assertTrue(MorphWeapons.clearNested(stack));assertTrue(stack.get(DataComponents.BUNDLE_CONTENTS).isEmpty());
        assertFalse(saved.isEmpty());assertFalse(MorphWeapons.clearNested(stack));
    }
    @Test void cleanupRemovesNestedWeaponsWithoutMovingOrChangingPersonalContents(){
        var weapon=MorphWeapons.create(MorphRegistry.find("skeleton"),UUID.randomUUID());
        var personal=new ItemStack(Items.DIAMOND_SWORD);personal.setDamageValue(12);
        var stack=new ItemStack(Items.SHULKER_BOX);
        stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(personal,ItemStack.EMPTY,bundle(weapon),new ItemStack(Items.ARROW,7),weapon)));
        assertTrue(MorphWeapons.clearNested(stack));
        var items=stack.get(DataComponents.CONTAINER).itemCopies().toList();
        assertTrue(ItemStack.matches(personal,items.get(0)));assertTrue(items.get(1).isEmpty());
        assertTrue(items.get(2).is(Items.BUNDLE));assertTrue(items.get(2).get(DataComponents.BUNDLE_CONTENTS).isEmpty());
        assertTrue(items.get(3).is(Items.ARROW));assertEquals(7,items.get(3).getCount());
        // Native container contents may trim trailing empty slots, but retain all preceding indexes.
        assertTrue(items.size()==4||items.get(4).isEmpty());
    }
    @Test void unrelatedContainerComponentsAndBundleOrderRemainUntouched(){
        var personal=bundle(new ItemStack(Items.ARROW,7));var original=personal.get(DataComponents.BUNDLE_CONTENTS);
        assertFalse(MorphWeapons.clearNested(personal));assertSame(original,personal.get(DataComponents.BUNDLE_CONTENTS));
        var malformed=new ItemStack(Items.BUNDLE);
        var weapon=MorphWeapons.create(MorphRegistry.find("skeleton"),UUID.randomUUID());
        malformed.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(new ItemStackTemplate(Items.ARROW,2),ItemStackTemplate.fromNonEmptyStack(weapon),new ItemStackTemplate(Items.TIPPED_ARROW,3))));
        assertTrue(MorphWeapons.clearNested(malformed));
        var retained=malformed.get(DataComponents.BUNDLE_CONTENTS).itemCopies().toList();
        assertEquals(2,retained.size());assertTrue(retained.get(0).is(Items.ARROW));assertEquals(2,retained.get(0).getCount());
        assertTrue(retained.get(1).is(Items.TIPPED_ARROW));assertEquals(3,retained.get(1).getCount());
    }
    @Test void bothNativeBundleInsertionDirectionsAreBlockedButOrdinaryMovementRemains()throws Exception{
        var player=actor(UUID.randomUUID(),"pillager");var weapon=MorphWeapons.create(player.form,player.getUUID());var menu=player.containerMenu;
        player.inventory.setItem(0,new ItemStack(Items.BUNDLE));menu.setCarried(weapon);
        assertTrue(MorphWeapons.blockClick(menu,0,0,ContainerInput.PICKUP,player));
        player.inventory.setItem(0,weapon);menu.setCarried(new ItemStack(Items.BUNDLE));
        assertTrue(MorphWeapons.blockClick(menu,0,0,ContainerInput.PICKUP,player));
        menu.setCarried(ItemStack.EMPTY);assertFalse(MorphWeapons.blockClick(menu,0,0,ContainerInput.PICKUP,player));
        menu.setCarried(new ItemStack(Items.DIAMOND_SWORD));assertFalse(MorphWeapons.blockClick(menu,0,0,ContainerInput.PICKUP,player));
        menu.setCarried(weapon);assertTrue(MorphWeapons.blockClick(menu,2,0,ContainerInput.PICKUP,player));
        player.inventory.setItem(1,new ItemStack(Items.BUNDLE));assertFalse(MorphWeapons.blockClick(menu,0,1,ContainerInput.SWAP,player));
    }
    @Test void auditRepairsNestedItemsAndKeepsExactlyOneMatchingCarriedWeapon()throws Exception{
        var player=actor(UUID.randomUUID(),"pillager");var weapon=MorphWeapons.create(player.form,player.getUUID());
        var carried=weapon.copyWithCount(4);player.containerMenu.setCarried(carried);
        player.inventory.setItem(0,weapon);player.inventory.setItem(1,bundle(weapon));
        var personal=new ItemStack(Items.DIAMOND_SWORD);player.inventory.setItem(2,personal);
        MorphWeapons.audit(player);
        assertSame(carried,player.containerMenu.getCarried());assertEquals(1,carried.getCount());
        assertTrue(player.inventory.getItem(0).isEmpty());assertTrue(player.inventory.getItem(1).get(DataComponents.BUNDLE_CONTENTS).isEmpty());
        assertSame(personal,player.inventory.getItem(2));
        player.form=null;MorphWeapons.audit(player);assertTrue(player.containerMenu.getCarried().isEmpty());
    }
}
