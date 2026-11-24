package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.AbilityHooks;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
public abstract class CreakingMovementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleMovePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
    private void randommorph$watchedMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci){
        if(!packet.hasPosition()||!player.isAlive()||player.isSpectator()||!AbilityHooks.immobile(player))return;
        double dx=packet.getX(player.getX())-player.getX(),dz=packet.getZ(player.getZ())-player.getZ(),dy=packet.getY(player.getY())-player.getY();
        if(dx*dx+dz*dz>1.0E-8||dy>1.0E-5||kr.randommorph.fabric.ability.ShulkerAttachment.shulker(player)&&Math.abs(dy)>1.0E-5){
            if(packet.hasRotation()){player.setYRot(packet.getYRot(player.getYRot()));player.setXRot(packet.getXRot(player.getXRot()));}
            player.teleportTo(player.getX(),player.getY(),player.getZ());ci.cancel();
        }
    }
}
