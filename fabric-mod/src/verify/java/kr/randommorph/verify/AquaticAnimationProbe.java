package kr.randommorph.verify;
import java.lang.reflect.*;
import net.minecraft.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.squid.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import kr.randommorph.fabric.morph.MorphAccess;
/** Real transformed cache/accessors/native CPU poses, without a world or entity AI ticks. */
public final class AquaticAnimationProbe {
    public static class GuardianFixture extends Guardian {
        private GuardianFixture(EntityType<? extends Guardian> type,Level level){super(type,level);}
        @Override public void setMoving(boolean value){} // Only SynchedEntityData is absent in a worldless fixture.
    }
    public static class ElderFixture extends ElderGuardian {
        private ElderFixture(EntityType<? extends ElderGuardian> type,Level level){super(type,level);}
        @Override public void setMoving(boolean value){}
    }
    private static Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static <T>T fixture(Class<T> type)throws Exception{return (T)((sun.misc.Unsafe)field(sun.misc.Unsafe.class,"theUnsafe").get(null)).allocateInstance(type);}
    private static Object cached(LivingEntity body,Player player)throws Exception{
        var type=Class.forName("kr.randommorph.fabric.client.ClientMorphs$Model");var c=type.getDeclaredConstructor(LivingEntity.class,Player.class);c.setAccessible(true);return c.newInstance(body,player);
    }
    private static void animate(Object cache)throws Exception{var m=cache.getClass().getDeclaredMethod("animate");m.setAccessible(true);m.invoke(cache);}
    private static void pose(Player p,int tick,boolean wet,Vec3 motion,boolean ground)throws Exception{
        p.tickCount=tick;field(Entity.class,"wasTouchingWater").setBoolean(p,wet);field(Entity.class,"onGround").setBoolean(p,ground);p.setDeltaMovement(motion);
    }
    private static Object model(String name,String layer)throws Exception{
        var type=Class.forName(name);var definition=type.getMethod(layer).invoke(null);var root=definition.getClass().getMethod("bakeRoot").invoke(definition);
        return type.getConstructor(Class.forName("net.minecraft.client.model.geom.ModelPart")).newInstance(root);
    }
    private static float part(Object model,String name,String axis)throws Exception{
        var p=field(model.getClass(),name).get(model);if(p instanceof Object[] array)p=array[0];return p.getClass().getField(axis).getFloat(p);
    }
    private static void setup(Object model,Object state)throws Exception{model.getClass().getMethod("setupAnim",state.getClass()).invoke(model,state);}
    private static Object state(String name)throws Exception{return Class.forName("net.minecraft.client.renderer.entity.state."+name).getConstructor().newInstance();}
    private static void value(Object state,String name,float v)throws Exception{state.getClass().getField(name).setFloat(state,v);}
    private static void require(boolean c,String error){if(!c)throw new AssertionError(error);}
    public static void run(String side)throws Exception{
        if(!side.equals("CLIENT"))return;int cases=0;var player=fixture(net.minecraft.server.level.ServerPlayer.class);
        for(var type:java.util.List.of(GuardianFixture.class,ElderFixture.class)){
            var guardian=fixture(type);var cache=cached(guardian,player);pose(player,0,true,Vec3.ZERO,false);animate(cache);
            var model=model("net.minecraft.client.model.monster.guardian.GuardianModel",type==ElderFixture.class?"createElderGuardianLayer":"createBodyLayer");
            var state=state("GuardianRenderState");value(state,"tailAnimation",guardian.getTailAnimation(.5f));value(state,"spikesAnimation",guardian.getSpikesAnimation(.5f));setup(model,state);
            float tail=part(model,"tailParts","yRot"),spike=part(model,"spikeParts","y");
            for(int tick=1;tick<=20;tick++){pose(player,tick,true,new Vec3(.1,0,0),false);animate(cache);}
            value(state,"tailAnimation",guardian.getTailAnimation(.5f));value(state,"spikesAnimation",guardian.getSpikesAnimation(.5f));setup(model,state);
            require(Math.abs(part(model,"tailParts","yRot")-tail)>.01f,"Native guardian tail remains rigid");
            require(Math.abs(part(model,"spikeParts","y")-spike)>.01f,"Native guardian spikes remain extended while swimming");cases++;
            float now=guardian.getTailAnimation(.5f);for(int i=0;i<80;i++)animate(cache);require(now==guardian.getTailAnimation(.5f),"Guardian render advances animation each frame");cases++;
        }
        for(var type:java.util.List.of(Squid.class,GlowSquid.class)){
            var squid=fixture(type);var cache=cached(squid,player);pose(player,0,true,Vec3.ZERO,false);animate(cache);
            var model=model("net.minecraft.client.model.animal.squid.SquidModel","createBodyLayer");var state=state("SquidRenderState");
            value(state,"tentacleAngle",Mth.lerp(.5f,squid.oldTentacleAngle,squid.tentacleAngle));setup(model,state);float initial=part(model,"tentacles","xRot");
            for(int tick=1;tick<=12;tick++){pose(player,tick,true,new Vec3(.1,.04,0),false);animate(cache);}
            value(state,"tentacleAngle",Mth.lerp(.5f,squid.oldTentacleAngle,squid.tentacleAngle));setup(model,state);
            require(Math.abs(part(model,"tentacles","xRot")-initial)>.05f&&squid.xBodyRot<0,"Native squid tentacles/pitch remain rigid");cases++;
        }
        var axolotl=fixture(Axolotl.class);
        for(var name:java.util.List.of("playingDeadAnimator","inWaterAnimator","onGroundAnimator","movingAnimator"))field(Axolotl.class,name).set(axolotl,new BinaryAnimator(10,EasingType.IN_OUT_SINE));
        var cache=cached(axolotl,player);((MorphAccess)player).randommorph$flags(0);
        for(int tick=0;tick<=10;tick++){pose(player,tick,true,new Vec3(.1,0,0),false);animate(cache);}
        require(axolotl.inWaterAnimator.getFactor(.5f)>.95&&axolotl.movingAnimator.getFactor(.5f)>.95,"Axolotl swim animators do not advance");
        var model=model("net.minecraft.client.model.animal.axolotl.AdultAxolotlModel","createBodyLayer");var state=state("AxolotlRenderState");
        value(state,"inWaterFactor",axolotl.inWaterAnimator.getFactor(.5f));value(state,"movingFactor",axolotl.movingAnimator.getFactor(.5f));value(state,"ageInTicks",10);setup(model,state);float tail=part(model,"tail","yRot"),gills=part(model,"topGills","xRot");
        value(state,"ageInTicks",20);setup(model,state);require(Math.abs(part(model,"tail","yRot")-tail)>.01f&&Math.abs(part(model,"topGills","xRot")-gills)>.01f,"Native axolotl swimming tail/gills remain rigid");cases++;
        for(int tick=11;tick<=22;tick++){pose(player,tick,false,new Vec3(.1,0,0),true);animate(cache);}
        require(axolotl.onGroundAnimator.getFactor(.5f)>.95&&axolotl.inWaterAnimator.getFactor(.5f)<.05,"Axolotl cannot transition to land crawling");cases++;
        ((MorphAccess)player).randommorph$flags(4);for(int tick=23;tick<=35;tick++){pose(player,tick,false,Vec3.ZERO,true);animate(cache);}
        require(axolotl.playingDeadAnimator.getFactor(.5f)>.95&&axolotl.movingAnimator.getFactor(.5f)<.05,"Axolotl play-dead pose not isolated");cases++;
        System.out.println("RANDOMMORPH_AQUATIC_ANIMATION_OK side="+side+" cases="+cases+"; real cache/native CPU poses, no world/GPU/game was started");
    }
}
