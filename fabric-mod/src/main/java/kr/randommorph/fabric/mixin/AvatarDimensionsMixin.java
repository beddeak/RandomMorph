package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Avatar.class, remap=false)
public abstract class AvatarDimensionsMixin {
    @Inject(method="getDefaultDimensions", at=@At("HEAD"), cancellable=true)
    private void randommorph$body(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ((Object)this instanceof MorphAccess access && access.randommorph$form() != null &&
            pose != Pose.SLEEPING && pose != Pose.DYING) {
            var dimensions = access.randommorph$form().dimensions(access.randommorph$flags());
            cir.setReturnValue(pose == Pose.CROUCHING && !access.randommorph$form().id().equals("shulker") ? dimensions.scale(1, 0.85f) : dimensions);
        }
    }
}
