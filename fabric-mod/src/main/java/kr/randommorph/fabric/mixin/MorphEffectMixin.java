package kr.randommorph.fabric.mixin;

import kr.randommorph.fabric.ability.MorphEffectAccess;
import net.minecraft.world.effect.MobEffectInstance;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = MobEffectInstance.class, remap = false)
public abstract class MorphEffectMixin implements MorphEffectAccess {
    @Shadow private MobEffectInstance hiddenEffect;
    @Unique private boolean randommorph$owned;

    public boolean randommorph$owned() { return randommorph$owned; }
    public void randommorph$owned(boolean value) { randommorph$owned = value; }
    public MobEffectInstance randommorph$hidden() { return hiddenEffect; }

    // Both vanilla's copy constructor and hidden-effect promotion use this method.
    @Inject(method = "setDetailsFrom", at = @At("RETURN"))
    private void randommorph$copyOwnership(MobEffectInstance source, CallbackInfo ci) {
        randommorph$owned = ((MorphEffectAccess) source).randommorph$owned();
    }

    // Transfer the active layer only AFTER vanilla has saved the previous layer as hidden.
    // The two writes are the stronger-effect and same-strength duration-extension branches.
    @Inject(method = "update", require = 2, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;duration:I",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void randommorph$takeOwnership(MobEffectInstance source, CallbackInfoReturnable<Boolean> cir) {
        randommorph$owned = ((MorphEffectAccess) source).randommorph$owned();
    }
}
