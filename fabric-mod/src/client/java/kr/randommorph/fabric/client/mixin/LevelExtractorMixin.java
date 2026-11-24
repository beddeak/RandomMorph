package kr.randommorph.fabric.client.mixin;
import kr.randommorph.fabric.client.ClientMorphs;
import kr.randommorph.fabric.client.WorldMorphRender;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=LevelExtractor.class,remap=false)
public abstract class LevelExtractorMixin {
    // The entire visible-entity iteration must see a matching entity and render state.
    // Changing only extractEntity's result makes Lunar pass a RemotePlayer to MobRenderer.
    // This is a local render value, never a replacement in ClientLevel or entity picking.
    @ModifyExpressionValue(method="extractVisibleEntities",at=@At(value="INVOKE",target="Ljava/util/Iterator;next()Ljava/lang/Object;"))
    private Object randommorph$model(Object value,@Local(argsOnly=true) Camera camera) {
        return value instanceof Entity entity
                ? WorldMorphRender.select(entity,camera.entity(),camera.isDetached(),ClientMorphs::prepare)
                : value;
    }
}
