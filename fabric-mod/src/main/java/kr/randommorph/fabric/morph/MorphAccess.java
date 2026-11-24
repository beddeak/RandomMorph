package kr.randommorph.fabric.morph;
/** Form/flags are transient; only wool and egg resource progress is saved in player NBT. */
public interface MorphAccess {
    MorphProfile randommorph$form();
    void randommorph$form(MorphProfile profile);
    int randommorph$flags();
    void randommorph$flags(int flags);
    kr.randommorph.fabric.core.AnimalState randommorph$animals();
}
