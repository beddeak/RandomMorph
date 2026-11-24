package kr.randommorph.verify;

import java.util.List;
import kr.randommorph.fabric.ability.*;
import net.minecraft.world.effect.*;

/** Behavioral regressions on actual Mixin-transformed effects; no entity/world is constructed. */
public final class EffectOwnershipProbe {
    private EffectOwnershipProbe() {}
    private static MobEffectInstance effect(int duration, int amplifier) {
        return new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier);
    }
    private static MobEffectInstance owned(int duration, int amplifier) {
        var effect = effect(duration, amplifier);
        MorphEffects.own(effect);
        return effect;
    }
    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void ticks(MobEffectInstance effect, int ticks) {
        for (int i = 0; i < ticks; i++) effect.tickClient();
    }
    private static void values(MobEffectInstance effect, int duration, int amplifier) {
        expect(effect != null && effect.getDuration() == duration && effect.getAmplifier() == amplifier,
                "Wrong retained effect: " + effect + "; expected " + amplifier + "/" + duration);
        expect(!((MorphEffectAccess) effect).randommorph$owned(), "Owned layer was retained");
    }
    public static void run(String side) {
        var cases = List.<Runnable>of(
            () -> expect(MorphEffects.externalRemainder(owned(200, 0)) == null, "Owned effect survived"),
            () -> {
                var external = effect(200, 0);
                expect(MorphEffects.externalRemainder(external) == external, "External instance changed");
            },
            () -> {
                var current = owned(200, 0);
                expect(current.update(effect(400, 0)), "Expected duration extension");
                ticks(current, 10);
                expect(MorphEffects.externalRemainder(current) == current, "Extension was removed");
                values(current, 390, 0);
            },
            () -> {
                var current = owned(200, 0);
                current.update(effect(40, 1));
                ticks(current, 10);
                var retained = MorphEffects.externalRemainder(current);
                values(retained, 30, 1);
                expect(((MorphEffectAccess) retained).randommorph$hidden() == null, "Owned hidden tail survived");
            },
            () -> {
                var current = owned(200, 2);
                expect(!current.update(effect(400, 0)), "Hidden-only merge should return false in 26.3");
                ticks(current, 10);
                values(MorphEffects.externalRemainder(current), 390, 0);
            },
            () -> {
                var current = owned(200, 2);
                current.update(effect(400, 0));
                expect(!current.update(effect(500, 0)), "Hidden-only refresh should return false");
                ticks(current, 10);
                values(MorphEffects.externalRemainder(current), 490, 0);
            },
            () -> {
                var current = owned(200, 2);
                expect(!current.update(effect(100, 0)), "Weaker shorter effect should be rejected");
                expect(MorphEffects.externalRemainder(current) == null, "Rejected update acquired ownership");
            },
            () -> {
                var current = owned(200, 2);
                current.update(effect(400, 0));
                ticks(current, 200);
                expect(MorphEffects.externalRemainder(current) == current, "Promoted external effect was removed");
                values(current, 200, 0);
            },
            () -> {
                var current = owned(200, 0);
                current.update(effect(40, 1));
                ticks(current, 40);
                expect(current.getAmplifier() == 0 && current.getDuration() == 160, "Native downgrade changed");
                expect(MorphEffects.externalRemainder(current) == null, "Promoted owned effect survived");
            },
            () -> {
                var current = owned(200, 1);
                current.update(effect(40, 2));
                current.update(effect(600, 0));
                ticks(current, 20);
                var retained = MorphEffects.externalRemainder(current);
                values(retained, 20, 2);
                values(((MorphEffectAccess) retained).randommorph$hidden(), 580, 0);
                expect(((MorphEffectAccess) ((MorphEffectAccess) retained).randommorph$hidden()).randommorph$hidden() == null,
                        "Owned middle layer survived");
            },
            () -> {
                var current = owned(200, 0);
                current.update(effect(MobEffectInstance.INFINITE_DURATION, 0));
                ticks(current, 300);
                values(MorphEffects.externalRemainder(current), MobEffectInstance.INFINITE_DURATION, 0);
            },
            () -> {
                var current = owned(200, 0);
                var copy = new MobEffectInstance(current);
                expect(((MorphEffectAccess) copy).randommorph$owned(), "Native copy lost ownership");
                current.update(new MobEffectInstance(MobEffects.REGENERATION, 400, 0, true, false, false));
                var retained = MorphEffects.externalRemainder(current);
                expect(retained.isAmbient() && !retained.isVisible() && !retained.showIcon(), "External flags changed");
            },
            () -> {
                var original=effect(200,0);original.update(effect(40,1));
                var copy=MorphEffects.copyExternal(original);
                expect(copy!=original,"Respawn reused active effect");
                expect(((MorphEffectAccess)copy).randommorph$hidden()!=((MorphEffectAccess)original).randommorph$hidden(),"Respawn reused hidden effect");
                ticks(copy,40);values(copy,160,0);values(original,40,1);
            },
            () -> {
                var original=owned(200,1);original.update(effect(40,2));original.update(effect(600,0));
                ticks(original,20);var copy=MorphEffects.copyExternal(original);
                values(copy,20,2);values(((MorphEffectAccess)copy).randommorph$hidden(),580,0);
                ticks(copy,20);values(copy,560,0);
                expect(((MorphEffectAccess)original).randommorph$hidden()!=null,"Copy modified old player's chain");
            },
            () -> {
                var original=new MobEffectInstance(MobEffects.REGENERATION,MobEffectInstance.INFINITE_DURATION,0,true,false,false);
                var copy=MorphEffects.copyExternal(original);ticks(copy,300);
                values(copy,MobEffectInstance.INFINITE_DURATION,0);
                expect(copy.isAmbient()&&!copy.isVisible()&&!copy.showIcon(),"Respawn lost effect presentation");
            }
        );
        for (var test : cases) test.run();
        System.out.println("RANDOMMORPH_EFFECTS_OK side=" + side + " cases=" + cases.size() + "; no world/server was started");
    }
}
