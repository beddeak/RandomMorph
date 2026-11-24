package kr.randommorph.fabric.morph;
import java.util.Set;
import kr.randommorph.fabric.ability.*;
import net.minecraft.world.entity.*;
public record MorphProfile(String id, String name, EntityType<? extends LivingEntity> type,
                           Set<Trait> traits, Active primary, Active secondary) {
    public MorphProfile { traits=Set.copyOf(traits); }
    public boolean has(Trait trait){return traits.contains(trait);}
    public boolean phases(){return has(Trait.VEX_PHASE)||has(Trait.DRAGON_PHASE);}
    public boolean boss(){return id.equals("wither")||id.equals("ender_dragon")||id.equals("warden");}
    public boolean cube(){return id.equals("slime")||id.equals("magma_cube")||id.equals("sulfur_cube");}
    public EntityDimensions dimensions(int flags){
        var body=type.getDimensions();
        if(cube())body=body.scale(kr.randommorph.fabric.core.MorphFlags.cubeSize(flags));
        if(id.equals("pufferfish")&&(flags&2)==0)body=body.scale(.5f);
        return body;
    }
}
