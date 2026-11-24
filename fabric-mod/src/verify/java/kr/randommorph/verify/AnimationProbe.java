package kr.randommorph.verify;

import java.lang.reflect.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.*;
import net.minecraft.world.entity.monster.cubemob.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Exercises the real cached model update and native CPU poses; no entity constructors/world/GPU. */
public final class AnimationProbe {
    private static Field field(Class<?> type,String name)throws Exception{
        var field=type.getDeclaredField(name);field.setAccessible(true);return field;
    }
    @SuppressWarnings("unchecked")
    private static <T>T fixture(Class<T> type)throws Exception{
        var field=field(sun.misc.Unsafe.class,"theUnsafe");
        return (T)((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
    }
    private static void pose(Player player,int tick,Vec3 position,float yaw,boolean ground)throws Exception{
        player.tickCount=tick;
        field(Entity.class,"position").set(player,position);
        field(Entity.class,"onGround").setBoolean(player,ground);
        player.setYRot(yaw);
    }
    private static Object cached(LivingEntity body,Player player)throws Exception{
        var type=Class.forName("kr.randommorph.fabric.client.ClientMorphs$Model");
        var constructor=type.getDeclaredConstructor(LivingEntity.class,Player.class);constructor.setAccessible(true);
        return constructor.newInstance(body,player);
    }
    private static void animate(Object cached)throws Exception{
        var method=cached.getClass().getDeclaredMethod("animate");method.setAccessible(true);method.invoke(cached);
    }
    private static Object nativeModel(Class<?> model)throws Exception{
        var layer=model.getMethod("createBodyLayer").invoke(null);
        var root=layer.getClass().getMethod("bakeRoot").invoke(layer);
        return model.getConstructor(Class.forName("net.minecraft.client.model.geom.ModelPart")).newInstance(root);
    }
    private static float rotation(Object model,String part,String axis)throws Exception{
        var value=field(model.getClass(),part).get(model);
        if(value instanceof Object[] parts)value=parts[0];
        return value.getClass().getField(axis).getFloat(value);
    }
    private static void require(boolean condition,String failure){if(!condition)throw new AssertionError(failure);}
    private static void dragonPose(Object model,Object state,EnderDragon dragon)throws Exception{
        var type=state.getClass();
        type.getField("flapTime").setFloat(state,Mth.lerp(.5f,dragon.oFlapTime,dragon.flapTime));
        type.getField("partialTicks").setFloat(state,.5f);
        ((DragonFlightHistory)type.getField("flightHistory").get(state)).copyFrom(dragon.flightHistory);
        model.getClass().getMethod("setupAnim",type).invoke(model,state);
    }
    public static void run(String side)throws Exception{
        if(!side.equals("CLIENT"))return;
        int cases=0;
        var player=fixture(net.minecraft.server.level.ServerPlayer.class);
        var dragon=fixture(EnderDragon.class);field(EnderDragon.class,"flightHistory").set(dragon,new DragonFlightHistory());
        var cached=cached(dragon,player);
        pose(player,100,new Vec3(40,150,40),60,false);animate(cached);
        for(int i=0;i<DragonFlightHistory.LENGTH;i++)require(dragon.flightHistory.get(i).y()==150&&dragon.flightHistory.get(i).yRot()==-120,"Unseeded dragon history");
        cases++;

        var dragonModel=nativeModel(Class.forName("net.minecraft.client.model.monster.dragon.EnderDragonModel"));
        var dragonState=Class.forName("net.minecraft.client.renderer.entity.state.EnderDragonRenderState").getConstructor().newInstance();
        dragonPose(dragonModel,dragonState,dragon);
        float wing=rotation(dragonModel,"leftWing","zRot"),wingTip=rotation(dragonModel,"leftWingTip","zRot");
        pose(player,101,player.position(),60,false);animate(cached);dragonPose(dragonModel,dragonState,dragon);
        require(Math.abs(rotation(dragonModel,"leftWing","zRot")-wing)>.1f&&Math.abs(rotation(dragonModel,"leftWingTip","zRot")-wingTip)>.01f,"Native dragon wings did not animate");
        cases++;

        for(int tick=102;tick<=130;tick++){
            pose(player,tick,new Vec3(40+(tick-101)*.2,150+(tick-101)*.1,40),60+(tick-101)*3,false);animate(cached);
        }
        dragonPose(dragonModel,dragonState,dragon);
        require(Math.abs(rotation(dragonModel,"head","yRot"))>.1f&&Math.abs(rotation(dragonModel,"neckParts","yRot"))>.01f&&
                Math.abs(rotation(dragonModel,"tailParts","yRot"))>.01f&&Math.abs(rotation(dragonModel,"body","zRot"))>.1f,"Native dragon head/neck/tail/body turn stayed rigid");
        require(Math.abs(dragon.flightHistory.get(5).y()-dragon.flightHistory.get(10).y())>.4,"Native body pitch history stayed flat");
        cases++;

        var sample=dragon.flightHistory.get(0);float flap=dragon.flapTime,oldFlap=dragon.oFlapTime;
        for(int frame=0;frame<120;frame++)animate(cached);
        require(sample==dragon.flightHistory.get(0)&&flap==dragon.flapTime&&oldFlap==dragon.oFlapTime,"Rendering advanced or froze the wrong animation interval");
        cases++;

        pose(player,131,new Vec3(900,250,900),90,false);animate(cached);
        for(int i=0;i<DragonFlightHistory.LENGTH;i++)require(dragon.flightHistory.get(i).y()==250,"Teleport retained the old flight curve");
        cases++;

        for(var cubeType:java.util.List.of(Slime.class,MagmaCube.class,SulfurCube.class)){
            var cube=fixture(cubeType);var cubeCached=cached(cube,player);
            pose(player,0,Vec3.ZERO,0,true);animate(cubeCached);require(cube.squish==0,"Cube starts with a false landing");
            pose(player,1,Vec3.ZERO,0,false);animate(cubeCached);
            pose(player,2,Vec3.ZERO,0,false);animate(cubeCached);
            require(cube.squish>0&&cube.oSquish==0,"Native jump squish was not assigned");
            float jump=cube.squish;
            for(int frame=0;frame<60;frame++)animate(cubeCached);
            require(cube.squish==jump,"Cube animation advanced per render frame");
            if(cube instanceof MagmaCube){
                var magmaModel=nativeModel(Class.forName("net.minecraft.client.model.monster.slime.MagmaCubeModel"));
                var stateType=Class.forName("net.minecraft.client.renderer.entity.state.SlimeRenderState");
                var state=stateType.getConstructor().newInstance();
                var setup=magmaModel.getClass().getMethod("setupAnim",stateType);setup.invoke(magmaModel,state);
                float closed=rotation(magmaModel,"bodyCubes","y");
                stateType.getField("squish").setFloat(state,Mth.lerp(.5f,cube.oSquish,cube.squish));setup.invoke(magmaModel,state);
                require(Math.abs(rotation(magmaModel,"bodyCubes","y")-closed)>.1f,"Native magma layers did not separate");
                cases++;
            }
            pose(player,3,Vec3.ZERO,0,true);animate(cubeCached);
            for(int tick=4;tick<=9;tick++){pose(player,tick,Vec3.ZERO,0,true);animate(cubeCached);}
            require(cube.squish<0,"Landing did not compress cube");
            cases++;
        }
        System.out.println("RANDOMMORPH_ANIMATION_OK side="+side+" cases="+cases+"; real cache/native CPU poses, no entity constructor/world/GPU/game was started");
    }
}
