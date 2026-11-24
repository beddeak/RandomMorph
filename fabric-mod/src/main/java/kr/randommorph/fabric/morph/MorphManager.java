package kr.randommorph.fabric.morph;
import java.util.*;
import kr.randommorph.fabric.MorphConfig;
import kr.randommorph.fabric.ability.*;
import kr.randommorph.fabric.core.MorphSession;
import kr.randommorph.fabric.core.Cleanup;
import kr.randommorph.fabric.network.MorphPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** One manager per logical server. No async calls or scheduled ability callbacks. */
public final class MorphManager {
    private final MinecraftServer server;
    private final Map<UUID,MorphSession> sessions=new HashMap<>();
    private final Map<UUID,AbilityState> abilities=new HashMap<>();
    private final Map<UUID,LivingEntity> identities=new HashMap<>();
    private final Map<UUID,ServerPlayer> owners=new HashMap<>();
    private final Map<UUID,SafePosition> humanSafe=new HashMap<>();
    private final Random random=new Random();
    private MorphConfig config;
    private long revision;
    private int ticks;
    private final kr.randommorph.fabric.core.StartCountdown pendingStart=new kr.randommorph.fabric.core.StartCountdown();
    private record SafePosition(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 position){}
    public MorphManager(MinecraftServer server){
        this.server=server;
        try{config=MorphConfig.load();}catch(Exception error){config=new MorphConfig();LoggerFactory.getLogger("RandomMorph").error("Config rejected; safe defaults used. Fix randommorph.json and /morph reload",error);}
    }
    public MorphConfig config(){return config;}
    public boolean interval(int seconds){
        int previous=config.morphIntervalSeconds;config.morphIntervalSeconds=seconds;
        try{config.save();}catch(Exception failure){config.morphIntervalSeconds=previous;LoggerFactory.getLogger("RandomMorph").error("Interval save failed; unchanged",failure);return false;}
        sessions.values().forEach(s->s.interval(seconds));return true;
    }
    public boolean interval(ServerPlayer player,int seconds){var s=session(player);if(s==null)return fail(player,"먼저 /morph start로 참가하세요.");s.interval(seconds);broadcast(player);return true;}
    public boolean instincts(boolean value){
        boolean previous=config.playerInstincts;config.playerInstincts=value;
        try{config.save();}catch(Exception failure){config.playerInstincts=previous;LoggerFactory.getLogger("RandomMorph").error("Instinct config save failed; unchanged",failure);return false;}
        if(!value)server.getPlayerList().getPlayers().forEach(kr.randommorph.fabric.ai.PlayerInstincts::stop);return true;
    }
    public boolean cooldown(MorphProfile profile,boolean secondary,Integer seconds){
        var next=config.copy();next.mobs.get(profile.id()).cooldown(secondary,seconds);
        return saveCooldowns(next);
    }
    public boolean resetCooldowns(){
        var next=config.copy();next.mobs.values().forEach(rule->{rule.primaryCooldownSeconds=null;rule.secondaryCooldownSeconds=null;});
        return saveCooldowns(next);
    }
    private boolean saveCooldowns(MorphConfig next){
        try{next.save();}catch(Exception failure){LoggerFactory.getLogger("RandomMorph").error("Cooldown save failed; live config and timers unchanged",failure);return false;}
        adoptConfig(next);return true;
    }
    private void adoptConfig(MorphConfig next){
        config=next;
        for(var state:abilities.values()){
            var profile=((MorphAccess)state.owner()).randommorph$form();if(profile==null)continue;
            if(profile.primary()!=Active.NONE)state.cooldownDuration(profile.primary(),next.cooldownTicks(profile,false));
            if(profile.secondary()!=Active.NONE)state.cooldownDuration(profile.secondary(),next.cooldownTicks(profile,true));
            MorphWeapons.syncCooldown(state.owner(),state);
        }
    }
    public boolean reload(){try{var next=MorphConfig.load();adoptConfig(next);return true;}catch(Exception error){LoggerFactory.getLogger("RandomMorph").error("Reload rejected; live config retained",error);return false;}}
    public MorphSession session(ServerPlayer player){return sessions.get(player.getUUID());}
    public AbilityState abilityState(ServerPlayer player){var state=abilities.get(player.getUUID());return state!=null&&state.owner()==player?state:null;}
    public static boolean present(ServerPlayer player){return player.isAlive()&&!player.isRemoved()&&!player.wonGame;}
    public boolean set(ServerPlayer player,MorphProfile profile){
        if(!present(player)||player.isSpectator()||player.isPassenger())return fail(player,"월드에 살아 있는 상태에서 탑승을 해제하세요. 크레딧은 먼저 닫으세요.");
        if(!ServerPlayNetworking.canSend(player,MorphPayload.TYPE))return fail(player,"서버와 같은 RandomMorph Fabric 0.7.0 모드를 설치하세요.");
        if(!fits(player,profile,player.position()))return fail(player,"변신할 몸이 들어갈 공간이 부족합니다. 넓은 곳에서 시도하세요.");
        var previous=session(player);boolean paused=previous!=null&&previous.paused();rememberSafe(player);
        if(!apply(player,profile))return false;
        var session=new MorphSession(profile.id(),previous==null?config.morphIntervalSeconds:previous.intervalSeconds());session.paused(paused);sessions.put(player.getUUID(),session);
        pendingStart.cancel(player.getUUID());changed(player,profile);return true;
    }
    public boolean start(Collection<ServerPlayer> players){
        var targets=new LinkedHashSet<UUID>();
        for(var player:players){
            if(present(player)&&!player.isSpectator()&&!player.isPassenger()&&ServerPlayNetworking.canSend(player,MorphPayload.TYPE))targets.add(player.getUUID());
            else player.sendSystemMessage(Component.literal("시작 불가: 살아 있는 상태로 탑승을 해제하고 같은 버전 모드를 설치하세요."));
        }
        if(!pendingStart.begin(targets))return false;
        for(var player:server.getPlayerList().getPlayers())player.sendSystemMessage(Component.literal("Mod by beddeak(침댁)").withStyle(net.minecraft.ChatFormatting.DARK_GRAY,net.minecraft.ChatFormatting.ITALIC));
        countdownTitle(3);return true;
    }
    private void countdownTitle(int seconds){
        for(var id:pendingStart.players()){
            var player=server.getPlayerList().getPlayer(id);if(player==null)continue;
            player.sendSystemMessage(Component.literal(Integer.toString(seconds)));
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0,20,0));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(Integer.toString(seconds))));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("랜덤 변신 시작")));
        }
    }
    private void tickStart(){
        int previous=pendingStart.seconds();var due=pendingStart.tick();
        if(pendingStart.active()&&previous!=pendingStart.seconds())countdownTitle(pendingStart.seconds());
        for(var id:due){
            var player=server.getPlayerList().getPlayer(id);if(player==null)continue;
            try{if(next(player))pause(player,false);}
            catch(RuntimeException|LinkageError error){
                LoggerFactory.getLogger("RandomMorph").error("Countdown start failed for {}; isolated cleanup",id,error);
                forceClear(player);player.sendSystemMessage(Component.literal("변신 시작 오류: 서버 로그를 확인하세요."));
            }
        }
    }
    public boolean next(ServerPlayer player){
        if(!present(player))return fail(player,"리스폰하거나 엔드 크레딧을 닫은 뒤 시도하세요.");
        var session=session(player);String previous=session==null?null:session.form();
        var profile=config.random(random,previous,candidate->fits(player,candidate,player.position()));
        return profile!=null?set(player,profile):fail(player,"사용 가능한 변신이 이 장소에 들어가지 않습니다. 넓은 곳에서 다시 시도하세요.");
    }
    /** Native conversions (mooshroom shearing) keep the current clock/pause/health ratio. */
    public boolean convert(ServerPlayer player,MorphProfile profile){
        var session=session(player);if(session==null||!present(player)||!fits(player,profile,player.position()))return false;
        if(!apply(player,profile))return false;
        session.setForm(profile.id(),false);changed(player,profile);return true;
    }
    /** Resize only the body: clock, pause, cooldowns and owned resources remain current. */
    public boolean size(ServerPlayer player,int size){
        if(size!=1&&size!=2&&size!=4)return fail(player,"크기는 1, 2, 4 중 하나여야 합니다.");
        var access=(MorphAccess)player;var profile=access.randommorph$form();var state=abilityState(player);var session=session(player);
        if(!present(player)||player.isSpectator()||player.isPassenger()||state==null||session==null||profile==null||!profile.cube())
            return fail(player,"탑승하지 않은 살아 있는 슬라임·마그마 큐브·유황 큐브만 크기를 바꿀 수 있습니다.");
        if(state.cubeSize==size)return true;
        int flags=kr.randommorph.fabric.core.MorphFlags.withCubeSize(state.flags(),size);
        if(!fits(player,profile,player.position(),flags))return fail(player,"해당 크기가 들어갈 공간이 부족합니다.");
        LivingEntity body;
        try{body=MorphBodies.create(profile,player);MorphBodies.flags(body,flags);}
        catch(RuntimeException|LinkageError error){LoggerFactory.getLogger("RandomMorph").error("Cannot construct resized body",error);return fail(player,"크기 변경에 실패했습니다. 서버 로그를 확인하세요.");}
        try{
            var old=identities.get(player.getUUID());
            if(old!=null)kr.randommorph.fabric.ai.SpeciesAI.invalidate(player,old);
            MorphAttributes.apply(player,profile,body);
            state.cubeSize=size;session.cubeSize(size);
            body.addTag(kr.randommorph.fabric.ai.SpeciesAI.IDENTITY_TAG);identities.put(player.getUUID(),body);
            if(old!=null)old.discard();
            syncFlags(player);return true;
        }catch(RuntimeException|LinkageError error){
            LoggerFactory.getLogger("RandomMorph").error("Resize failed; restoring player body",error);
            normalize(player);cleanup(player).attempt(body::discard);broadcast(player);return false;
        }
    }
    public boolean stop(ServerPlayer player){
        pendingStart.cancel(player.getUUID());
        if(!restore(player))return fail(player,"몸을 복구할 공간을 찾지 못했습니다. 넓은 곳에서 다시 시도하세요.");
        ((MorphAccess)player).randommorph$animals().form("");
        sessions.remove(player.getUUID());humanSafe.remove(player.getUUID());broadcast(player);return true;
    }
    private boolean apply(ServerPlayer player,MorphProfile profile){return apply(player,profile,2);}
    private boolean apply(ServerPlayer player,MorphProfile profile,int cubeSize){
        LivingEntity identity=null;
        // Validate the native entity before clearing the old form; it is never spawned.
        try{if(profile!=null){identity=MorphBodies.create(profile,player);MorphBodies.flags(identity,kr.randommorph.fabric.core.MorphFlags.withCubeSize(0,cubeSize));}}
        catch(RuntimeException|LinkageError error){LoggerFactory.getLogger("RandomMorph").error("Cannot construct form {}",profile.id(),error);return fail(player,"해당 몹 생성에 실패했습니다. 서버 로그를 확인하세요.");}
        ShulkerAttachment.Placement attachment=null;
        if(identity instanceof net.minecraft.world.entity.monster.Shulker shulker){
            attachment=ShulkerAttachment.find(player,shulker,player.position(),net.minecraft.core.Direction.DOWN,false);
            if(attachment==null){identity.discard();return fail(player,"셜커가 붙을 블록 면과 껍질이 열릴 공간이 필요합니다.");}
        }
        clearAbilities(player);
        try{
            MorphAttributes.apply(player,profile,identity);
            var access=(MorphAccess)player;access.randommorph$form(profile);access.randommorph$flags(0);player.refreshDimensions();
            if(profile!=null){
                access.randommorph$animals().form(profile.id());
                identity.addTag(kr.randommorph.fabric.ai.SpeciesAI.IDENTITY_TAG);identities.put(player.getUUID(),identity);
                var state=new AbilityState(player);state.cubeSize=cubeSize;abilities.put(player.getUUID(),state);AbilityEngine.apply(player,profile,state);
                if(attachment!=null)ShulkerAttachment.attach(this,player,state,attachment,false);
                access.randommorph$flags(state.flags());MorphBodies.flags(identity,state.flags());player.refreshDimensions();
            }
            return true;
        }catch(RuntimeException|LinkageError error){
            LoggerFactory.getLogger("RandomMorph").error("Morph apply failed; restoring the player",error);
            normalize(player);if(identity!=null){var failedBody=identity;cleanup(player).attempt(failedBody::discard);}broadcast(player);return false;
        }
    }
    private void clearAbilities(ServerPlayer player){
        var state=abilities.remove(player.getUUID());var owner=state==null?player:state.owner();var old=identities.remove(player.getUUID());
        owners.entrySet().removeIf(e->e.getValue().getUUID().equals(player.getUUID()));
        var cleanup=cleanup(player);
        cleanup.attempt(()->AbilityEngine.clear(owner,state));
        cleanup.attempt(()->kr.randommorph.fabric.ai.PlayerInstincts.stop(owner));
        if(old!=null){cleanup.attempt(()->kr.randommorph.fabric.ai.SpeciesAI.invalidate(owner,old));cleanup.attempt(old::discard);}
    }
    private static Cleanup cleanup(ServerPlayer player){return new Cleanup(error->LoggerFactory.getLogger("RandomMorph").error("Player cleanup failed for {}; remaining steps still processed",player.getUUID(),error));}
    private void normalize(ServerPlayer player){
        var cleanup=cleanup(player);
        // Disable trait callbacks before removing effects, discarding entities or restoring attributes.
        var access=(MorphAccess)player;access.randommorph$form(null);access.randommorph$flags(0);
        cleanup.attempt(()->clearAbilities(player));
        cleanup.attempt(()->MorphAttributes.apply(player,null,null));
        cleanup.attempt(()->player.noPhysics=player.isSpectator());
        cleanup.attempt(player::refreshDimensions);
    }
    /** Detach the body and its resources, retaining the UUID session and its clock. Never teleport here. */
    public void suspend(ServerPlayer player){
        var state=abilities.get(player.getUUID());if(state!=null&&state.owner()!=player)return;
        humanSafe.remove(player.getUUID());
        if(((MorphAccess)player).randommorph$form()==null&&state==null&&!identities.containsKey(player.getUUID()))return;
        normalize(player);broadcast(player);
    }
    /** Recover a live avatar only after a human-sized position is available; keep the existing clock. */
    public boolean suspendSafely(ServerPlayer player){
        if(!restore(player))return false;
        broadcast(player);return true;
    }
    public LivingEntity identity(ServerPlayer player){
        var profile=((MorphAccess)player).randommorph$form();if(profile==null)return player;
        var identity=identities.get(player.getUUID());
        if(identity==null||identity.level()!=player.level()||identity.getType()!=profile.type()){if(identity!=null){kr.randommorph.fabric.ai.SpeciesAI.invalidate(player,identity);identity.discard();}identity=MorphBodies.create(profile,player);identity.addTag(kr.randommorph.fabric.ai.SpeciesAI.IDENTITY_TAG);identities.put(player.getUUID(),identity);}
        identity.setUUID(player.getUUID());identity.copyPosition(player);identity.tickCount=player.tickCount;identity.setInvisible(player.isInvisible());identity.setPermanentlyInvulnerable(player.isPermanentlyInvulnerable()||player.getAbilities().invulnerable);
        MorphBodies.flags(identity,((MorphAccess)player).randommorph$flags());identity.setHealth(identity.getMaxHealth()*MorphSession.healthRatio(player.getHealth(),player.getMaxHealth()));
        for(var slot:EquipmentSlot.values())identity.setItemSlot(slot,player.getItemBySlot(slot).copy());
        return identity;
    }
    public void track(ServerPlayer player,Entity entity,int lifetime){
        var state=abilityState(player);if(state==null){entity.discard();return;}
        entity.addTag(AbilityEngine.OWNED_TAG);state.track(entity,lifetime);owners.put(entity.getUUID(),player);
    }
    public ServerPlayer owner(Entity entity){return entity==null?null:owners.get(entity.getUUID());}
    public boolean owns(Entity entity){var owner=owners.get(entity.getUUID());var state=owner==null?null:abilityState(owner);return state!=null&&state.owns(entity);}
    public boolean fits(ServerPlayer player,MorphProfile profile,Vec3 position){return fits(player,profile,position,0);}
    private boolean fits(ServerPlayer player,MorphProfile profile,Vec3 position,int flags){
        if(profile!=null&&profile.id().equals("shulker")){
            var body=MorphBodies.create(profile,player);
            try{return body instanceof net.minecraft.world.entity.monster.Shulker shulker&&ShulkerAttachment.find(player,shulker,position,net.minecraft.core.Direction.DOWN,false)!=null;}finally{body.discard();}
        }
        var dimensions=profile==null?EntityTypes.PLAYER.getDimensions():profile.dimensions(flags);
        var box=dimensions.scale(player.getScale()).makeBoundingBox(position);
        if(box.minY<player.level().getMinY()||box.maxY>player.level().getMaxY()+1||!player.level().getWorldBorder().isWithinBounds(box))return false;
        int x0=net.minecraft.util.Mth.floor(box.minX)>>4,x1=net.minecraft.util.Mth.floor(box.maxX)>>4,z0=net.minecraft.util.Mth.floor(box.minZ)>>4,z1=net.minecraft.util.Mth.floor(box.maxZ)>>4;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(!player.level().hasChunk(x,z))return false;
        if(profile!=null&&profile.phases())return true;
        // Nearby mobs/teammates are normal overlaps, not a reason to reject a transformation.
        if(profile==null)return player.level().noCollision(player,box); // Human restoration still uses safe placement.
        return !player.level().getBlockCollisions(player,box).iterator().hasNext();
    }
    private void rememberSafe(ServerPlayer player){if(present(player)&&fits(player,null,player.position()))humanSafe.put(player.getUUID(),new SafePosition(player.level().dimension(),player.position()));}
    private boolean restore(ServerPlayer player){
        if(!present(player)){normalize(player);return true;}
        if(!fits(player,null,player.position())){
            Vec3 destination=null;var last=humanSafe.get(player.getUUID());
            if(last!=null&&last.dimension.equals(player.level().dimension())&&fits(player,null,last.position))destination=last.position;
            outer:for(int radius=0;destination==null&&radius<=8;radius++)for(int y=-radius;y<=radius;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
                if(Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z))!=radius)continue;
                var candidate=player.position().add(x,y,z);if(fits(player,null,candidate)){destination=candidate;break outer;}
            }
            if(destination==null)return false;player.teleportTo(destination.x,destination.y,destination.z);
        }
        return apply(player,null);
    }
    public void tick(){
        ticks++;tickStart();
        for(var player:server.getPlayerList().getPlayers()){
            var session=session(player);if(session==null)continue;
            try{
                if(!present(player))suspend(player);
                if(session.tick()){
                    var profile=present(player)?config.random(random,session.form(),candidate->fits(player,candidate,player.position())):config.random(random,session.form());
                    if(!present(player))session.setForm(profile.id(),false);
                    else if(profile!=null&&apply(player,profile)){session.setForm(profile.id(),false);changed(player,profile);}
                    else session.retryAfter(5); // Only solid blocks/world safety prevent a roll; never waste a full interval.
                    broadcast(player);
                }
                // Credits keep a session in PlayerList, but that player has no body in a world.
                if(!present(player)){if(ticks%20==0)broadcast(player);continue;}
                var profile=((MorphAccess)player).randommorph$form();var state=abilityState(player);
                if(profile!=null&&state!=null){identity(player);AbilityEngine.tick(this,player,profile,state,ticks);if(present(player))kr.randommorph.fabric.ai.PlayerInstincts.tick(this,player,ticks);}
                // Ability damage can synchronously invoke died()/cleanup. Do not reapply that tick.
                if(!present(player)||session(player)!=session)continue;
                if(ticks%20==0){
                    if(((MorphAccess)player).randommorph$form()==null){var pending=MorphRegistry.find(session.form());if(fits(player,pending,player.position(),kr.randommorph.fabric.core.MorphFlags.withCubeSize(0,session.cubeSize())))apply(player,pending,session.cubeSize());else session.retryAfter(5);}
                    MorphAttributes.environment(player,((MorphAccess)player).randommorph$form());
                    rememberSafe(player);hud(player);broadcast(player);
                }
            }catch(RuntimeException|LinkageError error){
                LoggerFactory.getLogger("RandomMorph").error("Form tick failed for {}; isolated cleanup",player.getUUID(),error);
                forceClear(player);player.sendSystemMessage(Component.literal("변신 오류로 참가를 종료했습니다. 서버 로그를 확인하세요."));
            }
        }
        if(ticks%20==0)owners.entrySet().removeIf(e->{var state=abilityState(e.getValue());return state==null||!state.owns(e.getValue().level().getEntity(e.getKey()));});
    }
    private void hud(ServerPlayer p){
        var session=session(p);if(session==null)return;var profile=MorphRegistry.find(session.form());
        String text=profile.name()+(profile.cube()?" (크기 "+session.cubeSize()+")":"")+" | "+session.remainingSeconds()+"초"+(session.paused()?" | 일시정지":"");var state=abilityState(p);
        if(state!=null){
            if(profile.primary()!=Active.NONE)text+=" | "+(MorphWeapons.ranged(profile)?"우클릭 발사 / F 무기 받기":"F "+profile.primary().title())+cooldown(state,profile.primary());
            if(profile.secondary()!=Active.NONE)text+=" | Shift+F "+profile.secondary().title()+cooldown(state,profile.secondary());
            if(state.channelTicks>0)text+=" | 준비 "+((state.channelTicks+19)/20)+"초";
            if(state.blazeBurst.active())text+=" | 화염구 충전/3연발";
            if(state.grazingTicks>0)text+=" | 풀 먹기 "+((state.grazingTicks+19)/20)+"초";
        }else if(p.isAlive())text+=" | 공간 대기";
        p.sendSystemMessage(Component.literal(text),true);
    }
    private static String cooldown(AbilityState state,Active action){return state.cooldown(action)>0?" "+((state.cooldown(action)+19)/20)+"초":" 준비";}
    private void changed(ServerPlayer player,MorphProfile profile){
        broadcast(player);player.connection.send(new ClientboundSetTitlesAnimationPacket(5,35,10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(profile.id().toUpperCase(Locale.ROOT))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(profile.name()+"으로 변신했습니다!")));
    }
    public void syncFlags(ServerPlayer player){
        var state=abilityState(player);int flags=state==null?0:state.flags();var access=(MorphAccess)player;
        if(access.randommorph$flags()!=flags){access.randommorph$flags(flags);player.refreshDimensions();broadcast(player);}
    }
    public void pause(ServerPlayer player,boolean value){var session=session(player);if(session!=null){session.paused(value);broadcast(player);}}
    public void died(ServerPlayer player){suspend(player);((MorphAccess)player).randommorph$animals().resetSting();broadcast(player);}
    public void respawn(ServerPlayer oldPlayer,ServerPlayer player){
        suspend(oldPlayer);
        var session=session(player);humanSafe.remove(player.getUUID());
        if(session!=null&&present(player)){var profile=MorphRegistry.find(session.form());if(fits(player,profile,player.position(),kr.randommorph.fabric.core.MorphFlags.withCubeSize(0,session.cubeSize())))apply(player,profile,session.cubeSize());else session.retryAfter(5);}
        rememberSafe(player);broadcast(player);sendSnapshot(player);
    }
    public void changedWorld(ServerPlayer player){
        suspend(player);var session=session(player);
        if(session!=null&&present(player)){var profile=MorphRegistry.find(session.form());if(fits(player,profile,player.position(),kr.randommorph.fabric.core.MorphFlags.withCubeSize(0,session.cubeSize())))apply(player,profile,session.cubeSize());else{apply(player,null);session.retryAfter(5);}}
        rememberSafe(player);broadcast(player);sendSnapshot(player);
    }
    private void forceClear(ServerPlayer player){
        var cleanup=cleanup(player);
        try{
            cleanup.attempt(()->{if(!restore(player))LoggerFactory.getLogger("RandomMorph").warn("No safe human-body position for {}; clearing owned state at the current position",player.getUUID());});
            cleanup.attempt(()->normalize(player));
        }
        finally{((MorphAccess)player).randommorph$animals().form("");sessions.remove(player.getUUID());humanSafe.remove(player.getUUID());cleanup.attempt(()->broadcast(player));}
    }
    public void disconnected(ServerPlayer player){pendingStart.cancel(player.getUUID());forceClear(player);}
    public void sendSnapshot(ServerPlayer viewer){for(var player:server.getPlayerList().getPlayers())send(viewer,snapshot(player));}
    private MorphPayload snapshot(ServerPlayer player){
        var access=(MorphAccess)player;var profile=access.randommorph$form();var session=session(player);
        return new MorphPayload(player.getUUID(),profile==null?"":profile.id(),session==null?0:session.remainingTicks(),session!=null&&session.paused(),++revision,access.randommorph$flags());
    }
    public void broadcast(ServerPlayer player){var payload=snapshot(player);for(var viewer:server.getPlayerList().getPlayers())send(viewer,payload);}
    private static void send(ServerPlayer viewer,MorphPayload payload){try{if(ServerPlayNetworking.canSend(viewer,MorphPayload.TYPE))ServerPlayNetworking.send(viewer,payload);}catch(RuntimeException error){LoggerFactory.getLogger("RandomMorph").warn("State send failed for {}; cleanup continues",viewer.getUUID(),error);}}
    public void shutdown(){
        pendingStart.clear();
        var players=new LinkedHashSet<>(server.getPlayerList().getPlayers());
        abilities.values().forEach(state->players.add(state.owner()));
        try{for(var player:players)cleanup(player).attempt(()->forceClear(player));}
        finally{sessions.clear();abilities.clear();identities.clear();owners.clear();humanSafe.clear();}
    }
    private static boolean fail(ServerPlayer player,String message){player.sendSystemMessage(Component.literal(message));return false;}
}
