package kr.randommorph.fabric.morph;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Native bytecode checks, not a claim that runtime Mixin transformation was executed. */
class MixinTargetsTest {
    @org.junit.jupiter.api.BeforeAll static void registries(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private ClassNode nativeClass(String name) throws IOException {
        try(var stream=getClass().getResourceAsStream("/"+name+".class")) {
            assertNotNull(stream,name);
            var node=new ClassNode();new ClassReader(stream).accept(node,0);return node;
        }
    }
    @Test void avatarBodyInjectionMatchesNative26_3() throws Exception {
        var node=nativeClass("net/minecraft/world/entity/Avatar");
        assertEquals(1,node.methods.stream().filter(m->m.name.equals("getDefaultDimensions")&&m.desc.equals("(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;")).count());
    }
    @Test void playerInheritsAvatar() throws Exception {
        assertEquals("net/minecraft/world/entity/Avatar",nativeClass("net/minecraft/world/entity/player/Player").superName);
    }
    @Test void worldRenderInputHookIsExactlyOneIteratorCallBeforeCameraAndExtraction() throws Exception {
        var node=nativeClass("net/minecraft/client/renderer/extract/LevelExtractor");
        int calls=0;
        for(var method:node.methods)if(method.name.equals("extractVisibleEntities"))for(var instruction:method.instructions)
            if(instruction instanceof MethodInsnNode invoke&&invoke.owner.equals("java/util/Iterator")&&invoke.name.equals("next")&&
                invoke.desc.equals("()Ljava/lang/Object;"))calls++;
        assertEquals(1,calls);
        var visible=node.methods.stream().filter(m->m.name.equals("extractVisibleEntities")).findFirst().orElseThrow();
        assertTrue(callPosition(visible,"next")<callPosition(visible,"isEntityVisible"));
        assertTrue(callPosition(visible,"next")<callPosition(visible,"extractEntity"));
        assertTrue(callPosition(visible,"entity")<callPosition(visible,"extractEntity"));
    }
    @Test void mobNameBridgeRequiresAMobEvenWhenTheRenderStateCameFromAPlayer() throws Exception {
        var bridge=method(nativeClass("net/minecraft/client/renderer/entity/MobRenderer"),"shouldShowName",
                "(Lnet/minecraft/world/entity/LivingEntity;D)Z");
        assertTrue(java.util.stream.StreamSupport.stream(bridge.instructions.spliterator(),false)
                .anyMatch(i->i instanceof TypeInsnNode cast&&cast.getOpcode()==Opcodes.CHECKCAST&&cast.desc.equals("net/minecraft/world/entity/Mob")));
    }
    @Test void clientEntitiesDoNotReceiveAnIdFromTheBaseLevel() throws Exception {
        var level=nativeClass("net/minecraft/world/level/Level");
        var next=method(level,"getNextEntityId","()I");
        var operations=java.util.stream.StreamSupport.stream(next.instructions.spliterator(),false)
                .mapToInt(AbstractInsnNode::getOpcode).filter(op->op>=0).toArray();
        assertArrayEquals(new int[]{Opcodes.ICONST_0,Opcodes.IRETURN},operations);
        assertFalse(nativeClass("net/minecraft/client/multiplayer/ClientLevel").methods.stream().anyMatch(m->m.name.equals("getNextEntityId")));
    }
    @Test void nativeLivingItemExtractionRequiresAnAssignedEntityId() throws Exception {
        var resolver=nativeClass("net/minecraft/client/renderer/item/ItemModelResolver");
        var update=method(resolver,"updateForLiving","(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/LivingEntity;)V");
        assertTrue(callPosition(update,"getId")<callPosition(update,"updateForTopItem"));
    }
    @Test void dragonRendererAndBodyOrientationConsumeNativeAnimationHistory() throws Exception {
        var renderer=nativeClass("net/minecraft/client/renderer/entity/EnderDragonRenderer");
        var extract=method(renderer,"extractRenderState","(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;F)V");
        var fields=new java.util.HashSet<String>();
        for(var instruction:extract.instructions)if(instruction instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.GETFIELD)fields.add(field.name);
        assertTrue(fields.containsAll(java.util.Set.of("oFlapTime","flapTime","flightHistory")));
        assertTrue(callPosition(extract,"copyFrom")>=0);
        var submit=method(renderer,"submit","(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V");
        assertTrue(callPosition(submit,"getHistoricalPos")<callPosition(submit,"rotateDegrees"));
    }
    @Test void cubeRendererInterpolatesTheNativeSquishPair() throws Exception {
        var extract=method(nativeClass("net/minecraft/client/renderer/entity/AbstractCubeMobRenderer"),"extractRenderState",
                "(Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob;Lnet/minecraft/client/renderer/entity/state/SlimeRenderState;F)V");
        var fields=new java.util.HashSet<String>();
        for(var instruction:extract.instructions)if(instruction instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.GETFIELD)fields.add(field.name);
        assertTrue(fields.containsAll(java.util.Set.of("oSquish","squish")));assertTrue(callPosition(extract,"lerp")>=0);
    }
    @Test void slimeAndMagmaKeepDifferentNativeSpringDamping() throws Exception {
        for(var entry:java.util.Map.of("AbstractCubeMob",.6f,"MagmaCube",.9f).entrySet()){
            var decrease=method(nativeClass("net/minecraft/world/entity/monster/cubemob/"+entry.getKey()),"decreaseSquish","()V");
            assertTrue(java.util.stream.StreamSupport.stream(decrease.instructions.spliterator(),false)
                    .anyMatch(i->i instanceof LdcInsnNode constant&&entry.getValue().equals(constant.cst)));
        }
    }
    @Test void everyFoundationFormHasNativeRenderer() throws Exception {
        var renderers=nativeClass("net/minecraft/client/renderer/entity/EntityRenderers");
        var types=new java.util.HashSet<String>();
        for(var method:renderers.methods)for(var instruction:method.instructions)
            if(instruction instanceof FieldInsnNode field && field.owner.equals("net/minecraft/world/entity/EntityTypes"))types.add(field.name);
        for(var profile:MorphRegistry.profiles())assertTrue(types.contains(profile.id().toUpperCase(java.util.Locale.ROOT)),profile.id());
    }
    @Test void effectOwnershipHooksMatchCopyPromotionAndBothDurationWrites() throws Exception {
        var node=nativeClass("net/minecraft/world/effect/MobEffectInstance");
        assertEquals(1,node.methods.stream().filter(m->m.name.equals("setDetailsFrom")&&
            m.desc.equals("(Lnet/minecraft/world/effect/MobEffectInstance;)V")).count());
        int writes=0;
        for(var method:node.methods)if(method.name.equals("update"))for(var instruction:method.instructions)
            if(instruction instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.PUTFIELD&&
                field.owner.equals(node.name)&&field.name.equals("duration")&&field.desc.equals("I"))writes++;
        assertEquals(2,writes);
    }
    private MethodNode method(ClassNode node,String name,String descriptor) {
        var matches=node.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(descriptor)).toList();
        assertEquals(1,matches.size(),name);return matches.getFirst();
    }
    private int callPosition(MethodNode method,String name) {
        for(int i=0;i<method.instructions.size();i++)if(method.instructions.get(i) instanceof MethodInsnNode call&&call.name.equals(name))return i;
        fail("Missing native call "+name);return -1;
    }
    @Test void creditsRemovesThePlayerWithoutCallingNormalDimensionTeleport() throws Exception {
        var credits=method(nativeClass("net/minecraft/server/level/ServerPlayer"),"showEndCredits","()V");
        assertTrue(callPosition(credits,"removePlayerImmediately")>callPosition(credits,"unRide"));
        assertTrue(java.util.stream.StreamSupport.stream(credits.instructions.spliterator(),false)
                .noneMatch(i->i instanceof MethodInsnNode call&&call.name.equals("teleport")));
        assertTrue(java.util.stream.StreamSupport.stream(credits.instructions.spliterator(),false)
                .anyMatch(i->i instanceof FieldInsnNode field&&field.name.equals("wonGame")&&field.getOpcode()==Opcodes.PUTFIELD));
    }
    @Test void replacementCleanupMustPrecedeSpawnSelectionAndRestoreFrom() throws Exception {
        var respawn=method(nativeClass("net/minecraft/server/players/PlayerList"),"respawn",
                "(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;");
        assertTrue(callPosition(respawn,"findRespawnPositionAndUseSpawnBlock")<callPosition(respawn,"removePlayerImmediately"));
        assertTrue(callPosition(respawn,"removePlayerImmediately")<callPosition(respawn,"restoreFrom"));
    }
    @Test void aliveRespawnCopiesHealthAndEffectsUsingThePinnedConstructor() throws Exception {
        var node=nativeClass("net/minecraft/server/level/ServerPlayer");
        var restore=method(node,"restoreFrom","(Lnet/minecraft/server/level/ServerPlayer;Z)V");
        assertTrue(callPosition(restore,"getHealth")<callPosition(restore,"getActiveEffects"));
        int copies=0;
        for(var instruction:restore.instructions)if(instruction instanceof MethodInsnNode call&&
                call.owner.equals("net/minecraft/world/effect/MobEffectInstance")&&call.name.equals("<init>")&&
                call.desc.equals("(Lnet/minecraft/world/effect/MobEffectInstance;)V"))copies++;
        assertEquals(1,copies);
        method(node,"teleport","(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;");
    }
}
