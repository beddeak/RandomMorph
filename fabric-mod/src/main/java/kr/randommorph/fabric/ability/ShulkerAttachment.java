package kr.randommorph.fabric.ability;

import kr.randommorph.fabric.mixin.ShulkerAccess;
import kr.randommorph.fabric.morph.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** The avatar occupies the native shulker cell. Only teleportation changes its attachment. */
public final class ShulkerAttachment {
    public record Placement(BlockPos cell,Direction face){
        public Placement{cell=cell.immutable();}
        public Vec3 position(){return Vec3.atBottomCenterOf(cell);}
    }
    private ShulkerAttachment(){}
    public static boolean shulker(net.minecraft.world.entity.Entity entity){var profile=AbilityHooks.profile(entity);return profile!=null&&profile.id().equals("shulker");}
    public static boolean inTeleportRange(BlockPos from,BlockPos to){
        return Math.abs((long)from.getX()-to.getX())<=8&&Math.abs((long)from.getY()-to.getY())<=8&&Math.abs((long)from.getZ()-to.getZ())<=8;
    }
    private static boolean available(ServerPlayer player,BlockPos cell,Direction face){
        var level=player.level();
        var box=Shulker.getProgressAabb(player.getScale(),face.getOpposite(),1,Vec3.atBottomCenterOf(cell)).deflate(1.0E-6);
        if(box.minY<level.getMinY()||box.maxY>level.getMaxY()+1||!level.getWorldBorder().isWithinBounds(box))return false;
        // Check the whole opening envelope and support before native block/collision queries; never load a chunk.
        for(int x=(net.minecraft.util.Mth.floor(box.minX)-1)>>4;x<=(net.minecraft.util.Mth.floor(box.maxX)+1)>>4;x++)
            for(int z=(net.minecraft.util.Mth.floor(box.minZ)-1)>>4;z<=(net.minecraft.util.Mth.floor(box.maxZ)+1)>>4;z++)if(!level.hasChunk(x,z))return false;
        return level.hasChunkAt(cell.relative(face));
    }
    public static Placement find(ServerPlayer player,Shulker body,Vec3 position,Direction preferred,boolean teleport){
        var cell=BlockPos.containing(position);
        var scale=body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);if(scale!=null)scale.setBaseValue(player.getScale());
        if(valid(player,body,cell,preferred,teleport))return new Placement(cell,preferred);
        for(var face:Direction.values())if(face!=preferred&&valid(player,body,cell,face,teleport))return new Placement(cell,face);
        return null;
    }
    public static boolean valid(ServerPlayer player,Shulker body,BlockPos cell,Direction face,boolean teleport){
        return available(player,cell,face)&&(!teleport||!player.level().getBlockState(cell.relative(face)).is(BlockTags.SHULKER_DOES_NOT_TELEPORT_TO))
                &&((ShulkerAccess)body).randommorph$canStayAt(cell,face);
    }
    public static void attach(MorphManager manager,ServerPlayer player,AbilityState state,Placement placement,boolean teleport){
        var previous=player.blockPosition();state.shulkerCell=placement.cell();state.shulkerFace=placement.face();state.shulkerOpenTicks=0;
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        var pos=placement.position();if(player.position().distanceToSqr(pos)>1.0E-10)player.teleportTo(pos.x,pos.y,pos.z);
        AbilityEngine.shell(player,state,true);manager.syncFlags(player);
        if(teleport){
            player.level().playSound(null,previous,SoundEvents.SHULKER_TELEPORT,SoundSource.PLAYERS,1,1);
            player.level().gameEvent(GameEvent.TELEPORT,previous,GameEvent.Context.of(player));
            player.level().levelEvent(2016,previous,net.minecraft.util.BlockUtil.packDifferenceInPosition(previous,placement.cell(),8,8,8));
        }
    }
    public static boolean teleport(MorphManager manager,ServerPlayer player,AbilityState state){
        if(!(manager.identity(player) instanceof Shulker body))return false;
        var origin=player.blockPosition();
        var hit=player.level().clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(8)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(hit.getType()==HitResult.Type.BLOCK){
            var cell=hit.getBlockPos().relative(hit.getDirection());var face=hit.getDirection().getOpposite();
            if(!cell.equals(origin)&&inTeleportRange(origin,cell)&&valid(player,body,cell,face,true)){
                attach(manager,player,state,new Placement(cell,face),true);return true;
            }
        }
        return randomTeleport(manager,player,state,body);
    }
    public static boolean randomTeleport(MorphManager manager,ServerPlayer player,AbilityState state,Shulker body){
        var origin=player.blockPosition();
        // The native teleport rolls five candidates, each coordinate independently in [-8,8].
        for(int i=0;i<5;i++){
            var cell=origin.offset(player.getRandom().nextInt(17)-8,player.getRandom().nextInt(17)-8,player.getRandom().nextInt(17)-8);
            if(cell.equals(origin))continue;
            var next=find(player,body,Vec3.atBottomCenterOf(cell),Direction.DOWN,true);
            if(next!=null){attach(manager,player,state,next,true);return true;}
        }
        return false;
    }
    public static void tick(MorphManager manager,ServerPlayer player,AbilityState state){
        if(player.isSpectator())return;
        if(player.isPassenger())player.unRide();
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        if(!(manager.identity(player) instanceof Shulker body))return;
        if(state.shulkerEscapePending){state.shulkerEscapePending=false;randomTeleport(manager,player,state,body);}
        var current=player.blockPosition();
        if(state.shulkerCell==null||!state.shulkerCell.equals(current)||!valid(player,body,current,state.shulkerFace,false)){
            var replacement=find(player,body,player.position(),state.shulkerFace,false);
            if(replacement!=null)attach(manager,player,state,replacement,false);
            else if(!randomTeleport(manager,player,state,body)){
                // Prefer adjacent native surfaces after support is broken before suspending in a sealed/empty space.
                for(var side:Direction.values()){
                    replacement=find(player,body,Vec3.atBottomCenterOf(current.relative(side)),side.getOpposite(),true);
                    if(replacement!=null){attach(manager,player,state,replacement,true);break;}
                }
                if(replacement==null){
                    if(manager.suspendSafely(player))player.sendSystemMessage(net.minecraft.network.chat.Component.literal("붙을 블록이 없어 안전한 위치에서 사람 몸으로 대기합니다. 변신 타이머는 계속 진행됩니다."),true);
                    else if(player.tickCount%20==0)player.sendSystemMessage(net.minecraft.network.chat.Component.literal("붙을 면과 복구 공간이 없습니다. 셜커 몸을 유지하며 다시 찾습니다. Shift+F로 이동을 시도하세요."),true);
                    return;
                }
            }
        }
        var anchor=Vec3.atBottomCenterOf(state.shulkerCell);
        if(player.position().distanceToSqr(anchor)>1.0E-10)player.teleportTo(anchor.x,anchor.y,anchor.z);
        if(state.shulkerOpenTicks>0&&--state.shulkerOpenTicks==0){AbilityEngine.shell(player,state,true);manager.syncFlags(player);}
    }
    public static void open(MorphManager manager,ServerPlayer player,AbilityState state){
        state.shulkerOpenTicks=60;AbilityEngine.shell(player,state,false);manager.syncFlags(player);
    }
    /** Native Shulker rolls 1/4 only after accepted damage and below (not at) half health. */
    public static boolean escapeRoll(float health,float maximum,int roll){return health>0&&health<maximum*.5f&&roll==0;}
    public static void damaged(ServerPlayer player){
        if(!shulker(player)||!player.isAlive())return;
        var manager=kr.randommorph.fabric.RandomMorphMod.manager(player.level().getServer());var state=manager.abilityState(player);
        if(state!=null&&player.getHealth()<player.getMaxHealth()*.5f&&escapeRoll(player.getHealth(),player.getMaxHealth(),player.getRandom().nextInt(4)))state.shulkerEscapePending=true;
    }
}
