package kr.randommorph.fabric.mixin;
import kr.randommorph.fabric.morph.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Player.class, remap=false)
public abstract class PlayerMorphMixin implements MorphAccess {
    @Unique private MorphProfile randommorph$form;
    @Unique private int randommorph$flags;
    @Unique private final kr.randommorph.fabric.core.AnimalState randommorph$animals=new kr.randommorph.fabric.core.AnimalState();
    public MorphProfile randommorph$form() { return randommorph$form; }
    public void randommorph$form(MorphProfile profile) { randommorph$form = profile; }
    public int randommorph$flags(){return randommorph$flags;}
    public void randommorph$flags(int value){randommorph$flags=value;}
    public kr.randommorph.fabric.core.AnimalState randommorph$animals(){return randommorph$animals;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void randommorph$saveProduction(net.minecraft.world.level.storage.ValueOutput output,CallbackInfo ci){
        output.putBoolean("randommorph_sheared",randommorph$animals.sheared());
        output.putInt("randommorph_egg_ticks",randommorph$animals.eggTicks());
        output.putInt("randommorph_wool_color",randommorph$animals.woolColor());
        output.putBoolean("randommorph_pumpkin_removed",randommorph$animals.pumpkinRemoved());
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void randommorph$loadProduction(net.minecraft.world.level.storage.ValueInput input,CallbackInfo ci){
        randommorph$animals.restoreProduction(input.getBooleanOr("randommorph_sheared",false),input.getIntOr("randommorph_egg_ticks",-1),input.getIntOr("randommorph_wool_color",0),input.getBooleanOr("randommorph_pumpkin_removed",false));
    }
    // Override the default TypedInstance tag lookup while retaining the PLAYER packet/save type.
    // TypedInstance<T>.is(T) is erased to Object in 26.3 bytecode.
    public boolean is(Object type){return randommorph$form==null?((Player)(Object)this).getType()==type:randommorph$form.type()==type;}
    public boolean is(net.minecraft.core.Holder<net.minecraft.world.entity.EntityType<?>> type){return (randommorph$form==null?((Player)(Object)this).typeHolder():randommorph$form.type().builtInRegistryHolder()).is(type);}
    public boolean is(net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>> key){return (randommorph$form==null?((Player)(Object)this).typeHolder():randommorph$form.type().builtInRegistryHolder()).is(key);}
    public boolean is(net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tag){
        return (randommorph$form==null?((Player)(Object)this).typeHolder():randommorph$form.type().builtInRegistryHolder()).is(tag);
    }
    public boolean is(net.minecraft.core.HolderSet<net.minecraft.world.entity.EntityType<?>> types){
        return types.contains(randommorph$form==null?((Player)(Object)this).typeHolder():randommorph$form.type().builtInRegistryHolder());
    }
}
