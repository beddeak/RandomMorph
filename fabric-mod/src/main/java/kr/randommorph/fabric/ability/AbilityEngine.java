package kr.randommorph.fabric.ability;

import kr.randommorph.fabric.morph.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.entity.projectile.hurtingprojectile.*;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.*;
import java.util.*;
import static kr.randommorph.fabric.ability.Trait.*;

/** Native projectiles, damage sources and block tags. No invented scouting or combat powers. */
public final class AbilityEngine {
    public static final String OWNED_TAG="randommorph_ability";
    private static final net.minecraft.resources.Identifier SHELL_ARMOR=net.minecraft.resources.Identifier.fromNamespaceAndPath("randommorph","ability/shell_armor");
    private AbilityEngine(){}
    public static void apply(ServerPlayer player,MorphProfile profile,AbilityState state){
        if(profile.has(FLIGHT)){
            state.ownsFlight=true;
            if(MorphFlight.update(player.getAbilities(),player.isSpectator(),NativeMovement.flightSpeed(profile),true))player.onUpdateAbilities();
        }
        MorphWeapons.give(player,profile);
        if(profile.id().equals("warden"))state.senses=new WardenSense(player);
        player.setAirSupply(player.getMaxAirSupply());
    }
    public static void clear(ServerPlayer player,AbilityState state){
        var owner=state==null?player:state.owner();
        var cleanup=new kr.randommorph.fabric.core.Cleanup(error->org.slf4j.LoggerFactory.getLogger("RandomMorph").error("Ability cleanup failed for {}",owner.getUUID(),error));
        cleanup.attempt(()->WitchActions.clear(owner));
        cleanup.attempt(()->{var armor=owner.getAttribute(Attributes.ARMOR);if(armor!=null)armor.removeModifier(SHELL_ARMOR);});
        if(state!=null)cleanup.attempt(state::clear);
    }
    public static void shell(ServerPlayer player,AbilityState state,boolean closed){
        if(state.shell==closed)return;
        state.shell=closed;var armor=player.getAttribute(Attributes.ARMOR);
        if(armor!=null){armor.removeModifier(SHELL_ARMOR);if(closed)armor.addTransientModifier(new AttributeModifier(SHELL_ARMOR,20,AttributeModifier.Operation.ADD_VALUE));}
        if(ShulkerAttachment.shulker(player)){
            player.level().playSound(null,player.blockPosition(),closed?SoundEvents.SHULKER_CLOSE:SoundEvents.SHULKER_OPEN,SoundSource.PLAYERS,1,1);
            player.gameEvent(closed?net.minecraft.world.level.gameevent.GameEvent.CONTAINER_CLOSE:net.minecraft.world.level.gameevent.GameEvent.CONTAINER_OPEN);
        }
    }
    public static boolean use(MorphManager manager,ServerPlayer player,boolean secondary){
        var profile=((MorphAccess)player).randommorph$form();var state=manager.abilityState(player);
        if(profile==null||state==null||!MorphManager.present(player)||player.isSpectator()||player.isPassenger())return false;
        Active action=secondary?profile.secondary():profile.primary();
        if(action==Active.NONE)return false; // This slot keeps vanilla offhand swapping.
        if(!secondary&&MorphWeapons.ranged(profile)){MorphWeapons.give(player,profile);message(player,"전용 무기를 손에 들고 우클릭으로 장전·발사하세요. 화살 아이템은 필요 없습니다.");return true;}
        if(state.cooldown(action)>0){player.sendSystemMessage(Component.literal(action.title()+" | "+((state.cooldown(action)+19)/20)+"초"),true);return true;}
        if(state.fuseTicks>0||state.channelTicks>0||state.witchDrinkTicks>0||state.blazeBurst.active()){player.sendSystemMessage(Component.literal("능력 준비 중"),true);return true;}
        if(activate(manager,player,profile,state,action))state.used(action,manager.config().cooldownTicks(profile,secondary));
        return true;
    }
    private static boolean activate(MorphManager manager,ServerPlayer p,MorphProfile profile,AbilityState s,Active action){
        ServerLevel level=p.level();Vec3 direction=p.getLookAngle();
        switch(action){
            case GRAZE -> {return AnimalActions.graze(p,s);}
            case TELEPORT -> {
                var hit=level.clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(direction.scale(profile.id().equals("endermite")?8:32)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                Vec3 destination=hit.getLocation().subtract(direction.scale(.6));
                if(!teleport(manager,p,profile,destination)){message(p,"안전하게 착지할 곳이 없습니다.");return false;}
            }
            case SHULKER_TELEPORT -> {if(!ShulkerAttachment.teleport(manager,p,s)){message(p,"8블록 이내에 붙을 수 있는 블록 표면을 조준하세요.");return false;}}
            case EXPLODE -> {s.fuseTicks=30;level.playSound(null,p.blockPosition(),SoundEvents.CREEPER_PRIMED,SoundSource.PLAYERS,1,1);}
            case BLAZE_FIRE -> {if(!s.blazeBurst.begin())return false;manager.syncFlags(p);}
            case GHAST_FIRE -> fire(manager,p,new LargeFireball(level,p,direction,1),direction);
            case WITHER_SKULL -> fire(manager,p,new WitherSkull(level,p,direction),direction);
            case WITHER_BLUE_SKULL -> {var skull=new WitherSkull(level,p,direction);skull.setDangerous(true);fire(manager,p,skull,direction);}
            case SUMMON_VEX -> {
                var owner=manager.identity(p);var target=target(p,24,false);
                if(owner instanceof Mob mob)mob.setTarget(target);
                for(int i=0;i<3;i++){
                    var vex=EntityTypes.VEX.create(level,EntitySpawnReason.LOAD);if(vex==null)continue;
                    vex.setPos(p.position().add(i-1,1,1));vex.setBoundOrigin(p.blockPosition());vex.setLimitedLife(20*(30+p.getRandom().nextInt(90)));
                    vex.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
                    if(owner instanceof Mob mob)vex.setOwner(mob);vex.setTarget(target);spawn(manager,p,vex,2400);
                }
            }
            case DRAGON_FIRE -> fire(manager,p,new DragonFireball(level,p,direction),direction);
            case SHULKER_BULLET -> {
                var target=target(p,24,false);if(target==null){message(p,"탄환을 쏠 대상을 조준하세요.");return false;}
                ShulkerAttachment.open(manager,p,s);
                fire(manager,p,new ShulkerBullet(level,p,target,s.shulkerFace.getAxis()),direction);
            }
            case ARROW,POISON_ARROW,SLOW_ARROW,WEAK_ARROW -> {return MorphWeapons.give(p,profile);}
            case TRIDENT -> {var trident=new ThrownTrident(level,p,new ItemStack(Items.TRIDENT));trident.pickup=AbstractArrow.Pickup.DISALLOWED;shoot(manager,p,trident,1.6f);}
            case SNOWBALL -> shoot(manager,p,new Snowball(level,p,new ItemStack(Items.SNOWBALL)),1.6f);
            case SPIT -> {var spit=new LlamaSpit(EntityTypes.LLAMA_SPIT,level);spit.setOwner(p);shoot(manager,p,spit,1.5f);}
            case WIND_CHARGE -> {var wind=new BreezeWindCharge(EntityTypes.BREEZE_WIND_CHARGE,level);wind.setOwner(p);fire(manager,p,wind,direction);}
            case LASER,SONIC_BOOM -> {
                var target=target(p,action==Active.SONIC_BOOM?20:24,action==Active.SONIC_BOOM);
                if(target==null){message(p,"대상을 조준하세요.");return false;}
                if(action==Active.SONIC_BOOM && (Math.abs(target.getY()-p.getY())>20 || horizontalSquared(target.position().subtract(p.position()))>225)){message(p,"소닉 붐 범위를 벗어났습니다.");return false;}
                s.channel=action;s.channelTarget=target.getUUID();s.channelTicks=action==Active.SONIC_BOOM?34:profile.id().equals("elder_guardian")?60:80;
                level.playSound(null,p.blockPosition(),action==Active.SONIC_BOOM?SoundEvents.WARDEN_SONIC_CHARGE:SoundEvents.GUARDIAN_ATTACK,SoundSource.PLAYERS,1,1);
            }
            case FANGS -> fangs(manager,p);
            case SHELL -> {if(profile.id().equals("armadillo")&&p.getFluidHeight(net.minecraft.tags.FluidTags.WATER)>=p.getBbHeight()){message(p,"물에 완전히 잠기면 몸을 말 수 없습니다.");return false;}s.shell=!s.shell;var armor=p.getAttribute(Attributes.ARMOR);if(armor!=null){armor.removeModifier(SHELL_ARMOR);if(s.shell&&profile.id().equals("shulker"))armor.addTransientModifier(new AttributeModifier(SHELL_ARMOR,20,AttributeModifier.Operation.ADD_VALUE));}manager.syncFlags(p);}
            case PUFFER_INFLATE -> {s.inflated=!s.inflated;manager.syncFlags(p);}
            case PLAY_DEAD -> {if(!p.isInWater()){message(p,"물속에서만 죽은 척할 수 있습니다.");return false;}s.playDead=200;s.effect(p,new MobEffectInstance(MobEffects.REGENERATION,200));manager.syncFlags(p);}
            case CAMEL_DASH -> impulse(p,direction.multiply(1,0,1).normalize().scale(1.5).add(0,.25,0));
            case NAUTILUS_DASH -> impulse(p,direction.scale(p.isInWater()?1.2:.5));
            case GOAT_RAM -> {
                var target=target(p,3,false);if(target==null){message(p,"가까운 대상을 조준하세요.");return false;}
                target.hurtServer(level,p.damageSources().playerAttack(p),2);target.push(direction.x*1.5,.3,direction.z*1.5);target.needsSync=true;
            }
            case RAVAGER_ROAR -> {
                level.playSound(null,p.blockPosition(),SoundEvents.RAVAGER_ROAR,SoundSource.PLAYERS,1,1);
                for(var target:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(4),e->e!=p&&e.isAlive()&&!p.isAlliedTo(e))){
                    target.hurtServer(level,p.damageSources().mobAttack(p),6);Vec3 away=target.position().subtract(p.position()).normalize();target.push(away.x*1.5,.4,away.z*1.5);target.needsSync=true;
                }
            }
            case WITCH_HEAL -> {return WitchActions.drink(manager,p,s);}
            case WITCH_POTION -> {
                var potion=WitchActions.randomPotion(p.getRandom());
                shoot(manager,p,new ThrownSplashPotion(level,p,PotionContents.createItemStack(Items.SPLASH_POTION,potion)),.75f);
                level.playSound(null,p.blockPosition(),SoundEvents.WITCH_THROW,SoundSource.PLAYERS,1,1);
            }
            default -> {return false;}
        }
        return true;
    }
    public static void tick(MorphManager manager,ServerPlayer p,MorphProfile profile,AbilityState state,int tick){
        if(!MorphManager.present(p))return;
        state.tick();if(p.isSpectator())return;
        MorphWeapons.audit(p);
        if(profile.id().equals("shulker")){ShulkerAttachment.tick(manager,p,state);if(manager.abilityState(p)!=state)return;}
        AnimalActions.tick(manager,p,profile,state);
        if(!MorphManager.present(p)||manager.abilityState(p)!=state)return;
        if(state.senses!=null)state.senses.tick(manager,tick);
        if(state.shell&&profile.id().equals("armadillo")&&p.getFluidHeight(net.minecraft.tags.FluidTags.WATER)>=p.getBbHeight()){state.shell=false;manager.syncFlags(p);}
        if(profile.has(FLIGHT)&&state.ownsFlight&&MorphFlight.update(p.getAbilities(),false,NativeMovement.flightSpeed(profile),false))p.onUpdateAbilities();
        if(p.is(EntityTypeTags.BURN_IN_DAYLIGHT)&&tick%20==0 && p.level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MONSTERS_BURN,p.position())&&!p.isInWaterOrRain()&&!p.isInPowderSnow&&p.level().canSeeSky(BlockPos.containing(p.getEyePosition()))&&p.getItemBySlot(EquipmentSlot.HEAD).isEmpty())p.igniteForSeconds(8);
        if(WaterPhysiology.dries(profile.id()))WaterPhysiology.tick(p,state,manager.config());
        else if(profile.has(DRY_LAND)&&!AbilityHooks.needsWater(p)){
            if(p.isInWaterOrRain())state.dryTicks=0;else state.dryTicks++;
            if(state.dryTicks>=300&&tick%20==0)p.hurtServer(p.level(),p.damageSources().dryOut(),2);
        }
        if(!p.isAlive()||manager.abilityState(p)!=state)return;
        if(profile.id().equals("wither"))WitherTraits.tick(manager,p,state);
        if(profile.id().equals("witch"))WitchActions.tick(manager,p,state);
        if(profile.id().equals("elder_guardian"))ElderAura.tick(p,state);
        if(profile.id().equals("blaze")&&state.blazeBurst.active()){
            if(state.blazeBurst.tick()){
                var direction=p.getLookAngle();fire(manager,p,new SmallFireball(p.level(),p,direction),direction);
                p.level().levelEvent(null,1018,p.blockPosition(),0);
            }
            if(!state.blazeBurst.active())manager.syncFlags(p);
        }
        if(profile.has(HEAT_WEAK)&&tick%20==0 && p.level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SNOW_GOLEM_MELTS,p.position()))p.hurtServer(p.level(),p.damageSources().onFire(),1);
        if(!p.isAlive()||manager.abilityState(p)!=state)return;
        int regenTicks=profile.id().equals("happy_ghast")&&!p.isInWaterOrRain()?600:20;
        if(profile.has(REGENERATION)&&tick%regenTicks==0 && p.getHealth()<p.getMaxHealth())p.heal(1);
        if(profile.has(DRAGON_PHASE)&&tick%5==0)dragonBreak(manager,p);
        if(profile.id().equals("snow_golem")&&tick%5==0)snowTrail(manager,p);
        if(state.inflated&&profile.id().equals("pufferfish")&&tick%20==0){
            for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(.3),e->e!=p&&e.isAlive()&&!p.isAlliedTo(e))){
                if(target.hurtServer(p.level(),p.damageSources().sting(p),3))target.addEffect(new MobEffectInstance(MobEffects.POISON,120),p);
            }
        }
        if(state.fuseTicks>0 && --state.fuseTicks==0){
            p.level().explode(p,p.getX(),p.getY(),p.getZ(),3,destruction(manager,p)?interaction(manager):Level.ExplosionInteraction.NONE);
            if(manager.config().creeperSuicide&&p.isAlive())p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE);
        }
        if(!p.isAlive()||manager.abilityState(p)!=state)return;
        if(state.channelTicks>0)tickChannel(manager,p,profile,state);
        if(tick%20==0)manager.syncFlags(p);
    }
    private static void tickChannel(MorphManager manager,ServerPlayer p,MorphProfile profile,AbilityState s){
        var entity=p.level().getEntity(s.channelTarget);var target=entity instanceof LivingEntity l?l:null;
        boolean sonic=s.channel==Active.SONIC_BOOM;
        if(target==null||!target.isAlive()||p.isAlliedTo(target)||p.distanceToSqr(target)>(sonic?800:576)||!sonic&&!p.hasLineOfSight(target)){
            s.channelTicks=0;s.channelTarget=null;s.channel=Active.NONE;return;
        }
        if(sonic&&(horizontalSquared(target.position().subtract(p.position()))>225||Math.abs(target.getY()-p.getY())>20)){s.channelTicks=0;return;}
        Vec3 start=p.getEyePosition(),end=target.getEyePosition(),delta=end.subtract(start);
        if(!sonic&&s.channelTicks%4==0)particles(p.level(),start,delta,ParticleTypes.BUBBLE,Math.min(24,(int)delta.length()+1));
        if(--s.channelTicks>0)return;
        if(sonic){
            particles(p.level(),start,delta,ParticleTypes.SONIC_BOOM,(int)delta.length()+7);
            p.level().playSound(null,p.blockPosition(),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.PLAYERS,3,1);
            if(target.hurtServer(p.level(),p.damageSources().sonicBoom(p),10)){
                Vec3 direction=delta.normalize();double resistance=1-target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                target.push(direction.x*2.5*resistance,direction.y*.5*resistance,direction.z*2.5*resistance);target.needsSync=true;
            }
        }else{
            float magic=1+(p.level().getDifficulty()==Difficulty.HARD?2:0)+(profile.id().equals("elder_guardian")?2:0);
            target.hurtServer(p.level(),p.damageSources().indirectMagic(p,p),magic);
            target.hurtServer(p.level(),p.damageSources().mobAttack(p),(float)p.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        s.channelTarget=null;s.channel=Active.NONE;
    }
    public static boolean teleport(MorphManager manager,ServerPlayer p,MorphProfile profile,Vec3 destination){
        BlockPos block=BlockPos.containing(destination);ServerLevel level=p.level();
        if(!level.hasChunkAt(block))return false;
        int y=Math.min(block.getY(),level.getMaxY());
        while(y>level.getMinY()&&!level.getBlockState(new BlockPos(block.getX(),y-1,block.getZ())).isSolid())y--;
        Vec3 candidate=new Vec3(destination.x,y,destination.z);
        if(!manager.fits(p,profile,candidate)||level.getFluidState(BlockPos.containing(candidate)).is(FluidTags.WATER))return false;
        level.playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        p.teleportTo(candidate.x,candidate.y,candidate.z);p.fallDistance=0;
        level.playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);return true;
    }
    public static boolean dodge(MorphManager manager,ServerPlayer p,MorphProfile profile){
        for(int i=0;i<16;i++)if(teleport(manager,p,profile,p.position().add((p.getRandom().nextDouble()-.5)*32,p.getRandom().nextInt(32)-16,(p.getRandom().nextDouble()-.5)*32)))return true;
        return false;
    }
    private static void shoot(MorphManager manager,ServerPlayer p,Projectile projectile,float speed){
        projectile.setOwner(p);projectile.setPos(p.getEyePosition().add(p.getLookAngle().scale(.4)).add(0,-.1,0));
        projectile.shootFromRotation(p,p.getXRot(),p.getYRot(),0,speed,1);spawn(manager,p,projectile,300);
    }
    private static void fire(MorphManager manager,ServerPlayer p,Projectile projectile,Vec3 direction){
        projectile.setOwner(p);projectile.setPos(p.getEyePosition().add(direction.scale(.5)));projectile.setDeltaMovement(direction.scale(.5));spawn(manager,p,projectile,300);
    }
    private static void spawn(MorphManager manager,ServerPlayer p,Entity entity,int lifetime){
        manager.track(p,entity,lifetime);if(!p.level().addFreshEntity(entity))entity.discard();
    }
    private static void fangs(MorphManager manager,ServerPlayer p){
        Vec3 direction=p.getLookAngle().multiply(1,0,1).normalize();
        for(int i=0;i<16;i++){
            var pos=p.position().add(direction.scale((i+1)*1.25));BlockPos floor=BlockPos.containing(pos).above(2);
            for(int n=0;n<7;n++,floor=floor.below()){
                if(!p.level().hasChunkAt(floor))break;
                if(p.level().getBlockState(floor.below()).isFaceSturdy(p.level(),floor.below(),Direction.UP)){
                    var shape=p.level().getBlockState(floor).getCollisionShape(p.level(),floor);double offset=shape.isEmpty()?0:shape.max(Direction.Axis.Y);
                    spawn(manager,p,new EvokerFangs(p.level(),pos.x,floor.getY()+offset,pos.z,(float)Math.atan2(direction.z,direction.x),i,p),100);break;
                }
            }
        }
    }
    public static LivingEntity target(ServerPlayer p,double range,boolean throughWalls){
        Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(range));
        if(!throughWalls)end=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
        LivingEntity best=null;double closest=Double.MAX_VALUE;
        for(var entity:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1),e->e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e))){
            var hit=entity.getBoundingBox().inflate(.3).clip(start,end);
            if(hit.isPresent()){double d=start.distanceToSqr(hit.get());if(d<closest){best=entity;closest=d;}}
        }
        return best;
    }
    public static boolean destruction(MorphManager manager,ServerPlayer p){return manager.config().blockDestruction&&(!manager.config().respectMobGriefing||p.level().getGameRules().get(GameRules.MOB_GRIEFING));}
    private static Level.ExplosionInteraction interaction(MorphManager manager){return manager.config().respectMobGriefing?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.TNT;}
    private static void dragonBreak(MorphManager manager,ServerPlayer p){
        if(!destruction(manager,p))return;
        var box=p.getBoundingBox();boolean changed=false;
        // Native dragon's destructive body/head corridor, not its entire decorative wingspan.
        var center=p.position().add(p.getLookAngle().multiply(1,0,1).scale(2));
        var corridor=new AABB(center.x-2,p.getY(),center.z-2,center.x+2,Math.min(box.maxY,p.getY()+3),center.z+2);
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(corridor.minX,corridor.minY,corridor.minZ),BlockPos.containing(corridor.maxX,corridor.maxY,corridor.maxZ))){
            if(!p.level().hasChunkAt(pos)||!p.level().getWorldBorder().isWithinBounds(pos))continue;
            var block=p.level().getBlockState(pos);
            if(!block.isAir()&&!block.is(BlockTags.DRAGON_TRANSPARENT)&&!block.is(BlockTags.DRAGON_IMMUNE))changed=p.level().removeBlock(pos,false)||changed;
        }
        if(changed)p.level().levelEvent(2008,p.blockPosition(),0);
    }
    private static void snowTrail(MorphManager manager,ServerPlayer p){
        if(!destruction(manager,p)||!p.onGround())return;
        var pos=p.blockPosition();var snow=Blocks.SNOW.defaultBlockState();
        if(p.level().getBlockState(pos).isAir()&&snow.canSurvive(p.level(),pos))p.level().setBlockAndUpdate(pos,snow);
    }
    private static void impulse(ServerPlayer p,Vec3 velocity){p.setDeltaMovement(p.getDeltaMovement().add(velocity));p.needsSync=true;p.connection.send(new ClientboundSetEntityMotionPacket(p));}
    private static void particles(ServerLevel level,Vec3 start,Vec3 delta,net.minecraft.core.particles.ParticleOptions particle,int steps){
        Vec3 direction=delta.normalize();for(int i=1;i<=steps;i++){Vec3 pos=start.add(direction.scale(i));level.sendParticles(particle,pos.x,pos.y,pos.z,1,0,0,0,0);}
    }
    private static double horizontalSquared(Vec3 value){return value.x*value.x+value.z*value.z;}
    private static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text),true);}
}
