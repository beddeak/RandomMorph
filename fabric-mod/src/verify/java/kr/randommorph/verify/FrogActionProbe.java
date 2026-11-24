package kr.randommorph.verify;

import java.util.*;
import kr.randommorph.fabric.*;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.core.MorphFlags;
import kr.randommorph.fabric.morph.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.frog.*;
import net.minecraft.world.entity.monster.cubemob.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;
import static kr.randommorph.verify.SpeciesQueryProbe.*;

/** Merged native frog actions with fixed paths/damage outcomes, not a world or an AI loop. */
public final class FrogActionProbe {
    private static int cases;
    private static void check(boolean ok,String message){cases++;if(!ok)throw new AssertionError(message);}
    public static class Space extends SpeciesQueryProbe.Space {
        private Space(){super();}
        @Override public void playSound(Entity except,Entity source,SoundEvent sound,SoundSource category,float volume,float pitch){}
    }
    public static class Prey extends Person {
        int flags,removals;float health;
        private Prey(){super();}
        @Override public int randommorph$flags(){return flags;}
        @Override public float getHealth(){return health;}
        @Override public void setHealth(float value){health=value;}
        @Override public ItemStack getItemBySlot(EquipmentSlot slot){return ItemStack.EMPTY;}
        @Override public void remove(Entity.RemovalReason reason){removals++;}
    }
    public static class Cube extends Slime {
        int removals;boolean alive=true;float copiedHealth;
        private Cube(){super(null,null);}
        @Override public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> entry){}
        @Override public void copyPosition(Entity original){}
        @Override public void setHealth(float value){copiedHealth=value;}
        @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack){}
        @Override public void setSize(int size,boolean heal){
            getEntityData().set(ID_SIZE,size);
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(size*size);
        }
        @Override public boolean isAlive(){return alive;}
        @Override public void remove(Entity.RemovalReason reason){removals++;}
    }
    public static class TongueFrog extends Frog {
        Entity target,hit;boolean lethal;int hits;Pose pose=Pose.STANDING;
        private TongueFrog(){super(null,null);}
        @Override public Pose getPose(){return pose;}
        @Override public void setPose(Pose next){pose=next;}
        @Override public Optional<Entity> getTongueTarget(){return Optional.ofNullable(target);}
        @Override public void setTongueTarget(Entity next){target=next;}
        @Override public void eraseTongueTarget(){target=null;}
        @Override public boolean doHurtTarget(ServerLevel level,Entity target){
            hit=target;hits++;
            if(lethal){if(target instanceof Prey prey)prey.alive=false;else if(target instanceof Cube cube)cube.alive=false;}
            return true;
        }
    }
    public static class FrogNav extends net.minecraft.world.entity.ai.navigation.GroundPathNavigation {
        boolean reachable=true;Entity requested;
        private FrogNav(){super(null,null);}
        @Override public Path createPath(Entity target,int distance){
            requested=target;return reachable?new Path(List.of(new Node(1,0,0)),new BlockPos(1,0,0),true):null;
        }
    }
    private static Object invoke(ShootTongue action,String method,Class<?>[] types,Object... arguments)throws Exception{
        var call=ShootTongue.class.getDeclaredMethod(method,types);call.setAccessible(true);return call.invoke(action,arguments);
    }
    private static boolean canStart(ShootTongue action,Space level,TongueFrog frog)throws Exception{
        return (boolean)invoke(action,"checkExtraStartConditions",new Class<?>[]{ServerLevel.class,Frog.class},level,frog);
    }
    private static void action(ShootTongue action,String name,Space level,TongueFrog frog)throws Exception{
        invoke(action,name,new Class<?>[]{ServerLevel.class,Frog.class,long.class},level,frog,0L);
    }
    @SuppressWarnings("unchecked") public static void run(String side)throws Exception{
        cases=0;
        var level=NativeEntityFixtures.allocate(Space.class);level.people=new ArrayList<>();level.candidates=new ArrayList<>();
        var server=NativeEntityFixtures.allocate(DedicatedServer.class);var people=NativeEntityFixtures.allocate(DedicatedPlayerList.class);
        field(server,MinecraftServer.class,"playerList",people);level.server=server;
        var online=new HashMap<UUID,net.minecraft.server.level.ServerPlayer>();field(people,PlayerList.class,"playersByUUID",online);
        var config=new MorphConfig();var manager=NativeEntityFixtures.allocate(MorphManager.class);
        field(manager,MorphManager.class,"config",config);
        var identities=new HashMap<UUID,LivingEntity>();field(manager,MorphManager.class,"identities",identities);
        var managersField=RandomMorphMod.class.getDeclaredField("MANAGERS");managersField.setAccessible(true);
        var managers=(Map<MinecraftServer,MorphManager>)managersField.get(null);managers.put(server,manager);
        var tags=net.minecraft.core.Holder.Reference.class.getDeclaredField("tags");tags.setAccessible(true);
        var previousTags=new LinkedHashMap<net.minecraft.core.Holder.Reference<?>,Object>();
        // Native datapack's complete frog_food list is slime and magma_cube; preserve all prior bindings.
        for(var type:List.of(EntityTypes.SLIME,EntityTypes.MAGMA_CUBE,EntityTypes.SULFUR_CUBE,EntityTypes.PLAYER)){
            var holder=type.builtInRegistryHolder();previousTags.put(holder,tags.get(holder));
            tags.set(holder,type==EntityTypes.SLIME||type==EntityTypes.MAGMA_CUBE?Set.of(net.minecraft.tags.EntityTypeTags.FROG_FOOD):Set.of());
        }
        try{
            var owner=at(Prey.class,level,1,0,0);owner.health=1;field(owner,Player.class,"abilities",new Abilities());
            field(owner,LivingEntity.class,"attributes",new net.minecraft.world.entity.ai.attributes.AttributeMap(Player.createAttributes().build()));
            owner.randommorph$form(MorphRegistry.find("slime"));online.put(owner.getUUID(),owner);level.people.add(owner);
            var cube=at(Cube.class,level,1,0,0);cube.alive=true;field(cube,Entity.class,"type",EntityTypes.SLIME);
            field(cube,LivingEntity.class,"attributes",new net.minecraft.world.entity.ai.attributes.AttributeMap(Mob.createMobAttributes().build()));
            cube.setUUID(owner.getUUID());cube.setId(owner.getId());cube.entityTags().add(SpeciesAI.IDENTITY_TAG);identities.put(owner.getUUID(),cube);
            for(var species:List.of("slime","magma_cube","sulfur_cube")){
                var profile=MorphRegistry.find(species);owner.randommorph$form(profile);field(cube,Entity.class,"type",profile.type());
                for(int size:new int[]{1,2,4}){
                    owner.flags=MorphFlags.withCubeSize(0,size);cube.setSize(size,false);
                    boolean edible=size==1&&!species.equals("sulfur_cube");
                    check(Frog.canEat(owner)==edible,"Mapped frog food/size mismatch: "+species+" size="+size);
                    check(Frog.canEat(cube)==edible,"Native cube food/size changed: "+species+" size="+size);
                }
            }
            owner.randommorph$form(null);check(!Frog.canEat(owner),"Ordinary player became frog food");
            owner.randommorph$form(MorphRegistry.find("slime"));field(cube,Entity.class,"type",EntityTypes.SLIME);
            owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(16);
            owner.flags=MorphFlags.withCubeSize(0,4);owner.health=12;cube.setSize(2,false);
            check(manager.identity(owner)==cube&&cube.getMaxHealth()==16&&cube.copiedHealth==12,"First resized identity query copied health before updating native maximum");
            owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);
            var externalId=net.minecraft.resources.Identifier.fromNamespaceAndPath("other_mod","health");
            var external=new net.minecraft.world.entity.ai.attributes.AttributeModifier(externalId,8,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).addTransientModifier(external);
            owner.health=14;
            MorphAttributes.apply(owner,MorphRegistry.find("slime"),cube);
            check(owner.getMaxHealth()==24&&owner.health==12,"Resize did not preserve health ratio with an external health modifier");
            check(owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getModifier(externalId)==external,"Resize erased another owner's modifier");
            manager.identity(owner);
            check(cube.copiedHealth==8,"Identity copied boosted absolute player health rather than native body ratio");
            MorphAttributes.apply(owner,null,null);
            check(owner.getMaxHealth()==28&&owner.health==14&&owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getModifier(externalId)==external,"Human restoration lost ratio or external modifier");
            owner.health=1;
            owner.flags=MorphFlags.withCubeSize(0,2);cube.setSize(2,false);
            config.speciesAi=false;check(Frog.canEat(owner),"Disabled species AI did not retain existing native player/tag path");config.speciesAi=true;
            check(!Frog.canEat(owner),"Re-enabled species AI lost native cube size rejection");

            var frog=NativeEntityFixtures.allocate(TongueFrog.class);frog.pose=Pose.STANDING;
            field(frog,Entity.class,"level",level);field(frog,Entity.class,"position",Vec3.ZERO);
            field(frog,LivingEntity.class,"brain",BrainRelationProbe.brain());
            var nav=NativeEntityFixtures.allocate(FrogNav.class);nav.reachable=true;field(frog,Mob.class,"navigation",nav);
            var action=new ShootTongue(SoundEvents.FROG_TONGUE,SoundEvents.FROG_EAT);
            frog.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,cube);
            check(frog.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow()==owner,"Frog attack memory does not use real player");
            check(!canStart(action,level,frog)&&nav.requested==owner,"Size two prey passed native start or path used detached body");
            owner.flags=MorphFlags.withCubeSize(0,1);cube.setSize(1,false);
            check(canStart(action,level,frog),"Tiny food could not start native tongue action");
            frog.pose=Pose.CROAKING;check(!canStart(action,level,frog),"Croaking exclusion lost");frog.pose=Pose.STANDING;
            nav.reachable=false;check(!canStart(action,level,frog),"Unreachable target accepted");
            check(!frog.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                    &&frog.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseThrow().contains(owner.getUUID())
                    &&frog.getBrain().getTimeUntilExpiry(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS)==100,"Native unreachable target memory/cooldown changed");
            nav.reachable=true;frog.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,owner);
            action(action,"start",level,frog);check(frog.target==owner,"Tongue visual target points at detached body");
            action(action,"tick",level,frog);
            check(owner.getDeltaMovement().equals(new Vec3(-.75,0,0))&&owner.syncVelocity,"Player pull missing native vector or self velocity sync");
            check(frog.hits==0&&frog.pose==Pose.USING_TONGUE,"Tongue timing skipped catch animation");
            for(int tick=0;tick<7;tick++)action(action,"tick",level,frog);
            check(frog.hits==1&&frog.hit==owner&&owner.alive&&owner.removals==0,"Native hit did not reach surviving real player exactly once");
            frog.lethal=true;invoke(action,"eatEntity",new Class<?>[]{ServerLevel.class,Frog.class},level,frog);
            check(!owner.alive&&owner.removals==0,"Lethal tongue removed player outside native respawn lifecycle");
            frog.target=cube;cube.alive=true;invoke(action,"eatEntity",new Class<?>[]{ServerLevel.class,Frog.class},level,frog);
            check(!cube.alive&&cube.removals==1,"Native NPC consumption/removal was suppressed");
            owner.alive=true;action(action,"stop",level,frog);
            check(frog.target==null&&frog.pose==Pose.STANDING&&!frog.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Native tongue stop did not clear pose/targets");
            System.out.println("RANDOMMORPH_FROG_ACTION_OK side="+side+" cases="+cases+"; native food/size/start/tongue timing/pull/stop, fixed navigation/damage outcomes; no world/AI loop/network client");
        }finally{
            managers.remove(server);
            for(var entry:previousTags.entrySet())tags.set(entry.getKey(),entry.getValue());
        }
    }
}
