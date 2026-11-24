package kr.randommorph.fabric.morph;
import java.util.UUID;
import kr.randommorph.fabric.ability.*;
import kr.randommorph.fabric.core.MorphFlags;
import kr.randommorph.fabric.network.MorphPayload;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShulkerStateTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @Test void eachAttachmentFaceSurvivesGrazingAndTraitUpdates(){
        for(var face:Direction.values())for(int grazing:new int[]{0,1,40}){
            int flags=MorphFlags.withFace(MorphFlags.withGrazing(127,grazing),face);
            assertTrue(MorphFlags.valid(flags));assertEquals(face,MorphFlags.face(flags));assertEquals(grazing,MorphFlags.grazing(flags));
            int updated=MorphFlags.withGrazing(flags,0);assertEquals(face,MorphFlags.face(updated));assertEquals(127,updated&127);
        }
    }
    @Test void malformedFaceCountdownAndUnknownBitsAreRejected(){
        for(int flags:new int[]{6<<13,7<<13,41<<7,1<<22,-1})assertFalse(MorphFlags.valid(flags));
    }
    @Test void attachmentPayloadAcceptsAllSixDirectionsAndRequiresNewProtocol(){
        assertEquals("form_v7",MorphPayload.TYPE.id().getPath());
        for(var face:Direction.values())assertDoesNotThrow(()->new MorphPayload(UUID.randomUUID(),"shulker",1200,false,1,MorphFlags.withFace(1,face)));
    }
    @Test void cellUsesNativeBottomCentreIncludingNegativeCoordinates(){
        var placement=new ShulkerAttachment.Placement(new BlockPos(-3,70,5),Direction.NORTH);
        assertEquals(-2.5,placement.position().x);assertEquals(70,placement.position().y);assertEquals(5.5,placement.position().z);
    }
    @Test void teleportRangeIsNativePerAxisAndDoesNotOverflow(){
        var origin=BlockPos.ZERO;assertTrue(ShulkerAttachment.inTeleportRange(origin,new BlockPos(8,-8,8)));
        assertFalse(ShulkerAttachment.inTeleportRange(origin,new BlockPos(0,9,0)));
        assertFalse(ShulkerAttachment.inTeleportRange(new BlockPos(Integer.MAX_VALUE,0,0),new BlockPos(Integer.MIN_VALUE,0,0)));
    }
    @Test void closedShellIsStationaryAndTeleportUsesSecondarySlot(){
        assertEquals(Active.SHULKER_BULLET,MorphRegistry.find("shulker").primary());assertEquals(Active.SHULKER_TELEPORT,MorphRegistry.find("shulker").secondary());
        assertEquals(0,NativeMovement.landSpeed("shulker",1));assertEquals(Active.TELEPORT,MorphRegistry.find("endermite").primary());
    }
    @Test void nativePeekOpensAndClosesWithoutRenderFrameAcceleration(){
        var animation=new ShulkerAnimation();animation.update(1,false);assertEquals(.05f,animation.current(),1e-6);
        for(int i=0;i<200;i++)animation.update(1,false);assertEquals(.05f,animation.current(),1e-6);
        for(int i=2;i<=20;i++)animation.update(i,false);assertEquals(1,animation.current(),1e-6);
        animation.update(21,true);assertEquals(1,animation.previous(),1e-6);assertEquals(.95f,animation.current(),1e-6);
        for(int i=22;i<=40;i++)animation.update(i,true);assertEquals(0,animation.current(),1e-6);
        assertEquals(0,new ShulkerAnimation().current());
    }
    @Test void nativeOpeningBoxExtendsAwayFromEachSupportFace(){
        var pos=new net.minecraft.world.phys.Vec3(.5,70,.5);
        for(var attachment:Direction.values()){
            var opening=attachment.getOpposite();
            var closed=net.minecraft.world.entity.monster.Shulker.getProgressAabb(1,opening,0,pos);
            var open=net.minecraft.world.entity.monster.Shulker.getProgressAabb(1,opening,1,pos);
            assertEquals(1,closed.getXsize(),1e-6);assertEquals(1,closed.getYsize(),1e-6);assertEquals(1,closed.getZsize(),1e-6);
            var delta=open.getCenter().subtract(closed.getCenter());
            assertTrue(delta.dot(new net.minecraft.world.phys.Vec3(opening.getStepX(),opening.getStepY(),opening.getStepZ()))>0);
        }
    }
    @Test void unsupportedLiveShulkerUsesSpaceCheckedRecoveryWithoutStoppingItsSession()throws Exception{
        try(var input=getClass().getResourceAsStream("/kr/randommorph/fabric/morph/MorphManager.class")){
            assertNotNull(input);var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(input).accept(node,0);
            var method=node.methods.stream().filter(m->m.name.equals("suspendSafely")).findFirst().orElseThrow();
            var calls=new java.util.ArrayList<String>();
            for(var instruction:method.instructions)if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call)calls.add(call.name);
            assertTrue(calls.contains("restore"));assertTrue(calls.indexOf("restore")<calls.indexOf("broadcast"));
            assertFalse(calls.contains("stop"));assertFalse(calls.contains("remove"));assertFalse(calls.contains("setForm"));
        }
    }
}
