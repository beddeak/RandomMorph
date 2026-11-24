package kr.randommorph.fabric.client;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Preserve vanilla camera exclusions before swapping a world-render input. */
public final class WorldMorphRender {
    private WorldMorphRender() {}
    public static Entity select(Entity entity,Entity cameraEntity,boolean detached,Function<Player,Entity> prepare) {
        if(!(entity instanceof Player player))return entity;
        // Vanilla compares by reference, so a detached body must not bypass these exclusions.
        if(entity==cameraEntity&&!detached&&!player.isSleeping())return entity;
        if(entity instanceof LocalPlayer&&entity!=cameraEntity)return entity;
        return Objects.requireNonNull(prepare.apply(player),"Missing world render input");
    }
}
