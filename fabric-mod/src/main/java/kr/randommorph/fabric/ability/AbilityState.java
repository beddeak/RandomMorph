package kr.randommorph.fabric.ability;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.*;
import net.minecraft.core.Holder;
import kr.randommorph.fabric.core.Cleanup;
import kr.randommorph.fabric.core.Cooldown;
/** All mutable ability state belongs to one current form; clearing never affects another player. */
public final class AbilityState {
    private final Map<Active,Cooldown> cooldowns=new EnumMap<>(Active.class);
    private final Map<Entity,Integer> entities=new IdentityHashMap<>();
    private final Map<Holder<MobEffect>,MobEffectInstance> effects=new HashMap<>();
    public boolean shell, inflated;
    public boolean ownsFlight;
    public int cubeSize=2;
    public WardenSense senses;
    public int playDead, dryTicks, fuseTicks, channelTicks, witchDrinkTicks, witherDestroyTicks;
    public int grazingTicks;
    public net.minecraft.core.BlockPos shulkerCell;
    public net.minecraft.core.Direction shulkerFace=net.minecraft.core.Direction.DOWN;
    public int shulkerOpenTicks,elderAuraTicks;
    public final kr.randommorph.fabric.core.BlazeBurst blazeBurst=new kr.randommorph.fabric.core.BlazeBurst();
    public boolean shulkerEscapePending;
    public net.minecraft.core.BlockPos grazingPosition;
    public UUID channelTarget;
    public Active channel=Active.NONE;
    private final FlightSnapshot flight;
    private final int air;
    private final ServerPlayer owner;
    private boolean cleared;
    public AbilityState(ServerPlayer player){
        owner=player;
        var abilities=player.getAbilities();
        flight=new FlightSnapshot(abilities.mayfly,abilities.flying,abilities.getFlyingSpeed(),player.isCreative(),player.isSpectator());
        air=player.getAirSupply();
    }
    public ServerPlayer owner(){return owner;}
    public int cooldown(Active action){var timer=cooldowns.get(action);return timer==null?0:timer.remaining();}
    public void used(Active action,int ticks){cooldowns.computeIfAbsent(action,a->new Cooldown()).start(ticks);}
    public void cooldownDuration(Active action,int ticks){var timer=cooldowns.get(action);if(timer!=null)timer.duration(ticks);}
    public void track(Entity entity,int lifetime){entities.put(entity,lifetime);}
    public boolean owns(Entity entity){return entities.containsKey(entity);}
    public int flags(){return kr.randommorph.fabric.core.MorphFlags.withCubeSize(kr.randommorph.fabric.core.MorphFlags.withFace(kr.randommorph.fabric.core.MorphFlags.withGrazing((shell?1:0)|(inflated?2:0)|(playDead>0?4:0)|(fuseTicks>0?8:0)|(channelTicks>0||witchDrinkTicks>0||blazeBurst.active()?16:0)|((kr.randommorph.fabric.morph.MorphAccess)owner).randommorph$animals().flags(),grazingTicks),shulkerFace),cubeSize);}
    public void effect(ServerPlayer player,MobEffectInstance effect){
        if(player.hasEffect(effect.getEffect()))return;
        MorphEffects.own(effect);
        if(player.addEffect(effect))effects.put(effect.getEffect(),player.getEffect(effect.getEffect()));
    }
    public void tick(){
        cooldowns.values().forEach(Cooldown::tick);
        var iterator=entities.entrySet().iterator();
        while(iterator.hasNext()){
            var e=iterator.next();
            if(e.getKey().isRemoved()){iterator.remove();continue;}
            int remaining=e.getValue()-1;
            if(remaining<=0){e.getKey().discard();iterator.remove();}else e.setValue(remaining);
        }
        if(playDead>0)playDead--;
    }
    public void clear(){
        if(cleared)return;
        cleared=true;
        var player=owner;
        var cleanup=new Cleanup(error->org.slf4j.LoggerFactory.getLogger("RandomMorph").error("Ability resource cleanup failed for {}; remaining resources still processed",player.getUUID(),error));
        cleanup.attempt(()->MorphWeapons.clear(player));
        var previousSenses=senses;senses=null;
        if(previousSenses!=null)cleanup.attempt(previousSenses::close);
        var previousEntities=List.copyOf(entities.keySet());entities.clear();cooldowns.clear();
        previousEntities.forEach(entity->cleanup.attempt(entity::discard));
        var previousEffects=new HashMap<>(effects);effects.clear();
        previousEffects.forEach((type,effect)->cleanup.attempt(()->{
            if(player.getEffect(type)!=effect)return;
            var retained=MorphEffects.externalRemainder(effect);
            if(retained!=effect){player.removeEffect(type);if(retained!=null)player.addEffect(retained);}
        }));
        if(ownsFlight){
            ownsFlight=false;
            cleanup.attempt(()->{
                var abilities=player.getAbilities();var restored=flight.restored(player.isCreative(),player.isSpectator());
                abilities.mayfly=restored.mayfly();abilities.flying=restored.flying();abilities.setFlyingSpeed(restored.speed());
                player.onUpdateAbilities();
            });
        }
        cleanup.attempt(()->player.setAirSupply(Math.min(air,player.getMaxAirSupply())));
        cleanup.attempt(()->player.noPhysics=player.isSpectator());
        cubeSize=2;witchDrinkTicks=0;witherDestroyTicks=0;dryTicks=0;elderAuraTicks=0;blazeBurst.clear();
        shell=false;inflated=false;playDead=0;fuseTicks=0;channelTicks=0;channelTarget=null;channel=Active.NONE;
        grazingTicks=0;grazingPosition=null;shulkerCell=null;shulkerFace=net.minecraft.core.Direction.DOWN;shulkerOpenTicks=0;shulkerEscapePending=false;
    }
}
