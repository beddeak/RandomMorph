package kr.randommorph.verify;
import java.util.*;
import kr.randommorph.fabric.mixin.*;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.breeze.Breeze;

/** Force verification and exercise the native static strategy accessor; no entity/world is created. */
public final class NaturalActionProbe {
    public static void run(String side)throws Exception{
        if(!BeeAccess.class.isAssignableFrom(Bee.class)||!SheepEatAccess.class.isAssignableFrom(Sheep.class)||
                !ShearingLootAccess.class.isAssignableFrom(LivingEntity.class))throw new AssertionError("Animal accessors missing");
        var strategy=Breeze.class.getDeclaredField("PROJECTILE_DEFLECTION");strategy.setAccessible(true);
        if(BreezeDeflectionAccess.randommorph$deflection()!=strategy.get(null))throw new AssertionError("Deflection must use the native Breeze strategy");
        Player.class.getDeclaredMethod("randommorph$animals");
        var methods=new ArrayList<java.lang.reflect.Method>();methods.addAll(Arrays.asList(Player.class.getDeclaredMethods()));methods.addAll(Arrays.asList(Entity.class.getDeclaredMethods()));
        for(var hook:List.of("randommorph$breeze","randommorph$harvest","randommorph$oneSting","randommorph$oneStab",
                "randommorph$noBeeSweep","randommorph$stingDamage","randommorph$stingHit","randommorph$saveProduction","randommorph$loadProduction"))
            if(methods.stream().noneMatch(m->m.getName().contains(hook)))throw new AssertionError("Missing action hook: "+hook);
        if(Arrays.stream(Player.class.getDeclaredFields()).noneMatch(f->f.getType()==kr.randommorph.fabric.core.AnimalState.class))throw new AssertionError("Per-player resource field missing");
        System.out.println("RANDOMMORPH_NATURAL_LINKS_OK side="+side+" accessors=4 hooks=9; no entity/player/world interaction was executed");
    }
}
