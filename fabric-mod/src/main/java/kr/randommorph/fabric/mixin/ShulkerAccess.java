package kr.randommorph.fabric.mixin;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(value=Shulker.class,remap=false)
public interface ShulkerAccess {
    // Native setRawPeekAmount broadcasts sound + gameEvent even for an unchanged value.
    // A detached identity must copy data without those world side effects.
    @Accessor("DATA_PEEK_ID") static net.minecraft.network.syncher.EntityDataAccessor<Byte> randommorph$peekData(){throw new AssertionError();}
    @Invoker("setAttachFace") void randommorph$face(Direction face);
    @Invoker("canStayAt") boolean randommorph$canStayAt(BlockPos cell,Direction face);
    @Accessor("currentPeekAmount") float randommorph$peekCurrent();
    @Accessor("currentPeekAmount") void randommorph$peekCurrent(float value);
    @Accessor("currentPeekAmountO") void randommorph$peekPrevious(float value);
}
