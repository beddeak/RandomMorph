package kr.randommorph.fabric.morph;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
/** Render/selector bodies are never added to the world's entity list or ticked with mob AI. */
public final class MorphBodies {
    private MorphBodies(){}
    public static LivingEntity create(MorphProfile profile,LivingEntity owner){
        // 26.3 leaves client-created entities at ID 0 until their spawn packet assigns an ID.
        // Detached bodies receive no packet; share their owner's ID without joining the world.
        int entityId=owner.getId();
        var body=profile.type().create(owner.level(),new EntitySpawnRequest(EntitySpawnReason.LOAD,true));
        if(body==null)throw new IllegalStateException("No native model: "+profile.id());
        body.setId(entityId);
        body.setSilent(true); // Detached render/AI identities never author world audio.
        if(body instanceof AbstractCubeMob cube)cube.setSize(2,true);
        return body;
    }
    public static void flags(LivingEntity body,int flags){
        if(body instanceof AbstractCubeMob cube){
            int size=kr.randommorph.fabric.core.MorphFlags.cubeSize(flags);
            if(cube.getSize()!=size)cube.setSize(size,false);
        }
        if(body instanceof net.minecraft.world.entity.monster.Shulker shulker){
            var access=(kr.randommorph.fabric.mixin.ShulkerAccess)shulker;access.randommorph$face(kr.randommorph.fabric.core.MorphFlags.face(flags));
            shulker.getEntityData().set(kr.randommorph.fabric.mixin.ShulkerAccess.randommorph$peekData(),(byte)((flags&1)==0?100:0));
        }
        if(body instanceof net.minecraft.world.entity.animal.bee.Bee bee)((kr.randommorph.fabric.mixin.BeeAccess)bee).randommorph$stung((flags&kr.randommorph.fabric.core.AnimalState.STUNG)!=0);
        if(body instanceof net.minecraft.world.entity.animal.golem.SnowGolem snow)snow.setPumpkin((flags&kr.randommorph.fabric.core.AnimalState.SHEARED)==0);
        if(body instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep){
            sheep.setSheared((flags&kr.randommorph.fabric.core.AnimalState.SHEARED)!=0);
            sheep.setColor(net.minecraft.world.item.DyeColor.byId(kr.randommorph.fabric.core.MorphFlags.color(flags)));
            ((kr.randommorph.fabric.mixin.SheepEatAccess)sheep).randommorph$eating(kr.randommorph.fabric.core.MorphFlags.grazing(flags));
        }
        if(body instanceof net.minecraft.world.entity.monster.Blaze blaze)((kr.randommorph.fabric.mixin.BlazeAccess)blaze).randommorph$charged((flags&16)!=0);
        if(body instanceof Pufferfish fish)fish.setPuffState((flags&2)==0?0:2);
        if(body instanceof Armadillo armadillo)armadillo.switchToState((flags&1)==0?Armadillo.ArmadilloState.IDLE:Armadillo.ArmadilloState.SCARED);
        if(body instanceof Axolotl axolotl)axolotl.setPlayingDead((flags&4)!=0);
    }
}
