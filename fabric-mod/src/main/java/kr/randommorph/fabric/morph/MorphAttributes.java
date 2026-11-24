package kr.randommorph.fabric.morph;
import java.util.List;
import kr.randommorph.fabric.core.MorphSession;
import kr.randommorph.fabric.ability.Trait;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
public final class MorphAttributes {
    private static final List<Holder<Attribute>> TRACKED=List.of(Attributes.MAX_HEALTH,Attributes.MOVEMENT_SPEED,
        Attributes.ATTACK_DAMAGE,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.KNOCKBACK_RESISTANCE,
        Attributes.JUMP_STRENGTH,Attributes.STEP_HEIGHT,Attributes.SAFE_FALL_DISTANCE,Attributes.CAMERA_DISTANCE);
    private MorphAttributes(){}
    private static Identifier id(Holder<Attribute> attribute){return Identifier.fromNamespaceAndPath("randommorph","body/"+attribute.unwrapKey().orElseThrow().identifier().getPath());}
    public static void apply(Player player,MorphProfile profile,LivingEntity nativeBody){
        float ratio=MorphSession.healthRatio(player.getHealth(),player.getMaxHealth());clearModifiers(player);
        if(profile!=null){
            if(nativeBody==null)throw new IllegalArgumentException("Native body required");
            for(var attribute:TRACKED){
                var target=player.getAttribute(attribute);var source=nativeBody.getAttribute(attribute);
                if(target==null||source==null&&!(attribute==Attributes.JUMP_STRENGTH&&profile.cube()))continue;
                double value=source==null?Attributes.JUMP_STRENGTH.value().getDefaultValue():source.getBaseValue();
                if(attribute==Attributes.MOVEMENT_SPEED)value=movement(player,profile,value);
                if(attribute==Attributes.JUMP_STRENGTH)value=jumpStrength(profile,nativeBody,value);
                target.addTransientModifier(new AttributeModifier(id(attribute),value-target.getBaseValue(),AttributeModifier.Operation.ADD_VALUE));
            }
            // Dragon damage comes from multipart contact attacks, not an ATTACK_DAMAGE attribute.
            if(profile.id().equals("ender_dragon")&&nativeBody.getAttribute(Attributes.ATTACK_DAMAGE)==null){var attack=player.getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null)attack.addTransientModifier(new AttributeModifier(id(Attributes.ATTACK_DAMAGE),10-attack.getBaseValue(),AttributeModifier.Operation.ADD_VALUE));}
        }
        if(player.isAlive())player.setHealth(Math.min(player.getMaxHealth(),Math.max(.01f,ratio*player.getMaxHealth())));
    }
    public static void environment(Player player,MorphProfile profile){
        if(profile==null)return;
        var attribute=player.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute==null)return;
        var defaults=DefaultAttributes.getSupplier(profile.type());if(defaults==null||!defaults.hasAttribute(Attributes.MOVEMENT_SPEED))return;
        double base=defaults.getBaseValue(Attributes.MOVEMENT_SPEED);
        if(profile.cube())base=NativeMovement.cubeSpeed(kr.randommorph.fabric.core.MorphFlags.cubeSize(((MorphAccess)player).randommorph$flags()));
        attribute.addOrUpdateTransientModifier(new AttributeModifier(id(Attributes.MOVEMENT_SPEED),movement(player,profile,base)-attribute.getBaseValue(),AttributeModifier.Operation.ADD_VALUE));
    }
    private static double movement(Player player,MorphProfile profile,double nativeSpeed){
        // Native mob path-control gain differs from the player controls. Preserve relative species speed.
        if(profile.has(Trait.AQUATIC))return player.isInWater()?(profile.id().equals("axolotl")?.054*nativeSpeed*nativeSpeed:nativeSpeed*nativeSpeed):NativeMovement.landSpeed(profile.id(),nativeSpeed);
        double speed=NativeMovement.landSpeed(profile.id(),nativeSpeed);
        if(profile.has(Trait.COLD_STRIDER)&&!player.isInLava()&&!player.level().getBlockState(player.blockPosition()).is(net.minecraft.tags.BlockTags.STRIDER_WARM_BLOCKS))speed*=.66;
        return speed;
    }
    private static double jump(MorphProfile profile){return switch(profile.id()){
        case "goat"->1;case "frog","breeze"->.9;case "horse","donkey","mule","skeleton_horse","zombie_horse"->.7;
        case "fox","rabbit"->.5;default->.6;
    };}
    static double jumpStrength(MorphProfile profile,LivingEntity nativeBody,double nativeJump){
        if(profile.cube()){
            // Native MagmaCube.jumpFromGround adds this size gain to getJumpPower(), in float.
            if(profile.id().equals("magma_cube")){
                int size=nativeBody instanceof net.minecraft.world.entity.monster.cubemob.AbstractCubeMob cube?cube.getSize():2;
                return (float)nativeJump+.1f*size;
            }
            return nativeJump;
        }
        return profile.has(Trait.HIGH_JUMP)?Math.max(nativeJump,jump(profile)):nativeJump;
    }
    public static void clearModifiers(Player player){for(var attribute:TRACKED){var instance=player.getAttribute(attribute);if(instance!=null)instance.removeModifier(id(attribute));}}
}
