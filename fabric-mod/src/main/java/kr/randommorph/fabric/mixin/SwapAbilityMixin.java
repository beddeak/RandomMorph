package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.ability.AbilityEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
public abstract class SwapAbilityMixin {
    @Shadow public ServerPlayer player;
    // AFTER thread enforcement: no Minecraft mutation is performed on the Netty thread.
    @Inject(method="handlePlayerAction",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
    private void randommorph$swap(ServerboundPlayerActionPacket packet,CallbackInfo ci){
        if(packet.getAction()==ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND&&AbilityEngine.use(RandomMorphMod.manager(player.level().getServer()),player,player.isShiftKeyDown()))ci.cancel();
    }
}
