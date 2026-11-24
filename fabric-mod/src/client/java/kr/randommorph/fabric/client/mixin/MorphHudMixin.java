package kr.randommorph.fabric.client.mixin;
import kr.randommorph.fabric.ability.AbilityHooks;
import kr.randommorph.fabric.client.ClientSenses;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Reuses native hearts and native air sprites/layout without adding a custom health bar. */
@Mixin(value=Hud.class,remap=false)
public abstract class MorphHudMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void randommorph$blindWorld(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){ClientSenses.draw(g);}
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void randommorph$keepBlindWithF1(GuiGraphicsExtractor g,DeltaTracker delta,CallbackInfo ci){
        var mc=net.minecraft.client.Minecraft.getInstance();var p=mc.player;
        if(p!=null&&p.isAlive()&&!p.isSpectator()&&AbilityHooks.profile(p)!=null&&AbilityHooks.profile(p).id().equals("warden"))mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden=false;
    }
    @Redirect(method="extractAirBubbles",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean randommorph$gillBubbles(Player p,TagKey<Fluid> fluid){var form=AbilityHooks.profile(p);return kr.randommorph.fabric.ability.WaterPhysiology.showAir(form==null?"":form.id(),AbilityHooks.needsWater(p),p.isInWaterOrRain(),p.isEyeInFluid(fluid));}
}
