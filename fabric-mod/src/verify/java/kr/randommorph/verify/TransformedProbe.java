package kr.randommorph.verify;
import java.util.*;
/** Registry bootstrap and transformed class loading, no world, port, SDL window or game loop. */
public final class TransformedProbe {
    public static void run(String side)throws Exception{
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        var names=new ArrayList<>(List.of(
            "net.minecraft.world.entity.player.Player","net.minecraft.world.entity.Avatar",
            "net.minecraft.world.entity.animal.frog.Frog","net.minecraft.world.entity.animal.frog.ShootTongue",
            "net.minecraft.world.item.BowItem","net.minecraft.world.item.CrossbowItem","net.minecraft.world.item.ProjectileWeaponItem","net.minecraft.world.inventory.AbstractContainerMenu",
            "net.minecraft.world.entity.Entity","net.minecraft.world.entity.LivingEntity",
            "net.minecraft.world.effect.MobEffectInstance",
            "net.minecraft.server.level.ServerPlayer","net.minecraft.server.players.PlayerList",
            "net.minecraft.world.entity.monster.breeze.Breeze","net.minecraft.world.entity.animal.bee.Bee",
            "net.minecraft.world.entity.animal.sheep.Sheep","net.minecraft.world.entity.animal.chicken.Chicken",
            "net.minecraft.world.entity.animal.cow.AbstractCow","net.minecraft.world.entity.projectile.Projectile",
            "net.minecraft.server.network.ServerGamePacketListenerImpl",
            "net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal",
            "net.minecraft.world.entity.ai.goal.AvoidEntityGoal",
            "net.minecraft.world.entity.ai.targeting.TargetingConditions",
            "net.minecraft.world.entity.monster.Shulker","net.minecraft.world.entity.monster.Guardian","net.minecraft.world.entity.monster.Blaze",
            "net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball",
            "net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball",
            "net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball",
            "net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull",
            "net.minecraft.world.entity.Mob","net.minecraft.world.entity.ai.Brain",
            "net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities","net.minecraft.world.entity.ai.behavior.EntityTracker",
            "net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor","net.minecraft.world.entity.ai.sensing.PlayerSensor",
            "net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal","net.minecraft.world.entity.animal.wolf.Wolf",
            "net.minecraft.world.entity.monster.warden.Warden","net.minecraft.world.entity.monster.warden.AngerManagement",
            "net.minecraft.world.entity.monster.warden.AngerManagement$Sorter","net.minecraft.world.entity.monster.warden.WardenAi",
            "net.minecraft.world.entity.ai.goal.AvoidEntityGoal","net.minecraft.world.entity.monster.piglin.PiglinAi","net.minecraft.world.entity.monster.piglin.PiglinBruteAi"));
        if(side.equals("CLIENT"))names.addAll(List.of("net.minecraft.client.renderer.extract.LevelExtractor","net.minecraft.client.player.LocalPlayer","net.minecraft.client.gui.Hud",
            "net.minecraft.client.renderer.entity.EntityRenderDispatcher","net.minecraft.client.renderer.entity.LivingEntityRenderer","net.minecraft.client.renderer.entity.MobRenderer","net.minecraft.client.player.RemotePlayer","net.minecraft.client.renderer.item.ItemModelResolver",
            "net.minecraft.world.entity.boss.enderdragon.EnderDragon","net.minecraft.world.entity.monster.cubemob.AbstractCubeMob",
            "net.minecraft.client.renderer.entity.EnderDragonRenderer","net.minecraft.client.model.monster.dragon.EnderDragonModel",
            "net.minecraft.client.renderer.entity.AbstractCubeMobRenderer","net.minecraft.client.model.monster.slime.MagmaCubeModel"));
        names=new ArrayList<>(new LinkedHashSet<>(names));
        for(var name:names)Class.forName(name,false,Thread.currentThread().getContextClassLoader());
        if(side.equals("SERVER")){
            net.fabricmc.loader.impl.FabricLoaderImpl.INSTANCE.invokeEntrypoints("main",net.fabricmc.api.ModInitializer.class,net.fabricmc.api.ModInitializer::onInitialize);
            var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
            Class.forName("kr.randommorph.fabric.MorphCommands").getMethod("register",com.mojang.brigadier.CommandDispatcher.class).invoke(null,dispatcher);
            var command=dispatcher.getRoot().getChild("morph");
            for(var action:List.of("start","stop","pause","resume","set","next","reroll","status","list","reload","interval","instincts","cooldown","size"))if(command.getChild(action)==null)throw new AssertionError("Missing command: "+action);
        }
        var registry=Class.forName("kr.randommorph.fabric.morph.MorphRegistry");
        var profiles=(Collection<?>)registry.getMethod("profiles").invoke(null);
        if(profiles.size()!=90)throw new AssertionError("Expected 90 species");
        var player=Class.forName("net.minecraft.world.entity.player.Player");
        if(!Class.forName("kr.randommorph.fabric.morph.MorphAccess").isAssignableFrom(player))throw new AssertionError("Player mixin missing");
        player.getDeclaredMethod("is",net.minecraft.tags.TagKey.class);
        player.getDeclaredMethod("is",Object.class);
        // Reflection forces JVM verification of the merged lifecycle handlers as well as loading.
        var lifecycle=Class.forName("net.minecraft.server.level.ServerPlayer").getDeclaredMethods();
        for(var hook:List.of("randommorph$credits","randommorph$leaveLevel","randommorph$beforeCopy","randommorph$copyCandidates","randommorph$externalLayers","randommorph$animalResources"))
            if(Arrays.stream(lifecycle).noneMatch(method->method.getName().contains(hook)))throw new AssertionError("Missing lifecycle hook: "+hook);
        if(Arrays.stream(Class.forName("net.minecraft.server.players.PlayerList").getDeclaredMethods())
                .noneMatch(method->method.getName().contains("randommorph$beforeRespawn")))throw new AssertionError("Missing pre-respawn hook");
        System.out.println("RANDOMMORPH_LIFECYCLE_HOOKS_OK side="+side+" hooks=7; native world transitions were not executed");
        EffectOwnershipProbe.run(side);
        NaturalActionProbe.run(side);
        ShulkerSoundProbe.run(side);
        NaturalModelProbe.run(side);
        SpeciesQueryProbe.run(side);
        BrainRelationProbe.run(side);
        ProtectionAngerProbe.run(side);
        FrogActionProbe.run(side);
        DetachedBodyProbe.run(side);
        AnimationProbe.run(side);
        AquaticAnimationProbe.run(side);
        MorphWeaponProbe.run(side);
        if(side.equals("CLIENT")){
            var visible=Class.forName("net.minecraft.client.renderer.extract.LevelExtractor").getDeclaredMethods();
            if(Arrays.stream(visible).noneMatch(method->method.getName().contains("randommorph$model")&&method.getReturnType()==Object.class&&Arrays.equals(method.getParameterTypes(),new Class<?>[]{Object.class,net.minecraft.client.Camera.class})))
                throw new AssertionError("Missing upstream world-render input hook");
            RenderCompatibilityProbe.run();
        }
        // Any invalid injection descriptor/shadow/override aborts the process during class transformation.
        System.out.println("RANDOMMORPH_MIXIN_OK side="+side+" targets="+names.size()+" forms="+profiles.size()+"; no game/server was started");
    }
}
