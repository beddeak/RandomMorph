package kr.randommorph.fabric.ability;
import kr.randommorph.fabric.MorphConfig;
import kr.randommorph.fabric.RandomMorphMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
/** Moisture is distinct from dolphin lung air and from fish gills. */
public final class WaterPhysiology {
    private WaterPhysiology(){}
    public static boolean dries(String id){return id.equals("dolphin")||id.equals("axolotl");}
    public static boolean showAir(String id,boolean gills,boolean wet,boolean underwater){
        if(gills||id.equals("axolotl"))return !wet;
        if(id.equals("dolphin"))return !wet||underwater;
        return underwater;
    }
    public static boolean dries(LivingEntity entity){var form=AbilityHooks.profile(entity);return form!=null&&dries(form.id());}
    public static int graceTicks(String id,MorphConfig config){return (id.equals("dolphin")?config.dolphinDryOutSeconds:config.axolotlDryOutSeconds)*20;}
    public static int displayAir(int elapsed,int grace,int maximum){return (int)Math.max(0,(long)maximum*Math.max(0,grace-elapsed)/grace);}
    /** Re-entry must reset dry display air before native underwater drowning checks run. */
    public static void beforeBreathing(LivingEntity entity){
        if(!(entity instanceof ServerPlayer player)||!dries(entity)||!entity.isInWaterOrRain())return;
        var state=RandomMorphMod.manager(player.level().getServer()).abilityState(player);
        if(state!=null&&state.dryTicks>0){state.dryTicks=0;player.setAirSupply(player.getMaxAirSupply());}
    }
    public static void tick(ServerPlayer player,AbilityState state,MorphConfig config){
        var form=AbilityHooks.profile(player);if(form==null||!dries(form.id()))return;
        if(player.isInWaterOrRain()){
            state.dryTicks=0;
            if(form.id().equals("axolotl"))player.setAirSupply(player.getMaxAirSupply());
            return; // Dolphin lung air is handled by normal breathing while submerged.
        }
        if(player.isCreative())return;
        state.dryTicks=Math.min(12001,state.dryTicks+1);
        int grace=graceTicks(form.id(),config);
        player.setAirSupply(displayAir(state.dryTicks,grace,player.getMaxAirSupply()));
        if(state.dryTicks>=grace)player.hurtServer(player.level(),player.damageSources().dryOut(),form.id().equals("dolphin")?1:2);
    }
}
