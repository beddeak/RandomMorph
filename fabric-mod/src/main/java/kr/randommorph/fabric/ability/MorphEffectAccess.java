package kr.randommorph.fabric.ability;

import net.minecraft.world.effect.MobEffectInstance;

/** Local provenance for each active/hidden effect layer; never sent or persisted. */
public interface MorphEffectAccess {
    boolean randommorph$owned();
    void randommorph$owned(boolean value);
    MobEffectInstance randommorph$hidden();
}
