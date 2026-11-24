package kr.randommorph.fabric.morph;
import net.minecraft.world.entity.ai.attributes.*;
/** Converts vanilla Mob control input into unit player input, rather than sharing one swimming gain. */
public final class NativeMovement {
    private NativeMovement(){}
    public static double rawSpeed(MorphProfile p){
        if(p.cube())return cubeSpeed(2);
        var defaults=DefaultAttributes.getSupplier(p.type());
        return defaults!=null&&defaults.hasAttribute(Attributes.MOVEMENT_SPEED)?defaults.getBaseValue(Attributes.MOVEMENT_SPEED):.1;
    }
    /** AbstractCubeMob.setSize performs this arithmetic in float before storing a double. */
    public static double cubeSpeed(int size){return .2f+.1f*size;}
    public static double landSpeed(String id,double nativeSpeed){
        // MoveControl.setSpeed also sets Mob.zza to that speed; a player's forward input is 1.
        double control=switch(id){
            case "wandering_trader" -> nativeSpeed*.35; // Native stroll goal, not raw attribute input.
            case "villager" -> nativeSpeed*.5; // Normal Brain walking gain, not panic speed.
            case "frog" -> nativeSpeed*.1; // SmoothSwimmingMoveControl outsideWaterSpeedModifier.
            case "shulker" -> 0; // Attached; only teleportation moves the avatar.
            case "turtle" -> .875*.06+.125*nativeSpeed; // TurtleMoveControl ground floor/lerp.
            case "cod","salmon","pufferfish","tropical_fish","tadpole","squid","glow_squid" -> .05;
            case "axolotl" -> nativeSpeed*.15; // Requested slow land crawling.
            case "dolphin" -> nativeSpeed*.1;
            case "nautilus","zombie_nautilus" -> 0;
            default -> nativeSpeed;
        };
        return control*control*movementScale(id);
    }
    public static float swimAcceleration(MorphProfile p){
        double n=rawSpeed(p);
        return (float)(movementScale(p.id())*switch(p.id()){
            case "cod","salmon","pufferfish","tropical_fish","tadpole" -> .01*n;
            case "dolphin" -> .02*n*n;
            case "axolotl" -> .02*n*n;
            case "nautilus","zombie_nautilus" -> .011*n*n;
            case "turtle","guardian","elder_guardian" -> .1*n;
            case "squid","glow_squid" -> .02;
            default -> .1*n;
        });
    }
    public static float flightSpeed(MorphProfile p){
        var attributes=DefaultAttributes.getSupplier(p.type());
        double speed=attributes!=null&&attributes.hasAttribute(Attributes.FLYING_SPEED)?attributes.getBaseValue(Attributes.FLYING_SPEED):rawSpeed(p);
        return (float)(speed*.1*movementScale(p.id()));
    }
    // Requested balance adjustment applies after native input conversion, rather than squaring it.
    private static double movementScale(String id){return id.equals("vex")?.6:id.equals("axolotl")?.6:1;}
}
