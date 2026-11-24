package kr.randommorph.verify;

import java.lang.reflect.*;
import kr.randommorph.fabric.client.WorldMorphRender;
import net.minecraft.client.player.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/** Reproduce Lunar's mismatched receiver using the real native bridge, without Lunar/GPU. */
public final class RenderCompatibilityProbe {
    private static <T> T fixture(Class<T> type)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    public static class LocalFixture extends LocalPlayer {
        private LocalFixture(){super(null,null,null,null,null,null,false,null,null);}
        @Override public boolean isSleeping(){return false;}
    }
    public static class NameRenderer extends MobRenderer<Mob,LivingEntityRenderState,EntityModel<LivingEntityRenderState>> {
        private NameRenderer(){super(null,null,0);}
        @Override protected boolean shouldShowName(Mob mob,double distance){return true;}
        @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
        @Override public Identifier getTextureLocation(LivingEntityRenderState state){return null;}
    }
    public static void run()throws Exception{
        var remote=fixture(RemotePlayer.class);remote.setId(41);
        var renderer=fixture(NameRenderer.class);
        var bridge=MobRenderer.class.getDeclaredMethod("shouldShowName",LivingEntity.class,double.class);bridge.setAccessible(true);
        try{bridge.invoke(renderer,remote,0d);throw new AssertionError("Original receiver must reproduce ClassCastException");}
        catch(InvocationTargetException error){if(!(error.getCause() instanceof ClassCastException))throw error;}
        int cases=1;
        for(var type:java.util.List.of(Vex.class,CopperGolem.class)){
            var body=fixture(type);body.setId(41);
            var selected=WorldMorphRender.select(remote,null,false,p->body);
            if(selected!=body||!Boolean.TRUE.equals(bridge.invoke(renderer,selected,0d))||body.level()!=null||remote.getId()!=41)
                throw new AssertionError("Matching body/renderer receiver failed");
            cases++;
        }
        var local=fixture(LocalFixture.class);var body=fixture(Vex.class);
        if(WorldMorphRender.select(local,local,false,p->{throw new AssertionError("First-person body was prepared");})!=local)
            throw new AssertionError("First-person exclusion failed");cases++;
        if(WorldMorphRender.select(local,local,true,p->body)!=body)throw new AssertionError("F5 body missing");cases++;
        if(WorldMorphRender.select(local,remote,true,p->{throw new AssertionError("Spectator local body was prepared");})!=local)
            throw new AssertionError("Camera identity exclusion failed");cases++;
        if(WorldMorphRender.select(remote,null,false,p->p)!=remote)throw new AssertionError("Normal/fallback player changed");cases++;
        System.out.println("RANDOMMORPH_RENDER_COMPAT_OK side=CLIENT cases="+cases+"; original ClassCastException reproduced, matching body/name bridge and camera exclusions pass; Lunar/world/GPU not launched");
    }
}
