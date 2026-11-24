package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.MorphWeapons;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=CrossbowItem.class,remap=false)
public abstract class MorphCrossbowMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void randommorph$crossbowUse(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
        var weapon=player.getItemInHand(hand);if(MorphWeapons.borrowed(weapon)&&!MorphWeapons.canUse(player,weapon))cir.setReturnValue(InteractionResult.FAIL);
    }
    @Inject(method="performShooting",at=@At("HEAD"),cancellable=true)
    private void randommorph$chargedFormGuard(Level level,LivingEntity owner,InteractionHand hand,ItemStack weapon,float speed,float spread,LivingEntity target,CallbackInfo ci){
        if(MorphWeapons.borrowed(weapon)&&(!(owner instanceof Player player)||!MorphWeapons.canUse(player,weapon)))ci.cancel();
    }
}
