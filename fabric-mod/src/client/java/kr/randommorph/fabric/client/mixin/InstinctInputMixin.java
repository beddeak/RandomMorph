package kr.randommorph.fabric.client.mixin;
import kr.randommorph.fabric.client.ClientInstincts;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LocalPlayer.class,remap=false)
public abstract class InstinctInputMixin {
    @Inject(method="applyInput",at=@At("TAIL"))
    private void randommorph$speciesInput(CallbackInfo ci){ClientInstincts.apply((LocalPlayer)(Object)this);}
}
