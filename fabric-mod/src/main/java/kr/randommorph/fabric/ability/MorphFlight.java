package kr.randommorph.fabric.ability;

import net.minecraft.world.entity.player.Abilities;

/** Reapply the current form after vanilla changes game-mode abilities, retaining the player's landing choice. */
final class MorphFlight {
    private MorphFlight(){}
    static boolean update(Abilities abilities,boolean spectator,float speed,boolean takeOff){
        if(spectator)return false;
        boolean changed=!abilities.mayfly||Float.compare(abilities.getFlyingSpeed(),speed)!=0||takeOff&&!abilities.flying;
        abilities.mayfly=true;
        abilities.setFlyingSpeed(speed);
        if(takeOff)abilities.flying=true;
        return changed;
    }
}
