package kr.randommorph.verify;
import java.util.*;
import java.util.function.Predicate;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.core.AnimalState;
import kr.randommorph.fabric.morph.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.PlayerTeam;
/** Real query/conditions/avoid-stop execution with fixed candidates; no world or AI tick is started. */
public final class SpeciesQueryProbe {
    private static int cases;
    private static int nextEntityId=1000;
    public static class Space extends ServerLevel {
        List<ServerPlayer> people;List<Entity> candidates;int entityQueries;
        net.minecraft.server.MinecraftServer server;
        protected Space(){super(null,null,null,null,null,null,false,0,List.of(),false);}
        @Override public List<ServerPlayer> players(){return people;}
        @Override public net.minecraft.server.MinecraftServer getServer(){return server;}
        @Override public Entity getEntityInAnyDimension(UUID uuid){
            return java.util.stream.Stream.concat(people.stream(),candidates.stream())
                    .filter(e->!e.isRemoved()&&e.getUUID().equals(uuid)).findFirst().orElse(null);
        }
        @Override public Iterable<Entity> getAllEntities(){return candidates;}
        @Override public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type,AABB area,Predicate<? super T> filter){
            entityQueries++;return candidates.stream().filter(type::isInstance).map(type::cast)
                    .filter(e->area.intersects(e.getBoundingBox())&&filter.test(e)).toList();
        }
    }
    public static class Person extends ServerPlayer implements MorphAccess {
        boolean alive=true,creative,spectator;
        MorphProfile profile;
        protected Person(){super(null,null,null,null);}
        public MorphProfile randommorph$form(){return profile;}
        public void randommorph$form(MorphProfile form){
            profile=form;
            try{field(this,Player.class,"randommorph$form",form);}catch(Exception error){throw new AssertionError(error);}
        }
        public int randommorph$flags(){return 0;}
        public void randommorph$flags(int flags){}
        public AnimalState randommorph$animals(){return null;}
        @Override public boolean isAlive(){return alive;}
        @Override public boolean isCreative(){return creative;}
        @Override public boolean isSpectator(){return spectator;}
        @Override public PlayerTeam getTeam(){return null;}
        @Override public boolean canBeSeenByAnyone(){return true;}
        @Override public double getVisibilityPercent(ServerLevel level,Entity source){return 1;}
    }
    public static class Hunter extends Skeleton {
        private Hunter(){super(null,null);}
        @Override public PlayerTeam getTeam(){return null;}
        @Override public boolean isAlive(){return true;}
        @Override public boolean canBeSeenByAnyone(){return true;}
        @Override public boolean canAttack(LivingEntity target){return target!=this;}
        @Override public double getEyeY(){return getY()+1.62;}
        @Override public double getVisibilityPercent(ServerLevel level,Entity source){return 1;}
    }
    public static class Nav extends GroundPathNavigation {
        int stops;private Nav(){super(null,null);}
        @Override public void stop(){stops++;}
    }
    static void field(Object object,Class<?> type,String name,Object value)throws Exception{
        var field=type.getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    static <T extends LivingEntity>T at(Class<T> type,Space level,double x,double y,double z)throws Exception{
        var entity=NativeEntityFixtures.allocate(type);NativeEntityFixtures.initializeData(entity);
        field(entity,Entity.class,"level",level);field(entity,Entity.class,"position",new Vec3(x,y,z));
        field(entity,Entity.class,"bb",new AABB(x-.3,y,z-.3,x+.3,y+1.8,z+.3));
        field(entity,Entity.class,"tags",new HashSet<String>());entity.setUUID(UUID.randomUUID());
        entity.setId(++nextEntityId);
        if(entity instanceof Person person){person.alive=true;field(entity,Entity.class,"type",EntityTypes.PLAYER);}
        else if(entity instanceof Hunter)field(entity,Entity.class,"type",EntityTypes.SKELETON);
        return entity;
    }
    private static TargetingConditions conditions(double range){return TargetingConditions.forCombat().range(range).ignoreLineOfSight();}
    private static void check(boolean success,String message){cases++;if(!success)throw new AssertionError(message);}
    public static void run(String side)throws Exception{
        cases=0;
        var level=NativeEntityFixtures.allocate(Space.class);level.people=new ArrayList<>();level.candidates=new ArrayList<>();
        var hunter=at(Hunter.class,level,0,0,0);var high=at(Person.class,level,1,8,0);level.people.add(high);level.candidates.add(high);
        var oldArea=hunter.getBoundingBox().inflate(16,4,16);
        check(SpeciesAI.nearest(level,hunter,Player.class,oldArea,conditions(16))==null,"Original four-block query regression not reproduced");
        int queries=level.entityQueries;
        check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==high,"Higher player lost");
        check(SpeciesAI.nearestPlayer(level,hunter,ServerPlayer.class,conditions(16))==high,"ServerPlayer branch lost");
        check(level.entityQueries==queries,"Human goal must not query a short vertical box");
        check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(4))==null,"Native follow range ignored");
        check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(0))==high,"Native unbounded conditions lost");
        var feet=at(Person.class,level,.4,0,0);var eyes=at(Person.class,level,.5,1.62,0);level.people=List.of(feet,eyes);
        check(hunter.distanceToSqr(feet)<hunter.distanceToSqr(eyes),"Ranking regression fixture invalid");
        check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==eyes,"Nearest ranking must use eye origin");
        var upper=at(Hunter.class,level,.5,1.62,0);var lower=at(Hunter.class,level,.4,0,0);level.people=List.of();level.candidates=List.of(lower,upper);
        check(SpeciesAI.nearest(level,hunter,Skeleton.class,oldArea,conditions(16))==upper,"Typed selection eye ranking wrong");
        check(SpeciesAI.nearest(level,hunter,Skeleton.class,new AABB(-1,-1,-1,1,1,1),conditions(16))==lower,"Typed native area was expanded");
        level.people=List.of(eyes);
        eyes.creative=true;check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==null,"Creative target acquired");eyes.creative=false;
        eyes.spectator=true;check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==null,"Spectator target acquired");eyes.spectator=false;
        eyes.alive=false;check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==null,"Dead target acquired");eyes.alive=true;
        check(SpeciesAI.refreshAvoided(level,eyes,"",Player.class)==eyes,"Human-only fear must remain native");
        check(SpeciesAI.refreshAvoided(level,eyes,"wolf",Player.class)==null,"Changed fear form accepted");
        check(SpeciesAI.refreshAvoided(level,upper,"wolf",Skeleton.class)==null,"NPC mistaken for a borrowed player identity");
        field(eyes,Entity.class,"level",NativeEntityFixtures.allocate(Space.class));
        check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==null,"Cross-world target acquired");field(eyes,Entity.class,"level",level);
        field(eyes,Entity.class,"removalReason",Entity.RemovalReason.DISCARDED);
        check(SpeciesAI.nearestPlayer(level,hunter,Player.class,conditions(16))==null,"Removed target acquired");
        var nav=NativeEntityFixtures.allocate(Nav.class);var goal=NativeEntityFixtures.allocate(AvoidEntityGoal.class);
        field(goal,AvoidEntityGoal.class,"pathNav",nav);field(goal,AvoidEntityGoal.class,"toAvoid",upper);
        field(goal,AvoidEntityGoal.class,"randommorph$avoidedForm","wolf");goal.stop();
        check(nav.stops==1,"Borrowed fear path survived stop");goal.stop();check(nav.stops==1,"Repeated cleanup stopped navigation again");
        field(goal,AvoidEntityGoal.class,"toAvoid",upper);goal.stop();check(nav.stops==1,"Native NPC-only fear path was altered");
        field(goal,AvoidEntityGoal.class,"randommorph$avoidedForm","");goal.stop();check(nav.stops==1,"Normal human-only fear path was altered");
        field(goal,AvoidEntityGoal.class,"randommorph$avoidedForm","");field(goal,AvoidEntityGoal.class,"randommorph$cancelFearPath",true);
        goal.stop();check(nav.stops==2,"Human-to-mob transition did not clear canceled fear path");
        System.out.println("RANDOMMORPH_SPECIES_QUERY_OK side="+side+" cases="+cases+"; actual query/conditions/stop, fixed candidates, no world/AI tick started");
    }
}
