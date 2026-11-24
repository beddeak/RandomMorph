package kr.randommorph.fabric.client;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.player.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Vex;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Worldless native class/renderer fixtures; no client, world, SDL window or GPU. */
class WorldMorphRenderTest {
    @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private static <T> T fixture(Class<T> type)throws Exception{
        var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)f.get(null)).allocateInstance(type));
    }
    public static class LocalFixture extends LocalPlayer {
        boolean sleeping;
        private LocalFixture(){super(null,null,null,null,null,null,false,null,null);}
        @Override public boolean isSleeping(){return sleeping;}
    }
    public static class NameRenderer extends MobRenderer<Mob,LivingEntityRenderState,EntityModel<LivingEntityRenderState>> {
        private NameRenderer(){super(null,null,0);}
        @Override protected boolean shouldShowName(Mob mob,double distance){return true;}
        @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
        @Override public Identifier getTextureLocation(LivingEntityRenderState state){return null;}
    }
    private static Object nameBridge(Entity entity)throws Exception{
        var method=MobRenderer.class.getDeclaredMethod("shouldShowName",LivingEntity.class,double.class);method.setAccessible(true);
        return method.invoke(fixture(NameRenderer.class),entity,0d);
    }
    @Test void originalRemotePlayerReproducesTheActualMobNameBridgeCrash()throws Exception{
        var remote=fixture(RemotePlayer.class);
        var error=assertThrows(InvocationTargetException.class,()->nameBridge(remote));
        assertInstanceOf(ClassCastException.class,error.getCause());
    }
    @Test void remoteBodyPassesTheSameNativeBridgeAndLeavesThePlayerUntouched()throws Exception{
        var remote=fixture(RemotePlayer.class);remote.setId(41);var body=fixture(Vex.class);body.setId(41);
        var selected=WorldMorphRender.select(remote,null,false,p->body);
        assertSame(body,selected);assertEquals(Boolean.TRUE,nameBridge(selected));
        assertInstanceOf(RemotePlayer.class,remote);assertEquals(41,remote.getId());assertEquals(41,body.getId());assertNull(body.level());
    }
    @Test void firstPersonSelfNeverPreparesOrDisplaysABody()throws Exception{
        var local=fixture(LocalFixture.class);var calls=new AtomicInteger();
        assertSame(local,WorldMorphRender.select(local,local,false,p->{calls.incrementAndGet();return fixtureUnchecked();}));
        assertEquals(0,calls.get());
    }
    @Test void detachedF5SelfUsesTheBody()throws Exception{
        var local=fixture(LocalFixture.class);var body=fixture(Vex.class);
        assertSame(body,WorldMorphRender.select(local,local,true,p->body));
    }
    @Test void sleepingFirstPersonPreservesVanillaBodyVisibility()throws Exception{
        var local=fixture(LocalFixture.class);local.sleeping=true;var body=fixture(Vex.class);
        assertSame(body,WorldMorphRender.select(local,local,false,p->body));
    }
    @Test void spectatingAnotherEntityNeverReintroducesTheLocalBody()throws Exception{
        var local=fixture(LocalFixture.class);var camera=fixture(RemotePlayer.class);
        assertSame(local,WorldMorphRender.select(local,camera,true,p->{fail("Hidden local body must not be prepared");return null;}));
    }
    @Test void normalMobsBypassPlayerPreparation()throws Exception{
        var mob=fixture(Vex.class);
        assertSame(mob,WorldMorphRender.select(mob,null,true,p->{fail("Native mob must remain native");return null;}));
    }
    @Test void untransformedPlayerOrPreparationFallbackKeepsThePlayerInput()throws Exception{
        var remote=fixture(RemotePlayer.class);
        assertSame(remote,WorldMorphRender.select(remote,null,false,p->p));
    }
    @Test void simultaneousPlayersAndReplacementFormsKeepSeparateRenderInputs()throws Exception{
        var first=fixture(RemotePlayer.class);var second=fixture(RemotePlayer.class);
        var firstBody=fixture(Vex.class);var secondBody=fixture(Vex.class);var replacement=fixture(Vex.class);
        assertSame(firstBody,WorldMorphRender.select(first,null,false,p->p==first?firstBody:secondBody));
        assertSame(secondBody,WorldMorphRender.select(second,null,false,p->p==first?firstBody:secondBody));
        assertSame(replacement,WorldMorphRender.select(first,null,false,p->replacement));
        assertSame(secondBody,WorldMorphRender.select(second,null,false,p->secondBody));
    }
    private static Entity fixtureUnchecked(){try{return fixture(Vex.class);}catch(Exception e){throw new AssertionError(e);}}
}
