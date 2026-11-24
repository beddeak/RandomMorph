package kr.randommorph.fabric.morph;
import java.util.*;
import kr.randommorph.fabric.ability.*;
import net.minecraft.world.entity.*;
import static kr.randommorph.fabric.ability.Trait.*;
import static kr.randommorph.fabric.ability.Active.*;
/** Explicit audited 26.3 species, including command-only Giant/Illusioner. */
public final class MorphRegistry {
    private static final Map<String,MorphProfile> PROFILES=new LinkedHashMap<>();
    static {
        add("allay","알레이",EntityTypes.ALLAY,NONE,NONE, FALL_IMMUNE, FLIGHT);
        add("armadillo","아르마딜로",EntityTypes.ARMADILLO,SHELL,NONE);
        add("axolotl","아홀로틀",EntityTypes.AXOLOTL,PLAY_DEAD,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("bat","박쥐",EntityTypes.BAT,NONE,NONE, FALL_IMMUNE, FLIGHT);
        add("bee","꿀벌",EntityTypes.BEE,NONE,NONE, FALL_IMMUNE, FLIGHT, POISON_ATTACK);
        add("blaze","블레이즈",EntityTypes.BLAZE,BLAZE_FIRE,NONE, FALL_IMMUNE, FIRE_IMMUNE, FLIGHT, WATER_WEAK);
        add("bogged","보그드",EntityTypes.BOGGED,POISON_ARROW,NONE, SUN_BURN);
        add("breeze","브리즈",EntityTypes.BREEZE,WIND_CHARGE,NONE, FALL_IMMUNE, HIGH_JUMP);
        add("camel","낙타",EntityTypes.CAMEL,CAMEL_DASH,NONE, HIGH_JUMP);
        add("camel_husk","낙타 허스크",EntityTypes.CAMEL_HUSK,CAMEL_DASH,NONE, HIGH_JUMP);
        add("cat","고양이",EntityTypes.CAT,NONE,NONE, FALL_IMMUNE);
        add("cave_spider","동굴 거미",EntityTypes.CAVE_SPIDER,NONE,NONE, POISON_ATTACK, WALL_CLIMB);
        add("chicken","닭",EntityTypes.CHICKEN,NONE,NONE, SLOW_FALL);
        add("cod","대구",EntityTypes.COD,NONE,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("copper_golem","구리 골렘",EntityTypes.COPPER_GOLEM,NONE,NONE);
        add("cow","소",EntityTypes.COW,NONE,NONE);
        add("creaking","크리킹",EntityTypes.CREAKING,NONE,NONE, CREAKING_WATCH);
        add("creeper","크리퍼",EntityTypes.CREEPER,EXPLODE,NONE);
        add("dolphin","돌고래",EntityTypes.DOLPHIN,NONE,NONE, AQUATIC, DRY_LAND);
        add("donkey","당나귀",EntityTypes.DONKEY,NONE,NONE, HIGH_JUMP);
        add("drowned","드라운드",EntityTypes.DROWNED,TRIDENT,NONE, AQUATIC, SUN_BURN, WATER_BREATHING);
        add("elder_guardian","엘더 가디언",EntityTypes.ELDER_GUARDIAN,LASER,NONE, AQUATIC, THORNS, WATER_BREATHING);
        add("ender_dragon","엔더 드래곤",EntityTypes.ENDER_DRAGON,DRAGON_FIRE,NONE, DRAGON_PHASE, FALL_IMMUNE, FIRE_IMMUNE, FLIGHT, LAUNCH_ATTACK);
        add("enderman","엔더맨",EntityTypes.ENDERMAN,TELEPORT,NONE, WATER_WEAK);
        add("endermite","엔더마이트",EntityTypes.ENDERMITE,TELEPORT,NONE);
        add("evoker","소환사",EntityTypes.EVOKER,FANGS,SUMMON_VEX);
        add("fox","여우",EntityTypes.FOX,NONE,NONE, HIGH_JUMP);
        add("frog","개구리",EntityTypes.FROG,NONE,NONE, HIGH_JUMP);
        add("ghast","가스트",EntityTypes.GHAST,GHAST_FIRE,NONE, FALL_IMMUNE, FIRE_IMMUNE, FLIGHT);
        add("giant","자이언트",EntityTypes.GIANT,NONE,NONE);
        add("glow_squid","발광 오징어",EntityTypes.GLOW_SQUID,NONE,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("goat","염소",EntityTypes.GOAT,GOAT_RAM,NONE, HIGH_JUMP);
        add("guardian","가디언",EntityTypes.GUARDIAN,LASER,NONE, AQUATIC, THORNS, WATER_BREATHING);
        add("happy_ghast","행복한 가스트",EntityTypes.HAPPY_GHAST,NONE,NONE, FALL_IMMUNE, FLIGHT, REGENERATION);
        add("hoglin","호글린",EntityTypes.HOGLIN,NONE,NONE, LAUNCH_ATTACK);
        add("horse","말",EntityTypes.HORSE,NONE,NONE, HIGH_JUMP);
        add("husk","허스크",EntityTypes.HUSK,NONE,NONE, HUNGER_ATTACK);
        add("illusioner","환술사",EntityTypes.ILLUSIONER,ARROW,NONE);
        add("iron_golem","철 골렘",EntityTypes.IRON_GOLEM,NONE,NONE, FALL_IMMUNE, LAUNCH_ATTACK);
        add("llama","라마",EntityTypes.LLAMA,SPIT,NONE);
        add("magma_cube","마그마 큐브",EntityTypes.MAGMA_CUBE,NONE,NONE, FALL_IMMUNE, FIRE_IMMUNE, HIGH_JUMP);
        add("mooshroom","무시룸",EntityTypes.MOOSHROOM,NONE,NONE);
        add("mule","노새",EntityTypes.MULE,NONE,NONE, HIGH_JUMP);
        add("nautilus","노틸러스",EntityTypes.NAUTILUS,NAUTILUS_DASH,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("ocelot","오실롯",EntityTypes.OCELOT,NONE,NONE, FALL_IMMUNE);
        add("panda","판다",EntityTypes.PANDA,NONE,NONE);
        add("parched","파치드",EntityTypes.PARCHED,WEAK_ARROW,NONE);
        add("parrot","앵무새",EntityTypes.PARROT,NONE,NONE, FALL_IMMUNE, FLIGHT);
        add("phantom","팬텀",EntityTypes.PHANTOM,NONE,NONE, FALL_IMMUNE, FLIGHT, SUN_BURN);
        add("pig","돼지",EntityTypes.PIG,NONE,NONE);
        add("piglin","피글린",EntityTypes.PIGLIN,NONE,NONE);
        add("piglin_brute","피글린 야수",EntityTypes.PIGLIN_BRUTE,NONE,NONE);
        add("pillager","약탈자",EntityTypes.PILLAGER,ARROW,NONE);
        add("polar_bear","북극곰",EntityTypes.POLAR_BEAR,NONE,NONE);
        add("pufferfish","복어",EntityTypes.PUFFERFISH,PUFFER_INFLATE,NONE, AQUATIC, DRY_LAND, POISON_ATTACK, WATER_BREATHING);
        add("rabbit","토끼",EntityTypes.RABBIT,NONE,NONE, HIGH_JUMP);
        add("ravager","파괴수",EntityTypes.RAVAGER,RAVAGER_ROAR,NONE, LAUNCH_ATTACK);
        add("salmon","연어",EntityTypes.SALMON,NONE,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("sheep","양",EntityTypes.SHEEP,GRAZE,NONE);
        add("shulker","셜커",EntityTypes.SHULKER,SHULKER_BULLET,SHULKER_TELEPORT, FALL_IMMUNE);
        add("silverfish","좀벌레",EntityTypes.SILVERFISH,NONE,NONE);
        add("skeleton","스켈레톤",EntityTypes.SKELETON,ARROW,NONE, SUN_BURN);
        add("skeleton_horse","스켈레톤 말",EntityTypes.SKELETON_HORSE,NONE,NONE, AQUATIC, HIGH_JUMP, WATER_BREATHING);
        add("slime","슬라임",EntityTypes.SLIME,NONE,NONE, FALL_IMMUNE, HIGH_JUMP);
        add("sniffer","스니퍼",EntityTypes.SNIFFER,NONE,NONE);
        add("snow_golem","눈 골렘",EntityTypes.SNOW_GOLEM,SNOWBALL,NONE, FALL_IMMUNE, HEAT_WEAK, WATER_WEAK);
        add("spider","거미",EntityTypes.SPIDER,NONE,NONE, WALL_CLIMB);
        add("squid","오징어",EntityTypes.SQUID,NONE,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("stray","스트레이",EntityTypes.STRAY,SLOW_ARROW,NONE, SUN_BURN);
        add("strider","스트라이더",EntityTypes.STRIDER,NONE,NONE, COLD_STRIDER, FIRE_IMMUNE, LAVA_WALK, WATER_WEAK);
        add("sulfur_cube","유황 큐브",EntityTypes.SULFUR_CUBE,NONE,NONE, WATER_BREATHING);
        add("tadpole","올챙이",EntityTypes.TADPOLE,NONE,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("trader_llama","상인 라마",EntityTypes.TRADER_LLAMA,SPIT,NONE);
        add("tropical_fish","열대어",EntityTypes.TROPICAL_FISH,NONE,NONE, AQUATIC, DRY_LAND, WATER_BREATHING);
        add("turtle","거북",EntityTypes.TURTLE,NONE,NONE, AQUATIC, WATER_BREATHING);
        add("vex","벡스",EntityTypes.VEX,NONE,NONE, FALL_IMMUNE, FIRE_IMMUNE, FLIGHT, VEX_PHASE);
        add("villager","주민",EntityTypes.VILLAGER,NONE,NONE);
        add("vindicator","변명자",EntityTypes.VINDICATOR,NONE,NONE);
        add("wandering_trader","떠돌이 상인",EntityTypes.WANDERING_TRADER,NONE,NONE);
        add("warden","워든",EntityTypes.WARDEN,SONIC_BOOM,NONE, FIRE_IMMUNE);
        add("witch","마녀",EntityTypes.WITCH,WITCH_POTION,WITCH_HEAL, MAGIC_RESISTANCE);
        add("wither","위더",EntityTypes.WITHER,WITHER_SKULL,WITHER_BLUE_SKULL, FALL_IMMUNE, FIRE_IMMUNE, FLIGHT, REGENERATION, WITHER_ARMOR);
        add("wither_skeleton","위더 스켈레톤",EntityTypes.WITHER_SKELETON,NONE,NONE, FIRE_IMMUNE, SUN_BURN, WITHER_ATTACK);
        add("wolf","늑대",EntityTypes.WOLF,NONE,NONE);
        add("zoglin","조글린",EntityTypes.ZOGLIN,NONE,NONE, LAUNCH_ATTACK);
        add("zombie","좀비",EntityTypes.ZOMBIE,NONE,NONE, SUN_BURN);
        add("zombie_horse","좀비 말",EntityTypes.ZOMBIE_HORSE,NONE,NONE, HIGH_JUMP, SUN_BURN);
        add("zombie_nautilus","좀비 노틸러스",EntityTypes.ZOMBIE_NAUTILUS,NAUTILUS_DASH,NONE, AQUATIC, DRY_LAND, SUN_BURN, WATER_BREATHING);
        add("zombie_villager","좀비 주민",EntityTypes.ZOMBIE_VILLAGER,NONE,NONE, SUN_BURN);
        add("zombified_piglin","좀비화 피글린",EntityTypes.ZOMBIFIED_PIGLIN,NONE,NONE, FIRE_IMMUNE);
    }
    private MorphRegistry() {}
    private static void add(String id,String name,EntityType<? extends LivingEntity> type,Active primary,Active secondary,Trait... traits){
        var profile=new MorphProfile(id,name,type,Set.of(traits),primary,secondary);
        if(PROFILES.putIfAbsent(id,profile)!=null)throw new IllegalStateException("Duplicate morph: "+id);
    }
    public static MorphProfile find(String id){return id==null?null:PROFILES.get(id.toLowerCase(Locale.ROOT).replace("minecraft:",""));}
    public static Collection<MorphProfile> profiles(){return Collections.unmodifiableCollection(PROFILES.values());}
    public static MorphProfile random(Random random,String previous){
        var list=profiles().stream().filter(p->!p.id().equals(previous)).toList();return list.get(random.nextInt(list.size()));
    }
}
