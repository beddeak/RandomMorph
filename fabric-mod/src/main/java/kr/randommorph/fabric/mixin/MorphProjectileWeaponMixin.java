package kr.randommorph.fabric.mixin;
import java.util.List;
import kr.randommorph.fabric.ability.MorphWeapons;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=ProjectileWeaponItem.class,remap=false)
public abstract class MorphProjectileWeaponMixin {
    @Inject(method="createProjectile",at=@At("RETURN"))
    private void randommorph$unrecoverableArrow(Level level,LivingEntity owner,ItemStack weapon,ItemStack ammo,boolean critical,CallbackInfoReturnable<Projectile> cir){MorphWeapons.projectile(owner,weapon,cir.getReturnValue());}
    @Inject(method="shoot",at=@At("HEAD"),cancellable=true)
    private void randommorph$weaponShotGuard(ServerLevel level,LivingEntity owner,InteractionHand hand,ItemStack weapon,List<ItemStack> ammo,float speed,float spread,boolean critical,LivingEntity target,CallbackInfo ci){
        if(MorphWeapons.borrowed(weapon)&&(!(owner instanceof Player player)||!MorphWeapons.canUse(player,weapon)))ci.cancel();
    }
    @Inject(method="shoot",at=@At("RETURN"))
    private void randommorph$weaponCooldown(ServerLevel level,LivingEntity owner,InteractionHand hand,ItemStack weapon,List<ItemStack> ammo,float speed,float spread,boolean critical,LivingEntity target,CallbackInfo ci){
        if(!ammo.isEmpty()&&owner instanceof ServerPlayer player&&MorphWeapons.borrowed(weapon))MorphWeapons.fired(player,weapon);
    }
}
