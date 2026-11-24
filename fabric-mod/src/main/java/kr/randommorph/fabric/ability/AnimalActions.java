package kr.randommorph.fabric.ability;

import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.mixin.ShearingLootAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Server-owned natural actions. Harvested items are ordinary drops, not temporary ability entities. */
public final class AnimalActions {
    private AnimalActions(){}
    public enum Harvest {NONE,MILK,DYE,STEW,SHEEP_SHEAR,MOOSHROOM_SHEAR,SNOW_GOLEM_SHEAR,IRON_GOLEM_REPAIR}
    public static Harvest harvest(String form,ItemStack tool){
        if((form.equals("cow")||form.equals("mooshroom")||form.equals("goat"))&&tool.is(Items.BUCKET))return Harvest.MILK;
        if(form.equals("iron_golem")&&tool.is(Items.IRON_INGOT))return Harvest.IRON_GOLEM_REPAIR;
        if(form.equals("sheep")&&tool.getItem() instanceof DyeItem&&tool.get(net.minecraft.core.component.DataComponents.DYE)!=null)return Harvest.DYE;
        if(form.equals("mooshroom")&&tool.is(Items.BOWL))return Harvest.STEW;
        if(tool.is(Items.SHEARS)){if(form.equals("sheep"))return Harvest.SHEEP_SHEAR;if(form.equals("mooshroom"))return Harvest.MOOSHROOM_SHEAR;if(form.equals("snow_golem"))return Harvest.SNOW_GOLEM_SHEAR;}
        return Harvest.NONE;
    }
    public static InteractionResult interact(Entity entity,Player actor,InteractionHand hand) {
        var profile=AbilityHooks.profile(entity);
        if(profile==null||!entity.isAlive()||entity.isRemoved()||entity.isSpectator()||actor.isSpectator())return InteractionResult.PASS;
        var tool=actor.getItemInHand(hand);
        var harvest=harvest(profile.id(),tool);
        if(harvest==Harvest.NONE)return InteractionResult.PASS;
        if(!(entity instanceof ServerPlayer target)||!(actor instanceof ServerPlayer user))return InteractionResult.CONSUME;
        if(!MorphManager.present(target)||!MorphManager.present(user)||user==target)return InteractionResult.PASS;
        var manager=RandomMorphMod.manager(target.level().getServer());
        if(manager.abilityState(target)==null)return InteractionResult.PASS;
        if(harvest==Harvest.MILK){
            user.playSound(profile.id().equals("goat")?SoundEvents.GOAT_MILK:SoundEvents.COW_MILK,1,1);
            user.setItemInHand(hand,ItemUtils.createFilledResult(tool,user,Items.MILK_BUCKET.getDefaultInstance()));
            return InteractionResult.SUCCESS_SERVER;
        }
        if(harvest==Harvest.IRON_GOLEM_REPAIR){
            if(!repair(target,user,tool))return InteractionResult.PASS;
            target.level().playSound(null,target,SoundEvents.IRON_GOLEM_REPAIR,SoundSource.PLAYERS,1,
                    1+(target.getRandom().nextFloat()-target.getRandom().nextFloat())*.2f);
            return InteractionResult.SUCCESS_SERVER;
        }
        if(harvest==Harvest.DYE){
            var color=tool.get(net.minecraft.core.component.DataComponents.DYE);
            if(!((MorphAccess)target).randommorph$animals().dye(color.getId()))return InteractionResult.PASS;
            tool.shrink(1);
            target.level().playSound(null,target,SoundEvents.DYE_USE,SoundSource.PLAYERS,1,1);manager.syncFlags(target);
            return InteractionResult.SUCCESS_SERVER;
        }
        if(harvest==Harvest.STEW){
            user.setItemInHand(hand,ItemUtils.createFilledResult(tool,user,Items.MUSHROOM_STEW.getDefaultInstance()));
            target.level().playSound(null,target,SoundEvents.MOOSHROOM_MILK,SoundSource.PLAYERS,1,1);
            return InteractionResult.SUCCESS_SERVER;
        }
        if(harvest==Harvest.MOOSHROOM_SHEAR){
            if(!(manager.identity(target) instanceof net.minecraft.world.entity.animal.cow.MushroomCow body))return InteractionResult.PASS;
            // Convert the player, never call native Mob.convertTo (which would spawn a second cow).
            if(!manager.convert(target,MorphRegistry.find("cow")))return InteractionResult.CONSUME;
            try{
                ((ShearingLootAccess)body).randommorph$shearLoot(target.level(),BuiltInLootTables.SHEAR_MOOSHROOM,tool,
                        (level,stack)->target.spawnAtLocation(level,stack,1));
                target.level().playSound(null,target,SoundEvents.MOOSHROOM_SHEAR,SoundSource.PLAYERS,1,1);
                target.gameEvent(net.minecraft.world.level.gameevent.GameEvent.SHEAR,user);
            }finally{tool.hurtAndBreak(1,user,hand.asEquipmentSlot());}
            return InteractionResult.SUCCESS_SERVER;
        }
        if(harvest==Harvest.SNOW_GOLEM_SHEAR){
            if(!(manager.identity(target) instanceof net.minecraft.world.entity.animal.golem.SnowGolem body))return InteractionResult.PASS;
            if(!((MorphAccess)target).randommorph$animals().shearPumpkin())return InteractionResult.PASS;
            try{
                body.setPumpkin(false);
                ((ShearingLootAccess)body).randommorph$shearLoot(target.level(),BuiltInLootTables.SHEAR_SNOW_GOLEM,tool,
                        (level,stack)->target.spawnAtLocation(level,stack,target.getEyeHeight()));
                target.level().playSound(null,target,SoundEvents.SNOW_GOLEM_SHEAR,SoundSource.PLAYERS,1,1);
                target.gameEvent(net.minecraft.world.level.gameevent.GameEvent.SHEAR,user);
            }finally{
                tool.hurtAndBreak(1,user,hand.asEquipmentSlot());manager.syncFlags(target);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        // Acquire the unsheared native loot identity before committing the persistent resource state.
        if(!(manager.identity(target) instanceof Sheep body))return InteractionResult.PASS;
        if(!((MorphAccess)target).randommorph$animals().shear())return InteractionResult.CONSUME;
        try{
            ((ShearingLootAccess)body).randommorph$shearLoot(target.level(),BuiltInLootTables.SHEAR_SHEEP,tool,
                    (level,stack)->target.spawnAtLocation(level,stack,1));
            target.level().playSound(null,target,SoundEvents.SHEEP_SHEAR,SoundSource.PLAYERS,1,1);
            target.gameEvent(net.minecraft.world.level.gameevent.GameEvent.SHEAR,user);
        }finally{
            tool.hurtAndBreak(1,user,hand.asEquipmentSlot());manager.syncFlags(target);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
    /** Native iron golem repair consumes one ingot only if the real owner's health increased. */
    static boolean repair(LivingEntity target,LivingEntity actor,ItemStack ingot){
        if(ingot.isEmpty()||!ingot.is(Items.IRON_INGOT))return false;
        float before=target.getHealth();target.heal(25);
        if(target.getHealth()<=before)return false;
        ingot.consume(1,actor);return true;
    }
    public static boolean graze(ServerPlayer player,AbilityState state){
        var animals=((MorphAccess)player).randommorph$animals();
        if(!animals.sheared()||!player.onGround()||player.isPassenger()||!edible(player,player.blockPosition())){
            player.sendSystemMessage(Component.literal("털이 깎인 양으로 풀 위에 서서 F를 누르세요."),true);return false;
        }
        if(state.grazingTicks>0)return false;
        state.grazingPosition=player.blockPosition().immutable();state.grazingTicks=40;
        RandomMorphMod.manager(player.level().getServer()).syncFlags(player);return true;
    }
    private static boolean edible(ServerPlayer player,BlockPos pos){
        return player.level().getBlockState(pos).is(BlockTags.EDIBLE_FOR_SHEEP)||player.level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK);
    }
    public static void tick(MorphManager manager,ServerPlayer player,MorphProfile profile,AbilityState state){
        var animals=((MorphAccess)player).randommorph$animals();
        if(profile.id().equals("bee")&&animals.stingDeath(player.getRandom()::nextInt)){
            // Match native Bee's generic damage; normal armor, creative and invulnerability still apply.
            player.hurtServer(player.level(),player.damageSources().generic(),player.getHealth());
            return;
        }
        if(profile.id().equals("chicken")&&animals.chickenTick(player.getRandom()::nextInt)&&manager.identity(player) instanceof Chicken body){
            boolean dropped=body.dropFromGiftLootTable(player.level(),BuiltInLootTables.CHICKEN_LAY,
                    (level,stack)->player.spawnAtLocation(level,stack));
            if(dropped){
                player.level().playSound(null,player,SoundEvents.CHICKEN_EGG,SoundSource.PLAYERS,1,
                        1+(player.getRandom().nextFloat()-player.getRandom().nextFloat())*.2f);
                player.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE);
            }
        }
        if(!profile.id().equals("sheep")||state.grazingTicks<=0)return;
        if(!player.onGround()||player.isPassenger()||!player.blockPosition().equals(state.grazingPosition)){
            state.grazingTicks=0;state.grazingPosition=null;manager.syncFlags(player);return;
        }
        if(--state.grazingTicks!=4)return;
        var pos=state.grazingPosition;
        if(!edible(player,pos)){state.grazingTicks=0;state.grazingPosition=null;manager.syncFlags(player);return;}
        boolean destroy=manager.config().blockDestruction&&(!manager.config().respectMobGriefing||player.level().getGameRules().get(GameRules.MOB_GRIEFING));
        if(destroy){
            if(player.level().getBlockState(pos).is(BlockTags.EDIBLE_FOR_SHEEP))player.level().destroyBlock(pos,false);
            else{
                player.level().levelEvent(2001,pos.below(),Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                player.level().setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),2);
            }
        }
        animals.ate();manager.syncFlags(player);
    }
}
