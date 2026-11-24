package kr.randommorph.fabric.mixin;
import java.util.Map;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Brain.class,remap=false)
public interface BrainSensorsAccess {
    @Accessor("sensors") Map<SensorType<?>,Sensor<?>> randommorph$sensors();
}
