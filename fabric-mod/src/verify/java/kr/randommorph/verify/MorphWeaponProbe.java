package kr.randommorph.verify;
import java.util.*;
import kr.randommorph.fabric.ability.MorphWeapons;
import kr.randommorph.fabric.morph.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.GameType;

/** Actual transformed ammo/drop/menu hooks and vanilla draw, with worldless, constructor-free player fixtures. */
public final class MorphWeaponProbe {
    public static final class Actor extends Player implements MorphAccess {
        MorphProfile form;Inventory inventory;ItemCooldowns cooldowns;boolean alive=true;
        Actor(){super(null,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"fixture"));}
        @Override public GameType gameMode(){return GameType.SURVIVAL;}
        @Override public Inventory getInventory(){return inventory;}
        @Override public ItemCooldowns getCooldowns(){return cooldowns;}
        @Override public boolean isAlive(){return alive;}
        @Override public boolean hasInfiniteMaterials(){return false;}
        @Override public ItemStack getUseItem(){return ItemStack.EMPTY;}
        @Override public ItemStack getItemInHand(net.minecraft.world.InteractionHand hand){return hand==net.minecraft.world.InteractionHand.MAIN_HAND?inventory.getSelectedItem():inventory.getItem(40);}
        public MorphProfile randommorph$form(){return form;}
        public void randommorph$form(MorphProfile profile){form=profile;}
        public int randommorph$flags(){return 0;}
        public void randommorph$flags(int flags){}
        public kr.randommorph.fabric.core.AnimalState randommorph$animals(){return null;}
    }
    static final class Menu extends AbstractContainerMenu {
        Menu(Inventory inventory,SimpleContainer chest){super(null,0);addSlot(new Slot(inventory,0,0,0));addSlot(new Slot(inventory,1,0,0));addSlot(new Slot(chest,0,0,0));}
        public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        public boolean stillValid(Player player){return true;}
    }
    private static <T> T unassigned(Class<T> type)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run(String side)throws Exception{
        NativeItemFixtures.bind();
        var player=unassigned(Actor.class);player.setUUID(UUID.randomUUID());player.alive=true;
        player.inventory=new Inventory(player,new EntityEquipment());player.cooldowns=new ItemCooldowns();
        var nativeInventory=Player.class.getDeclaredField("inventory");nativeInventory.setAccessible(true);nativeInventory.set(player,player.inventory);
        var chest=new SimpleContainer(1);var menu=new Menu(player.inventory,chest);player.containerMenu=menu;
        var inventoryMenu=Player.class.getDeclaredField("inventoryMenu");inventoryMenu.setAccessible(true);inventoryMenu.set(player,unassigned(InventoryMenu.class));
        var draw=ProjectileWeaponItem.class.getDeclaredMethod("draw",ItemStack.class,ItemStack.class,LivingEntity.class);draw.setAccessible(true);
        int cases=0;
        for(var id:List.of("pillager","skeleton","stray","bogged","parched","illusioner")){
            player.form=MorphRegistry.find(id);var weapon=MorphWeapons.create(player.form,player.getUUID());
            var ammo=player.getProjectile(weapon);require(!ammo.isEmpty()&&ammo.has(DataComponents.INTANGIBLE_PROJECTILE),"Actual Player ammo hook failed: "+id);
            var projectiles=(List<?>)draw.invoke(null,weapon,ammo,player);require(projectiles.size()==1,"Vanilla draw failed");
            require(((ItemStack)projectiles.getFirst()).has(DataComponents.INTANGIBLE_PROJECTILE),"Recoverable virtual ammo");
            require(player.inventory.isEmpty(),"Ammo entered an inventory");cases++;
        }
        player.form=MorphRegistry.find("pillager");var weapon=MorphWeapons.create(player.form,player.getUUID());
        player.cooldowns.addCooldown(MorphWeapons.COOLDOWN,40);require(player.getProjectile(weapon).isEmpty(),"Weapon cooldown ignored");player.cooldowns.removeCooldown(MorphWeapons.COOLDOWN);cases++;
        player.form=null;require(player.getProjectile(weapon).isEmpty(),"Borrowed weapon usable after clear");cases++;
        player.form=MorphRegistry.find("pillager");player.alive=false;require(player.getProjectile(weapon).isEmpty(),"Dead player has ammo");player.alive=true;cases++;
        var foreign=MorphWeapons.create(player.form,UUID.randomUUID());require(player.getProjectile(foreign).isEmpty(),"Another player can use borrowed weapon");cases++;
        require(player.getProjectile(new ItemStack(Items.BOW)).isEmpty(),"Personal bow became unlimited");cases++;
        var arrow=unassigned(net.minecraft.world.entity.projectile.arrow.Arrow.class);arrow.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED;
        MorphWeapons.projectile(player,weapon,arrow);require(arrow.pickup==net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED,"Virtual arrow can be picked up");cases++;
        require(player.drop(weapon,false,net.minecraft.util.Prediction.SERVER_ONLY)==null,"Borrowed weapon became a drop");cases++;
        player.inventory.setItem(0,weapon);player.inventory.setItem(1,new ItemStack(Items.DIAMOND_SWORD));
        require(MorphWeapons.blockClick(menu,0,0,ContainerInput.QUICK_MOVE,player),"Shift click escaped binding");
        require(MorphWeapons.blockClick(menu,0,0,ContainerInput.THROW,player),"Menu throw escaped binding");
        require(!MorphWeapons.blockClick(menu,0,0,ContainerInput.PICKUP,player),"Player inventory movement blocked");cases++;
        menu.setCarried(weapon);menu.clicked(2,0,ContainerInput.PICKUP,player);
        require(chest.isEmpty()&&menu.getCarried()==weapon,"Actual menu hook allowed chest storage");cases++;
        menu.setCarried(ItemStack.EMPTY);MorphWeapons.clear(player);
        require(player.inventory.getItem(0).isEmpty()&&player.inventory.getItem(1).is(Items.DIAMOND_SWORD),"Cleanup changed personal equipment");cases++;
        // Run the native mutation used by both BundleItem PRIMARY-click handlers: it accepts a bound bow.
        var hidden=BundleContents.EMPTY.asMutable();var hiddenWeapon=weapon.copy();
        require(hidden.tryInsert(hiddenWeapon)==1&&hiddenWeapon.isEmpty(),"Native bundle storage regression fixture failed");
        var savedBundle=new ItemStack(Items.BUNDLE);savedBundle.set(DataComponents.BUNDLE_CONTENTS,hidden.toImmutable());
        require(!MorphWeapons.borrowed(savedBundle)&&MorphWeapons.borrowed(savedBundle.get(DataComponents.BUNDLE_CONTENTS).itemCopies().findFirst().orElseThrow()),"Previous outer-only audit failure not reproduced");cases++;
        var emptyBundle=new ItemStack(Items.BUNDLE);player.inventory.setItem(0,emptyBundle);menu.setCarried(weapon);
        menu.clicked(0,0,ContainerInput.PICKUP,player);
        require(menu.getCarried()==weapon&&emptyBundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),"Actual menu hook let held weapon enter bundle");cases++;
        player.inventory.setItem(0,weapon);menu.setCarried(emptyBundle);menu.clicked(0,0,ContainerInput.PICKUP,player);
        require(player.inventory.getItem(0)==weapon&&emptyBundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),"Actual menu hook let held bundle swallow weapon");cases++;
        var box=new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND_SWORD),ItemStack.EMPTY,savedBundle)));
        player.inventory.setItem(0,box);menu.setCarried(ItemStack.EMPTY);MorphWeapons.clear(player);
        var repaired=box.get(DataComponents.CONTAINER).itemCopies().toList();
        require(repaired.size()==3&&repaired.get(0).is(Items.DIAMOND_SWORD)&&repaired.get(1).isEmpty()&&repaired.get(2).get(DataComponents.BUNDLE_CONTENTS).isEmpty(),"Cleanup left nested bound weapon or lost/reordered personal contents");cases++;
        menu.setCarried(savedBundle.copy());MorphWeapons.clear(player);
        require(menu.getCarried().is(Items.BUNDLE)&&menu.getCarried().get(DataComponents.BUNDLE_CONTENTS).isEmpty(),"Cleanup missed cursor bundle");cases++;
        var carried=weapon.copyWithCount(4);menu.setCarried(carried);player.inventory.setItem(0,weapon.copy());MorphWeapons.audit(player);
        require(carried.getCount()==1&&player.inventory.getItem(0).isEmpty(),"Audit retained stacked/copy bound weapons");cases++;
        menu.setCarried(ItemStack.EMPTY);
        if(side.equals("CLIENT"))MorphWeapons.registerInteractionGuards(); // SERVER invokes the real common entrypoint.
        player.inventory.setItem(0,weapon);
        for(var targetType:List.of(net.minecraft.world.entity.decoration.ItemFrame.class,net.minecraft.world.entity.decoration.GlowItemFrame.class,
                net.minecraft.world.entity.decoration.ArmorStand.class,net.minecraft.world.entity.animal.allay.Allay.class)){
            var target=unassigned(targetType);
            require(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player,null,InteractionHand.MAIN_HAND,target,null)==InteractionResult.FAIL,"Registered callback let bound weapon enter "+targetType.getSimpleName());cases++;
        }
        var allay=unassigned(net.minecraft.world.entity.animal.allay.Allay.class);player.inventory.setItem(0,new ItemStack(Items.BOW));
        require(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player,null,InteractionHand.MAIN_HAND,allay,null)==InteractionResult.PASS,"Personal item interaction blocked");cases++;
        player.inventory.setItem(0,weapon);
        require(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player,null,InteractionHand.MAIN_HAND,unassigned(net.minecraft.world.entity.npc.villager.Villager.class),null)==InteractionResult.PASS,"Unrelated entity interaction blocked");cases++;
        var pot=net.minecraft.world.level.block.Blocks.DECORATED_POT.defaultBlockState();
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.ZERO,net.minecraft.core.Direction.UP,net.minecraft.core.BlockPos.ZERO,false);
        require(pot.useItemOn(weapon,null,player,InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL,"Actual Fabric block-state hook let bound weapon reach pot storage");cases++;
        require(net.fabricmc.fabric.api.event.player.BlockEvents.USE_ITEM_ON.invoker().useItemOn(new ItemStack(Items.BOW),pot,null,net.minecraft.core.BlockPos.ZERO,player,InteractionHand.MAIN_HAND,hit)==null,"Personal pot insertion no longer follows native path");cases++;
        require(net.fabricmc.fabric.api.event.player.BlockEvents.USE_ITEM_ON.invoker().useItemOn(weapon,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),null,net.minecraft.core.BlockPos.ZERO,player,InteractionHand.MAIN_HAND,hit)==null,"Chest opening blocked while holding bound weapon");cases++;
        // Accessors and merged gravity/movement handlers must verify on both environments.
        require(kr.randommorph.fabric.mixin.ShulkerAccess.class.isAssignableFrom(net.minecraft.world.entity.monster.Shulker.class),"Native shulker invokers missing");
        require(kr.randommorph.fabric.mixin.WeaponAnimationAccess.class.isAssignableFrom(LivingEntity.class),"Use animation accessor missing");
        System.out.println("RANDOMMORPH_WEAPON_FIXTURES_OK side="+side+" cases="+cases+"; actual hooks/native draw, no player constructor/world/server/game was started");
    }
}
