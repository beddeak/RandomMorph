package kr.randommorph.fabric.ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
/** Native 50-block survival/team/effect threshold; the morphed owner does not debuff itself. */
public final class ElderAura {
    private ElderAura(){}
    public static boolean refresh(MobEffectInstance current){return current==null||current.getAmplifier()<2||current.endsWithin(1199);}
    public static void tick(ServerPlayer owner,AbilityState state){
        if(state.elderAuraTicks>0){state.elderAuraTicks--;return;}
        state.elderAuraTicks=1199;
        for(var target:owner.level().players()){
            if(target==owner||!target.gameMode.isSurvival()||owner.isAlliedTo(target)||!owner.position().closerThan(target.position(),50))continue;
            if(refresh(target.getEffect(MobEffects.MINING_FATIGUE))&&target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,6000,2),owner))
                target.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT,1));
        }
    }
}
