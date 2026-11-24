package kr.randommorph.fabric.ai;
import java.util.*;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.mixin.*;
import kr.randommorph.fabric.network.InstinctPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
/** Opt-in controller evaluates native target selectors, never runs a detached mob's full AI/actions. */
public final class PlayerInstincts {
    private PlayerInstincts(){}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void tick(MorphManager manager,ServerPlayer player,int tick){
        if(!manager.config().playerInstincts||!manager.config().speciesAi||!MorphManager.present(player)||player.isCreative()||player.isSpectator()||player.isPassenger()){if(tick%10==0)send(player,null,0);return;}
        var identity=manager.identity(player);if(!(identity instanceof Mob mob)){if(tick%10==0)send(player,null,0);return;}
        mob.getSensing().tick();
        // Sensor-only refresh: no Brain behaviors that could attack/spawn/drop items are executed.
        for(var sensor:((BrainSensorsAccess)mob.getBrain()).randommorph$sensors().values())((Sensor)sensor).tick(player.level(),mob);
        ((MobGoalsAccess)mob).randommorph$targets().tick();
        if(tick%10!=0)return;
        for(var wrapped:mob.getGoalSelector().getAvailableGoals().stream().sorted(Comparator.comparingInt(WrappedGoal::getPriority)).toList()){
            if(wrapped.getGoal() instanceof AvoidEntityGoal<?> avoid&&avoid.canUse()){
                var feared=((AvoidTargetAccess)avoid).randommorph$avoided();if(feared!=null){send(player,SpeciesAI.actual(feared),2);return;}
            }
        }
        LivingEntity target=mob.getTarget();
        if(target==null){var memory=mob.getBrain().getMemoryInternal(MemoryModuleType.ATTACK_TARGET);if(memory!=null)target=memory.orElse(null);}
        if(target!=null)target=SpeciesAI.actual(target);
        if(target==null||!target.isAlive()||target.isSpectator()||target==player||player.isAlliedTo(target)||target instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||!player.canHarmPlayer(p))){send(player,null,0);return;}
        send(player,target,1);
        boolean melee=mob.getGoalSelector().getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof MeleeAttackGoal);
        if(melee&&mob.isWithinMeleeAttackRange(target)&&player.hasLineOfSight(target)&&player.getAttackStrengthScale(0)>=1&&player.containerMenu==player.inventoryMenu){
            player.attack(target);player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        }
    }
    public static void stop(ServerPlayer player){send(player,null,0);}
    private static void send(ServerPlayer player,LivingEntity target,int mode){if(ServerPlayNetworking.canSend(player,InstinctPayload.TYPE))ServerPlayNetworking.send(player,new InstinctPayload(target==null?-1:target.getId(),mode));}
}
