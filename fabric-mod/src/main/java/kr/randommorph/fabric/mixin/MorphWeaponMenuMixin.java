package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.ability.MorphWeapons;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AbstractContainerMenu.class,remap=false)
public abstract class MorphWeaponMenuMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void randommorph$boundEquipment(int slot,int button,ContainerInput input,Player player,CallbackInfo ci){
        var menu=(AbstractContainerMenu)(Object)this;
        if(MorphWeapons.blockClick(menu,slot,button,input,player)){if(player instanceof net.minecraft.server.level.ServerPlayer)menu.broadcastFullState();ci.cancel();}
    }
}
