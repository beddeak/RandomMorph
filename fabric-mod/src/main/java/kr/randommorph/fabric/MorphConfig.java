package kr.randommorph.fabric;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.core.WeightedPool;
import kr.randommorph.fabric.ability.Active;
import net.fabricmc.loader.api.FabricLoader;
/** Server-authoritative config; reload validates completely before replacing the live settings. */
public final class MorphConfig {
    public int morphIntervalSeconds=60;
    public int dolphinDryOutSeconds=15;
    public int axolotlDryOutSeconds=15;
    public boolean creeperSuicide=true;
    public boolean blockDestruction=true;
    public boolean respectMobGriefing=true;
    public boolean speciesAi=true;
    public boolean playerInstincts=false;
    public boolean blazeImpactExplosion=false; // Vanilla small fireballs ignite, they do not explode.
    public double blazeExplosionPower=0.5;
    public Map<String,Mob> mobs=new LinkedHashMap<>();
    public static final class Mob {
        public boolean enabled=true;public double weight=1;
        public Integer primaryCooldownSeconds,secondaryCooldownSeconds; // Omitted/null uses the ability default.
        public Mob(){}public Mob(double weight){this.weight=weight;}
        public Integer cooldown(boolean secondary){return secondary?secondaryCooldownSeconds:primaryCooldownSeconds;}
        public void cooldown(boolean secondary,Integer seconds){if(secondary)secondaryCooldownSeconds=seconds;else primaryCooldownSeconds=seconds;}
    }
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public MorphConfig(){for(var p:MorphRegistry.profiles())mobs.put(p.id(),new Mob(p.boss()?(p.id().equals("warden")?.25:.1):1));}
    public static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("randommorph.json");}
    public static MorphConfig load() throws IOException {return load(path());}
    public static MorphConfig load(Path path) throws IOException {
        path=path.toAbsolutePath();
        if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,JSON.toJson(new MorphConfig())+"\n");}
        MorphConfig config=JSON.fromJson(Files.readString(path),MorphConfig.class);
        if(config==null)throw new IllegalArgumentException("Empty config");config.validate();return config;
    }
    public void save() throws IOException {save(path());}
    public void save(Path path) throws IOException {
        validate();path=path.toAbsolutePath();Files.createDirectories(path.getParent());var temp=Files.createTempFile(path.getParent(),"randommorph-",".json.tmp");
        try{Files.writeString(temp,JSON.toJson(this)+"\n");
            try{Files.move(temp,path,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException unsupported){Files.move(temp,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    public void validate(){
        if(dolphinDryOutSeconds<1||dolphinDryOutSeconds>600||axolotlDryOutSeconds<1||axolotlDryOutSeconds>600)throw new IllegalArgumentException("dryOutSeconds: 1..600");
        if(morphIntervalSeconds<5||morphIntervalSeconds>3600)throw new IllegalArgumentException("morphIntervalSeconds: 5..3600");
        if(!Double.isFinite(blazeExplosionPower)||blazeExplosionPower<0||blazeExplosionPower>4)throw new IllegalArgumentException("blazeExplosionPower: 0..4");
        if(mobs==null)throw new IllegalArgumentException("Missing mobs");
        var normalized=new LinkedHashMap<String,Mob>();
        for(var entry:mobs.entrySet()){
            var p=MorphRegistry.find(entry.getKey());if(p==null)throw new IllegalArgumentException("Unknown mob: "+entry.getKey());
            var rule=entry.getValue();if(rule==null||!Double.isFinite(rule.weight)||rule.weight<0)throw new IllegalArgumentException("Invalid weight: "+entry.getKey());
            checkCooldown(p,rule,false);checkCooldown(p,rule,true);
            if(normalized.putIfAbsent(p.id(),rule)!=null)throw new IllegalArgumentException("Duplicate mob: "+entry.getKey());
        }
        for(var p:MorphRegistry.profiles())normalized.putIfAbsent(p.id(),new Mob(p.boss()?(p.id().equals("warden")?.25:.1):1));
        if(normalized.values().stream().noneMatch(m->m.enabled&&m.weight>0))throw new IllegalArgumentException("At least one random mob must be enabled");
        mobs=normalized;
    }
    private static void checkCooldown(MorphProfile profile,Mob rule,boolean secondary){
        var seconds=rule.cooldown(secondary);
        if(seconds==null)return;
        if(seconds<0||seconds>3600)throw new IllegalArgumentException(profile.id()+" cooldown: 0..3600 seconds");
        if((secondary?profile.secondary():profile.primary())==Active.NONE)throw new IllegalArgumentException(profile.id()+": no "+(secondary?"secondary":"primary")+" ability");
    }
    public int cooldownTicks(MorphProfile profile,boolean secondary){
        var action=secondary?profile.secondary():profile.primary();if(action==Active.NONE)return 0;
        var rule=mobs.get(profile.id());var seconds=rule==null?null:rule.cooldown(secondary);
        return seconds==null?action.cooldownTicks():seconds*20;
    }
    /** Stage edits independently so a failed save cannot alter the live Mob objects. */
    public MorphConfig copy(){return JSON.fromJson(JSON.toJson(this),MorphConfig.class);}
    public MorphProfile random(Random random,String previous){
        return random(random,previous,profile->true);
    }
    /** A constrained roll tries each enabled form once before reporting that no body fits. */
    public MorphProfile random(Random random,String previous,java.util.function.Predicate<MorphProfile> accepted){
        var weights=new LinkedHashMap<String,Double>();
        mobs.forEach((id,rule)->{if(rule.enabled)weights.put(id,rule.weight);});
        var id=WeightedPool.chooseMatching(random,weights,previous,key->accepted.test(MorphRegistry.find(key)));
        return id==null?null:MorphRegistry.find(id);
    }
}
