package kr.randommorph.fabric.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.morph.*;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.effect.*;

/** Borrowed vanilla weapons. Virtual ammunition never enters an inventory or becomes recoverable loot. */
public final class MorphWeapons {
    private static final String OWNER="randommorph_weapon_owner",FORM="randommorph_weapon_form";
    public static final Identifier COOLDOWN=Identifier.fromNamespaceAndPath("randommorph","borrowed_weapon");
    private MorphWeapons(){}
    public static void registerInteractionGuards(){
        UseEntityCallback.EVENT.register((player,level,hand,target,hit)->borrowed(player.getItemInHand(hand))
                &&(target instanceof ItemFrame||target instanceof ArmorStand||target instanceof Allay)?InteractionResult.FAIL:InteractionResult.PASS);
        // This event uses null to continue into vanilla; PASS would skip the native block method.
        BlockEvents.USE_ITEM_ON.register((stack,state,level,pos,player,hand,hit)->borrowed(stack)&&state.is(Blocks.DECORATED_POT)?InteractionResult.FAIL:null);
    }
    public static boolean ranged(MorphProfile profile){
        return profile!=null&&switch(profile.primary()){
            case ARROW,POISON_ARROW,SLOW_ARROW,WEAK_ARROW -> true;
            default -> false;
        };
    }
    public static boolean borrowed(ItemStack stack){
        if(stack==null||stack.isEmpty())return false;
        return marked(stack);
    }
    private static boolean marked(DataComponentGetter stack){
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data!=null&&!data.copyTag().getStringOr(OWNER,"").isEmpty();
    }
    public static ItemStack create(MorphProfile profile,java.util.UUID owner){
        if(!ranged(profile))throw new IllegalArgumentException("Not an archer form");
        var stack=new ItemStack(profile.id().equals("pillager")?Items.CROSSBOW:Items.BOW);
        var tag=new CompoundTag();tag.putString(OWNER,owner.toString());tag.putString(FORM,profile.id());
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME,Component.literal(profile.name()+"의 변신 전용 "+(stack.is(Items.CROSSBOW)?"석궁":"활")));
        stack.set(DataComponents.UNBREAKABLE,Unit.INSTANCE);
        stack.set(DataComponents.USE_COOLDOWN,new UseCooldown(0,Optional.of(COOLDOWN)));
        return stack;
    }
    public static boolean matches(Player player,ItemStack stack){
        var profile=((MorphAccess)player).randommorph$form();
        if(!ranged(profile)||!borrowed(stack))return false;
        var tag=stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return tag.getStringOr(OWNER,"").equals(player.getUUID().toString())&&tag.getStringOr(FORM,"").equals(profile.id())
                &&stack.is(profile.id().equals("pillager")?Items.CROSSBOW:Items.BOW);
    }
    public static boolean canUse(Player player,ItemStack stack){
        if(!matches(player,stack)||!player.isAlive()||player.isRemoved()||player.isSpectator())return false;
        if(player instanceof ServerPlayer server){
            var manager=RandomMorphMod.manager(server.level().getServer());var state=manager.abilityState(server);
            return MorphManager.present(server)&&state!=null&&state.cooldown(((MorphAccess)server).randommorph$form().primary())==0;
        }
        return !player.getCooldowns().isOnCooldown(stack);
    }
    public static ItemStack ammunition(MorphProfile profile){
        var stack=new ItemStack(Items.ARROW);
        var effect=switch(profile.primary()){
            case POISON_ARROW -> new MobEffectInstance(MobEffects.POISON,100);
            case SLOW_ARROW -> new MobEffectInstance(MobEffects.SLOWNESS,600);
            case WEAK_ARROW -> new MobEffectInstance(MobEffects.WEAKNESS,600);
            default -> null;
        };
        if(effect!=null){
            stack=new ItemStack(Items.TIPPED_ARROW);
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.empty(),java.util.List.of(effect),Optional.empty()));
        }
        stack.set(DataComponents.INTANGIBLE_PROJECTILE,Unit.INSTANCE);
        return stack;
    }
    public static boolean give(ServerPlayer player,MorphProfile profile){
        if(!ranged(profile))return false;
        audit(player);
        if(borrowed(player.containerMenu.getCarried()))return true;
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)if(matches(player,inventory.getItem(i)))return true;
        for(int i=0;i<36;i++)if(inventory.getItem(i).isEmpty()){
            inventory.setItem(i,create(profile,player.getUUID()));player.containerMenu.broadcastChanges();return true;
        }
        player.sendSystemMessage(Component.literal("전용 무기를 받으려면 인벤토리 한 칸을 비우고 F를 누르세요."),true);return false;
    }
    /** Also handles a saved borrowed item after reconnect and creative-mode duplicates. */
    public static void audit(Player player){
        var carried=player.containerMenu.getCarried();clearNested(carried);
        boolean found=borrowed(carried)&&matches(player,carried);
        if(found)carried.setCount(1);
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);clearNested(stack);if(!borrowed(stack))continue;
            if(!matches(player,stack)||found)inventory.setItem(i,ItemStack.EMPTY);
            else{stack.setCount(1);found=true;}
        }
        if(borrowed(carried)&&!matches(player,carried))player.containerMenu.setCarried(ItemStack.EMPTY);
    }
    public static void clear(Player player){
        if(borrowed(player.getUseItem()))player.stopUsingItem();
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);
            if(borrowed(stack))inventory.setItem(i,ItemStack.EMPTY);else clearNested(stack);
        }
        clearNested(player.containerMenu.getCarried());clearNested(player.inventoryMenu.getCarried());
        if(borrowed(player.containerMenu.getCarried()))player.containerMenu.setCarried(ItemStack.EMPTY);
        if(borrowed(player.inventoryMenu.getCarried()))player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.getCooldowns().removeCooldown(COOLDOWN);
        player.containerMenu.broadcastChanges();
    }
    /** Repair items saved by older versions that allowed a bound weapon into a bundle. */
    public static boolean clearNested(ItemStack stack){
        if(stack==null||stack.isEmpty()||!containsStoredWeapon(stack))return false;
        boolean changed=false;
        var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null){
            var items=new ArrayList<>(bundle.itemCopies().toList());
            if(clearStored(items)){
                // Native copyWithContents reinserts and reverses items; retain unrelated contents/order.
                stack.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(items.stream().filter(item->!item.isEmpty()).map(ItemStackTemplate::fromNonEmptyStack).toList()));
                changed=true;
            }
        }
        var container=stack.get(DataComponents.CONTAINER);
        if(container!=null){
            var items=new ArrayList<>(container.itemCopies().toList());
            if(clearStored(items)){stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));changed=true;}
        }
        return changed;
    }
    /** Inspect immutable templates first: the per-tick audit must not copy every personal container. */
    private static boolean containsStoredWeapon(DataComponentGetter stack){
        var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null)for(var item:bundle.items())if(marked(item)||containsStoredWeapon(item))return true;
        var container=stack.get(DataComponents.CONTAINER);
        if(container!=null)for(var item:container.nonEmptyItems())if(marked(item)||containsStoredWeapon(item))return true;
        return false;
    }
    private static boolean clearStored(List<ItemStack> items){
        boolean changed=false;
        for(int i=0;i<items.size();i++){
            var item=items.get(i);
            if(borrowed(item)){items.set(i,ItemStack.EMPTY);changed=true;}
            else changed|=clearNested(item);
        }
        return changed;
    }
    public static void projectile(LivingEntity owner,ItemStack weapon,Projectile projectile){
        if(!borrowed(weapon))return;
        if(projectile instanceof AbstractArrow arrow)arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        if(owner instanceof ServerPlayer player&&matches(player,weapon))RandomMorphMod.manager(player.level().getServer()).track(player,projectile,300);
    }
    public static void fired(ServerPlayer player,ItemStack weapon){
        if(!matches(player,weapon))return;
        var manager=RandomMorphMod.manager(player.level().getServer());var state=manager.abilityState(player);
        var profile=((MorphAccess)player).randommorph$form();if(state==null||!MorphManager.present(player))return;
        state.used(profile.primary(),manager.config().cooldownTicks(profile,false));syncCooldown(player,state);
    }
    public static void syncCooldown(ServerPlayer player,AbilityState state){
        var profile=((MorphAccess)player).randommorph$form();if(!ranged(profile))return;
        int remaining=state.cooldown(profile.primary());
        if(remaining==0)player.getCooldowns().removeCooldown(COOLDOWN);else player.getCooldowns().addCooldown(COOLDOWN,remaining);
    }
    /** Borrowed equipment may move between player slots, but cannot enter crafting/storage or become loot. */
    public static boolean blockClick(AbstractContainerMenu menu,int slot,int button,ContainerInput input,Player player){
        var clicked=slot>=0&&slot<menu.slots.size()?menu.slots.get(slot):null;
        boolean involved=borrowed(menu.getCarried())||clicked!=null&&borrowed(clicked.getItem());
        if(input==ContainerInput.SWAP&&button>=0&&button<player.getInventory().getContainerSize())involved|=borrowed(player.getInventory().getItem(button));
        if(!involved)return false;
        boolean ownSlot=clicked!=null&&clicked.container==player.getInventory()&&(clicked.getContainerSlot()<36||clicked.getContainerSlot()==40);
        // Native PRIMARY pickup inserts into a bundle even when both slots belong to the player.
        if(input==ContainerInput.PICKUP&&button==0&&(menu.getCarried().has(DataComponents.BUNDLE_CONTENTS)||clicked!=null&&clicked.getItem().has(DataComponents.BUNDLE_CONTENTS)))return true;
        return !ownSlot||input!=ContainerInput.PICKUP&&input!=ContainerInput.SWAP;
    }
}
