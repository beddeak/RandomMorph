package kr.randommorph.fabric.ability;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
/** 26.3 defers item components until data loading. Run only the real initializers needed by headless fixtures. */
final class NativeItemFixtures {
    public static void bind()throws Exception{
        var bindTags=net.minecraft.core.Holder.Reference.class.getDeclaredMethod("bindTags",Collection.class);bindTags.setAccessible(true);
        bindTags.invoke(Items.AIR.builtInRegistryHolder(),Set.of());
        var items=new ArrayList<>(List.of(Items.BOW,Items.CROSSBOW,Items.ARROW,Items.TIPPED_ARROW,Items.DIAMOND_SWORD,
                Items.BUCKET,Items.MILK_BUCKET,Items.BOWL,Items.MUSHROOM_STEW,Items.SHEARS,Items.BUNDLE,Items.SHULKER_BOX,Items.IRON_INGOT,Items.CARVED_PUMPKIN));
        for(var color:DyeColor.values())items.add(Items.DYE.pick(color));
        var holders=items.stream().map(Item::builtInRegistryHolder).toList();
        var field=DataComponentInitializers.class.getDeclaredField("initializers");field.setAccessible(true);
        var entries=(List<?>)field.get(BuiltInRegistries.DATA_COMPONENT_INITIALIZERS);
        // Iron's original component initializer references the data-driven trim material registry.
        var lookup=new net.minecraft.core.RegistrySetBuilder()
                .add(net.minecraft.core.registries.Registries.TRIM_MATERIAL,net.minecraft.world.item.equipment.trim.TrimMaterials::bootstrap)
                .build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        for(var entry:entries){
            var key=entry.getClass().getDeclaredMethod("key");key.setAccessible(true);
            var matching=holders.stream().filter(holder->{try{return holder.key().equals(key.invoke(entry));}catch(Exception error){throw new AssertionError(error);}}).findFirst();
            if(matching.isEmpty()||matching.get().areComponentsBound())continue;
            var builder=DataComponentMap.builder();var run=entry.getClass().getDeclaredMethod("run",DataComponentMap.Builder.class,net.minecraft.core.HolderLookup.Provider.class);run.setAccessible(true);
            run.invoke(entry,builder,lookup);matching.get().bindComponents(builder.build());
        }
        for(var holder:holders)if(!holder.areComponentsBound())throw new AssertionError("Native fixture components unbound: "+holder.key());
    }
}
