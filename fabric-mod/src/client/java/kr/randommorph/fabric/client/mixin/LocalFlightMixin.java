package kr.randommorph.fabric.client.mixin;
import kr.randommorph.fabric.ability.AbilityHooks;
import kr.randommorph.fabric.ability.Trait;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LocalPlayer.class,remap=false)
public abstract class LocalFlightMixin {
    @Inject(method="aiStep",at=@At("TAIL"))
    private void randommorph$keepFlying(CallbackInfo ci){
        var player=(LocalPlayer)(Object)this;
        if(player.isAlive()&&!player.isSpectator()&&!player.isPassenger()&&AbilityHooks.has(player,Trait.FLIGHT)&&player.getAbilities().mayfly&&!player.getAbilities().flying){player.getAbilities().flying=true;player.onUpdateAbilities();}
    }
}
