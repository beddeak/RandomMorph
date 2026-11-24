package kr.randommorph.fabric.mixin;

import kr.randommorph.fabric.RandomMorphMod;
import kr.randommorph.fabric.ability.MorphEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = ServerPlayer.class, remap = false)
public abstract class PlayerLifecycleMixin {
    @Inject(method="restoreFrom",at=@At("TAIL"))
    private void randommorph$animalResources(ServerPlayer oldPlayer,boolean alive,CallbackInfo ci){
        ((kr.randommorph.fabric.morph.MorphAccess)this).randommorph$animals().copyFrom(
                ((kr.randommorph.fabric.morph.MorphAccess)oldPlayer).randommorph$animals(),alive);
    }
    @Inject(method = "showEndCredits", at = @At("HEAD"))
    private void randommorph$credits(CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        RandomMorphMod.manager(player.level().getServer()).suspend(player);
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void randommorph$leaveLevel(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> ci) {
        var player = (ServerPlayer) (Object) this;
        if (!player.isRemoved() && transition.newLevel() != player.level())
            RandomMorphMod.manager(player.level().getServer()).suspend(player);
    }

    // Also covers direct restoreFrom calls: remove owned state before native health/effect copying.
    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void randommorph$beforeCopy(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        RandomMorphMod.manager(oldPlayer.level().getServer()).suspend(oldPlayer);
    }

    @Redirect(method = "restoreFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getActiveEffects()Ljava/util/Collection;"))
    private java.util.Collection<MobEffectInstance> randommorph$copyCandidates(ServerPlayer oldPlayer) {
        var effects = oldPlayer.getActiveEffects();
        if (RandomMorphMod.manager(oldPlayer.level().getServer()).session(oldPlayer) == null) return effects;
        // A failed resource callback must not copy an owned effect onto the new player.
        return effects.stream().map(MorphEffects::externalRemainder).filter(java.util.Objects::nonNull).toList();
    }

    @Redirect(method = "restoreFrom", at = @At(value = "NEW", target = "net/minecraft/world/effect/MobEffectInstance"))
    private MobEffectInstance randommorph$externalLayers(MobEffectInstance source, ServerPlayer oldPlayer, boolean alive) {
        // Vanilla's copy constructor drops hidden layers. Preserve them for a participating player.
        return RandomMorphMod.manager(oldPlayer.level().getServer()).session(oldPlayer) == null
                ? new MobEffectInstance(source) : MorphEffects.copyExternal(source);
    }
}
