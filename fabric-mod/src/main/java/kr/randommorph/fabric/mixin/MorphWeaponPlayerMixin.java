package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.MorphWeapons;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Player.class,remap=false)
public abstract class MorphWeaponPlayerMixin {
    @Inject(method="getProjectile",at=@At("HEAD"),cancellable=true)
    private void randommorph$virtualAmmo(ItemStack weapon,CallbackInfoReturnable<ItemStack> cir){
        if(MorphWeapons.borrowed(weapon)){
            var player=(Player)(Object)this;
            cir.setReturnValue(MorphWeapons.canUse(player,weapon)?MorphWeapons.ammunition(((MorphAccess)player).randommorph$form()):ItemStack.EMPTY);
        }
    }
    @Inject(method="dropEquipment",at=@At("HEAD"))
    private void randommorph$noBorrowedDeathLoot(ServerLevel level,CallbackInfo ci){MorphWeapons.clear((Player)(Object)this);}
}
