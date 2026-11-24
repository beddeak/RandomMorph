package kr.randommorph.fabric.ability;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.network.SensePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
/** Real vanilla vibration propagation: sneaking exceptions, wool occlusion, range and travel time. */
public final class WardenSense implements VibrationSystem,AutoCloseable {
    private final ServerPlayer player;private final ServerLevel level;
    private final Data data=new Data();private final User user;
    private final DynamicGameEventListener<Listener> listener;
    private long lastVibration=Long.MIN_VALUE/2;private boolean closed;
    public WardenSense(ServerPlayer player){this.player=player;this.level=player.level();user=new Ear();listener=new DynamicGameEventListener<>(new Listener(this));listener.add(level);}
    @Override public Data getVibrationData(){return data;}
    @Override public User getVibrationUser(){return user;}
    public void tick(MorphManager manager,int tick){
        if(closed||!MorphManager.present(player)||player.level()!=level)return;
        listener.move(level);Ticker.tick(level,data,user);
        if(tick%100==0&&manager.identity(player) instanceof Warden warden){
            LivingEntity nearest=null;double distance=Double.MAX_VALUE;
            for(var e:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(6,20,6),e->e!=player&&e.isAlive()&&!e.isSpectator())){
                var d=e.position().subtract(player.position());
                if(d.horizontalDistanceSqr()>36||Math.abs(d.y)>20||e instanceof net.minecraft.world.entity.player.Player p&&p.isCreative()||!warden.canTargetEntity(SpeciesAI.identity(e)))continue;
                double dist=player.distanceToSqr(e);if(dist<distance){distance=dist;nearest=e;}
            }
            if(nearest!=null)signal(nearest.blockPosition(),nearest,true);
        }
    }
    private void signal(BlockPos pos,Entity source,boolean smell){
        if(closed||!MorphManager.present(player)||!ServerPlayNetworking.canSend(player,SensePayload.TYPE))return;
        ServerPlayNetworking.send(player,new SensePayload(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,source==null?-1:source.getId(),smell));
        player.level().playSound(null,player.blockPosition(),smell?net.minecraft.sounds.SoundEvents.WARDEN_SNIFF:net.minecraft.sounds.SoundEvents.WARDEN_TENDRIL_CLICKS,net.minecraft.sounds.SoundSource.PLAYERS,.6f,1);
    }
    @Override public void close(){if(!closed){closed=true;listener.remove(level);}}
    private final class Ear implements User {
        private final PositionSource position=new EntityPositionSource(player,player.getEyeHeight());
        @Override public int getListenerRadius(){return 16;}
        @Override public PositionSource getPositionSource(){return position;}
        @Override public TagKey<GameEvent> getListenableEvents(){return GameEventTags.WARDEN_CAN_LISTEN;}
        @Override public boolean canTriggerAvoidVibration(){return true;}
        @Override public boolean canReceiveVibration(ServerLevel level,BlockPos pos,Holder<GameEvent> event,GameEvent.Context context){
            if(closed||!MorphManager.present(player)||player.isSpectator()||player.level()!=level||level.getGameTime()-lastVibration<40||!level.getWorldBorder().isWithinBounds(pos))return false;
            Entity source=context.sourceEntity();
            if(source==player||source!=null&&source.getUUID().equals(player.getUUID()))return false;
            if(source instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()))return false;
            var manager=kr.randommorph.fabric.RandomMorphMod.manager(level.getServer());
            return !(source instanceof LivingEntity living)||manager.identity(player) instanceof Warden w&&w.canTargetEntity(SpeciesAI.identity(living));
        }
        @Override public void onReceiveVibration(ServerLevel level,BlockPos pos,Holder<GameEvent> event,Entity source,Entity owner,float distance){
            lastVibration=level.getGameTime();signal(pos,source instanceof LivingEntity?source:null,false);
        }
    }
}
