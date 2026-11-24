package kr.randommorph.fabric.ability;

import net.minecraft.world.effect.MobEffectInstance;

public final class MorphEffects {
    private MorphEffects() {}

    public static void own(MobEffectInstance effect) {
        ((MorphEffectAccess) effect).randommorph$owned(true);
    }

    /** A replacement player needs independent effect objects, including the external hidden chain. */
    public static MobEffectInstance copyExternal(MobEffectInstance source) {
        var retained = externalRemainder(source);
        if (retained == null) throw new IllegalArgumentException("Owned effect reached respawn copying");
        return copyLayer(retained);
    }

    private static MobEffectInstance copyLayer(MobEffectInstance source) {
        if (source == null) return null;
        var result = new MobEffectInstance(source.getEffect(), source.getDuration(), source.getAmplifier(),
                source.isAmbient(), source.isVisible(), source.showIcon(),
                copyLayer(((MorphEffectAccess) source).randommorph$hidden()));
        result.copyBlendState(source);
        return result;
    }

    /** Keep external layers and their already-ticked durations, promoting a hidden one if needed. */
    public static MobEffectInstance externalRemainder(MobEffectInstance effect) {
        if (effect == null) return null;
        var access = (MorphEffectAccess) effect;
        var hidden = access.randommorph$hidden();
        var retained = externalRemainder(hidden);
        if (access.randommorph$owned() || !effect.isInfiniteDuration() && effect.getDuration() <= 0)
            return retained;
        if (retained == hidden) return effect;
        var result = new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(),
                effect.isAmbient(), effect.isVisible(), effect.showIcon(), retained);
        result.copyBlendState(effect);
        return result;
    }
}
