package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Guardian.class,remap=false)
public interface GuardianAnimationAccess {
    @Accessor("clientSideTailAnimation") void randommorph$tail(float value);
    @Accessor("clientSideTailAnimationO") void randommorph$tailOld(float value);
    @Accessor("clientSideSpikesAnimation") void randommorph$spikes(float value);
    @Accessor("clientSideSpikesAnimationO") void randommorph$spikesOld(float value);
}
