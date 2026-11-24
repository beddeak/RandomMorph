package kr.randommorph.fabric.client;
import java.util.*;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.network.MorphPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.slf4j.LoggerFactory;

/** Native render models are detached from the world; they cannot intercept mining/entity picking. */
public final class ClientMorphs {
    private static final Map<UUID,Entry> STATES=new HashMap<>();
    private static final Map<UUID,Model> MODELS=new HashMap<>();
    private static final Map<UUID,Player> FAILED=new HashMap<>();
    private static final kr.randommorph.fabric.core.ClientContext CONTEXT=new kr.randommorph.fabric.core.ClientContext();
    private static int tick;
    private record Entry(MorphPayload state,int received) {}
    private record Model(LivingEntity body,Player player,DragonAnimation dragonAnimation,CubeAnimation cubeAnimation,ShulkerAnimation shulkerAnimation,AquaticAnimation aquaticAnimation) {
        Model(LivingEntity body,Player player){
            this(body,player,body instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon?new DragonAnimation(dragon.flightHistory):null,
                    body instanceof net.minecraft.world.entity.monster.cubemob.AbstractCubeMob?new CubeAnimation(body instanceof net.minecraft.world.entity.monster.cubemob.MagmaCube):null,
                    body instanceof net.minecraft.world.entity.monster.Shulker?new ShulkerAnimation():null,
                    body instanceof net.minecraft.world.entity.monster.Guardian||body instanceof net.minecraft.world.entity.animal.squid.Squid||body instanceof net.minecraft.world.entity.animal.axolotl.Axolotl?new AquaticAnimation():null);
        }
        void animate(){
            if(aquaticAnimation!=null){
                var motion=player.getDeltaMovement();boolean moving=motion.lengthSqr()>.0001;
                if(aquaticAnimation.update(player.tickCount,player.isInWater(),moving,motion.y,motion.horizontalDistance())){
                    if(body instanceof net.minecraft.world.entity.animal.axolotl.Axolotl axolotl){
                        boolean dead=(((MorphAccess)player).randommorph$flags()&4)!=0;
                        axolotl.playingDeadAnimator.tick(dead);axolotl.inWaterAnimator.tick(!dead&&player.isInWater());
                        axolotl.onGroundAnimator.tick(!dead&&!player.isInWater()&&player.onGround());axolotl.movingAnimator.tick(!dead&&moving);
                    }
                }
                if(body instanceof net.minecraft.world.entity.monster.Guardian guardian){
                    var access=(kr.randommorph.fabric.mixin.GuardianAnimationAccess)guardian;
                    guardian.setMoving(player.isInWater()&&moving);
                    access.randommorph$tail(aquaticAnimation.tail());access.randommorph$tailOld(aquaticAnimation.tailOld());
                    access.randommorph$spikes(aquaticAnimation.spikes());access.randommorph$spikesOld(aquaticAnimation.spikesOld());
                }
                if(body instanceof net.minecraft.world.entity.animal.squid.Squid squid){
                    squid.tentacleAngle=aquaticAnimation.tentacle();squid.oldTentacleAngle=aquaticAnimation.tentacleOld();
                    squid.xBodyRot=aquaticAnimation.pitch();squid.xBodyRotO=aquaticAnimation.pitchOld();
                    squid.zBodyRot=aquaticAnimation.roll();squid.zBodyRotO=aquaticAnimation.rollOld();
                }
            }
            if(body instanceof net.minecraft.world.entity.monster.Shulker shulker){
                shulkerAnimation.update(player.tickCount,(((MorphAccess)player).randommorph$flags()&1)!=0);
                var access=(kr.randommorph.fabric.mixin.ShulkerAccess)shulker;
                access.randommorph$peekPrevious(shulkerAnimation.previous());access.randommorph$peekCurrent(shulkerAnimation.current());
            }
            if(body instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon){
                dragonAnimation.update(player.tickCount,player.position(),player.getYRot());
                dragon.oFlapTime=dragonAnimation.previousFlapTime();dragon.flapTime=dragonAnimation.flapTime();
            }
            if(body instanceof net.minecraft.world.entity.monster.cubemob.AbstractCubeMob cube){
                cubeAnimation.update(player.tickCount,player.onGround());
                cube.oSquish=cubeAnimation.previousSquish();cube.squish=cubeAnimation.squish();cube.targetSquish=cubeAnimation.targetSquish();
            }
        }
    }
    private ClientMorphs() {}
    public static void refreshContext() {
        var client=Minecraft.getInstance();
        if(CONTEXT.update(client.level,client.player)){
            MODELS.clear();FAILED.clear();ClientSenses.clear();ClientInstincts.clear();
        }
    }
    public static void receive(MorphPayload payload) {
        refreshContext();
        var previous=STATES.get(payload.player());
        if(previous!=null && previous.state.revision()>payload.revision())return;
        var local=Minecraft.getInstance().player;
        if(local!=null&&local.getUUID().equals(payload.player())&&previous!=null&&!previous.state.form().equals(payload.form())){
            ClientSenses.clear();ClientInstincts.clear();
        }
        if(previous==null || !previous.state.form().equals(payload.form())){MODELS.remove(payload.player());FAILED.remove(payload.player());}
        STATES.put(payload.player(),new Entry(payload,tick));
    }
    public static void reset() {
        var level=Minecraft.getInstance().level;
        if(level!=null)for(var player:level.players()){((MorphAccess)player).randommorph$form(null);((MorphAccess)player).randommorph$flags(0);player.refreshDimensions();}
        STATES.clear();MODELS.clear();FAILED.clear();CONTEXT.reset();tick=0;ClientSenses.clear();ClientInstincts.clear();
    }
    public static void tick() {
        refreshContext();tick++;
        STATES.entrySet().removeIf(e -> tick-e.getValue().received>200);
        var level=Minecraft.getInstance().level;
        if(level==null){ClientSenses.tick();ClientInstincts.tick();return;}
        for(var player:level.players()){
            var entry=STATES.get(player.getUUID());
            var profile=entry==null?null:MorphRegistry.find(entry.state.form());
            var access=(MorphAccess)player;
            int flags=entry==null?0:kr.randommorph.fabric.core.MorphFlags.withGrazing(entry.state.flags(),
                    kr.randommorph.fabric.core.MorphFlags.grazing(entry.state.flags())-Math.max(0,tick-entry.received));
            if(access.randommorph$form()!=profile||access.randommorph$flags()!=flags){access.randommorph$form(profile);access.randommorph$flags(flags);player.refreshDimensions();}
        }
        MODELS.entrySet().removeIf(e -> !STATES.containsKey(e.getKey()) || e.getValue().body.level()!=level || level.getPlayerByUUID(e.getKey())!=e.getValue().player);
        FAILED.entrySet().removeIf(e->level.getPlayerByUUID(e.getKey())!=e.getValue());
        for(var entry:MODELS.entrySet()){
            var player=level.getPlayerByUUID(entry.getKey());
            if(player!=null)entry.getValue().body.walkAnimation.update(player.walkAnimation.speed(),1,1);
        }
        ClientSenses.tick();ClientInstincts.tick();
    }
    public static void tickAnimations(){
        refreshContext();
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        // END_CLIENT_TICK observes this tick's movement, even while a cached model is offscreen.
        for(var model:MODELS.values())if(level.getPlayerByUUID(model.player.getUUID())==model.player && model.player.isAlive())model.animate();
    }
    /** Prepare the actual render input, before renderer lookup and other mods consume it. */
    public static Entity prepare(Player player) {
        refreshContext();
        if(player.isAlive()) {
            var profile=((MorphAccess)player).randommorph$form();
            if(profile!=null && FAILED.get(player.getUUID())!=player)try{
                var cached=MODELS.get(player.getUUID());
                if(cached==null || cached.player!=player || cached.body.getType()!=profile.type() || cached.body.level()!=player.level()) {
                    cached=new Model(MorphBodies.create(profile,player),player);
                    MODELS.put(player.getUUID(),cached);
                }
                var model=cached.body;
                cached.animate(); // Seed a newly visible model; repeated extraction is tick-idempotent.
                model.copyPosition(player);model.xOld=player.xOld;model.yOld=player.yOld;model.zOld=player.zOld;
                model.tickCount=player.tickCount;model.setOnGround(player.onGround());model.setDeltaMovement(player.getDeltaMovement());
                model.setInvisible(player.isInvisible());model.setGlowingTag(player.isCurrentlyGlowing());
                model.setRemainingFireTicks(player.getRemainingFireTicks());model.setShiftKeyDown(player.isShiftKeyDown());
                model.yBodyRot=player.yBodyRot;model.yBodyRotO=player.yBodyRotO;model.yHeadRot=player.yHeadRot;model.yHeadRotO=player.yHeadRotO;
                model.setCustomName(player.getDisplayName());model.setCustomNameVisible(player.isCustomNameVisible());
                MorphBodies.flags(model,((MorphAccess)player).randommorph$flags());
                for(var slot:EquipmentSlot.values())model.setItemSlot(slot,player.getItemBySlot(slot).copy());
                if(player.getMainHandItem().isEmpty()){
                    var item=switch(profile.id()){
                        case "skeleton","stray","bogged","parched","illusioner"->net.minecraft.world.item.Items.BOW;
                        case "pillager"->net.minecraft.world.item.Items.CROSSBOW;
                        case "vindicator"->net.minecraft.world.item.Items.IRON_AXE;
                        case "vex"->net.minecraft.world.item.Items.IRON_SWORD;
                        case "wither_skeleton"->net.minecraft.world.item.Items.STONE_SWORD;
                        case "piglin_brute"->net.minecraft.world.item.Items.GOLDEN_AXE;
                        case "piglin"->net.minecraft.world.item.Items.GOLDEN_SWORD;
                        case "drowned"->net.minecraft.world.item.Items.TRIDENT;
                        default->null;
                    };
                    if(item!=null)model.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(item));
                }
                if(kr.randommorph.fabric.ability.MorphWeapons.ranged(profile)){
                    if(model instanceof Mob mob)mob.setAggressive(player.isUsingItem());
                    if(player.isUsingItem()){model.startUsingItem(player.getUsedItemHand());((kr.randommorph.fabric.mixin.WeaponAnimationAccess)model).randommorph$useRemaining(player.getUseItemRemainingTicks());}
                    else if(model.isUsingItem())model.stopUsingItem();
                    if(model instanceof net.minecraft.world.entity.monster.illager.Pillager pillager)pillager.setChargingCrossbow(player.isUsingItem());
                }
                if(model instanceof net.minecraft.world.entity.monster.Witch witch){
                    boolean drinking=(((MorphAccess)player).randommorph$flags()&16)!=0;witch.setUsingItem(drinking);
                    if(drinking)model.setItemSlot(EquipmentSlot.MAINHAND,net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.HEALING));
                }
                if(model instanceof net.minecraft.world.entity.boss.wither.WitherBoss || model instanceof net.minecraft.world.entity.animal.golem.IronGolem || model instanceof net.minecraft.world.entity.monster.cubemob.AbstractCubeMob)model.setHealth(model.getMaxHealth()*kr.randommorph.fabric.core.MorphSession.healthRatio(player.getHealth(),player.getMaxHealth()));
                model.hurtTime=player.hurtTime;model.hurtDuration=player.hurtDuration;
                return model;
            }catch(RuntimeException|LinkageError error){
                FAILED.put(player.getUUID(),player);MODELS.remove(player.getUUID());
                LoggerFactory.getLogger("RandomMorph").error("Morph body preparation failed; using player model until form changes",error);
            }
        }
        return player;
    }
}
