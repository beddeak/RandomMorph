package kr.randommorph.fabric.ability;
import java.util.List;
import kr.randommorph.fabric.morph.MorphManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.alchemy.*;
/** User-requested uniform vanilla potion pool and native healing drink. */
public final class WitchActions {
    private static final Identifier DRINK_SLOW=Identifier.fromNamespaceAndPath("randommorph","ability/witch_drinking");
    private WitchActions(){}
    public static List<Holder<Potion>> potions(){
        return BuiltInRegistries.POTION.listElements().filter(p->p.key().identifier().getNamespace().equals("minecraft")).map(p->(Holder<Potion>)p).toList();
    }
    public static Holder<Potion> randomPotion(RandomSource random){var pool=potions();if(pool.isEmpty())return Potions.HARMING;return pool.get(random.nextInt(pool.size()));}
    public static boolean drink(MorphManager manager,ServerPlayer player,AbilityState state){
        if(player.getHealth()>=player.getMaxHealth()||state.witchDrinkTicks>0)return false;
        state.witchDrinkTicks=32;
        var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);
        if(speed!=null)speed.addOrUpdateTransientModifier(new AttributeModifier(DRINK_SLOW,-.85,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        manager.syncFlags(player);
        player.level().playSound(null,player.blockPosition(),SoundEvents.WITCH_DRINK,SoundSource.PLAYERS,1,1);
        return true;
    }
    public static void tick(MorphManager manager,ServerPlayer player,AbilityState state){
        if(state.witchDrinkTicks<=0||--state.witchDrinkTicks>0)return;
        clear(player);
        for(var effect:Potions.HEALING.value().getEffects())
            effect.getEffect().value().applyInstantaneousEffect(player.level(),player,player,player,effect.getAmplifier(),1);
        manager.syncFlags(player);
    }
    public static void clear(ServerPlayer player){var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(DRINK_SLOW);}
}
