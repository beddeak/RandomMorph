package kr.randommorph.fabric.ability;
import kr.randommorph.fabric.morph.MorphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
/** Native Java armor threshold and damage-triggered destructive volume, plus collision escape. */
public final class WitherTraits {
    private WitherTraits(){}
    public static boolean powered(float health,float maximum){return maximum>0&&health<=maximum/2;}
    public static int radius(float width){return Mth.floor(width/2+1);}
    public static int height(float height){return Mth.floor(height);}
    public static void damaged(ServerPlayer player){
        var form=AbilityHooks.profile(player);if(form==null||!form.id().equals("wither"))return;
        var state=kr.randommorph.fabric.RandomMorphMod.manager(player.level().getServer()).abilityState(player);
        if(state!=null&&state.witherDestroyTicks<=0)state.witherDestroyTicks=20;
    }
    public static void tick(MorphManager manager,ServerPlayer player,AbilityState state){
        if(player.horizontalCollision&&state.witherDestroyTicks<=0)state.witherDestroyTicks=20;
        if(state.witherDestroyTicks<=0||--state.witherDestroyTicks!=0||!AbilityEngine.destruction(manager,player))return;
        var level=player.level();var center=player.blockPosition();boolean changed=false;
        int radius=radius(player.getBbWidth()),height=height(player.getBbHeight());
        for(var pos:BlockPos.betweenClosed(center.offset(-radius,0,-radius),center.offset(radius,height,radius))){
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos))continue;
            if(WitherBoss.canDestroy(level.getBlockState(pos)))changed=level.destroyBlock(pos,true,player)||changed;
        }
        if(changed)level.levelEvent(1022,center,0);
    }
}
