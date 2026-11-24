package kr.randommorph.fabric.mixin;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=LivingEntity.class,remap=false)
public interface ShearingLootAccess {
    @Invoker("dropFromShearingLootTable") void randommorph$shearLoot(ServerLevel level,ResourceKey<LootTable> table,
            ItemInstance tool,BiConsumer<ServerLevel,ItemStack> drop);
}
